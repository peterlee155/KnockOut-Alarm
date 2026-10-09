package com.aistudio.knockoutalarm.data

import android.content.Context
import androidx.room.Room
import com.aistudio.knockoutalarm.model.Alarm
import com.aistudio.knockoutalarm.model.AlarmSession
import com.aistudio.knockoutalarm.model.Difficulty
import com.aistudio.knockoutalarm.model.UserStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

class AlarmRepository(context: Context) {

    private val db = Room.databaseBuilder(
        context.applicationContext,
        KnockoutDatabase::class.java,
        "knockout_alarm.db"
    ).fallbackToDestructiveMigration().build()

    private val alarmDao = db.alarmDao()
    private val sessionDao = db.alarmSessionDao()
    private val statsDao = db.userStatsDao()

    val alarmsFlow: Flow<List<Alarm>> = alarmDao.getAllAlarmsFlow().map { entities ->
        entities.map { it.toDomain() }
    }

    val sessionsFlow: Flow<List<AlarmSession>> = sessionDao.getAllSessionsFlow().map { entities ->
        entities.map { it.toDomain() }
    }

    val statsFlow: Flow<UserStats> = statsDao.getUserStatsFlow().map { entity ->
        entity?.toDomain() ?: UserStats(
            currentStreak = 1,
            longestStreak = 1,
            totalPunches = 0,
            successfulWakeups = 0,
            failedWakeups = 0
        )
    }

    suspend fun getAlarmById(id: Long): Alarm? {
        return alarmDao.getAlarmById(id)?.toDomain()
    }

    suspend fun getEnabledAlarms(): List<Alarm> {
        return alarmDao.getEnabledAlarms().map { it.toDomain() }
    }

    suspend fun saveAlarm(alarm: Alarm): Long {
        return if (alarm.id == 0L) {
            alarmDao.insertAlarm(AlarmEntity.fromDomain(alarm))
        } else {
            alarmDao.updateAlarm(AlarmEntity.fromDomain(alarm.copy(updatedAt = System.currentTimeMillis())))
            alarm.id
        }
    }

    suspend fun toggleAlarm(alarmId: Long, enabled: Boolean) {
        val current = alarmDao.getAlarmById(alarmId) ?: return
        alarmDao.updateAlarm(current.copy(enabled = enabled, updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteAlarm(alarm: Alarm) {
        alarmDao.deleteAlarm(AlarmEntity.fromDomain(alarm))
    }

    suspend fun recordCompletedSession(
        alarmId: Long,
        targetPunches: Int,
        completedPunches: Int,
        leftPunches: Int,
        rightPunches: Int,
        durationMs: Long,
        averageVelocity: Float,
        averageCadence: Float,
        emergencyStopped: Boolean
    ): AlarmSession {
        val now = System.currentTimeMillis()
        val session = AlarmSession(
            alarmId = alarmId,
            startedAt = now - durationMs,
            completedAt = now,
            targetPunches = targetPunches,
            completedPunches = completedPunches,
            leftPunches = leftPunches,
            rightPunches = rightPunches,
            durationMs = durationMs,
            emergencyStopped = emergencyStopped,
            averagePunchVelocity = averageVelocity,
            averageCadence = averageCadence
        )
        val id = sessionDao.insertSession(AlarmSessionEntity.fromDomain(session))
        val savedSession = session.copy(id = id)

        // Update user stats
        val currentStats = statsDao.getUserStats()?.toDomain() ?: UserStats()
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))

        if (emergencyStopped) {
            val updated = currentStats.copy(
                failedWakeups = currentStats.failedWakeups + 1,
                currentStreak = 0 // Break streak on emergency stop
            )
            statsDao.insertOrUpdate(UserStatsEntity.fromDomain(updated))
        } else {
            val isNewDay = currentStats.lastCompletedDate != todayStr
            val newStreak = if (isNewDay) currentStats.currentStreak + 1 else currentStats.currentStreak
            val newLongest = max(currentStats.longestStreak, newStreak)
            val totalPunches = currentStats.totalPunches + completedPunches
            val successfulWakeups = currentStats.successfulWakeups + 1
            val updated = currentStats.copy(
                currentStreak = newStreak,
                longestStreak = newLongest,
                totalPunches = totalPunches,
                successfulWakeups = successfulWakeups,
                lastCompletedDate = todayStr
            )
            statsDao.insertOrUpdate(UserStatsEntity.fromDomain(updated))
        }

        return savedSession
    }

    suspend fun seedInitialDataIfEmpty() {
        val existing = alarmDao.getAlarmById(1L)
        if (existing == null) {
            val defaultAlarm = Alarm(
                hour = 7,
                minute = 0,
                enabled = true,
                repeatDays = setOf(1, 2, 3, 4, 5),
                punchTarget = 30,
                difficulty = Difficulty.NORMAL,
                soundId = "SIREN"
            )
            alarmDao.insertAlarm(AlarmEntity.fromDomain(defaultAlarm))

            val initialStats = UserStats(
                id = 1L,
                currentStreak = 3,
                longestStreak = 5,
                totalPunches = 120,
                successfulWakeups = 4,
                failedWakeups = 0,
                averageCompletionTimeMs = 38000L
            )
            statsDao.insertOrUpdate(UserStatsEntity.fromDomain(initialStats))
        }
    }
}
