package com.cch.momentmark.ui.daybook

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.cch.momentmark.ui.components.*
import com.cch.momentmark.ui.theme.MomentMarkTokens
import com.cch.momentmark.ui.theme.MmSemanticColor

/** Task and management chrome: fixed title/close, independently scrollable body, inherited density. */
@Composable
internal fun TaskActionDialog(
    title: String, tag: String, onDismiss: () -> Unit,
    closeLabel: String = "关闭任务操作", closeTag: String = "task-actions-close",
    subtitle: String? = null, error: String? = null,
    scrollState: ScrollState = rememberScrollState(),
    dismissEnabled: Boolean = true,
    avoidIme: Boolean = false,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val density = LocalDensity.current
    Dialog(onDismissRequest = { if (dismissEnabled) onDismiss() },
        properties = DialogProperties(decorFitsSystemWindows = !avoidIme)) {
        CompositionLocalProvider(LocalDensity provides density) {
            if (avoidIme) {
                val window = (LocalView.current.parent as DialogWindowProvider).window
                SideEffect { window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE) }
            }
            PixelPanel(Modifier.then(if (avoidIme) Modifier.windowInsetsPadding(
                WindowInsets.systemBars.union(WindowInsets.ime)) else Modifier).fillMaxWidth().testTag(tag)) {
                Column(Modifier.fillMaxWidth().padding(MomentMarkTokens.SpaceInner)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceUnit)) {
                        Text(title, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleSmall)
                        TaskActionButton(closeLabel, PixelButtonIcon.Close, onDismiss,
                            Modifier.testTag(closeTag), enabled = dismissEnabled, layout = PixelButtonLayout.Icon)
                    }
                    subtitle?.let { Text(it, Modifier.padding(bottom = MomentMarkTokens.SpaceUnit),
                        color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
                    error?.let { Text("⚠ $it", Modifier.testTag("$tag-error"),
                        color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    Column(Modifier.weight(1f, fill = false).verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(MomentMarkTokens.SpaceCard), content = content)
                    footer?.invoke(this)
                }
            }
        }
    }
}

/** Keep the Chinese label and the Android click on one native node. */
@Composable
internal fun TaskActionButton(label: String, icon: PixelButtonIcon?, onClick: () -> Unit,
    modifier: Modifier = Modifier, variant: PixelButtonVariant = PixelButtonVariant.Ghost,
    enabled: Boolean = true, description: String = label, checked: Boolean? = null,
    layout: PixelButtonLayout = PixelButtonLayout.Compact) {
    var hasFocus by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    PixelButton(label, onClick, modifier.focusProperties { canFocus = enabled }.onFocusChanged { hasFocus = it.isFocused }.clearAndSetSemantics {
        focused = hasFocus
        contentDescription = description
        role = if (checked == null) Role.Button else Role.Switch
        checked?.let {
            toggleableState = if (it) ToggleableState.On else ToggleableState.Off
            stateDescription = if (it) "已开启" else "已关闭"
        }
        if (enabled) onClick(label) { onClick(); true } else disabled()
    }, variant, enabled, icon = icon, selected = checked, layout = layout,
        selectedColors = MmSemanticColor(scheme.secondary, scheme.secondaryContainer, scheme.onSecondaryContainer))
}

@Composable
internal fun TaskActionNote(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
}

@Composable
internal fun TaskActionDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
}
