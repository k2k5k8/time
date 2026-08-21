package com.cch.momentmark.ui.eventdetail

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.drawToBitmap
import com.cch.momentmark.data.settings.EventDetailStore
import com.cch.momentmark.domain.model.RelatedCountdown
import com.cch.momentmark.domain.model.RepeatRule
import com.cch.momentmark.domain.model.RepeatType
import com.cch.momentmark.domain.model.TimeEvent
import com.cch.momentmark.domain.model.EventTimeType
import com.cch.momentmark.domain.time.EventTimeCalculator
import com.cch.momentmark.domain.time.EventTimeStatus
import com.cch.momentmark.domain.time.InterestingCard
import com.cch.momentmark.domain.time.TimeInterestingGenerator
import com.cch.momentmark.utils.BackgroundManager
import com.cch.momentmark.ui.components.DeleteConfirmationDialog
import com.cch.momentmark.ui.home.HomeHeroScenes
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

private val DetailBackgroundShade = Color(0xFF2A201C).copy(alpha = 0.46f)
private val DetailCard = Color(0xFFFFFBF6).copy(alpha = 0.88f)
private val DetailText = Color(0xFF382B25)
private val DetailMuted = Color(0xFFEFE3D8)
private val DetailAccent = Color(0xFFE8AD8D)
private val DetailDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.CHINA)

@Composable
private fun rememberMinuteClock(): Clock {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            now = System.currentTimeMillis()
        }
    }
    return remember(now) { Clock.fixed(java.time.Instant.ofEpochMilli(now), ZoneId.systemDefault()) }
}

private fun eventTargetDate(event: TimeEvent, clock: Clock): LocalDate? = when (event.timeType) {
    EventTimeType.ALL_DAY -> event.localDate?.let {
        EventTimeCalculator.resolveNextDate(it, event.repeatRule, clock, ZoneId.of(event.zoneId ?: clock.zone.id))
    }
    EventTimeType.TIMED -> event.targetInstant?.let {
        EventTimeCalculator.resolveNextInstant(it, event.repeatRule, clock, ZoneId.of(event.zoneId ?: clock.zone.id))
            .atZone(ZoneId.of(event.zoneId ?: clock.zone.id)).toLocalDate()
    }
}

