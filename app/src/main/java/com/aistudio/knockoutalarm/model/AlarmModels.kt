package com.aistudio.knockoutalarm.model

enum class Difficulty(val displayName: String, val description: String) {
    EASY("Easy", "Forgiving elbow angles & lower velocity threshold. Ideal for beginners."),
    NORMAL("Normal", "Balanced extension required (150° elbow) and realistic punch cadence."),
    HARD("Hard", "Full extension required (160° elbow) and snappy velocity.")
}

enum class AlarmState {
    SCHEDULED,
    RINGING,
    CHALLENGE_ACTIVE,
    COMPLETED,
    EMERGENCY_STOPPED,
    MISSED
}

data class Alarm(
    val id: Long = 0L,
    val hour: Int = 7,
    val minute: Int = 0,
    val enabled: Boolean = true,
    val repeatDays: Set<Int> = setOf(1, 2, 3, 4, 5), // 1=Mon .. 7=Sun
    val punchTarget: Int = 30,
    val difficulty: Difficulty = Difficulty.NORMAL,
    val soundId: String = "SIREN",
    val vibrate: Boolean = true,
    val announcerEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val formattedTime: String
        get() {
            val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
            val m = minute.toString().padStart(2, '0')
            val amPm = if (hour < 12) "AM" else "PM"
            return "$h:$m $amPm"
        }

    val repeatDaysSummary: String
        get() {
            if (repeatDays.isEmpty()) return "Once"
            if (repeatDays.size == 7) return "Every day"
            if (repeatDays == setOf(1, 2, 3, 4, 5)) return "Weekdays"
            if (repeatDays == setOf(6, 7)) return "Weekends"
            val names = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            return repeatDays.sorted().mapNotNull { if (it in 1..7) names[it - 1] else null }.joinToString(", ")
        }
}

data class AlarmSession(
    val id: Long = 0L,
    val alarmId: Long = 0L,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long = System.currentTimeMillis(),
    val targetPunches: Int = 30,
    val completedPunches: Int = 30,
    val leftPunches: Int = 15,
    val rightPunches: Int = 15,
    val durationMs: Long = 35000L,
    val emergencyStopped: Boolean = false,
    val averagePunchVelocity: Float = 1.45f,
    val averageCadence: Float = 0.85f // punches per second
) {
    val activeMillis: Long get() = durationMs

    val formattedDuration: String
        get() {
            val totalSeconds = (durationMs / 1000).toInt()
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
        }
}

data class UserStats(
    val id: Long = 1L,
    val currentStreak: Int = 1,
    val longestStreak: Int = 1,
    val totalPunches: Int = 0,
    val successfulWakeups: Int = 0,
    val failedWakeups: Int = 0,
    val averageCompletionTimeMs: Long = 40000L,
    val lastCompletedDate: String = ""
)

enum class SubscriptionTier {
    FREE,
    PRO
}

typealias ChallengeRecord = AlarmSession

