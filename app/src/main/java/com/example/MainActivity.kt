package com.example

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.MainViewModel
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Room database initialization
        val database = AppDatabase.getDatabase(this)
        val repository = TaskRepository(database.taskDao, database.userDao, database.teamDao)

        setContent {
            val viewModel: MainViewModel = viewModel(
                factory = MainViewModel.Factory(application, repository)
            )

            val themeColor by viewModel.themeColor.collectAsStateWithLifecycle()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val isDarkTheme = remember(themeMode, systemDark) {
                when (themeMode) {
                    "DARK" -> true
                    "LIGHT" -> false
                    else -> systemDark
                }
            }

            MyApplicationTheme(themeStyle = themeColor, darkTheme = isDarkTheme) {
                // Request Permissions on Startup
                val context = LocalContext.current
                var hasNotificationPermission by remember {
                    mutableStateOf(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                        } else true
                    )
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    hasNotificationPermission = isGranted
                    if (!isGranted) {
                        Toast.makeText(
                            context,
                            "Recordatorios deshabilitados. Por favor otorga permisos de notificación.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    
                    // Clear leftover empty soft deleted tasks upon database startup to save space
                    try {
                        repository.clearDeletedTasks()
                    } catch (e: Exception) {
                        // Suppressed
                    }
                }

                val loggedInUser by viewModel.loggedInUser.collectAsStateWithLifecycle()
                if (loggedInUser == null) {
                    com.example.ui.AuthScreen(viewModel = viewModel)
                } else {
                    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

                    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
                    val tabColorPref by viewModel.tabColor.collectAsStateWithLifecycle()
                    val isFormOpen by viewModel.isFormVisible.collectAsStateWithLifecycle()
                    val isProfileOpen by viewModel.isProfileVisible.collectAsStateWithLifecycle()

                    val resolvedTabColor = when (tabColorPref.uppercase()) {
                        "PURPLE" -> androidx.compose.ui.graphics.Color(0xFF6750A4)
                        "EMERALD" -> androidx.compose.ui.graphics.Color(0xFF006C47)
                        "RUBY" -> androidx.compose.ui.graphics.Color(0xFFBA1A1A)
                        "AMBER" -> androidx.compose.ui.graphics.Color(0xFF725C0C)
                        "OCEAN" -> androidx.compose.ui.graphics.Color(0xFF2196F3)
                        else -> MaterialTheme.colorScheme.primary
                    }

                    if (isLandscape) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .navigationBarsPadding()
                        ) {
                            if (!isFormOpen && !isProfileOpen) {
                                NavigationRail(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.fillMaxHeight(),
                                    header = {
                                        if (currentTab == MainViewModel.AppTab.TASKS) {
                                            FloatingActionButton(
                                                onClick = { viewModel.showAddForm() },
                                                shape = RoundedCornerShape(14.dp),
                                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier
                                                    .testTag("add_task_fab")
                                                    .padding(vertical = 12.dp)
                                                    .size(44.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "Añadir Tarea Nueva",
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                ) {
                                    Spacer(modifier = Modifier.weight(1f))
                                    NavigationRailItem(
                                        selected = currentTab == MainViewModel.AppTab.TASKS,
                                        onClick = { viewModel.selectTab(MainViewModel.AppTab.TASKS) },
                                        label = { Text("Tareas", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                        icon = { Icon(Icons.Default.List, contentDescription = "Tareas") },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = resolvedTabColor,
                                            selectedTextColor = resolvedTabColor,
                                            indicatorColor = resolvedTabColor.copy(alpha = 0.15f),
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    NavigationRailItem(
                                        selected = currentTab == MainViewModel.AppTab.CALENDAR,
                                        onClick = { viewModel.selectTab(MainViewModel.AppTab.CALENDAR) },
                                        label = { Text("Calendario", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                        icon = { Icon(Icons.Default.DateRange, contentDescription = "Calendario") },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = resolvedTabColor,
                                            selectedTextColor = resolvedTabColor,
                                            indicatorColor = resolvedTabColor.copy(alpha = 0.15f),
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    NavigationRailItem(
                                        selected = currentTab == MainViewModel.AppTab.STATISTICS,
                                        onClick = { viewModel.selectTab(MainViewModel.AppTab.STATISTICS) },
                                        label = { Text("Rendimiento", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                        icon = { Icon(Icons.Default.TrendingUp, contentDescription = "Rendimiento") },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = resolvedTabColor,
                                            selectedTextColor = resolvedTabColor,
                                            indicatorColor = resolvedTabColor.copy(alpha = 0.15f),
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    NavigationRailItem(
                                        selected = currentTab == MainViewModel.AppTab.TEAMS,
                                        onClick = { viewModel.selectTab(MainViewModel.AppTab.TEAMS) },
                                        label = { Text("Equipos", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                        icon = { Icon(Icons.Default.Groups, contentDescription = "Equipos") },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = resolvedTabColor,
                                            selectedTextColor = resolvedTabColor,
                                            indicatorColor = resolvedTabColor.copy(alpha = 0.15f),
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .widthIn(max = 1000.dp)
                                ) {
                                    when (currentTab) {
                                        MainViewModel.AppTab.TASKS -> {
                                            MainScreenContent(viewModel = viewModel)
                                        }
                                        MainViewModel.AppTab.CALENDAR -> {
                                            com.example.ui.CalendarScreen(
                                                viewModel = viewModel,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        MainViewModel.AppTab.STATISTICS -> {
                                            com.example.ui.StatsScreen(
                                                viewModel = viewModel,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        MainViewModel.AppTab.TEAMS -> {
                                            com.example.ui.TeamsScreen(
                                                viewModel = viewModel,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }

                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = isFormOpen,
                                        enter = slideInVertically(
                                            initialOffsetY = { it },
                                            animationSpec = tween(durationMillis = 350)
                                        ) + fadeIn(),
                                        exit = slideOutVertically(
                                            targetOffsetY = { it },
                                            animationSpec = tween(durationMillis = 300)
                                        ) + fadeOut(),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.5f))
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Spacer(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clickable { viewModel.hideForm() }
                                            )
                                            TaskFormPanel(viewModel = viewModel)
                                        }
                                    }

                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = isProfileOpen,
                                        enter = slideInVertically(
                                            initialOffsetY = { it },
                                            animationSpec = tween(durationMillis = 350)
                                        ) + fadeIn(),
                                        exit = slideOutVertically(
                                            targetOffsetY = { it },
                                            animationSpec = tween(durationMillis = 300)
                                        ) + fadeOut(),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.5f))
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Spacer(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clickable { viewModel.setProfileVisible(false) }
                                            )
                                            com.example.ui.ProfileCustomizationPanel(viewModel = viewModel)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Scaffold(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .navigationBarsPadding(),
                            bottomBar = {
                                if (!isFormOpen && !isProfileOpen) {
                                    NavigationBar(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        tonalElevation = 8.dp,
                                        modifier = Modifier.height(72.dp)
                                    ) {
                                        NavigationBarItem(
                                            selected = currentTab == MainViewModel.AppTab.TASKS,
                                            onClick = { viewModel.selectTab(MainViewModel.AppTab.TASKS) },
                                            label = { Text("Mis Tareas", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                            icon = {
                                                Icon(
                                                    imageVector = Icons.Default.List,
                                                    contentDescription = "Tareas"
                                                )
                                            },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = resolvedTabColor,
                                                selectedTextColor = resolvedTabColor,
                                                indicatorColor = resolvedTabColor.copy(alpha = 0.15f),
                                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        )
                                        NavigationBarItem(
                                            selected = currentTab == MainViewModel.AppTab.CALENDAR,
                                            onClick = { viewModel.selectTab(MainViewModel.AppTab.CALENDAR) },
                                            label = { Text("Calendario", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                            icon = {
                                                Icon(
                                                    imageVector = Icons.Default.DateRange,
                                                    contentDescription = "Calendario"
                                                )
                                            },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = resolvedTabColor,
                                                selectedTextColor = resolvedTabColor,
                                                indicatorColor = resolvedTabColor.copy(alpha = 0.15f),
                                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        )
                                        NavigationBarItem(
                                            selected = currentTab == MainViewModel.AppTab.STATISTICS,
                                            onClick = { viewModel.selectTab(MainViewModel.AppTab.STATISTICS) },
                                            label = { Text("Rendimiento", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                            icon = {
                                                Icon(
                                                    imageVector = Icons.Default.TrendingUp,
                                                    contentDescription = "Rendimiento"
                                                )
                                            },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = resolvedTabColor,
                                                selectedTextColor = resolvedTabColor,
                                                indicatorColor = resolvedTabColor.copy(alpha = 0.15f),
                                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        )
                                        NavigationBarItem(
                                            selected = currentTab == MainViewModel.AppTab.TEAMS,
                                            onClick = { viewModel.selectTab(MainViewModel.AppTab.TEAMS) },
                                            label = { Text("Equipos", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                            icon = {
                                                Icon(
                                                    imageVector = Icons.Default.Groups,
                                                    contentDescription = "Equipos"
                                                )
                                            },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = resolvedTabColor,
                                                selectedTextColor = resolvedTabColor,
                                                indicatorColor = resolvedTabColor.copy(alpha = 0.15f),
                                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        )
                                    }
                                }
                            },
                            floatingActionButton = {
                                val isFormOpen by viewModel.isFormVisible.collectAsStateWithLifecycle()
                                val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
                                
                                if (!isFormOpen && currentTab == MainViewModel.AppTab.TASKS) {
                                    FloatingActionButton(
                                        onClick = { viewModel.showAddForm() },
                                        shape = RoundedCornerShape(20.dp),
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier
                                            .testTag("add_task_fab")
                                            .padding(bottom = 16.dp, end = 16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Añadir Tarea Nueva",
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            },
                            contentWindowInsets = WindowInsets.safeDrawing
                        ) { innerPadding ->
                            val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
                            val isFormOpen by viewModel.isFormVisible.collectAsStateWithLifecycle()
                            val isProfileOpen by viewModel.isProfileVisible.collectAsStateWithLifecycle()

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .widthIn(max = 820.dp)
                                ) {
                                    when (currentTab) {
                                        MainViewModel.AppTab.TASKS -> {
                                            MainScreenContent(viewModel = viewModel)
                                        }
                                        MainViewModel.AppTab.CALENDAR -> {
                                            com.example.ui.CalendarScreen(
                                                viewModel = viewModel,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        MainViewModel.AppTab.STATISTICS -> {
                                            com.example.ui.StatsScreen(
                                                viewModel = viewModel,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        MainViewModel.AppTab.TEAMS -> {
                                            com.example.ui.TeamsScreen(
                                                viewModel = viewModel,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }

                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = isFormOpen,
                                        enter = slideInVertically(
                                            initialOffsetY = { it },
                                            animationSpec = tween(durationMillis = 350)
                                        ) + fadeIn(),
                                        exit = slideOutVertically(
                                            targetOffsetY = { it },
                                            animationSpec = tween(durationMillis = 300)
                                        ) + fadeOut(),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.5f))
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.BottomCenter
                                        ) {
                                            Spacer(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clickable { viewModel.hideForm() }
                                            )
                                            TaskFormPanel(viewModel = viewModel)
                                        }
                                    }

                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = isProfileOpen,
                                        enter = slideInVertically(
                                            initialOffsetY = { it },
                                            animationSpec = tween(durationMillis = 350)
                                        ) + fadeIn(),
                                        exit = slideOutVertically(
                                            targetOffsetY = { it },
                                            animationSpec = tween(durationMillis = 300)
                                        ) + fadeOut(),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.5f))
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.BottomCenter
                                        ) {
                                            Spacer(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clickable { viewModel.setProfileVisible(false) }
                                            )
                                            com.example.ui.ProfileCustomizationPanel(viewModel = viewModel)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } }
            }
        }
    }

@Composable
fun MainScreenContent(viewModel: MainViewModel) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val statusFilter by viewModel.statusFilter.collectAsStateWithLifecycle()
    val priorityFilter by viewModel.priorityFilter.collectAsStateWithLifecycle()
    val isFormOpen by viewModel.isFormVisible.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncSummary by viewModel.syncSummary.collectAsStateWithLifecycle()
    val showLogs by viewModel.showSyncLogs.collectAsStateWithLifecycle()

    var showSyncSettings by remember { mutableStateOf(false) }

    val userToken by viewModel.userToken.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val profilePhotoUri by viewModel.profilePhotoUri.collectAsStateWithLifecycle()
    val isProfileOpen by viewModel.isProfileVisible.collectAsStateWithLifecycle()

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 12.dp)
            ) {
                HeaderSection(
                    tasks = tasks,
                    isSyncing = isSyncing,
                    userToken = userToken,
                    userName = userName,
                    profilePhotoUri = profilePhotoUri,
                    onShowSyncSettings = { showSyncSettings = !showSyncSettings },
                    onProfileClick = { viewModel.setProfileVisible(true) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                BentoStatsSection(tasks = tasks)

                val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()

                if (currentTab == MainViewModel.AppTab.TASKS) {
                    AnimatedVisibility(
                        visible = showSyncSettings || isSyncing || syncSummary != null,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        SyncManagementPanel(
                            viewModel = viewModel,
                            showSettings = showSyncSettings,
                            onCloseSettings = { showSyncSettings = false }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    FilterPanel(
                        statusFilter = statusFilter,
                        priorityFilter = priorityFilter,
                        onStatusFilterChanged = { viewModel.setStatusFilter(it) },
                        onPriorityFilterChanged = { viewModel.setPriorityFilter(it) }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight()
                    .padding(vertical = 12.dp)
            ) {
                if (tasks.isEmpty()) {
                    EmptyTasksState(
                        statusFilter = statusFilter,
                        priorityFilter = priorityFilter
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(
                            items = tasks,
                            key = { it.id }
                        ) { task ->
                            TaskListItem(
                                task = task,
                                onToggleComplete = { viewModel.toggleTaskCompletion(task) },
                                onEdit = { viewModel.showEditForm(task) },
                                onDelete = { viewModel.deleteTask(task) },
                                onStatusChanged = { newStatus -> viewModel.updateTaskStatus(task, newStatus) }
                            )
                        }
                    }
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            HeaderSection(
                tasks = tasks,
                isSyncing = isSyncing,
                userToken = userToken,
                userName = userName,
                profilePhotoUri = profilePhotoUri,
                onShowSyncSettings = { showSyncSettings = !showSyncSettings },
                onProfileClick = { viewModel.setProfileVisible(true) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            BentoStatsSection(tasks = tasks)

            Spacer(modifier = Modifier.height(12.dp))

            val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()

            if (currentTab == MainViewModel.AppTab.TASKS) {
                AnimatedVisibility(
                    visible = showSyncSettings || isSyncing || syncSummary != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    SyncManagementPanel(
                        viewModel = viewModel,
                        showSettings = showSyncSettings,
                        onCloseSettings = { showSyncSettings = false }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                FilterPanel(
                    statusFilter = statusFilter,
                    priorityFilter = priorityFilter,
                    onStatusFilterChanged = { viewModel.setStatusFilter(it) },
                    onPriorityFilterChanged = { viewModel.setPriorityFilter(it) }
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (tasks.isEmpty()) {
                    EmptyTasksState(
                        statusFilter = statusFilter,
                        priorityFilter = priorityFilter
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(
                            items = tasks,
                            key = { it.id }
                        ) { task ->
                            TaskListItem(
                                task = task,
                                onToggleComplete = { viewModel.toggleTaskCompletion(task) },
                                onEdit = { viewModel.showEditForm(task) },
                                onDelete = { viewModel.deleteTask(task) },
                                onStatusChanged = { newStatus -> viewModel.updateTaskStatus(task, newStatus) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HeaderSection(
    tasks: List<TaskEntity>,
    isSyncing: Boolean,
    userToken: String,
    userName: String,
    profilePhotoUri: String,
    onShowSyncSettings: () -> Unit,
    onProfileClick: () -> Unit
) {
    val totalTasks = tasks.size
    val completedTasks = tasks.count { it.completed }
    val progress = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f
    val progressPercent = (progress * 100).toInt()

    val currentTimeStr = remember {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        sdf.format(Date())
    }

    // High priority or first pending task to recommend as "Focus"
    val pendingTasksList = tasks.filter { !it.completed }
    val focusTask = pendingTasksList.maxByOrNull { it.priority }
    val focusTitle = focusTask?.title ?: "Día Completado ✨"
    val focusDesc = focusTask?.description?.ifBlank { "Revisando tareas pendientes." }
        ?: if (totalTasks > 0) "¡Buen trabajo! Todas tus tareas están hechas." else "Crea una tarea para empezar."

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        // Bento-Style Top Minimalist Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Circular user avatar badge with click feedback
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape)
                        .clickable { onProfileClick() },
                    contentAlignment = Alignment.Center
                ) {
                    if (profilePhotoUri.isNotBlank()) {
                        if (profilePhotoUri.length <= 2) {
                            Text(
                                text = profilePhotoUri,
                                fontSize = 20.sp,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            // Coil AsyncImage
                            coil.compose.AsyncImage(
                                model = profilePhotoUri,
                                contentDescription = "Foto de perfil",
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        val avatarLetter = if (userName.isNotBlank()) userName.take(1).uppercase() else "J"
                        Text(
                            text = avatarLetter,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column {
                    Text(
                        text = "Mis Tareas",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        lineHeight = 20.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Green pulsing status indicator
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(color = Color(0xFF4CAF50), shape = CircleShape)
                        )
                        Text(
                            text = "En la Nube",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Simple Cloud Sync round button
            IconButton(
                onClick = onShowSyncSettings,
                modifier = Modifier
                    .testTag("sync_settings_button")
                    .size(48.dp)
                    .background(
                        color = if (isSyncing) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    )
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = "Configuración de Nube",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Primary Bento Focus Block
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = "ENFOQUE PRINCIPAL",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            letterSpacing = 0.5.sp
                        )
                    }
                    Text(
                        text = currentTimeStr,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = focusTitle,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    lineHeight = 26.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = focusDesc,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Bento-Style Progress bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinearProgressIndicator(
                        progress = { progress },
                        trackColor = MaterialTheme.colorScheme.outlineVariant,
                        color = MaterialTheme.colorScheme.primary,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "$progressPercent%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun BentoStatsSection(tasks: List<TaskEntity>) {
    val totalTasks = tasks.size
    val completedTasks = tasks.count { it.completed }
    val pendingTasks = totalTasks - completedTasks

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Compact Card 1: Pending Tasks
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier
                .weight(1f)
                .height(60.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Work,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = String.format("%02d", pendingTasks),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        lineHeight = 16.sp
                    )
                    Text(
                        text = "Pendientes",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 12.sp
                    )
                }
            }
        }

        // Compact Card 2: Completed Tasks
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier
                .weight(1f)
                .height(60.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = String.format("%02d", completedTasks),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 16.sp
                    )
                    Text(
                        text = "Completas",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun FilterPanel(
    statusFilter: MainViewModel.StatusFilter,
    priorityFilter: MainViewModel.PriorityFilter,
    onStatusFilterChanged: (MainViewModel.StatusFilter) -> Unit,
    onPriorityFilterChanged: (MainViewModel.PriorityFilter) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Completion Filters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MainViewModel.StatusFilter.values().forEach { filter ->
                val selected = statusFilter == filter
                val label = when (filter) {
                    MainViewModel.StatusFilter.ALL -> "Todas"
                    MainViewModel.StatusFilter.PENDING -> "Pendientes"
                    MainViewModel.StatusFilter.COMPLETED -> "Completas"
                }

                FilterChip(
                    selected = selected,
                    onClick = { onStatusFilterChanged(filter) },
                    label = { Text(label, fontSize = 13.sp) },
                    leadingIcon = if (selected) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("filter_status_${filter.name.lowercase()}")
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Priorities Filter row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "Prioridad:",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(end = 4.dp)
            )

            MainViewModel.PriorityFilter.values().forEach { filter ->
                val selected = priorityFilter == filter
                val color = when (filter) {
                    MainViewModel.PriorityFilter.ALL -> MaterialTheme.colorScheme.outline
                    MainViewModel.PriorityFilter.HIGH -> Color(0xFFE53935)
                    MainViewModel.PriorityFilter.MEDIUM -> Color(0xFFFFB300)
                    MainViewModel.PriorityFilter.LOW -> Color(0xFF1E88E5)
                }

                val label = when (filter) {
                    MainViewModel.PriorityFilter.ALL -> "Todas"
                    MainViewModel.PriorityFilter.LOW -> "Baja"
                    MainViewModel.PriorityFilter.MEDIUM -> "Media"
                    MainViewModel.PriorityFilter.HIGH -> "Alta"
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) color.copy(alpha = 0.15f) else Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (selected) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier
                        .clickable { onPriorityFilterChanged(filter) }
                        .testTag("filter_priority_${filter.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (filter == MainViewModel.PriorityFilter.ALL) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(color, CircleShape)
                            )
                        } else {
                            val priorityInt = when (filter) {
                                MainViewModel.PriorityFilter.HIGH -> 3
                                MainViewModel.PriorityFilter.MEDIUM -> 2
                                else -> 1
                            }
                            com.example.ui.PriorityPattern(
                                priority = priorityInt,
                                color = color,
                                size = 10.dp
                            )
                        }
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SyncManagementPanel(
    viewModel: MainViewModel,
    showSettings: Boolean,
    onCloseSettings: () -> Unit
) {
    val serverUrl by viewModel.serverUrl.collectAsStateWithLifecycle()
    val userToken by viewModel.userToken.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncSummary by viewModel.syncSummary.collectAsStateWithLifecycle()
    val showLogs by viewModel.showSyncLogs.collectAsStateWithLifecycle()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sync_panel")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudQueue,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Sincronización en la Nube",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                IconButton(
                    onClick = onCloseSettings,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar Panel",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (showSettings) {
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { viewModel.serverUrl.value = it },
                    label = { Text("Base URL del Servidor", fontSize = 12.sp) },
                    placeholder = { Text("Ej. http://10.0.2.2:8080/api") },
                    singleLine = true,
                    supportingText = {
                        Text(
                            "Escribe 'Simulado' para emular la sincronización en disco con resolución de conflictos de marca temporal local.",
                            fontSize = 10.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sync_server_input")
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = userToken,
                    onValueChange = { viewModel.userToken.value = it },
                    label = { Text("Usuario / Token Único", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sync_user_input")
                )
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Servidor: $serverUrl  •  Usuario: $userToken",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = { viewModel.syncWithCloud() },
                enabled = !isSyncing,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sync_now_button")
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sincronizando...", fontSize = 13.sp)
                } else {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sincronizar Tareas Ahora", fontSize = 13.sp)
                }
            }

            // Sync Summary Notification Banner
            syncSummary?.let { summary ->
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val icon = if (summary.success) Icons.Default.CloudDone else Icons.Default.WifiOff
                    val tint = if (summary.success) Color(0xFF2E7D32) else Color(0xFFC62828)
                    
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (summary.success) {
                                if (summary.isSimulated) "Sincronización Simulada Exitosa" else "Sincronización Exitosa"
                            } else "Error de Sincronización",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = tint
                        )

                        Text(
                            text = "📤 Subidas: ${summary.uploadedRemotely}  •  📥 Descargadas: ${summary.updatedLocally}  •  🗑️ Borradas: ${summary.deletedLocally}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    TextButton(
                        onClick = { viewModel.setShowSyncLogs(!showLogs) },
                        modifier = Modifier.testTag("toggle_logs_button")
                    ) {
                        Text(if (showLogs) "Ocultar Logs" else "Ver Logs", fontSize = 11.sp)
                    }
                }

                // Scrollable Sync Logs Console Block
                if (showLogs) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 130.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E1E1E))
                            .padding(8.dp)
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(summary.logs) { log ->
                                Text(
                                    text = log,
                                    color = if (log.contains("⚠️")) Color(0xFFFFB300) else if (log.contains("❌") || log.contains("Error")) Color(0xFFEF5350) else Color(0xFF81C784),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskListItem(
    task: TaskEntity,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStatusChanged: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val formattedDueDate = if (task.startDate != null && task.dueDate != null) {
        val sdfStart = SimpleDateFormat("dd MMM", Locale("es", "ES"))
        val sdfEnd = SimpleDateFormat("dd MMM, yyyy", Locale("es", "ES"))
        "${sdfStart.format(Date(task.startDate))} - ${sdfEnd.format(Date(task.dueDate))}"
    } else if (task.dueDate != null) {
        val sdf = SimpleDateFormat("dd MMM, yyyy", Locale("es", "ES"))
        sdf.format(Date(task.dueDate))
    } else if (task.startDate != null) {
        val sdf = SimpleDateFormat("dd MMM, yyyy", Locale("es", "ES"))
        "Desde: " + sdf.format(Date(task.startDate))
    } else {
        null
    }

    val formattedReminder = task.reminderTime?.let {
        val sdf = SimpleDateFormat("HH:mm (dd MMM)", Locale.getDefault())
        "Recordatorio: " + sdf.format(Date(it))
    }

    // Determine priority design elements
    val priorityColor = when (task.priority) {
        3 -> Color(0xFFE53935) // High
        2 -> Color(0xFFFFB300) // Medium
        else -> Color(0xFF1E88E5) // Low
    }

    val priorityText = when (task.priority) {
        3 -> "Alta"
        2 -> "Media"
        else -> "Baja"
    }

    val cardBackground = MaterialTheme.colorScheme.surface

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBackground
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (task.completed) 0.dp else 2.dp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_item_${task.id}")
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Elegant watermark priority pattern dynamically adjusted for dark/light contrast
            val isDarkThemeActive = MaterialTheme.colorScheme.surface.let { 
                (it.red * 0.299f + it.green * 0.587f + it.blue * 0.114f) < 0.5f 
            }
            val watermarkAlpha = when (task.priority) {
                3 -> if (isDarkThemeActive) 0.08f else 0.16f
                2 -> if (isDarkThemeActive) 0.09f else 0.20f
                else -> if (isDarkThemeActive) 0.08f else 0.16f
            }
            com.example.ui.PriorityPattern(
                priority = task.priority,
                color = priorityColor.copy(alpha = watermarkAlpha),
                size = 115.dp,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 20.dp, y = 0.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
            // Task completed Checkbox with ripple
            IconButton(
                onClick = { onToggleComplete() },
                modifier = Modifier
                    .testTag("task_checkbox_${task.id}")
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = if (task.completed) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = "Completar Tarea",
                    tint = if (task.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Task info details
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 4.dp)
            ) {
                // Title and Priority dot row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    com.example.ui.PriorityPattern(
                        priority = task.priority,
                        color = priorityColor,
                        size = 12.dp
                    )

                    Text(
                        text = task.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (task.completed) TextDecoration.LineThrough else null,
                        color = if (task.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )

                    // Sync Indicator Status Icon
                    if (!task.synced) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "No guardado en nube",
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                if (task.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = task.description,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Date & Reminders details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Interactive Status Pill
                    var showStatusMenu by remember { mutableStateOf(false) }
                    
                    Box {
                        val statusLabel = when(task.status) {
                            "EN_PROCESO" -> "En Proceso"
                            "BLOQUEADO" -> "Bloqueado"
                            "COMPLETADO" -> "Completada"
                            else -> "Pendiente"
                        }
                        val statusBg = when(task.status) {
                            "EN_PROCESO" -> if (isDarkThemeActive) Color(0xFF0D47A1).copy(alpha = 0.4f) else Color(0xFFE3F2FD)
                            "BLOQUEADO" -> if (isDarkThemeActive) Color(0xFFB71C1C).copy(alpha = 0.4f) else Color(0xFFFFEBEE)
                            "COMPLETADO" -> if (isDarkThemeActive) Color(0xFF1B5E20).copy(alpha = 0.4f) else Color(0xFFE8F5E9)
                            else -> if (isDarkThemeActive) Color(0xFF37474F).copy(alpha = 0.4f) else Color(0xFFF5F5F5)
                        }
                        val statusText = when(task.status) {
                            "EN_PROCESO" -> if (isDarkThemeActive) Color(0xFF90CAF9) else Color(0xFF1565C0)
                            "BLOQUEADO" -> if (isDarkThemeActive) Color(0xFFEF9A9A) else Color(0xFFC62828)
                            "COMPLETADO" -> if (isDarkThemeActive) Color(0xFFA5D6A7) else Color(0xFF2E7D32)
                            else -> if (isDarkThemeActive) Color(0xFFCFD8DC) else Color(0xFF616161)
                        }
                        val statusDot = when(task.status) {
                            "EN_PROCESO" -> if (isDarkThemeActive) Color(0xFF42A5F5) else Color(0xFF1976D2)
                            "BLOQUEADO" -> if (isDarkThemeActive) Color(0xFFE57373) else Color(0xFFD32F2F)
                            "COMPLETADO" -> if (isDarkThemeActive) Color(0xFF81C784) else Color(0xFF4CAF50)
                            else -> if (isDarkThemeActive) Color(0xFFB0BEC5) else Color(0xFF9E9E9E)
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = statusBg,
                            modifier = Modifier
                                .height(26.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showStatusMenu = true }
                                .testTag("task_status_pill_${task.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(statusDot, CircleShape)
                                )
                                Text(
                                    text = statusLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusText
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Cambiar Estado",
                                    tint = statusText,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showStatusMenu,
                            onDismissRequest = { showStatusMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(Modifier.size(8.dp).background(if (isDarkThemeActive) Color(0xFFB0BEC5) else Color(0xFF9E9E9E), CircleShape))
                                        Text("Pendiente", fontWeight = FontWeight.Medium)
                                    }
                                },
                                onClick = {
                                    showStatusMenu = false
                                    onStatusChanged?.invoke("PENDIENTE")
                                }
                            )
                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(Modifier.size(8.dp).background(if (isDarkThemeActive) Color(0xFF42A5F5) else Color(0xFF1976D2), CircleShape))
                                        Text("En Proceso", fontWeight = FontWeight.Medium)
                                    }
                                },
                                onClick = {
                                    showStatusMenu = false
                                    onStatusChanged?.invoke("EN_PROCESO")
                                }
                            )
                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(Modifier.size(8.dp).background(if (isDarkThemeActive) Color(0xFFE57373) else Color(0xFFD32F2F), CircleShape))
                                        Text("Bloqueado", fontWeight = FontWeight.Medium)
                                    }
                                },
                                onClick = {
                                    showStatusMenu = false
                                    onStatusChanged?.invoke("BLOQUEADO")
                                }
                            )
                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(Modifier.size(8.dp).background(if (isDarkThemeActive) Color(0xFF81C784) else Color(0xFF4CAF50), CircleShape))
                                        Text("Completada", fontWeight = FontWeight.Medium)
                                    }
                                },
                                onClick = {
                                    showStatusMenu = false
                                    onStatusChanged?.invoke("COMPLETADO")
                                }
                            )
                        }
                    }

                    // Due Date Tag
                    formattedDueDate?.let {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    text = it,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Alert / Notification Alarm Indicator Tag
                    formattedReminder?.let {
                        val isPast = task.reminderTime < System.currentTimeMillis()
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isPast || task.completed) MaterialTheme.colorScheme.surfaceVariant
                                    else Color(0xFFFFECEB),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (task.completed) Icons.Default.NotificationsOff
                                                  else Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = if (isPast || task.completed) MaterialTheme.colorScheme.outline
                                           else Color(0xFFD32F2F),
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    text = it,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPast || task.completed) MaterialTheme.colorScheme.outline
                                           else Color(0xFFD32F2F)
                                )
                            }
                        }
                    }
                }
            }

            // Quick Actions: Edit + Delete icon buttons
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onEdit() },
                    modifier = Modifier
                        .testTag("edit_task_${task.id}")
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar Tarea",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = { onDelete() },
                    modifier = Modifier
                        .testTag("delete_task_${task.id}")
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Eliminar Tarea",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
}

@Composable
fun TaskFormPanel(viewModel: MainViewModel) {
    val title by viewModel.titleInput.collectAsStateWithLifecycle()
    val desc by viewModel.descInput.collectAsStateWithLifecycle()
    val priority by viewModel.priorityInput.collectAsStateWithLifecycle()
    val statusForm by viewModel.statusInput.collectAsStateWithLifecycle()
    val startDate by viewModel.startDateInput.collectAsStateWithLifecycle()
    val dueDate by viewModel.dueDateInput.collectAsStateWithLifecycle()
    val reminderTime by viewModel.reminderTimeInput.collectAsStateWithLifecycle()
    val editId by viewModel.editingTaskId.collectAsStateWithLifecycle()
    val teams by viewModel.teams.collectAsStateWithLifecycle()
    val selectedTeamIdForm by viewModel.teamIdInput.collectAsStateWithLifecycle()
    val sharedEmailForm by viewModel.sharedEmailInput.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val isEditing = editId != null

    Card(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
        modifier = Modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { /* Consume clips inside to prevent slide-down on internal clicks */ }
            .testTag("task_form_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
                .imePadding() // Adjusts itself when the virtual keyboard rises
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditing) "Editar Tarea" else "Crear Nueva Tarea",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = { viewModel.hideForm() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Task Name Inputs
            OutlinedTextField(
                value = title,
                onValueChange = { viewModel.updateTitle(it) },
                label = { Text("Escribe el nombre de la tarea") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("form_title_input"),
                colors = OutlinedTextFieldDefaults.colors()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Task Description Details
            OutlinedTextField(
                value = desc,
                onValueChange = { viewModel.updateDesc(it) },
                label = { Text("Detalles opcionales") },
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("form_desc_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Priority Selection Segmented buttons style
            Text(
                "Nivel de Prioridad:",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(1, 2, 3).forEach { pLevel ->
                    val isSelected = priority == pLevel
                    val color = when (pLevel) {
                        3 -> Color(0xFFE53935)
                        2 -> Color(0xFFFFB300)
                        else -> Color(0xFF1E88E5)
                    }
                    val textLabel = when (pLevel) {
                        3 -> "Alta"
                        2 -> "Media"
                        else -> "Baja"
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.5.dp,
                            color = if (isSelected) color else Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clickable { viewModel.updatePriority(pLevel) }
                            .testTag("priority_chip_$pLevel"),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            com.example.ui.PriorityPattern(
                                priority = pLevel,
                                color = color,
                                size = 12.dp
                            )
                            Text(
                                text = textLabel,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Task Status Selection
            Text(
                "Estado de la Tarea:",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Determine theme context inside the form panel
                val isDarkThemeActive = MaterialTheme.colorScheme.surface.let { 
                    (it.red * 0.299f + it.green * 0.587f + it.blue * 0.114f) < 0.5f 
                }

                val row1 = listOf("PENDIENTE" to "Pendiente", "EN_PROCESO" to "En Proceso")
                val row2 = listOf("BLOQUEADO" to "Bloqueado", "COMPLETADO" to "Completada")

                listOf(row1, row2).forEach { optionRow ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        optionRow.forEach { (statusKey, statusLabel) ->
                            val isSelected = statusForm == statusKey
                            val color = when (statusKey) {
                                "EN_PROCESO" -> Color(0xFF1976D2)
                                "BLOQUEADO" -> Color(0xFFD32F2F)
                                "COMPLETADO" -> Color(0xFF4CAF50)
                                else -> if (isDarkThemeActive) Color(0xFFCFD8DC) else Color(0xFF6F6F6F)
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.5.dp,
                                    color = if (isSelected) color else Color.Transparent
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clickable { viewModel.updateStatusInput(statusKey) }
                                    .testTag("status_chip_$statusKey"),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(color, CircleShape)
                                    )
                                    Text(
                                        text = statusLabel,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Fecha de Vencimiento y Alertas:",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Start Date row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val calendar = Calendar.getInstance()
                                startDate?.let { calendar.timeInMillis = it }
                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        calendar.set(Calendar.YEAR, year)
                                        calendar.set(Calendar.MONTH, month)
                                        calendar.set(Calendar.DAY_OF_MONTH, day)
                                        viewModel.updateStartDate(calendar.timeInMillis)
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                            .testTag("form_start_date_picker_button")
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = if (startDate != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Fecha de Inicio (Rango)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = startDate?.let {
                                        SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy", Locale("es", "ES")).format(Date(it))
                                    } ?: "No configurada (Opcional)",
                                    fontSize = 12.sp,
                                    color = if (startDate != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (startDate != null) {
                            IconButton(
                                onClick = { viewModel.updateStartDate(null) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Limpiar fecha inicio",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )

                    // Due Date row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val calendar = Calendar.getInstance()
                                dueDate?.let { calendar.timeInMillis = it }
                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        calendar.set(Calendar.YEAR, year)
                                        calendar.set(Calendar.MONTH, month)
                                        calendar.set(Calendar.DAY_OF_MONTH, day)
                                        viewModel.updateDueDate(calendar.timeInMillis)
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                            .testTag("form_date_picker_button")
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = if (dueDate != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Fecha de Vencimiento",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = dueDate?.let {
                                        SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy", Locale("es", "ES")).format(Date(it))
                                    } ?: "No configurada (Sin vencimiento)",
                                    fontSize = 12.sp,
                                    color = if (dueDate != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (dueDate != null) {
                            IconButton(
                                onClick = { viewModel.updateDueDate(null) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Limpiar fecha",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )

                    // Reminder row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val calendar = Calendar.getInstance()
                                reminderTime?.let { calendar.timeInMillis = it }
                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        calendar.set(Calendar.YEAR, year)
                                        calendar.set(Calendar.MONTH, month)
                                        calendar.set(Calendar.DAY_OF_MONTH, day)
                                        
                                        TimePickerDialog(
                                            context,
                                            { _, hour, minute ->
                                                calendar.set(Calendar.HOUR_OF_DAY, hour)
                                                calendar.set(Calendar.MINUTE, minute)
                                                calendar.set(Calendar.SECOND, 0)
                                                viewModel.updateReminderTime(calendar.timeInMillis)
                                            },
                                            calendar.get(Calendar.HOUR_OF_DAY),
                                            calendar.get(Calendar.MINUTE),
                                            true
                                        ).show()
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                            .testTag("form_reminder_picker_button")
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = if (reminderTime != null) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Alerta / Recordatorio",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = reminderTime?.let {
                                        SimpleDateFormat("EEEE, dd 'de' MMMM 'a las' HH:mm", Locale("es", "ES")).format(Date(it))
                                    } ?: "No configurado (Sin alarma)",
                                    fontSize = 12.sp,
                                    color = if (reminderTime != null) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (reminderTime != null) {
                            IconButton(
                                onClick = { viewModel.updateReminderTime(null) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Limpiar recordatorio",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Compartir / Vincular Tarea (Opcional):",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Team Selector Dropdown / Scrollable Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Vincular a Equipo:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (teams.isEmpty()) {
                    Text(
                        text = "No has creado ningún equipo aún. Ve a la pestaña 'Equipos' para crear uno.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // "Ninguno" Option
                        Surface(
                            selected = selectedTeamIdForm == null,
                            onClick = { viewModel.updateTeamId(null) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedTeamIdForm == null) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = if (selectedTeamIdForm == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Ninguno", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        teams.forEach { team ->
                            val isSelected = selectedTeamIdForm == team.id
                            Surface(
                                selected = isSelected,
                                onClick = { viewModel.updateTeamId(team.id) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(team.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                // Shared with Direct Contact Email
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AlternateEmail,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Compartir por Correo Electrónico:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = sharedEmailForm,
                        onValueChange = { viewModel.updateSharedEmail(it) },
                        placeholder = { Text("colaborador@correo.com", fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("form_shared_email_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.secondary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Submit and Cancel Drawer buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.hideForm() },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("form_cancel_button")
                ) {
                    Text("Cancelar")
                }

                Button(
                    onClick = { viewModel.saveTask() },
                    enabled = title.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.5f)
                        .height(48.dp)
                        .testTag("form_submit_button")
                ) {
                    Text(if (isEditing) "Actualizar" else "Crear Tarea")
                }
            }
        }
    }
}

@Composable
fun EmptyTasksState(
    statusFilter: MainViewModel.StatusFilter,
    priorityFilter: MainViewModel.PriorityFilter
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.widthIn(max = 380.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.TaskAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                modifier = Modifier.size(82.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            val titleText = when {
                statusFilter == MainViewModel.StatusFilter.COMPLETED -> "No hay tareas completadas"
                statusFilter == MainViewModel.StatusFilter.PENDING -> "¡Libre de tareas pendientes!"
                else -> "Organiza tu día hoy"
            }

            val descText = when {
                priorityFilter != MainViewModel.PriorityFilter.ALL -> "No se encontraron resultados con la prioridad seleccionada."
                statusFilter == MainViewModel.StatusFilter.COMPLETED -> "Las tareas que completes se organizarán en esta sección para tu historial."
                statusFilter == MainViewModel.StatusFilter.PENDING -> "Has despachado todas tus tareas. Añade una nueva para programar recordatorios."
                else -> "Crea una lista de control, configura notificaciones de alarma y sincroniza en un servidor en la nube seguro."
            }

            Text(
                text = titleText,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = descText,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}
