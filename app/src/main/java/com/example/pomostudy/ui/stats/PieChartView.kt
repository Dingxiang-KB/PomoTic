package com.example.pomostudy.ui.stats

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

/**
 * 使用 MPAndroidChart 的饼图展示“各待办”学习时长分布。
 * 通过 AndroidView 桥接到 Compose，文字颜色随传入的 [textColor] 适配深浅色。
 */
@Composable
fun StatPieChart(
    slices: List<StatSlice>,
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

/**
 * 本周（周一..周日）每日学习时长曲线图。
 */
@Composable
fun WeeklyLineChart(
    points: List<DailyPoint>,
    lineColor: Int,
    textColor: Int,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            LineChart(ctx).apply {
                description.isEnabled = false
                setTouchEnabled(true)
                isDragEnabled = false
                setScaleEnabled(false)
                legend.isEnabled = false
                setNoDataText("本周暂无学习数据")
                setNoDataTextColor(textColor)
                setExtraOffsets(8f, 8f, 12f, 8f)
                axisRight.isEnabled = false
                axisLeft.axisMinimum = 0f
                axisLeft.granularity = 1f
                axisLeft.textColor = textColor
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.setDrawGridLines(false)
                xAxis.granularity = 1f
                xAxis.textColor = textColor
            }
        },
        update = { chart ->
            val entries = points.mapIndexed { i, p -> Entry(i.toFloat(), p.seconds / 60f) }
            val dataSet = LineDataSet(entries, "分钟").apply {
                colors = mutableListOf(lineColor)
                setCircleColor(lineColor)
                lineWidth = 2.5f
                setDrawCircleHole(false)
                circleRadius = 4f
                setDrawValues(false)
                setDrawFilled(true)
                fillColor = lineColor
                fillAlpha = 60
                mode = LineDataSet.Mode.CUBIC_BEZIER
            }
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(points.map { it.label })
            chart.xAxis.axisMinimum = -0.5f
            chart.xAxis.axisMaximum = (points.size - 0.5f).coerceAtLeast(0.5f)
            chart.data = LineData(dataSet)
            chart.invalidate()
        }
    )
}
