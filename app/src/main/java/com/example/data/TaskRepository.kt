package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

data class SyncSummary(
    val updatedLocally: Int,
    val uploadedRemotely: Int,
    val deletedLocally: Int,
    val logs: List<String>,
    val isSimulated: Boolean,
    val success: Boolean
)

class TaskRepository(
    private val taskDao: TaskDao,
    private val userDao: UserDao,
    private val teamDao: TeamDao
) {

    // --- Team & Sharing Operations ---
    fun getTeamsFlow(username: String): Flow<List<TeamEntity>> = teamDao.getTeamsFlow(username)

    suspend fun insertTeam(team: TeamEntity): Long = withContext(Dispatchers.IO) {
        teamDao.insertTeam(team)
    }

    suspend fun deleteTeam(teamId: Int) = withContext(Dispatchers.IO) {
        teamDao.deleteTeam(teamId)
    }

    fun getTeamMembersFlow(teamId: Int): Flow<List<TeamMemberEntity>> = teamDao.getTeamMembersFlow(teamId)

    suspend fun insertTeamMember(member: TeamMemberEntity): Long = withContext(Dispatchers.IO) {
        teamDao.insertTeamMember(member)
    }

    suspend fun deleteTeamMember(memberId: Int) = withContext(Dispatchers.IO) {
        teamDao.deleteTeamMember(memberId)
    }

    suspend fun updateMemberStatus(memberId: Int, status: String) = withContext(Dispatchers.IO) {
        teamDao.updateMemberStatus(memberId, status)
    }

    fun getTeamTasksFlow(teamId: Int): Flow<List<TaskEntity>> = teamDao.getTeamTasksFlow(teamId)

    fun getActiveTasksFlow(username: String): Flow<List<TaskEntity>> = taskDao.getActiveTasksFlow(username)

    // User CRUD and Authentication Security
    suspend fun getUserByUsername(username: String): UserEntity? = withContext(Dispatchers.IO) {
        userDao.getUserByUsername(username)
    }

    suspend fun registerUser(user: UserEntity) = withContext(Dispatchers.IO) {
        userDao.insertUser(user)
    }

    suspend fun updateUser(user: UserEntity) = withContext(Dispatchers.IO) {
        userDao.updateUser(user)
    }

    suspend fun insertTask(task: TaskEntity): Long = withContext(Dispatchers.IO) {
        val updatedTask = task.copy(
            lastUpdated = System.currentTimeMillis(),
            synced = false
        )
        taskDao.insertTask(updatedTask)
    }

    suspend fun updateTask(task: TaskEntity) = withContext(Dispatchers.IO) {
        val updatedTask = task.copy(
            lastUpdated = System.currentTimeMillis(),
            synced = false
        )
        taskDao.updateTask(updatedTask)
    }

    suspend fun toggleCompleted(task: TaskEntity) = withContext(Dispatchers.IO) {
        val newCompleted = !task.completed
        val newStatus = if (newCompleted) "COMPLETADO" else "PENDIENTE"
        val updatedTask = task.copy(
            completed = newCompleted,
            status = newStatus,
            lastUpdated = System.currentTimeMillis(),
            synced = false
        )
        taskDao.updateTask(updatedTask)
    }

    suspend fun updateTaskStatus(task: TaskEntity, newStatus: String) = withContext(Dispatchers.IO) {
        val nextCompleted = (newStatus == "COMPLETADO")
        val updatedTask = task.copy(
            status = newStatus,
            completed = nextCompleted,
            lastUpdated = System.currentTimeMillis(),
            synced = false
        )
        taskDao.updateTask(updatedTask)
    }

    suspend fun softDeleteTask(id: Int) = withContext(Dispatchers.IO) {
        taskDao.softDeleteById(id, System.currentTimeMillis())
    }

    suspend fun hardDeleteTask(id: Int) = withContext(Dispatchers.IO) {
        taskDao.hardDeleteById(id)
    }

    suspend fun getTaskById(id: Int): TaskEntity? = withContext(Dispatchers.IO) {
        taskDao.getTaskById(id)
    }

    suspend fun clearDeletedTasks() = withContext(Dispatchers.IO) {
        taskDao.clearDeletedTasks()
    }

    /**
     * core Offline-First Sync Module.
     * Takes parameters configured dynamically by the user and executes real Retrofit transfers
     * with conflict resolution based on priority and "lastUpdated" timestamps.
     */
    suspend fun syncWithCloud(serverUrl: String, userToken: String, username: String): SyncSummary = withContext(Dispatchers.IO) {
        val logs = mutableListOf<String>()
        var updatedLocallyCount = 0
        var uploadedRemotelyCount = 0
        var deletedLocallyCount = 0

        val unsyncedLocalList = taskDao.getUnsyncedTasks(username)
        logs.add("Detectadas ${unsyncedLocalList.size} tareas sin sincronizar localmente.")

        // If the user selects the default simulated cloud, or if the URL format isn't fully set up yet
        if (serverUrl.isBlank() || serverUrl.equals("http://simulado", ignoreCase = true) || serverUrl.equals("simulado", ignoreCase = true)) {
            // -- LOGIC FOR COHESIVE INTERACTIVE SIMULATION --
            // This is perfect because it runs completely offline, shows real UX/conflict animations,
            // resolves mock conflicts, and avoids hardcrashed network permissions issues.
            try {
                logs.add("Iniciando simulación de sincronización de datos con 'Nube Inteligente'...")
                kotlinx.coroutines.delay(1800) // Realistic networks delays

                val simulatedRemoteUpdates = listOf(
                    NetworkTask(
                        id = 9991,
                        title = "💻 [Remoto] Revisar repositorios en GitHub",
                        description = "Sincronizado automáticamente desde el backend con prioridad alta.",
                        priority = 3,
                        completed = false,
                        status = "PENDIENTE",
                        dueDate = System.currentTimeMillis() + 86400000,
                        lastUpdated = System.currentTimeMillis() - 5000 // remote was updated 5s ago
                    ),
                    NetworkTask(
                        id = 9992,
                        title = "🚀 [Remoto] Planificación de Sprint",
                        description = "Actualizar tablero Kanban del equipo.",
                        priority = 2,
                        completed = true,
                        status = "COMPLETADO",
                        dueDate = System.currentTimeMillis() + 172800000,
                        lastUpdated = System.currentTimeMillis() - 10000
                    )
                )

                // Process Local Changes
                for (localTask in unsyncedLocalList) {
                    if (localTask.isDeleted) {
                        taskDao.hardDeleteById(localTask.id)
                        deletedLocallyCount++
                        logs.add("🗑️ Upload: Eliminada físicamente la tarea ID ${localTask.id} en la nube.")
                    } else {
                        // Mark as synced
                        taskDao.updateTask(localTask.copy(synced = true))
                        uploadedRemotelyCount++
                        logs.add("📤 Upload: Enviada tarea '${localTask.title}' a la nube.")
                    }
                }

                // Process Remote Changes
                for (remote in simulatedRemoteUpdates) {
                    val localMatch = taskDao.getTaskById(remote.id)
                    if (localMatch == null) {
                        // Insert remote since it doesn't exist locally
                        val newLocal = TaskEntity(
                            id = remote.id,
                            title = remote.title,
                            description = remote.description,
                            priority = remote.priority,
                            completed = remote.completed,
                            status = remote.status,
                            startDate = remote.startDate,
                            dueDate = remote.dueDate,
                            reminderTime = remote.reminderTime,
                            synced = true,
                            isDeleted = false,
                            lastUpdated = remote.lastUpdated,
                            username = username
                        )
                        taskDao.insertTask(newLocal)
                        updatedLocallyCount++
                        logs.add("📥 Download: Nueva tarea añadida desde la nube: '${remote.title}'")
                    } else {
                        // Conflict Resolution based on timestamps
                        if (remote.lastUpdated > localMatch.lastUpdated) {
                            val updatedLocal = localMatch.copy(
                                title = remote.title,
                                description = remote.description,
                                priority = remote.priority,
                                completed = remote.completed,
                                status = remote.status,
                                dueDate = remote.dueDate,
                                reminderTime = remote.reminderTime,
                                synced = true,
                                lastUpdated = remote.lastUpdated
                            )
                            taskDao.updateTask(updatedLocal)
                            updatedLocallyCount++
                            logs.add("🔄 Conflicto: Se actualizó '${remote.title}' porque la versión en la nube es más reciente.")
                        } else {
                            logs.add("🤝 Conservado local: '${localMatch.title}' es más reciente que la versión remota.")
                        }
                    }
                }

                logs.add("Sincronización simulada completada con éxito.")
                return@withContext SyncSummary(
                    updatedLocally = updatedLocallyCount,
                    uploadedRemotely = uploadedRemotelyCount,
                    deletedLocally = deletedLocallyCount,
                    logs = logs,
                    isSimulated = true,
                    success = true
                )
            } catch (e: Exception) {
                return@withContext SyncSummary(0, 0, 0, listOf("Error en simulación: ${e.message}"), true, false)
            }
        }

        // -- ACTUAL DEPLOYED HTTP NETWORK CONNECTION --
        try {
            logs.add("Conectando con el servidor en: $serverUrl")
            
            // Setup Retrofit dynamically
            val interceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            val client = OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .addInterceptor(interceptor)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/")
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()

            val api = retrofit.create(TaskApi::class.java)

            // Convert local unsynced to Network payload
            val changesToUpload = unsyncedLocalList.map {
                NetworkTask(
                    id = if (it.id >= 9900) 0 else it.id, // clean temporary test IDs
                    title = it.title,
                    description = it.description,
                    priority = it.priority,
                    completed = it.completed,
                    status = it.status,
                    startDate = it.startDate,
                    dueDate = it.dueDate,
                    reminderTime = it.reminderTime,
                    isDeleted = it.isDeleted,
                    lastUpdated = it.lastUpdated
                )
            }

            logs.add("Enviando un total de ${changesToUpload.size} modificaciones locales...")
            
            val response = api.syncTasks(
                userToken = "Bearer $userToken",
                localChanges = changesToUpload
            )

            if (response.isSuccessful) {
                val remoteTasks = response.body() ?: emptyList()
                logs.add("¡Respuesta del servidor exitosa! Recibidas ${remoteTasks.size} tareas para fusionar.")

                // Clear soft deleted tasks locally since they are confirmed deleted on sever
                val deletedIds = unsyncedLocalList.filter { it.isDeleted }.map { it.id }
                deletedIds.forEach { id ->
                    taskDao.hardDeleteById(id)
                    deletedLocallyCount++
                }

                // Process merged list returned from servers
                for (remote in remoteTasks) {
                    if (remote.isDeleted) {
                        val localMatch = taskDao.getTaskById(remote.id)
                        if (localMatch != null) {
                            taskDao.hardDeleteById(remote.id)
                            deletedLocallyCount++
                            logs.add("🗑️ Sincronización Remota: Tarea '${remote.title}' eliminada por orden remota.")
                        }
                        continue
                    }

                    val localMatch = taskDao.getTaskById(remote.id)
                    if (localMatch == null) {
                        // Insert remote
                        val entity = TaskEntity(
                            id = remote.id,
                            title = remote.title,
                            description = remote.description,
                            priority = remote.priority,
                            completed = remote.completed,
                            status = remote.status,
                            startDate = remote.startDate,
                            dueDate = remote.dueDate,
                            reminderTime = remote.reminderTime,
                            synced = true,
                            isDeleted = false,
                            lastUpdated = remote.lastUpdated,
                            username = username
                        )
                        taskDao.insertTask(entity)
                        updatedLocallyCount++
                        logs.add("📥 Descargada nueva tarea: '${remote.title}'")
                    } else {
                        // Compare timestamps to resolve conflict
                        if (remote.lastUpdated > localMatch.lastUpdated) {
                            val entity = localMatch.copy(
                                title = remote.title,
                                description = remote.description,
                                priority = remote.priority,
                                completed = remote.completed,
                                status = remote.status,
                                dueDate = remote.dueDate,
                                reminderTime = remote.reminderTime,
                                synced = true,
                                lastUpdated = remote.lastUpdated
                            )
                            taskDao.updateTask(entity)
                            updatedLocallyCount++
                            logs.add("🔄 Fusionado: '${remote.title}' sobrescrito con datos del servidor.")
                        } else {
                            uploadedRemotelyCount++ // Local was newer; keep local and mark to publish next
                            taskDao.updateTask(localMatch.copy(synced = false))
                            logs.add("🤝 Conservado local: '${localMatch.title}' es más reciente, se subirá en el próximo ciclo.")
                        }
                    }
                }

                // Mark remaining uploaded items asSynced
                unsyncedLocalList.filter { !it.isDeleted && !deletedIds.contains(it.id) }.forEach {
                    taskDao.updateTask(it.copy(synced = true))
                    uploadedRemotelyCount++
                }

                logs.add("Completada sincronización web con el servidor.")
                return@withContext SyncSummary(
                    updatedLocally = updatedLocallyCount,
                    uploadedRemotely = uploadedRemotelyCount,
                    deletedLocally = deletedLocallyCount,
                    logs = logs,
                    isSimulated = false,
                    success = true
                )

            } else {
                val errorCode = response.code()
                val errorBody = response.errorBody()?.string() ?: "Sin detalles"
                logs.add("⚠️ Error del Servidor HTTP $errorCode: $errorBody")
                return@withContext SyncSummary(
                    updatedLocally = 0,
                    uploadedRemotely = 0,
                    deletedLocally = 0,
                    logs = logs,
                    isSimulated = false,
                    success = false
                )
            }

        } catch (e: Exception) {
            Log.e("TaskRepository", "Sync failed: ${e.message}", e)
            logs.add("⚠️ Excepción de Red: ${e.localizedMessage ?: "Fallo de conexión"}")
            logs.add("Sugerencia: Revisa que la URL base sea correcta y que haya internet activo.")
            return@withContext SyncSummary(
                updatedLocally = 0,
                uploadedRemotely = 0,
                deletedLocally = 0,
                logs = logs,
                isSimulated = false,
                success = false
            )
        }
    }
}
