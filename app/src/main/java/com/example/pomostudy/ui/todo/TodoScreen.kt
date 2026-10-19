package com.example.pomostudy.ui.todo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pomostudy.data.local.entity.TagEntity
import com.example.pomostudy.data.local.entity.TodoEntity
import com.example.pomostudy.di.AppContainer
import com.example.pomostudy.domain.model.Priority
import com.example.pomostudy.domain.model.RepeatRule
import com.example.pomostudy.ui.components.EmptyHint
import com.example.pomostudy.ui.components.PriorityBadge
import com.example.pomostudy.ui.components.SimpleDropdown
import com.example.pomostudy.ui.components.TagChip
import com.example.pomostudy.util.TimeUtils

/** 待办页：当周日期条 + 折叠筛选 + 列表 + 新增/编辑/删除。 */
@Composable
fun TodoScreen(container: AppContainer) {
    val vm: TodoViewModel = viewModel(factory = TodoViewModel.factory(container))
    val todos by vm.filtered.collectAsStateWithLifecycle()
    val tags by vm.tags.collectAsStateWithLifecycle()
    val filters by vm.filters.collectAsStateWithLifecycle()
    val weekOffset by vm.weekOffset.collectAsStateWithLifecycle()

    val now = remember { System.currentTimeMillis() }
    val todayStart = remember { TimeUtils.startOfDay(now) }
    val weekStart = remember(weekOffset) {
        TimeUtils.startOfWeek(TimeUtils.addDays(now, weekOffset * 7))
    }
    val days = remember(weekStart) { (0..6).map { TimeUtils.startOfDay(TimeUtils.addDays(weekStart, it)) } }

    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TodoEntity?>(null) }
    var editorDueDate by remember { mutableStateOf<Long?>(null) }
    var pendingDelete by remember { mutableStateOf<TodoEntity?>(null) }
    var filtersExpanded by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            WeekStrip(
                days = days,
                selectedDay = filters.selectedDay,
                todayStart = todayStart,
                onSelectDay = { vm.selectDay(it) },
                onShiftWeek = { vm.shiftWeek(it) }
            )

            // 折叠筛选
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { filtersExpanded = !filtersExpanded }) {
                    Text("筛选")
                    Icon(
                        imageVector = if (filtersExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null
                    )
                }
            }
            AnimatedVisibility(
                visible = filtersExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                TodoFilterPanel(
                    filters = filters,
                    tags = tags,
                    onTogglePriority = vm::togglePriorityFilter,
                    onSetTag = vm::setTagFilter,
                    onSetCompletion = vm::setCompletionFilter
                )
            }

            Crossfade(targetState = todos.isEmpty(), label = "todoList") { isEmpty ->
                if (isEmpty) {
                    EmptyHint("这一天没有待办，点击右下角 + 新增")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(todos, key = { it.id }) { todo ->
                            TodoItem(
                                modifier = Modifier.animateItem(),
                                todo = todo,
                                tags = tags,
                                onToggle = { vm.toggle(todo) },
                                onEdit = { editing = todo; editorDueDate = null; showEditor = true },
                                onDelete = { pendingDelete = todo }
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { editing = null; editorDueDate = filters.selectedDay; showEditor = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "新增待办")
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
        if (isDaily) {
            AlertDialog(
                onDismissRequest = { pendingDelete = null },
                title = { Text("删除每日待办") },
                text = { Text("「${toDelete.title}」是每日重复待办，请选择要删除的范围：") },
                confirmButton = {
                    Row {
                        TextButton(onClick = {
                            vm.deleteInstance(toDelete)
                            pendingDelete = null
                        }) { Text("仅当日") }
                        TextButton(onClick = {
                            toDelete.seriesId?.let { vm.deleteSeries(it) }
                            pendingDelete = null
                        }) { Text("全部") }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDelete = null }) { Text("取消") }
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = { pendingDelete = null },
                title = { Text("删除待办") },
                text = { Text("确定删除「${toDelete.title}」吗？此操作不可撤销。") },
                confirmButton = {
                    TextButton(onClick = {
                        vm.deleteInstance(toDelete)
                        pendingDelete = null
                    }) { Text("删除") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDelete = null }) { Text("取消") }
                }
            )
        }
    }
}

/** 当周日期条：左右滑动切换周，点击某天筛选当天待办。 */
@Composable
private fun WeekStrip(
    days: List<Long>,
    selectedDay: Long?,
    todayStart: Long,
    onSelectDay: (Long?) -> Unit,
    onShiftWeek: (Int) -> Unit
) {
    var dragAccum by remember { mutableStateOf(0f) }
    // 记住上一次选中的“某一天”，用于“全部 / 当日”来回切换
    var lastDay by remember { mutableStateOf(selectedDay ?: todayStart) }

    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onShiftWeek(-1) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "上一周")
            }
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = TimeUtils.formatDate(days.first()).takeLast(5),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .width(20.dp)
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                )
                Text(
                    text = TimeUtils.formatDate(days.last()).takeLast(5),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            IconButton(onClick = { onShiftWeek(1) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "下一周")
            }
            TextButton(onClick = {
                val cur = selectedDay
                if (cur != null) {
                    lastDay = cur
                    onSelectDay(null)
                } else {
                    onSelectDay(lastDay)
                }
            }) { Text(if (selectedDay == null) "当日" else "全部") }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragAccum = 0f },
                        onHorizontalDrag = { _, dx -> dragAccum += dx },
                        onDragEnd = {
                            if (dragAccum > 140f) onShiftWeek(-1)
                            else if (dragAccum < -140f) onShiftWeek(1)
                        }
                    )
                },
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            days.forEachIndexed { index, day ->
                val selected = selectedDay == day
                val isToday = day == todayStart
                val bg = when {
                    selected -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                }
                val fg = when {
                    selected -> MaterialTheme.colorScheme.onPrimary
                    else -> MaterialTheme.colorScheme.onSurface
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(bg)
                        .clickable { lastDay = day; onSelectDay(day) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = TimeUtils.WEEKDAY_LABELS[index],
                        style = MaterialTheme.typography.labelSmall,
                        color = fg
                    )
                    Text(
                        text = TimeUtils.formatDate(day).takeLast(2),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        color = fg
                    )
                }
            }
        }
        if (selectedDay == null) {
            Text(
                text = "当前显示全部待办",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun TodoFilterPanel(
    filters: TodoFilters,
    tags: List<TagEntity>,
    onTogglePriority: (Priority) -> Unit,
    onSetTag: (Long?) -> Unit,
    onSetCompletion: (CompletionFilter) -> Unit
) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompletionFilter.entries.forEach { c ->
                FilterChip(
                    selected = filters.completion == c,
                    onClick = { onSetCompletion(c) },
                    label = { Text(completionLabel(c)) }
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Priority.entries.reversed().forEach { p ->
                FilterChip(
                    selected = p in filters.priorities,
                    onClick = { onTogglePriority(p) },
                    label = { Text("优先级${priorityLabel(p)}") }
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        val selectedTag = tags.firstOrNull { it.id == filters.tagId }
        SimpleDropdown(
            label = "标签筛选",
            items = tags,
            selected = selectedTag,
            itemLabel = { it.name },
            onSelect = { onSetTag(it.id) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = "全部标签"
        )
        if (selectedTag != null) {
            TextButton(onClick = { onSetTag(null) }) { Text("清除标签筛选") }
        }
    }
}

@Composable
private fun TodoItem(
    todo: TodoEntity,
    tags: List<TagEntity>,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tag = tags.firstOrNull { it.id == todo.tagId }
    var actionsExpanded by remember(todo.id) { mutableStateOf(false) }
    Card(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = todo.isCompleted, onCheckedChange = { onToggle() })
            Column(Modifier.weight(1f)) {
                Text(
                    text = todo.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else null,
                    color = if (todo.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface
                )
                if (todo.note.isNotBlank()) {
                    Text(
                        text = todo.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PriorityBadge(todo.priority)
                    if (tag != null) TagChip(tag.name, tag.color)
                    todo.dueDate?.let {
                        Text(
                            text = TimeUtils.formatDate(it),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (todo.repeatRule == RepeatRule.DAILY) {
                        Icon(
                            Icons.Filled.Repeat,
                            contentDescription = "每日重复",
                            modifier = Modifier.height(16.dp)
                        )
                        todo.endDate?.let {
                            Text(
                                text = "至 ${TimeUtils.formatDate(it)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            AnimatedVisibility(actionsExpanded) {
                Row {
                    IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "修改") }
                    IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "删除") }
                }
            }
            IconButton(onClick = { actionsExpanded = !actionsExpanded }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "更多操作")
            }
        }
    }
}

private fun completionLabel(c: CompletionFilter): String = when (c) {
    CompletionFilter.ALL -> "全部"
    CompletionFilter.ACTIVE -> "未完成"
    CompletionFilter.DONE -> "已完成"
}

private fun priorityLabel(p: Priority): String = when (p) {
    Priority.HIGH -> "高"
    Priority.MEDIUM -> "中"
    Priority.LOW -> "低"
}
