package com.cch.momentmark.ui.system

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cch.momentmark.ui.app.MainTab
import com.cch.momentmark.ui.components.PageTitlePanel
import com.cch.momentmark.ui.components.PixelBottomNav
import com.cch.momentmark.ui.components.PixelDashedDivider
import com.cch.momentmark.ui.components.PixelPanel
import com.cch.momentmark.ui.recyclebin.RecycleBinUiState
import com.cch.momentmark.ui.theme.LocalMmExtendedColors
import com.cch.momentmark.ui.theme.MmGoldDark
import com.cch.momentmark.ui.theme.MomentMarkTokens
import com.cch.momentmark.ui.theme.ThemeMode

/**
 * J ⑨ 设置 SYSTEM · 昼（design.html 第⑨屏）。
 *
 * 结构：页题 → 冒险者卡（头像/LV/XP 条）→ 显示模式三态 → 设置列表
 * （战斗音效/命中震动/封印之地/关于刻间）→ footer → 底部导航（日子簿选中）。
 *
 * 后端未设计的项（冒险者等级/连续天数/XP、音效、震动、关于）仅静态展示；
 * 已有逻辑的项（封印之地计数与入口、显示模式 ThemeMode）接入真实回调。
 */
@Composable
fun SystemSettingsScreen(
    recycleBinState: RecycleBinUiState,
    onBack: () -> Unit,
    onOpenRecycleBin: () -> Unit,
    modifier: Modifier = Modifier,
    selectedTab: MainTab = MainTab.DAYBOOK,
    onTabSelected: (MainTab) -> Unit = {},
    onNewMoment: () -> Unit = {},
    themeMode: ThemeMode? = null,
    onThemeModeChanged: (ThemeMode) -> Unit = {},
    versionName: String = "v0.1.0",
) {
    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            Modifier
                .weight(1f)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            // design `.statusbar{padding:11px 18px 5px;font-size:11px mono/800;color:var(--ink-2)}`
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, top = 11.dp, bottom = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "♥×3",
                    style = monoStyle(fontSize = 11.sp, lineHeight = 14.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "SYSTEM",
                    style = monoStyle(fontSize = 11.sp, lineHeight = 14.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // design `.pagetitle{margin:0 12px 10px}` —— 无返回箭头（⑨ 设置页题纯标题）。
            PageTitlePanel(
                title = "系统设置",
                hud = "OPTIONS",
                modifier = Modifier.padding(horizontal = MomentMarkTokens.SettingsContentPaddingHorizontal),
            )
            Spacer(Modifier.height(MomentMarkTokens.SpaceCard))
            Column(
                Modifier.padding(horizontal = MomentMarkTokens.SettingsContentPaddingHorizontal),
                verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SettingsContentGap),
            ) {
                AdventurerCard()
                DisplayModePanel(
                    themeMode = themeMode ?: ThemeMode.SYSTEM,
                    onThemeModeChanged = onThemeModeChanged,
                )
                SettingsListPanel(
                    recycleBinCount = recycleBinState.items.size,
                    onOpenRecycleBin = onOpenRecycleBin,
                    versionName = versionName,
                )
                Text(
                    text = "MOMENT MARK · MADE WITH ♥ · SAVE FILE LOCAL",
                    style = monoStyle(
                        fontSize = MomentMarkTokens.SettingsFooterFontSize,
                        lineHeight = MomentMarkTokens.SettingsFooterLineHeight,
                        letterSpacing = MomentMarkTokens.SettingsFooterLetterSpacing,
                    ),
                    color = LocalMmExtendedColors.current.labelTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(MomentMarkTokens.SettingsBottomSpacer))
        }
        PixelBottomNav(
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            onNewMoment = onNewMoment,
        )
    }
}

// ── 冒险者卡 ──────────────────────────────────────────────

