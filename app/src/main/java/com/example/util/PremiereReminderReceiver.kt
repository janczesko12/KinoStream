package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PremiereReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = PremiereReminderScheduler.readTitle(intent)
        if (title.isBlank()) return

        NotificationHelper.sendPremiereNotification(
            context = context,
            title = title,
            message = PremiereReminderScheduler.readMessage(intent),
            platformName = PremiereReminderScheduler.readPlatform(intent),
            filmwebRating = PremiereReminderScheduler.readRating(intent)
        )
    }
}
