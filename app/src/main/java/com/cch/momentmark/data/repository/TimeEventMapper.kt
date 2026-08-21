package com.cch.momentmark.data.repository

import com.cch.momentmark.data.local.TimeEventEntity
import com.cch.momentmark.domain.model.EventCardPaletteKey
import com.cch.momentmark.domain.model.EventCardTemplateKey
import com.cch.momentmark.domain.model.EventCalendarType
import com.cch.momentmark.domain.model.EventColorRole
import com.cch.momentmark.domain.model.EventCoverPreset
import com.cch.momentmark.domain.model.EventTimeType
import com.cch.momentmark.domain.model.NotificationMethod
import com.cch.momentmark.domain.model.ReminderConfig
import com.cch.momentmark.domain.model.RepeatCustomUnit
import com.cch.momentmark.domain.model.RepeatRule
import com.cch.momentmark.domain.model.RepeatType
import com.cch.momentmark.domain.model.TimeEvent
import com.cch.momentmark.domain.model.TravelBackgroundPreset
import com.cch.momentmark.domain.model.TravelCardConfig
import com.cch.momentmark.domain.model.TravelCardIcon
import com.cch.momentmark.domain.model.TravelCardSize
import com.cch.momentmark.domain.time.EventTimeCalculator
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.json.JSONObject

/** Converts persisted facts to the existing domain model without storing derived labels. */
object TimeEventMapper {
    internal data class PersistenceMetadata(
        val sortOrder: Long,
        val deletedAt: Long?,
        val createdAt: Long,
        val updatedAt: Long,
    )

    internal fun persistenceMetadata(
        previous: TimeEventEntity?,
        nowMillis: Long,
    ): PersistenceMetadata = PersistenceMetadata(
        sortOrder = previous?.sortOrder ?: 0,
        deletedAt = previous?.deletedAt,
        createdAt = previous?.createdAt ?: nowMillis,
        updatedAt = nowMillis,
    )

    /**
     * Builds the persistence shape from editable event facts.  Existing
     * ordering and lifecycle metadata must survive a content edit: callers
     * use explicit archive/delete actions for those transitions.
     */
    fun TimeEvent.toEntity(
        nowMillis: Long = System.currentTimeMillis(),
        previous: TimeEventEntity? = null,
    ): TimeEventEntity {
        val config = travelCardConfig
        val metadata = persistenceMetadata(previous, nowMillis)
        return TimeEventEntity(
            id = id,
            title = title,
            subtitle = subtitle.ifBlank { config?.badgeLabel.orEmpty() },
            timeType = timeType.name,
            localDateIso = localDate?.toString(),
            instantEpochMillis = targetInstant?.toEpochMilli(),
            zoneId = zoneId,
            note = note,
            iconKey = icon,
            paletteKey = cardPaletteKey.name,
            templateKey = cardTemplateKey.name,
            templateConfigJson = config?.toJson()?.toString().orEmpty(),
            advancedConfigJson = advancedConfigToJson().toString(),
            groupId = groupLabel.ifBlank { config?.groupLabel.orEmpty() }
                .takeIf { it.isNotBlank() },
            isPinned = isPinned,
            isArchived = isArchived,
            sortOrder = metadata.sortOrder,
            deletedAt = metadata.deletedAt,
            createdAt = metadata.createdAt,
            updatedAt = metadata.updatedAt,
        )
    }

