package com.honeynotify.demo

import android.app.NotificationChannel
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class DemoMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        Thread {
            runCatching {
                HoneyNotifyClient.get(applicationContext).onTokenRefresh(token)
            }
        }.start()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Thread {
            runCatching {
                HoneyNotifyClient.get(applicationContext).trackReceived(message.data)
            }
        }.start()

        val title = message.notification?.title ?: message.data["title"]
        val body = message.notification?.body ?: message.data["body"]
        if (title != null || body != null) {
            showNotification(title ?: getString(R.string.app_name), body.orEmpty(), message.data)
        }
    }

    private fun showNotification(title: String, body: String, data: Map<String, String>) {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            data.forEach { (key, value) -> putExtra(key, value) }
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            data["honeynotify_notification_id"]?.hashCode() ?: 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(data["honeynotify_notification_id"]?.hashCode() ?: 1, notification)
    }

    companion object {
        private const val CHANNEL_ID = "honeynotify_default"
    }
}
