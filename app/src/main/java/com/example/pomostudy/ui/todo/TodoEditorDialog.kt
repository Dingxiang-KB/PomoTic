package com.example.pomostudy.ui.todo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
 * - 重复：不重复 / 每日。
 * - 每日：显示“开始日期”和“结束日期”。开始日期默认当天；结束日期留空表示此后每天都添加。
 *   （保存时会把区间内每天的实例一次性铺满。）
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
    val todayStart = remember { TimeUtils.startOfDay(System.currentTimeMillis()) }
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var tagId by remember { mutableStateOf(initial?.tagId) }
    var priority by remember { mutableStateOf(initial?.priority ?: Priority.MEDIUM) }
    var repeatRule by remember {
        mutableStateOf(
            if (initial?.repeatRule == RepeatRule.DAILY) RepeatRule.DAILY else RepeatRule.NONE
        )
    }
    var startDate by remember {
        mutableStateOf(initial?.dueDate ?: defaultDueDate ?: todayStart)
    }
    var endDate by remember { mutableStateOf(initial?.endDate) }
    var plannedEnabled by remember { mutableStateOf(initial?.plannedMinutes != null) }
    var plannedMinutes by remember { mutableStateOf(initial?.plannedMinutes ?: 30) }
    // 0=无, 1=选开始日期, 2=选结束日期
    var pickerTarget by remember { mutableStateOf(0) }

    val isDaily = repeatRule == RepeatRule.DAILY
    val fixedDueDate = initial?.dueDate ?: defaultDueDate

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 600.dp)
        ) {
            Column(
                Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (initial == null) "新增待办" else "编辑待办",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(12.dp))
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
                        onClick = {
                            repeatRule = RepeatRule.DAILY
                            if (startDate < todayStart) startDate = todayStart
                        },
                        label = { Text("每日") }
                    )
                }
                if (isDaily) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("开始日期", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(64.dp))
                        OutlinedButton(onClick = { pickerTarget = 1 }) {
                            Icon(Icons.Filled.DateRange, contentDescription = null)
                            Text(TimeUtils.formatDate(startDate), modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("结束日期", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(64.dp))
                        OutlinedButton(onClick = { pickerTarget = 2 }) {
                            Icon(Icons.Filled.DateRange, contentDescription = null)
                            Text(
                                text = endDate?.let { TimeUtils.formatDate(it) } ?: "每天（无结束）",
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
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = plannedEnabled,
                        onCheckedChange = { plannedEnabled = it }
                    )
                    Text("设置每日计划时长", style = MaterialTheme.typography.bodyMedium)
                }
                if (plannedEnabled) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = {
                            plannedMinutes = (plannedMinutes - 10).coerceAtLeast(10)
                        }) { Text("−") }
                        Text(
                            text = "$plannedMinutes 分",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(84.dp),
                            textAlign = TextAlign.Center
                        )
                        OutlinedButton(onClick = {
                            plannedMinutes = (plannedMinutes + 10).coerceAtMost(1440)
                        }) { Text("＋") }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val base = initial ?: TodoEntity(title = "")
                            val due = if (isDaily) startDate else fixedDueDate
                            onSave(
                                base.copy(
                                    title = title.trim(),
                                    note = note.trim(),
                                    tagId = tagId,
                                    priority = priority,
                                    repeatRule = repeatRule,
                                    dueDate = due,
                                    endDate = if (isDaily) endDate else null,
                                    seriesId = if (isDaily) base.seriesId else null,
                                    plannedMinutes = if (plannedEnabled) plannedMinutes else null,
                                    carryOver = false
                                )
                            )
                        },
                        enabled = title.isNotBlank()
                    ) { Text("保存") }
                }
            }
        }
    }

    if (pickerTarget != 0) {
        val initialMillis = if (pickerTarget == 1) startDate else endDate
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { pickerTarget = 0 },
            confirmButton = {
                TextButton(onClick = {
                    val picked = pickerState.selectedDateMillis
                    if (pickerTarget == 1) {
                        if (picked != null) startDate = TimeUtils.startOfDay(picked)
                    } else {
                        endDate = picked?.let { TimeUtils.startOfDay(it) }
                    }
                    pickerTarget = 0
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { pickerTarget = 0 }) { Text("取消") } }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

private fun priorityLabel(p: Priority): String = when (p) {
    Priority.HIGH -> "高"
    Priority.MEDIUM -> "中"
    Priority.LOW -> "低"
}
