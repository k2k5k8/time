package com.cch.momentmark.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.cch.momentmark.ui.theme.LocalMmExtendedColors
import com.cch.momentmark.ui.theme.MomentMarkTokens
import java.time.LocalTime

/** All wording and candidate values are supplied by the caller; no storage or clock access. */
data class TimeWheelCopy(
    val title: String, val preview: String, val note: String, val hour: String, val minute: String,
    val now: String, val nextHour: String, val cancel: String, val confirm: String, val clear: String,
)

/** No rollover shortcut: this component edits time only and cannot silently change the date. */
fun nextHourOnSameDay(time: LocalTime): LocalTime? =
    if (time.hour == 23) null else LocalTime.of(time.hour + 1, 0)

@Composable
fun PixelTimeWheel(
    visible: Boolean, selection: LocalTime, copy: TimeWheelCopy, now: LocalTime,
    onSelected: (LocalTime) -> Unit, onConfirm: () -> Unit, onClear: () -> Unit, onDismiss: () -> Unit,
) {
    if (!visible) return
    val density = LocalDensity.current
    val large = density.fontScale >= 1.5f
    val rows = if (large) 3 else 5
    val rowHeight = if (large) MomentMarkTokens.DateWheelLargeRowHeight else MomentMarkTokens.DateWheelRowHeight
    val busy = remember { mutableStateMapOf<String, Boolean>() }
    var closing by remember { mutableStateOf(false) }
    fun finish(action: () -> Unit) { if (!closing) { closing = true; action() } }
    Dialog(onDismissRequest = { finish(onDismiss) }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        CompositionLocalProvider(LocalDensity provides density) {
            val window = (LocalView.current.parent as DialogWindowProvider).window
            SideEffect {
                window.setDimAmount(.5f); window.setWindowAnimations(0)
                // Compose Dialog also applies system-theme flags during its parameter update.
                // Apply the app theme after that update so an open sheet follows theme changes.
                window.decorView.post {
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = false // status bar sits over the dimmed backdrop
                        isAppearanceLightNavigationBars = false // bright gesture handle over the pixel outline band
                    }
                }
            }
            BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.BottomCenter) {
                PixelPanel(Modifier.fillMaxWidth().padding(horizontal = MomentMarkTokens.DateWheelSheetMargin)
                    .heightIn(max = maxHeight - WindowInsets.statusBars.asPaddingValues().calculateTopPadding() - MomentMarkTokens.SpaceInner)
                    .semantics { paneTitle = copy.title }) {
                    Column {
                        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                            .padding(MomentMarkTokens.DateWheelContentPadding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(copy.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text(copy.preview, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                            if (copy.note.isNotBlank()) Text(copy.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.DateWheelColumnGap)) {
                                listOf(copy.hour, copy.minute).forEach { Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                            Box(Modifier.fillMaxWidth().background(LocalMmExtendedColors.current.inputSurface)) {
                                Box(Modifier.fillMaxWidth().padding(top = rowHeight * (rows / 2)).height(rowHeight)
                                    .background(MaterialTheme.colorScheme.surfaceVariant).border(MomentMarkTokens.PxThinBorderWidth, MaterialTheme.colorScheme.tertiary))
                                Row(Modifier.border(MomentMarkTokens.PxThinBorderWidth, MaterialTheme.colorScheme.outline)) {
                                    WheelColumn(copy.hour, selection.hour, 0..23, rows, rowHeight,
                                        { onSelected(selection.withHour(it)) }, { busy["hour"] = it }, Modifier.weight(1f), enabled = !closing)
                                    WheelColumn(copy.minute, selection.minute, 0..59, rows, rowHeight,
                                        { onSelected(selection.withMinute(it)) }, { busy["minute"] = it }, Modifier.weight(1f), enabled = !closing)
                                }
                            }
                            val nextHour = nextHourOnSameDay(now)
                            // Stack shortcuts at large font sizes; every action retains a full touch target.
                            if (large) {
                                WheelAction(copy.now, { onSelected(now) }, Modifier.fillMaxWidth(), false, !closing, icon = PixelButtonIcon.Clock)
                                if (nextHour != null) WheelAction(copy.nextHour, { onSelected(nextHour) }, Modifier.fillMaxWidth(), false, !closing, icon = PixelButtonIcon.Clock)
                            } else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                WheelAction(copy.now, { onSelected(now) }, Modifier.weight(1f), false, !closing, icon = PixelButtonIcon.Clock)
                                if (nextHour != null) WheelAction(copy.nextHour, { onSelected(nextHour) }, Modifier.weight(1f), false, !closing, icon = PixelButtonIcon.Clock)
                            }
                            WheelAction(copy.clear, { finish(onClear) }, Modifier.fillMaxWidth(), false, !closing)
                        }
                        Row(Modifier.fillMaxWidth().padding(horizontal = MomentMarkTokens.DateWheelContentPadding, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            WheelAction(copy.cancel, { finish(onDismiss) }, Modifier.weight(1f), false, !closing, icon = PixelButtonIcon.Close)
                            WheelAction(copy.confirm, { finish(onConfirm) }, Modifier.weight(1.5f), true, !closing && busy.values.none { it }, icon = PixelButtonIcon.Check)
                        }
                        Spacer(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.outline).navigationBarsPadding())
                    }
                }
            }
        }
    }
}
