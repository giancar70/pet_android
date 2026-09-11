package com.petapp.android.core.notifications

import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.petapp.android.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val REMINDER_NOTIFICATION_CHANNEL_ID = "recordatorios"

/// FCM notification-payload messages (see the backend's apps/notifications/push.py)
/// auto-display via the system tray when the app is backgrounded/killed, but only
/// reach onMessageReceived -- and need to be shown manually -- when the app is in the
/// foreground. This mirrors that: the channel/permission setup lives in PetApp.kt.
class PetFirebaseMessagingService : FirebaseMessagingService() {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onNewToken(token: String) {
        scope.launch { PushTokenManager.registerToken(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: return
        val body = message.notification?.body ?: message.data["body"].orEmpty()
        val manager = NotificationManagerCompat.from(this)
        // Guards the API 33+ POST_NOTIFICATIONS runtime permission: notify() throws a
        // SecurityException if the user denied it, which would otherwise crash this
        // background service.
        if (!manager.areNotificationsEnabled()) return
        val notification = NotificationCompat.Builder(this, REMINDER_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
