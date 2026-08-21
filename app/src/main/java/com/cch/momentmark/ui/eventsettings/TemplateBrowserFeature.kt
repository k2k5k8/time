package com.cch.momentmark.ui.eventsettings

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cch.momentmark.data.settings.TemplateStore
import com.cch.momentmark.data.templates.TemplateCategory
import com.cch.momentmark.data.templates.TemplateCatalog
import com.cch.momentmark.data.templates.TemplateDefinition
import com.cch.momentmark.domain.model.EventCardTemplateKey
import com.cch.momentmark.domain.model.TimeEvent
import com.cch.momentmark.domain.model.TravelCardConfig
import com.cch.momentmark.domain.model.TravelCardSize
import com.cch.momentmark.ui.EventCard
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import kotlin.math.abs
import androidx.compose.ui.input.pointer.util.VelocityTracker

private val TemplateInk = Color(0xFF4F4036)
private val TemplateMuted = Color(0xFF9E8D7D)
private val TemplateAccent = Color(0xFFE29A72)
private val TemplatePageGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFFFFBF5), Color(0xFFFFEEF0), Color(0xFFF0ECFA)),
)
private val CardSettleEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

private data class TemplateRandomTarget(
    val categoryIndex: Int,
    val templateIndex: Int,
    val requestId: Int,
)

@Composable
private fun TemplateTopActionButton(
    contentDescription: String,
    onClick: () -> Unit,
    active: Boolean = false,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier
            .size(56.dp)
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Button
            }
            .clickable(role = Role.Button, onClick = onClick),
        shape = CircleShape,
        color = if (active) Color(0xFFFFF2EE).copy(alpha = .96f) else Color.White.copy(alpha = .86f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .92f)),
        shadowElevation = 9.dp,
        contentColor = if (active) TemplateAccent else TemplateInk,
    ) {
        Box(contentAlignment = Alignment.Center) { icon() }
    }
}

/**
 * Full-screen template browser. The catalog owns the available metadata; this
 * feature only owns browsing state and binds the selected renderer to the
 * current event.
 */
