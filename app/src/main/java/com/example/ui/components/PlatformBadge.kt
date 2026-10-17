package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.StreamingPlatform

private val PlatformBadgeShape = RoundedCornerShape(6.dp)

@Composable
fun PlatformBadge(
    platform: StreamingPlatform,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val (bg, textColor, label) = when (platform) {
        StreamingPlatform.NETFLIX -> Triple(
            Color(0xFF220808),
            Color(0xFFFF333A),
            "Netflix"
        )
        StreamingPlatform.PRIME_VIDEO -> Triple(
            Color(0xFF041E2B),
            Color(0xFF00C2FF),
            if (compact) "Prime" else "Prime Video"
        )
        StreamingPlatform.MAX -> Triple(
            Color(0xFF100B2B),
            Color(0xFF9D65FF),
            "Max"
        )
        StreamingPlatform.DISNEY_PLUS -> Triple(
            Color(0xFF041838),
            Color(0xFF38B6FF),
            "Disney+"
        )
        StreamingPlatform.SKYSHOWTIME -> Triple(
            Color(0xFF032322),
            Color(0xFF00E5D9),
            if (compact) "Sky" else "SkyShowtime"
        )
    }

    Box(
        modifier = modifier
            .testTag("platform_badge_${platform.id}")
            .clip(PlatformBadgeShape)
            .background(bg)
            .border(1.dp, textColor.copy(alpha = 0.5f), PlatformBadgeShape)
            .padding(horizontal = if (compact) 5.dp else 8.dp, vertical = if (compact) 2.dp else 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = if (compact) 10.sp else 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp
        )
    }
}

@Composable
fun PlatformBadgesRow(
    platforms: List<StreamingPlatform>,
    modifier: Modifier = Modifier,
    compact: Boolean = true
) {
    val displayed = if (compact && platforms.size > 3) platforms.take(3) else platforms
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        displayed.forEach { platform ->
            PlatformBadge(platform = platform, compact = compact)
        }
    }
}
