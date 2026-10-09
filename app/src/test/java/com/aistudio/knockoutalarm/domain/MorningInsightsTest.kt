package com.aistudio.knockoutalarm.domain

import com.aistudio.knockoutalarm.model.AlarmSession
import com.aistudio.knockoutalarm.model.ChallengeRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MorningInsightsTest {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

    private fun parseTime(str: String): Long {
        return dateFormat.parse(str)?.time ?: System.currentTimeMillis()
    }

    @Test
    fun testInitialXpZeroAndRookieRank() {
        val info = MorningProgressLogic.calculateXpAndRank(emptyList())
        assertEquals(0, info.totalXp)
        assertEquals(0, info.distinctCompletedDays)
        assertEquals(FighterRank.ROOKIE, info.rank)
        assertEquals(FighterRank.CONTENDER, info.nextRank)
        assertEquals(0f, info.progressToNextRank, 0.001f)
    }

    @Test
    fun testDistinctDayXpIncrement() {
        val sessions = listOf(
            AlarmSession(
                id = 1L,
                startedAt = parseTime("2026-10-01 07:00"),
                completedAt = parseTime("2026-10-01 07:01"),
                targetPunches = 30,
                completedPunches = 30,
                durationMs = 45000L,
                emergencyStopped = false
            )
        )
        val info = MorningProgressLogic.calculateXpAndRank(sessions)
        assertEquals(100, info.totalXp)
        assertEquals(1, info.distinctCompletedDays)
        assertEquals(FighterRank.ROOKIE, info.rank)
    }

    @Test
    fun testSameDayDuplicateAlarmDoesNotFarmExtraXp() {
        // Two completed alarms on the exact same date (e.g. 7:00 AM and 8:30 AM)
        val sessions = listOf(
            AlarmSession(
                id = 1L,
                completedAt = parseTime("2026-10-01 07:00"),
                targetPunches = 30,
                completedPunches = 30,
                emergencyStopped = false
            ),
            AlarmSession(
                id = 2L,
                completedAt = parseTime("2026-10-01 08:30"),
                targetPunches = 30,
                completedPunches = 30,
                emergencyStopped = false
            )
        )
        val info = MorningProgressLogic.calculateXpAndRank(sessions)
        assertEquals("Repeated alarms on the same day cannot farm extra XP", 100, info.totalXp)
        assertEquals(1, info.distinctCompletedDays)
    }

    @Test
    fun testEmergencyStopAwardsZeroXp() {
        val sessions = listOf(
            AlarmSession(
                id = 1L,
                completedAt = parseTime("2026-10-01 07:00"),
                targetPunches = 30,
                completedPunches = 12,
                emergencyStopped = true // Emergency aborted!
            )
        )
        val info = MorningProgressLogic.calculateXpAndRank(sessions)
        assertEquals("Emergency stop must never award XP", 0, info.totalXp)
        assertEquals(0, info.distinctCompletedDays)
    }

    @Test
    fun testRanksThresholds() {
        // 7 distinct days -> 700 XP -> Contender
        val contenderSessions = (1..7).map { day ->
            val dayStr = if (day < 10) "0$day" else "$day"
            AlarmSession(
                id = day.toLong(),
                completedAt = parseTime("2026-10-$dayStr 07:00"),
                targetPunches = 30,
                completedPunches = 30,
                emergencyStopped = false
            )
        }
        val contenderInfo = MorningProgressLogic.calculateXpAndRank(contenderSessions)
        assertEquals(700, contenderInfo.totalXp)
        assertEquals(FighterRank.CONTENDER, contenderInfo.rank)

        // 21 distinct days -> 2100 XP -> Fighter
        val fighterSessions = (1..21).map { day ->
            val dayStr = if (day < 10) "0$day" else "$day"
            AlarmSession(
                id = day.toLong(),
                completedAt = parseTime("2026-09-$dayStr 07:00"),
                targetPunches = 30,
                completedPunches = 30,
                emergencyStopped = false
            )
        }
        val fighterInfo = MorningProgressLogic.calculateXpAndRank(fighterSessions)
        assertEquals(2100, fighterInfo.totalXp)
        assertEquals(FighterRank.FIGHTER, fighterInfo.rank)

        // 50 distinct days -> 5000 XP -> Champion
        val championSessions = (1..50).map { idx ->
            AlarmSession(
                id = idx.toLong(),
                completedAt = parseTime("2026-01-01 07:00") + (idx * 86400000L),
                targetPunches = 30,
                completedPunches = 30,
                emergencyStopped = false
            )
        }
        val championInfo = MorningProgressLogic.calculateXpAndRank(championSessions)
        assertEquals(5000, championInfo.totalXp)
        assertEquals(FighterRank.CHAMPION, championInfo.rank)
    }

    @Test
    fun testSevenDaySummary() {
        val refDate = dateFormat.parse("2026-10-07 12:00")!!

        val sessions = listOf(
            // Today (Oct 7)
            AlarmSession(id = 1L, completedAt = parseTime("2026-10-07 07:00"), targetPunches = 30, completedPunches = 30, durationMs = 35000L),
            // Oct 6
            AlarmSession(id = 2L, completedAt = parseTime("2026-10-06 07:00"), targetPunches = 30, completedPunches = 30, durationMs = 36000L),
            // Oct 5 (Aborted - should not count)
            AlarmSession(id = 3L, completedAt = parseTime("2026-10-05 07:00"), targetPunches = 30, completedPunches = 10, durationMs = 15000L, emergencyStopped = true),
            // Oct 4
            AlarmSession(id = 4L, completedAt = parseTime("2026-10-04 07:00"), targetPunches = 30, completedPunches = 30, durationMs = 38000L),
            // Oct 1 (outside 7-day window if window is Oct 1..7: Oct 1 is 6 days prior to Oct 7, so included)
            AlarmSession(id = 5L, completedAt = parseTime("2026-10-01 07:00"), targetPunches = 30, completedPunches = 30, durationMs = 40000L),
            // Sept 25 (way older, outside window)
            AlarmSession(id = 6L, completedAt = parseTime("2026-09-25 07:00"), targetPunches = 30, completedPunches = 30, durationMs = 40000L)
        )

        val summary = MorningProgressLogic.calculateSevenDaySummary(sessions, refDate)
        assertEquals(4, summary.completedMorningsOutOfSeven)
        assertEquals(120, summary.confirmedPunches)
        assertEquals((35 + 36 + 38 + 40).toLong(), summary.validatedDurationSeconds)
        assertTrue(summary.encouragingMessage.contains("4 of 7 mornings"))
    }

    @Test
    fun testRotatingPracticeFocus() {
        val focus = PracticeFocus.getFocusForDate(Date())
        assertNotNull(focus.title)
        assertNotNull(focus.prompt)
        assertNotNull(focus.tip)
        // Ensure no untracked movements (hooks, slips, rolls) are advertised
        assertFalse(focus.prompt.contains("hook", ignoreCase = true))
        assertFalse(focus.prompt.contains("slip", ignoreCase = true))
        assertFalse(focus.prompt.contains("roll", ignoreCase = true))
    }

    @Test
    fun testOptionalRoundTimerCalculations() {
        // At start (180s left)
        val s1 = OptionalRoundState(isRunning = true, totalRemainingSeconds = 180)
        assertEquals(1, s1.currentCycle)
        assertTrue("First 30 seconds must be shadowboxing", s1.isShadowboxingPhase)
        assertEquals(30, s1.phaseRemainingSeconds)

        // At 160s left (20s elapsed -> still shadowboxing)
        val s2 = OptionalRoundState(isRunning = true, totalRemainingSeconds = 160)
        assertEquals(1, s2.currentCycle)
        assertTrue(s2.isShadowboxingPhase)
        assertEquals(10, s2.phaseRemainingSeconds)

        // At 140s left (40s elapsed -> rest phase of cycle 1)
        val s3 = OptionalRoundState(isRunning = true, totalRemainingSeconds = 140)
        assertEquals(1, s3.currentCycle)
        assertFalse("Second 30s of cycle is rest", s3.isShadowboxingPhase)
        assertEquals(20, s3.phaseRemainingSeconds)

        // At 100s left (80s elapsed: 60s in cycle 1, 20s in cycle 2 -> cycle 2 shadowboxing)
        val s4 = OptionalRoundState(isRunning = true, totalRemainingSeconds = 100)
        assertEquals(2, s4.currentCycle)
        assertTrue(s4.isShadowboxingPhase)
    }

    @Test
    fun testChallengeRecordActiveMillis() {
        val record: ChallengeRecord = AlarmSession(
            id = 99L,
            durationMs = 42100L
        )
        assertEquals(42100L, record.activeMillis)
    }
}
