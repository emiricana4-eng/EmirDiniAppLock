package com.applock.free

import android.app.*
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import android.provider.Settings
import androidx.core.app.NotificationCompat

class LockService : Service() {

    private lateinit var prefManager: PrefManager
    private lateinit var lockOverlay: LockOverlay
    private val handler = Handler(Looper.getMainLooper())

    private val ignoredPackages by lazy {
        setOf(
            packageName,
            "com.android.systemui",
            "com.google.android.permissioncontroller",
            "com.android.launcher",
            "com.google.android.launcher",
            "com.android.settings",
            "com.android.permissioncontroller",
            "com.android.inputmethod.latin"
        )
    }

    private val pollRunnable = object : Runnable {
        override fun run() {
            if (prefManager.isEnabled && System.currentTimeMillis() > pollPausedUntil) {
                checkForeground()
            }
            handler.postDelayed(this, POLL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefManager = PrefManager(this)
        lockOverlay = LockOverlay(this)

        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
        handler.post(pollRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        handler.removeCallbacks(pollRunnable)
        lockOverlay.hide()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun checkForeground() {
        if (!Settings.canDrawOverlays(this)) return

        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 5000, now)

        if (stats.isNullOrEmpty()) return

        val topApp = stats.maxByOrNull { it.lastTimeUsed }?.packageName ?: return

        if (topApp in ignoredPackages) return

        // 1. If not locked in database, ensure overlay is hidden
        if (!prefManager.isLocked(topApp)) {
            if (lockOverlay.currentPackage == topApp && lockOverlay.isShowing()) {
                lockOverlay.hide()
            }
            return
        }

        // 2. If already unlocked (session based), don't show overlay
        if (unlockedApps.contains(topApp)) {
            return
        }

        // 3. Otherwise: Must lock
        if (!lockOverlay.isShowing() || lockOverlay.currentPackage != topApp) {
            lockOverlay.show(topApp)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "App Lock", NotificationManager.IMPORTANCE_MIN)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("App Lock Active")
            .setContentText("Protecting your apps")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val NOTIF_ID = 1001
        private const val CHANNEL_ID = "applock_channel"
        private const val POLL_MS = 1000L // Faster polling for better responsiveness

        @JvmField var pollPausedUntil = 0L
        @JvmField val unlockedApps = mutableSetOf<String>()
        
        fun start(context: Context) {
            val intent = Intent(context, LockService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
