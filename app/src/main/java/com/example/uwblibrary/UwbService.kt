package com.example.uwblibrary

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class UwbService() : Service() {

    private var context: Context? = null

    private lateinit var uwbManager: UwbManager

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: ")


        val notification = createNotification()

        uwbManager = UwbManager()


        // Start the service in the foreground and show the notification
        startForeground(NOTIFICATION_ID, notification)


    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ")

        uwbManager.startUwbRanging()
        return START_STICKY

    }


    override fun onDestroy() {
        Log.d(TAG, "onDestroy: ")
        super.onDestroy()

        uwbManager.stopUwbRanging()
    }

    companion object {
        private val TAG = UwbService::class.java.simpleName
        private const val CHANNEL_ID = "MyServiceChannel"
        private const val NOTIFICATION_ID = 1
    }

    private fun createNotification(): Notification? {
        // Create a notification channel for Android 8.0 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Dooropen is running",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(
                NotificationManager::class.java
            )
            manager.createNotificationChannel(channel)
        }

        // Build a notification for the foreground service
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Logger Service")
            .setContentText("Dooropen app is running")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
    }

}