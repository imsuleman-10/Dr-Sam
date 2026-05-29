package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val email: String,
    val passwordHash: String,
    val role: String = "user", // "user", "admin"
    val profileImage: String? = null,
    val dob: String? = null,
    val gender: String? = null,
    val work: String? = null,
    val bio: String? = null,
    val country: String? = null,
    val height: String? = null,
    val weight: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val title: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val chatId: Int,
    val sender: String, // "user", "ai"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val aiProvider: String = "Gemini", // "Gemini", "OpenAI", "OpenRouter", "Custom"
    val apiKeyEncrypted: String = "",
    val personalizationPrompt: String = "",
    val darkMode: Boolean = true,
    val supabaseUrl: String = "",
    val supabaseAnonKey: String = "",
    val supabaseSyncEnabled: Boolean = false
)

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val userName: String,
    val issue: String,
    val createdAt: Long = System.currentTimeMillis()
)
