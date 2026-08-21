package com.cch.momentmark.data.templates

import com.cch.momentmark.domain.model.EventCardTemplateKey
import com.cch.momentmark.domain.model.TravelCardSize

data class TemplateDefinition(
    val id: String,
    val categoryId: String,
    val categoryName: String,
    val name: String,
    val description: String,
    val keywords: Set<String>,
    val templateKey: EventCardTemplateKey,
    val supportedSizes: Set<TravelCardSize> = setOf(TravelCardSize.SMALL, TravelCardSize.WIDE),
    val elements: List<String> = listOf("title", "countdown", "date"),
)

data class TemplateCategory(
    val id: String,
    val name: String,
    val description: String,
    val templates: List<TemplateDefinition>,
)

/** Configuration-first template catalog; cards still reuse the existing renderers. */
object TemplateCatalog {
    val categories: List<TemplateCategory> = listOf(
        TemplateCategory(
            id = "travel_editorial",
            name = "奶油极简旅行",
            description = "奶油留白与轻量信息层。",
            templates = listOf(
                template("travel_minimal", "奶油极简旅行", "奶油留白与轻量信息层", EventCardTemplateKey.TRAVEL_MINIMAL_EDITORIAL, "travel_editorial"),
            ),
        ),
        TemplateCategory(
            id = "travel_sunset",
            name = "黄昏照片旅行",
            description = "暖色风景与玻璃质感。",
            templates = listOf(
                template("travel_sunset", "黄昏照片旅行", "暖色风景与玻璃质感", EventCardTemplateKey.TRAVEL_SUNSET_GLASS, "travel_sunset"),
            ),
        ),
        TemplateCategory(
            id = "travel_scrapbook",
            name = "旅行手账纪念",
            description = "贴纸装饰与手写感。",
            templates = listOf(
                template("travel_scrapbook", "旅行手账纪念", "贴纸装饰与手写感", EventCardTemplateKey.TRAVEL_SCRAPBOOK, "travel_scrapbook"),
            ),
        ),
        TemplateCategory(
            id = "travel_countdown",
            name = "旅行倒计时",
            description = "兼容旧版旅行卡片样式。",
            templates = listOf(
                template("travel_countdown", "旅行倒计时", "兼容旧版旅行卡片样式", EventCardTemplateKey.TRAVEL_COUNTDOWN, "travel_countdown"),
            ),
        ),
        TemplateCategory(
            id = "classic",
            name = "简洁记录",
            description = "仅保留一张通用的经典信息卡，避免重复的蓝白模板。",
            templates = listOf(
                template("classic_clean", "经典简洁记录", "通用信息层级与清晰日期", EventCardTemplateKey.CLASSIC, "classic"),
            ),
        ),
    )

    val all: List<TemplateDefinition> = categories.flatMap { it.templates }

    private fun template(
        id: String,
        name: String,
        description: String,
        key: EventCardTemplateKey,
        categoryId: String,
        extraElements: List<String> = emptyList(),
    ) = TemplateDefinition(
        id = id,
        categoryId = categoryId,
        categoryName = categoriesName(categoryId),
        name = name,
        description = description,
        keywords = setOf(id, name, categoryId, categoriesName(categoryId)),
        templateKey = key,
        elements = listOf("title", "countdown", "date") + extraElements,
    )

    private fun categoriesName(categoryId: String): String = when (categoryId) {
        "travel_editorial" -> "奶油极简旅行"
        "travel_sunset" -> "黄昏照片旅行"
        "travel_scrapbook" -> "旅行手账纪念"
        "travel_countdown" -> "旅行倒计时"
        "classic" -> "简洁记录"
        else -> categoryId
    }
}
