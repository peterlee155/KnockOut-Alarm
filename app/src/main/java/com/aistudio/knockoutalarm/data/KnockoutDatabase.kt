package com.aistudio.knockoutalarm.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import com.aistudio.knockoutalarm.model.Alarm
import com.aistudio.knockoutalarm.model.AlarmSession
import com.aistudio.knockoutalarm.model.Difficulty
import com.aistudio.knockoutalarm.model.UserStats
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean,
    val repeatDays: String, // comma separated days e.g. "1,2,3,4,5"
    val punchTarget: Int,
    val difficulty: String,
    val soundId: String,
    val vibrate: Boolean,
    val announcerEnabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): Alarm {
        val days = if (repeatDays.isBlank()) emptySet() else repeatDays.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
        val diff = try { Difficulty.valueOf(difficulty) } catch (e: Exception) { Difficulty.NORMAL }
        return Alarm(
            id = id,
            hour = hour,
            minute = minute,
            enabled = enabled,
            repeatDays = days,
            punchTarget = punchTarget,
            difficulty = diff,
            soundId = soundId,
            vibrate = vibrate,
            announcerEnabled = announcerEnabled,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(alarm: Alarm): AlarmEntity {
            return AlarmEntity(
                id = alarm.id,
                hour = alarm.hour,
                minute = alarm.minute,
                enabled = alarm.enabled,
                repeatDays = alarm.repeatDays.joinToString(","),
                punchTarget = alarm.punchTarget,
                difficulty = alarm.difficulty.name,
                soundId = alarm.soundId,
                vibrate = alarm.vibrate,
                announcerEnabled = alarm.announcerEnabled,
                createdAt = alarm.createdAt,
                updatedAt = alarm.updatedAt
            )
        }
    }
}

@Entity(tableName = "alarm_sessions")
data class AlarmSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val alarmId: Long,
    val startedAt: Long,
    val completedAt: Long,
    val targetPunches: Int,
    val completedPunches: Int,
    val leftPunches: Int,
    val rightPunches: Int,
    val durationMs: Long,
    val emergencyStopped: Boolean,
    val averagePunchVelocity: Float,
    val averageCadence: Float
) {
    fun toDomain(): AlarmSession = AlarmSession(
        id = id,
        alarmId = alarmId,
        startedAt = startedAt,
        completedAt = completedAt,
        targetPunches = targetPunches,
        completedPunches = completedPunches,
        leftPunches = leftPunches,
        rightPunches = rightPunches,
        durationMs = durationMs,
        emergencyStopped = emergencyStopped,
        averagePunchVelocity = averagePunchVelocity,
        averageCadence = averageCadence
    )

    companion object {
        fun fromDomain(session: AlarmSession): AlarmSessionEntity = AlarmSessionEntity(
            id = session.id,
            alarmId = session.alarmId,
            startedAt = session.startedAt,
            completedAt = session.completedAt,
            targetPunches = session.targetPunches,
            completedPunches = session.completedPunches,
            leftPunches = session.leftPunches,
            rightPunches = session.rightPunches,
            durationMs = session.durationMs,
            emergencyStopped = session.emergencyStopped,
            averagePunchVelocity = session.averagePunchVelocity,
            averageCadence = session.averageCadence
        )
    }
}

@Entity(tableName = "user_stats")
data class UserStatsEntity(
    @PrimaryKey val id: Long = 1L,
    val currentStreak: Int,
    val longestStreak: Int,
    val totalPunches: Int,
    val successfulWakeups: Int,
    val failedWakeups: Int,
    val averageCompletionTimeMs: Long,
    val lastCompletedDate: String
) {
    fun toDomain(): UserStats = UserStats(
        id = id,
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        totalPunches = totalPunches,
        successfulWakeups = successfulWakeups,
        failedWakeups = failedWakeups,
        averageCompletionTimeMs = averageCompletionTimeMs,
        lastCompletedDate = lastCompletedDate
    )

    companion object {
        fun fromDomain(stats: UserStats): UserStatsEntity = UserStatsEntity(
            id = stats.id,
            currentStreak = stats.currentStreak,
            longestStreak = stats.longestStreak,
            totalPunches = stats.totalPunches,
            successfulWakeups = stats.successfulWakeups,
            failedWakeups = stats.failedWakeups,
            averageCompletionTimeMs = stats.averageCompletionTimeMs,
            lastCompletedDate = stats.lastCompletedDate
        )
    }
}

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY hour ASC, minute ASC")
    fun getAllAlarmsFlow(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE enabled = 1 ORDER BY hour ASC, minute ASC")
    suspend fun getEnabledAlarms(): List<AlarmEntity>

    @Query("SELECT * FROM alarms WHERE id = :id LIMIT 1")
    suspend fun getAlarmById(id: Long): AlarmEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: AlarmEntity): Long

    @Update
    suspend fun updateAlarm(alarm: AlarmEntity)

    @Delete
    suspend fun deleteAlarm(alarm: AlarmEntity)
}

@Dao
interface AlarmSessionDao {
    @Query("SELECT * FROM alarm_sessions ORDER BY completedAt DESC")
    fun getAllSessionsFlow(): Flow<List<AlarmSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: AlarmSessionEntity): Long
}

@Dao
interface UserStatsDao {
    @Query("SELECT * FROM user_stats WHERE id = 1 LIMIT 1")
    fun getUserStatsFlow(): Flow<UserStatsEntity?>

    @Query("SELECT * FROM user_stats WHERE id = 1 LIMIT 1")
    suspend fun getUserStats(): UserStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stats: UserStatsEntity)
}

@Database(
    entities = [AlarmEntity::class, AlarmSessionEntity::class, UserStatsEntity::class],
    version = 1,
    exportSchema = false
)
abstract class KnockoutDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun alarmSessionDao(): AlarmSessionDao
    abstract fun userStatsDao(): UserStatsDao
}
