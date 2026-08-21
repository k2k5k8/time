package com.cch.momentmark.ui.home

import android.app.Activity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.cch.momentmark.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val homeDateFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日")
private val NotoSerifSc = FontFamily(
    Font(R.font.noto_serif_sc_vf, FontWeight.Normal),
)

/**
 * A scene is selected once when a screen enters composition. Keeping the image,
 * quote and tonal treatment together prevents a bright photo from receiving a
 * dark scene's text treatment when the same pool is reused by the create screen.
 */
internal data class HomeHeroScene(
    val imageRes: Int,
    val quote: String,
    val imageTint: Color,
    val bottomShade: Color,
    /** Content color over the home Hero image. */
    val heroTextColor: Color,
    /** Content color over the image-led header on the create screen. */
    val editorTextColor: Color,
    val imageScale: Float,
    val imageShift: Float,
)

internal val HomeHeroScenes = listOf(
    HomeHeroScene(
        imageRes = R.drawable.lake_mountain_landscape,
        quote = "时间总在不经意间流逝\n珍惜当下，期待未来",
        imageTint = Color.Transparent,
        bottomShade = Color(0xCC102238),
        heroTextColor = Color(0xFFFDF9F3),
        editorTextColor = Color(0xFF2D3940),
        imageScale = 1.04f,
        imageShift = 0f,
    ),
    HomeHeroScene(
        imageRes = R.drawable.mountain_lake_sunset,
        quote = "山川湖海会替你记得\n那些认真走过的路",
        imageTint = Color(0x223B244A),
        bottomShade = Color(0xD20D1C2B),
        heroTextColor = Color(0xFFFFFBF5),
        editorTextColor = Color(0xFFFFFBF5),
        imageScale = 1.06f,
        imageShift = -0.02f,
    ),
    HomeHeroScene(
        imageRes = R.drawable.sunset_over_lake,
        quote = "慢一点也没关系\n好风景值得等待",
        imageTint = Color(0x226B3E45),
        bottomShade = Color(0xD0181C2E),
        heroTextColor = Color(0xFFFFFBF5),
        editorTextColor = Color(0xFF2D3940),
        imageScale = 1.08f,
        imageShift = 0.02f,
    ),
    HomeHeroScene(
        imageRes = R.drawable.desert_sunset_arizona,
        quote = "把今天过好\n就是给未来最好的回答",
        imageTint = Color(0x223A2432),
        bottomShade = Color(0xD22A1D2C),
        heroTextColor = Color(0xFFFFFBF5),
        editorTextColor = Color(0xFF2D3940),
        imageScale = 1.06f,
        imageShift = 0f,
    ),
    HomeHeroScene(
        imageRes = R.drawable.view_lake_forest,
        quote = "愿你心里有光\n脚下有路，眼里有春天",
        imageTint = Color(0x223A6B58),
        bottomShade = Color(0xC91B2636),
        heroTextColor = Color(0xFFFFFBF5),
        editorTextColor = Color(0xFF263740),
        imageScale = 1.09f,
        imageShift = -0.02f,
    ),
    HomeHeroScene(
        imageRes = R.drawable.lake_mountain_landscape,
        quote = "每一个今天都是风景\n请温柔地和它相处",
        imageTint = Color(0x182A3450),
        bottomShade = Color(0xD0182434),
        heroTextColor = Color(0xFFFFFBF5),
        editorTextColor = Color(0xFF2D3940),
        imageScale = 1.10f,
        imageShift = 0.025f,
    ),
    HomeHeroScene(
        imageRes = R.drawable.mountain_lake_sunset,
        quote = "不必急着抵达\n沿途也值得收藏",
        imageTint = Color(0x223E2838),
        bottomShade = Color(0xD2171B2A),
        heroTextColor = Color(0xFFFFFBF5),
        editorTextColor = Color(0xFFFFFBF5),
        imageScale = 1.11f,
        imageShift = 0.02f,
    ),
    HomeHeroScene(
        imageRes = R.drawable.view_lake_forest,
        quote = "给心一点留白\n让日子慢慢长成喜欢的样子",
        imageTint = Color(0x183A6B58),
        bottomShade = Color(0xD0202C35),
        heroTextColor = Color(0xFFFFFBF5),
        editorTextColor = Color(0xFF2D3940),
        imageScale = 1.06f,
        imageShift = 0.015f,
    ),
)

private fun Color.usesDarkContent(): Boolean =
    red < 0.55f && green < 0.60f && blue < 0.65f

/** A scene-independent readable backdrop for text placed directly on a photo. */
internal fun contentBackdropFor(contentColor: Color): Color = if (contentColor.usesDarkContent()) {
    Color(0xFFFFFBF5)
} else {
    Color(0xFF101B25)
}

internal fun homeHeroCollapseProgress(
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int,
    collapseDistancePx: Float,
): Float {
    if (collapseDistancePx <= 0f) return 1f
    val scrollOffset = if (firstVisibleItemIndex == 0) {
        firstVisibleItemScrollOffset.toFloat()
    } else {
        collapseDistancePx
    }
    return (scrollOffset / collapseDistancePx).coerceIn(0f, 1f)
}

