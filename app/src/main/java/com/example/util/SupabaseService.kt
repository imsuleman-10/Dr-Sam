package com.example.util

import android.util.Log
import com.example.BuildConfig
import com.example.data.database.ChatEntity
import com.example.data.database.MessageEntity
import com.example.data.database.ReportEntity
import com.example.data.database.SettingsEntity
import com.example.data.database.UserEntity
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object SupabaseService {
    private const val TAG = "SupabaseService"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Resolves the configured Supabase URL.
     * Prioritizes user's custom settings, falling back to BuildConfig.SUPABASE_URL if set.
     */
    fun getUrl(settings: SettingsEntity?): String {
        val settingsUrl = settings?.supabaseUrl?.trim() ?: ""
        if (settingsUrl.isNotBlank() && settingsUrl.startsWith("http")) {
            return settingsUrl
        }
        // Fallback to BuildConfig if defined and not empty placeholder
        return try {
            val buildUrl = BuildConfig.SUPABASE_URL
            if (buildUrl.isNotBlank() && !buildUrl.contains("your-project-id")) buildUrl else ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Resolves the configured Supabase public anon key.
     * Prioritizes user's custom settings, falling back to BuildConfig.SUPABASE_ANON_KEY if set.
     */
    fun getAnonKey(settings: SettingsEntity?): String {
        val settingsKey = settings?.supabaseAnonKey?.trim() ?: ""
        if (settingsKey.isNotBlank()) {
            return settingsKey
        }
        // Fallback to BuildConfig if defined and not empty placeholder
        return try {
            val buildKey = BuildConfig.SUPABASE_ANON_KEY
            if (buildKey.isNotBlank() && !buildKey.contains("eyJhbGciOiJIUzI1NiIsIn")) buildKey else ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Returns true if both URL and Key are configured and Sync Toggle is enabled.
     */
    fun isSyncActive(settings: SettingsEntity?): Boolean {
        val active = (settings?.supabaseSyncEnabled == true) && getUrl(settings).isNotBlank() && getAnonKey(settings).isNotBlank()
        Log.d(TAG, "isSyncActive: settingsSyncActive=${settings?.supabaseSyncEnabled}, urlEmpty=${getUrl(settings).isBlank()}, keyEmpty=${getAnonKey(settings).isBlank()} -> active=$active")
        return active
    }

    /**
     * Helper to test connection with Supabase. Does a lightweight request on the status API.
     */
    suspend fun testConnection(url: String, anonKey: String): Boolean {
        if (url.isBlank() || anonKey.isBlank()) return false
        val cleanUrl = if (url.endsWith("/")) url else "$url/"
        val requestUrl = "${cleanUrl}rest/v1/"
        
        Log.d(TAG, "Testing Supabase connection at: $requestUrl")
        return try {
            val request = Request.Builder()
                .url(requestUrl)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $anonKey")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                Log.d(TAG, "testConnection: code=${response.code}, message=${response.message}")
                response.code in 200..299
            }
        } catch (e: Exception) {
            Log.e(TAG, "testConnection failed: ${e.message}", e)
            false
        }
    }

    /**
     * Fetches user profile from Supabase using user email.
     */
    suspend fun fetchUserByEmail(email: String, settings: SettingsEntity?): UserEntity? {
        val baseUrl = getUrl(settings)
        val key = getAnonKey(settings)
        if (baseUrl.isBlank() || key.isBlank() || email.isBlank()) return null

        val cleanUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val requestUrl = "${cleanUrl}rest/v1/users?email=eq.${email.trim().lowercase()}&select=*"

        Log.d(TAG, "fetchUserByEmail: searching $email")
        try {
            val request = Request.Builder()
                .url(requestUrl)
                .addHeader("apikey", key)
                .addHeader("Authorization", "Bearer $key")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "fetchUserByEmail failed with code ${response.code}: ${response.body?.string()}")
                    return null
                }
                val bodyString = response.body?.string() ?: "[]"
                val jsonArray = JSONArray(bodyString)
                if (jsonArray.length() == 0) return null

                val json = jsonArray.getJSONObject(0)
                return UserEntity(
                    id = json.optInt("id", 0),
                    name = json.optString("name", ""),
                    email = json.optString("email", ""),
                    passwordHash = json.optString("password_hash", ""),
                    role = json.optString("role", "user"),
                    profileImage = json.optString("profile_image", null),
                    dob = json.optString("dob", null),
                    gender = json.optString("gender", null),
                    work = json.optString("work", null),
                    bio = json.optString("bio", null),
                    country = json.optString("country", null),
                    height = json.optString("height", null),
                    weight = json.optString("weight", null),
                    createdAt = json.optLong("created_at", System.currentTimeMillis())
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchUserByEmail failed: ${e.message}", e)
            return null
        }
    }

    /**
     * Inserts user profile record inside Supabase users table and returns with auto-generated row ID.
     */
    suspend fun insertUser(user: UserEntity, settings: SettingsEntity?): UserEntity? {
        val baseUrl = getUrl(settings)
        val key = getAnonKey(settings)
        if (baseUrl.isBlank() || key.isBlank()) return null

        val cleanUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val requestUrl = "${cleanUrl}rest/v1/users"

        Log.d(TAG, "insertUser: posting user ${user.email} -> $requestUrl")
        try {
            val jsonObject = JSONObject().apply {
                put("name", user.name)
                put("email", user.email)
                put("password_hash", user.passwordHash)
                put("role", user.role)
                put("dob", user.dob)
                put("gender", user.gender)
                put("work", user.work)
                put("bio", user.bio)
                put("country", user.country)
                put("height", user.height)
                put("weight", user.weight)
                put("created_at", user.createdAt)
            }

            val request = Request.Builder()
                .url(requestUrl)
                .addHeader("apikey", key)
                .addHeader("Authorization", "Bearer $key")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation") // Returns inserted object in array
                .post(jsonObject.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                Log.d(TAG, "insertUser: response code=${response.code}, body=$bodyString")
                if (!response.isSuccessful) {
                    Log.e(TAG, "insertUser failed: status ${response.code}")
                    return null
                }
                val jsonArray = JSONArray(bodyString)
                if (jsonArray.length() == 0) return null
                val json = jsonArray.getJSONObject(0)
                
                return user.copy(id = json.optInt("id", user.id))
            }
        } catch (e: Exception) {
            Log.e(TAG, "insertUser failed: ${e.message}", e)
            return null
        }
    }

    /**
     * Updates an existing user inside Supabase users table.
     */
    suspend fun updateUser(user: UserEntity, settings: SettingsEntity?): Boolean {
        val baseUrl = getUrl(settings)
        val key = getAnonKey(settings)
        if (baseUrl.isBlank() || key.isBlank()) return false

        val cleanUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val requestUrl = "${cleanUrl}rest/v1/users?email=eq.${user.email.lowercase()}"

        Log.d(TAG, "updateUser: patching user ${user.email}")
        try {
            val jsonObject = JSONObject().apply {
                put("name", user.name)
                put("dob", user.dob)
                put("gender", user.gender)
                put("work", user.work)
                put("bio", user.bio)
                put("country", user.country)
                put("height", user.height)
                put("weight", user.weight)
                put("role", user.role)
            }

            val request = Request.Builder()
                .url(requestUrl)
                .addHeader("apikey", key)
                .addHeader("Authorization", "Bearer $key")
                .addHeader("Content-Type", "application/json")
                .patch(jsonObject.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                Log.d(TAG, "updateUser: code=${response.code}")
                return response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateUser failed: ${e.message}", e)
            return false
        }
    }

    /**
     * Deletes user account from Supabase users table.
     */
    suspend fun deleteUser(userId: Int, settings: SettingsEntity?): Boolean {
        val baseUrl = getUrl(settings)
        val key = getAnonKey(settings)
        if (baseUrl.isBlank() || key.isBlank()) return false

        val cleanUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val requestUrl = "${cleanUrl}rest/v1/users?id=eq.$userId"

        Log.d(TAG, "deleteUser: deleting user id=$userId")
        try {
            val request = Request.Builder()
                .url(requestUrl)
                .addHeader("apikey", key)
                .addHeader("Authorization", "Bearer $key")
                .delete()
                .build()

            client.newCall(request).execute().use { response ->
                return response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "deleteUser failed: ${e.message}", e)
            return false
        }
    }

    /**
     * Syncs (downloads) all chats belonging to a user from Supabase.
     */
    suspend fun fetchChats(userId: Int, settings: SettingsEntity?): List<ChatEntity> {
        val baseUrl = getUrl(settings)
        val key = getAnonKey(settings)
        if (baseUrl.isBlank() || key.isBlank()) return emptyList()

        val cleanUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val requestUrl = "${cleanUrl}rest/v1/chats?user_id=eq.$userId&order=created_at.desc"

        Log.d(TAG, "fetchChats: pulling chats for userId=$userId")
        try {
            val request = Request.Builder()
                .url(requestUrl)
                .addHeader("apikey", key)
                .addHeader("Authorization", "Bearer $key")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val bodyString = response.body?.string() ?: "[]"
                val jsonArray = JSONArray(bodyString)
                val list = mutableListOf<ChatEntity>()
                for (i in 0 until jsonArray.length()) {
                    val json = jsonArray.getJSONObject(i)
                    list.add(
                        ChatEntity(
                            id = json.optInt("id", 0),
                            userId = json.optInt("user_id", userId),
                            title = json.optString("title", "Dr SAM Chat Summary"),
                            createdAt = json.optLong("created_at", System.currentTimeMillis())
                        )
                    )
                }
                return list
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchChats failed: ${e.message}", e)
            return emptyList()
        }
    }

    /**
     * Uploads/Inserts chat session on Supabase; receives back Supabase generated row ID.
     */
    suspend fun insertChat(chat: ChatEntity, settings: SettingsEntity?): ChatEntity? {
        val baseUrl = getUrl(settings)
        val key = getAnonKey(settings)
        if (baseUrl.isBlank() || key.isBlank()) return null

        val cleanUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val requestUrl = "${cleanUrl}rest/v1/chats"

        Log.d(TAG, "insertChat: pushing chat '${chat.title}'")
        try {
            val jsonObject = JSONObject().apply {
                put("user_id", chat.userId)
                put("title", chat.title)
                put("created_at", chat.createdAt)
            }

            val request = Request.Builder()
                .url(requestUrl)
                .addHeader("apikey", key)
                .addHeader("Authorization", "Bearer $key")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .post(jsonObject.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val bodyString = response.body?.string() ?: ""
                val jsonArray = JSONArray(bodyString)
                if (jsonArray.length() == 0) return null
                val json = jsonArray.getJSONObject(0)
                return chat.copy(id = json.optInt("id", chat.id))
            }
        } catch (e: Exception) {
            Log.e(TAG, "insertChat failed: ${e.message}", e)
            return null
        }
    }

    /**
     * Deletes chat and related messages online.
     */
    suspend fun deleteChat(chatId: Int, settings: SettingsEntity?): Boolean {
        val baseUrl = getUrl(settings)
        val key = getAnonKey(settings)
        if (baseUrl.isBlank() || key.isBlank()) return false

        val cleanUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        
        Log.d(TAG, "deleteChat: deleting online chat id=$chatId")
        try {
            // 1. Delete messages first
            val delMsgRequest = Request.Builder()
                .url("${cleanUrl}rest/v1/messages?chat_id=eq.$chatId")
                .addHeader("apikey", key)
                .addHeader("Authorization", "Bearer $key")
                .delete()
                .build()
            client.newCall(delMsgRequest).execute().close()

            // 2. Delete chat
            val delChatRequest = Request.Builder()
                .url("${cleanUrl}rest/v1/chats?id=eq.$chatId")
                .addHeader("apikey", key)
                .addHeader("Authorization", "Bearer $key")
                .delete()
                .build()
            client.newCall(delChatRequest).execute().use { response ->
                return response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "deleteChat failed: ${e.message}", e)
            return false
        }
    }

    /**
     * Rename chat online.
     */
    suspend fun renameChat(chatId: Int, newTitle: String, settings: SettingsEntity?): Boolean {
        val baseUrl = getUrl(settings)
        val key = getAnonKey(settings)
        if (baseUrl.isBlank() || key.isBlank()) return false

        val cleanUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val requestUrl = "${cleanUrl}rest/v1/chats?id=eq.$chatId"

        Log.d(TAG, "renameChat: syncing new name online for chat id=$chatId")
        try {
            val jsonObject = JSONObject().apply {
                put("title", newTitle)
            }
            val request = Request.Builder()
                .url(requestUrl)
                .addHeader("apikey", key)
                .addHeader("Authorization", "Bearer $key")
                .addHeader("Content-Type", "application/json")
                .patch(jsonObject.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                return response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "renameChat failed: ${e.message}", e)
            return false
        }
    }

    /**
     * Syncs (downloads) chronological dialogue messages for a specific chat.
     */
    suspend fun fetchMessages(chatId: Int, settings: SettingsEntity?): List<MessageEntity> {
        val baseUrl = getUrl(settings)
        val key = getAnonKey(settings)
        if (baseUrl.isBlank() || key.isBlank()) return emptyList()

        val cleanUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val requestUrl = "${cleanUrl}rest/v1/messages?chat_id=eq.$chatId&order=timestamp.asc"

        Log.d(TAG, "fetchMessages: pulling messages for remote chatId=$chatId")
        try {
            val request = Request.Builder()
                .url(requestUrl)
                .addHeader("apikey", key)
                .addHeader("Authorization", "Bearer $key")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val bodyString = response.body?.string() ?: "[]"
                val jsonArray = JSONArray(bodyString)
                val list = mutableListOf<MessageEntity>()
                for (i in 0 until jsonArray.length()) {
                    val json = jsonArray.getJSONObject(i)
                    list.add(
                        MessageEntity(
                            id = json.optInt("id", 0),
                            chatId = json.optInt("chat_id", chatId),
                            sender = json.optString("sender", "user"),
                            content = json.optString("content", ""),
                            timestamp = json.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
                return list
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchMessages failed: ${e.message}", e)
            return emptyList()
        }
    }

    /**
     * Uploads dialog messages to Supabase database.
     */
    suspend fun insertMessage(message: MessageEntity, settings: SettingsEntity?): Boolean {
        val baseUrl = getUrl(settings)
        val key = getAnonKey(settings)
        if (baseUrl.isBlank() || key.isBlank()) return false

        val cleanUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val requestUrl = "${cleanUrl}rest/v1/messages"

        Log.d(TAG, "insertMessage: uploading message inside chat ${message.chatId}")
        try {
            val jsonObject = JSONObject().apply {
                put("chat_id", message.chatId)
                put("sender", message.sender)
                put("content", message.content)
                put("timestamp", message.timestamp)
            }

            val request = Request.Builder()
                .url(requestUrl)
                .addHeader("apikey", key)
                .addHeader("Authorization", "Bearer $key")
                .addHeader("Content-Type", "application/json")
                .post(jsonObject.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                Log.d(TAG, "insertMessage: response code=${response.code}")
                return response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "insertMessage failed: ${e.message}", e)
            return false
        }
    }

    /**
     * Uploads/Inserts report concern entries online.
     */
    suspend fun insertReport(report: ReportEntity, settings: SettingsEntity?): Boolean {
        val baseUrl = getUrl(settings)
        val key = getAnonKey(settings)
        if (baseUrl.isBlank() || key.isBlank()) return false

        val cleanUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val requestUrl = "${cleanUrl}rest/v1/reports"

        Log.d(TAG, "insertReport: uploading report concern to Supabase")
        try {
            val jsonObject = JSONObject().apply {
                put("user_id", report.userId)
                put("user_name", report.userName)
                put("issue", report.issue)
                put("created_at", report.createdAt)
            }

            val request = Request.Builder()
                .url(requestUrl)
                .addHeader("apikey", key)
                .addHeader("Authorization", "Bearer $key")
                .addHeader("Content-Type", "application/json")
                .post(jsonObject.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                return response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "insertReport failed: ${e.message}", e)
            return false
        }
    }

    /**
     * Syncs (downloads) all reports from Supabase.
     */
    suspend fun fetchReports(settings: SettingsEntity?): List<ReportEntity> {
        val baseUrl = getUrl(settings)
        val key = getAnonKey(settings)
        if (baseUrl.isBlank() || key.isBlank()) return emptyList()

        val cleanUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val requestUrl = "${cleanUrl}rest/v1/reports?order=created_at.desc"

        Log.d(TAG, "fetchReports: pulling reports...")
        try {
            val request = Request.Builder()
                .url(requestUrl)
                .addHeader("apikey", key)
                .addHeader("Authorization", "Bearer $key")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val bodyString = response.body?.string() ?: "[]"
                val jsonArray = JSONArray(bodyString)
                val list = mutableListOf<ReportEntity>()
                for (i in 0 until jsonArray.length()) {
                    val json = jsonArray.getJSONObject(i)
                    list.add(
                        ReportEntity(
                            id = json.optInt("id", 0),
                            userId = json.optInt("user_id", 0),
                            userName = json.optString("user_name", ""),
                            issue = json.optString("issue", ""),
                            createdAt = json.optLong("created_at", System.currentTimeMillis())
                        )
                    )
                }
                return list
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchReports failed: ${e.message}", e)
            return emptyList()
        }
    }

    /**
     * Deletes report concern entries online.
     */
    suspend fun deleteReport(reportId: Int, settings: SettingsEntity?): Boolean {
        val baseUrl = getUrl(settings)
        val key = getAnonKey(settings)
        if (baseUrl.isBlank() || key.isBlank()) return false

        val cleanUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val requestUrl = "${cleanUrl}rest/v1/reports?id=eq.$reportId"

        Log.d(TAG, "deleteReport: deleting online report id=$reportId")
        try {
            val request = Request.Builder()
                .url(requestUrl)
                .addHeader("apikey", key)
                .addHeader("Authorization", "Bearer $key")
                .delete()
                .build()

            client.newCall(request).execute().use { response ->
                return response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "deleteReport failed: ${e.message}", e)
            return false
        }
    }
}