@Composable
private fun AdventurerCard() {
    PixelPanel(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = MomentMarkTokens.SettingsAdventurerPaddingVertical,
                    horizontal = MomentMarkTokens.SettingsAdventurerPaddingHorizontal,
                ),
            horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SettingsAdventurerGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 头像 `width:52px;height:52px;background:var(--gold-n);border:3px;font-size:24px`
            // light-preview 不覆盖 gold-n 背景，故渲染为 #FFC53D（MmGoldDark），非昼间 tertiary #F5B301。
            Box(
                Modifier
                    .size(MomentMarkTokens.SettingsAvatarSize)
                    .background(MmGoldDark)
                    .border(MomentMarkTokens.PxBorderWidth, MaterialTheme.colorScheme.outline),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "🧙",
                    fontSize = MomentMarkTokens.SettingsAvatarFontSize,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = "冒险者 · cch",
                    style = TextStyle(
                        fontSize = MomentMarkTokens.SettingsAdventurerTitleFontSize,
                        lineHeight = MomentMarkTokens.SettingsAdventurerTitleLineHeight,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(MomentMarkTokens.SettingsLvTopGap))
                Text(
                    text = "LV.07 · 连续打卡 12 天 🔥",
                    style = monoStyle(
                        fontSize = MomentMarkTokens.SettingsLvFontSize,
                        lineHeight = MomentMarkTokens.SettingsLvLineHeight,
                    ),
                    color = LocalMmExtendedColors.current.goldInk,
                )
                Spacer(Modifier.height(MomentMarkTokens.SettingsXpBarTopGap))
                SettingsXpBar(filledCells = 4)
                Spacer(Modifier.height(MomentMarkTokens.SettingsExpTopGap))
                Text(
                    text = "EXP 61% → LV.08",
                    style = monoStyle(
                        fontSize = MomentMarkTokens.SettingsExpFontSize,
                        lineHeight = MomentMarkTokens.SettingsExpLineHeight,
                    ),
                    color = LocalMmExtendedColors.current.labelTertiary,
                )
            }
        }
    }
}

/** 设置页专用 8 格 XP 条（空格用 background 色，与 design `.light-preview .xpbar .cell:not(.fill)` 一致）。 */
@Composable
private fun SettingsXpBar(filledCells: Int) {
    val count = MomentMarkTokens.SettingsXpCellCount
    Row(
        Modifier
            .fillMaxWidth()
            .height(MomentMarkTokens.XpCellHeight),
        horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.XpCellGap),
    ) {
        repeat(count) { index ->
            Box(
                Modifier
                    .weight(1f)
                    .height(MomentMarkTokens.XpCellHeight)
                    .background(
                        if (index < filledCells) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.background
                        },
                    )
                    .border(MomentMarkTokens.PxThinBorderWidth, MaterialTheme.colorScheme.outline),
            )
        }
    }
}

// ── 显示模式 ──────────────────────────────────────────────

@Composable
private fun DisplayModePanel(
    themeMode: ThemeMode,
    onThemeModeChanged: (ThemeMode) -> Unit,
) {
    PixelPanel(
        modifier = Modifier.fillMaxWidth(),
        shadowColor = Color.Transparent,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = MomentMarkTokens.SettingsDisplayPaddingVertical,
                    horizontal = MomentMarkTokens.SettingsDisplayPaddingHorizontal,
                ),
        ) {
            Text(
                text = "▸ 显示模式 DISPLAY",
                style = monoStyle(
                    fontSize = MomentMarkTokens.SettingsDisplayLabelFontSize,
                    lineHeight = MomentMarkTokens.SettingsDisplayLabelLineHeight,
                    letterSpacing = MomentMarkTokens.SettingsDisplayLabelLetterSpacing,
                ),
                color = LocalMmExtendedColors.current.labelTertiary,
            )
            Spacer(Modifier.height(MomentMarkTokens.SettingsDisplayRowTopGap))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SettingsDisplayRowGap),
            ) {
                DisplayModeButton(
                    label = "昼间地图",
                    selected = themeMode == ThemeMode.LIGHT,
                    onClick = { onThemeModeChanged(ThemeMode.LIGHT) },
                    modifier = Modifier.weight(1f),
                )
                DisplayModeButton(
                    label = "洞窟",
                    selected = themeMode == ThemeMode.DARK,
                    onClick = { onThemeModeChanged(ThemeMode.DARK) },
                    modifier = Modifier.weight(1f),
                )
                DisplayModeButton(
                    label = "跟随系统 ✓",
                    selected = themeMode == ThemeMode.SYSTEM,
                    onClick = { onThemeModeChanged(ThemeMode.SYSTEM) },
                    modifier = Modifier.weight(MomentMarkTokens.SettingsDisplaySelectedWeight),
                )
            }
        }
    }
}

