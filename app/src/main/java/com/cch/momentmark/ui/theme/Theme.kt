package com.cch.momentmark.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

/** 昼 · 任务板（浅色 ColorScheme） */
private val LightColors = lightColorScheme(
    primary = MmHpGreenLight,
    onPrimary = Color.White,
    primaryContainer = MmHpContainerLight,
    onPrimaryContainer = MmOnHpContainerLight,
    secondary = MmXpBlueLight,
    onSecondary = Color.White,
    secondaryContainer = MmXpContainerLight,
    onSecondaryContainer = MmOnXpContainerLight,
    tertiary = MmGoldLight,
    onTertiary = MmLabelLight,
    background = MmBackgroundLight,
    onBackground = MmLabelLight,
    surface = MmSurfaceLight,
    onSurface = MmLabelLight,
    surfaceVariant = MmSurfaceVariantLight,
    onSurfaceVariant = MmLabelSecondaryLight,
    surfaceTint = MmHpGreenLight,
    inverseSurface = MmLabelLight,
    inverseOnSurface = MmSurfaceLight,
    inversePrimary = MmHpGreenDark,
    outline = MmOutlineLight,
    outlineVariant = MmOutlineVariantLight,
    error = MmDangerLight,
    onError = Color.White,
    errorContainer = MmDangerContainerLight,
    onErrorContainer = MmOnDangerContainerLight,
    scrim = Color.Black,
)

/** 夜 · 洞窟（深色 ColorScheme） */
private val DarkColors = darkColorScheme(
    primary = MmHpGreenDark,
    onPrimary = Color(0xFF0E2A18),
    primaryContainer = MmHpContainerDark,
    onPrimaryContainer = MmOnHpContainerDark,
    secondary = MmXpBlueDark,
    onSecondary = Color(0xFF0B2238),
    secondaryContainer = MmXpContainerDark,
    onSecondaryContainer = MmOnXpContainerDark,
    tertiary = MmGoldDark,
    onTertiary = Color(0xFF2B1F00),
    background = MmBackgroundDark,
    onBackground = MmLabelDark,
    surface = MmSurfaceDark,
    onSurface = MmLabelDark,
    surfaceVariant = MmSurfaceVariantDark,
    onSurfaceVariant = MmLabelSecondaryDark,
    surfaceTint = MmHpGreenDark,
    inverseSurface = MmLabelDark,
    inverseOnSurface = Color(0xFF201B30),
    inversePrimary = MmHpGreenLight,
    outline = MmOutlineDark,
    outlineVariant = MmOutlineVariantDark,
    error = MmDangerDark,
    onError = Color(0xFF2B0B0C),
    errorContainer = MmDangerContainerDark,
    onErrorContainer = MmOnDangerContainerDark,
    scrim = Color.Black,
)

@Composable
fun MomentMarkTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    // 默认关闭：壁纸动态色会整体覆盖设计语言，与「唯一风格源」冲突。
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme -> {
            dynamicDarkColorScheme(context)
        }

        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colors,
        typography = MmTypography,
        shapes = MmShapes,
        content = content,
    )
}
