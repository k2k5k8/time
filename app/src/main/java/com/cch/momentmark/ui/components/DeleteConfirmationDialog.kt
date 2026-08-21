package com.cch.momentmark.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cch.momentmark.R

// ── Design tokens (match project warm/cream palette) ─────────────

private val DialogSurface = Color(0xFFFFFCF9)
private val DialogInk = Color(0xFF4F4036)
private val DialogMuted = Color(0xFF9E8D7D)
private val DialogLine = Color(0xFFE9DCD0)
private val DialogDanger = Color(0xFFCC6B4F)
private val DialogDangerTint = Color(0xFFFBEAE4)
private val NotoSerifSc = FontFamily(
    Font(R.font.noto_serif_sc_vf, FontWeight.Normal),
)

private val DialogCornerRadius = 28.dp

/**
 * 暖白奶油风格的删除确认弹窗，替代默认 AlertDialog。
 *
 * @param title        确认标题，如"删除这个事件？"
 * @param message      说明文案
 * @param onConfirm    确认回调
 * @param onDismiss    取消/外部关闭回调
 * @param confirmLabel 确认按钮文字，默认"删除"
 * @param dismissLabel 取消按钮文字，默认"取消"
 */
@Composable
fun DeleteConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmLabel: String = "删除",
    dismissLabel: String = "取消",
) {
    val noRipple = remember { MutableInteractionSource() }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.32f))
                .clickable(
                    interactionSource = noRipple,
                    indication = null,
                ) { onDismiss() }
                .padding(horizontal = 40.dp),
            contentAlignment = Alignment.Center,
        ) {
            DialogContent(
                title = title,
                message = message,
                confirmLabel = confirmLabel,
                dismissLabel = dismissLabel,
                onConfirm = onConfirm,
                onDismiss = onDismiss,
            )
        }
    }
}

@Composable
private fun DialogContent(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val consumeClick = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DialogCornerRadius))
            .background(DialogSurface)
            .clickable(
                interactionSource = consumeClick,
                indication = null,
            ) { /* 消费点击，阻止穿透到背景 */ }
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ── 警告图标 ──
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(DialogDangerTint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = null,
                tint = DialogDanger,
                modifier = Modifier.size(28.dp),
            )
        }

        Spacer(Modifier.height(18.dp))

        // ── 标题 ──
        Text(
            text = title,
            fontFamily = NotoSerifSc,
            fontSize = 19.sp,
            fontWeight = FontWeight.Medium,
            color = DialogInk,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(8.dp))

        // ── 说明文案 ──
        Text(
            text = message,
            fontFamily = NotoSerifSc,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = DialogMuted,
            textAlign = TextAlign.Center,
            lineHeight = 21.sp,
        )

        Spacer(Modifier.height(24.dp))

        // ── 按钮区 ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 取消按钮 — 描边样式
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, DialogLine, RoundedCornerShape(14.dp))
                    .background(Color.Transparent)
                    .clickable { onDismiss() }
                    .semantics {
                        role = Role.Button
                        contentDescription = dismissLabel
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = dismissLabel,
                    fontFamily = NotoSerifSc,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = DialogMuted,
                )
            }

            // 确认删除按钮 — 实心危险色
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(DialogDanger)
                    .clickable { onConfirm() }
                    .semantics {
                        role = Role.Button
                        contentDescription = confirmLabel
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = confirmLabel,
                    fontFamily = NotoSerifSc,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                )
            }
        }
    }
}
