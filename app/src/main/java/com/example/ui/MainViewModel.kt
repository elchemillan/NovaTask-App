package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.notification.ReminderScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(
    private val app: Application,
    private val repository: TaskRepository
) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("user_profile", android.content.Context.MODE_PRIVATE)

    // Logged in user tracking
    private val _loggedInUser = MutableStateFlow<String?>(prefs.getString("logged_in_username", null))
    val loggedInUser = _loggedInUser.asStateFlow()

    // Auth screen states
    val authUsernameInput = MutableStateFlow("")
    val authPasswordInput = MutableStateFlow("")
    val authConfirmPasswordInput = MutableStateFlow("")
    val isRegisterMode = MutableStateFlow(false)
    
    private val _authErrorMessage = MutableStateFlow<String?>(null)
    val authErrorMessage = _authErrorMessage.asStateFlow()
    
    private val _authLoading = MutableStateFlow(false)
    val authLoading = _authLoading.asStateFlow()
    
    private val _cooldownTimeLeft = MutableStateFlow(0)
    val cooldownTimeLeft = _cooldownTimeLeft.asStateFlow()

    // Filtering State
    private val _statusFilter = MutableStateFlow(StatusFilter.ALL)
    val statusFilter = _statusFilter.asStateFlow()

    private val _priorityFilter = MutableStateFlow(PriorityFilter.ALL)
    val priorityFilter = _priorityFilter.asStateFlow()

    // Task List State
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val tasks: StateFlow<List<TaskEntity>> = _loggedInUser.flatMapLatest { username ->
        if (username == null) {
            flowOf(emptyList())
        } else {
            combine(
                repository.getActiveTasksFlow(username),
                _statusFilter,
                _priorityFilter
            ) { allTasks, status, priority ->
                allTasks.filter { task ->
                    val matchesStatus = when (status) {
                        StatusFilter.ALL -> true
                        StatusFilter.PENDING -> !task.completed
                        StatusFilter.COMPLETED -> task.completed
                    }
                    val matchesPriority = when (priority) {
                        PriorityFilter.ALL -> true
                        PriorityFilter.LOW -> task.priority == 1
                        PriorityFilter.MEDIUM -> task.priority == 2
                        PriorityFilter.HIGH -> task.priority == 3
                    }
                    matchesStatus && matchesPriority
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Form inputs state
    var titleInput = MutableStateFlow("")
        private set
    var descInput = MutableStateFlow("")
        private set
    var priorityInput = MutableStateFlow(2) // Default to Medium
        private set
    var startDateInput = MutableStateFlow<Long?>(null)
        private set
    var dueDateInput = MutableStateFlow<Long?>(null)
        private set
    var reminderTimeInput = MutableStateFlow<Long?>(null)
        private set
    var editingTaskId = MutableStateFlow<Int?>(null)
        private set
    var teamIdInput = MutableStateFlow<Int?>(null)
        private set
    var sharedEmailInput = MutableStateFlow("")
        private set
    var statusInput = MutableStateFlow("PENDIENTE")
        private set

    // Panel controllers
    private val _isFormVisible = MutableStateFlow(false)
    val isFormVisible = _isFormVisible.asStateFlow()

    // Profile preferences & state
    private val _userName = MutableStateFlow(prefs.getString("user_name", "Jim C.") ?: "Jim C.")
    val userName = _userName.asStateFlow()

    private val _userEmail = MutableStateFlow(prefs.getString("user_email", "jimc.inf@gmail.com") ?: "jimc.inf@gmail.com")
    val userEmail = _userEmail.asStateFlow()

    private val _profilePhotoUri = MutableStateFlow(prefs.getString("profile_photo_uri", "") ?: "")
    val profilePhotoUri = _profilePhotoUri.asStateFlow()

    private val _themeColor = MutableStateFlow(prefs.getString("theme_color_style", "OCEAN") ?: "OCEAN")
    val themeColor = _themeColor.asStateFlow()

    private val _tabColor = MutableStateFlow(prefs.getString("tab_color_style", "DEFAULT") ?: "DEFAULT")
    val tabColor = _tabColor.asStateFlow()

    private val _themeMode = MutableStateFlow(prefs.getString("theme_mode_preference", "FOLLOW_SYSTEM") ?: "FOLLOW_SYSTEM")
    val themeMode = _themeMode.asStateFlow()

    private val _isProfileVisible = MutableStateFlow(false)
    val isProfileVisible = _isProfileVisible.asStateFlow()

    // Sync settings & status
    val serverUrl = MutableStateFlow(prefs.getString("server_url", "Simulado") ?: "Simulado")
    val userToken = MutableStateFlow(prefs.getString("user_token", "user_${(1000..9999).random()}") ?: "user_${(1000..9999).random()}")

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _syncSummary = MutableStateFlow<SyncSummary?>(null)
    val syncSummary = _syncSummary.asStateFlow()

    private val _showSyncLogs = MutableStateFlow(false)
    val showSyncLogs = _showSyncLogs.asStateFlow()

    init {
        // Save initial values to preferences if they are not already set
        if (!prefs.contains("user_name")) {
            prefs.edit().putString("user_name", _userName.value).apply()
        }
        if (!prefs.contains("user_email")) {
            prefs.edit().putString("user_email", _userEmail.value).apply()
        }
        if (!prefs.contains("user_token")) {
            prefs.edit().putString("user_token", userToken.value).apply()
        }
        if (!prefs.contains("server_url")) {
            prefs.edit().putString("server_url", serverUrl.value).apply()
        }

        // Start cooldown countdown timer coroutine
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000)
                if (_cooldownTimeLeft.value > 0) {
                    _cooldownTimeLeft.value -= 1
                }
            }
        }

        // Keep local preferences in sync with text entry fields reactively
        viewModelScope.launch {
            serverUrl.collect {
                prefs.edit().putString("server_url", it).apply()
            }
        }
        viewModelScope.launch {
            userToken.collect {
                prefs.edit().putString("user_token", it).apply()
            }
        }
    }

    fun setProfileVisible(visible: Boolean) {
        _isProfileVisible.value = visible
    }

    fun updateProfile(name: String, email: String, photoUri: String = _profilePhotoUri.value) {
        _userName.value = name
        _userEmail.value = email
        _profilePhotoUri.value = photoUri

        val cleanedName = name.trim().replace(" ", "").lowercase(java.util.Locale.getDefault())
        if (cleanedName.isNotEmpty() && userToken.value.startsWith("user_")) {
            val newToken = "${cleanedName}_${(1000..9999).random()}"
            userToken.value = newToken
            prefs.edit().putString("user_token", newToken).apply()
        }

        prefs.edit().apply {
            putString("user_name", name)
            putString("user_email", email)
            putString("profile_photo_uri", photoUri)
            apply()
        }
    }

    fun updateThemeColor(style: String) {
        _themeColor.value = style
        prefs.edit().putString("theme_color_style", style).apply()
    }

    fun updateTabColor(style: String) {
        _tabColor.value = style
        prefs.edit().putString("tab_color_style", style).apply()
    }

    fun updateThemeMode(mode: String) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode_preference", mode).apply()
    }

    enum class StatusFilter { ALL, PENDING, COMPLETED }
    enum class PriorityFilter { ALL, LOW, MEDIUM, HIGH }

    fun register() {
        val username = authUsernameInput.value.trim()
        val password = authPasswordInput.value
        val confirm = authConfirmPasswordInput.value

        if (username.isBlank() || password.isBlank() || confirm.isBlank()) {
            _authErrorMessage.value = "Por favor, completa todos los campos de registro."
            return
        }

        if (username.length < 3) {
            _authErrorMessage.value = "El nombre de usuario debe tener al menos 3 caracteres."
            return
        }

        if (password.length < 6) {
            _authErrorMessage.value = "La contraseña debe tener al menos 6 caracteres por seguridad."
            return
        }

        if (password != confirm) {
            _authErrorMessage.value = "Las contraseñas no coinciden."
            return
        }

        viewModelScope.launch {
            _authLoading.value = true
            _authErrorMessage.value = null

            // Delay to simulate verification and prevent quick creation scripts
            kotlinx.coroutines.delay(600)

            val existing = repository.getUserByUsername(username)
            if (existing != null) {
                _authErrorMessage.value = "Este nombre de usuario ya está registrado."
                _authLoading.value = false
                return@launch
            }

            // Secure salt generation and SHA-256 password hashing
            val salt = UserEntity.generateSalt()
            val hash = UserEntity.hashPassword(password, salt)

            val newUser = UserEntity(
                username = username,
                passwordHash = hash,
                salt = salt
            )

            repository.registerUser(newUser)

            // Switch to login view with success
            isRegisterMode.value = false
            authPasswordInput.value = ""
            authConfirmPasswordInput.value = ""
            _authErrorMessage.value = "Registro de usuario exitoso. Ya puedes iniciar sesión con tus credenciales."
            _authLoading.value = false
        }
    }

    fun login() {
        val username = authUsernameInput.value.trim()
        val password = authPasswordInput.value

        if (username.isBlank() || password.isBlank()) {
            _authErrorMessage.value = "Por favor, completa todos los campos."
            return
        }

        if (_cooldownTimeLeft.value > 0) {
            _authErrorMessage.value = "Sistema bloqueado provisionalmente. Espera ${_cooldownTimeLeft.value} segundos."
            return
        }

        viewModelScope.launch {
            _authLoading.value = true
            _authErrorMessage.value = null
            
            // Prevention of automated speed brute-force attacks via network simulation penalty
            kotlinx.coroutines.delay(800)

            val user = repository.getUserByUsername(username)
            if (user == null) {
                _authErrorMessage.value = "Usuario o contraseña inválidos."
                _authLoading.value = false
                return@launch
            }

            // Lockout security check
            val now = System.currentTimeMillis()
            if (user.lockedUntil > now) {
                val remainingSeconds = ((user.lockedUntil - now) / 1000).toInt().coerceAtLeast(1)
                _cooldownTimeLeft.value = remainingSeconds
                _authErrorMessage.value = "Cuenta bloqueada por seguridad. Inténtalo de nuevo en $remainingSeconds segundos."
                _authLoading.value = false
                return@launch
            }

            // Verify safe password hashed comparison
            val hashedInput = UserEntity.hashPassword(password, user.salt)
            if (hashedInput == user.passwordHash) {
                // SUCCESS! Login sequence
                val updatedUser = user.copy(failedAttempts = 0, lockedUntil = 0)
                repository.updateUser(updatedUser)

                // Set session
                _loggedInUser.value = username
                prefs.edit().putString("logged_in_username", username).apply()

                // Sync standard profile settings for individual context
                _userName.value = username
                _userEmail.value = "${username.lowercase()}@app.com"
                prefs.edit().apply {
                    putString("user_name", username)
                    putString("user_email", "${username.lowercase()}@app.com")
                    apply()
                }

                // Clear input strings
                authUsernameInput.value = ""
                authPasswordInput.value = ""
                _authErrorMessage.value = null
            } else {
                // FAIL! Compute brute force penalties
                val newFailedCount = user.failedAttempts + 1
                var newLockUntil = 0L
                var errorMessage = "Usuario o contraseña inválidos."

                if (newFailedCount >= 5) {
                    newLockUntil = System.currentTimeMillis() + 30000 // 30 seconds block
                    _cooldownTimeLeft.value = 30
                    errorMessage = "Máximos intentos fallidos alcanzados (5/5). Cuenta bloqueada temporalmente por 30s para evitar hackeo."
                } else {
                    errorMessage = "Usuario o contraseña inválidos. (Intento fallido: $newFailedCount/5)"
                }

                val updatedUser = user.copy(
                    failedAttempts = newFailedCount,
                    lockedUntil = newLockUntil
                )
                repository.updateUser(updatedUser)

                _authErrorMessage.value = errorMessage
            }
            _authLoading.value = false
        }
    }

    fun logout() {
        _loggedInUser.value = null
        prefs.edit().remove("logged_in_username").apply()
        _userName.value = "Sesión Cerrada"
        _userEmail.value = ""
        resetForm()
    }

    fun loginWithGoogle(email: String, displayName: String?) {
        viewModelScope.launch {
            _authLoading.value = true
            _authErrorMessage.value = null
            
            // Simula un retardo corto para indicar conexión con Google
            kotlinx.coroutines.delay(1000)
            
            val user = repository.getUserByUsername(email)
            if (user == null) {
                // No existe en la base de datos local -> Hacer Registro
                val salt = UserEntity.generateSalt()
                val hash = UserEntity.hashPassword("GOOGLE_OAUTH_ACCOUNT", salt)
                
                val newUser = UserEntity(
                    username = email,
                    passwordHash = hash,
                    salt = salt
                )
                repository.registerUser(newUser)
                Log.d("MainViewModel", "Google User registered: $email")
            }
            
            // Hacer Login -> Configurar sesión
            _loggedInUser.value = email
            prefs.edit().putString("logged_in_username", email).apply()
            
            // Configurar perfil
            val name = displayName ?: email.substringBefore("@")
            _userName.value = name
            _userEmail.value = email
            prefs.edit().apply {
                putString("user_name", name)
                putString("user_email", email)
                apply()
            }
            
            // Borrar entradas del formulario normal
            authUsernameInput.value = ""
            authPasswordInput.value = ""
            _authErrorMessage.value = null
            _authLoading.value = false
        }
    }

    // Form Input updates
    fun updateTitle(value: String) { titleInput.value = value }
    fun updateDesc(value: String) { descInput.value = value }
    fun updatePriority(value: Int) { priorityInput.value = value }
    fun updateStatusInput(value: String) { statusInput.value = value }
    fun updateStartDate(value: Long?) { startDateInput.value = value }
    fun updateDueDate(value: Long?) { dueDateInput.value = value }
    fun updateReminderTime(value: Long?) { reminderTimeInput.value = value }
    fun updateTeamId(value: Int?) { teamIdInput.value = value }
    fun updateSharedEmail(value: String) { sharedEmailInput.value = value }

    fun setStatusFilter(filter: StatusFilter) { _statusFilter.value = filter }
    fun setPriorityFilter(filter: PriorityFilter) { _priorityFilter.value = filter }

    fun showAddForm() {
        resetForm()
        _isFormVisible.value = true
    }

    fun showEditForm(task: TaskEntity) {
        editingTaskId.value = task.id
        titleInput.value = task.title
        descInput.value = task.description
        priorityInput.value = task.priority
        statusInput.value = task.status
        startDateInput.value = task.startDate
        dueDateInput.value = task.dueDate
        reminderTimeInput.value = task.reminderTime
        teamIdInput.value = task.teamId
        sharedEmailInput.value = task.sharedWithEmail ?: ""
        _isFormVisible.value = true
    }

    fun hideForm() {
        _isFormVisible.value = false
        resetForm()
    }

    private fun resetForm() {
        editingTaskId.value = null
        titleInput.value = ""
        descInput.value = ""
        priorityInput.value = 2
        statusInput.value = "PENDIENTE"
        startDateInput.value = null
        dueDateInput.value = null
        reminderTimeInput.value = null
        teamIdInput.value = null
        sharedEmailInput.value = ""
    }

    // Task Actions
    fun saveTask() {
        val title = titleInput.value.trim()
        if (title.isBlank()) return

        val desc = descInput.value.trim()
        val priority = priorityInput.value
        val status = statusInput.value
        val isCompleted = (status == "COMPLETADO")
        val start = startDateInput.value
        val due = dueDateInput.value
        val reminder = reminderTimeInput.value
        val currentEditId = editingTaskId.value
        val teamId = teamIdInput.value
        val sharedEmail = sharedEmailInput.value.trim().ifBlank { null }

        viewModelScope.launch {
            if (currentEditId != null) {
                // Fetch current state
                val existing = repository.getTaskById(currentEditId)
                if (existing != null) {
                    val updated = existing.copy(
                        title = title,
                        description = desc,
                        priority = priority,
                        completed = isCompleted,
                        status = status,
                        startDate = start,
                        dueDate = due,
                        reminderTime = reminder,
                        teamId = teamId,
                        sharedWithEmail = sharedEmail,
                        synced = false
                    )
                    repository.updateTask(updated)

                    // Reschedule reminder if update occurred
                    ReminderScheduler.cancelReminder(app, currentEditId)
                    if (reminder != null && !updated.completed) {
                        ReminderScheduler.scheduleReminder(app, currentEditId, reminder, title, desc.ifBlank { "Alerta de tarea" })
                    }
                }
            } else {
                // New Task Insert
                val newTask = TaskEntity(
                    title = title,
                    description = desc,
                    priority = priority,
                    completed = isCompleted,
                    status = status,
                    startDate = start,
                    dueDate = due,
                    reminderTime = reminder,
                    teamId = teamId,
                    sharedWithEmail = sharedEmail,
                    username = loggedInUser.value ?: ""
                )
                val newId = repository.insertTask(newTask).toInt()

                // Schedule notification if trigger configured
                if (reminder != null) {
                    ReminderScheduler.scheduleReminder(app, newId, reminder, title, desc.ifBlank { "Alerta de tarea" })
                }
            }
            hideForm()
        }
    }

    fun updateTaskStatus(task: TaskEntity, newStatus: String) {
        viewModelScope.launch {
            repository.updateTaskStatus(task, newStatus)
            if (newStatus == "COMPLETADO") {
                ReminderScheduler.cancelReminder(app, task.id)
            } else {
                task.reminderTime?.let { reminderTime ->
                    if (reminderTime > System.currentTimeMillis()) {
                        ReminderScheduler.scheduleReminder(app, task.id, reminderTime, task.title, task.description)
                    }
                }
            }
        }
    }

    fun toggleTaskCompletion(task: TaskEntity) {
        viewModelScope.launch {
            repository.toggleCompleted(task)
            // Cancel reminders for completed tasks
            if (!task.completed) {
                ReminderScheduler.cancelReminder(app, task.id)
            } else {
                // Re-schedule reminder if task was un-completed and had future reminder time
                task.reminderTime?.let { reminderTime ->
                    if (reminderTime > System.currentTimeMillis()) {
                        ReminderScheduler.scheduleReminder(app, task.id, reminderTime, task.title, task.description)
                    }
                }
            }
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            // Cancel alarm
            ReminderScheduler.cancelReminder(app, task.id)
            // Perform soft delete (flags it for later cloud deletion)
            repository.softDeleteTask(task.id)
        }
    }

    fun setShowSyncLogs(show: Boolean) {
        _showSyncLogs.value = show
    }

    // Cloud Synchronization Execution
    fun syncWithCloud() {
        if (_isSyncing.value) return

        viewModelScope.launch {
            _isSyncing.value = true
            _syncSummary.value = null
            
            try {
                val result = repository.syncWithCloud(serverUrl.value.trim(), userToken.value.trim(), loggedInUser.value ?: "")
                _syncSummary.value = result
                _showSyncLogs.value = true
            } catch (e: Exception) {
                Log.e("MainViewModel", "Sync unexpected failure", e)
                _syncSummary.value = SyncSummary(
                    0, 0, 0, 
                    listOf("Error inesperado en sincronización: ${e.message}"), 
                    isSimulated = false, 
                    success = false
                )
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun dismissSyncSummary() {
        _syncSummary.value = null
    }

    // --- Teams flows & actions integration ---
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val teams: StateFlow<List<TeamEntity>> = _loggedInUser.flatMapLatest { username ->
        if (username == null) flowOf(emptyList()) else repository.getTeamsFlow(username)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTeamId = MutableStateFlow<Int?>(null)
    val selectedTeamId = _selectedTeamId.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedTeamMembers: StateFlow<List<TeamMemberEntity>> = _selectedTeamId.flatMapLatest { teamId ->
        if (teamId == null) flowOf(emptyList()) else repository.getTeamMembersFlow(teamId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedTeamTasks: StateFlow<List<TaskEntity>> = _selectedTeamId.flatMapLatest { teamId ->
        if (teamId == null) flowOf(emptyList()) else repository.getTeamTasksFlow(teamId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createTeam(name: String) {
        val username = _loggedInUser.value ?: return
        if (name.isBlank()) return
        viewModelScope.launch {
            val teamId = repository.insertTeam(TeamEntity(name = name, creatorUsername = username))
            // Auto add creator to team members
            repository.insertTeamMember(TeamMemberEntity(teamId = teamId.toInt(), email = "jimc.inf@gmail.com", status = "JOINED"))
            _selectedTeamId.value = teamId.toInt()
        }
    }

    fun deleteTeam(teamId: Int) {
        viewModelScope.launch {
            repository.deleteTeam(teamId)
            if (_selectedTeamId.value == teamId) {
                _selectedTeamId.value = null
            }
        }
    }

    fun selectTeam(teamId: Int?) {
        _selectedTeamId.value = teamId
    }

    fun inviteMember(email: String) {
        val teamId = _selectedTeamId.value ?: return
        if (email.isBlank()) return
        viewModelScope.launch {
            repository.insertTeamMember(TeamMemberEntity(teamId = teamId, email = email, status = "INVITED"))
        }
    }

    fun deleteMember(memberId: Int) {
        viewModelScope.launch {
            repository.deleteTeamMember(memberId)
        }
    }

    fun shareTaskWithTeam(task: TaskEntity, teamId: Int) {
        viewModelScope.launch {
            repository.updateTask(task.copy(teamId = teamId))
        }
    }

    enum class AppTab { TASKS, CALENDAR, STATISTICS, TEAMS }

    private val _currentTab = MutableStateFlow(AppTab.TASKS)
    val currentTab = _currentTab.asStateFlow()

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
    }

    class Factory(
        private val app: Application,
        private val repository: TaskRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(app, repository) as T
            }
            throw IllegalArgumentException("ViewModel class desconocido")
        }
    }
}
