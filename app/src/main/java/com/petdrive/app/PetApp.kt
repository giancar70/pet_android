package com.petdrive.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.petdrive.app.core.notifications.REMINDER_NOTIFICATION_CHANNEL_ID
import com.petdrive.app.core.storage.OnboardingState
import com.petdrive.app.core.storage.PetPreferences
import com.petdrive.app.core.storage.TokenStore

class PetApp : Application() {
    override fun onCreate() {
        super.onCreate()
        TokenStore.init(applicationContext)
        OnboardingState.init(applicationContext)
        PetPreferences.init(applicationContext)
        createReminderNotificationChannel()
    }

    // Notification channels are required from API 26+; a notification posted without
    // one having been created first is silently dropped.
    private fun createReminderNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            REMINDER_NOTIFICATION_CHANNEL_ID,
            "Recordatorios",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Avisos cuando un recordatorio de tu mascota vence."
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
