package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.ProblemReport
import com.example.ui.theme.FilmwebGreen
import com.example.ui.theme.FilmwebRed
import com.example.ui.theme.RoyalBlue

@Composable
fun AdminPanelScreen(
    adminUserEmail: String?,
    reports: List<ProblemReport>,
    message: String?,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onSignOut: () -> Unit,
    onResolveReport: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen_admin_panel"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Text(
                    "Panel administratora",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Zgłoszenia błędów od użytkowników KinoStream",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Zalogowano: ${adminUserEmail ?: "administrator"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onRefresh,
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text("Odśwież")
                        }
                        OutlinedButton(onClick = onSignOut, shape = RoundedCornerShape(10.dp)) {
                            Text("Wyloguj")
                        }
                    }
                }
            }
        }

        message?.let { value ->
            item {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (
                        value.contains("nie udało", ignoreCase = true) ||
                        value.contains("brak", ignoreCase = true) ||
                        value.contains("sprawdź", ignoreCase = true)
                    ) FilmwebRed else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (reports.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.BugReport,
                            contentDescription = null,
                            tint = RoyalBlue,
                            modifier = Modifier.size(28.dp)
                        )
                        Text("Brak zgłoszeń", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Nowe zgłoszenia pojawią się tutaj automatycznie.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            item {
                Text(
                    "Ostatnie zgłoszenia (${reports.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(items = reports, key = { it.id }) { report ->
                ProblemReportCard(report = report, onResolve = { onResolveReport(report.id) })
            }
        }
    }
}

@Composable
private fun ProblemReportCard(
    report: ProblemReport,
    onResolve: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when (report.status) {
                        "resolved" -> "ROZWIĄZANE"
                        "in_progress" -> "W TRAKCIE"
                        else -> "NOWE"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (report.status == "resolved") FilmwebGreen else RoyalBlue
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    report.createdAt,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(report.description, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${report.device} • Android ${report.androidVersion} • KinoStream ${report.appVersion}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (report.context.isNotBlank()) {
                Text(
                    report.context,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (report.status != "resolved") {
                OutlinedButton(
                    onClick = onResolve,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Oznacz jako rozwiązane")
                }
            }
        }
    }
}
