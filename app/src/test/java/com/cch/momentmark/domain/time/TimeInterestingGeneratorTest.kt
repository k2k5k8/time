package com.cch.momentmark.domain.time

import java.time.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeInterestingGeneratorTest {
    @Test
    fun pastDateProducesStableTimeFactsAndMilestones() {
        val cards = TimeInterestingGenerator.generate(
            targetDate = LocalDate.of(1998, 10, 4),
            today = LocalDate.of(2026, 8, 20),
        )

        assertTrue(cards.any { it.title == "已经过了多久" && it.value.contains("天") })
        assertTrue(cards.any { it.title == "吃饭次数" && it.value.contains("顿") })
        assertTrue(cards.any { it.title == "万日节点" })
        assertTrue(cards.any { it.title == "日期巧合" })
    }

    @Test
    fun futureDateDoesNotReportNegativeFacts() {
        val cards = TimeInterestingGenerator.generate(
            targetDate = LocalDate.of(2027, 8, 20),
            today = LocalDate.of(2026, 8, 20),
        )

        assertTrue(cards.first().value.contains("还有"))
        assertTrue(cards.first { it.title == "万日节点" }.value.contains("10000 天"))
        assertTrue(cards.first { it.title == "还需要吃饭几顿" }.value.contains("还需要吃"))
    }

    @Test
    fun targetDayUsesTargetDayLanguageInsteadOfZeroPastFacts() {
        val cards = TimeInterestingGenerator.generate(
            targetDate = LocalDate.of(2026, 8, 20),
            today = LocalDate.of(2026, 8, 20),
        )

        assertTrue(cards.any { it.title == "今天就是目标日" })
        assertTrue(cards.none { it.value.contains("已经吃了 0 顿") })
    }
}
