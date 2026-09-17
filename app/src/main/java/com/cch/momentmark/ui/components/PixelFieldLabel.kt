package com.cch.momentmark.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.cch.momentmark.ui.theme.LocalMmExtendedColors
import com.cch.momentmark.ui.theme.MomentMarkTokens

/** Visual tone for a field label; business meaning stays with the caller. */
enum class PixelFieldLabelTone {
    Default,
    Accent,
    Error,
}

/**
 * Shared mono HUD label for form fields and compact section headings.
 *
 * The component renders only caller-provided text. It does not infer required,
 * optional, LORE, STORY, Moment, or Task semantics.
 */
@Composable
fun PixelFieldLabel(
    label: String,
    modifier: Modifier = Modifier,
    helper: String? = null,
    tone: PixelFieldLabelTone = PixelFieldLabelTone.Default,
) {
    val labelColor = when (tone) {
        PixelFieldLabelTone.Default -> LocalMmExtendedColors.current.labelTertiary
        PixelFieldLabelTone.Accent -> MaterialTheme.colorScheme.secondary
        PixelFieldLabelTone.Error -> MaterialTheme.colorScheme.error
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.8.sp,
                lineHeight = 11.sp,
            ),
            color = labelColor,
        )
        helper?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.8.sp,
                    lineHeight = 11.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
