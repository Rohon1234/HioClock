package com.hioclock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager

class ClockService : Service() {
    private val handler = Handler(Looper.getMainLooper())

    private val providers = listOf(
        Triple(AnalogBlackWidget::class.java, false, true),
        Triple(AnalogWhiteWidget::class.java, false, false),
        Triple(DigitalBlackWidget::class.java, true, true),
        Triple(DigitalWhiteWidget::class.java, true, false)
    )

    private val tick = object : Runnable {
        override fun run() {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isInteractive) {
                handler.postDelayed(this, 3000)
                return
            }
            val mgr = AppWidgetManager.getInstance(this@ClockService)
            var any = false
            for ((cls, digital, dark) in providers) {
                val ids = mgr.getAppWidgetIds(ComponentName(this@ClockService, cls))
                if (ids.isNotEmpty()) {
                    any = true
                    ClockUpdater.update(this@ClockService, mgr, ids, digital, dark)
                }
            }
            if (!any) {
                stopSelf()
                return
            }
            handler.postDelayed(this, 1000 - System.currentTimeMillis() % 1000 + 5)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()
        handler.removeCallbacks(tick)
        handler.post(tick)
        return START_STICKY
    }

    private fun startAsForeground() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel("clock", "Clock widgets", NotificationManager.IMPORTANCE_MIN)
        )
        val n = Notification.Builder(this, "clock")
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentTitle("HiOS Clock running")
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, n)
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        fun start(ctx: Context) {
            try {
                ctx.startForegroundService(Intent(ctx, ClockService::class.java))
            } catch (e: Exception) {
                // Background start can be blocked; opening the app once starts it.
            }
        }
    }
}