@Composable
internal fun CollapsibleHeroBackground(
    scene: HomeHeroScene,
    palette: AdaptiveBackgroundPalette,
    collapseProgress: Float,
    heroContentHeight: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    val sceneAlpha = remember(scene.imageRes) { Animatable(0f) }
    LaunchedEffect(scene.imageRes) {
        sceneAlpha.snapTo(0f)
        sceneAlpha.animateTo(1f, animationSpec = tween(durationMillis = 850))
    }
    val backgroundAlpha = sceneAlpha.value * (1f - collapseProgress * 1.08f).coerceIn(0f, 1f)
    val quoteAlpha = sceneAlpha.value * (1f - collapseProgress * 1.35f).coerceIn(0f, 1f)
    val quoteTranslation = (18f - collapseProgress * 28f).dp
    val todayLabel = LocalDate.now().format(homeDateFormatter)
    val heroTextShadow = Shadow(
        color = contentBackdropFor(scene.heroTextColor).copy(alpha = 0.78f),
        offset = Offset(0f, 2.5f),
        blurRadius = 9f,
    )

    Box(modifier = modifier) {
        Image(
            painter = painterResource(scene.imageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = backgroundAlpha
                    translationY = -collapseProgress * 18.dp.toPx()
                    translationX = scene.imageShift * size.width
                    scaleX = scene.imageScale - collapseProgress * 0.04f
                    scaleY = scene.imageScale - collapseProgress * 0.04f
                },
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(backgroundAlpha)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x300E1824),
                            scene.imageTint.copy(alpha = 0.18f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        AdaptiveTransitionLayer(
            palette = palette,
            collapseProgress = collapseProgress,
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(heroContentHeight)
                .padding(horizontal = 32.dp)
                .alpha(quoteAlpha)
                .graphicsLayer {
                    translationY = quoteTranslation.toPx()
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = scene.quote,
                color = scene.heroTextColor.copy(alpha = 0.96f),
                textAlign = TextAlign.Center,
                fontFamily = NotoSerifSc,
                fontSize = 19.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Medium,
                style = TextStyle(shadow = heroTextShadow),
            )
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .width(34.dp)
                    .height(2.dp)
                    .background(scene.heroTextColor.copy(alpha = 0.72f)),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = todayLabel,
                color = scene.heroTextColor.copy(alpha = 0.88f),
                textAlign = TextAlign.Center,
                fontFamily = NotoSerifSc,
                fontSize = 15.sp,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.Medium,
                style = TextStyle(shadow = heroTextShadow),
            )
        }
    }
}

@Composable
internal fun CollapsibleHomeTopBar(
    collapseProgress: Float,
    isSearchVisible: Boolean,
    searchQuery: String,
    searchFocusRequester: FocusRequester,
    onSearchQueryChange: (String) -> Unit,
    onCloseSearch: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenGroups: () -> Unit,
    onOpenSettings: () -> Unit,
    heroTextColor: Color,
    palette: AdaptiveBackgroundPalette,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val context = LocalContext.current
    val surfaceProgress = if (isSearchVisible) 1f else collapseProgress
    val topBarContentColor = lerp(heroTextColor, palette.cardContentColor, surfaceProgress)
    val heroUsesDarkContent = heroTextColor.usesDarkContent()
    // The Hero photo can contain a strong horizon line. Keep enough of the
    // floating bar's surface opaque that photo detail never reads as a divider
    // running through the navigation controls.
    val topBarSurfaceAlpha = if (isSearchVisible) 0.94f else 0.76f + 0.16f * surfaceProgress

    SideEffect {
        val window = (context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
            surfaceProgress > 0.55f || heroUsesDarkContent
    }

    Surface(
        modifier = modifier
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            // Material3 OutlinedTextField lays out its text at a 56dp minimum.
            // Giving the expanded bar that native height prevents its glyphs
            // from being clipped by a 44dp parent.
            .height(if (isSearchVisible) 64.dp else 44.dp)
            .shadow(1.5.dp + 1.5.dp * surfaceProgress, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = palette.uiBaseColor.copy(alpha = topBarSurfaceAlpha),
        contentColor = topBarContentColor,
        border = BorderStroke(
            width = 1.dp,
            color = palette.cardHighlightColor.copy(alpha = 0.18f + 0.12f * surfaceProgress),
        ),
    ) {
        if (isSearchVisible) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onCloseSearch,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "关闭搜索")
                }
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .focusRequester(searchFocusRequester)
                        .clip(RoundedCornerShape(15.dp))
                        .background(palette.cardHighlightColor.copy(alpha = .22f)),
                    singleLine = true,
                    placeholder = { Text("搜索你的时间收藏", color = topBarContentColor.copy(alpha = .62f), fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = null,
                            tint = topBarContentColor.copy(alpha = .72f),
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchQueryChange("") },
                                modifier = Modifier.semantics {
                                    contentDescription = "清空搜索"
                                },
                            ) {
                                Text("×", style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    },
                    shape = RoundedCornerShape(15.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = topBarContentColor,
                        unfocusedTextColor = topBarContentColor,
                        cursorColor = topBarContentColor,
                    ),
                )
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Moment Mark",
                    color = topBarContentColor,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = NotoSerifSc,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.4.sp,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(
                        onClick = onOpenGroups,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(Icons.Outlined.Menu, contentDescription = "打开分类与分组")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onOpenSearch,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(Icons.Outlined.Search, contentDescription = "搜索")
                        }
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(Icons.Outlined.Settings, contentDescription = "设置")
                        }
                    }
                }
            }
        }
    }
}
