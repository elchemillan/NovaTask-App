package com.example.data

import retrofit2.Response
import retrofit2.http.*

data class NetworkTask(
    val id: Int = 0,
    val title: String,
    val description: String,
    val priority: Int,
    val completed: Boolean,
    val status: String = "PENDIENTE",
    val startDate: Long? = null,
    val dueDate: Long? = null,
    val reminderTime: Long? = null,
    val isDeleted: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)

interface TaskApi {
    @GET("tasks")
    suspend fun getTasks(
        @Header("Authorization") userToken: String
    ): Response<List<NetworkTask>>

    @POST("tasks/sync")
    suspend fun syncTasks(
        @Header("Authorization") userToken: String,
        @Body localChanges: List<NetworkTask>
    ): Response<List<NetworkTask>>
}
