package com.example.pomostudy.ui.timer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pomostudy.data.local.entity.TagEntity
import com.example.pomostudy.data.local.entity.TodoEntity
import com.example.pomostudy.di.AppContainer
import com.example.pomostudy.domain.model.TimerMode
import com.example.pomostudy.domain.model.TimerPhase
import com.example.pomostudy.domain.model.TimerStatus
import com.example.pomostudy.ui.components.SectionTitle
import com.example.pomostudy.ui.components.SimpleDropdown
import com.example.pomostudy.util.TimeUtils

/** 计时页：倒计时番茄 / 正计时自由学习，含开始/暂停/继续/重置/结束以及待办绑定。 */
@Composable
fun TimerScreen(
    container: AppContainer,
    snackbarHostState: SnackbarHostState
) {
    val vm: TimerViewModel = viewModel(factory = TimerViewModel.factory(container))
    val state by vm.timerState.collectAsStateWithLifecycle()
    val tags by vm.tags.collectAsStateWithLifecycle()
    val todos by vm.todos.collectAsStateWithLifecycle()
    val records by vm.records.collectAsStateWithLifecycle()

    val todayTotal = remember(records) {
        val start = TimeUtils.startOfDay(System.currentTimeMillis())
        records.filter { it.startTime >= start }.sumOf { it.durationSeconds }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 模式选择
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.mode == TimerMode.POMODORO,
                onClick = { vm.setMode(TimerMode.POMODORO) },
                enabled = !state.isActive,
                label = { Text("倒计时番茄") }
            )
            FilterChip(
                selected = state.mode == TimerMode.FREE,
                onClick = { vm.setMode(TimerMode.FREE) },
                enabled = !state.isActive,
                label = { Text("自由学习") }
            )
        }

        // 阶段选择（仅番茄模式、空闲时）
        if (state.mode == TimerMode.POMODORO && !state.isActive) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.phase == TimerPhase.WORK,
                    onClick = { vm.setPhase(TimerPhase.WORK) },
                    label = { Text("工作 ${state.workMinutes} 分") }
                )
                FilterChip(
                    selected = state.phase == TimerPhase.BREAK,
                    onClick = { vm.setPhase(TimerPhase.BREAK) },
                    label = { Text("休息 ${state.breakMinutes} 分") }
                )
            }
            // 直接在计时页设置倒计时时长
            Spacer(Modifier.height(8.dp))
            Card(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DurationStepper(
                        label = "工作时长",
                        value = state.workMinutes,
                        range = 1..180,
                        onChange = { vm.setWorkMinutes(it) }
                    )
                    DurationStepper(
                        label = "休息时长",
                        value = state.breakMinutes,
                        range = 1..60,
                        onChange = { vm.setBreakMinutes(it) }
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CircularTimer(state = state)
        }
        Spacer(Modifier.height(16.dp))

        // 控制按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            when (state.status) {
                TimerStatus.IDLE -> {
                    Button(onClick = { vm.start() }) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Text("开始", modifier = Modifier.padding(start = 4.dp))
                    }
                }

                TimerStatus.RUNNING -> {
                    OutlinedButton(onClick = { vm.pause() }) {
                        Icon(Icons.Filled.Pause, contentDescription = null)
                        Text("暂停", modifier = Modifier.padding(start = 4.dp))
                    }
                    Spacer(Modifier.size(12.dp))
                    Button(onClick = { vm.finish() }) {
                        Icon(Icons.Filled.Stop, contentDescription = null)
                        Text("结束", modifier = Modifier.padding(start = 4.dp))
                    }
                }

                TimerStatus.PAUSED -> {
                    Button(onClick = { vm.resume() }) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Text("继续", modifier = Modifier.padding(start = 4.dp))
                    }
                    Spacer(Modifier.size(12.dp))
                    OutlinedButton(onClick = { vm.finish() }) {
                        Icon(Icons.Filled.Stop, contentDescription = null)
                        Text("结束", modifier = Modifier.padding(start = 4.dp))
                    }
                    Spacer(Modifier.size(12.dp))
                    OutlinedButton(onClick = { vm.reset() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Text("重置", modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        SectionTitle("本次学习")

        // 标签
        val selectedTag = tags.firstOrNull { it.id == state.tagId }
        SimpleDropdown(
            label = "标签",
            items = tags,
            selected = selectedTag,
            itemLabel = { it.name },
            onSelect = { vm.setTag(it.id) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = if (tags.isEmpty()) "暂无标签，请到标签管理新增" else "未选择"
        )
        if (selectedTag != null) {
            TextButton(onClick = { vm.setTag(null) }) { Text("清除标签") }
        }

        Spacer(Modifier.height(8.dp))

        // 绑定待办
        val activeTodos = todos.filter { !it.isCompleted }
        val selectedTodo = todos.firstOrNull { it.id == state.boundTodoId }
        SimpleDropdown(
            label = "绑定待办（用于统计该待办的学习时长）",
            items = activeTodos,
            selected = selectedTodo,
            itemLabel = { it.title },
            onSelect = { vm.setBoundTodo(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = if (activeTodos.isEmpty()) "暂无未完成待办" else "不绑定"
        )
        if (selectedTodo != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = state.autoCompleteTodo,
                    onCheckedChange = { vm.setAutoComplete(it) }
                )
                Text(
                    text = "计时结束后自动标记该待办完成",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            TextButton(onClick = { vm.setBoundTodo(null) }) { Text("取消绑定") }
        }

        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = state.note,
            onValueChange = { vm.setNote(it) },
            label = { Text("备注") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        Spacer(Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = "今日学习总时长",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = TimeUtils.formatDuration(todayTotal),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** 圆环形计时盘：番茄模式显示剩余进度，自由模式显示正计时。 */
@Composable
private fun CircularTimer(state: com.example.pomostudy.service.TimerUiState) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val progressColor = MaterialTheme.colorScheme.primary
    val sweepTarget = if (state.mode == TimerMode.FREE) 360f else 360f * state.progress
    val sweep by animateFloatAsState(targetValue = sweepTarget, label = "timerSweep")
    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(240.dp)) {
            val stroke = 18.dp.toPx()
            val diameter = size.minDimension - stroke
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = TimeUtils.formatClock(state.displaySeconds),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold
            )
            val hint = when {
                state.mode == TimerMode.FREE -> "自由学习中"
                state.phase == TimerPhase.WORK -> "专注工作"
                else -> "休息"
            }
            Text(
                text = if (state.status == TimerStatus.PAUSED) "已暂停" else hint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 时长加减组件（计时页直接设置倒计时分钟数）。 */
@Composable
private fun DurationStepper(
    label: String,
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { onChange((value - 1).coerceIn(range.first, range.last)) }) {
                Text("−")
            }
            Text(
                text = "$value",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            OutlinedButton(onClick = { onChange((value + 1).coerceIn(range.first, range.last)) }) {
                Text("＋")
            }
        }
    }
}
