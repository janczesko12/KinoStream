package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Renders the custom KinoStream logo uploaded by the user:
 * - Deep navy-black rounded square with glowing electric-blue border
 * - Diagonal dark-blue film-strip ribbon in the background
 * - Glowing 3D cyan-to-electric-blue folded play triangle with 4 film sprocket holes on the left
 * - "Kino" (White) + "Stream" (Electric Blue -> Cyan gradient)
 * - 5 mini platform badges at the bottom (N, HBO, prime, skySHOWTIME, Disney+)
 */
@Composable
fun KinoStreamLogoBadge(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val cornerRadius = size * 0.24f
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .testTag("kinostream_logo_badge")
            .size(size)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF050C1E),
                        Color(0xFF091736),
                        Color(0xFF030712)
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF38E8FF),
                        Color(0xFF0077FF),
                        Color(0xFF0055CC)
                    )
                ),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height

            // Subtle radial blue glow in center
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x5500A8FF),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.5f, h * 0.44f),
                    radius = w * 0.48f
                ),
                radius = w * 0.48f,
                center = Offset(w * 0.5f, h * 0.44f)
            )

            // Diagonal film-strip in background
            rotate(degrees = -18f, pivot = Offset(w * 0.5f, h * 0.45f)) {
                drawRect(
                    color = Color(0x220099FF),
                    topLeft = Offset(-w * 0.1f, h * 0.34f),
                    size = Size(w * 1.2f, h * 0.22f)
                )
            }

            // Bottom-right folded ribbon of Play Triangle
            val bottomRibbon = Path().apply {
                moveTo(w * 0.35f, h * 0.66f)
                lineTo(w * 0.74f, h * 0.44f)
                lineTo(w * 0.66f, h * 0.37f)
                lineTo(w * 0.32f, h * 0.58f)
                close()
            }
            drawPath(
                path = bottomRibbon,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF003BB5),
                        Color(0xFF0088FF),
                        Color(0xFF2CE6FF)
                    ),
                    start = Offset(w * 0.32f, h * 0.64f),
                    end = Offset(w * 0.74f, h * 0.40f)
                )
            )

            // Main Left + Top-Right Play Triangle Ribbon with 4 Film Sprocket Holes
            val playPath = Path().apply {
                fillType = PathFillType.EvenOdd
                // Outer triangle ribbon
                moveTo(w * 0.28f, h * 0.20f)
                lineTo(w * 0.74f, h * 0.43f)
                lineTo(w * 0.63f, h * 0.51f)
                lineTo(w * 0.41f, h * 0.38f)
                lineTo(w * 0.41f, h * 0.66f)
                lineTo(w * 0.28f, h * 0.66f)
                close()

                // 4 sprocket holes on the left vertical bar
                val holeX = w * 0.31f
                val holeW = w * 0.065f
                val holeH = h * 0.068f
                for (i in 0..3) {
                    val holeY = h * (0.25f + i * 0.095f)
                    addRect(
                        androidx.compose.ui.geometry.Rect(
                            offset = Offset(holeX, holeY),
                            size = Size(holeW, holeH)
                        )
                    )
                }
            }

            drawPath(
                path = playPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF80F6FF),
                        Color(0xFF00C8FF),
                        Color(0xFF0062E6)
                    ),
                    start = Offset(w * 0.28f, h * 0.20f),
                    end = Offset(w * 0.74f, h * 0.62f)
                )
            )

            // Bottom 5 mini platform dots/tiles (N, HBO, prime, sky, D+)
            val tileColors = listOf(
                Color(0xFFE50914),
                Color(0xFF7B42F6),
                Color(0xFF00C2FF),
                Color(0xFFFF7A00),
                Color(0xFF0063E5)
            )
            val tileW = w * 0.13f
            val tileH = h * 0.11f
            val startX = w * 0.12f
            val gap = w * 0.03f
            val yPos = h * 0.78f

            tileColors.forEachIndexed { idx, color ->
                val xPos = startX + idx * (tileW + gap)
                drawRoundRect(
                    color = color.copy(alpha = 0.9f),
                    topLeft = Offset(xPos, yPos),
                    size = Size(tileW, tileH),
                    cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
                )
            }
        }
    }
}

/**
 * Renders the stylized "KinoStream" wordmark matching the uploaded logo ("Kino" in white, "Stream" in cyan-blue).
 */
@Composable
fun KinoStreamWordmark(
    modifier: Modifier = Modifier,
    fontSizeSp: Int = 22
) {
    Text(
        text = buildAnnotatedString {
            withStyle(
                SpanStyle(
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
            ) {
                append("Kino")
            }
            withStyle(
                SpanStyle(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF1E90FF),
                            Color(0xFF00E5FF)
                        )
                    ),
                    fontWeight = FontWeight.ExtraBold
                )
            ) {
                append("Stream")
            }
        },
        fontSize = fontSizeSp.sp,
        modifier = modifier
    )
}
