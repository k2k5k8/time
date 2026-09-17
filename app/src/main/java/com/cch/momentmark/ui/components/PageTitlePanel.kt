package com.cch.momentmark.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * J 页题面板（proposal_J_final .pagetitle）：
 * panel 面 + 3dp 墨线 + 3dp 硬影，左标题、右 mono HUD 弱注释。
 */
@Composable
fun PageTitlePanel(
    title: String,
    hud: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    PixelPageHeader(title = title, hud = hud, modifier = modifier, onBack = onBack)
}