@Composable
internal fun TemplateBrowserFeature(
    previewEvent: TimeEvent?,
    selectedTemplate: EventCardTemplateKey,
    selectedSize: TravelCardSize,
    onBack: () -> Unit,
    onApply: (TemplateDefinition, TravelCardSize) -> Unit,
) {
    val context = LocalContext.current
    val templateStore = remember(context) { TemplateStore(context) }
    val favoriteIds by templateStore.favoriteTemplates.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()
    var isSearchVisible by rememberSaveable { mutableStateOf(false) }
    var searchText by rememberSaveable { mutableStateOf("") }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    var categoryIndex by rememberSaveable { mutableIntStateOf(0) }
    var templateIndex by rememberSaveable { mutableIntStateOf(0) }
    var browsingSizeName by rememberSaveable { mutableStateOf(selectedSize.name) }
    var randomRequestId by rememberSaveable { mutableIntStateOf(0) }
    var randomTarget by remember { mutableStateOf<TemplateRandomTarget?>(null) }

    val normalizedQuery = searchText.trim()
    val visibleCategories = remember(normalizedQuery, favoritesOnly, favoriteIds) {
        TemplateCatalog.categories.mapNotNull { category ->
            val templates = category.templates.filter { template ->
                val matchesQuery = normalizedQuery.isBlank() || listOf(
                    template.name,
                    template.categoryName,
                    template.description,
                    template.keywords.joinToString(" "),
                ).joinToString(" ").contains(normalizedQuery, ignoreCase = true)
                matchesQuery && (!favoritesOnly || template.id in favoriteIds)
            }
            category.takeIf { templates.isNotEmpty() }?.copy(templates = templates)
        }
    }

    LaunchedEffect(visibleCategories) {
        categoryIndex = categoryIndex.coerceIn(0, (visibleCategories.size - 1).coerceAtLeast(0))
        val category = visibleCategories.getOrNull(categoryIndex)
        templateIndex = templateIndex.coerceIn(0, (category?.templates?.size?.minus(1) ?: 0).coerceAtLeast(0))
    }
    LaunchedEffect(Unit) {
        val initialCategory = visibleCategories.indexOfFirst { category ->
            category.templates.any { it.templateKey == selectedTemplate }
        }
        if (initialCategory >= 0) {
            categoryIndex = initialCategory
            templateIndex = visibleCategories[initialCategory].templates.indexOfFirst { it.templateKey == selectedTemplate }
                .coerceAtLeast(0)
        }
    }

    val currentCategory = visibleCategories.getOrNull(categoryIndex)
    val currentTemplates = currentCategory?.templates.orEmpty()
    val safeTemplateIndex = templateIndex.coerceIn(0, (currentTemplates.size - 1).coerceAtLeast(0))
    val currentTemplate = currentTemplates.getOrNull(safeTemplateIndex)
    val supportedSizes = currentTemplate?.let { template ->
        TravelCardSize.entries.filter { it in template.supportedSizes }
    }.orEmpty()
    val browsingSize = supportedSizes.firstOrNull { it.name == browsingSizeName }
        ?: supportedSizes.firstOrNull()
        ?: selectedSize
    val safeSizeIndex = supportedSizes.indexOf(browsingSize).coerceAtLeast(0)
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                ) {
                    TemplateTopActionButton(
                        contentDescription = "返回",
                        onClick = onBack,
                        icon = {
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(30.dp),
                            )
                        },
                        modifier = Modifier.align(Alignment.CenterStart),
                    )
                    Row(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        TemplateTopActionButton(
                            contentDescription = "搜索模板",
                            onClick = { isSearchVisible = !isSearchVisible },
                            active = isSearchVisible,
                            icon = {
                                Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(29.dp))
                            },
                        )
                        TemplateTopActionButton(
                            contentDescription = "我的收藏模板",
                            onClick = { favoritesOnly = !favoritesOnly },
                            active = favoritesOnly,
                            icon = {
                                Icon(Icons.Outlined.FolderOpen, contentDescription = null, modifier = Modifier.size(28.dp))
                            },
                        )
                    }
                }
                if (isSearchVisible) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .height(52.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = .66f)),
                        singleLine = true,
                        placeholder = { Text("搜索模板、分类或关键词", color = TemplateMuted, fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Search, contentDescription = null, tint = TemplateAccent)
                        },
                        trailingIcon = {
                            IconButton(onClick = {
                                searchText = ""
                                isSearchVisible = false
                            }) { Text("×", fontSize = 22.sp) }
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = TemplateInk,
                            unfocusedTextColor = TemplateInk,
                            cursorColor = TemplateAccent,
                        ),
                    )
                }
            }
        },
        bottomBar = {
            TemplateActionDock(
                isFavorite = currentTemplate?.id in favoriteIds,
                onToggleFavorite = {
                    currentTemplate?.let { template ->
                        scope.launch { templateStore.setFavorite(template.id, template.id !in favoriteIds) }
                    }
                },
                onApply = { currentTemplate?.let { onApply(it, browsingSize) } },
                onRandom = {
                    if (visibleCategories.isNotEmpty()) {
                        val newCategory = visibleCategories.indices.random()
                        randomRequestId += 1
                        randomTarget = TemplateRandomTarget(
                            categoryIndex = newCategory,
                            templateIndex = visibleCategories[newCategory].templates.indices.random(),
                            requestId = randomRequestId,
                        )
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TemplatePageGradient),
        ) {
            if (currentCategory == null || currentTemplate == null) {
                EmptyTemplateState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    favoritesOnly = favoritesOnly,
                    onReset = {
                        favoritesOnly = false
                        searchText = ""
                        isSearchVisible = false
                    },
                )
            } else {
                TemplateBrowserContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    category = currentCategory,
                    categories = visibleCategories,
                    categoryPosition = categoryIndex,
                    categoryCount = visibleCategories.size,
                    templateIndex = safeTemplateIndex,
                    supportedSizes = supportedSizes,
                    sizeIndex = safeSizeIndex,
                    selectedSize = browsingSize,
                    previewEvent = previewEvent,
                    onCategorySwipe = { direction ->
                        if (visibleCategories.isNotEmpty()) {
                            val nextIndex = (categoryIndex + direction).coerceIn(0, visibleCategories.lastIndex)
                            if (nextIndex != categoryIndex) {
                                categoryIndex = nextIndex
                                templateIndex = 0
                            }
                        }
                    },
                    onSizeSwipe = { direction ->
                        if (supportedSizes.isNotEmpty()) {
                            val nextIndex = (safeSizeIndex + direction).coerceIn(0, supportedSizes.lastIndex)
                            if (nextIndex != safeSizeIndex) browsingSizeName = supportedSizes[nextIndex].name
                        }
                    },
                    onSizeChange = { size ->
                        if (size in currentTemplate.supportedSizes) browsingSizeName = size.name
                    },
                    randomTarget = randomTarget,
                    onRandomTargetSettled = { target ->
                        categoryIndex = target.categoryIndex
                        templateIndex = target.templateIndex
                        randomTarget = null
                    },
                )
            }
        }
    }
}

