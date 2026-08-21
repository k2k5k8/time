package com.cch.momentmark.ui.settings

import com.cch.momentmark.ui.theme.ThemeMode

fun themeModeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> "跟随系统"
    ThemeMode.LIGHT -> "浅色"
    ThemeMode.DARK -> "深色"
}
