package com.example.pomostudy.ui.stats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pomostudy.di.AppContainer
import com.example.pomostudy.ui.components.EmptyHint
import com.example.pomostudy.ui.components.SectionTitle
import com.example.pomostudy.ui.theme.toComposeColor
import com.example.pomostudy.util.TimeUtils

/** 统计页：按日/周/月聚合“各待办”学习时长（支持回溯）+ 饼图 + 学习曲线 + 计划完成进度。 */
@Composable
fun StatsScreen(container: AppContainer) {
    val vm: StatsViewModel = viewModel(factory = StatsViewModel.factory(container))
    val state by vm.state.collectAsStateWithLifecycle()
    var plannedExpanded by remember { mutableStateOf(false) }

    val planProgressTarget =
        if (state.plannedSeconds <= 0L) 0f
        else (state.plannedActualSeconds.toFloat() / state.plannedSeconds.toFloat()).coerceIn(0f, 1f)
    val planProgress by animateFloatAsState(targetValue = planProgressTarget, label = "planProgress")

    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val lineColor = MaterialTheme.colorScheme.primary.toArgb()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 维度选择 + 刷新
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatsPeriod.entries.forEach { p ->
                    FilterChip(
                        selected = state.period == p,
                        onClick = { vm.setPeriod(p) },
                        label = { Text(periodLabel(p)) }
                    )
                }
            }
            IconButton(onClick = { vm.refresh() }) {
                Icon(Icons.Filled.Refresh, contentDescription = "刷新")
            }
        }

        Spacer(Modifier.height(8.dp))
        // 回溯控制
        Card(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { vm.moveOffset(-1) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "上一周期")
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (state.rangeEndLabel != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = state.rangeStartLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .width(18.dp)
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            )
                            Text(
                                text = state.rangeEndLabel ?: "",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Text(
                            text = state.rangeLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = if (state.isCurrent) "当前周期" else "回溯查看",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { vm.goToCurrent() },
                    enabled = !state.isCurrent,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Filled.Today, contentDescription = "回到当前", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { vm.moveOffset(1) }, enabled = !state.isCurrent) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "下一周期")
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = "${periodLabel(state.period)}学习总时长",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = TimeUtils.formatDuration(state.totalSeconds),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionTitle("计划完成进度")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                if (state.plannedSeconds <= 0L) {
                    Text(
                        text = "本${periodLabel(state.period)}还没有设置计划时长的待办",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "可在待办中为事项设置“每日计划时长”",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "计划 ${state.plannedTodoCount} 个 · 完成 ${(planProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        IconButton(onClick = { plannedExpanded = !plannedExpanded }) {
                            Icon(
                                imageVector = if (plannedExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = if (plannedExpanded) "收起" else "展开"
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { planProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    AnimatedVisibility(
                        visible = plannedExpanded,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column(Modifier.padding(top = 8.dp)) {
                            state.plannedItems.forEach { item ->
                                PlannedItemRow(item)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionTitle("各待办时间分布")
        if (state.slices.isEmpty()) {
            EmptyHint("当前时间段暂无学习记录")
        } else {
            Card(Modifier.fillMaxWidth()) {
                StatPieChart(
                    slices = state.slices,
                    textColor = textColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .padding(8.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            state.slices.forEach { slice ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(slice.colorLong.toComposeColor())
                    )
                    Text(
                        text = slice.label,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = TimeUtils.formatDuration(slice.seconds),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (state.showWeekly) {
            Spacer(Modifier.height(16.dp))
            SectionTitle("学习曲线")
            Card(Modifier.fillMaxWidth()) {
                WeeklyLineChart(
                    points = state.weekly,
                    lineColor = lineColor,
                    textColor = textColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .padding(8.dp)
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PlannedItemRow(item: PlannedProgress) {
    val frac = if (item.plannedSeconds <= 0L) 0f
    else (item.actualSeconds.toFloat() / item.plannedSeconds.toFloat()).coerceIn(0f, 1f)
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${TimeUtils.formatDuration(item.actualSeconds)} / ${TimeUtils.formatDuration(item.plannedSeconds)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = { frac },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun periodLabel(p: StatsPeriod): String = when (p) {
    StatsPeriod.DAY -> "日"
    StatsPeriod.WEEK -> "周"
    StatsPeriod.MONTH -> "月"
}
