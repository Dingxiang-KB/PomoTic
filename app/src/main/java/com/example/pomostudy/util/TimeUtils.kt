package com.example.pomostudy.util

import java.util.Calendar
import java.util.Locale

/** 时间与时长格式化、以及日期边界计算工具。全部使用 [Calendar]，兼容 minSdk 24。 */
object TimeUtils {

    const val ONE_DAY_MILLIS = 24L * 60 * 60 * 1000

    /** 计时显示：超过 1 小时显示 H:MM:SS，否则 MM:SS。 */
    fun formatClock(seconds: Long): String {
        val s = seconds.coerceAtLeast(0)
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) {
            String.format(Locale.getDefault(), "%d:%02d:%02d", h, m, sec)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", m, sec)
        }
    }

    /** 时长可读化，如 "1 小时 23 分" / "23 分" / "45 秒"。 */
    fun formatDuration(seconds: Long): String {
        val s = seconds.coerceAtLeast(0)
        val h = s / 3600
        val m = (s % 3600) / 60
        return when {
            h > 0 -> "$h 小时 $m 分"
            m > 0 -> "$m 分"
            else -> "$s 秒"
        }
    }

    fun startOfDay(millis: Long): Long = calendar(millis).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun endOfDay(millis: Long): Long = startOfDay(millis) + ONE_DAY_MILLIS - 1

    /** 以周一为一周起点。 */
    fun startOfWeek(millis: Long): Long = calendar(startOfDay(millis)).apply {
        val dow = get(Calendar.DAY_OF_WEEK) // Sunday=1, Monday=2 ... Saturday=7
        val offset = if (dow == Calendar.SUNDAY) 6 else dow - Calendar.MONDAY
        add(Calendar.DAY_OF_MONTH, -offset)
    }.timeInMillis

    fun endOfWeek(millis: Long): Long = startOfWeek(millis) + 7 * ONE_DAY_MILLIS - 1

    fun startOfMonth(millis: Long): Long = calendar(millis).apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun endOfMonth(millis: Long): Long =
        calendar(startOfMonth(millis)).apply { add(Calendar.MONTH, 1) }.timeInMillis - 1

    fun addOneMonth(millis: Long): Long = calendar(millis).apply { add(Calendar.MONTH, 1) }.timeInMillis

    fun addDays(millis: Long, days: Int): Long =
        calendar(millis).apply { add(Calendar.DAY_OF_MONTH, days) }.timeInMillis

    fun addMonths(millis: Long, months: Int): Long =
        calendar(millis).apply { add(Calendar.MONTH, months) }.timeInMillis

    fun formatDate(millis: Long): String {
        val c = calendar(millis)
        return String.format(
            Locale.getDefault(), "%04d-%02d-%02d",
            c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun formatTime(millis: Long): String {
        val c = calendar(millis)
        return String.format(
            Locale.getDefault(), "%02d:%02d",
            c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)
        )
    }

    fun formatDateTime(millis: Long): String = "${formatDate(millis)} ${formatTime(millis)}"

    fun formatYearMonth(millis: Long): String {
        val c = calendar(millis)
        return String.format(Locale.getDefault(), "%04d 年 %02d 月", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1)
    }

    /** 周一为 0 ... 周日为 6。 */
    fun dayIndexFromMonday(millis: Long): Int {
        val dow = calendar(millis).get(Calendar.DAY_OF_WEEK)
        return if (dow == Calendar.SUNDAY) 6 else dow - Calendar.MONDAY
    }

    val WEEKDAY_LABELS = listOf("一", "二", "三", "四", "五", "六", "日")

    private fun calendar(millis: Long): Calendar =
        Calendar.getInstance().apply { timeInMillis = millis }
}
