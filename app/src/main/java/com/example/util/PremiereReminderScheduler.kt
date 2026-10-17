package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.model.Movie
import com.example.model.MovieRatingSource
import com.example.model.StreamingPlatform
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Schedules a reminder only for a future ISO date and a platform explicitly enabled by the user.
 * Metadata-only results are not scheduled because their regional availability is unverified.
 */
object PremiereReminderScheduler {
    private const val ACTION_PREFIX = "com.example.PREMIERE_REMINDER."
    private const val EXTRA_TITLE = "reminder_title"
    private const val EXTRA_MESSAGE = "reminder_message"
    private const val EXTRA_PLATFORM = "reminder_platform"
    private const val EXTRA_RATING = "reminder_rating"

    fun schedule(
        context: Context,
        movie: Movie,
        enabledPlatforms: Set<StreamingPlatform>
    ): Boolean {
        if (!movie.isAvailabilityVerified || !movie.hasPolishDubbing) return false
        if (!movie.isPremiere && !movie.isUpcoming) return false

        val premiereDate = movie.premiereDate?.takeIf { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }
            ?: return false
        val platform = movie.platforms.firstOrNull { it in enabledPlatforms } ?: return false
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
        val dateMillis = try {
            formatter.parse(premiereDate)?.time
        } catch (_: Exception) {
            null
        } ?: return false
        // Set the reminder for 10:00 local time on the premiere date.
        val triggerAtMillis = dateMillis + 10L * 60L * 60L * 1000L
        if (triggerAtMillis <= System.currentTimeMillis()) return false

        val intent = Intent(context, PremiereReminderReceiver::class.java).apply {
            action = ACTION_PREFIX + movie.id
            putExtra(EXTRA_TITLE, movie.displayTitle())
            putExtra(EXTRA_MESSAGE, "Premiera planowana na $premiereDate. Sprawdź dostępność w aplikacji.")
            putExtra(EXTRA_PLATFORM, platform.displayName)
            putExtra(
                EXTRA_RATING,
                movie.filmwebRating.takeIf { movie.ratingSource == MovieRatingSource.FILMWEB && it > 0.0 } ?: 0.0
            )
        }
        val requestCode = movie.id.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        return true
    }

    fun cancel(context: Context, movieId: String) {
        val intent = Intent(context, PremiereReminderReceiver::class.java).apply {
            action = ACTION_PREFIX + movieId
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            movieId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    internal fun readTitle(intent: Intent): String = intent.getStringExtra(EXTRA_TITLE).orEmpty()
    internal fun readMessage(intent: Intent): String = intent.getStringExtra(EXTRA_MESSAGE).orEmpty()
    internal fun readPlatform(intent: Intent): String =
        intent.getStringExtra(EXTRA_PLATFORM).orEmpty().ifBlank { "Nieznana platforma" }
    internal fun readRating(intent: Intent): Double = intent.getDoubleExtra(EXTRA_RATING, 0.0)
}
