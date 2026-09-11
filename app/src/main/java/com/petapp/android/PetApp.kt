package com.petapp.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.petapp.android.core.notifications.REMINDER_NOTIFICATION_CHANNEL_ID
import com.petapp.android.core.storage.OnboardingState
import com.petapp.android.core.storage.PetPreferences
import com.petapp.android.core.storage.TokenStore

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
