package com.cch.momentmark.ui.recyclebin

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cch.momentmark.domain.model.RecycleBinItem
import com.cch.momentmark.ui.app.MainTab
import com.cch.momentmark.ui.components.PageTitlePanel
import com.cch.momentmark.ui.components.PixelBottomNav
import com.cch.momentmark.ui.components.PixelConfirmationDialog
import com.cch.momentmark.ui.components.PixelPanel
import com.cch.momentmark.ui.components.PixelPanelVariant
import com.cch.momentmark.ui.components.PixelStatusHud
import com.cch.momentmark.ui.components.RecycleBinItemCard
import com.cch.momentmark.ui.components.RecycleBinItemPresentation
import com.cch.momentmark.ui.theme.LocalMmExtendedColors
import com.cch.momentmark.ui.theme.MmDangerDark
import com.cch.momentmark.ui.theme.MomentMarkTokens
import java.time.Clock
import java.time.Duration

private val RecycleDays = Duration.ofDays(30)

/**
 * J ⑩ 回收站 · 封印之地（design.html 第⑩屏 GRAVEYARD · 昼）。
 *
 * 结构：自定义状态栏（♥×3 | GRAVEYARD）→ 页题（◂ 封印之地 | N 件封存 · 30 天后净化）→
 * 可滚动内容区（封印条目卡列表 + 底部提示文字）→ 底部固定区（⚔ 全部净化）→ 底部导航（日子簿选中）。
 *
 * 后端逻辑已接入真实 Repository（复活/净化/自动净化）；本页仅做视觉复刻，不改动数据层。
 * [fixtureState] / [fixtureClock] 仅用于 DEBUG 视觉回归截图，生产构建始终为 null。
 */
