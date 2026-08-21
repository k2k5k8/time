package com.cch.momentmark.ui.home

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

/**
 * Low-frequency environmental colors derived from the current Hero image.
 * The values are intentionally muted so the image can influence the UI without
 * turning cards into saturated photo filters.
 */
internal data class AdaptiveBackgroundPalette(
    val environmentColor: Color,
    val mutedEnvironmentColor: Color,
    val transitionColor: Color,
    val uiBaseColor: Color,
    val cardSurfaceColor: Color,
    val cardTintColor: Color,
    val cardHighlightColor: Color,
    val ambientShadowColor: Color,
    val cardContentColor: Color,
    val quietStrength: Float,
    val isDarkScene: Boolean,
)

internal object AdaptiveBackgroundPaletteAnalyzer {
    fun analyze(context: Context, imageRes: Int): AdaptiveBackgroundPalette {
        val options = BitmapFactory.Options().apply {
            inSampleSize = 8
            inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
        }
        val bitmap = BitmapFactory.decodeResource(context.resources, imageRes, options)
            ?: return fallbackPalette()

        return try {
            analyze(bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    private fun analyze(bitmap: android.graphics.Bitmap): AdaptiveBackgroundPalette {
        val stepX = (bitmap.width / 32).coerceAtLeast(1)
        val stepY = (bitmap.height / 32).coerceAtLeast(1)
        val average = FloatArray(3)
        val accent = FloatArray(3)
        var averageWeight = 0f
        var accentWeight = 0f
        var luminanceSum = 0f
        var luminanceSquaredSum = 0f
        var saturationSum = 0f
        var sampleCount = 0
        val hsv = FloatArray(3)

        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                val red = AndroidColor.red(pixel) / 255f
                val green = AndroidColor.green(pixel) / 255f
                val blue = AndroidColor.blue(pixel) / 255f
                val luminance = red * 0.2126f + green * 0.7152f + blue * 0.0722f
                AndroidColor.colorToHSV(pixel, hsv)

                // Give the lower image slightly more influence because it is
                // the part that visually meets the card area.
                val spatialWeight = if (y > bitmap.height * 0.42f) 1.16f else 0.84f
                average[0] += red * spatialWeight
                average[1] += green * spatialWeight
                average[2] += blue * spatialWeight
                averageWeight += spatialWeight
                luminanceSum += luminance
                luminanceSquaredSum += luminance * luminance
                saturationSum += hsv[1]
                sampleCount++

                if (hsv[1] >= 0.18f) {
                    accent[0] += red * spatialWeight
                    accent[1] += green * spatialWeight
                    accent[2] += blue * spatialWeight
                    accentWeight += spatialWeight
                }
                x += stepX
            }
            y += stepY
        }

        val averageColor = Color(
            red = (average[0] / averageWeight).coerceIn(0f, 1f),
            green = (average[1] / averageWeight).coerceIn(0f, 1f),
            blue = (average[2] / averageWeight).coerceIn(0f, 1f),
        )
        val accentColor = if (accentWeight > averageWeight * 0.12f) {
            Color(
                red = (accent[0] / accentWeight).coerceIn(0f, 1f),
                green = (accent[1] / accentWeight).coerceIn(0f, 1f),
                blue = (accent[2] / accentWeight).coerceIn(0f, 1f),
            )
        } else {
            averageColor
        }
        val environment = androidx.compose.ui.graphics.lerp(averageColor, accentColor, 0.42f)
        val luminance = luminanceSum / sampleCount.coerceAtLeast(1)
        val variance = (luminanceSquaredSum / sampleCount.coerceAtLeast(1) - luminance * luminance)
            .coerceAtLeast(0f)
        val averageSaturation = saturationSum / sampleCount.coerceAtLeast(1)
        val isDark = luminance < 0.38f
        val isBright = luminance > 0.72f
        val isSaturated = averageSaturation > 0.42f
        val complexity = (sqrt(variance) / 0.28f).coerceIn(0f, 1f)
        val quietStrength = when {
            isSaturated -> 0.30f
            complexity > 0.62f -> 0.25f
            else -> 0.16f
        }

        val muted = desaturate(environment, if (isSaturated) 0.58f else 0.38f)
        val neutral = when {
            isDark -> Color(0xFFDAD8D0)
            isBright -> Color(0xFFF0ECE3)
            else -> Color(0xFFECE8DE)
        }
        val cardBase = when {
            isDark -> Color(0xFFE2DED5)
            isBright -> Color(0xFFF3EFE6)
            else -> Color(0xFFF0EBE1)
        }
        val uiBase = androidx.compose.ui.graphics.lerp(neutral, muted, 0.12f)
        val cardSurface = androidx.compose.ui.graphics.lerp(cardBase, muted, if (isSaturated) 0.07f else 0.12f)
        val shadow = androidx.compose.ui.graphics.lerp(muted, Color(0xFF3B4543), 0.58f)

        return AdaptiveBackgroundPalette(
            environmentColor = environment,
            mutedEnvironmentColor = muted,
            transitionColor = androidx.compose.ui.graphics.lerp(muted, uiBase, 0.38f),
            uiBaseColor = uiBase,
            cardSurfaceColor = cardSurface,
            cardTintColor = muted,
            cardHighlightColor = if (isBright) Color.White else Color(0xFFFFFBF3),
            ambientShadowColor = shadow,
            cardContentColor = if (isDark) Color(0xFF263238) else Color(0xFF303638),
            quietStrength = quietStrength,
            isDarkScene = isDark,
        )
    }

    private fun desaturate(color: Color, amount: Float): Color {
        val gray = color.red * 0.2126f + color.green * 0.7152f + color.blue * 0.0722f
        return androidx.compose.ui.graphics.lerp(
            color,
            Color(gray, gray, gray),
            amount.coerceIn(0f, 1f),
        )
    }

    private fun fallbackPalette() = AdaptiveBackgroundPalette(
        environmentColor = Color(0xFF71828A),
        mutedEnvironmentColor = Color(0xFF899295),
        transitionColor = Color(0xFFC9C9C1),
        uiBaseColor = Color(0xFFECE8DE),
        cardSurfaceColor = Color(0xFFF0EBE1),
        cardTintColor = Color(0xFF899295),
        cardHighlightColor = Color(0xFFFFFBF3),
        ambientShadowColor = Color(0xFF596460),
        cardContentColor = Color(0xFF303638),
        quietStrength = 0.20f,
        isDarkScene = false,
    )
}

/** The environmental haze that replaces the former hard white Hero boundary. */
@Composable
internal fun AdaptiveTransitionLayer(
    palette: AdaptiveBackgroundPalette,
    collapseProgress: Float,
    modifier: Modifier = Modifier,
) {
    val imagePresence = (1f - collapseProgress * 0.18f).coerceIn(0.78f, 1f)
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color.Transparent,
                            0.24f to palette.mutedEnvironmentColor.copy(alpha = 0.04f * imagePresence),
                            0.46f to palette.mutedEnvironmentColor.copy(alpha = 0.10f * imagePresence),
                            0.68f to palette.transitionColor.copy(alpha = 0.16f),
                            0.88f to palette.uiBaseColor.copy(alpha = 0.18f),
                            1.00f to palette.uiBaseColor.copy(alpha = 0.28f),
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(72.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            palette.environmentColor.copy(alpha = 0.06f * palette.quietStrength / 0.16f),
                            palette.transitionColor.copy(alpha = 0.14f),
                        ),
                    ),
                ),
        )
        if (palette.quietStrength > 0.2f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(palette.uiBaseColor.copy(alpha = palette.quietStrength * 0.28f)),
            )
        }
    }
}

/** A soft paper mat; existing card renderers remain the content inside it. */
@Composable
internal fun AdaptiveCardSurface(
    palette: AdaptiveBackgroundPalette,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(28.dp)
    Surface(
        modifier = modifier.graphicsLayer {
            shadowElevation = 9.dp.toPx()
            this.shape = shape
            ambientShadowColor = palette.ambientShadowColor.copy(alpha = 0.28f)
            spotShadowColor = palette.ambientShadowColor.copy(alpha = 0.20f)
        },
        shape = shape,
        color = palette.cardSurfaceColor.copy(alpha = 0.86f),
        contentColor = palette.cardContentColor,
        border = BorderStroke(1.dp, palette.cardHighlightColor.copy(alpha = 0.42f)),
        tonalElevation = 0.dp,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                palette.cardHighlightColor.copy(alpha = 0.14f),
                                Color.Transparent,
                                palette.cardTintColor.copy(alpha = 0.07f),
                            ),
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp)
                    .clip(RoundedCornerShape(25.dp)),
            ) {
                content()
            }
        }
    }
}
