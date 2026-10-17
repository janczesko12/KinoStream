package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class AppUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val sha256: String,
    val changelog: String
)

/** Downloads release metadata and verifies the APK before handing it to Android's installer. */
object AppUpdateManager {
    private const val RELEASE_API =
        "https://api.github.com/repos/janczesko12/KinoStream/releases/tags/latest"
    private const val EXPECTED_APK_PREFIX =
        "https://github.com/janczesko12/KinoStream/releases/download/latest/"

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .build()

    suspend fun fetchLatestUpdate(): AppUpdateInfo = withContext(Dispatchers.IO) {
        val releaseJson = getJson(RELEASE_API)
        val assets = releaseJson.optJSONArray("assets")
            ?: throw IOException("W wydaniu nie znaleziono listy plików.")

        var manifestUrl: String? = null
        for (index in 0 until assets.length()) {
            val asset = assets.optJSONObject(index) ?: continue
            if (asset.optString("name") == "update.json") {
                manifestUrl = asset.optString("browser_download_url").takeIf { it.isNotBlank() }
                break
            }
        }

        val url = manifestUrl
            ?: throw IOException("W najnowszym wydaniu brakuje pliku update.json.")
        val manifest = getJson(url)
        val apkUrl = manifest.optString("apkUrl")
        val checksum = manifest.optString("sha256").lowercase()

        if (!apkUrl.startsWith(EXPECTED_APK_PREFIX)) {
            throw IOException("Nieprawidłowy adres pliku aktualizacji.")
        }
        if (!checksum.matches(Regex("^[a-f0-9]{64}$"))) {
            throw IOException("Manifest aktualizacji zawiera nieprawidłową sumę kontrolną.")
        }

        AppUpdateInfo(
            versionCode = manifest.optInt("versionCode", -1).takeIf { it > 0 }
                ?: throw IOException("Nieprawidłowy numer wersji w manifeście."),
            versionName = manifest.optString("versionName").takeIf { it.isNotBlank() }
                ?: throw IOException("Brak nazwy wersji w manifeście."),
            apkUrl = apkUrl,
            sha256 = checksum,
            changelog = manifest.optString("changelog", "Poprawki i ulepszenia aplikacji.")
        )
    }

    private fun getJson(url: String): JSONObject {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "KinoStream-Android")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Serwer aktualizacji odpowiedział kodem \${response.code}.")
            }
            val body = response.body?.string()
                ?: throw IOException("Serwer zwrócił pustą odpowiedź.")
            return JSONObject(body)
        }
    }

    suspend fun downloadApk(
        context: Context,
        update: AppUpdateInfo,
        onProgress: (Int) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(update.apkUrl)
            .header("User-Agent", "KinoStream-Android")
            .build()

        val directory = File(context.cacheDir, "updates")
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("Nie można utworzyć katalogu pobierania.")
        }
        val temporaryFile = File(directory, "kinostream-update.apk.part")
        val apkFile = File(directory, "kinostream-update.apk")

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Pobieranie APK nie powiodło się (HTTP \${response.code}).")
                }
                val body = response.body ?: throw IOException("Brak pliku APK w odpowiedzi.")
                val totalBytes = body.contentLength()
                val digest = MessageDigest.getInstance("SHA-256")
                var downloadedBytes = 0L
                var lastReportedProgress = -1

                body.byteStream().use { input ->
                    temporaryFile.outputStream().buffered().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            digest.update(buffer, 0, read)
                            downloadedBytes += read
                            if (totalBytes > 0L) {
                                val progress = ((downloadedBytes * 100) / totalBytes)
                                    .toInt().coerceIn(0, 100)
                                if (progress != lastReportedProgress) {
                                    lastReportedProgress = progress
                                    withContext(Dispatchers.Main) { onProgress(progress) }
                                }
                            }
                        }
                    }
                }

                if (downloadedBytes == 0L) throw IOException("Pobrany plik APK jest pusty.")
                val actualChecksum = digest.digest().joinToString("") { "%02x".format(it) }
                if (!actualChecksum.equals(update.sha256, ignoreCase = true)) {
                    throw IOException("Weryfikacja APK nie powiodła się. Plik został odrzucony.")
                }
            }

            if (apkFile.exists() && !apkFile.delete()) {
                throw IOException("Nie można zastąpić poprzedniego pliku aktualizacji.")
            }
            if (!temporaryFile.renameTo(apkFile)) {
                throw IOException("Nie udało się przygotować APK do instalacji.")
            }
            withContext(Dispatchers.Main) { onProgress(100) }
            apkFile
        } catch (exception: Exception) {
            temporaryFile.delete()
            throw exception
        }
    }

    /** Returns false when Android settings must be used to grant package-install permission first. */
    fun requestInstall(context: Context, apkFile: File): Boolean {
        if (!apkFile.isFile) throw IOException("Najpierw pobierz aktualizację.")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val settingsIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:\${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(settingsIntent)
            return false
        }

        val apkUri = FileProvider.getUriForFile(
            context,
            "\${context.packageName}.fileprovider",
            apkFile
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(installIntent)
        return true
    }
}
