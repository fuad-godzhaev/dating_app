package com.apiguave.pds

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * Android foreground service to run PDS in the background.
 *
 * This ensures the PDS server continues running even when the app is backgrounded.
 */
class PdsBackgroundService : Service() {

    private lateinit var pdsService: PdsService
    private val binder = PdsBinder()

    companion object {
        private const val TAG = "PdsBackgroundService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "pds_service_channel"
        private const val CHANNEL_NAME = "PDS Service"

        const val ACTION_START = "com.apiguave.pds.START"
        const val ACTION_STOP = "com.apiguave.pds.STOP"

        /**
         * Starts the PDS background service.
         */
        fun start(context: Context) {
            val intent = Intent(context, PdsBackgroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * Stops the PDS background service.
         */
        fun stop(context: Context) {
            val intent = Intent(context, PdsBackgroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
    }

    inner class PdsBinder : Binder() {
        fun getService(): PdsBackgroundService = this@PdsBackgroundService
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "PDS Background Service created")

        pdsService = PdsService(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                Log.i(TAG, "Starting PDS in foreground service")
                startForeground(NOTIFICATION_ID, createNotification("PDS is starting..."))

                pdsService.start(
                    onStarted = {
                        Log.i(TAG, "PDS started successfully")
                        updateNotification("PDS is running")
                    },
                    onError = { error ->
                        Log.e(TAG, "PDS failed to start: $error")
                        updateNotification("PDS failed to start")
                        stopSelf()
                    }
                )
            }
            ACTION_STOP -> {
                Log.i(TAG, "Stopping PDS")
                pdsService.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        Log.i(TAG, "PDS Background Service destroyed")
        pdsService.stop()
        super.onDestroy()
    }

    /**
     * Gets the PDS service instance.
     */
    fun getPdsService(): PdsService = pdsService

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps PDS server running in background"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ATProto PDS")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val notification = createNotification(contentText)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
