package com.example.data.repository

import android.util.Log
import com.example.data.database.UserEntity
import com.example.data.database.UserDao
import com.example.data.database.SettingsEntity
import com.example.util.SecurityUtils
import com.example.util.SupabaseService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

sealed class AuthResult {
    data class Success(val user: UserEntity) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class UserRepository(private val userDao: UserDao) {

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    suspend fun login(email: String, password: String, settings: SettingsEntity? = null): AuthResult = withContext(Dispatchers.IO) {
        if (email.isBlank() || password.isBlank()) {
            return@withContext AuthResult.Error("Email and password cannot be empty")
        }

        val trimmedEmail = email.trim().lowercase()

        // 1. If Supabase is active, verify online credentials
        if (SupabaseService.isSyncActive(settings)) {
            try {
                val cloudUser = SupabaseService.fetchUserByEmail(trimmedEmail, settings)
                if (cloudUser != null) {
                    val hashedInput = SecurityUtils.hashPassword(password)
                    if (cloudUser.passwordHash == hashedInput) {
                        // Persist/Update locally so user credentials exist in the Room cache
                        userDao.insertUser(cloudUser)
                        _currentUser.value = cloudUser
                        return@withContext AuthResult.Success(cloudUser)
                    } else {
                        return@withContext AuthResult.Error("Incorrect password (verified via Cloud)")
                    }
                }
            } catch (e: Exception) {
                Log.e("UserRepository", "Supabase online login query error: ${e.message}", e)
                // Continue to local verification
            }
        }

        // 2. Offline / local database fallback search
        val user = userDao.getUserByEmail(trimmedEmail)
            ?: return@withContext AuthResult.Error("No account found with this email")

        val hashedInput = SecurityUtils.hashPassword(password)
        if (user.passwordHash == hashedInput) {
            _currentUser.value = user
            AuthResult.Success(user)
        } else {
            AuthResult.Error("Incorrect password")
        }
    }

    suspend fun signup(
        name: String,
        email: String,
        password: String,
        dob: String? = null,
        gender: String? = null,
        work: String? = null,
        country: String? = null,
        height: String? = null,
        weight: String? = null,
        bio: String? = null,
        settings: SettingsEntity? = null
    ): AuthResult = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim().lowercase()
        if (name.isBlank() || trimmedEmail.isBlank() || password.isBlank()) {
            return@withContext AuthResult.Error("All fields are required")
        }

        // 1. Email existence checks
        if (SupabaseService.isSyncActive(settings)) {
            val cloudUser = SupabaseService.fetchUserByEmail(trimmedEmail, settings)
            if (cloudUser != null) {
                return@withContext AuthResult.Error("Email is already registered on cloud database")
            }
        }

        val existingUser = userDao.getUserByEmail(trimmedEmail)
        if (existingUser != null) {
            return@withContext AuthResult.Error("Email is already registered")
        }

        // Auto promote default admin email to 'admin' role
        val role = if (trimmedEmail == "sulemanzaheer09@gmail.com") "admin" else "user"
        val passwordHash = SecurityUtils.hashPassword(password)

        var newUser = UserEntity(
            name = name.trim(),
            email = trimmedEmail,
            passwordHash = passwordHash,
            role = role,
            dob = dob,
            gender = gender,
            work = work,
            country = country,
            height = height,
            weight = weight,
            bio = bio
        )

        // 2. Dispatch account profile online if Supabase sync is enabled
        if (SupabaseService.isSyncActive(settings)) {
            val remoteUser = SupabaseService.insertUser(newUser, settings)
            if (remoteUser != null) {
                newUser = remoteUser
            }
        }

        try {
            val insertedId = userDao.insertUser(newUser)
            val registeredUser = newUser.copy(id = if (newUser.id != 0) newUser.id else insertedId.toInt())
            _currentUser.value = registeredUser
            AuthResult.Success(registeredUser)
        } catch (e: Exception) {
            AuthResult.Error("Registration failed: ${e.localizedMessage}")
        }
    }

    suspend fun updateProfile(updatedUser: UserEntity, settings: SettingsEntity? = null): Boolean = withContext(Dispatchers.IO) {
        if (SupabaseService.isSyncActive(settings)) {
            SupabaseService.updateUser(updatedUser, settings)
        }
        try {
            userDao.updateUser(updatedUser)
            _currentUser.value = updatedUser
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteAccount(userId: Int, settings: SettingsEntity? = null): Boolean = withContext(Dispatchers.IO) {
        if (SupabaseService.isSyncActive(settings)) {
            SupabaseService.deleteUser(userId, settings)
        }
        try {
            userDao.deleteUserById(userId)
            logout()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun logout() {
        _currentUser.value = null
    }

    // Admin Functions
    suspend fun getAllUsers(): List<UserEntity> = withContext(Dispatchers.IO) {
        userDao.getAllUsers()
    }

    suspend fun deleteUser(userId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            userDao.deleteUserById(userId)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateUserRole(userId: Int, newRole: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val user = userDao.getUserById(userId)
            if (user != null) {
                userDao.updateUser(user.copy(role = newRole))
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}
