package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.TaskEntity
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalendarScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()

    // Calendar state tracking
    val currentDate = remember { Calendar.getInstance() }
    var displayedCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    
    // Day currently selected by the user to view lists
    var selectedDate by remember {
        mutableStateOf(
            Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
            }
        )
    }

    // Calculated fields based on displayedCalendar
    val year = displayedCalendar.get(Calendar.YEAR)
    val month = displayedCalendar.get(Calendar.MONTH)
    
    val monthName = remember(month) {
        displayedCalendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("es", "ES"))
            ?.replaceFirstChar { it.uppercase() } ?: ""
    }

    // Days calculation
    val startOfMonthCalendar = remember(year, month) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }
    
    val maxDays = startOfMonthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = startOfMonthCalendar.get(Calendar.DAY_OF_WEEK) // 1: Sunday, 2: Monday...
    
    // Shift: number of empty leading days in the grid
    // For Monday as start of week: Monday is 0, Sunday is 6
    val emptyLeadingDays = (firstDayOfWeek - Calendar.MONDAY + 7) % 7

    // Tasks specifically filtered for the selected day
    val selectedDayTasks = remember(tasks, selectedDate) {
        tasks.filter { task ->
            isTaskActiveOnDay(task, selectedDate)
        }
    }

    // Month navigation helpers
    fun changeMonth(amount: Int) {
        val next = displayedCalendar.clone() as Calendar
        next.add(Calendar.MONTH, amount)
        displayedCalendar = next
    }

    @Composable
    fun CalendarGridCardContent(modifier: Modifier = Modifier) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
            modifier = modifier
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header Month Navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { changeMonth(-1) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Mes Anterior",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = "$monthName $year",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    IconButton(
                        onClick = { changeMonth(1) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Mes Siguiente",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Days of week short labels row
                val weekDays = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    weekDays.forEach { dayLabel ->
                        Text(
                            text = dayLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Days Grid Layout composed cleanly of Rows with equal weight spacing
                val totalGridSlots = emptyLeadingDays + maxDays
                val rowsCount = (totalGridSlots + 6) / 7

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (rowIdx in 0 until rowsCount) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            for (colIdx in 0..6) {
                                val slotNumber = rowIdx * 7 + colIdx
                                val dayNumber = slotNumber - emptyLeadingDays + 1

                                if (dayNumber in 1..maxDays) {
                                    // Build representing calendar day date object
                                    val dayCal = Calendar.getInstance().apply {
                                        set(Calendar.YEAR, year)
                                        set(Calendar.MONTH, month)
                                        set(Calendar.DAY_OF_MONTH, dayNumber)
                                    }

                                    val isSelected = isSameCalendarDay(dayCal, selectedDate)
                                    val isToday = isSameCalendarDay(dayCal, currentDate)

                                    // Check tasks for this cell
                                    val dayTasks = tasks.filter { isTaskActiveOnDay(it, dayCal) }
                                    
                                    // Separate into multi-day vs single-day tasks
                                    val (multiDayTasks, singleDayTasks) = dayTasks.partition { task ->
                                        task.startDate != null && task.dueDate != null && 
                                        getMidnightTimeInMillis(task.dueDate) > getMidnightTimeInMillis(task.startDate)
                                    }

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(58.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                color = when {
                                                    isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                    isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .border(
                                                width = when {
                                                    isSelected -> 1.5.dp
                                                    isToday -> 1.dp
                                                    else -> 0.dp
                                                },
                                                color = when {
                                                    isSelected -> MaterialTheme.colorScheme.primary
                                                    isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                                    else -> Color.Transparent
                                                },
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                selectedDate = dayCal
                                            }
                                            .padding(vertical = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Day number text
                                        Text(
                                            text = "$dayNumber",
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                                            color = when {
                                                isSelected -> MaterialTheme.colorScheme.primary
                                                isToday -> MaterialTheme.colorScheme.primary
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )

                                        // Task indicators stack (Bars and dots)
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(1.5.dp)
                                        ) {
                                            // Handle multi-day task continuity bars
                                            if (multiDayTasks.isNotEmpty()) {
                                                multiDayTasks.take(2).forEach { mTask ->
                                                    val isStart = mTask.startDate?.let { isSameCalendarDay(Calendar.getInstance().apply { timeInMillis = it }, dayCal) } ?: true
                                                    val isEnd = mTask.dueDate?.let { isSameCalendarDay(Calendar.getInstance().apply { timeInMillis = it }, dayCal) } ?: true
                                                    
                                                    val mColor = when {
                                                        mTask.completed -> Color(0xFF4CAF50).copy(alpha = 0.6f)
                                                        mTask.priority == 3 -> Color(0xFFE53935)
                                                        mTask.priority == 2 -> Color(0xFFFFB300)
                                                        else -> Color(0xFF2196F3)
                                                    }
                                                    
                                                    val startShapeRadius = if (isStart) 3.dp else 0.dp
                                                    val endShapeRadius = if (isEnd) 3.dp else 0.dp

                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(3.dp)
                                                            .padding(
                                                                start = if (isStart) 2.dp else 0.dp,
                                                                end = if (isEnd) 2.dp else 0.dp
                                                            )
                                                            .background(
                                                                color = mColor,
                                                                shape = RoundedCornerShape(
                                                                    topStart = startShapeRadius,
                                                                    bottomStart = startShapeRadius,
                                                                    topEnd = endShapeRadius,
                                                                    bottomEnd = endShapeRadius
                                                                )
                                                            )
                                                    )
                                                }
                                            }

                                            // Draw discrete multiple dots for single-day tasks
                                            if (singleDayTasks.isNotEmpty()) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(1.5.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(top = 1.dp)
                                                ) {
                                                    // Show up to 4 dots
                                                    singleDayTasks.take(4).forEach { sTask ->
                                                        val dotColor = when {
                                                            sTask.completed -> Color(0xFF4CAF50)
                                                            sTask.priority == 3 -> Color(0xFFE53935)
                                                            sTask.priority == 2 -> Color(0xFFFFB300)
                                                            else -> Color(0xFF2196F3)
                                                        }
                                                        Box(
                                                            modifier = Modifier
                                                                .size(4.dp)
                                                                .background(dotColor, CircleShape)
                                                        )
                                                    }
                                                    if (singleDayTasks.size > 4) {
                                                        Text(
                                                            text = "+",
                                                            fontSize = 7.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                            
                                            // Fallback spacer to keep clean layout height if no tasks
                                            if (multiDayTasks.isEmpty() && singleDayTasks.isEmpty()) {
                                                Spacer(modifier = Modifier.height(3.dp))
                                            }
                                        }
                                    }
                                } else {
                                    // Empty slot placeholder space maintaining weight grid balance
                                    Spacer(modifier = Modifier.weight(1f).height(58.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun SelectedDateBannerContent() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val formattedSelected = remember(selectedDate) {
                val formatter = SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "ES"))
                formatter.format(selectedDate.time).replaceFirstChar { it.uppercase() }
            }

            Text(
                text = formattedSelected,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )

            // Button to quickly schedule a task on the active highlighted date!
            Button(
                onClick = {
                    viewModel.updateStartDate(selectedDate.timeInMillis)
                    viewModel.updateDueDate(selectedDate.timeInMillis)
                    viewModel.showAddForm()
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier
                    .height(32.dp)
                    .testTag("quick_calendar_add_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Añadir", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    @Composable
    fun SelectedDayTasksListContent(modifier: Modifier = Modifier) {
        if (selectedDayTasks.isEmpty()) {
            Box(
                modifier = modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Sin compromisos para esta fecha",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Puedes presionar el botón 'Añadir' de arriba para agendar pendientes rápidamente aquí.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, start = 20.dp, end = 20.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 70.dp, top = 4.dp)
            ) {
                items(
                    items = selectedDayTasks,
                    key = { it.id }
                ) { task ->
                    CalendarTaskItem(
                        task = task,
                        onToggleComplete = { viewModel.toggleTaskCompletion(task) },
                        onEdit = { viewModel.showEditForm(task) },
                        onDelete = { viewModel.deleteTask(task) }
                    )
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp)
    ) {
        // Header Row with Title and Close X Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Calendario",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Organiza tu tiempo y visualiza rangos de tareas",
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
                    contentDescription = "Cerrar Calendario",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

        if (isLandscape) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(0.55f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                ) {
                    CalendarGridCardContent(
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(0.45f)
                        .fillMaxHeight()
                ) {
                    SelectedDateBannerContent()

                    Spacer(modifier = Modifier.height(4.dp))

                    SelectedDayTasksListContent(
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            CalendarGridCardContent(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            SelectedDateBannerContent()

            Spacer(modifier = Modifier.height(4.dp))

            SelectedDayTasksListContent(
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun CalendarTaskItem(
    task: TaskEntity,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val priorityColor = when (task.priority) {
        3 -> Color(0xFFE53935) // High: Red
        2 -> Color(0xFFFFB300) // Medium: Yellow/Amber
        else -> Color(0xFF1E88E5) // Low: Blue
    }

    val cardBackground = MaterialTheme.colorScheme.surface

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBackground
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (task.completed) 0.dp else 2.dp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("calendar_task_item_${task.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Task priority color indicator bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        when (task.priority) {
                            3 -> Color(0xFFE53935) // High: Red
                            2 -> Color(0xFFFFB300) // Medium: Yellow/Amber
                            else -> Color(0xFF4CAF50) // Low: Green
                        }
                    )
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Checkbox
            Checkbox(
                checked = task.completed,
                onCheckedChange = { onToggleComplete() },
                modifier = Modifier.testTag("calendar_task_check_${task.id}")
            )

            Spacer(modifier = Modifier.width(4.dp))

            // Title & Priority Label
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val priorityColor = when (task.priority) {
                        3 -> Color(0xFFE53935) // High: Red
                        2 -> Color(0xFFFFB300) // Medium: Yellow/Amber
                        else -> Color(0xFF1E88E5) // Low: Blue
                    }
                    PriorityPattern(
                        priority = task.priority,
                        color = priorityColor,
                        size = 12.dp
                    )
                    Text(
                        text = task.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (task.completed) MaterialTheme.colorScheme.onSurfaceVariant 
                        else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Quick actions
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private fun isTaskActiveOnDay(task: TaskEntity, targetCalendar: Calendar): Boolean {
    val start = task.startDate
    val end = task.dueDate

    val targetTime = getMidnightTimeInMillis(targetCalendar)

    return when {
        start != null && end != null -> {
            val startMidnight = getMidnightTimeInMillis(start)
            val endMidnight = getMidnightTimeInMillis(end)
            targetTime in startMidnight..endMidnight
        }
        start != null -> {
            getMidnightTimeInMillis(start) == targetTime
        }
        end != null -> {
            getMidnightTimeInMillis(end) == targetTime
        }
        else -> false
    }
}

private fun getMidnightTimeInMillis(timeInMillis: Long): Long {
    val cal = Calendar.getInstance().apply {
        this.timeInMillis = timeInMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

private fun getMidnightTimeInMillis(cal: Calendar): Long {
    val temp = cal.clone() as Calendar
    temp.set(Calendar.HOUR_OF_DAY, 0)
    temp.set(Calendar.MINUTE, 0)
    temp.set(Calendar.SECOND, 0)
    temp.set(Calendar.MILLISECOND, 0)
    return temp.timeInMillis
}

private fun isSameCalendarDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
            cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
}