private fun dateWithWeekday(date: LocalDate): String = "${date.format(DetailDateFormatter)} ${when (date.dayOfWeek.value) {
    1 -> "星期一"; 2 -> "星期二"; 3 -> "星期三"; 4 -> "星期四"; 5 -> "星期五"; 6 -> "星期六"; else -> "星期日"
}}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailFeature(
    event: TimeEvent,
    detailStore: EventDetailStore,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onAddRelated: () -> Unit,
    onEditRelated: (RelatedCountdown) -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val clock = rememberMinuteClock()
    val backgroundNames = remember(context) { BackgroundManager.list(context) }
    val savedBackground by detailStore.selectedBackground(event.id).collectAsState(initial = null)
    var currentBackground by remember(event.id) { mutableStateOf<String?>(null) }
    var showBackgrounds by rememberSaveable(event.id) { mutableStateOf(false) }
    var showInteresting by rememberSaveable(event.id) { mutableStateOf(false) }
    val related by detailStore.relatedCountdowns(event.id).collectAsState(initial = emptyList())
    val snackbar = remember { SnackbarHostState() }
    val targetDate = eventTargetDate(event, clock)
    val countdown = remember(event, clock) {
        when (event.timeType) {
            EventTimeType.ALL_DAY -> event.localDate?.let { EventTimeCalculator.calculate(event.timeType, localDate = it, repeatRule = event.repeatRule, clock = clock, zoneId = ZoneId.of(event.zoneId ?: clock.zone.id)) }
            EventTimeType.TIMED -> event.targetInstant?.let { EventTimeCalculator.calculate(event.timeType, targetInstant = it, repeatRule = event.repeatRule, clock = clock, zoneId = ZoneId.of(event.zoneId ?: clock.zone.id)) }
        }
    }

    LaunchedEffect(event.id, backgroundNames, savedBackground) {
        if (backgroundNames.isNotEmpty()) {
            val selected = savedBackground ?: backgroundNames.random()
            currentBackground = selected
            if (savedBackground == null) detailStore.setBackground(event.id, selected)
        }
    }

    BackHandler(onBack = onBack)
    Box(modifier = Modifier.fillMaxSize()) {
        Crossfade(targetState = currentBackground, label = "detail-background") { assetName ->
            val bitmap = assetName?.let { remember(it) { BackgroundManager.load(context, it) } }
            if (bitmap != null) {
                Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer))
            }
        }
        Box(Modifier.fillMaxSize().background(DetailBackgroundShade))
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    DetailActionBar(
                        onShare = { shareDetail(context, event, countdown?.amount ?: 0L) },
                        onSaveImage = { scope.launch { snackbar.showSnackbar(saveDetailImage(context, view, event.title)) } },
                        onBackground = { showBackgrounds = true },
                        onInteresting = { showInteresting = !showInteresting },
                        onAdd = onAddRelated,
                    )
                }
            },
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp),
            ) {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    DetailCircleButton(
                        if (showInteresting) "返回详情" else "返回",
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        onClick = { if (showInteresting) showInteresting = false else onBack() },
                    )
                    if (showInteresting) {
                        Text("时间趣味发现", modifier = Modifier.weight(1f).padding(horizontal = 14.dp), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, textAlign = TextAlign.Center)
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    DetailCircleButton("编辑", Icons.Outlined.Edit, onEdit)
                }
                AnimatedContent(
                    targetState = showInteresting,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    transitionSpec = { (fadeIn() + scaleIn()) togetherWith fadeOut() },
                    label = "detail-content-transition",
                ) { interesting ->
                    if (interesting) {
                        InterestingPanel(event = event, clock = clock, modifier = Modifier.fillMaxSize())
                    } else {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            Spacer(Modifier.height(34.dp))
                            AnimatedVisibility(visible = countdown != null, enter = fadeIn() + scaleIn(), exit = fadeOut()) {
                                MainDetailCard(event = event, targetDate = targetDate, amount = countdown?.amount ?: 0, status = countdown?.status ?: EventTimeStatus.TODAY)
                            }
                            Spacer(Modifier.height(18.dp))
                            related.forEach { item ->
                                RelatedCountdownCard(item, clock, onClick = { onEditRelated(item) })
                                Spacer(Modifier.height(12.dp))
                            }
                            Spacer(Modifier.height(18.dp))
                        }
                    }
                }
            }
        }
    }

    if (showBackgrounds) {
        ModalBottomSheet(
            onDismissRequest = { showBackgrounds = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFFFFF9F3),
        ) {
            Text("选择详情背景", modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp), style = MaterialTheme.typography.titleLarge, color = DetailText)
            if (backgroundNames.isEmpty()) {
                Text("background 文件夹中暂时没有图片", modifier = Modifier.padding(22.dp), color = DetailText)
            } else {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).navigationBarsPadding()) {
                    backgroundNames.chunked(2).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { name ->
                                BackgroundThumbnail(name, selected = currentBackground == name, onClick = {
                                    currentBackground = name
                                    scope.launch { detailStore.setBackground(event.id, name) }
                                    showBackgrounds = false
                                }, modifier = Modifier.weight(1f))
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun DetailCircleButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.78f)).semantics { contentDescription = label; role = Role.Button }) {
        Icon(icon, contentDescription = null, tint = DetailText)
    }
}

