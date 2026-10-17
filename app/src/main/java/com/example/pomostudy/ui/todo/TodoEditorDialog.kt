package com.example.pomostudy.ui.todo

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import com.example.pomostudy.data.local.entity.TagEntity
import com.example.pomostudy.data.local.entity.TodoEntity
import com.example.pomostudy.domain.model.Priority
import com.example.pomostudy.domain.model.RepeatRule
import com.example.pomostudy.ui.components.SimpleDropdown
import com.example.pomostudy.util.TimeUtils

/**
 * 新增 / 编辑待办弹窗（待办页与日历页共用）。
 *
 * 规则：
 * - 重复：仅“不重复 / 每日”。
 * - 每日重复：显示“结束日期”（即每天添加到哪一天为止，留空表示每天重复）。
 * - 一次性：不显示日期选择；若从日历某一天进入（[defaultDueDate] 非空），则归入该天。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoEditorDialog(
    initial: TodoEntity?,
    defaultDueDate: Long?,
    tags: List<TagEntity>,
    onDismiss: () -> Unit,
    onSave: (TodoEntity) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var tagId by remember { mutableStateOf(initial?.tagId) }
    var priority by remember { mutableStateOf(initial?.priority ?: Priority.MEDIUM) }
    var repeatRule by remember {
        mutableStateOf(
            if (initial?.repeatRule == RepeatRule.DAILY) RepeatRule.DAILY else RepeatRule.NONE
        )
    }
    var endDate by remember { mutableStateOf(initial?.endDate) }
    var showEndPicker by remember { mutableStateOf(false) }

    val isDaily = repeatRule == RepeatRule.DAILY
    // 一次性任务归属日期：编辑时沿用原日期，否则用传入的日期（来自日历某天）
    val fixedDueDate = initial?.dueDate ?: defaultDueDate

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val base = initial ?: TodoEntity(title = "")
                    val dueDate = when {
                        isDaily -> initial?.dueDate ?: defaultDueDate
                        ?: TimeUtils.startOfDay(System.currentTimeMillis())
                        else -> fixedDueDate
                    }
                    onSave(
                        base.copy(
                            title = title.trim(),
                            note = note.trim(),
                            tagId = tagId,
                            priority = priority,
                            repeatRule = repeatRule,
                            dueDate = dueDate,
                            endDate = if (isDaily) endDate else null,
                            seriesId = if (isDaily) base.seriesId else null,
                            carryOver = false
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
                    FilterChip(
                        selected = repeatRule == RepeatRule.NONE,
                        onClick = { repeatRule = RepeatRule.NONE },
                        label = { Text("不重复") }
                    )
                    FilterChip(
                        selected = repeatRule == RepeatRule.DAILY,
                        onClick = { repeatRule = RepeatRule.DAILY },
                        label = { Text("每日") }
                    )
                }
                if (isDaily) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "结束日期（留空则每天重复）",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { showEndPicker = true }) {
                            Icon(Icons.Filled.DateRange, contentDescription = null)
                            Text(
                                text = endDate?.let { TimeUtils.formatDate(it) } ?: "每天重复",
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                        if (endDate != null) {
                            TextButton(onClick = { endDate = null }) { Text("清除") }
                        }
                    }
                } else if (fixedDueDate != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "日期：${TimeUtils.formatDate(fixedDueDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    )

    if (showEndPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = endDate)
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endDate = state.selectedDateMillis
                    showEndPicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showEndPicker = false }) { Text("取消") } }
        ) {
            DatePicker(state = state)
        }
    }
}

private fun priorityLabel(p: Priority): String = when (p) {
    Priority.HIGH -> "高"
    Priority.MEDIUM -> "中"
    Priority.LOW -> "低"
}
