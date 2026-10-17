package com.example.pomostudy.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.pomostudy.MainActivity
import com.example.pomostudy.R
import com.example.pomostudy.domain.model.TimerMode
import com.example.pomostudy.domain.model.TimerPhase
import com.example.pomostudy.domain.model.TimerStatus
import com.example.pomostudy.util.TimeUtils

/** 通知帮助类：创建通知渠道、构建“计时进行中”常驻通知与“计时结束”提醒通知。 */
class NotificationHelper(private val context: Context) {

    private val manager = NotificationManagerCompat.from(context)

    /** 创建通知渠道（Android 8.0+）。 */
    fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ongoing = NotificationChannel(
                CHANNEL_ONGOING,
                "计时进行中",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示正在进行的计时状态"
                setShowBadge(false)
            }
            val finished = NotificationChannel(
                CHANNEL_FINISHED,
                "计时结束提醒",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "计时结束时发送系统通知"
            }
            val nm = context.getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(ongoing)
            nm?.createNotificationChannel(finished)
        }
    }

    private fun contentIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** 前台服务的常驻通知。 */
    fun buildOngoing(state: TimerUiState): Notification {
        val timeText = TimeUtils.formatClock(state.displaySeconds)
        val label = when {
            state.mode == TimerMode.FREE -> "自由学习"
            state.phase == TimerPhase.WORK -> "专注工作"
            else -> "休息中"
        }
        val text = if (state.status == TimerStatus.PAUSED) "已暂停 · $timeText" else "$label · $timeText"
        return NotificationCompat.Builder(context, CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle("PomoStudy 计时中")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setContentIntent(contentIntent())
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    /** 计时结束的系统通知（仅通知，无应用内弹窗 / 震动 / 声音由渠道控制）。 */
    fun notifySessionFinished(title: String, text: String) {
        if (!hasNotificationPermission()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_FINISHED)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(contentIntent())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        runCatching { manager.notify(FINISHED_ID, notification) }
    }

    private fun hasNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    companion object {
        const val CHANNEL_ONGOING = "timer_ongoing"
        const val CHANNEL_FINISHED = "timer_finished"
        const val FINISHED_ID = 1002
    }
}
