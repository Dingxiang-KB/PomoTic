package com.example.pomostudy.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pomostudy.di.AppContainer
import com.example.pomostudy.domain.model.ThemeMode

/** 设置页：主题、使用说明、数据导入导出、清空数据。 */
@Composable
fun SettingsScreen(container: AppContainer) {
    val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container))
    val settings by vm.settings.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current

    var showClearConfirm by remember { mutableStateOf(false) }
    var showGuide by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var info by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 主题
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("主题", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick = { vm.setThemeMode(mode) },
                            label = { Text(themeLabel(mode)) }
                        )
                    }
                }
            }
        }

        // 数据备份（导入 / 导出，复制粘贴）
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("数据备份", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "导出为 JSON 文本可复制到剪贴板；导入将覆盖现有全部数据。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        vm.exportData { res ->
                            res.onSuccess { json ->
                                clipboard.setText(AnnotatedString(json))
                                info = "已导出并复制到剪贴板"
                            }.onFailure { info = "导出失败：${it.message}" }
                        }
                    }) { Text("导出到剪贴板") }
                    OutlinedButton(onClick = {
                        importText = clipboard.getText()?.text ?: ""
                        showImport = true
                    }) { Text("从剪贴板导入") }
                }
                info?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // 使用说明
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("使用说明", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { showGuide = true }) { Text("查看说明书") }
            }
        }

        // 调试：清空所有数据
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("调试", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "清空所有标签、待办与学习记录，并重置配置（不可恢复）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { showClearConfirm = true }) { Text("清空所有数据") }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("清空所有数据") },
            text = { Text("将删除所有标签、待办、学习记录，并重置计时与配置，且无法恢复。确定继续吗？") },
            confirmButton = {
                TextButton(onClick = { vm.clearAllData(); showClearConfirm = false; info = "已清空" }) {
                    Text("确定清空")
                }
            },
            dismissButton = { TextButton(onClick = { showClearConfirm = false }) { Text("取消") } }
        )
    }

    if (showImport) {
        AlertDialog(
            onDismissRequest = { showImport = false },
            title = { Text("导入数据") },
            text = {
                Column {
                    Text(
                        "粘贴导出的 JSON 文本（将覆盖现有全部数据）：",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 240.dp),
                        minLines = 6
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.importData(importText) { ok ->
                        info = if (ok) "导入成功" else "导入失败：格式错误"
                        showImport = false
                    }
                }) { Text("导入") }
            },
            dismissButton = { TextButton(onClick = { showImport = false }) { Text("取消") } }
        )
    }

    if (showGuide) {
        AlertDialog(
            onDismissRequest = { showGuide = false },
            title = { Text("使用说明") },
            text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    GuideText()
                }
            },
            confirmButton = { TextButton(onClick = { showGuide = false }) { Text("知道了") } }
        )
    }
}

@Composable
private fun GuideText() {
    val tips = listOf(
        "计时：顶部可切换「倒计时 / 自由」；倒计时下切换「工作 / 休息」，并设置对应时长。开始后计时在后台继续，结束会推送系统通知。",
        "绑定待办：计时页可选择标签与待办，结束后自动记录该待办的学习时长；可勾选“结束后自动完成该待办”。",
        "待办：顶部为当周日期条，点击某天查看当天待办，可左右滑动切换周；「筛选」可展开按完成状态/优先级/标签过滤。",
        "每日待办：选择「每日」后可设开始/结束日期，区间内每天都会生成待办；删除时会询问“仅当日”或“全部”。",
        "计划时长：在待办里可为事项设置“每日计划时长”，统计页“计划完成进度”据此展示完成度，可展开查看每条明细。",
        "日历：点击任意一天可查看并新增/编辑/删除当天待办。",
        "统计：日/周/月三种维度并可回溯上一周期；图表展示各待办时长分布；日/周维度显示学习曲线。",
        "数据：设置中可将全部数据导出为 JSON（复制到剪贴板），或粘贴 JSON 导入覆盖。"
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tips.forEach { t ->
            Text("• $t", style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun themeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> "跟随系统"
    ThemeMode.LIGHT -> "浅色"
    ThemeMode.DARK -> "深色"
}
