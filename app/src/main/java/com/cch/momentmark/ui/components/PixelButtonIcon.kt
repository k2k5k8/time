package com.cch.momentmark.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.cch.momentmark.ui.theme.MomentMarkTokens

/** Small monochrome pixel glyphs; callers retain the label and business meaning. */
enum class PixelButtonIcon(internal val pixels: List<String>) {
    Back(listOf("00010000", "00110000", "01100000", "11111111", "11111111", "01100000", "00110000", "00010000")),
    Repeat(listOf("00111100", "01000010", "10000001", "00000111", "11100000", "10000001", "01000010", "00111100")),
    Calendar(listOf("00100100", "11111111", "10000001", "11111111", "10101001", "10010101", "10101001", "11111111")),
    Clock(listOf("00111100", "01000010", "10010001", "10010001", "10011101", "10000001", "01000010", "00111100")),
    Sunrise(listOf("00010000", "01000100", "00111000", "01000100", "01000100", "11111111", "00000000", "11111111")),
    Sun(listOf("10011001", "01000010", "00111100", "10100101", "10100101", "00111100", "01000010", "10011001")),
    Moon(listOf("00111000", "01100000", "11000000", "11000000", "11000001", "11100011", "01111110", "00111100")),
    Note(listOf("11111111", "10000001", "10111101", "10000001", "10111101", "10000001", "10111001", "11111111")),
    Settings(listOf("00100100", "11111111", "00100100", "00000000", "01001000", "11111111", "01001000", "00000000")),
    Star(listOf("00011000", "00011000", "11111111", "01111110", "00111100", "01111110", "01100110", "11000011")),
    Play(listOf("01000000", "01100000", "01110000", "01111000", "01111000", "01110000", "01100000", "01000000")),
    Check(listOf("00000000", "00000001", "00000011", "00000110", "11001100", "01111000", "00110000", "00000000")),
    Edit(listOf("00000110", "00001111", "00011110", "00111100", "01111000", "11110000", "11100000", "11000000")),
    Skip(listOf("10000011", "11000011", "11100011", "11110011", "11110011", "11100011", "11000011", "10000011")),
    Pause(listOf("01100110", "01100110", "01100110", "01100110", "01100110", "01100110", "01100110", "01100110")),
    Stop(listOf("00000000", "01111110", "01111110", "01111110", "01111110", "01111110", "01111110", "00000000")),
    Seal(listOf("11111111", "10000001", "11111111", "01000010", "01011010", "01000010", "01000010", "01111110")),
    Close(listOf("11000011", "01100110", "00111100", "00011000", "00011000", "00111100", "01100110", "11000011")),
}

@Composable
internal fun PixelButtonGlyph(icon: PixelButtonIcon, color: Color) {
    Canvas(Modifier.size(MomentMarkTokens.FormButtonIconSize).clearAndSetSemantics {}) {
        val unit = size.minDimension / 8f
        icon.pixels.forEachIndexed { y, row -> row.forEachIndexed { x, pixel ->
            if (pixel == '1') drawRect(color, Offset(x * unit, y * unit), Size(unit, unit))
        } }
    }
}

/** Stack at large fonts so icons never consume the Chinese label's wrapping width. */
@Composable
internal fun PixelButtonLabel(label: String, icon: PixelButtonIcon?, color: Color, style: TextStyle,
    padding: PaddingValues, fillWidth: Boolean = true) {
    @Composable fun labelText(modifier: Modifier) {
        Text(label, modifier, style = style, color = color, textAlign = TextAlign.Center)
    }
    val width = if (fillWidth) Modifier.fillMaxWidth() else Modifier
    if (icon == null) labelText(width.padding(padding))
    else if (LocalDensity.current.fontScale >= 1.5f) {
        Column(width.padding(padding), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit)) {
            Row(horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit)) {
                PixelButtonGlyph(icon, color)
            }
            labelText(width)
        }
    } else Row(width.padding(padding), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit, Alignment.CenterHorizontally)) {
        PixelButtonGlyph(icon, color)
        labelText(Modifier.weight(1f, fill = false))
    }
}