    fun TimeEventEntity.toDomain(clock: Clock = Clock.systemDefaultZone()): TimeEvent {
        val type = runCatching { EventTimeType.valueOf(timeType) }.getOrDefault(EventTimeType.ALL_DAY)
        val parsedDate = localDateIso?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val parsedInstant = instantEpochMillis?.let(Instant::ofEpochMilli)
        val displayZone = zoneId?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: clock.zone
        val countdown = when (type) {
            EventTimeType.ALL_DAY -> parsedDate?.let {
                EventTimeCalculator.calculate(
                    timeType = type,
                    localDate = it,
                    repeatRule = advancedConfigFromJson(advancedConfigJson)?.repeatRule,
                    clock = clock,
                    zoneId = displayZone,
                )
            }
            EventTimeType.TIMED -> parsedInstant?.let {
                EventTimeCalculator.calculate(
                    timeType = type,
                    targetInstant = it,
                    repeatRule = advancedConfigFromJson(advancedConfigJson)?.repeatRule,
                    clock = clock,
                    zoneId = displayZone,
                )
            }
        }
        val advancedConfig = advancedConfigFromJson(advancedConfigJson)
        val travelConfig = templateConfigJson.takeIf { it.isNotBlank() }?.let(::travelConfigFromJson)
        val displayTitle = travelConfig?.title?.takeIf { it.isNotBlank() } ?: title
        val dateLabel = when (type) {
            EventTimeType.ALL_DAY -> parsedDate?.let {
                EventTimeCalculator.dateLabel(
                    EventTimeCalculator.resolveNextDate(
                        targetDate = it,
                        repeatRule = advancedConfig?.repeatRule,
                        clock = clock,
                        zoneId = displayZone,
                    ),
                )
            }.orEmpty()
            EventTimeType.TIMED -> parsedInstant?.let {
                EventTimeCalculator.formatInstant(
                    EventTimeCalculator.resolveNextInstant(
                        targetInstant = it,
                        repeatRule = advancedConfig?.repeatRule,
                        clock = clock,
                        zoneId = displayZone,
                    ),
                    displayZone,
                )
            }.orEmpty()
        }
        val relativeLabel = countdown?.let { "${it.statusLabel} ${it.amount} 天" }.orEmpty()
        return TimeEvent(
            id = id,
            title = displayTitle,
            subtitle = subtitle.ifBlank { travelConfig?.badgeLabel.orEmpty() },
            // groupId is the authoritative assignment. Keeping the fallback
            // here would resurrect a deleted group from an old template JSON.
            groupLabel = groupId.orEmpty(),
            timeType = type,
            dateLabel = dateLabel,
            relativeLabel = relativeLabel,
            note = note,
            icon = iconKey,
            colorRole = countdown?.status?.let { status ->
                if (status == com.cch.momentmark.domain.time.EventTimeStatus.PAST) {
                    EventColorRole.PAST
                } else {
                    EventColorRole.FUTURE
                }
            } ?: EventColorRole.FUTURE,
            cardPaletteKey = runCatching { EventCardPaletteKey.valueOf(paletteKey) }
                .getOrDefault(EventCardPaletteKey.BLUE_WHITE),
            isPinned = isPinned,
            isArchived = isArchived,
            cardTemplateKey = runCatching { EventCardTemplateKey.valueOf(templateKey) }
                .getOrDefault(EventCardTemplateKey.CLASSIC),
            travelCardConfig = travelConfig,
            localDate = parsedDate,
            targetInstant = parsedInstant,
            zoneId = zoneId,
            calendarType = advancedConfig?.calendarType ?: EventCalendarType.SOLAR,
            isRepeat = advancedConfig?.isRepeat ?: false,
            repeatRule = advancedConfig?.repeatRule,
            reminder = advancedConfig?.reminder,
            coverPreset = advancedConfig?.coverPreset ?: EventCoverPreset.DEFAULT,
            notificationMethods = advancedConfig?.notificationMethods
                ?: setOf(NotificationMethod.IN_APP),
        )
    }

    private data class AdvancedConfig(
        val calendarType: EventCalendarType,
        val isRepeat: Boolean,
        val repeatRule: RepeatRule?,
        val reminder: ReminderConfig?,
        val coverPreset: EventCoverPreset,
        val notificationMethods: Set<NotificationMethod>,
    )