private enum class TemplateSwipeAxis {
    HORIZONTAL,
    VERTICAL,
}

private fun isSwipeBlocked(
    axis: TemplateSwipeAxis?,
    offset: Float,
    categoryIndex: Int,
    categoryCount: Int,
    sizeIndex: Int,
    sizeCount: Int,
): Boolean = when (axis) {
    TemplateSwipeAxis.VERTICAL ->
        // The lower preview becomes the next centre card when the finger moves
        // up; the upper preview does the reverse on a downward drag.
        (offset < 0f && categoryIndex >= categoryCount - 1) || (offset > 0f && categoryIndex <= 0)
    TemplateSwipeAxis.HORIZONTAL ->
        (offset > 0f && sizeIndex <= 0) || (offset < 0f && sizeIndex >= sizeCount - 1)
    null -> true
}

@Composable
private fun TemplateBrowserContent(
    modifier: Modifier,
    category: TemplateCategory,
    categories: List<TemplateCategory>,
    categoryPosition: Int,
    categoryCount: Int,
    templateIndex: Int,
    supportedSizes: List<TravelCardSize>,
    sizeIndex: Int,
    selectedSize: TravelCardSize,
    previewEvent: TimeEvent?,
    onCategorySwipe: (Int) -> Unit,
    onSizeSwipe: (Int) -> Unit,
    onSizeChange: (TravelCardSize) -> Unit,
    randomTarget: TemplateRandomTarget?,
    onRandomTargetSettled: (TemplateRandomTarget) -> Unit,
) {
    val current = category.templates[templateIndex]

    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Keep the screen-level title as the only introduction. Removing this
        // category headline gives the upper and lower preview cards enough
        // vertical room to fade out beyond the viewport like a real stack.
        TemplateCarouselStage(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            categories = categories,
            categoryPosition = categoryPosition,
            template = current,
            supportedSizes = supportedSizes,
            sizeIndex = sizeIndex,
            selectedSize = selectedSize,
            previewEvent = previewEvent,
            onCategorySwipe = onCategorySwipe,
            onSizeSwipe = onSizeSwipe,
            randomTarget = randomTarget,
            onRandomTargetSettled = onRandomTargetSettled,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("左右滑动尺寸", color = TemplateMuted, fontSize = 12.sp)
            Spacer(Modifier.width(8.dp))
            supportedSizes.forEach { size ->
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .clickable { onSizeChange(size) },
                    shape = CircleShape,
                    color = if (selectedSize == size) TemplateAccent else Color.White.copy(alpha = .75f),
                    contentColor = if (selectedSize == size) Color.White else TemplateInk,
                    border = BorderStroke(1.dp, if (selectedSize == size) TemplateAccent else Color.White),
                ) {
                    Text(
                        if (size == TravelCardSize.WIDE) "大卡" else "小卡",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun TemplateCarouselStage(
    modifier: Modifier,
    categories: List<TemplateCategory>,
    categoryPosition: Int,
    template: TemplateDefinition,
    supportedSizes: List<TravelCardSize>,
    sizeIndex: Int,
    selectedSize: TravelCardSize,
    previewEvent: TimeEvent?,
    onCategorySwipe: (Int) -> Unit,
    onSizeSwipe: (Int) -> Unit,
    randomTarget: TemplateRandomTarget?,
    onRandomTargetSettled: (TemplateRandomTarget) -> Unit,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val latestCategorySwipe by rememberUpdatedState(onCategorySwipe)
    val latestSizeSwipe by rememberUpdatedState(onSizeSwipe)
    val latestCategoryIndex by rememberUpdatedState(categoryPosition)
    val latestCategoryCount by rememberUpdatedState(categories.size)
    val latestSizeIndex by rememberUpdatedState(sizeIndex)
    val latestSizeCount by rememberUpdatedState(supportedSizes.size)
    val latestSizeSlot by rememberUpdatedState(selectedSize.ordinal.toFloat())
    // Keep the physics state independent from the active category/template.
    // Changing an index moves already composed cards; it does not rebuild the
    // viewport or restart an in-progress gesture.
    // The visual positions own the animation. They are deliberately separate
    // from the selected indices so changing selection at the end of a gesture
    // cannot reset a card back to its old slot for a frame.
    val categoryVisualPosition = remember { Animatable(categoryPosition.toFloat()) }
    // Size is a global Small/Wide slot, not a per-category list index. Every
    // vertical row therefore inherits exactly the same horizontal coordinate.
    val sizeVisualPosition = remember { Animatable(selectedSize.ordinal.toFloat()) }
    var swipeAxis by remember { mutableStateOf<TemplateSwipeAxis?>(null) }
    var settlingAxis by remember { mutableStateOf<TemplateSwipeAxis?>(null) }
    var dragTotal by remember { mutableStateOf(Offset.Zero) }
    var liveDragOffset by remember { mutableStateOf(0f) }

    LaunchedEffect(categoryPosition, swipeAxis, settlingAxis) {
        if (swipeAxis != TemplateSwipeAxis.VERTICAL && settlingAxis != TemplateSwipeAxis.VERTICAL) {
            categoryVisualPosition.snapTo(categoryPosition.toFloat())
        }
    }
    LaunchedEffect(selectedSize, swipeAxis, settlingAxis) {
        if (swipeAxis != TemplateSwipeAxis.HORIZONTAL && settlingAxis != TemplateSwipeAxis.HORIZONTAL) {
            sizeVisualPosition.snapTo(selectedSize.ordinal.toFloat())
        }
    }
    // Random browsing uses the same physical coordinate as a gesture.  The
    // selected index is committed only after the travelling card reaches its
    // fixed centre slot, so it never visually teleports to its destination.
    LaunchedEffect(randomTarget?.requestId) {
        val target = randomTarget ?: return@LaunchedEffect
        if (target.categoryIndex == categoryPosition) {
            onRandomTargetSettled(target)
            return@LaunchedEffect
        }
        swipeAxis = null
        settlingAxis = TemplateSwipeAxis.VERTICAL
        categoryVisualPosition.snapTo(categoryPosition.toFloat())
        categoryVisualPosition.animateTo(
            targetValue = target.categoryIndex.toFloat(),
            animationSpec = tween(durationMillis = 460, easing = CardSettleEasing),
        )
        settlingAxis = null
        onRandomTargetSettled(target)
    }

    BoxWithConstraints(
        modifier = modifier
            // Clip only the travelling cards. The stage itself deliberately has
            // no panel, so previews can dissolve into the page background.
            .clip(RoundedCornerShape(32.dp)),
        contentAlignment = Alignment.Center,
    ) {
        val horizontalStep = with(density) { maxWidth.toPx() * .60f }
        // One step is deliberately close to a card height: the two neighbours
        // are visible only at the stage edges, rather than overlaying the
        // centre card. This mirrors the reference's hidden/fading stack.
        val verticalStep = with(density) { maxHeight.toPx() * .56f }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(horizontalStep, verticalStep) {
                    val tracker = VelocityTracker()
                    detectDragGestures(
                        onDragStart = {
                            dragTotal = Offset.Zero
                            swipeAxis = null
                            settlingAxis = null
                            liveDragOffset = 0f
                            tracker.resetTracking()
                        },
                        onDragEnd = {
                            val velocity = tracker.calculateVelocity()
                            val axis = swipeAxis
                            val releasedOffset = if (axis == TemplateSwipeAxis.HORIZONTAL) dragTotal.x else dragTotal.y
                            val movement = if (axis == TemplateSwipeAxis.HORIZONTAL) abs(dragTotal.x) else abs(dragTotal.y)
                            val speed = if (axis == TemplateSwipeAxis.HORIZONTAL) abs(velocity.x) else abs(velocity.y)
                            val step = if (axis == TemplateSwipeAxis.HORIZONTAL) horizontalStep else verticalStep
                            val blocked = isSwipeBlocked(
                                axis = axis,
                                offset = releasedOffset,
                                categoryIndex = latestCategoryIndex,
                                categoryCount = latestCategoryCount,
                                sizeIndex = latestSizeIndex,
                                sizeCount = latestSizeCount,
                            )
                            val direction = if (axis != null && !blocked && (movement > step * .18f || speed > 900f)) {
                                when (axis) {
                                    TemplateSwipeAxis.HORIZONTAL -> if (dragTotal.x < 0f) 1 else -1
                                    TemplateSwipeAxis.VERTICAL -> if (dragTotal.y < 0f) 1 else -1
                                }
                            } else 0
                            scope.launch {
                                if (axis == null) {
                                    dragTotal = Offset.Zero
                                    liveDragOffset = 0f
                                    return@launch
                                }
                                val currentPosition = if (axis == TemplateSwipeAxis.HORIZONTAL) {
                                    latestSizeSlot
                                } else {
                                    latestCategoryIndex.toFloat()
                                }
                                val releasedPosition = when (axis) {
                                    TemplateSwipeAxis.HORIZONTAL -> currentPosition - releasedOffset / step
                                    TemplateSwipeAxis.VERTICAL -> currentPosition - releasedOffset / step
                                }
                                val targetPosition = currentPosition + direction
                                if (axis == TemplateSwipeAxis.HORIZONTAL) {
                                    sizeVisualPosition.snapTo(if (blocked) currentPosition else releasedPosition)
                                } else {
                                    categoryVisualPosition.snapTo(if (blocked) currentPosition else releasedPosition)
                                }
                                // Switch to the continuous visual coordinate
                                // before removing the live drag source.
                                settlingAxis = axis.takeUnless { blocked }
                                swipeAxis = null
                                if (axis == TemplateSwipeAxis.HORIZONTAL) {
                                    sizeVisualPosition.animateTo(
                                        targetValue = targetPosition,
                                        animationSpec = tween(durationMillis = 420, easing = CardSettleEasing),
                                    )
                                } else {
                                    categoryVisualPosition.animateTo(
                                        targetValue = targetPosition,
                                        animationSpec = tween(durationMillis = 420, easing = CardSettleEasing),
                                    )
                                }
                                if (direction != 0) {
                                    if (axis == TemplateSwipeAxis.HORIZONTAL) latestSizeSwipe(direction) else latestCategorySwipe(direction)
                                }
                                dragTotal = Offset.Zero
                                settlingAxis = null
                                liveDragOffset = 0f
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                val axis = swipeAxis
                                if (axis == TemplateSwipeAxis.HORIZONTAL) {
                                    sizeVisualPosition.snapTo(latestSizeSlot - dragTotal.x / horizontalStep)
                                } else if (axis == TemplateSwipeAxis.VERTICAL) {
                                    categoryVisualPosition.snapTo(latestCategoryIndex - dragTotal.y / verticalStep)
                                }
                                settlingAxis = axis
                                swipeAxis = null
                                if (axis == TemplateSwipeAxis.HORIZONTAL) {
                                    sizeVisualPosition.animateTo(latestSizeSlot, tween(260, easing = CardSettleEasing))
                                } else if (axis == TemplateSwipeAxis.VERTICAL) {
                                    categoryVisualPosition.animateTo(latestCategoryIndex.toFloat(), tween(260, easing = CardSettleEasing))
                                }
                                dragTotal = Offset.Zero
                                settlingAxis = null
                                liveDragOffset = 0f
                            }
                        },
                    ) { change, amount ->
                        tracker.addPosition(change.uptimeMillis, change.position)
                        dragTotal += amount
                        if (swipeAxis == null && dragTotal.getDistance() > 12f) {
                            swipeAxis = if (abs(dragTotal.x) > abs(dragTotal.y)) TemplateSwipeAxis.HORIZONTAL else TemplateSwipeAxis.VERTICAL
                        }
                        swipeAxis?.let { axis ->
                            val interactiveOffset = if (axis == TemplateSwipeAxis.HORIZONTAL) dragTotal.x else dragTotal.y
                            if (isSwipeBlocked(
                                    axis = axis,
                                    offset = interactiveOffset,
                                    categoryIndex = latestCategoryIndex,
                                    categoryCount = latestCategoryCount,
                                    sizeIndex = latestSizeIndex,
                                    sizeCount = latestSizeCount,
                                )
                            ) {
                                liveDragOffset = 0f
                                return@let
                            }
                            // The live drag value is stored in composition state;
                            // Animatable takes over only for the release settle.
                            liveDragOffset = interactiveOffset
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
        
        val horizontalVisual = when {
            swipeAxis == TemplateSwipeAxis.HORIZONTAL -> latestSizeSlot - liveDragOffset / horizontalStep
            settlingAxis == TemplateSwipeAxis.HORIZONTAL -> sizeVisualPosition.value
            else -> latestSizeSlot
        }
        val verticalVisual = when {
            swipeAxis == TemplateSwipeAxis.VERTICAL -> latestCategoryIndex - liveDragOffset / verticalStep
            settlingAxis == TemplateSwipeAxis.VERTICAL -> categoryVisualPosition.value
            else -> categoryPosition.toFloat()
        }

        // Every category owns a complete horizontal row. The active size column
        // is shared by all rows so row 2 / column 2 can transition directly to
        // row 1 / column 2 without first snapping to column 1.
        categories.indices
            .filter { index -> abs(index - verticalVisual) <= 1.35f }
            .sortedByDescending { index -> abs(index - verticalVisual) }
            .forEach { index ->
                val previewCategory = categories[index]
                val isCurrentRow = index == categoryPosition
                val rowTemplate = if (isCurrentRow) template else previewCategory.templates.firstOrNull() ?: return@forEach
                val rowSizes = if (isCurrentRow) supportedSizes else {
                    TravelCardSize.entries.filter { it in rowTemplate.supportedSizes }
                }
                // index - visual is a stable signed stack coordinate: previous
                // templates remain above, next templates remain below, and the
                // three cards exchange these physical slots continuously.
                val categoryTranslationY = (index - verticalVisual) * verticalStep
                val distance = abs(categoryTranslationY) / verticalStep
                key(previewCategory.id) {
                    TemplateCategoryRow(
                        template = rowTemplate,
                        supportedSizes = rowSizes,
                        // Never exchange a preview-row index for a centre-row
                        // index. This shared coordinate is the hand-off that
                        // keeps the new card's left size preview continuous.
                        sizePosition = horizontalVisual,
                        previewEvent = previewEvent,
                        horizontalStep = horizontalStep,
                        verticalScale = verticalStackScale(distance),
                        verticalAlpha = verticalStackAlpha(distance),
                        verticalRotationX = verticalStackRotation(categoryTranslationY, verticalStep),
                        verticalTransformOrigin = TransformOrigin(
                            pivotFractionX = .5f,
                            pivotFractionY = if (categoryTranslationY < 0f) 1f else 0f,
                        ),
                        // Shorter camera distance is intentional here: the
                        // previews must read as tilted cards at rest, not only
                        // as a barely-compressed vertical translation.
                        verticalCameraDistance = 4f * density.density,
                        horizontalSideReveal = horizontalSideReveal(distance),
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationY = categoryTranslationY
                            },
                        shadowElevation = if (distance < .2f) 18.dp else 4.dp,
                    )
                }
            }
        }
        Text(
            text = "模板 ${categoryPosition + 1} / ${categories.size}",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 10.dp)
                .background(Color.White.copy(alpha = .46f), RoundedCornerShape(12.dp))
                .padding(horizontal = 9.dp, vertical = 5.dp),
            color = TemplateMuted,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun TemplateCategoryRow(
    template: TemplateDefinition,
    supportedSizes: List<TravelCardSize>,
    sizePosition: Float,
    previewEvent: TimeEvent?,
    horizontalStep: Float,
    verticalScale: Float,
    verticalAlpha: Float,
    verticalRotationX: Float,
    verticalTransformOrigin: TransformOrigin,
    verticalCameraDistance: Float,
    horizontalSideReveal: Float,
    modifier: Modifier,
    shadowElevation: androidx.compose.ui.unit.Dp,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Each size owns a stable visual slot. During a swipe, the slot moves
        // through the shared floating coordinate instead of being replaced by a
        // newly selected card at the end of the animation.
        supportedSizes
            .sortedByDescending { size -> abs(size.ordinal - sizePosition) }
            .forEach { size ->
                val translationX = (size.ordinal - sizePosition) * horizontalStep
                val distance = abs(translationX) / horizontalStep
                val isCentreSize = distance < .02f
            TemplatePreviewCard(
                template = template,
                event = previewEvent.forTemplate(template, size),
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(.66f)
                    .graphicsLayer {
                        this.translationX = translationX
                        scaleX = cardScale(abs(translationX), horizontalStep) * verticalScale
                        scaleY = scaleX
                        // Vertical neighbours retain only their main card.
                        // Their horizontal Small/Wide companions fade in
                        // continuously as they become the centre row.
                        alpha = cardAlpha(abs(translationX), horizontalStep) *
                            verticalAlpha * if (isCentreSize) 1f else horizontalSideReveal
                        // Apply 3D to the actual card bounds, never the large
                        // stage-sized row. This makes the preview's top/bottom
                        // edges form the visible reference-style trapezoid.
                        rotationX = verticalRotationX
                        transformOrigin = verticalTransformOrigin
                        cameraDistance = verticalCameraDistance
                    },
                shadowElevation = if (distance < .5f) shadowElevation else shadowElevation / 2,
            )
            }
    }
}

private fun cardScale(distance: Float, step: Float): Float {
    return 1f - (distance / step).coerceIn(0f, 1f) * .18f
}

private fun cardAlpha(distance: Float, step: Float): Float {
    return 1f - (distance / step).coerceIn(0f, 1f) * .58f
}

private fun verticalStackScale(distance: Float): Float = when {
    distance <= 1f -> 1f - distance * .13f
    else -> .87f
}

private fun verticalStackAlpha(distance: Float): Float = when {
    distance <= 1f -> 1f - distance * .58f
    else -> .42f
}

private fun verticalStackRotation(translationY: Float, step: Float): Float =
    (translationY / step).coerceIn(-1f, 1f) * 24f

private fun horizontalSideReveal(verticalDistance: Float): Float =
    (1f - verticalDistance).coerceIn(0f, 1f)

@Composable
private fun TemplatePreviewCard(
    template: TemplateDefinition,
    event: TimeEvent?,
    modifier: Modifier,
    shadowElevation: androidx.compose.ui.unit.Dp = 16.dp,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = Color.White.copy(alpha = .84f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .9f)),
        shadowElevation = shadowElevation,
    ) {
        Column(
            modifier = Modifier.padding(11.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(template.categoryName, color = TemplateAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(template.name, color = TemplateInk, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            event?.let { EventCard(event = it) }
            Text(template.description, color = TemplateMuted, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun TemplateActionDock(
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onApply: () -> Unit,
    onRandom: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(28.dp),
        color = Color.White.copy(alpha = .76f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .95f)),
        shadowElevation = 12.dp,
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TemplateDockButton(
                    label = "收藏",
                    icon = { Icon(if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, contentDescription = null, tint = if (isFavorite) Color(0xFFE66C71) else TemplateInk) },
                    onClick = onToggleFavorite,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    modifier = Modifier
                        .weight(1.35f)
                        .height(54.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFFE69AA8), Color(0xFFE39B78))))
                        .clickable(role = Role.Button, onClick = onApply)
                        .semantics { contentDescription = "应用模板" },
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Transparent,
                    contentColor = Color.White,
                ) {
                    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("应用模板", fontWeight = FontWeight.Bold)
                    }
                }
                TemplateDockButton(
                    label = "随机看看",
                    icon = { Icon(Icons.Outlined.Refresh, contentDescription = null, tint = TemplateInk) },
                    onClick = onRandom,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 7.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Lightbulb, contentDescription = null, tint = TemplateMuted, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("左右滑动预览更多尺寸效果", color = TemplateMuted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun TemplateDockButton(
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        icon()
        Text(label, color = TemplateInk, fontSize = 10.sp)
    }
}

@Composable
private fun EmptyTemplateState(modifier: Modifier, favoritesOnly: Boolean, onReset: () -> Unit) {
    Column(
        modifier = modifier.padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(if (favoritesOnly) "还没有收藏模板" else "没有找到匹配模板", color = TemplateInk, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(if (favoritesOnly) "先收藏喜欢的模板，它们会出现在这里。" else "换一个关键词试试。", color = TemplateMuted, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        Surface(
            modifier = Modifier.padding(top = 18.dp).clickable(onClick = onReset),
            shape = CircleShape,
            color = TemplateAccent,
            contentColor = Color.White,
        ) { Text("查看全部", modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp), fontSize = 13.sp) }
    }
}

private fun TimeEvent?.forTemplate(template: TemplateDefinition, size: TravelCardSize): TimeEvent? {
    return this?.copy(
        cardTemplateKey = template.templateKey,
        travelCardConfig = (travelCardConfig ?: TravelCardConfig()).copy(
            title = title,
            targetDate = localDate ?: targetInstant?.atZone(java.time.ZoneId.systemDefault())?.toLocalDate()
                ?: TravelCardConfig().targetDate,
            size = size,
        ),
    )
}
