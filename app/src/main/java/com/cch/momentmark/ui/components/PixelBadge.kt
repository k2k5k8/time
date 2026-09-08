package com.cch.momentmark.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.cch.momentmark.ui.theme.LocalMmExtendedColors
import com.cch.momentmark.ui.theme.MomentMarkTokens

/** Semantic color presets for the compact `.badge-tag` used across J cards. */
enum class PixelBadgeVariant {
    Main,
    Side,
    Limited,
    Achievement,
}

/**
 * Compact mono badge shared by Moment and Task presentation components.
 *
 * The component only renders the supplied label and tone; status selection and
 * business meaning remain in the presentation layer. A custom [color] may be
 * supplied for domain-specific badges while keeping the same geometry.
 */
@Composable
fun PixelBadge(
    label: String,
    modifier: Modifier = Modifier,
    variant: PixelBadgeVariant = PixelBadgeVariant.Side,
    color: Color? = null,
) {
    val resolvedColor = color ?: when (variant) {
        PixelBadgeVariant.Main -> MaterialTheme.colorScheme.tertiary
        PixelBadgeVariant.Side -> MaterialTheme.colorScheme.secondary
        PixelBadgeVariant.Limited -> LocalMmExtendedColors.current.amber
        PixelBadgeVariant.Achievement -> LocalMmExtendedColors.current.goldInk
    }
    Text(
        text = label,
        style = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 8.sp,
            lineHeight = 10.sp,
        ),
        color = resolvedColor,
        modifier = modifier
            .semantics { contentDescription = label }
            .border(MomentMarkTokens.PxThinBorderWidth, resolvedColor)
            .padding(
                horizontal = MomentMarkTokens.BadgePaddingHorizontal,
                vertical = MomentMarkTokens.BadgePaddingVertical,
            ),
    )
}