    private fun TimeEvent.advancedConfigToJson(): JSONObject = JSONObject().apply {
        put("calendarType", calendarType.name)
        put("isRepeat", isRepeat)
        repeatRule?.let { rule ->
            put("repeatType", rule.type.name)
            put("repeatInterval", rule.interval)
            put("repeatCustomUnit", rule.customUnit.name)
            rule.weekday?.let { put("repeatWeekday", it.value) }
        }
        reminder?.let { put("reminderMinutes", it.offsetMinutes) }
        put("coverPreset", coverPreset.name)
        put("notificationMethods", org.json.JSONArray(notificationMethods.map { it.name }))
    }

    private fun advancedConfigFromJson(value: String): AdvancedConfig? = runCatching {
        if (value.isBlank()) return null
        val json = JSONObject(value)
        val repeatRule = if (json.optBoolean("isRepeat", false) && json.has("repeatType")) {
            RepeatRule(
                type = RepeatType.valueOf(json.getString("repeatType")),
                interval = json.optInt("repeatInterval", 1).coerceAtLeast(1),
                customUnit = runCatching {
                    RepeatCustomUnit.valueOf(json.optString("repeatCustomUnit"))
                }.getOrDefault(RepeatCustomUnit.DAY),
                weekday = json.optInt("repeatWeekday", 0).takeIf { it in 1..7 }
                    ?.let(java.time.DayOfWeek::of),
            )
        } else {
            null
        }
        val methods = buildSet {
            val array = json.optJSONArray("notificationMethods")
            if (array != null) {
                repeat(array.length()) {
                    runCatching { add(NotificationMethod.valueOf(array.getString(it))) }
                }
            }
        }.ifEmpty { setOf(NotificationMethod.IN_APP) }
        AdvancedConfig(
            calendarType = runCatching {
                EventCalendarType.valueOf(json.optString("calendarType"))
            }.getOrDefault(EventCalendarType.SOLAR),
            isRepeat = json.optBoolean("isRepeat", false),
            repeatRule = repeatRule,
            reminder = json.optInt("reminderMinutes", -1).takeIf { it >= 0 }
                ?.let(::ReminderConfig),
            coverPreset = runCatching {
                EventCoverPreset.valueOf(json.optString("coverPreset"))
            }.getOrDefault(EventCoverPreset.DEFAULT),
            notificationMethods = methods,
        )
    }.getOrNull()

    private fun TravelCardConfig.toJson(): JSONObject = JSONObject().apply {
        put("badgeLabel", badgeLabel)
        put("title", title)
        put("groupLabel", groupLabel)
        put("countdownUnit", countdownUnit)
        put("targetDate", targetDate.toString())
        put("locationLabel", locationLabel)
        put("backgroundPreset", backgroundPreset.name)
        put("badgeIcon", badgeIcon.name)
        put("dateIcon", dateIcon.name)
        put("size", size.name)
    }

    private fun travelConfigFromJson(value: String): TravelCardConfig? = runCatching {
        val json = JSONObject(value)
        TravelCardConfig(
            badgeLabel = json.optString("badgeLabel", "纪念日"),
            title = json.optString("title", "东京旅行"),
            groupLabel = json.optString("groupLabel", "旅行与生活"),
            countdownUnit = json.optString("countdownUnit", "天"),
            targetDate = LocalDate.parse(json.optString("targetDate", "2026-09-16")),
            locationLabel = json.optString("locationLabel", "东京"),
            backgroundPreset = runCatching {
                TravelBackgroundPreset.valueOf(json.optString("backgroundPreset"))
            }.getOrDefault(TravelBackgroundPreset.SCRAPBOOK_CREAM),
            badgeIcon = runCatching {
                TravelCardIcon.valueOf(json.optString("badgeIcon"))
            }.getOrDefault(TravelCardIcon.CALENDAR),
            dateIcon = runCatching {
                TravelCardIcon.valueOf(json.optString("dateIcon"))
            }.getOrDefault(TravelCardIcon.CLOCK),
            size = runCatching {
                TravelCardSize.valueOf(json.optString("size"))
            }.getOrDefault(TravelCardSize.WIDE),
        )
    }.getOrNull()
}
