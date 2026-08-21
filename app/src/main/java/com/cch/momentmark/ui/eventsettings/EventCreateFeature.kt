package com.cch.momentmark.ui.eventsettings

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.Image
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cch.momentmark.R
import com.cch.momentmark.domain.model.EventCardPaletteKey
import com.cch.momentmark.domain.model.EventCardTemplateKey
import com.cch.momentmark.domain.model.EventColorRole
import com.cch.momentmark.domain.model.EventCoverPreset
import com.cch.momentmark.domain.model.EventTimeType
import com.cch.momentmark.domain.model.NotificationMethod
import com.cch.momentmark.domain.model.ReminderConfig
import com.cch.momentmark.domain.model.RepeatCustomUnit
import com.cch.momentmark.domain.model.RepeatRule
import com.cch.momentmark.domain.model.RepeatType
import com.cch.momentmark.domain.model.TimeEvent
import com.cch.momentmark.domain.model.TravelCardConfig
import com.cch.momentmark.domain.model.TravelCardSize
import com.cch.momentmark.domain.time.EventTimeCalculator
import com.cch.momentmark.ui.EventCard
import com.cch.momentmark.ui.eventsettings.eventTemplateLabel
import com.cch.momentmark.ui.home.HomeHeroScenes
import com.cch.momentmark.ui.home.contentBackdropFor
import com.cch.momentmark.utils.BackgroundManager
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val editorDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val editorTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val displayDateFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA)

private val FormBackground = Color(0xFFFFF9F2)
private val WarmBrown = Color(0xFF4F4036)
private val WarmMuted = Color(0xFF9E8D7D)
private val WarmLine = Color(0xFFE9DCD0)
private val WarmAccent = Color(0xFFE9B18F)
private val WarmAccentDark = Color(0xFFB98263)
private val WarmCard = Color(0xFFFFFCF9).copy(alpha = 0.94f)
private val NotoSerifSc = FontFamily(
    Font(R.font.noto_serif_sc_vf, FontWeight.Normal),
)

private val GroupOptions = listOf("未分组", "纪念日", "生日", "节日", "工作", "学习", "旅行", "生活")

private data class ReminderOption(val label: String, val minutes: Int?)

