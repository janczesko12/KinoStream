package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.StreamingPlatform
import com.example.ui.components.FilmwebRatingBadge
import com.example.ui.components.AppUpdateSection
import com.example.ui.components.SupportFooterSection
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.FilmwebGreen
import com.example.ui.theme.FilmwebRed
import com.example.ui.theme.FilmwebYellow
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.ThemeMode
import java.util.Locale

@Composable
fun SettingsScreen(
    currentThemeMode: ThemeMode,
    notificationsEnabled: Boolean,
    enabledNotificationPlatforms: Set<StreamingPlatform>,
    minFilmwebRating: Double = 6.5,
    maxFilmwebRating: Double = 10.0,
    autoAddNewReleasesEnabled: Boolean = true,
    autoSyncedCount: Int = 0,
    isSyncingNewReleases: Boolean = false,
    usePolishTitles: Boolean = true,
    onThemeModeChange: (ThemeMode) -> Unit,
    onNotificationsEnabledChange: (Boolean) -> Unit,
    onToggleNotificationPlatform: (StreamingPlatform) -> Unit,
    onSendTestNotification: () -> Unit,
    onFilmwebRatingRangeChange: (Double, Double) -> Unit = { _, _ -> },
    onAutoAddNewReleasesChange: (Boolean) -> Unit = {},
    onSyncNewReleasesNow: () -> Unit = {},
    onUsePolishTitlesChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onSendTestNotification()
        }
    }

    var minInputText by remember { mutableStateOf(String.format(Locale.US, "%.1f", minFilmwebRating)) }
    var maxInputText by remember { mutableStateOf(String.format(Locale.US, "%.1f", maxFilmwebRating)) }
    var inputError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(minFilmwebRating, maxFilmwebRating) {
        minInputText = String.format(Locale.US, "%.1f", minFilmwebRating)
        maxInputText = String.format(Locale.US, "%.1f", maxFilmwebRating)
    }

    LazyColumn(
        modifier = modifier
            .testTag("screen_settings")
            .fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                RoyalBlue.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Column {
                    Text(
                        text = "Ustawienia",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Język tytułów (EN/PL), przedział oceny Filmweb, premiery VOD i komfort oczu",
                        style = MaterialTheme.typography.bodySmall,
                        color = RoyalBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // App updates are checked whenever the user opens Settings.
        item {
            AppUpdateSection(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
            )
        }

        // Section 0: Movie Title Language from Polish Streaming (English/Native vs Polish Streaming)
        item {
            Card(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .testTag("card_title_language_settings"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(
                        listOf(RoyalBlue.copy(alpha = 0.6f), AccentCyan.copy(alpha = 0.4f))
                    )
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Nazwy z Polskiego Streamingu (PL / NAT)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tytuły zaciągane z polskiego streamingu — pozycje nietłumaczone (np. Breaking Bad, The Boys, Fallout) zostają bez zmian w obu trybach",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = usePolishTitles,
                            onCheckedChange = onUsePolishTitlesChange,
                            modifier = Modifier.testTag("switch_polish_titles"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = RoyalBlue
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Option 1: Polish Streaming Titles (Default)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("option_polish_titles")
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (usePolishTitles) RoyalBlue.copy(alpha = 0.12f) else Color.Transparent)
                            .border(
                                1.dp,
                                if (usePolishTitles) RoyalBlue.copy(alpha = 0.5f) else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { onUsePolishTitlesChange(true) }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🇵🇱 Polskie nazwy ze streamingów (Domyślnie)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (usePolishTitles) FontWeight.Bold else FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Zaciągane z polskich platform VOD (np. Incepcja, Wiedźmin, Ród Smoka, Diuna: Część druga, a nietłumaczone jak Breaking Bad czy The Boys zostają bez zmian)",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        RadioButton(
                            selected = usePolishTitles,
                            onClick = { onUsePolishTitlesChange(true) },
                            colors = RadioButtonDefaults.colors(selectedColor = RoyalBlue)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Option 2: Native / Original Titles
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("option_english_titles")
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!usePolishTitles) RoyalBlue.copy(alpha = 0.12f) else Color.Transparent)
                            .border(
                                1.dp,
                                if (!usePolishTitles) RoyalBlue.copy(alpha = 0.5f) else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { onUsePolishTitlesChange(false) }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🇬🇧 Oryginalne / Natywne nazwy (EN)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (!usePolishTitles) FontWeight.Bold else FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Np. Inception, The Witcher, House of the Dragon, Dune: Part Two, Breaking Bad, The Boys",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        RadioButton(
                            selected = !usePolishTitles,
                            onClick = { onUsePolishTitlesChange(false) },
                            colors = RadioButtonDefaults.colors(selectedColor = RoyalBlue)
                        )
                    }
                }
            }
        }

        // Section 1: Custom Filmweb Rating Range ("ustawienie przedziału oceny na filmweb ze mozna wpisac swoją według uznania")
        item {
            Card(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .testTag("card_filmweb_range_settings"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(
                        listOf(RoyalBlue.copy(alpha = 0.6f), FilmwebYellow.copy(alpha = 0.4f))
                    )
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = FilmwebYellow,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Przedział Oceny Filmweb",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Wpisz własny przedział według uznania (filmy poza zakresem mają ocenę na czerwono)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Custom Min & Max Text Inputs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = minInputText,
                            onValueChange = {
                                minInputText = it
                                inputError = null
                            },
                            label = { Text("Min ocena (od)", fontSize = 12.sp) },
                            placeholder = { Text("6.5") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_min_filmweb_rating"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RoyalBlue,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                        )

                        Text(
                            text = "—",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = maxInputText,
                            onValueChange = {
                                maxInputText = it
                                inputError = null
                            },
                            label = { Text("Max ocena (do)", fontSize = 12.sp) },
                            placeholder = { Text("10.0") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_max_filmweb_rating"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RoyalBlue,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                        )
                    }

                    if (inputError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = inputError!!,
                            color = FilmwebRed,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Presets Row
                    Text(
                        text = "Szybki wybór przedziału:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val presets = listOf(
                        Triple("Domyślny (6.5 - 10)", 6.5, 10.0),
                        Triple("Dobre (7.0 - 10)", 7.0, 10.0),
                        Triple("Hity (7.5 - 10)", 7.5, 10.0),
                        Triple("Arcydzieła (8.0 - 10)", 8.0, 10.0),
                        Triple("Średniaki (5.0 - 6.9)", 5.0, 6.9)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.forEach { (label, presetMin, presetMax) ->
                            val isSelected = minFilmwebRating == presetMin && maxFilmwebRating == presetMax
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) RoyalBlue
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                    )
                                    .clickable {
                                        minInputText = String.format(Locale.US, "%.1f", presetMin)
                                        maxInputText = String.format(Locale.US, "%.1f", presetMax)
                                        inputError = null
                                        onFilmwebRatingRangeChange(presetMin, presetMax)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Save Button & Reset Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val parsedMin = minInputText.replace(',', '.').trim().toDoubleOrNull()
                                val parsedMax = maxInputText.replace(',', '.').trim().toDoubleOrNull()
                                when {
                                    parsedMin == null || parsedMax == null -> {
                                        inputError = "Wpisz poprawne liczby (np. 6.5 oraz 10.0)"
                                    }
                                    parsedMin < 0.0 || parsedMax > 10.0 || parsedMin > parsedMax -> {
                                        inputError = "Zakres musi mieścić się w przedziale 0.0 – 10.0 (Min ≤ Max)"
                                    }
                                    else -> {
                                        inputError = null
                                        onFilmwebRatingRangeChange(parsedMin, parsedMax)
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_save_filmweb_range"),
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Zapisz własny przedział",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                minInputText = "6.5"
                                maxInputText = "10.0"
                                inputError = null
                                onFilmwebRatingRangeChange(6.5, 10.0)
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Reset (6.5)", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Live Preview of Current Range
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "Aktywny przedział: %.1f – %.1f",
                                    minFilmwebRating,
                                    maxFilmwebRating
                                ),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = RoyalBlue
                            )
                            Text(
                                text = "Oceny poza tym przedziałem są czerwone",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilmwebRatingBadge(
                                rating = minFilmwebRating,
                                minRating = minFilmwebRating,
                                maxRating = maxFilmwebRating,
                                compact = true
                            )
                            val exampleOutside = if (minFilmwebRating > 1.0) minFilmwebRating - 0.5 else 0.0
                            FilmwebRatingBadge(
                                rating = exampleOutside,
                                minRating = minFilmwebRating,
                                maxRating = maxFilmwebRating,
                                compact = true
                            )
                        }
                    }
                }
            }
        }

        // Section 2: Automatic Adding of New Releases ("automatyczne dodawanie po tym jak jakis wyjdzie na netflix prime video max disney +")
        item {
            Card(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .testTag("card_auto_add_releases"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(
                        listOf(RoyalBlue.copy(alpha = 0.5f), FilmwebGreen.copy(alpha = 0.4f))
                    )
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = RoyalBlue,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Automatyczne Dodawanie Premier",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Automatycznie dodawaj nowe filmy i seriale po premierze na Netflix, Prime Video, Max, Disney+ i SkyShowtime",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = autoAddNewReleasesEnabled,
                            onCheckedChange = onAutoAddNewReleasesChange,
                            modifier = Modifier.testTag("switch_auto_add_releases"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = RoyalBlue
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (autoSyncedCount > 0) {
                        Text(
                            text = "✔️ Automatycznie pobrano i dodano do katalogu: $autoSyncedCount nowości VOD",
                            style = MaterialTheme.typography.bodySmall,
                            color = FilmwebGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Button(
                        onClick = onSyncNewReleasesNow,
                        enabled = !isSyncingNewReleases,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_settings_sync_now"),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Sync,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSyncingNewReleases) {
                                "Sprawdzanie premier na 5 platformach VOD..."
                            } else {
                                "Sprawdź i dodaj najnowsze premiery teraz"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Section 3: Dark Mode for Eye Comfort ("tryb ciemny dla komfortu oczu")
        item {
            Card(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(
                        listOf(RoyalBlue.copy(alpha = 0.5f), AccentCyan.copy(alpha = 0.3f))
                    )
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DarkMode,
                            contentDescription = null,
                            tint = RoyalBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Komfort Oczu (Tryb Ciemny)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Głębokie ciemnoniebieskie tony redukujące zmęczenie wzroku",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Theme options
                    ThemeOptionRow(
                        title = "Tryb Ciemny (Komfort Oczu)",
                        subtitle = "Ciemnoniebieski motyw z wysokim kontrastem i łagodnym światłem",
                        icon = Icons.Filled.DarkMode,
                        isSelected = currentThemeMode == ThemeMode.DARK,
                        onClick = { onThemeModeChange(ThemeMode.DARK) }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    ThemeOptionRow(
                        title = "Tryb Jasny",
                        subtitle = "Klasyczny jasny interfejs dzienny z błękitnymi akcentami",
                        icon = Icons.Filled.LightMode,
                        isSelected = currentThemeMode == ThemeMode.LIGHT,
                        onClick = { onThemeModeChange(ThemeMode.LIGHT) }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    ThemeOptionRow(
                        title = "Zgodny z systemem",
                        subtitle = "Automatyczne dostosowanie do ustawień systemowych Androida",
                        icon = Icons.Filled.SettingsBrightness,
                        isSelected = currentThemeMode == ThemeMode.SYSTEM,
                        onClick = { onThemeModeChange(ThemeMode.SYSTEM) }
                    )
                }
            }
        }

        // Section 4: Notifications for Movie Premieres ("obsługa powiadomień o nowych premierach z wybranych platform")
        item {
            Card(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(
                        listOf(RoyalBlue.copy(alpha = 0.4f), Color.Transparent)
                    )
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = null,
                                tint = RoyalBlue,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Powiadomienia o Premierach",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Bądź na bieżąco z nowościami z 5 platform VOD",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = onNotificationsEnabledChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = RoyalBlue
                            )
                        )
                    }

                    if (notificationsEnabled) {
                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Wybierz platformy dla powiadomień:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        StreamingPlatform.values().forEach { platform ->
                            val isEnabled = enabledNotificationPlatforms.contains(platform)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onToggleNotificationPlatform(platform) }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(platform.brandColor, RoundedCornerShape(3.dp))
                                    )
                                    Text(
                                        text = platform.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Switch(
                                    checked = isEnabled,
                                    onCheckedChange = { onToggleNotificationPlatform(platform) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = platform.brandColor
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onSendTestNotification()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_trigger_test_notification"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RoyalBlue
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Wyślij testowe powiadomienie o premierze",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Section 5: Filmweb Ratings Rule Guide
        item {
            Card(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = RoyalBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Zasady Ocen Filmweb",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "KinoStream podświetla oceny zgodnie z Twoim wybranym przedziałem (${String.format(Locale.US, "%.1f – %.1f", minFilmwebRating, maxFilmwebRating)}):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Rating within range
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(10.dp)
                    ) {
                        FilmwebRatingBadge(
                            rating = minFilmwebRating,
                            minRating = minFilmwebRating,
                            maxRating = maxFilmwebRating,
                            compact = true
                        )
                        Column {
                            Text(
                                text = String.format(Locale.US, "Ocena w przedziale %.1f – %.1f", minFilmwebRating, maxFilmwebRating),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Film mieści się w Twoim przedziale rekomendacji.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Rating outside range -> RED text
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(FilmwebRed.copy(alpha = 0.10f))
                            .padding(10.dp)
                    ) {
                        val lowExample = if (minFilmwebRating > 1.0) minFilmwebRating - 0.8 else 0.0
                        FilmwebRatingBadge(
                            rating = lowExample,
                            minRating = minFilmwebRating,
                            maxRating = maxFilmwebRating,
                            compact = true
                        )
                        Column {
                            Text(
                                text = "Ocena poza przedziałem (Tekst na czerwono)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = FilmwebRed
                            )
                            Text(
                                text = "Zgodnie z wymaganiem ocena i ostrzeżenie są wyświetlane na czerwono.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Section 6: Support Footer ("Postaw mi kawę" + "Created with ❤️ by: Jan Czesko")
        item {
            SupportFooterSection(
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
            )
        }
    }
}

@Composable
fun ThemeOptionRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) RoyalBlue.copy(alpha = 0.12f) else Color.Transparent)
            .border(
                1.dp,
                if (isSelected) RoyalBlue.copy(alpha = 0.5f) else Color.Transparent,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) RoyalBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = RoyalBlue
            )
        )
    }
}
