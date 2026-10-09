package com.aistudio.knockoutalarm.domain

import com.aistudio.knockoutalarm.model.AlarmSession
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

/**
 * Fighter ranks based on distinct completed wake-up days (100 XP per distinct day).
 * Rookie starts at 0, Contender at 700 (7 days), Fighter at 2100 (21 days), Champion at 5000 (50 days).
 */
enum class FighterRank(
    val displayName: String,
    val minXp: Int,
    val maxXp: Int,
    val badgeEmoji: String,
    val description: String
) {
    ROOKIE("Rookie", 0, 699, "🥊", "Building the morning wake-up routine"),
    CONTENDER("Contender", 700, 2099, "🔥", "Consistent 7+ morning bouts won"),
    FIGHTER("Fighter", 2100, 4999, "⚡", "Dedicated 21+ mornings completed"),
    CHAMPION("Champion", 5000, Int.MAX_VALUE, "👑", "50+ mornings conquered before breakfast");

    companion object {
        fun fromXp(xp: Int): FighterRank {
            return when {
                xp >= CHAMPION.minXp -> CHAMPION
                xp >= FIGHTER.minXp -> FIGHTER
                xp >= CONTENDER.minXp -> CONTENDER
                else -> ROOKIE
            }
        }
    }
}

data class XpRankInfo(
    val totalXp: Int,
    val distinctCompletedDays: Int,
    val rank: FighterRank,
    val nextRank: FighterRank?,
    val progressToNextRank: Float // 0.0 to 1.0
)

/**
 * Seven rotating daily practice focuses for the straight-punch routine.
 * Emphasizes rhythm, returning to guard, alternating arms, and camera setup.
 */
data class PracticeFocus(
    val dayNumber: Int,
    val title: String,
    val prompt: String,
    val tip: String
) {
    companion object {
        val ROTATING_FOCUSES = listOf(
            PracticeFocus(
                dayNumber = 1,
                title = "Reset to Guard",
                prompt = "Snap hands directly back to your chin after each punch.",
                tip = "The punch only counts when the elbow angle drops back to guard (< 115°)."
            ),
            PracticeFocus(
                dayNumber = 2,
                title = "Camera Distance & Framing",
                prompt = "Set device at chest-height and step back 4–6 feet.",
                tip = "Ensure both shoulders, elbows, and wrists are in clear view with steady lighting."
            ),
            PracticeFocus(
                dayNumber = 3,
                title = "Alternating Left & Right",
                prompt = "Alternate evenly between your left jab and right cross.",
                tip = "An alternating 1-2 cadence prevents fatigue and maintains fluid rhythm."
            ),
            PracticeFocus(
                dayNumber = 4,
                title = "Clean Arm Extension",
                prompt = "Extend fully straight toward the camera (150°+ elbow angle).",
                tip = "Avoid short half-punches; full extensions trigger clean peak detection."
            ),
            PracticeFocus(
                dayNumber = 5,
                title = "Breathing & Pacing",
                prompt = "Exhale sharply on each forward extension.",
                tip = "Aim for a relaxed 1 punch per second pace—speed does not grant extra points."
            ),
            PracticeFocus(
                dayNumber = 6,
                title = "Balanced Athletic Stance",
                prompt = "Plant feet shoulder-width apart with knees slightly bent.",
                tip = "A steady base keeps your torso stable so the anti-shake detector stays green."
            ),
            PracticeFocus(
                dayNumber = 7,
                title = "Finish Strong & Recover",
                prompt = "Keep your guard high through all 30 punches.",
                tip = "After the knockout bell, transition directly to the optional 3-minute shadowbox round."
            )
        )

        fun getFocusForDate(date: Date = Date()): PracticeFocus {
            val calendar = Calendar.getInstance()
            calendar.time = date
            val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
            val index = (dayOfYear - 1) % ROTATING_FOCUSES.size
            return ROTATING_FOCUSES[index]
        }
    }
}

/**
 * Summary metrics of the past seven days from local Room history.
 */
data class SevenDaySummary(
    val completedMorningsOutOfSeven: Int,
    val confirmedPunches: Int,
    val validatedDurationSeconds: Long,
    val encouragingMessage: String
)