private val ReminderOptions = listOf(
    ReminderOption("不提醒", null),
    ReminderOption("当天提醒", 0),
    ReminderOption("提前 10 分钟", 10),
    ReminderOption("提前 30 分钟", 30),
    ReminderOption("提前 1 小时", 60),
    ReminderOption("提前 1 天", 24 * 60),
    ReminderOption("提前 1 周", 7 * 24 * 60),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EventCreateFeature(
    initialEvent: TimeEvent? = null,
    initialDate: LocalDate? = null,
    onBack: () -> Unit,
    onSave: (TimeEvent) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val clock = remember { Clock.systemDefaultZone() }
    // The create screen is entered as a fresh composition after returning to
    // Home, so this changes once per entry but stays stable while typing.
    val scene = remember { HomeHeroScenes.random() }
    val editorTextColor = scene.editorTextColor
    val editorHeaderBackdrop = contentBackdropFor(editorTextColor).copy(alpha = 0.76f)
    val fixedBackgroundName = remember(context, initialEvent?.id) {
        initialEvent?.let { BackgroundManager.defaultName(context) }
    }
    val fixedBackgroundBitmap = fixedBackgroundName?.let { remember(it) { BackgroundManager.load(context, it) } }
    val zoneId = remember(initialEvent?.id, initialDate) {
        initialEvent?.zoneId?.let { runCatching { ZoneId.of(it) }.getOrNull() }
            ?: ZoneId.systemDefault()
    }
    val defaultDate = remember(initialEvent?.id, initialDate) {
        initialEvent?.localDate
            ?: initialEvent?.targetInstant?.atZone(zoneId)?.toLocalDate()
            ?: initialDate
            ?: EventTimeCalculator.today(clock, zoneId).plusDays(1)
    }
    val defaultTime = remember {
        initialEvent?.targetInstant?.atZone(zoneId)?.toLocalTime()
            ?: clock.instant().plusSeconds(3600).atZone(zoneId).toLocalTime()
            .withSecond(0)
            .withNano(0)
    }

    var title by rememberSaveable(initialEvent?.id, initialDate) { mutableStateOf(initialEvent?.title.orEmpty()) }
    var subtitle by rememberSaveable(initialEvent?.id, initialDate) { mutableStateOf(initialEvent?.subtitle.orEmpty()) }
    var groupName by rememberSaveable(initialEvent?.id, initialDate) { mutableStateOf(initialEvent?.groupLabel.orEmpty()) }
    var dateText by rememberSaveable(initialEvent?.id, initialDate) { mutableStateOf(defaultDate.format(editorDateFormatter)) }
    var timeText by rememberSaveable(initialEvent?.id, initialDate) { mutableStateOf(defaultTime.format(editorTimeFormatter)) }
    var timeTypeName by rememberSaveable(initialEvent?.id, initialDate) { mutableStateOf(initialEvent?.timeType?.name ?: EventTimeType.ALL_DAY.name) }
    var isPinned by rememberSaveable(initialEvent?.id, initialDate) { mutableStateOf(initialEvent?.isPinned ?: false) }
    var isRepeat by rememberSaveable(initialEvent?.id, initialDate) { mutableStateOf(initialEvent?.isRepeat ?: false) }
    var advancedExpanded by rememberSaveable { mutableStateOf(false) }
    var repeatTypeName by rememberSaveable(initialEvent?.id, initialDate) { mutableStateOf(initialEvent?.repeatRule?.type?.name.orEmpty()) }
    var repeatIntervalText by rememberSaveable(initialEvent?.id, initialDate) { mutableStateOf(initialEvent?.repeatRule?.interval?.toString() ?: "1") }
    var customUnitName by rememberSaveable(initialEvent?.id, initialDate) { mutableStateOf(initialEvent?.repeatRule?.customUnit?.name ?: RepeatCustomUnit.DAY.name) }
    var reminderMinutes by rememberSaveable(initialEvent?.id, initialDate) { mutableStateOf(initialEvent?.reminder?.offsetMinutes) }
    var coverPresetName by rememberSaveable(initialEvent?.id, initialDate) { mutableStateOf(initialEvent?.coverPreset?.name ?: EventCoverPreset.DEFAULT.name) }
    var note by rememberSaveable(initialEvent?.id, initialDate) { mutableStateOf(initialEvent?.note.orEmpty()) }
    var notificationNames by rememberSaveable(initialEvent?.id, initialDate) { mutableStateOf(initialEvent?.notificationMethods?.joinToString(",") { it.name } ?: NotificationMethod.IN_APP.name) }
    var errorText by rememberSaveable { mutableStateOf<String?>(null) }
    var showGroupSheet by rememberSaveable { mutableStateOf(false) }
    var showReminderSheet by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirmation by rememberSaveable(initialEvent?.id, initialDate) { mutableStateOf(false) }
    var showTemplateSheet by rememberSaveable { mutableStateOf(false) }
    var templateBrowserEntered by remember { mutableStateOf(false) }
    LaunchedEffect(showTemplateSheet) {
        // Start from pre-enter when the full-screen browser is composed, rather
        // than relying on an initial `visible = true` composition to animate.
        templateBrowserEntered = showTemplateSheet
    }
    var templateName by rememberSaveable(initialEvent?.id, initialDate) {
        mutableStateOf((initialEvent?.cardTemplateKey ?: EventCardTemplateKey.CLASSIC).name)
    }
    var templateSizeName by rememberSaveable(initialEvent?.id, initialDate) {
        mutableStateOf((initialEvent?.travelCardConfig?.size ?: TravelCardSize.WIDE).name)
    }

    val timeType = EventTimeType.valueOf(timeTypeName)
    val selectedDate = runCatching { LocalDate.parse(dateText, editorDateFormatter) }.getOrNull()
    val selectedTime = runCatching { LocalTime.parse(timeText, editorTimeFormatter) }.getOrNull()
    val notificationMethods = notificationNames.split(",")
        .filter { it.isNotBlank() }
        .mapNotNull { runCatching { NotificationMethod.valueOf(it) }.getOrNull() }
        .toSet()
        .ifEmpty { setOf(NotificationMethod.IN_APP) }
    val selectedTemplate = runCatching { EventCardTemplateKey.valueOf(templateName) }
        .getOrDefault(EventCardTemplateKey.CLASSIC)
    val selectedTemplateSize = runCatching { TravelCardSize.valueOf(templateSizeName) }
        .getOrDefault(TravelCardSize.WIDE)
    val previewEvent = buildEventOrNull(
        title = title.ifBlank { "我的事件" },
        subtitle = subtitle,
        groupName = groupName,
        dateText = dateText,
        timeText = timeText,
        timeType = timeType,
        isPinned = isPinned,
        isRepeat = isRepeat,
        repeatTypeName = repeatTypeName,
        repeatIntervalText = repeatIntervalText,
        customUnitName = customUnitName,
        reminderMinutes = reminderMinutes,
        coverPresetName = coverPresetName,
        note = note,
        notificationMethods = notificationMethods,
        zoneId = zoneId,
        baseEvent = initialEvent,
        selectedTemplateName = selectedTemplate.name,
        templateSizeName = selectedTemplateSize.name,
    )?.copy(
        id = "template-preview",
        dateLabel = selectedDate?.let(EventTimeCalculator::dateLabel).orEmpty(),
    )

    LaunchedEffect(showDatePicker) {
        if (showDatePicker) {
            val date = selectedDate ?: defaultDate
            DatePickerDialog(
                context,
                { _, year, month, day ->
                    dateText = LocalDate.of(year, month + 1, day).format(editorDateFormatter)
                    showDatePicker = false
                    errorText = null
                },
                date.year,
                date.monthValue - 1,
                date.dayOfMonth,
            ).apply {
                setOnCancelListener { showDatePicker = false }
                show()
            }
        }
    }
    LaunchedEffect(showTimePicker) {
        if (showTimePicker) {
            val time = selectedTime ?: defaultTime
            TimePickerDialog(
                context,
                { _, hour, minute ->
                    timeText = LocalTime.of(hour, minute).format(editorTimeFormatter)
                    showTimePicker = false
                    errorText = null
                },
                time.hour,
                time.minute,
                true,
            ).apply {
                setOnCancelListener { showTimePicker = false }
                show()
            }
        }
    }

    Scaffold(
        containerColor = FormBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(FormBackground),
        ) {
            if (fixedBackgroundBitmap != null) {
                Image(
                    bitmap = fixedBackgroundBitmap,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Image(
                    painter = painterResource(scene.imageRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    contentScale = ContentScale.Crop,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                scene.imageTint.copy(alpha = 0.30f),
                                scene.bottomShade.copy(alpha = 0.12f),
                                FormBackground.copy(alpha = 0.64f),
                                FormBackground,
                            ),
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Spacer(Modifier.width(52.dp))
                    Spacer(Modifier.weight(1f))
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(22.dp))
                            .background(editorHeaderBackdrop)
                            .padding(start = 18.dp, top = 10.dp, end = 14.dp, bottom = 12.dp),
                        horizontalAlignment = Alignment.End,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                text = if (initialEvent == null) "新增日期" else "修改日期",
                                fontFamily = NotoSerifSc,
                                fontSize = 38.sp,
                                lineHeight = 44.sp,
                                fontWeight = FontWeight.Bold,
                                color = editorTextColor,
                                maxLines = 1,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "✦ 〰",
                                modifier = Modifier.padding(end = 4.dp),
                                color = editorTextColor.copy(alpha = 0.78f),
                                fontFamily = NotoSerifSc,
                                fontSize = 20.sp,
                                maxLines = 1,
                            )
                        }
                        Text(
                            text = "记录每一个重要时刻",
                            fontFamily = NotoSerifSc,
                            fontSize = 16.sp,
                            color = editorTextColor.copy(alpha = 0.84f),
                        )
                        Text(
                            text = scene.quote.replace("\n", " · "),
                            modifier = Modifier.padding(top = 8.dp, end = 4.dp),
                            fontFamily = NotoSerifSc,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = editorTextColor.copy(alpha = 0.72f),
                            textAlign = TextAlign.End,
                            maxLines = 2,
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))

                SectionCard {
                    CountedInputRow(
                        icon = Icons.Outlined.TextFields,
                        label = "大标题",
                        hint = "输入事件大标题",
                        value = title,
                        maxLength = 20,
                        onValueChange = { title = it; errorText = null },
                    )
                    SectionDivider()
                    CountedInputRow(
                        icon = Icons.Outlined.TextFields,
                        label = "小标题",
                        hint = "输入事件小标题（可选）",
                        value = subtitle,
                        maxLength = 30,
                        onValueChange = { subtitle = it; errorText = null },
                        lowerCaseIcon = true,
                    )
                    SectionDivider()
                    FormRow(
                        icon = Icons.Outlined.FolderOpen,
                        title = "所属分组",
                        value = groupName.ifBlank { "未分组" },
                        onClick = { showGroupSheet = true },
                        trailing = Icons.Outlined.ExpandMore,
                    )
                }

                Spacer(Modifier.height(16.dp))
                SectionCard {
                    FormRow(
                        icon = Icons.Outlined.AccessTime,
                        title = "开始时间 / 到达时间",
                        onClick = { showDatePicker = true },
                        trailing = Icons.Outlined.ChevronRight,
                    )
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        val stackDateTimeCells = maxWidth < 330.dp
                        if (stackDateTimeCells) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                DateTimeSelectionCell(
                                    icon = Icons.Outlined.CalendarMonth,
                                    label = selectedDate?.format(displayDateFormatter) ?: "选择日期",
                                    onClick = { showDatePicker = true },
                                )
                                DateTimeSelectionCell(
                                    icon = Icons.Outlined.AccessTime,
                                    label = if (timeType == EventTimeType.ALL_DAY) "全天" else timeText,
                                    onClick = if (timeType == EventTimeType.TIMED) ({ showTimePicker = true }) else null,
                                )
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                DateTimeSelectionCell(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Outlined.CalendarMonth,
                                    label = selectedDate?.format(displayDateFormatter) ?: "选择日期",
                                    onClick = { showDatePicker = true },
                                )
                                DateTimeSelectionCell(
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Outlined.AccessTime,
                                    label = if (timeType == EventTimeType.ALL_DAY) "全天" else timeText,
                                    onClick = if (timeType == EventTimeType.TIMED) ({ showTimePicker = true }) else null,
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = timeType == EventTimeType.TIMED,
                            onClick = { timeTypeName = EventTimeType.TIMED.name },
                            colors = warmFilterChipColors(),
                            label = { Text("精确时间") },
                        )
                        FilterChip(
                            selected = timeType == EventTimeType.ALL_DAY,
                            onClick = { timeTypeName = EventTimeType.ALL_DAY.name },
                            colors = warmFilterChipColors(),
                            label = { Text("全天") },
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                SectionCard {
                    ToggleRow(
                        icon = Icons.Outlined.PushPin,
                        title = "是否置顶",
                        checked = isPinned,
                        onCheckedChange = { isPinned = it },
                    )
                    SectionDivider()
                    ToggleRow(
                        icon = Icons.Outlined.Repeat,
                        title = "是否重复",
                        checked = isRepeat,
                        onCheckedChange = {
                            isRepeat = it
                            if (it) advancedExpanded = true
                        },
                    )
                }

                Spacer(Modifier.height(12.dp))
                SectionCard(
                    modifier = Modifier.clickable { showTemplateSheet = true },
                    color = Color(0xFFFFF4E9),
                ) {
                    FormRow(
                        icon = Icons.Outlined.Settings,
                        title = "卡片模板",
                        subtitle = "选择当前事件的 Small / Wide 展示样式",
                        value = eventTemplateLabel(selectedTemplate),
                        trailing = Icons.Outlined.ChevronRight,
                    )
                }

                Spacer(Modifier.height(16.dp))
                SectionCard(
                    modifier = Modifier.clickable { advancedExpanded = !advancedExpanded },
                    color = Color(0xFFFFF4E9),
                ) {
                    FormRow(
                        icon = Icons.Outlined.Settings,
                        title = "进阶设置",
                        subtitle = "提醒、重复规则、封面、备注、通知方式",
                        trailing = if (advancedExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    )
                    AnimatedVisibility(visible = advancedExpanded) {
                        Column(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            AdvancedLabel("提醒")
                            FormRow(
                                icon = Icons.Outlined.NotificationsNone,
                                title = "提醒方式",
                                value = ReminderOptions.firstOrNull { it.minutes == reminderMinutes }?.label ?: "不提醒",
                                onClick = { showReminderSheet = true },
                                trailing = Icons.Outlined.ChevronRight,
                                compact = true,
                            )
                            if (isRepeat) {
                                AdvancedLabel("重复规则")
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    RepeatType.entries.forEach { type ->
                                        FilterChip(
                                            selected = repeatTypeName == type.name,
                                            onClick = { repeatTypeName = type.name; errorText = null },
                                            colors = warmFilterChipColors(),
                                            label = { Text(repeatTypeLabel(type)) },
                                        )
                                    }
                                }
                                if (repeatTypeName == RepeatType.CUSTOM.name) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        SmallNumberInput(
                                            value = repeatIntervalText,
                                            onValueChange = { repeatIntervalText = it.filter(Char::isDigit).take(2) },
                                        )
                                        Text("个", color = WarmMuted)
                                        FilterChip(
                                            selected = customUnitName == RepeatCustomUnit.DAY.name,
                                            onClick = { customUnitName = RepeatCustomUnit.DAY.name },
                                            colors = warmFilterChipColors(),
                                            label = { Text("天") },
                                        )
                                        FilterChip(
                                            selected = customUnitName == RepeatCustomUnit.WEEK.name,
                                            onClick = { customUnitName = RepeatCustomUnit.WEEK.name },
                                            colors = warmFilterChipColors(),
                                            label = { Text("周") },
                                        )
                                    }
                                }
                            }
                            AdvancedLabel("封面")
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                EventCoverPreset.entries.forEach { preset ->
                                    FilterChip(
                                        selected = coverPresetName == preset.name,
                                        onClick = { coverPresetName = preset.name },
                                        colors = warmFilterChipColors(),
                                        label = { Text(coverPresetLabel(preset)) },
                                    )
                                }
                            }
                            AdvancedLabel("备注")
                            BasicTextField(
                                value = note,
                                onValueChange = { note = it.take(300) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(88.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(alpha = 0.76f))
                                    .padding(14.dp),
                                textStyle = TextStyle(color = WarmBrown, fontSize = 15.sp),
                                maxLines = 4,
                                decorationBox = { inner ->
                                    Box {
                                        if (note.isBlank()) Text("记录这件事的备注（可选）", color = WarmMuted)
                                        inner()
                                    }
                                },
                            )
                            AdvancedLabel("通知方式")
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                NotificationMethod.entries.forEach { method ->
                                    val selected = method.name in notificationNames.split(",")
                                    FilterChip(
                                        selected = selected,
                                        onClick = {
                                            val names = notificationNames.split(",").filter { it.isNotBlank() }.toMutableSet()
                                            if (selected) names.remove(method.name) else names.add(method.name)
                                            notificationNames = names.joinToString(",")
                                        },
                                        colors = warmFilterChipColors(),
                                        label = { Text(notificationMethodLabel(method)) },
                                    )
                                }
                            }
                        }
                    }
                }

                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                    )
                }
                Spacer(Modifier.height(14.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val event = buildEventOrNull(
                                title = title,
                                subtitle = subtitle,
                                groupName = groupName,
                                dateText = dateText,
                                timeText = timeText,
                                timeType = timeType,
                                isPinned = isPinned,
                                isRepeat = isRepeat,
                                repeatTypeName = repeatTypeName,
                                repeatIntervalText = repeatIntervalText,
                                customUnitName = customUnitName,
                                reminderMinutes = reminderMinutes,
                                coverPresetName = coverPresetName,
                                note = note,
                                notificationMethods = notificationMethods,
                                zoneId = zoneId,
                                baseEvent = initialEvent,
                                selectedTemplateName = selectedTemplate.name,
                                templateSizeName = selectedTemplateSize.name,
                            )
                            if (event == null) {
                                errorText = validationError(
                                    title = title,
                                    dateText = dateText,
                                    timeText = timeText,
                                    timeType = timeType,
                                    isRepeat = isRepeat,
                                    repeatTypeName = repeatTypeName,
                                )
                            } else {
                                errorText = null
                                onSave(event)
                            }
                        }
                        .semantics(mergeDescendants = true) {
                            contentDescription = "保存事件"
                            role = Role.Button
                        },
                    shape = RoundedCornerShape(20.dp),
                    color = WarmAccent,
                    shadowElevation = 6.dp,
                ) {
                    Text(
                        "保存",
                        modifier = Modifier.fillMaxWidth().padding(vertical = 17.dp),
                        color = Color.White,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
                if (initialEvent != null && onDelete != null) {
                    TextButton(
                        onClick = { showDeleteConfirmation = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("删除此事件", color = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(Modifier.height(22.dp))
            }
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .zIndex(1f)
                    .statusBarsPadding()
                    .padding(start = 22.dp, top = 8.dp)
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.76f))
                    .semantics {
                        contentDescription = "返回"
                        role = Role.Button
                    },
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = WarmBrown)
            }
        }
    }

    if (showDeleteConfirmation && initialEvent != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("删除事件？") },
            text = { Text("删除后会从主页移除，但可以在短时间内撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                    onDelete()
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) { Text("取消") }
            },
        )
    }

    if (showTemplateSheet) {
        AnimatedVisibility(
            visible = templateBrowserEntered,
            enter = fadeIn(animationSpec = tween(220)) + slideInVertically(
                animationSpec = tween(420, easing = androidx.compose.animation.core.CubicBezierEasing(.22f, 1f, .36f, 1f)),
                initialOffsetY = { it / 12 },
            ),
            label = "template-browser-enter",
        ) {
            TemplateBrowserFeature(
                previewEvent = previewEvent,
                selectedTemplate = selectedTemplate,
                selectedSize = selectedTemplateSize,
                onBack = { showTemplateSheet = false },
                onApply = { template, size ->
                    templateName = template.templateKey.name
                    templateSizeName = size.name
                    showTemplateSheet = false
                },
            )
        }
        return
    }

    if (showGroupSheet) {
        ModalBottomSheet(
            onDismissRequest = { showGroupSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = FormBackground,
        ) {
            Text("选择所属分组", modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp), style = MaterialTheme.typography.titleLarge)
            GroupOptions.forEach { group ->
                ListItem(
                    modifier = Modifier.clickable {
                        groupName = group.takeUnless { it == "未分组" }.orEmpty()
                        showGroupSheet = false
                    },
                    headlineContent = { Text(group) },
                    trailingContent = if (groupName == group || (group == "未分组" && groupName.isBlank())) {
                        { Text("✓", color = WarmAccentDark, fontSize = 20.sp) }
                    } else null,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    if (showReminderSheet) {
        ModalBottomSheet(
            onDismissRequest = { showReminderSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = FormBackground,
        ) {
            Text("选择提醒方式", modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp), style = MaterialTheme.typography.titleLarge)
            ReminderOptions.forEach { option ->
                ListItem(
                    modifier = Modifier.clickable {
                        reminderMinutes = option.minutes
                        showReminderSheet = false
                    },
                    headlineContent = { Text(option.label) },
                    trailingContent = if (reminderMinutes == option.minutes) {
                        { Text("✓", color = WarmAccentDark, fontSize = 20.sp) }
                    } else null,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionCard(
    modifier: Modifier = Modifier,
    color: Color = WarmCard,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = color,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.82f)),
        shadowElevation = 3.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun CountedInputRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    hint: String,
    value: String,
    maxLength: Int,
    onValueChange: (String) -> Unit,
    lowerCaseIcon: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(38.dp),
            tint = WarmAccentDark.copy(alpha = 0.74f),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = WarmBrown, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            BasicTextField(
                value = value,
                onValueChange = { onValueChange(it.take(maxLength)) },
                modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
                singleLine = true,
                textStyle = TextStyle(color = WarmBrown, fontSize = 16.sp),
                cursorBrush = SolidColor(WarmAccentDark),
                decorationBox = { inner ->
                    Box {
                        if (value.isBlank()) Text(hint, color = WarmMuted, fontSize = 16.sp)
                        inner()
                    }
                },
            )
        }
        Spacer(Modifier.width(8.dp))
        Text("${value.length}/$maxLength", color = WarmMuted, fontSize = 12.sp)
    }
}

@Composable
private fun FormRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: androidx.compose.ui.graphics.vector.ImageVector? = null,
    compact: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = if (compact) 4.dp else 18.dp, vertical = if (compact) 7.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(if (compact) 28.dp else 38.dp), tint = WarmAccentDark.copy(alpha = 0.74f))
        Spacer(Modifier.width(if (compact) 10.dp else 14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = WarmBrown, fontSize = if (compact) 15.sp else 17.sp, fontWeight = FontWeight.Bold)
            subtitle?.let { Text(it, color = WarmMuted, fontSize = if (compact) 12.sp else 14.sp, modifier = Modifier.padding(top = 3.dp)) }
        }
        value?.let { Text(it, color = WarmMuted, fontSize = if (compact) 14.sp else 16.sp) }
        trailing?.let { Icon(it, contentDescription = null, tint = WarmMuted, modifier = Modifier.padding(start = 10.dp)) }
    }
}

@Composable
private fun ToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(38.dp), tint = WarmAccentDark.copy(alpha = 0.74f))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = WarmBrown, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            subtitle?.let {
                Text(it, color = WarmMuted, fontSize = 14.sp, modifier = Modifier.padding(top = 3.dp))
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = WarmAccent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFE8D7C4),
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

