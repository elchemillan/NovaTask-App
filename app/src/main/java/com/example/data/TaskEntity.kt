package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val priority: Int, // 1 = Low, 2 = Medium, 3 = High
    val completed: Boolean = false,
    val status: String = "PENDIENTE",
    val startDate: Long? = null,
    val dueDate: Long? = null,
    val reminderTime: Long? = null,
    val synced: Boolean = false,
    val isDeleted: Boolean = false, // Soft-delete to allow syncing deletion with cloud
    val lastUpdated: Long = System.currentTimeMillis(),
    val username: String = "",
    val teamId: Int? = null,
    val sharedWithEmail: String? = null
)
