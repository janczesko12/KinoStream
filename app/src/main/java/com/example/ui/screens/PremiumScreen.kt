package com.example.ui.screens

import android.graphics.Color as AndroidColor
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage

private data class FreeMovie(
    val id: String,
    val title: String,
    val year: Int,
    val duration: String,
    val genre: String,
    val description: String,
    val streamUrl: String,
    val posterUrl: String,
    val sourceUrl: String,
    val credit: String,
    val license: String,
    val licenseUrl: String,
    val colors: List<Color>
)

private val freeMovies = listOf(
    FreeMovie(
        id = "big-buck-bunny",
        title = "Big Buck Bunny",
        year = 2008,
        duration = "9 min 56 s",
        genre = "Animacja • Komedia",
        description = "Wielki, łagodny królik ma dość zaczepek trzech gryzoni z lasu. Zabawna animacja pełna pościgów i leśnych przygód.",
        streamUrl = "https://video.blender.org/object-storage/web_videos/bf1f3fb5-b119-4f9f-9930-8e20e892b898-720.mp4",
        posterUrl = "https://video.blender.org/lazy-static/thumbnails/bf1f3fb5-b119-4f9f-9930-8e20e892b898.jpg",
        sourceUrl = "https://peach.blender.org/",
        credit = "Big Buck Bunny (2008) © Blender Foundation",
        license = "CC BY 3.0",
        licenseUrl = "https://creativecommons.org/licenses/by/3.0/",
        colors = listOf(Color(0xFF164E38), Color(0xFF071B23))
    ),
    FreeMovie(
        id = "sintel",
        title = "Sintel",
        year = 2010,
        duration = "14 min 48 s",
        genre = "Fantasy • Przygodowy",
        description = "Samotna wędrowniczka przemierza niebezpieczną krainę, by odnaleźć smoczątko, z którym połączyła ją niezwykła więź.",
        streamUrl = "https://video.blender.org/object-storage/web_videos/0eb052d0-fd51-43e6-aa33-ecdbf77a5d40-818.mp4",
        posterUrl = "https://studio.blender.org/files/public/thumbnail/03/f7/03f7b0d15c55b428680d4c6bb313f742_m.webp",
        sourceUrl = "https://durian.blender.org/",
        credit = "Sintel (2010) © Blender Foundation",
        license = "CC BY 3.0",
        licenseUrl = "https://creativecommons.org/licenses/by/3.0/",
        colors = listOf(Color(0xFF8F3E30), Color(0xFF161F35))
    ),
    FreeMovie(
        id = "elephants-dream",
        title = "Elephants Dream",
        year = 2006,
        duration = "10 min 54 s",
        genre = "Sci-fi • Surrealistyczny",
        description = "Dwóch mężczyzn przemierza ogromny, tajemniczy świat maszyn. Starszy z nich wierzy, że sam go stworzył.",
        streamUrl = "https://video.blender.org/object-storage/web_videos/cccc3e60-0291-4ecc-aa56-39b2e2c7d0d5-720.mp4",
        posterUrl = "https://video.blender.org/lazy-static/thumbnails/cccc3e60-0291-4ecc-aa56-39b2e2c7d0d5.jpg",
        sourceUrl = "https://orange.blender.org/",
        credit = "Elephants Dream (2006) © Blender Foundation / Netherlands Media Art Institute",
        license = "CC BY 2.5",
        licenseUrl = "https://creativecommons.org/licenses/by/2.5/",
        colors = listOf(Color(0xFF525E72), Color(0xFF161925))
    ),
    FreeMovie(
        id = "spring",
        title = "Spring",
        year = 2019,
        duration = "7 min 44 s",
        genre = "Fantasy • Animacja",
        description = "Pasterka i jej pies spotykają pradawne duchy. Muszą zmierzyć się z niezwykłą siłą natury, aby zachować cykl życia.",
        streamUrl = "https://video.blender.org/object-storage/web_videos/3d95fb3d-c866-42c8-9db1-fe82f48ccb95-804.mp4",
        posterUrl = "https://studio.blender.org/files/public/thumbnail/ea/65/ea65feaacf95911f39844e2ea97e68b5_m.webp",
        sourceUrl = "https://studio.blender.org/projects/spring/",
        credit = "Spring (2019) © Blender Foundation",
        license = "CC BY 4.0",
        licenseUrl = "https://creativecommons.org/licenses/by/4.0/",
        colors = listOf(Color(0xFF476B52), Color(0xFF172B35))
    ),
    FreeMovie(
        id = "coffee-run",
        title = "Coffee Run",
        year = 2020,
        duration = "Krótkometrażowy",
        genre = "Animacja • Obyczajowy",
        description = "Nadmiar kawy napędza młodą kobietę w podróży przez wspomnienia dawnego związku.",
        streamUrl = "https://video.blender.org/object-storage/web_videos/ff8fe61b-026f-4f07-b66b-2a790d6f6ab1-804.mp4",
        posterUrl = "https://studio.blender.org/files/public/thumbnail/7a/43/7a43fa6b0bc44e62f1cbb21473800e07_m.webp",
        sourceUrl = "https://studio.blender.org/projects/coffee-run/",
        credit = "Coffee Run (2020) © Blender Foundation",
        license = "CC BY 4.0",
        licenseUrl = "https://creativecommons.org/licenses/by/4.0/",
        colors = listOf(Color(0xFF8E5E43), Color(0xFF261D2C))
    ),
    FreeMovie(
        id = "wing-it",
        title = "Wing It!",
        year = 2023,
        duration = "3 min 58 s",
        genre = "Animacja • Komedia",
        description = "Zbyt pewny siebie inżynier spotyka entuzjastycznego niedoszłego pilota. Ich kosmiczny lot szybko wymyka się spod kontroli.",
        streamUrl = "https://video.blender.org/object-storage/web_videos/1be4c93c-1fd2-498d-adc7-50126823033e-480.mp4",
        posterUrl = "https://studio.blender.org/files/public/thumbnail/c2/6d/c26d3b56aeb6bc9bf1ba6ee8e27b7ccd_m.webp",
        sourceUrl = "https://studio.blender.org/projects/wing-it/",
        credit = "Wing It! (2023) © Blender Foundation",
        license = "CC BY 4.0",
        licenseUrl = "https://creativecommons.org/licenses/by/4.0/",
        colors = listOf(Color(0xFF2C6991), Color(0xFF161E35))
    ),
    FreeMovie(
        id = "glass-half",
        title = "Glass Half",
        year = 2015,
        duration = "3 min 13 s",
        genre = "Animacja • Komedia",
        description = "Dwóch krytyków sztuki w niezrozumiały sposób spiera się o obrazy w galerii. Ich rozmowa szybko wymyka się spod kontroli.",
        streamUrl = "https://video.blender.org/object-storage/web_videos/64222c8a-c4c7-4b3b-9850-7fb2078edcf6-1080.mp4",
        posterUrl = "https://studio.blender.org/files/public/thumbnail/c6/3d/c63ddc47dd56cf369557e969f15e2cb644aa421a_m.webp",
        sourceUrl = "https://studio.blender.org/projects/glass-half/",
        credit = "Glass Half (2015) © Blender Foundation",
        license = "CC BY 3.0",
        licenseUrl = "https://creativecommons.org/licenses/by/3.0/",
        colors = listOf(Color(0xFF9B6239), Color(0xFF2A1C2D))
    ),
    FreeMovie(
        id = "caminandes-gran-dillama",
        title = "Caminandes: Gran Dillama",
        year = 2013,
        duration = "2 min 26 s",
        genre = "Animacja • Komedia",
        description = "Lama Koro próbuje dostać się na drugą stronę płotu, by zjeść soczystą trawę. Prosta sprawa staje się komiczną przygodą.",
        streamUrl = "https://video.blender.org/object-storage/web_videos/fb70d459-48d2-4db5-adba-813c84f9200a-1080.mp4",
        posterUrl = "https://studio.blender.org/files/public/thumbnail/80/4b/804beb324d82e98db44d8308d9e6f495_m.webp",
        sourceUrl = "https://caminandes.com/",
        credit = "Caminandes 2: Gran Dillama (2013) © caminandes.com / Blender Institute",
        license = "CC BY 3.0",
        licenseUrl = "https://creativecommons.org/licenses/by/3.0/",
        colors = listOf(Color(0xFF7F6D39), Color(0xFF1A2926))
    )
)

