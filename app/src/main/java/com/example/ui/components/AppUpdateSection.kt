package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.ui.theme.FilmwebGreen
import com.example.ui.theme.RoyalBlue
import com.example.util.AppUpdateInfo
import com.example.util.AppUpdateManager
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun AppUpdateSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var update by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var downloadedApk by remember { mutableStateOf<File?>(null) }
    var isChecking by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf("Sprawdzanie dostępności aktualizacji…") }
    var hasError by remember { mutableStateOf(false) }

    suspend fun checkForUpdates() {
        isChecking = true
        hasError = false
        try {
            val latest = AppUpdateManager.fetchLatestUpdate()
            update = latest.takeIf { it.versionCode > BuildConfig.VERSION_CODE }
            downloadedApk = null
            message = if (update != null) {
                "Dostępna jest nowa wersja KinoStream."
            } else {
                "Masz najnowszą wersję (${BuildConfig.VERSION_NAME})."
            }
        } catch (exception: Exception) {
            update = null
            hasError = true
            message = "Nie udało się sprawdzić aktualizacji. Sprawdź internet i spróbuj ponownie."
        } finally {
            isChecking = false
        }
    }

    LaunchedEffect(Unit) {
        checkForUpdates()
    }

    Card(
        modifier = modifier.testTag("card_app_updates"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Sync,
                    contentDescription = null,
                    tint = RoyalBlue,
                    modifier = Modifier.size(26.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Aktualizacje KinoStream",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Wersja zainstalowana: ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isChecking || isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = RoyalBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    hasError -> MaterialTheme.colorScheme.error
                    update != null -> RoyalBlue
                    downloadedApk != null -> FilmwebGreen
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            update?.let { latest ->
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Nowa wersja: ${latest.versionName} (kod ${latest.versionCode})",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (latest.changelog.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = latest.changelog,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isDownloading) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = progress / 100f,
                    modifier = Modifier.fillMaxWidth(),
                    color = RoyalBlue
                )
                Text(
                    text = "Pobieranie aktualizacji: $progress%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { scope.launch { checkForUpdates() } },
                    enabled = !isChecking && !isDownloading,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(if (isChecking) "Sprawdzanie…" else "Sprawdź ponownie")
                }

                if (update != null && downloadedApk == null) {
                    Button(
                        onClick = {
                            val selectedUpdate = update ?: return@Button
                            scope.launch {
                                isDownloading = true
                                progress = 0
                                hasError = false
                                try {
                                    downloadedApk = AppUpdateManager.downloadApk(
                                        context = context,
                                        update = selectedUpdate,
                                        onProgress = { progress = it }
                                    )
                                    message = "Aktualizacja pobrana i sprawdzona. Możesz ją zainstalować."
                                } catch (exception: Exception) {
                                    hasError = true
                                    message = exception.message
                                        ?: "Nie udało się pobrać aktualizacji."
                                } finally {
                                    isDownloading = false
                                }
                            }
                        },
                        enabled = !isChecking && !isDownloading,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Pobierz")
                    }
                }

                if (update != null && downloadedApk != null) {
                    Button(
                        onClick = {
                            try {
                                val openedInstaller = AppUpdateManager.requestInstall(
                                    context,
                                    downloadedApk ?: return@Button
                                )
                                message = if (openedInstaller) {
                                    "Otworzono instalator Androida."
                                } else {
                                    "Zezwól KinoStream na instalowanie nieznanych aplikacji, wróć tutaj i naciśnij „Zainstaluj”."
                                }
                                hasError = false
                            } catch (exception: Exception) {
                                hasError = true
                                message = exception.message ?: "Nie udało się uruchomić instalatora."
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Zainstaluj")
                    }
                }
            }
        }
    }
}