@Composable
fun RecycleBinScreen(
    uiState: RecycleBinUiState,
    clock: Clock,
    onBack: () -> Unit,
    onRestore: (RecycleBinItem) -> Unit,
    onPermanentlyDelete: (RecycleBinItem) -> Unit,
    onPurgeAll: () -> Unit,
    modifier: Modifier = Modifier,
    selectedTab: MainTab = MainTab.DAYBOOK,
    onTabSelected: (MainTab) -> Unit = {},
    onNewMoment: () -> Unit = {},
    fixtureState: RecycleBinUiState? = null,
    fixtureClock: Clock? = null,
) {
    val effectiveState = fixtureState ?: uiState
    val effectiveClock = fixtureClock ?: clock
    var pendingDelete by remember { mutableStateOf<RecycleBinItem?>(null) }
    var confirmPurgeAll by remember { mutableStateOf(false) }
    val extended = LocalMmExtendedColors.current

    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // 可滚动内容区
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            // 自定义状态栏 `♥×3 | GRAVEYARD`（design .statusbar{padding:11px 18px 5px;11px mono/800;ink-2}）
            PixelStatusHud(
                leftText = "♥\uFE0E×3",
                rightText = "GRAVEYARD",
                modifier = Modifier.statusBarsPadding(),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // 页题面板
            PageTitlePanel(
                title = "◂ 封印之地",
                hud = "${effectiveState.items.size} 件封存 · 30 天后净化",
                modifier = Modifier.padding(horizontal = MomentMarkTokens.RecycleBinContentPaddingHorizontal),
            )
            Spacer(Modifier.height(MomentMarkTokens.RecycleBinContentGap))
            // 内容区
            Column(
                Modifier.padding(horizontal = MomentMarkTokens.RecycleBinContentPaddingHorizontal),
                verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.RecycleBinContentGap),
            ) {
                when {
                    effectiveState.isLoading -> Text(
                        "▸ LOADING…",
                        style = MaterialTheme.typography.labelSmall,
                        color = extended.labelTertiary,
                    )
                    effectiveState.items.isEmpty() -> PixelPanel(
                        modifier = Modifier.fillMaxWidth(),
                        variant = PixelPanelVariant.Flat,
                    ) {
                        Text(
                            "封印之地暂时空无一物。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(MomentMarkTokens.SpaceInner),
                        )
                    }
                    else -> {
                        effectiveState.items.forEach { item ->
                            val sealedDaysAgo = Duration.between(item.deletedAt, effectiveClock.instant()).toDays().coerceAtLeast(0)
                            val remainingDays = (RecycleDays.minus(Duration.between(item.deletedAt, effectiveClock.instant())).toDays()).coerceAtLeast(0)
                            RecycleBinItemCard(
                                presentation = RecycleBinItemPresentation(
                                    title = item.title,
                                    typeLabel = item.type.label,
                                    sealedDaysAgo = sealedDaysAgo,
                                    remainingDays = remainingDays,
                                ),
                                onRestore = { onRestore(item) },
                                onPermanentlyDelete = { pendingDelete = item },
                            )
                        }
                        // 底部提示文字
                        Text(
                            text = "被净化的时刻与任务将永远消失 · 无法复活",
                            fontSize = MomentMarkTokens.RecycleBinFooterFontSize,
                            lineHeight = MomentMarkTokens.RecycleBinFooterLineHeight,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = MomentMarkTokens.RecycleBinFooterLetterSpacing,
                            color = extended.labelTertiary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = MomentMarkTokens.RecycleBinFooterTopGap),
                        )
                    }
                }
                effectiveState.errorMessage?.let {
                    Text("⚠ $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
        // 底部固定区：全部净化按钮
        if (effectiveState.items.isNotEmpty() && !effectiveState.isLoading) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = MomentMarkTokens.RecycleBinContentPaddingHorizontal,
                        end = MomentMarkTokens.RecycleBinContentPaddingHorizontal,
                        bottom = MomentMarkTokens.RecycleBinPurgeAllContainerPaddingBottom,
                    ),
            ) {
                PurgeAllButton(onClick = { confirmPurgeAll = true })
            }
        }
        // 底部导航（日子簿选中）
        PixelBottomNav(
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            onNewMoment = onNewMoment,
        )
    }

    pendingDelete?.let { item ->
        ConfirmDialog(
            title = "永久净化「${item.title}」？",
            message = "净化后将永远消失，无法复活。",
            confirmLabel = "确认净化",
            onConfirm = { onPermanentlyDelete(item); pendingDelete = null },
            onDismiss = { pendingDelete = null },
        )
    }
    if (confirmPurgeAll) {
        ConfirmDialog(
            title = "净化全部封存物？",
            message = "回收站中的时刻与任务将永远消失，无法复活。",
            confirmLabel = "全部净化",
            onConfirm = { onPurgeAll(); confirmPurgeAll = false },
            onDismiss = { confirmPurgeAll = false },
        )
    }
}

/**
 * ⑩ 全部净化按钮：ghost（panel 底）+ danger-n(#FF6B6F) 文字，width 100%，padding 10dp 垂直，12sp。
 * 带 .px 硬影。不使用通用 PixelButton，因其文字颜色无法设为 MmDangerDark。
 */
@Composable
private fun PurgeAllButton(onClick: () -> Unit) {
    PixelPanel(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "全部净化，清空回收站"
                role = Role.Button
            }
            .clickable(role = Role.Button, onClick = onClick),
        backgroundColor = MaterialTheme.colorScheme.surface,
    ) {
        Text(
            text = "⚔ 全部净化（清空回收站）",
            fontSize = MomentMarkTokens.RecycleBinPurgeAllFontSize,
            lineHeight = MomentMarkTokens.RecycleBinPurgeAllLineHeight,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
            color = MmDangerDark,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = MomentMarkTokens.RecycleBinPurgeAllPaddingVertical),
        )
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    PixelConfirmationDialog(
        title = title,
        message = message,
        confirmLabel = confirmLabel,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}