@Composable
private fun DateTimeSelectionCell(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.72f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = WarmBrown, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = WarmBrown,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (onClick != null) {
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = WarmMuted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(color = WarmLine, modifier = Modifier.padding(horizontal = 18.dp))
}

@Composable
private fun AdvancedLabel(text: String) {
    Text(text, color = WarmBrown, fontSize = 14.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun warmFilterChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = Color.White.copy(alpha = 0.72f),
    labelColor = WarmBrown,
    selectedContainerColor = Color(0xFFF0D9C2),
    selectedLabelColor = WarmBrown,
)

@Composable
private fun SmallNumberInput(value: String, onValueChange: (String) -> Unit) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .width(54.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.78f))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        textStyle = TextStyle(color = WarmBrown, fontSize = 15.sp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
    )
}

private fun buildEventOrNull(
    title: String,
    subtitle: String,
    groupName: String,
    dateText: String,
    timeText: String,
    timeType: EventTimeType,
    isPinned: Boolean,
    isRepeat: Boolean,
    repeatTypeName: String,
    repeatIntervalText: String,
    customUnitName: String,
    reminderMinutes: Int?,
    coverPresetName: String,
    note: String,
    notificationMethods: Set<NotificationMethod>,
    zoneId: ZoneId,
    baseEvent: TimeEvent?,
    selectedTemplateName: String,
    templateSizeName: String,
): TimeEvent? = runCatching {
    if (title.isBlank()) return null
    val date = LocalDate.parse(dateText, editorDateFormatter)
    val time = LocalTime.parse(timeText, editorTimeFormatter)
    val repeatRule = if (isRepeat) parseRepeatRule(repeatTypeName, repeatIntervalText, customUnitName) else null
    if (isRepeat && repeatRule == null) return null
    val selectedTemplate = runCatching { EventCardTemplateKey.valueOf(selectedTemplateName) }
        .getOrDefault(EventCardTemplateKey.CLASSIC)
    val selectedSize = runCatching { TravelCardSize.valueOf(templateSizeName) }
        .getOrDefault(TravelCardSize.WIDE)
    // Keep the chosen size for every template, including Classic. Classic
    // keeps its own visual renderer, while the shared config still lets the
    // home grid honor Small/Wide placement consistently.
    val updatedTravelConfig = (baseEvent?.travelCardConfig ?: TravelCardConfig()).copy(
        title = title.trim(),
        badgeLabel = subtitle.trim().ifBlank {
            if (selectedTemplate == EventCardTemplateKey.CLASSIC) "" else "纪念日"
        },
        groupLabel = groupName.trim(),
        targetDate = date,
        size = selectedSize,
    )
    baseEvent?.copy(
        title = title.trim(),
        subtitle = subtitle.trim(),
        groupLabel = groupName.trim(),
        timeType = timeType,
        dateLabel = "",
        relativeLabel = "",
        note = note.trim(),
        isPinned = isPinned,
        localDate = date.takeIf { timeType == EventTimeType.ALL_DAY },
        targetInstant = LocalDateTime.of(date, time).atZone(zoneId).toInstant()
            .takeIf { timeType == EventTimeType.TIMED },
        zoneId = zoneId.id.takeIf { timeType == EventTimeType.TIMED },
        isRepeat = isRepeat,
        repeatRule = repeatRule,
        reminder = reminderMinutes?.let(::ReminderConfig),
        coverPreset = runCatching { EventCoverPreset.valueOf(coverPresetName) }
            .getOrDefault(EventCoverPreset.DEFAULT),
        notificationMethods = notificationMethods,
        cardTemplateKey = selectedTemplate,
        travelCardConfig = updatedTravelConfig,
    ) ?: TimeEvent(
        id = "event-${System.currentTimeMillis()}",
        title = title.trim(),
        subtitle = subtitle.trim(),
        groupLabel = groupName.trim(),
        timeType = timeType,
        dateLabel = "",
        relativeLabel = "",
        note = note.trim(),
        icon = "●",
        colorRole = EventColorRole.FUTURE,
        cardPaletteKey = EventCardPaletteKey.BLUE_WHITE,
        isPinned = isPinned,
        cardTemplateKey = selectedTemplate,
        localDate = date.takeIf { timeType == EventTimeType.ALL_DAY },
        targetInstant = LocalDateTime.of(date, time).atZone(zoneId).toInstant()
            .takeIf { timeType == EventTimeType.TIMED },
        zoneId = zoneId.id.takeIf { timeType == EventTimeType.TIMED },
        isRepeat = isRepeat,
        repeatRule = repeatRule,
        reminder = reminderMinutes?.let(::ReminderConfig),
        coverPreset = runCatching { EventCoverPreset.valueOf(coverPresetName) }
            .getOrDefault(EventCoverPreset.DEFAULT),
        notificationMethods = notificationMethods,
        travelCardConfig = updatedTravelConfig,
    )
}.getOrNull()

