package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.RoyalBlue

@Immutable
private data class FreeFilmSource(
    val name: String,
    val subtitle: String,
    val description: String,
    val url: String
)

private val freeFilmSources = listOf(
    FreeFilmSource(
        name = "Ninateka",
        subtitle = "Bezpłatnie • bez logowania i reklam",
        description = "Filmy fabularne i dokumentalne, animacje, reportaże oraz materiały archiwalne z kolekcji Filmoteki Narodowej – Instytutu Audiowizualnego.",
        url = "https://ninateka.pl/"
    ),
    FreeFilmSource(
        name = "35mm.online",
        subtitle = "Polskie kino i archiwalia",
        description = "Platforma VOD WFDiF z polskimi filmami fabularnymi, dokumentami, kronikami filmowymi i animacjami.",
        url = "https://35mm.online/"
    ),
    FreeFilmSource(
        name = "TVP VOD",
        subtitle = "Wybrane tytuły bez opłat",
        description = "W katalogu znajdują się także bezpłatne filmy i seriale. Sprawdź oznaczenie konkretnego tytułu — część materiałów wymaga opłaty.",
        url = "https://vod.tvp.pl/"
    ),
    FreeFilmSource(
        name = "ARTE.tv",
        subtitle = "Kino europejskie i dokumenty",
        description = "Filmy, dokumenty i reportaże. Dostępność poszczególnych materiałów może zależeć od kraju i okresu udostępnienia.",
        url = "https://www.arte.tv/pl/"
    )
)

@Composable
fun PremiumScreen() {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(RoyalBlue.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = AccentCyan
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Premium",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Darmowe filmy z legalnych źródeł",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = RoyalBlue.copy(alpha = 0.11f)
            )
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Co obejrzysz?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Na start zebraliśmy darmowe biblioteki filmowe, dokumenty i polskie archiwalia. Wybierz źródło, aby przejść do jego oficjalnego katalogu.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 21.sp
                )
            }
        }

        freeFilmSources.forEach { source ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(11.dp)
                                )
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Movie,
                                contentDescription = null,
                                tint = RoyalBlue
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = source.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = source.subtitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = AccentCyan
                            )
                        }
                    }

                    Text(
                        text = source.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )

                    Button(
                        onClick = {
                            runCatching { uriHandler.openUri(source.url) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.OpenInNew,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Otwórz katalog")
                    }
                }
            }
        }

        Text(
            text = "KinoStream nie hostuje tych filmów — odtwarzanie odbywa się na oficjalnych stronach. Darmowa oferta i dostępność tytułów mogą się zmieniać.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp,
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
        )
    }
}
