package com.petdrive.app.core.notifications

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.petdrive.app.R
import com.petdrive.app.core.network.ApiClient
import com.petdrive.app.core.network.ApiEndpoints
import kotlinx.serialization.Serializable

const val OVERDUE_NOTIFICATION_CHANNEL_ID = "registros_vencidos"
private const val OVERDUE_NOTIFICATION_ID = 1001

@Serializable
private data class OverdueCountResponse(val count: Int)

// Android has no OS API to set a bare icon badge number with no visible notification --
// badges are derived from the app's active notifications (Notification.setNumber()), so
// showing a count on the launcher icon means keeping exactly one silent, low-importance
// notification around whose number tracks the backend's aggregate overdue count (see
// apps/pet/views.py's OverdueCountView), replacing it in place as the count changes and
// canceling it outright once nothing is overdue. iOS has a direct API for this instead
// (see OverdueBadgeManager.swift).
object OverdueBadgeManager {
    // Called on logout/account deletion/token expiry -- a shared/reused device shouldn't
    // keep showing a previous account's overdue count once it's no longer signed in.
    fun clear(context: Context) {
        NotificationManagerCompat.from(context).cancel(OVERDUE_NOTIFICATION_ID)
    }

    suspend fun refresh(context: Context) {
        val count = runCatching { ApiClient.get<OverdueCountResponse>(ApiEndpoints.PET_OVERDUE_COUNT) }.getOrNull()?.count ?: return
        val manager = NotificationManagerCompat.from(context)
        if (count <= 0) {
            manager.cancel(OVERDUE_NOTIFICATION_ID)
            return
        }
        if (!manager.areNotificationsEnabled()) return
        val label = if (count == 1) "Tienes 1 registro vencido" else "Tienes $count registros vencidos"
        val notification = NotificationCompat.Builder(context, OVERDUE_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentTitle(label)
            .setContentText("Revisa las vacunas o desparasitaciones vencidas de tus mascotas.")
            .setNumber(count)
            .setOngoing(false)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        manager.notify(OVERDUE_NOTIFICATION_ID, notification)
    }
}