@Composable
private fun MainDetailCard(event: TimeEvent, targetDate: LocalDate?, amount: Long, status: EventTimeStatus) {
    val statusText = when (status) {
        EventTimeStatus.PAST -> "已经过去"
        EventTimeStatus.FUTURE -> "距离目标还有"
        EventTimeStatus.TODAY -> "今天就是目标日"
    }
    Surface(modifier = Modifier.fillMaxWidth().shadow(18.dp, RoundedCornerShape(34.dp)), shape = RoundedCornerShape(34.dp), color = DetailCard) {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(event.title, color = DetailText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(statusText, modifier = Modifier.padding(top = 22.dp), color = DetailText.copy(alpha = 0.68f), fontSize = 15.sp)
            AnimatedContent(targetState = amount, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "detail-number") { value ->
                Text(value.toString(), color = DetailText, fontSize = 72.sp, lineHeight = 78.sp, fontWeight = FontWeight.Black)
            }
            Text(if (status == EventTimeStatus.TODAY) "" else "天", color = DetailText.copy(alpha = 0.64f), fontSize = 16.sp)
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(DetailMuted.copy(alpha = 0.7f)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CalendarMonth, null, tint = DetailText.copy(alpha = 0.72f))
                Column(Modifier.padding(start = 10.dp)) {
                    Text(if (status == EventTimeStatus.PAST) "开始日期" else "目标日期", color = DetailText.copy(alpha = 0.62f), fontSize = 12.sp)
                    Text(targetDate?.let(::dateWithWeekday) ?: event.dateLabel, color = DetailText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun RelatedCountdownCard(item: RelatedCountdown, clock: Clock, onClick: () -> Unit) {
    val result = EventTimeCalculator.calculate(
        EventTimeType.ALL_DAY,
        localDate = item.targetDate,
        repeatRule = if (item.isRepeat) RepeatRule(RepeatType.valueOf(item.repeatType)) else null,
        clock = clock,
    )
    Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(22.dp), color = Color(0xFFFFFBF6).copy(alpha = 0.82f)) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.title, color = DetailText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("目标日期：${item.targetDate}", color = DetailText.copy(alpha = 0.62f), fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(result.amount.toString(), color = DetailText, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text(if (result.status == EventTimeStatus.PAST) "已经过去" else if (result.status == EventTimeStatus.TODAY) "今天" else "还有几天", color = DetailText.copy(alpha = 0.62f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun DetailActionBar(onShare: () -> Unit, onSaveImage: () -> Unit, onBackground: () -> Unit, onInteresting: () -> Unit, onAdd: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        DetailAction("分享", Icons.Outlined.IosShare, onShare)
        DetailAction("保存图片", Icons.Outlined.SaveAlt, onSaveImage)
        DetailAction("背景", Icons.Outlined.Image, onBackground)
        DetailAction("趣味发现", Icons.Outlined.Flag, onInteresting)
        DetailAction("添加小倒计时", Icons.Outlined.Add, onAdd)
    }
}

@Composable
private fun DetailAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(50.dp)
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.48f), Color.White.copy(alpha = 0.20f)),
                ),
                CircleShape,
            )
            .border(1.dp, Color.White.copy(alpha = 0.70f), CircleShape)
            .shadow(12.dp, CircleShape)
            .semantics { contentDescription = label; role = Role.Button },
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(23.dp))
    }
}

@Composable
private fun BackgroundThumbnail(name: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap = remember(name) { BackgroundManager.load(context, name) }
    Box(modifier = modifier.height(150.dp).clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick).then(if (selected) Modifier.background(DetailAccent) else Modifier)) {
        bitmap?.let { Image(it, contentDescription = name, modifier = Modifier.fillMaxSize().padding(if (selected) 3.dp else 0.dp).clip(RoundedCornerShape(15.dp)), contentScale = ContentScale.Crop) }
        if (selected) Text("✓", modifier = Modifier.align(Alignment.TopEnd).padding(8.dp), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InterestingPanel(event: TimeEvent, clock: Clock, modifier: Modifier = Modifier) {
    val cards = remember(event, clock) { TimeInterestingGenerator.generate(event, EventTimeCalculator.today(clock)) }
    BoxWithConstraints(modifier = modifier.padding(horizontal = 2.dp, vertical = 12.dp)) {
        val batchSize = when {
            maxHeight < 420.dp -> 2
            maxHeight < 620.dp -> 3
            else -> 4
        }
        var displayedCards by remember(cards, batchSize) {
            mutableStateOf(pickInterestingBatch(cards, batchSize, emptyList()))
        }
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("围绕这个日期，看看时间留下了哪些痕迹。", modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.82f), fontSize = 13.sp, maxLines = 1)
                TextButton(
                    onClick = { displayedCards = pickInterestingBatch(cards, batchSize, displayedCards) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(17.dp), tint = Color.White)
                    Spacer(Modifier.width(4.dp))
                    Text("换一批", color = Color.White)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                displayedCards.forEachIndexed { index, card ->
                    InterestingCardView(index, card, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun InterestingCardView(index: Int, card: InterestingCard, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = if (index % 3 == 0) Color(0xFFFFE8D7) else Color.White) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (index % 2 == 0) Icons.Outlined.AutoAwesome else Icons.Outlined.Flag, null, tint = DetailAccent)
                Text(card.title, modifier = Modifier.weight(1f).padding(start = 10.dp), color = DetailText, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
            }
            Text(card.value, color = DetailText, fontSize = 20.sp, fontWeight = FontWeight.Black, maxLines = 1, modifier = Modifier.padding(top = 8.dp))
            Text(card.detail, color = DetailText.copy(alpha = 0.68f), fontSize = 12.sp, maxLines = 2, modifier = Modifier.padding(top = 5.dp))
        }
    }
}

private fun pickInterestingBatch(
    cards: List<InterestingCard>,
    size: Int,
    previous: List<InterestingCard>,
): List<InterestingCard> {
    if (cards.size <= size) return cards
    repeat(8) {
        val candidate = cards.shuffled(Random(Random.nextInt())).take(size)
        if (candidate.map { it.title } != previous.map { it.title }) return candidate
    }
    return cards.shuffled().take(size)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelatedCountdownEditorFeature(
    item: RelatedCountdown?,
    onBack: () -> Unit,
    onSave: (RelatedCountdown) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val scene = remember { HomeHeroScenes.random() }
    val editorBackground = Color(0xFFFFF9F2)
    val initialDate = item?.targetDate ?: LocalDate.now()
    var title by rememberSaveable(item?.id) { mutableStateOf(item?.title.orEmpty()) }
    var dateText by rememberSaveable(item?.id) { mutableStateOf(initialDate.toString()) }
    var repeat by rememberSaveable(item?.id) { mutableStateOf(item?.isRepeat ?: false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var showDeleteConfirmation by rememberSaveable(item?.id) { mutableStateOf(false) }
    LaunchedEffect(showDatePicker) {
        if (showDatePicker) {
            val date = runCatching { LocalDate.parse(dateText) }.getOrDefault(initialDate)
            android.app.DatePickerDialog(context, { _, year, month, day ->
                dateText = LocalDate.of(year, month + 1, day).toString(); showDatePicker = false
            }, date.year, date.monthValue - 1, date.dayOfMonth).apply {
                setOnCancelListener { showDatePicker = false }
                show()
            }
        }
    }
    Scaffold(containerColor = editorBackground, contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(editorBackground)) {
            Image(
                painter = painterResource(scene.imageRes),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(310.dp),
                contentScale = ContentScale.Crop,
            )
            Box(
                Modifier.fillMaxWidth().height(360.dp).background(
                    Brush.verticalGradient(
                        listOf(scene.imageTint.copy(alpha = 0.28f), scene.bottomShade.copy(alpha = 0.10f), editorBackground.copy(alpha = 0.68f), editorBackground),
                    ),
                ),
            )
            Column(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.Top) {
                    IconButton(onClick = onBack, modifier = Modifier.size(52.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.76f))) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回", tint = DetailText)
                    }
                    Spacer(Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(if (item == null) "添加小倒计时" else "编辑小倒计时", color = scene.editorTextColor, fontSize = 31.sp, fontWeight = FontWeight.Bold)
                        Text("记录一个只属于当前事件的小目标", color = scene.editorTextColor.copy(alpha = 0.78f), fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = Color.White.copy(alpha = 0.91f), shadowElevation = 3.dp) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("只属于「${item?.title ?: "当前事件"}」详情页，不会出现在主页。", color = DetailText.copy(alpha = 0.65f), fontSize = 14.sp)
                        OutlinedTextField(title, { title = it.take(30); error = null }, modifier = Modifier.fillMaxWidth(), label = { Text("事件名称") }, singleLine = true)
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFFFF5EC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                        ) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = DetailText.copy(alpha = 0.72f))
                                Column(Modifier.padding(start = 12.dp)) {
                                    Text("目标日期", color = DetailText.copy(alpha = 0.62f), fontSize = 12.sp)
                                    Text(dateText, color = DetailText, fontSize = 16.sp, modifier = Modifier.padding(top = 3.dp))
                                }
                            }
                        }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text("是否重复", color = DetailText, fontWeight = FontWeight.Bold); Text("例如每年生日、纪念日", color = DetailText.copy(alpha = 0.6f), fontSize = 13.sp) }
                            Switch(repeat, { repeat = it })
                        }
                    }
                }
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = Color(0xFFFFEBDD).copy(alpha = 0.92f)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Settings, null, tint = DetailAccent); Text("进阶设置", modifier = Modifier.padding(start = 10.dp), color = DetailText, fontWeight = FontWeight.Bold) }
                        Text("后续可继续扩展提醒、标签和展示样式。", color = DetailText.copy(alpha = 0.62f), fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable {
                        val date = runCatching { LocalDate.parse(dateText) }.getOrNull()
                        if (title.isBlank()) error = "请输入事件名称"
                        else if (date == null) error = "请选择正确的日期"
                        else onSave(RelatedCountdown(item?.id ?: "related-${System.currentTimeMillis()}", title.trim(), date, repeat))
                    },
                    shape = RoundedCornerShape(20.dp),
                    color = DetailAccent,
                    shadowElevation = 6.dp,
                ) { Text("保存", modifier = Modifier.fillMaxWidth().padding(vertical = 17.dp), color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
                if (item != null && onDelete != null) {
                    TextButton(onClick = { showDeleteConfirmation = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("删除此小倒计时", color = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(Modifier.height(22.dp))
            }
        }
    }

    if (showDeleteConfirmation && item != null && onDelete != null) {
        DeleteConfirmationDialog(
            title = "删除小倒计时？",
            message = "删除后只会从当前事件详情页移除，不影响主事件。",
            onConfirm = {
                showDeleteConfirmation = false
                onDelete()
            },
            onDismiss = { showDeleteConfirmation = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailEventEditorFeature(event: TimeEvent, onBack: () -> Unit, onSave: (TimeEvent) -> Unit) {
    val context = LocalContext.current
    val zone = ZoneId.of(event.zoneId ?: ZoneId.systemDefault().id)
    val initialDate = event.localDate ?: event.targetInstant?.atZone(zone)?.toLocalDate() ?: LocalDate.now()
    val initialTime = event.targetInstant?.atZone(zone)?.toLocalTime()?.withSecond(0)?.withNano(0) ?: LocalTime.NOON
    var title by rememberSaveable(event.id) { mutableStateOf(event.title) }
    var dateText by rememberSaveable(event.id) { mutableStateOf(initialDate.toString()) }
    var timeText by rememberSaveable(event.id) { mutableStateOf(initialTime.toString().take(5)) }
    var repeat by rememberSaveable(event.id) { mutableStateOf(event.isRepeat) }
    var advancedExpanded by rememberSaveable(event.id) { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(showDatePicker) {
        if (showDatePicker) {
            val date = runCatching { LocalDate.parse(dateText) }.getOrDefault(initialDate)
            android.app.DatePickerDialog(context, { _, year, month, day -> dateText = LocalDate.of(year, month + 1, day).toString(); showDatePicker = false }, date.year, date.monthValue - 1, date.dayOfMonth).apply { setOnCancelListener { showDatePicker = false }; show() }
        }
    }
    Scaffold(topBar = { TopAppBar(title = { Text("修改事件") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "返回") } }) }, containerColor = Color(0xFFFFF8F1)) { padding ->
        val defaultBackground = remember(context) { BackgroundManager.defaultName(context) }
        val backgroundBitmap = defaultBackground?.let { remember(it) { BackgroundManager.load(context, it) } }
        Box(Modifier.fillMaxSize().padding(padding)) {
            backgroundBitmap?.let { Image(it, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            Box(Modifier.fillMaxSize().background(Color(0xFFFFF8F1).copy(alpha = 0.86f)))
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("事件编辑", color = DetailText, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(title, { title = it; error = null }, modifier = Modifier.fillMaxWidth(), label = { Text("事件名称") }, singleLine = true)
            OutlinedTextField(dateText, {}, modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }, label = { Text("目标日期") }, readOnly = true)
            if (event.timeType == EventTimeType.TIMED) Text("精确时间：$timeText（当前编辑页保留原时区和时间）", color = DetailText.copy(alpha = 0.64f), fontSize = 13.sp)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("是否重复", color = DetailText, fontWeight = FontWeight.Bold); Text("跨年份和闰年由统一时间计算器处理", color = DetailText.copy(alpha = 0.6f), fontSize = 13.sp) }
                Switch(repeat, { repeat = it })
            }
            Surface(Modifier.fillMaxWidth().clickable { advancedExpanded = !advancedExpanded }, shape = RoundedCornerShape(18.dp), color = Color(0xFFFFEBDD)) {
                Column(Modifier.padding(16.dp)) { Text("进阶设置 ${if (advancedExpanded) "⌃" else "⌄"}", color = DetailText, fontWeight = FontWeight.Bold); AnimatedVisibility(advancedExpanded) { Text("当前事件的原有备注、提醒和通知配置会被保留。", color = DetailText.copy(alpha = 0.62f), fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) } }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            TextButton(onClick = {
                val date = runCatching { LocalDate.parse(dateText) }.getOrNull()
                if (title.isBlank()) error = "请输入事件名称"
                else if (date == null) error = "请选择正确的日期"
                else {
                    val updated = if (event.timeType == EventTimeType.ALL_DAY) event.copy(title = title.trim(), localDate = date, isRepeat = repeat, repeatRule = if (repeat) event.repeatRule ?: RepeatRule(RepeatType.YEARLY) else null) else event.copy(title = title.trim(), targetInstant = LocalDateTime.of(date, initialTime).atZone(zone).toInstant(), isRepeat = repeat, repeatRule = if (repeat) event.repeatRule ?: RepeatRule(RepeatType.YEARLY) else null)
                    onSave(updated)
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("保存修改", fontSize = 18.sp, color = DetailAccent) }
            }
        }
    }
}

private fun shareDetail(context: Context, event: TimeEvent, amount: Long) {
    val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "${event.title}\n${event.dateLabel}\n当前倒计时：$amount 天") }
    runCatching { context.startActivity(Intent.createChooser(intent, "分享时间详情")) }
}

private suspend fun saveDetailImage(context: Context, view: android.view.View, title: String): String {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return "Android 10 以下暂未自动写入系统相册"
    val bitmap = view.drawToBitmap()
    return withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "${title.take(20)}-${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MomentMark")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values)
            ?: return@withContext "保存图片失败"
        try {
            val compressed = resolver.openOutputStream(uri)?.use { output ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output)
            } ?: false
            check(compressed) { "无法写入相册输出流" }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            "图片已保存到系统相册"
        } catch (error: Exception) {
            resolver.delete(uri, null, null)
            "保存图片失败：${error.message.orEmpty()}"
        } finally {
            bitmap.recycle()
        }
    }
}