/**
 * State for the optional 3-minute post-alarm shadowbox round.
 * 3 cycles of: 30 seconds work / 30 seconds rest (180s total).
 */
data class OptionalRoundState(
    val isRunning: Boolean = false,
    val totalRemainingSeconds: Int = 180,
    val isCompleted: Boolean = false
) {
    val currentCycle: Int
        get() {
            val elapsed = 180 - totalRemainingSeconds
            return (elapsed / 60) + 1 // Cycle 1, 2, or 3
        }

    val isShadowboxingPhase: Boolean
        get() {
            val elapsedInCycle = (180 - totalRemainingSeconds) % 60
            return elapsedInCycle < 30
        }

    val phaseRemainingSeconds: Int
        get() {
            val elapsedInCycle = (180 - totalRemainingSeconds) % 60
            return if (elapsedInCycle < 30) 30 - elapsedInCycle else 60 - elapsedInCycle
        }

    val phaseLabel: String
        get() = if (isShadowboxingPhase) "SHADOWBOXING 🥊" else "REST & BREATHE 💧"
}

object MorningProgressLogic {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * Calculates XP and rank: 100 XP per distinct completed local day.
     * Emergency dismissal never earns credit. Same-day duplicate alarms do not award extra XP.
     */
    fun calculateXpAndRank(sessions: List<AlarmSession>): XpRankInfo {
        val successfulSessions = sessions.filter { !it.emergencyStopped && it.completedPunches >= it.targetPunches }
        val distinctDates = successfulSessions.map {
            dateFormat.format(Date(it.completedAt))
        }.toSet()

        val distinctDays = distinctDates.size
        val totalXp = distinctDays * 100
        val currentRank = FighterRank.fromXp(totalXp)

        val nextRank = when (currentRank) {
            FighterRank.ROOKIE -> FighterRank.CONTENDER
            FighterRank.CONTENDER -> FighterRank.FIGHTER
            FighterRank.FIGHTER -> FighterRank.CHAMPION
            FighterRank.CHAMPION -> null
        }

        val progress = if (nextRank == null) {
            1.0f
        } else {
            val range = nextRank.minXp - currentRank.minXp
            val currentInRange = totalXp - currentRank.minXp
            (currentInRange.toFloat() / range.toFloat()).coerceIn(0f, 1f)
        }

        return XpRankInfo(
            totalXp = totalXp,
            distinctCompletedDays = distinctDays,
            rank = currentRank,
            nextRank = nextRank,
            progressToNextRank = progress
        )
    }

    /**
     * Evaluates seven-day completion summary from local Room history.
     */
    fun calculateSevenDaySummary(
        sessions: List<AlarmSession>,
        referenceDate: Date = Date()
    ): SevenDaySummary {
        val cal = Calendar.getInstance()
        cal.time = referenceDate
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        val endMs = cal.timeInMillis

        cal.add(Calendar.DAY_OF_YEAR, -6)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val startMs = cal.timeInMillis

        // Filter valid completed sessions in window
        val windowSessions = sessions.filter {
            it.completedAt in startMs..endMs &&
                    !it.emergencyStopped &&
                    it.completedPunches >= it.targetPunches
        }

        val distinctDays = windowSessions.map {
            dateFormat.format(Date(it.completedAt))
        }.toSet().size

        val totalPunches = windowSessions.sumOf { it.completedPunches }
        val totalDurationSeconds = windowSessions.sumOf { it.durationMs } / 1000L

        val message = when {
            distinctDays >= 7 -> "Perfect 7/7 week! Complete mastery before breakfast."
            distinctDays >= 5 -> "$distinctDays of 7 mornings conquered! Outstanding morning discipline."
            distinctDays >= 3 -> "$distinctDays of 7 mornings completed! Great routine taking shape."
            distinctDays >= 1 -> "$distinctDays morning completed. Every round builds momentum."
            else -> "Start your week strong by beating tomorrow's morning alarm."
        }

        return SevenDaySummary(
            completedMorningsOutOfSeven = distinctDays,
            confirmedPunches = totalPunches,
            validatedDurationSeconds = totalDurationSeconds,
            encouragingMessage = message
        )
    }
}
