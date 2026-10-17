package com.example.pomostudy.ui.stats

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import java.time.YearMonth

/** 统计页：按日/周/月聚合“各待办”学习时长（支持回溯）+ 饼图 + 本周曲线 + 目标进度。 */
@Composable
fun StatsScreen(container: AppContainer) {
    val vm: StatsViewModel = viewModel(factory = StatsViewModel.factory(container))
    val state by vm.state.collectAsStateWithLifecycle()

    val periodDays = when (state.period) {
        StatsPeriod.DAY -> 1
        StatsPeriod.WEEK -> 7
        StatsPeriod.MONTH -> YearMonth.now().lengthOfMonth()
    }
    val goalSeconds = (state.goalMinutes * 60L * periodDays).coerceAtLeast(1L)
    val targetProgress = (state.totalSeconds.toFloat() / goalSeconds.toFloat()).coerceIn(0f, 1f)
    val progress by animateFloatAsState(targetValue = targetProgress, label = "goalProgress")

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
                    Text(
                        text = state.rangeLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (state.isCurrent) "当前周期" else "回溯查看",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { vm.moveOffset(1) },
                    enabled = !state.isCurrent
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "下一周期")
                }
            }
            if (!state.isCurrent) {
                TextButton(
                    onClick = { vm.goToCurrent() },
                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                ) { Text("回到当前") }
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
        SectionTitle("目标完成进度")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = "每日目标：${state.goalMinutes} 分钟",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (state.period != StatsPeriod.DAY) {
                    Text(
                        text = "本${periodLabel(state.period)}目标：${state.goalMinutes} × $periodDays = ${state.goalMinutes * periodDays} 分钟",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "已完成 ${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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

        Spacer(Modifier.height(16.dp))
        SectionTitle("本周学习曲线（周一 ~ 周日）")
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
        Spacer(Modifier.height(24.dp))
    }
}

private fun periodLabel(p: StatsPeriod): String = when (p) {
    StatsPeriod.DAY -> "日"
    StatsPeriod.WEEK -> "周"
    StatsPeriod.MONTH -> "月"
}