@Composable
private fun DisplayModeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor = if (selected) MmGoldDark else MaterialTheme.colorScheme.surface
    val borderColor = if (selected) MmGoldDark else MaterialTheme.colorScheme.outline
    val textColor = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    PixelPanel(
        modifier = modifier
            .semantics {
                contentDescription = label
                role = Role.Button
            }
            .clickable(role = Role.Button, onClick = onClick),
        backgroundColor = bgColor,
        borderColor = borderColor,
        shadowColor = Color.Transparent,
    ) {
        Text(
            text = label,
            style = monoStyle(
                fontSize = MomentMarkTokens.SettingsDisplayButtonFontSize,
                lineHeight = MomentMarkTokens.SettingsDisplayButtonLineHeight,
            ),
            color = textColor,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = MomentMarkTokens.SettingsDisplayButtonPaddingVertical),
        )
    }
}

// ── 设置列表 ──────────────────────────────────────────────

@Composable
private fun SettingsListPanel(
    recycleBinCount: Int,
    onOpenRecycleBin: () -> Unit,
    versionName: String,
) {
    PixelPanel(
        modifier = Modifier.fillMaxWidth(),
        shadowColor = Color.Transparent,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(MomentMarkTokens.SettingsListPanelPadding),
        ) {
            SettingsListItem(
                label = "🔊 战斗音效",
                trailing = "[ ] OFF",
                onClick = {},
                showDivider = true,
            )
            SettingsListItem(
                label = "📳 命中震动",
                trailing = "[x] ON",
                trailingColor = MaterialTheme.colorScheme.primary,
                onClick = {},
                showDivider = true,
            )
            SettingsListItem(
                label = "🗑 封印之地（回收站）",
                trailing = "$recycleBinCount 件 ›",
                trailingColor = MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = onOpenRecycleBin,
                showDivider = true,
            )
            SettingsListItem(
                label = "ℹ 关于刻间",
                trailing = "$versionName ›",
                trailingColor = MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = {},
                showDivider = false,
            )
        }
    }
}

@Composable
private fun SettingsListItem(
    label: String,
    trailing: String,
    onClick: () -> Unit,
    showDivider: Boolean,
    trailingColor: Color = LocalMmExtendedColors.current.labelTertiary,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = label
                role = Role.Button
            }
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = MomentMarkTokens.SettingsRowPaddingVertical,
                    horizontal = MomentMarkTokens.SettingsRowPaddingHorizontal,
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = TextStyle(
                    fontSize = MomentMarkTokens.SettingsRowLabelFontSize,
                    lineHeight = MomentMarkTokens.SettingsRowLabelLineHeight,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = trailing,
                style = monoStyle(
                    fontSize = MomentMarkTokens.SettingsRowTrailingFontSize,
                    lineHeight = MomentMarkTokens.SettingsRowTrailingLineHeight,
                ),
                color = trailingColor,
            )
        }
        if (showDivider) {
            PixelDashedDivider()
        }
    }
}

// ── 工具 ──────────────────────────────────────────────────

/** mono 字体 + ExtraBold 的统一 TextStyle 工厂（design.html `.mono{font-weight:700}`）。 */
private fun monoStyle(
    fontSize: androidx.compose.ui.unit.TextUnit,
    lineHeight: androidx.compose.ui.unit.TextUnit,
    letterSpacing: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
): TextStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.ExtraBold,
    fontSize = fontSize,
    lineHeight = lineHeight,
    letterSpacing = letterSpacing,
)
