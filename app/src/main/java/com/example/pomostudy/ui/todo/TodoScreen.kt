package com.example.pomostudy.ui.todo

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
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

/** 待办页：筛选 + 列表 + 新增/编辑/删除/勾选。 */
@Composable
fun TodoScreen(container: AppContainer) {
    val vm: TodoViewModel = viewModel(factory = TodoViewModel.factory(container))
    val todos by vm.filtered.collectAsStateWithLifecycle()
    val tags by vm.tags.collectAsStateWithLifecycle()
    val filters by vm.filters.collectAsStateWithLifecycle()

    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TodoEntity?>(null) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TodoFilterBar(
                filters = filters,
                tags = tags,
                onTogglePriority = vm::togglePriorityFilter,
                onSetTag = vm::setTagFilter,
                onSetCompletion = vm::setCompletionFilter,
                onSetDate = vm::setDateFilter
            )

            if (todos.isEmpty()) {
                EmptyHint("暂无符合条件的待办，点击右下角 + 新增")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(todos, key = { it.id }) { todo ->
                        TodoItem(
                            todo = todo,
                            tags = tags,
                            onToggle = { vm.toggle(todo) },
                            onEdit = { editing = todo; showEditor = true },
                            onDelete = { vm.delete(todo) }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { editing = null; showEditor = true },
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
            tags = tags,
            onDismiss = { showEditor = false },
            onSave = { entity ->
                if (entity.id == 0L) {
                    vm.add(entity.title, entity.note, entity.dueDate, entity.tagId, entity.priority, entity.repeatRule)
                } else {
                    vm.update(entity)
                }
                showEditor = false
            }
        )
    }
}

@Composable
private fun TodoFilterBar(
    filters: TodoFilters,
    tags: List<TagEntity>,
    onTogglePriority: (Priority) -> Unit,
    onSetTag: (Long?) -> Unit,
    onSetCompletion: (CompletionFilter) -> Unit,
    onSetDate: (DateFilter) -> Unit
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
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
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DateFilter.entries.forEach { d ->
                FilterChip(
                    selected = filters.date == d,
                    onClick = { onSetDate(d) },
                    label = { Text(dateLabel(d)) }
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
    onDelete: () -> Unit
) {
    val tag = tags.firstOrNull { it.id == todo.tagId }
    Card(Modifier.fillMaxWidth()) {
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
                    if (todo.repeatRule != RepeatRule.NONE) {
                        Icon(
                            Icons.Filled.Repeat,
                            contentDescription = repeatLabel(todo.repeatRule),
                            modifier = Modifier.height(16.dp)
                        )
                    }
                }
            }
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "编辑") }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "删除") }
        }
    }
}

/** 新增 / 编辑待办弹窗。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodoEditorDialog(
    initial: TodoEntity?,
    tags: List<TagEntity>,
    onDismiss: () -> Unit,
    onSave: (TodoEntity) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var dueDate by remember { mutableStateOf(initial?.dueDate) }
    var tagId by remember { mutableStateOf(initial?.tagId) }
    var priority by remember { mutableStateOf(initial?.priority ?: Priority.MEDIUM) }
    var repeatRule by remember { mutableStateOf(initial?.repeatRule ?: RepeatRule.NONE) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        (initial ?: TodoEntity(title = "")).copy(
                            title = title.trim(),
                            note = note.trim(),
                            dueDate = dueDate,
                            tagId = tagId,
                            priority = priority,
                            repeatRule = repeatRule
                        )
                    )
                },
                enabled = title.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        title = { Text(if (initial == null) "新增待办" else "编辑待办") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Filled.DateRange, contentDescription = null)
                        Text(
                            text = dueDate?.let { TimeUtils.formatDate(it) } ?: "选择截止日期",
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                    if (dueDate != null) {
                        TextButton(onClick = { dueDate = null }) { Text("清除") }
                    }
                }
                Spacer(Modifier.height(8.dp))
                val selectedTag = tags.firstOrNull { it.id == tagId }
                SimpleDropdown(
                    label = "标签",
                    items = tags,
                    selected = selectedTag,
                    itemLabel = { it.name },
                    onSelect = { tagId = it.id },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "未选择"
                )
                if (selectedTag != null) {
                    TextButton(onClick = { tagId = null }) { Text("清除标签") }
                }
                Spacer(Modifier.height(8.dp))
                Text("优先级", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Priority.entries.reversed().forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(priorityLabel(p)) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("重复", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RepeatRule.entries.forEach { r ->
                        FilterChip(
                            selected = repeatRule == r,
                            onClick = { repeatRule = r },
                            label = { Text(repeatLabel(r)) }
                        )
                    }
                }
            }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dueDate = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private fun completionLabel(c: CompletionFilter): String = when (c) {
    CompletionFilter.ALL -> "全部"
    CompletionFilter.ACTIVE -> "未完成"
    CompletionFilter.DONE -> "已完成"
}

private fun dateLabel(d: DateFilter): String = when (d) {
    DateFilter.ALL -> "不限日期"
    DateFilter.TODAY -> "今天"
    DateFilter.WEEK -> "本周"
}

private fun priorityLabel(p: Priority): String = when (p) {
    Priority.HIGH -> "高"
    Priority.MEDIUM -> "中"
    Priority.LOW -> "低"
}

private fun repeatLabel(r: RepeatRule): String = when (r) {
    RepeatRule.NONE -> "不重复"
    RepeatRule.DAILY -> "每日"
    RepeatRule.MONTHLY -> "每月"
}
