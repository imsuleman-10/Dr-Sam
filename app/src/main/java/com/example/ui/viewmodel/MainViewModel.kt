package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.ChatEntity
import com.example.data.database.MessageEntity
import com.example.data.database.ReportEntity
import com.example.data.database.SettingsEntity
import com.example.data.database.UserEntity
import com.example.data.repository.AuthResult
import com.example.data.repository.ChatRepository
import com.example.data.repository.UserRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class AuthState {
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: UserEntity) : AuthState()
    data class Error(val message: String) : AuthState()
}

class MainViewModel(
    private val userRepository: UserRepository,
    private val chatRepository: ChatRepository,
    application: Application
) : AndroidViewModel(application) {

    // Auth State
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // Chats List
    private val _chats = MutableStateFlow<List<ChatEntity>>(emptyList())
    val chats: StateFlow<List<ChatEntity>> = _chats.asStateFlow()

    // Selected Active Chat
    private val _activeChat = MutableStateFlow<ChatEntity?>(null)
    val activeChat: StateFlow<ChatEntity?> = _activeChat.asStateFlow()

    // Active Messages List
    private val _messages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val messages: StateFlow<List<MessageEntity>> = _messages.asStateFlow()

    // Active User Settings
    private val _userSettings = MutableStateFlow<SettingsEntity?>(null)
    val userSettings: StateFlow<SettingsEntity?> = _userSettings.asStateFlow()

    // AI Generation Loading indicator
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    // General UI Theme Dark Mode State (In Sync with Settings)
    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    // Admin Dashboard Info
    private val _adminStats = MutableStateFlow<Map<String, Int>?>(null)
    val adminStats: StateFlow<Map<String, Int>?> = _adminStats.asStateFlow()

    private val _adminUsers = MutableStateFlow<List<UserEntity>>(emptyList())
    val adminUsers: StateFlow<List<UserEntity>> = _adminUsers.asStateFlow()

    val adminReports: StateFlow<List<ReportEntity>> = chatRepository.allReports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Collect current user changes to update relative flows
        viewModelScope.launch {
            userRepository.currentUser.collect { user ->
                if (user != null) {
                    _authState.value = AuthState.Authenticated(user)
                    loadUserData(user.id)
                } else {
                    _authState.value = AuthState.Unauthenticated
                    _chats.value = emptyList()
                    _activeChat.value = null
                    _messages.value = emptyList()
                    _userSettings.value = null
                }
            }
        }
    }

    private fun loadUserData(userId: Int) {
        // Observe chats
        viewModelScope.launch {
            chatRepository.getChatsForUser(userId).collect { chatList ->
                _chats.value = chatList
            }
        }

        // Observe settings
        viewModelScope.launch {
            chatRepository.getSettingsForUser(userId).collect { settings ->
                val currentSettings = settings ?: SettingsEntity(userId = userId)
                _userSettings.value = currentSettings
                _isDarkMode.value = currentSettings.darkMode
            }
        }
    }

    // Sync status messages & logs
    private val _syncStatus = MutableStateFlow<String?>(null)
    val syncStatus: StateFlow<String?> = _syncStatus.asStateFlow()

    fun clearSyncStatus() {
        _syncStatus.value = null
    }

    fun syncFromCloud() {
        val user = userRepository.currentUser.value ?: return
        _syncStatus.value = "Synchronizing medical database with cloud..."
        viewModelScope.launch {
            val success = chatRepository.syncSupabaseToLocal(user.id)
            if (success) {
                _syncStatus.value = "Perfectly synchronized! All chats and dialogues retrieved successfully."
            } else {
                _syncStatus.value = "Sync completed or no active cloud connection detected."
            }
        }
    }

    fun testSupabaseConnection(url: String, key: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = com.example.util.SupabaseService.testConnection(url, key)
            onResult(success)
        }
    }

    // --- Authentication Actions ---
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            when (val result = userRepository.login(email, password, _userSettings.value)) {
                is AuthResult.Success -> {
                    // Handled inside flow collector
                }
                is AuthResult.Error -> {
                    _authState.value = AuthState.Error(result.message)
                }
            }
        }
    }

    fun signup(
        name: String,
        email: String,
        password: String,
        dob: String? = null,
        gender: String? = null,
        work: String? = null,
        country: String? = null,
        height: String? = null,
        weight: String? = null,
        bio: String? = null
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            when (val result = userRepository.signup(
                name, email, password, dob, gender, work, country, height, weight, bio, _userSettings.value
            )) {
                is AuthResult.Success -> {
                    // Create default settings on sign up
                    chatRepository.saveSettings(SettingsEntity(userId = result.user.id))
                }
                is AuthResult.Error -> {
                    _authState.value = AuthState.Error(result.message)
                }
            }
        }
    }

    fun logout() {
        userRepository.logout()
    }

    fun clearAuthError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Unauthenticated
        }
    }

    // --- Profile Editing Actions ---
    fun updateProfile(editedUser: UserEntity) {
        viewModelScope.launch {
            val success = userRepository.updateProfile(editedUser, _userSettings.value)
            if (success) {
                _authState.value = AuthState.Authenticated(editedUser)
            }
        }
    }

    fun deleteAccount() {
        val user = userRepository.currentUser.value ?: return
        viewModelScope.launch {
            userRepository.deleteAccount(user.id, _userSettings.value)
        }
    }

    // --- Settings Configuration Actions ---
    fun updateSettings(
        provider: String,
        apiKey: String,
        prompt: String,
        darkMode: Boolean,
        supabaseUrl: String = "",
        supabaseAnonKey: String = "",
        supabaseSyncEnabled: Boolean = false
    ) {
        val user = userRepository.currentUser.value ?: return
        val currentSettings = _userSettings.value ?: SettingsEntity(userId = user.id)
        val updatedSettings = currentSettings.copy(
            aiProvider = provider,
            apiKeyEncrypted = apiKey,
            personalizationPrompt = prompt,
            darkMode = darkMode,
            supabaseUrl = supabaseUrl,
            supabaseAnonKey = supabaseAnonKey,
            supabaseSyncEnabled = supabaseSyncEnabled
        )
        viewModelScope.launch {
            chatRepository.saveSettings(updatedSettings)
            _isDarkMode.value = darkMode
        }
    }

    // --- Chat Flow Actions ---
    fun selectChat(chatId: Int) {
        viewModelScope.launch {
            val selected = chats.value.find { it.id == chatId }
            if (selected != null) {
                _activeChat.value = selected
                // Observe messages
                chatRepository.getMessagesForChat(chatId).collect { msgList ->
                    _messages.value = msgList
                }
            }
        }
    }

    fun selectChatEntity(chat: ChatEntity?) {
        if (chat == null) {
            _activeChat.value = null
            _messages.value = emptyList()
            return
        }
        selectChat(chat.id)
    }

    suspend fun getMessagesForChatSync(chatId: Int): List<MessageEntity> {
        return chatRepository.getMessagesForChatSync(chatId)
    }

    fun createChat(title: String, navigateToChat: Boolean = true) {
        val user = userRepository.currentUser.value ?: return
        viewModelScope.launch {
            val chatId = chatRepository.createNewChat(user.id, title)
            if (navigateToChat) {
                selectChat(chatId)
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val user = userRepository.currentUser.value ?: return
        viewModelScope.launch {
            var active = _activeChat.value
            if (active == null) {
                // Generate a smart chat title of first words
                val title = if (text.length > 25) text.substring(0, 25) + "..." else text
                val chatId = chatRepository.createNewChat(user.id, title)
                active = ChatEntity(id = chatId, userId = user.id, title = title)
                _activeChat.value = active
                _messages.value = emptyList()

                // Force collecting messages for newly created chat
                launch {
                    chatRepository.getMessagesForChat(chatId).collect { msgList ->
                        _messages.value = msgList
                    }
                }
            }

            _isGenerating.value = true
            try {
                chatRepository.sendMessage(active.id, user.id, text.trim())
            } catch (e: Exception) {
                // Safe guard
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun deleteChat(chatId: Int) {
        viewModelScope.launch {
            chatRepository.deleteChat(chatId)
            if (_activeChat.value?.id == chatId) {
                _activeChat.value = null
                _messages.value = emptyList()
            }
        }
    }

    fun renameChat(chatId: Int, newTitle: String) {
        viewModelScope.launch {
            chatRepository.renameChat(chatId, newTitle)
            if (_activeChat.value?.id == chatId) {
                _activeChat.value = _activeChat.value?.copy(title = newTitle)
            }
        }
    }

    // --- Report Action ---
    fun submitReport(issue: String) {
        val user = userRepository.currentUser.value ?: return
        viewModelScope.launch {
            chatRepository.insertReport(user.id, user.name, issue)
        }
    }

    // --- Admin Dashboard Actions ---
    fun loadAdminDashboard() {
        viewModelScope.launch {
            _adminStats.value = chatRepository.getDbStats()
            _adminUsers.value = userRepository.getAllUsers()
        }
    }

    fun adminDeleteUser(userId: Int) {
        viewModelScope.launch {
            userRepository.deleteUser(userId)
            loadAdminDashboard()
        }
    }

    fun adminUpdateUserRole(userId: Int, newRole: String) {
        viewModelScope.launch {
            userRepository.updateUserRole(userId, newRole)
            loadAdminDashboard()
        }
    }

    fun adminDeleteReport(reportId: Int) {
        viewModelScope.launch {
            chatRepository.deleteReport(reportId)
            loadAdminDashboard()
        }
    }
}

// Factory Provider
class MainViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            val database = AppDatabase.getInstance(application)
            val userRepository = UserRepository(database.userDao())
            val chatRepository = ChatRepository(
                database.userDao(),
                database.chatDao(),
                database.messageDao(),
                database.settingsDao(),
                database.reportDao()
            )
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(userRepository, chatRepository, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
