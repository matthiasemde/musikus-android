package de.practicetime.practicetime

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.annotation.RequiresApi


class SessionForegroundService : Service() {

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Receive intent extras from Activity here, after it has started the Service via startForegroundService()
        Toast.makeText(this, "Service started. Look in your notifications.", Toast.LENGTH_SHORT).show()

        val channelId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel("my_service", "My Background Service")
            } else {
                // If earlier version channel ID is not used
                ""
            }
        val notification: Notification = Notification.Builder(this, channelId)
            .setContentTitle("Session running...")
            .setContentText("This text should display the active category later on")
            .setSmallIcon(android.R.drawable.ic_menu_today)
//            .setContentIntent(pendingIntent)  // not yet do anything on press because later on we will implement activity binding
            .build()
        startForeground(42, notification)

        // describes how the system should continue the service in the event that the system kills it
        return START_NOT_STICKY
    }

    // the "channel" is required for new Notifications from Oreo onwards https://stackoverflow.com/a/47533338
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String{
        val chan = NotificationChannel(channelId,
            channelName, NotificationManager.IMPORTANCE_NONE)
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    override fun onDestroy() {
        Toast.makeText(this, "Service done", Toast.LENGTH_SHORT).show()
    }

    override fun onBind(intent: Intent?): IBinder? {
        // we do not provide binding
        return null
    }
}