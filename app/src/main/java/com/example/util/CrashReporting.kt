package com.example.util

import android.content.Context
import android.os.Build
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Initializes Crashlytics only when the app was built with a valid Firebase Android config.
 * The app must remain usable in preview/local builds that do not have google-services.json.
 */
object CrashReporting {
    fun initialize(context: Context) {
        val app = runCatching { FirebaseApp.initializeApp(context.applicationContext) }
            .getOrNull() ?: return

        runCatching {
            FirebaseCrashlytics.getInstance(app).apply {
                setCrashlyticsCollectionEnabled(true)
                setCustomKey("app_name", "KinoStream")
                setCustomKey("app_version", BuildConfig.VERSION_NAME)
                setCustomKey("android_version", Build.VERSION.RELEASE ?: "unknown")
                setCustomKey("device_manufacturer", Build.MANUFACTURER ?: "unknown")
                setCustomKey("device_model", Build.MODEL ?: "unknown")
            }
        }
    }

    fun recordNonFatal(context: Context, error: Throwable) {
        runCatching {
            val app = FirebaseApp.getApps(context.applicationContext)
                .firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }
                ?: return
            FirebaseCrashlytics.getInstance(app).recordException(error)
        }
    }
}
