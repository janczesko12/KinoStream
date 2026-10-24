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
    val watchUrl: String,
    val sourceUrl: String,
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
        streamUrl = "https://video.blender.org/static/webseed/bf1f3fb5-b119-4f9f-9930-8e20e892b898-720.mp4",
        posterUrl = "https://video.blender.org/lazy-static/thumbnails/bf1f3fb5-b119-4f9f-9930-8e20e892b898.jpg",
        watchUrl = "https://video.blender.org/videos/watch/bf1f3fb5-b119-4f9f-9930-8e20e892b898",
        sourceUrl = "https://studio.blender.org/projects/big-buck-bunny/",
        colors = listOf(Color(0xFF164E38), Color(0xFF071B23))
    ),
    FreeMovie(
        id = "sintel",
        title = "Sintel",
        year = 2010,
        duration = "14 min 48 s",
        genre = "Fantasy • Przygodowy",
        description = "Samotna wędrowniczka przemierza niebezpieczną krainę, by odnaleźć smoczątko, z którym połączyła ją niezwykła więź.",
        streamUrl = "https://video.blender.org/static/webseed/0eb052d0-fd51-43e6-aa33-ecdbf77a5d40-720.mp4",
        posterUrl = "https://video.blender.org/lazy-static/thumbnails/0eb052d0-fd51-43e6-aa33-ecdbf77a5d40.jpg",
        watchUrl = "https://video.blender.org/videos/watch/0eb052d0-fd51-43e6-aa33-ecdbf77a5d40",
        sourceUrl = "https://studio.blender.org/projects/sintel/",
        colors = listOf(Color(0xFF8F3E30), Color(0xFF161F35))
    ),
    FreeMovie(
        id = "tears-of-steel",
        title = "Tears of Steel",
        year = 2012,
        duration = "12 min 14 s",
        genre = "Sci-fi • Akcja",
        description = "W futurystycznym Amsterdamie grupa naukowców próbuje ocalić świat, łącząc eksperymenty technologiczne z trudną misją.",
        streamUrl = "https://video.blender.org/static/webseed/8533ea43-4271-4a57-9694-e9d0b35e1aa1-720.mp4",
        posterUrl = "https://video.blender.org/lazy-static/thumbnails/8533ea43-4271-4a57-9694-e9d0b35e1aa1.jpg",
        watchUrl = "https://video.blender.org/videos/watch/8533ea43-4271-4a57-9694-e9d0b35e1aa1",
        sourceUrl = "https://mango.blender.org/production/tears-of-steel-download-watch/",
        colors = listOf(Color(0xFF126A7A), Color(0xFF10192C))
    ),
    FreeMovie(
        id = "spring",
        title = "Spring",
        year = 2019,
        duration = "7 min 44 s",
        genre = "Fantasy • Animacja",
        description = "Pasterka i jej pies spotykają pradawne duchy. Muszą zmierzyć się z niezwykłą siłą natury, aby zachować cykl życia.",
        streamUrl = "https://video.blender.org/static/webseed/3d95fb3d-c866-42c8-9db1-fe82f48ccb95-720.mp4",
        posterUrl = "https://video.blender.org/lazy-static/thumbnails/3d95fb3d-c866-42c8-9db1-fe82f48ccb95.jpg",
        watchUrl = "https://video.blender.org/videos/watch/3d95fb3d-c866-42c8-9db1-fe82f48ccb95",
        sourceUrl = "https://studio.blender.org/projects/spring/",
        colors = listOf(Color(0xFF476B52), Color(0xFF172B35))
    ),
    FreeMovie(
        id = "sprite-fright",
        title = "Sprite Fright",
        year = 2021,
        duration = "10 min 30 s",
        genre = "Horror • Komedia",
        description = "Wycieczka nastolatków do odległego lasu szybko wymyka się spod kontroli, gdy odkrywają grzybowe stworzenia, które nie są tak bezbronne, jak wyglądają.",
        streamUrl = "https://video.blender.org/download/videos/a69d68a5-a0e0-4a80-9d66-49f093c97aaf-720.mp4",
        posterUrl = "https://video.blender.org/lazy-static/thumbnails/a69d68a5-a0e0-4a80-9d66-49f093c97aaf.jpg",
        watchUrl = "https://video.blender.org/videos/watch/a69d68a5-a0e0-4a80-9d66-49f093c97aaf",
        sourceUrl = "https://studio.blender.org/projects/sprite-fright/",
        colors = listOf(Color(0xFF6B3C55), Color(0xFF171A2D))
    ),
    FreeMovie(
        id = "glass-half",
        title = "Glass Half",
        year = 2015,
        duration = "3 min 13 s",
        genre = "Animacja • Komedia",
        description = "Krótka animacja o dwóch osobach, które zupełnie inaczej patrzą na ten sam obraz — i na świat.",
        streamUrl = "https://video.blender.org/static/webseed/64222c8a-c4c7-4b3b-9850-7fb2078edcf6-720.mp4",
        posterUrl = "https://video.blender.org/lazy-static/thumbnails/64222c8a-c4c7-4b3b-9850-7fb2078edcf6.jpg",
        watchUrl = "https://video.blender.org/videos/watch/64222c8a-c4c7-4b3b-9850-7fb2078edcf6",
        sourceUrl = "https://video.blender.org/videos/watch/64222c8a-c4c7-4b3b-9850-7fb2078edcf6",
        colors = listOf(Color(0xFF9B6239), Color(0xFF2A1C2D))
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
                text = "Katalog zawiera bezpłatne filmy z projektów Blender Open Movies, a nie płatne produkcje z Netflixa czy Disney+. Materiały są odtwarzane z serwisu Blender Video; szczegóły i warunki licencji znajdziesz na stronie źródłowej każdego filmu.",
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
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xE6000000))
                            )
                        )
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
        Text(
            text = "Źródło: Blender Open Movies • ${movie.year}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = { runCatching { uriHandler.openUri(movie.sourceUrl) } }
            ) {
                Icon(
                    imageVector = Icons.Filled.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text("Źródło i licencja")
            }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(
                onClick = { runCatching { uriHandler.openUri(movie.watchUrl) } }
            ) {
                Text("Otwórz player")
            }
        }
        Text(
            text = "Film odtwarza się bezpośrednio w tej zakładce. Jeśli transmisja nie ruszy, użyj przycisku „Otwórz player” albo spróbuj ponownie po chwili.",
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
