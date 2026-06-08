package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.TaskEntity
import java.util.*

@Composable
fun StatsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()

    // Dynamic calculations
    val totalCount = tasks.size
    val completedCount = tasks.count { it.completed }
    val pendingCount = tasks.count { !it.completed }

    // Overdue tasks calculation
    val now = System.currentTimeMillis()
    val overdueCount = tasks.count { task ->
        !task.completed && task.dueDate != null && task.dueDate < now && !isToday(task.dueDate)
    }

    // High priority pending
    val urgentCount = tasks.count { !it.completed && it.priority == 3 }

    // "Tareas en proceso": Logical classification of pending tasks being active (Medium or High priority, or scheduled for today)
    val inProgressCount = tasks.count { task ->
        !task.completed && (task.priority >= 2 || (task.dueDate != null && isToday(task.dueDate)))
    }

    // "Tareas por hacer": Pending tasks with simpler low-priority or unassigned due dates
    val toDoCount = pendingCount - inProgressCount

    // Monthly evaluation calculations
    val calendar = Calendar.getInstance()
    val currentMonth = calendar.get(Calendar.MONTH)
    val currentYear = calendar.get(Calendar.YEAR)

    val tasksThisMonth = tasks.filter { task ->
        val itemCal = Calendar.getInstance()
        if (task.dueDate != null) {
            itemCal.timeInMillis = task.dueDate
            itemCal.get(Calendar.MONTH) == currentMonth && itemCal.get(Calendar.YEAR) == currentYear
        } else {
            itemCal.timeInMillis = task.lastUpdated
            itemCal.get(Calendar.MONTH) == currentMonth && itemCal.get(Calendar.YEAR) == currentYear
        }
    }

    val completedThisMonth = tasksThisMonth.count { it.completed }
    val totalThisMonth = tasksThisMonth.size

    // Productivity Improvement Index (Dynamic metric out of 100)
    val monthlyEfficiency = if (totalThisMonth > 0) {
        (completedThisMonth.toFloat() / totalThisMonth * 100).toInt()
    } else if (totalCount > 0) {
        (completedCount.toFloat() / totalCount * 100).toInt()
    } else {
        0
    }

    // Calculate a historic productivity trend comparison
    val productivityIndex = remember(completedCount, overdueCount, urgentCount, monthlyEfficiency) {
        // Base score starts around monthly efficiency, penalized nicely by overdue and unaddressed urgent tasks
        var score = monthlyEfficiency
        if (totalCount == 0) {
            score = 100 // High starting motivation style
        } else {
            val overduePenalty = overdueCount * 8
            val urgentPenalty = urgentCount * 5
            score = (score - overduePenalty - urgentPenalty).coerceIn(10, 100)
        }
        score
    }

    // Dynamic Assessment message based on evaluation criteria
    val (recommendationTitle, recommendationDesc, moodColor) = remember(productivityIndex, overdueCount, urgentCount) {
        when {
            productivityIndex >= 85 -> Triple(
                "¡Productividad Imparable! 🚀",
                "Estás gestionando tus compromisos con una eficiencia ejemplar este mes. Tu índice de cumplimiento es fabuloso y tienes absoluto control sobre tus prioridades. Sigue así.",
                Color(0xFF4CAF50) // Green
            )
            productivityIndex >= 60 -> Triple(
                "¡Buen Progreso Organizado! 📈",
                "Vas por un excelente camino este mes. Se observa una sólida mejora en tu productividad diaria. Resolver las tareas urgentes a primera hora te ayudará a elevar tu marcador aún más.",
                Color(0xFFFFB300) // Amber
            )
            overdueCount > 3 -> Triple(
                "Alerta de Sobrecarga Temporal ⚠️",
                "Tienes varias tareas acumuladas y vencidas. Te recomendamos ajustar las fechas límite, depurar las de prioridad baja y resolver lo urgente una cosa a la vez para aliviar la carga.",
                Color(0xFFE53935) // Red
            )
            else -> Triple(
                "Impulsa Tu Enfoque 💪",
                "Tu panel de organización está listo. Comenzar completando una tarea moderada hoy encenderá tu racha de finalización y mejorará visiblemente las métricas de tu mes.",
                Color(0xFF1E88E5) // Blue
            )
        }
    }

    @Composable
    fun PerformanceRingContent() {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            ),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Nivel de Efectividad Mensual",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(110.dp)
                ) {
                    CircularProgressIndicator(
                        progress = 1.0f,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        strokeWidth = 10.dp,
                        modifier = Modifier.fillMaxSize()
                    )
                    CircularProgressIndicator(
                        progress = productivityIndex / 100f,
                        color = moodColor,
                        strokeWidth = 10.dp,
                        modifier = Modifier.fillMaxSize()
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$productivityIndex%",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Eficiencia",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = moodColor.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, moodColor.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = recommendationTitle,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = moodColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = recommendationDesc,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun EncouragementFooter() {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 80.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Consejo de hoy: Las tareas de alta prioridad rinden un 30% más en satisfacción mental cuando se resuelven antes del mediodía. ¡Toma la iniciativa!",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    @Composable
    fun MetricsGrid() {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                StatItemCard(
                    title = "Tareas Completadas",
                    value = completedCount.toString(),
                    subtitle = "Finalizadas con éxito",
                    icon = Icons.Default.CheckCircle,
                    iconColor = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )

                StatItemCard(
                    title = "Tareas Por Hacer",
                    value = toDoCount.toString(),
                    subtitle = "Listas para iniciar",
                    icon = Icons.Default.PlaylistAddCheck,
                    iconColor = Color(0xFF1E88E5),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                StatItemCard(
                    title = "Tareas En Proceso",
                    value = inProgressCount.toString(),
                    subtitle = "Enfoque activo hoy",
                    icon = Icons.Default.DirectionsRun,
                    iconColor = Color(0xFFFFB300),
                    modifier = Modifier.weight(1f)
                )

                StatItemCard(
                    title = "Tareas Atrasadas",
                    value = overdueCount.toString(),
                    subtitle = "Vencidas fuera de plazo",
                    icon = Icons.Default.Warning,
                    iconColor = Color(0xFFE53935),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                StatItemCard(
                    title = "Tareas En Urgencia",
                    value = urgentCount.toString(),
                    subtitle = "Máxima prioridad",
                    icon = Icons.Default.PriorityHigh,
                    iconColor = Color(0xFFE53935),
                    modifier = Modifier.weight(1f)
                )

                StatItemCard(
                    title = "Creadas Este Mes",
                    value = totalThisMonth.toString(),
                    subtitle = "Flujo de planificación",
                    icon = Icons.Default.CalendarViewMonth,
                    iconColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp)
            .testTag("stats_screen_root")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Mi Rendimiento",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Métricas inteligentes de productividad",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = { viewModel.selectTab(MainViewModel.AppTab.TASKS) },
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar Rendimiento",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

        if (isLandscape) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    PerformanceRingContent()
                    EncouragementFooter()
                }

                Column(
                    modifier = Modifier.weight(1.2f)
                ) {
                    Text(
                        text = "Métricas de Actividad",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    MetricsGrid()
                }
            }
        } else {
            PerformanceRingContent()

            Text(
                text = "Métricas de Actividad",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            MetricsGrid()

            Spacer(modifier = Modifier.height(24.dp))

            EncouragementFooter()
        }
    }
}

@Composable
fun StatItemCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth()
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 1
            )
        }
    }
}

// Simple Helper to verify if custom long timestamp is representing today's date
private fun isToday(timestamp: Long): Boolean {
    val today = Calendar.getInstance()
    val target = Calendar.getInstance().apply {
        timeInMillis = timestamp
    }
    return today.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
            today.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
}
