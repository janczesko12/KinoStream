package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

object NotificationHelper {
    private const val CHANNEL_ID = "movie_premieres_channel"
    private const val CHANNEL_NAME = "Premiery Filmowe (Netflix, Max, Disney+)"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Powiadomienia o nowych premierach filmowych z wybranych platform streamingowych"
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendPremiereNotification(
        context: Context,
        title: String,
        message: String,
        platformName: String,
        filmwebRating: Double
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("🎬 Nowa premiera na $platformName: $title")
            .setContentText("$message • Ocena Filmweb: $filmwebRating/10")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$message\n\nPlatforma: $platformName\nOcena społeczności Filmweb: $filmwebRating/10 ⭐")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        } catch (_: SecurityException) {
            // Android 13+ POST_NOTIFICATIONS permission not granted
        }
    }
}
