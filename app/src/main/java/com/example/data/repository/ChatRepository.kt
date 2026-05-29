package com.example.data.repository

import android.util.Log
import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import com.example.data.database.*
import com.example.util.SupabaseService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class ChatRepository(
    private val userDao: UserDao,
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
    private val settingsDao: SettingsDao,
    private val reportDao: ReportDao
) {
    val allReports = reportDao.getAllReportsFlow()

    fun getChatsForUser(userId: Int): Flow<List<ChatEntity>> {
        return chatDao.getChatsByUserIdFlow(userId)
    }

    fun getAllChatsAdmin(): Flow<List<ChatEntity>> {
        return chatDao.getAllChatsFlow()
    }

    fun getMessagesForChat(chatId: Int): Flow<List<MessageEntity>> {
        return messageDao.getMessagesByChatIdFlow(chatId)
    }

    suspend fun getMessagesForChatSync(chatId: Int): List<MessageEntity> = withContext(Dispatchers.IO) {
        messageDao.getMessagesByChatId(chatId)
    }

    fun getSettingsForUser(userId: Int): Flow<SettingsEntity?> {
        return settingsDao.getSettingsByUserIdFlow(userId)
    }

    suspend fun createNewChat(userId: Int, title: String): Int = withContext(Dispatchers.IO) {
        val settings = settingsDao.getSettingsByUserIdSync(userId)
        var chat = ChatEntity(userId = userId, title = title)
        
        if (SupabaseService.isSyncActive(settings)) {
            try {
                val cloudChat = SupabaseService.insertChat(chat, settings)
                if (cloudChat != null) {
                    chat = cloudChat
                }
            } catch (e: Exception) {
                Log.e("ChatRepository", "Supabase createNewChat online insert failed: ${e.message}", e)
            }
        }
        chatDao.insertChat(chat).toInt()
    }

    suspend fun deleteChat(chatId: Int) = withContext(Dispatchers.IO) {
        try {
            // Retrieve chat locally first to find userId and load settings
            val chat = chatDao.getChatById(chatId)
            if (chat != null) {
                val settings = settingsDao.getSettingsByUserIdSync(chat.userId)
                if (SupabaseService.isSyncActive(settings)) {
                    SupabaseService.deleteChat(chatId, settings)
                }
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Supabase deleteChat online failed: ${e.message}", e)
        }
        messageDao.deleteMessagesByChatId(chatId)
        chatDao.deleteChatById(chatId)
    }

    suspend fun renameChat(chatId: Int, newTitle: String) = withContext(Dispatchers.IO) {
        try {
            val chat = chatDao.getChatById(chatId)
            if (chat != null) {
                val settings = settingsDao.getSettingsByUserIdSync(chat.userId)
                if (SupabaseService.isSyncActive(settings)) {
                    SupabaseService.renameChat(chatId, newTitle, settings)
                }
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Supabase renameChat online failed: ${e.message}", e)
        }
        chatDao.updateChatTitle(chatId, newTitle)
    }

    suspend fun saveSettings(settings: SettingsEntity) = withContext(Dispatchers.IO) {
        settingsDao.insertOrUpdateSettings(settings)
    }

    suspend fun insertReport(userId: Int, userName: String, issue: String) = withContext(Dispatchers.IO) {
        val settings = settingsDao.getSettingsByUserIdSync(userId)
        val report = ReportEntity(userId = userId, userName = userName, issue = issue)
        
        if (SupabaseService.isSyncActive(settings)) {
            try {
                SupabaseService.insertReport(report, settings)
            } catch (e: Exception) {
                Log.e("ChatRepository", "Supabase report insert online failed: ${e.message}", e)
            }
        }
        reportDao.insertReport(report)
    }

    suspend fun deleteReport(reportId: Int) = withContext(Dispatchers.IO) {
        try {
            // Find global cloud settings to sync report deletion if configured
            val settings = settingsDao.getSettingsByUserIdSync(1)
            if (SupabaseService.isSyncActive(settings)) {
                SupabaseService.deleteReport(reportId, settings)
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Supabase deleteReport online failed: ${e.message}", e)
        }
        reportDao.deleteReportById(reportId)
    }

    /**
     * Downloads/Syncs the remote database from Supabase and applies it to the local cache.
     */
    suspend fun syncSupabaseToLocal(userId: Int): Boolean = withContext(Dispatchers.IO) {
        val settings = settingsDao.getSettingsByUserIdSync(userId)
        if (!SupabaseService.isSyncActive(settings)) return@withContext false
        try {
            // 1. Fetch remote chats
            val remoteChats = SupabaseService.fetchChats(userId, settings)
            Log.d("ChatRepository", "syncSupabaseToLocal: fetched ${remoteChats.size} remote chats")
            
            for (chat in remoteChats) {
                // Upsert local chat cache
                chatDao.insertChat(chat)
                
                // 2. Fetch remote messages inside this chat
                val remoteMessages = SupabaseService.fetchMessages(chat.id, settings)
                Log.d("ChatRepository", "syncSupabaseToLocal: fetched ${remoteMessages.size} messages for chat id ${chat.id}")
                for (msg in remoteMessages) {
                    messageDao.insertMessage(msg)
                }
            }
            true
        } catch (e: Exception) {
            Log.e("ChatRepository", "syncSupabaseToLocal execution error: ${e.message}", e)
            false
        }
    }

    suspend fun getDbStats(): Map<String, Int> = withContext(Dispatchers.IO) {
        val usersCount = userDao.getAllUsers().size
        val reportsCount = reportDao.getAllReportsFlow().firstOrNull()?.size ?: 0
        val chatsCount = chatDao.getAllChatsFlow().firstOrNull()?.size ?: 0
        val messagesCount = messageDao.getAllMessagesFlow().firstOrNull()?.size ?: 0
        mapOf(
            "total_users" to usersCount,
            "total_reports" to reportsCount,
            "total_chats" to chatsCount,
            "total_messages" to messagesCount
        )
    }

    suspend fun sendMessage(
        chatId: Int,
        userId: Int,
        text: String
    ): String = withContext(Dispatchers.IO) {
        // 1. Fetch settings for synchronization
        val settings = settingsDao.getSettingsByUserIdSync(userId) ?: SettingsEntity(userId = userId)

        // 2. Save User Message locally
        val userMessage = MessageEntity(chatId = chatId, sender = "user", content = text)
        val generatedMsgId = messageDao.insertMessage(userMessage).toInt()

        // 3. Upload User Message to cloud Supabase if active
        if (SupabaseService.isSyncActive(settings)) {
            try {
                val syncMsg = userMessage.copy(id = generatedMsgId)
                SupabaseService.insertMessage(syncMsg, settings)
            } catch (e: Exception) {
                Log.e("ChatRepository", "Supabase insert message failed: ${e.message}", e)
            }
        }

        val provider = settings.aiProvider
        val userPromptOverride = settings.personalizationPrompt
        val customKey = settings.apiKeyEncrypted

        // Decide which API key to use
        val apiKey = if (customKey.isNotBlank()) customKey else BuildConfig.GEMINI_API_KEY
        val systemPrompt = buildSystemPrompt(userPromptOverride)

        var aiResponseText = ""

        try {
            if (provider == "Gemini") {
                // Fetch previous messages in this chat to feed history (last 10 turns for token safety)
                val conversationHistory = messageDao.getMessagesByChatIdFlow(chatId).firstOrNull() ?: emptyList()
                val apiContents = conversationHistory.takeLast(11).map { msg ->
                    Content(parts = listOf(Part(text = msg.content)))
                }

                val systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
                val request = GenerateContentRequest(
                    contents = apiContents,
                    systemInstruction = systemInstruction
                )

                // Call direct REST API
                val service = RetrofitClient.service
                val apiResponse = service.generateContent(apiKey, request)
                aiResponseText = apiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "Error: No response generated by Dr SAM."
            } else {
                // Simulation of Custom API Service for OpenAI, OpenRouter or Generic Endpoint
                aiResponseText = simulateCustomProviderResponse(provider, text, systemPrompt)
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "AI Error: ${e.message}", e)
            aiResponseText = getLocalDoctorFallback(text, systemPrompt, e.localizedMessage ?: "Network connection failed")
        }

        // 4. Save AI Response locally
        val aiMessage = MessageEntity(chatId = chatId, sender = "ai", content = aiResponseText)
        val generatedAiId = messageDao.insertMessage(aiMessage).toInt()

        // 5. Upload AI Response to cloud Supabase if active
        if (SupabaseService.isSyncActive(settings)) {
            try {
                val syncAi = aiMessage.copy(id = generatedAiId)
                SupabaseService.insertMessage(syncAi, settings)
            } catch (e: Exception) {
                Log.e("ChatRepository", "Supabase insert AI response failed: ${e.message}", e)
            }
        }

        aiResponseText
    }

    private fun buildSystemPrompt(userPromptOverride: String): String {
        val basePrompt = """
            You are Dr SAM, an AI-powered empathetic medical assistant. You help users understand health issues by explaining symptoms professionally and providing guidance.
            MEDICAL GUARDRAILS AND BEHAVIOR:
            1. DISCLAIMER REQUIREMENT: You MUST include this exact disclaimer in your answer: "Dr SAM is an AI assistant and not a licensed medical doctor." Explain that your guidance is purely educational and informational.
            2. DIAGNOSTIC RESTRAINT: Do not offer definitive diagnoses or declare clinical certainty. Use words like "could match symptoms of", "possible causes might be".
            3. CAUTIOUS OTC RECOMMENDATION: Carefully list standard over-the-counter options (e.g., Ibuprofen or Paracetamol for fever/mild pain, antacids for minor acid reflux, saline spray for congestion) where appropriate, with explicit instructions to check packages and ask pharmacists.
            4. COMPREHENSIVE RELEVANT FOLLOW-UPS: Ask 1 or 2 clear, helpful follow-up questions to understand critical context (e.g. onset, pain score, accompanying allergies).
            5. EMERGENCY TRIAGE: For severe signs list (chest pain, chest pressure, sudden numbness/paralysis, severe acute pain, shortness of breath), immediately urge consulting emergency services or visiting the nearest hospital.
        """.trimIndent()

        return if (userPromptOverride.isNotBlank()) {
            "$basePrompt\n\nADDITIONAL USER-SPECIFIED STYLE/INSTRUCTION: $userPromptOverride"
        } else {
            basePrompt
        }
    }

    private fun simulateCustomProviderResponse(provider: String, userText: String, systemPrompt: String): String {
        return """
            [Simulated $provider Response]
            
            Based on your report of symptoms, I would be glad to help provide information. As a reminder, Dr SAM is an AI assistant and not a licensed medical doctor.
            
            You asked: "$userText"
            
            Here is some educational guidance regarding this condition:
            • Possible Causes: This could be related to common non-urgent conditions, fatigue, or mild inflammation.
            • Precautions: Keep resting, consume plenty of fluids, and note any changes in temperature or pain.
            • Over-the-Counter Options: For minor muscle ache or discomfort, standard pain relievers like Paracetamol can be carefully considered under pharmacist direction.
            
            To help narrow this down further:
            Could you tell me how long this has been happening, and are you feeling any localized warmth or swelling?
            
            Please seek a formal consultation with a primary care physician to rule out serious issues or for an accurate diagnosis.
        """.trimIndent()
    }

    private fun getLocalDoctorFallback(userText: String, systemPrompt: String, errorMsg: String): String {
        val lower = userText.lowercase()
        val diagnosticHelp = when {
            lower.contains("headache") || lower.contains("migraine") -> {
                "For headaches, possible causes include tension, dehydration, or digital eye strain. Rest in a dark, quiet room, drink some water, and consider a warm compress. Cautious over-the-counter options include Paracetamol or Ibuprofen."
            }
            lower.contains("fever") || lower.contains("temperature") || lower.contains("cold") -> {
                "For a mild temperature or cold-like symptoms, rest and hydration are key. Over-the-counter options like Paracetamol can help naturally reduce discomfort, but monitor for breathing difficulties or neck stiffness."
            }
            lower.contains("stomach") || lower.contains("pain") || lower.contains("acid") || lower.contains("nausea") -> {
                "Stomach aches can arise from indigestion, mild food poisoning, or stress. Sip water or ginger tea. Avoid heavy, fatty foods. Normal antacids can assist with heartburn."
            }
            else -> {
                "I've received your query about these symptoms. Possible reasons vary from standard muscle tension to environmental factors. Please monitor your symptoms closely and rest."
            }
        }

        return """
            Dr SAM — AI Health Assistant
            Disclaimer: Dr SAM is an AI assistant and not a licensed medical doctor.
            
            *Note: Your device is currently offline or the API key limit has been reached. Showing offline guided assistance (${errorMsg}).*
            
            $diagnosticHelp
            
            Recommended General Precautions:
            1. Ensure proper hydration (water, electrolyte drinks).
            2. Rest in a comfortable, well-ventilated environment.
            3. Document any changes in pain levels or body temperature.
            
            Careful Over-the-Counter Options:
            • Standard mild pain relievers/fever reducers (e.g., Paracetamol or Ibuprofen) can be considered, carefully adhering to dosage instructions on packaging.
            
            Follow-up Questions:
            • Could you specify when these symptoms first started and their intensity on a scale of 1-10?
            • Are you experiencing any secondary issues like severe fatigue, dizziness, or chest strain? If you have emergency signs like sudden numbness or high breathing difficulty, please contact emergency lines immediately.
        """.trimIndent()
    }
}
