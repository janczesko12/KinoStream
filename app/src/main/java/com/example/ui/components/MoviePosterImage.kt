package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.data.PosterNetworkService
import com.example.model.Movie
import com.example.model.isDisplayableMetadata
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.FilmwebGreen
import com.example.ui.theme.RoyalBlue

private val PosterVignetteBrush = Brush.verticalGradient(
    colors = listOf(
        Color.Black.copy(alpha = 0.42f),
        Color.Transparent,
        Color.Transparent,
        Color.Black.copy(alpha = 0.56f)
    )
)

private val BadgePillShape = RoundedCornerShape(4.dp)

@Composable
fun MoviePosterImage(
    movie: Movie,
    modifier: Modifier = Modifier,
    isWatched: Boolean = false,
    showBadges: Boolean = true,
    cornerRadius: Dp = 12.dp
) {
    val context = LocalContext.current
    val clipShape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    val backgroundBrush = remember(movie.id) {
        Brush.verticalGradient(
            colors = movie.posterGradientColors.map { Color(it) }
        )
    }

    val initialUrl = remember(movie.id, movie.posterUrl, movie.backdropUrl) {
        PosterNetworkService.getCachedPosterUrl(movie.originalTitle)
            ?: movie.posterUrl.ifBlank { movie.backdropUrl }
    }
    var activePosterUrl by remember(movie.id, initialUrl) { mutableStateOf(initialUrl) }
    var shouldFetchFallback by remember(movie.id, initialUrl) { mutableStateOf(initialUrl.isBlank()) }

    // Automatically resolve official poster/logo if missing or if image load failed
    LaunchedEffect(movie.id, shouldFetchFallback) {
        if (shouldFetchFallback) {
            val fetched = PosterNetworkService.fetchOfficialPosterUrl(
                originalTitle = movie.originalTitle.ifBlank { movie.title },
                isSeries = movie.isSeries
            )
            if (!fetched.isNullOrBlank() && fetched != activePosterUrl) {
                activePosterUrl = fetched
            }
            shouldFetchFallback = false
        }
    }

    val imageRequest = remember(context, activePosterUrl) {
        if (activePosterUrl.isBlank()) {
            null
        } else {
            ImageRequest.Builder(context)
                .data(activePosterUrl)
                .size(260, 390)
                .memoryCacheKey(activePosterUrl)
                .diskCacheKey(activePosterUrl)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .crossfade(false)
                .setHeader("User-Agent", "Mozilla/5.0 (Android; KinoStream/1.0)")
                .build()
        }
    }

    Box(
        modifier = modifier
            .clip(clipShape)
            .background(backgroundBrush)
            .border(1.dp, Color.White.copy(alpha = 0.16f), clipShape),
        contentAlignment = Alignment.Center
    ) {
        PosterFallbackContent(movie = movie)

        if (imageRequest != null) {
            AsyncImage(
                model = imageRequest,
                contentDescription = "Plakat: ${movie.title}",
                contentScale = ContentScale.Crop,
                onError = {
                    if (!shouldFetchFallback) {
                        shouldFetchFallback = true
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (showBadges || isWatched) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PosterVignetteBrush)
                )
            }
        }

        // Top badges (Year & SERIAL/FILM or Age rating)
        if (showBadges) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(5.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(BadgePillShape)
                        .background(
                            if (movie.isSeries) RoyalBlue.copy(alpha = 0.9f)
                            else Color.Black.copy(alpha = 0.72f)
                        )
                        .padding(horizontal = 4.dp, vertical = 1.5.dp)
                ) {
                    Text(
                        text = if (movie.isSeries) "SERIAL" else "${movie.year}",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                val ageRating = movie.ageRating.trim()
                if (ageRating.isDisplayableMetadata()) {
                    Box(
                        modifier = Modifier
                            .clip(BadgePillShape)
                            .background(Color.Black.copy(alpha = 0.72f))
                            .padding(horizontal = 4.dp, vertical = 1.5.dp)
                    ) {
                        Text(
                            text = ageRating,
                            color = AccentCyan,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Bottom watched overlay
        if (isWatched) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(FilmwebGreen.copy(alpha = 0.9f))
                    .padding(vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "OBEJRZANO",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )
            }
        }
    }
}

/**
 * Circular profile icon / logo badge for a movie or TV series with automatic online fallback resolution.
 */
@Composable
fun MovieProfileAvatar(
    movie: Movie,
    size: Dp = 28.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val brandColor = movie.platforms.firstOrNull()?.brandColor ?: RoyalBlue
    val backgroundBrush = remember(movie.id) {
        Brush.linearGradient(
            colors = movie.posterGradientColors.map { Color(it) }
        )
    }

    val initialLogoUrl = remember(movie.id, movie.profileLogoUrl, movie.posterUrl) {
        PosterNetworkService.getCachedPosterUrl(movie.originalTitle)
            ?: movie.profileLogoUrl.ifBlank { movie.posterUrl.ifBlank { movie.backdropUrl } }
    }
    var activeLogoUrl by remember(movie.id, initialLogoUrl) { mutableStateOf(initialLogoUrl) }
    var shouldFetchLogo by remember(movie.id, initialLogoUrl) { mutableStateOf(initialLogoUrl.isBlank()) }

    LaunchedEffect(movie.id, shouldFetchLogo) {
        if (shouldFetchLogo) {
            val resolved = PosterNetworkService.fetchOfficialPosterUrl(
                originalTitle = movie.originalTitle.ifBlank { movie.title },
                isSeries = movie.isSeries
            )
            if (!resolved.isNullOrBlank() && resolved != activeLogoUrl) {
                activeLogoUrl = resolved
            }
            shouldFetchLogo = false
        }
    }

    val avatarRequest = remember(context, activeLogoUrl) {
        if (activeLogoUrl.isBlank()) {
            null
        } else {
            ImageRequest.Builder(context)
                .data(activeLogoUrl)
                .size(120, 120)
                .memoryCacheKey("avatar_$activeLogoUrl")
                .diskCacheKey(activeLogoUrl)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .crossfade(false)
                .setHeader("User-Agent", "Mozilla/5.0 (Android; KinoStream/1.0)")
                .build()
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundBrush)
            .border(1.5.dp, brandColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = movie.posterEmoji,
            fontSize = (size.value * 0.46f).sp
        )

        if (avatarRequest != null) {
            AsyncImage(
                model = avatarRequest,
                contentDescription = "Ikona profilowa: ${movie.title}",
                contentScale = ContentScale.Crop,
                onError = {
                    if (!shouldFetchLogo) {
                        shouldFetchLogo = true
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun PosterFallbackContent(movie: Movie) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = movie.posterEmoji,
            fontSize = 30.sp
        )
        Text(
            text = movie.title,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