@Composable
fun PremiumScreen() {
    var selectedMovie by remember { mutableStateOf<FreeMovie?>(null) }

    BackHandler(enabled = selectedMovie != null) {
        selectedMovie = null
    }

    if (selectedMovie == null) {
        PremiumMovieCatalog(onWatch = { selectedMovie = it })
    } else {
        PremiumMoviePlayer(
            movie = selectedMovie!!,
            onBack = { selectedMovie = null }
        )
    }
}

@Composable
private fun PremiumMovieCatalog(onWatch: (FreeMovie) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF1769D2), Color(0xFF00BCD4)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(27.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Premium",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Filmy do obejrzenia",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF10233C))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF66D9EF),
                            modifier = Modifier.size(28.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = "Oglądaj za darmo",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Wybierz film i odtwórz go bezpośrednio w KinoStream. Płatności na razie nie ma.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFD2DCE9),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                Text(
                    text = "Darmowe Open Movies",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Otwarte filmy i animacje udostępnione przez Blender Open Movies.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(freeMovies, key = { it.id }) { movie ->
            FreeMovieCard(movie = movie, onWatch = { onWatch(movie) })
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = "Katalog zawiera bezpłatne filmy z projektów Blender Open Movies, a nie płatne produkcje z Netflixa czy Disney+. Materiały są odtwarzane bezpośrednio z serwerów Blender Video. Informacje o autorach i licencjach pokazujemy przy każdym tytule.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun FreeMovieCard(movie: FreeMovie, onWatch: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(188.dp)
                    .background(Brush.verticalGradient(movie.colors))
            ) {
                Icon(
                    imageVector = Icons.Filled.Movie,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.28f),
                    modifier = Modifier
                        .size(58.dp)
                        .align(Alignment.Center)
                )
                AsyncImage(
                    model = movie.posterUrl,
                    contentDescription = "Plakat filmu ${movie.title}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color(0xE6000000))))
                        .padding(horizontal = 9.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "DARMOWY FILM",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp,
                        letterSpacing = 0.7.sp
                    )
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    minLines = 2
                )
                Text(
                    text = "${movie.year} • ${movie.duration}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = movie.genre,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF42B7D8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = movie.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    minLines = 3,
                    lineHeight = 17.sp
                )
                Button(
                    onClick = onWatch,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Oglądaj", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PremiumMoviePlayer(movie: FreeMovie, onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val webViewHolder = remember(movie.id) { arrayOfNulls<WebView>(1) }

    DisposableEffect(movie.id) {
        onDispose {
            webViewHolder[0]?.let { webView ->
                runCatching {
                    webView.stopLoading()
                    webView.loadUrl("about:blank")
                    webView.destroy()
                }
            }
            webViewHolder[0] = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Wróć do filmów"
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${movie.year} • ${movie.duration} • Darmowy",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        setBackgroundColor(AndroidColor.BLACK)
                        settings.javaScriptEnabled = false
                        settings.domStorageEnabled = false
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                        webChromeClient = WebChromeClient()
                        tag = movie.id
                        webViewHolder[0] = this
                        loadDataWithBaseURL(
                            "https://video.blender.org/",
                            moviePlayerHtml(movie),
                            "text/html",
                            "UTF-8",
                            null
                        )
                    }
                },
                update = { webView ->
                    if (webView.tag != movie.id) {
                        webView.tag = movie.id
                        webView.loadDataWithBaseURL(
                            "https://video.blender.org/",
                            moviePlayerHtml(movie),
                            "text/html",
                            "UTF-8",
                            null
                        )
                    }
                }
            )
        }

        Text(
            text = movie.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 23.sp
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
        ) {
            Column(
                modifier = Modifier.padding(13.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = "Autorstwo",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = movie.credit,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Licencja: ${movie.license}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = { runCatching { uriHandler.openUri(movie.sourceUrl) } }) {
                        Icon(
                            imageVector = Icons.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Źródło filmu")
                    }
                    TextButton(onClick = { runCatching { uriHandler.openUri(movie.licenseUrl) } }) {
                        Text("Warunki licencji")
                    }
                }
            }
        }
        Text(
            text = "Odtwarzamy plik z serwera Blender Video w oryginalnej postaci. Jeśli wideo nie uruchomi się, sprawdź połączenie z internetem lub otwórz stronę źródłową.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp
        )
    }
}

private fun moviePlayerHtml(movie: FreeMovie): String = """
    <!doctype html>
    <html>
      <head>
        <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
        <style>
          html, body { margin: 0; padding: 0; width: 100%; height: 100%; background: #000; overflow: hidden; }
          video { display: block; width: 100%; height: 100%; object-fit: contain; background: #000; }
        </style>
      </head>
      <body>
        <video controls autoplay playsinline preload="metadata" poster="${movie.posterUrl}">
          <source src="${movie.streamUrl}" type="video/mp4">
          Twoja przeglądarka nie obsługuje odtwarzania tego filmu.
        </video>
      </body>
    </html>
""".trimIndent()
