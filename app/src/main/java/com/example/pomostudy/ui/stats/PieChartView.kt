package com.example.pomostudy.ui.stats

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry

/**
 * 使用 MPAndroidChart 的饼图展示各标签学习时长分布。
 * 通过 AndroidView 桥接到 Compose，主题色随传入的 [textColor] 适配深浅色。
 */
@Composable
fun TagPieChart(
    slices: List<TagSlice>,
    textColor: Int,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PieChart(ctx).apply {
                description.isEnabled = false
                isDrawHoleEnabled = true
                setHoleColor(android.graphics.Color.TRANSPARENT)
                holeRadius = 52f
                setTransparentCircleAlpha(0)
                setDrawEntryLabels(false)
                setUsePercentValues(true)
                setNoDataText("暂无学习数据")
                setNoDataTextColor(textColor)
                setEntryLabelColor(textColor)
                legend.isEnabled = true
                legend.textColor = textColor
                legend.isWordWrapEnabled = true
            }
        },
        update = { chart ->
            if (slices.isEmpty()) {
                chart.clear()
            } else {
                val entries = slices.map { PieEntry(it.seconds.toFloat(), it.label) }
                val dataSet = PieDataSet(entries, "").apply {
                    colors = slices.map { it.colorLong.toInt() }.toMutableList()
                    sliceSpace = 2f
                    valueTextSize = 11f
                    valueTextColor = android.graphics.Color.WHITE
                    setDrawValues(true)
                }
                chart.data = PieData(dataSet)
                chart.highlightValues(null)
            }
            chart.invalidate()
        }
    )
}
