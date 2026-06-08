package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE isDeleted = 0 AND username = :username ORDER BY completed ASC, priority DESC, dueDate ASC, id DESC")
    fun getActiveTasksFlow(username: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isDeleted = 0 AND username = :username ORDER BY completed ASC, priority DESC, dueDate ASC, id DESC")
    suspend fun getActiveTasks(username: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Int): TaskEntity?

    @Query("SELECT * FROM tasks WHERE synced = 0 AND username = :username")
    suspend fun getUnsyncedTasks(username: String): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("UPDATE tasks SET isDeleted = 1, synced = 0, lastUpdated = :timestamp WHERE id = :id")
    suspend fun softDeleteById(id: Int, timestamp: Long)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun hardDeleteById(id: Int)

    @Query("DELETE FROM tasks WHERE isDeleted = 1")
    suspend fun clearDeletedTasks()
}
