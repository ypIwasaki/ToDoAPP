package com.example.todoapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.todoapp.data.TaskWithSubtasks
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

@Composable
fun CalendarScreen(
    tasks: List<TaskWithSubtasks>,
    onEditTask: (Long) -> Unit,
    onTaskChecked: (TaskWithSubtasks, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    var displayedYear by rememberSaveable { mutableIntStateOf(today.year) }
    var displayedMonthNumber by rememberSaveable { mutableIntStateOf(today.monthValue) }
    var selectedEpochDay by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    val displayedMonth = YearMonth.of(displayedYear, displayedMonthNumber)
    val selectedDate = LocalDate.ofEpochDay(selectedEpochDay)
    val tasksByDate = remember(tasks) {
        tasks
            .filter { it.task.deadlineEpochMillis != null }
            .groupBy { item ->
                Instant.ofEpochMilli(requireNotNull(item.task.deadlineEpochMillis))
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            }
    }
    val selectedTasks = tasksByDate[selectedDate].orEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
    ) {
        CalendarHeader(
            month = displayedMonth,
            onPrevious = {
                val previous = displayedMonth.minusMonths(1)
                displayedYear = previous.year
                displayedMonthNumber = previous.monthValue
                selectedEpochDay = previous.atDay(1).toEpochDay()
            },
            onNext = {
                val next = displayedMonth.plusMonths(1)
                displayedYear = next.year
                displayedMonthNumber = next.monthValue
                selectedEpochDay = next.atDay(1).toEpochDay()
            },
            onToday = {
                displayedYear = today.year
                displayedMonthNumber = today.monthValue
                selectedEpochDay = today.toEpochDay()
            },
        )

        WeekdayHeader()
        CalendarGrid(
            month = displayedMonth,
            selectedDate = selectedDate,
            today = today,
            taskCounts = tasksByDate.mapValues { it.value.size },
            onDateSelected = { selectedEpochDay = it.toEpochDay() },
        )

        Text(
            text = formatFullDate(selectedDate),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 8.dp),
        )

        if (selectedTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "この日のタスクはありません",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(selectedTasks, key = { it.task.id }) { item ->
                    CalendarTaskRow(
                        item = item,
                        onClick = { onEditTask(item.task.id) },
                        onChecked = { onTaskChecked(item, it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarHeader(
    month: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Outlined.ChevronLeft, contentDescription = "前の月")
        }
        Text(
            text = formatMonth(month),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onToday) { Text("今日") }
        IconButton(onClick = onNext) {
            Icon(Icons.Outlined.ChevronRight, contentDescription = "次の月")
        }
    }
}

@Composable
private fun WeekdayHeader() {
    val weekdays = listOf("月", "火", "水", "木", "金", "土", "日")
    Row(modifier = Modifier.fillMaxWidth()) {
        weekdays.forEachIndexed { index, day ->
            Text(
                text = day,
                style = MaterialTheme.typography.labelLarge,
                color = when (index) {
                    5 -> Color(0xFF426B9B)
                    6 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 6.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    month: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    taskCounts: Map<LocalDate, Int>,
    onDateSelected: (LocalDate) -> Unit,
) {
    val cells = remember(month) { calendarMonthCells(month) }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        cells.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                week.forEach { date ->
                    CalendarDayCell(
                        date = date,
                        selected = date == selectedDate,
                        isToday = date == today,
                        taskCount = date?.let { taskCounts[it] } ?: 0,
                        onClick = { date?.let(onDateSelected) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate?,
    selected: Boolean,
    isToday: Boolean,
    taskCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val baseModifier = modifier
        .aspectRatio(1.08f)
        .clip(RoundedCornerShape(12.dp))
        .then(
            when {
                selected -> Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                else -> Modifier
            },
        )
        .then(
            if (isToday && !selected) {
                Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
            } else Modifier,
        )
        .clickable(enabled = date != null, onClick = onClick)

    Box(modifier = baseModifier, contentAlignment = Alignment.Center) {
        if (date != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selected || isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else MaterialTheme.colorScheme.onSurface,
                )
                if (taskCount > 0) {
                    Box(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(18.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (taskCount > 9) "9+" else taskCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                } else {
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun CalendarTaskRow(
    item: TaskWithSubtasks,
    onClick: () -> Unit,
    onChecked: (Boolean) -> Unit,
) {
    val task = item.task
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            } else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = task.isCompleted, onCheckedChange = onChecked)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                )
                task.deadlineEpochMillis?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = formatDeadline(it).substringAfter(" "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
            }
            if (item.subtasks.isNotEmpty()) {
                Text(
                    text = "${item.subtasks.count { it.isCompleted }}/${item.subtasks.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 12.dp),
                )
            }
        }
    }
}