private fun parseRepeatRule(typeName: String, intervalText: String, customUnitName: String): RepeatRule? = runCatching {
    val type = RepeatType.valueOf(typeName)
    RepeatRule(
        type = type,
        interval = intervalText.toInt().coerceAtLeast(1),
        customUnit = RepeatCustomUnit.valueOf(customUnitName),
    )
}.getOrNull()

private fun validationError(
    title: String,
    dateText: String,
    timeText: String,
    timeType: EventTimeType,
    isRepeat: Boolean,
    repeatTypeName: String,
): String = when {
    title.isBlank() -> "请输入事件大标题"
    runCatching { LocalDate.parse(dateText, editorDateFormatter) }.isFailure -> "请选择正确的日期"
    timeType == EventTimeType.TIMED && runCatching { LocalTime.parse(timeText, editorTimeFormatter) }.isFailure -> "请选择正确的时间"
    isRepeat && repeatTypeName.isBlank() -> "请设置重复规则"
    else -> "请检查输入内容"
}

private fun repeatTypeLabel(type: RepeatType): String = when (type) {
    RepeatType.YEARLY -> "每年"
    RepeatType.MONTHLY -> "每月"
    RepeatType.WEEKLY -> "每周"
    RepeatType.CUSTOM -> "自定义"
}

private fun coverPresetLabel(preset: EventCoverPreset): String = when (preset) {
    EventCoverPreset.DEFAULT -> "默认"
    EventCoverPreset.CREAM -> "奶油"
    EventCoverPreset.SUNSET -> "黄昏"
}

private fun notificationMethodLabel(method: NotificationMethod): String = when (method) {
    NotificationMethod.IN_APP -> "App 内提醒"
    NotificationMethod.SYSTEM -> "系统通知"
}
