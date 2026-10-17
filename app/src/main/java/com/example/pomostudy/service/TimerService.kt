package com.example.pomostudy.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.pomostudy.PomoStudyApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 计时前台服务：以常驻通知保持进程存活，确保 APP 切后台 / 锁屏时计时不中断。
 * 服务仅展示通知，实际计时状态由 [TimerController] 维护。
 */
class TimerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var controller: TimerController
    private lateinit var helper: NotificationHelper
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val container = (application as PomoStudyApp).container
        controller = container.timerController
        helper = NotificationHelper(this)
        helper.createChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val current = controller.state.value
        startForegroundWithNotification(current)

        job?.cancel()
        job = scope.launch {
            controller.state.collect { st ->
                if (!st.isActive) {
                    stopForegroundAndSelf()
                } else {
                    NotificationManagerCompat.from(this@TimerService)
                        .notify(ONGOING_ID, helper.buildOngoing(st))
                }
            }
        }
        return START_STICKY
    }

    private fun startForegroundWithNotification(state: TimerUiState) {
        val notification = helper.buildOngoing(state)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(ONGOING_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(ONGOING_ID, notification)
        }
    }

    private fun stopForegroundAndSelf() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        job?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ONGOING_ID = 1001

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, TimerService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, TimerService::class.java))
        }
    }
}
