package com.example.pomostudy.ui.calendar

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pomostudy.data.local.entity.StudyRecordEntity
import com.example.pomostudy.data.local.entity.TagEntity
import com.example.pomostudy.data.local.entity.TodoEntity
import com.example.pomostudy.di.AppContainer
import com.example.pomostudy.domain.model.RepeatRule
import com.example.pomostudy.ui.components.PriorityBadge
import com.example.pomostudy.ui.components.TagChip
import com.example.pomostudy.ui.theme.toComposeColor
import com.example.pomostudy.ui.todo.TodoEditorDialog
import com.example.pomostudy.util.TimeUtils
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** 月日历视图：周一到周日排布，格子内展示当天待办；点击某天可查看并增删改当天待办。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(container: AppContainer) {
    val vm: CalendarViewModel = viewModel(factory = CalendarViewModel.factory(container))
    val todos by vm.todos.collectAsStateWithLifecycle()
    val records by vm.records.collectAsStateWithLifecycle()
    val tags by vm.tags.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now() }
    val state = rememberCalendarState(
        startMonth = YearMonth.from(today).minusMonths(24),
        endMonth = YearMonth.from(today).plusMonths(24),
        firstVisibleMonth = YearMonth.from(today),
        firstDayOfWeek = DayOfWeek.MONDAY
    )

    val todosByDate = remember(todos) {
        todos.filter { it.dueDate != null }.groupBy { millisToLocalDate(it.dueDate!!) }
    }
    val recordsByDate = remember(records) {
        records.groupBy { millisToLocalDate(it.startTime) }
    }
    val tagMap = remember(tags) { tags.associateBy { it.id } }

    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TodoEntity?>(null) }
    var editorDueDate by remember { mutableStateOf<Long?>(null) }
    var pendingDelete by remember { mutableStateOf<TodoEntity?>(null) }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                scope.launch { state.animateScrollToMonth(state.firstVisibleMonth.yearMonth.minusMonths(1)) }
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "上个月")
            }
            Text(
                text = "${state.firstVisibleMonth.yearMonth.year} 年 ${state.firstVisibleMonth.yearMonth.monthValue} 月",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = {
                scope.launch { state.animateScrollToMonth(state.firstVisibleMonth.yearMonth.plusMonths(1)) }
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "下个月")
            }
        }

        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendDot(color = MaterialTheme.colorScheme.primary, text = "待办事项")
            LegendDot(color = Color(0xFF43A047), text = "已完成番茄记录")
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            daysOfWeek(DayOfWeek.MONDAY).forEach { d ->
                Text(
                    text = weekDayLabel(d),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalCalendar(
            state = state,
            modifier = Modifier.fillMaxSize(),
            dayContent = { day ->
                CalendarDayCell(
                    day = day,
                    isToday = day.date == today,
                    isSelected = day.date == selectedDate,
                    todos = todosByDate[day.date].orEmpty(),
                    records = recordsByDate[day.date].orEmpty(),
                    tagMap = tagMap,
                    onClick = { selectedDate = day.date },
                    onLongClick = { selectedDate = day.date }
                )
            }
        )
    }

    val sheetDate = selectedDate
    if (sheetDate != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { selectedDate = null },
            sheetState = sheetState
        ) {
            DayDetailContent(
                date = sheetDate,
                todos = todosByDate[sheetDate].orEmpty(),
                records = recordsByDate[sheetDate].orEmpty(),
                tagMap = tagMap,
                onAdd = {
                    editing = null
                    editorDueDate = localDateToMillis(sheetDate)
                    showEditor = true
                },
                onEdit = { todo -> editing = todo; editorDueDate = null; showEditor = true },
                onToggle = { todo -> vm.toggle(todo) },
                onDelete = { todo -> pendingDelete = todo }
            )
        }
    }

    if (showEditor) {
        TodoEditorDialog(
            initial = editing,
            defaultDueDate = editorDueDate,
            tags = tags,
            onDismiss = { showEditor = false },
            onSave = { entity ->
                vm.save(entity)
                showEditor = false
            }
        )
    }

    val toDelete = pendingDelete
    if (toDelete != null) {
        val isDaily = toDelete.repeatRule == RepeatRule.DAILY && toDelete.seriesId != null
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(if (isDaily) "删除每日待办" else "删除待办") },
            text = {
                Text(
                    if (isDaily) "「${toDelete.title}」是每日重复待办，请选择删除范围："
                    else "确定删除「${toDelete.title}」吗？"
                )
            },
            confirmButton = {
                if (isDaily) {
                    Row {
                        TextButton(onClick = {
                            vm.deleteInstance(toDelete); pendingDelete = null
                        }) { Text("仅当日") }
                        TextButton(onClick = {
                            toDelete.seriesId?.let { vm.deleteSeries(it) }; pendingDelete = null
                        }) { Text("全部") }
                    }
                } else {
                    TextButton(onClick = {
                        vm.deleteInstance(toDelete); pendingDelete = null
                    }) { Text("删除") }
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalendarDayCell(
    day: CalendarDay,
    isToday: Boolean,
    isSelected: Boolean,
    todos: List<TodoEntity>,
    records: List<StudyRecordEntity>,
    tagMap: Map<Long, TagEntity>,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val isOutDate = day.position != DayPosition.MonthDate
    val bg by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer
            isToday -> MaterialTheme.colorScheme.secondaryContainer
            else -> Color.Transparent
        },
        label = "dayBg"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 90.dp)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(4.dp)
    ) {
        Column {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (isOutDate) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.onSurface
            )
            // 待办文本（最多 3 条，完成划线）
            todos.take(3).forEach { todo ->
                val tagColor = todo.tagId?.let { tagMap[it]?.color?.toComposeColor() }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(4.dp).clip(CircleShape).background(tagColor ?: MaterialTheme.colorScheme.primary))
                    Text(
                        text = todo.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 9.sp,
                        textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else null,
                        color = if (todo.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }
            }
            // 学习记录标识
            if (records.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF43A047)))
                    Text(
                        text = " ${records.size} 条",
                        fontSize = 9.sp,
                        color = Color(0xFF43A047)
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@Composable
private fun DayDetailContent(
    date: LocalDate,
    todos: List<TodoEntity>,
    records: List<StudyRecordEntity>,
    tagMap: Map<Long, TagEntity>,
    onAdd: () -> Unit,
    onEdit: (TodoEntity) -> Unit,
    onToggle: (TodoEntity) -> Unit,
    onDelete: (TodoEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${date.year}年${date.monthValue}月${date.dayOfMonth}日",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = onAdd) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("添加待办")
                }
            }
        }
        if (todos.isEmpty()) {
            item { Text("当日暂无待办", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(todos, key = { it.id }) { todo ->
                val tag = todo.tagId?.let { tagMap[it] }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = todo.isCompleted, onCheckedChange = { onToggle(todo) })
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = todo.title,
                            style = MaterialTheme.typography.bodyMedium,
                            textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else null
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PriorityBadge(todo.priority)
                            if (tag != null) TagChip(tag.name, tag.color)
                        }
                    }
                    IconButton(onClick = { onEdit(todo) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "编辑")
                    }
                    IconButton(onClick = { onDelete(todo) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "删除")
                    }
                }
            }
        }
        item { HorizontalDivider() }
        item {
            Text("当日学习记录（${records.size}）", style = MaterialTheme.typography.titleMedium)
        }
        if (records.isEmpty()) {
            item { Text("无", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(records, key = { it.id }) { r ->
                val tag = r.tagId?.let { tagMap[it] }
                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = TimeUtils.formatDuration(r.durationSeconds),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        if (tag != null) {
                            Spacer(Modifier.size(8.dp))
                            TagChip(tag.name, tag.color)
                        }
                    }
                    Text(
                        text = "${TimeUtils.formatTime(r.startTime)} - ${TimeUtils.formatTime(r.endTime)}" +
                            if (r.note.isNotBlank()) " · ${r.note}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

private fun millisToLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

private fun localDateToMillis(date: LocalDate): Long =
    date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

private fun weekDayLabel(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> "一"
    DayOfWeek.TUESDAY -> "二"
    DayOfWeek.WEDNESDAY -> "三"
    DayOfWeek.THURSDAY -> "四"
    DayOfWeek.FRIDAY -> "五"
    DayOfWeek.SATURDAY -> "六"
    DayOfWeek.SUNDAY -> "日"
}
