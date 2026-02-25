package com.easydine.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.easydine.app.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class NotificationService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage)
    {
        val title = message.notification?.title ?: "EeayDine"
        val body = message.notification?.body ?: "New update"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "updates_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            val channel = NotificationChannel(
                channelId,
                "Orders",
                NotificationManager.IMPORTANCE_HIGH
            )
            nm.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this,channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .build()

        nm.notify(System.currentTimeMillis().toInt(), notification)
    }

    override fun onNewToken(token: String)
    {
        android.util.Log.d("FCM_TOKEN", token)
    }
}
