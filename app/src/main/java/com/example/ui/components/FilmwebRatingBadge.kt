package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FilmwebRed
import com.example.ui.theme.FilmwebYellow
import java.util.Locale

/**
 * Displays Filmweb rating badge.
 * Uses user-configurable minRating and maxRating range (default 6.5..10.0).
 * If rating is outside [minRating..maxRating] (e.g. below minRating), text is strictly RED!
 */
@Composable
fun FilmwebRatingBadge(
    rating: Double,
    modifier: Modifier = Modifier,
    minRating: Double = 6.5,
    maxRating: Double = 10.0,
    showLabel: Boolean = true,
    compact: Boolean = false
) {
    val isGoodRating = rating in minRating..maxRating
    val formattedRating = String.format(Locale.US, "%.1f", rating)

    // Strictly RED text when rating is below minRating (or outside range)
    val textColor = if (isGoodRating) {
        MaterialTheme.colorScheme.onSurface
    } else {
        FilmwebRed
    }

    val iconColor = if (isGoodRating) {
        FilmwebYellow
    } else {
        FilmwebRed
    }

    val badgeBg = if (isGoodRating) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
    } else {
        FilmwebRed.copy(alpha = 0.12f)
    }

    val borderColor = if (isGoodRating) {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    } else {
        FilmwebRed.copy(alpha = 0.5f)
    }

    Box(
        modifier = modifier
            .testTag("filmweb_rating_badge")
            .clip(RoundedCornerShape(8.dp))
            .background(badgeBg)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = if (compact) 6.dp else 8.dp, vertical = if (compact) 3.dp else 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (isGoodRating) Icons.Filled.Star else Icons.Filled.Warning,
                contentDescription = "Ocena Filmweb",
                tint = iconColor,
                modifier = Modifier.size(if (compact) 14.dp else 16.dp)
            )

            Text(
                text = formattedRating,
                color = textColor,
                fontSize = if (compact) 13.sp else 15.sp,
                fontWeight = FontWeight.Bold
            )

            if (showLabel && !compact) {
                Text(
                    text = "Filmweb",
                    color = textColor.copy(alpha = if (isGoodRating) 0.7f else 0.9f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
