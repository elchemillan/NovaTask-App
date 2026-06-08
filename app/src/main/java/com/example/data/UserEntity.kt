package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.security.MessageDigest
import java.util.UUID

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val username: String,
    val passwordHash: String,
    val salt: String,
    val failedAttempts: Int = 0,
    val lockedUntil: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun generateSalt(): String = UUID.randomUUID().toString()

        fun hashPassword(password: String, salt: String): String {
            val md = MessageDigest.getInstance("SHA-256")
            val bytes = md.digest((password + salt).toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}
