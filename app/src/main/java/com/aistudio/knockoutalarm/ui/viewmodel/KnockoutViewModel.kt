package com.aistudio.knockoutalarm.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.knockoutalarm.alarm.AlarmRingingService
import com.aistudio.knockoutalarm.alarm.AlarmScheduler
import com.aistudio.knockoutalarm.data.AlarmRepository
import com.aistudio.knockoutalarm.model.Alarm
import com.aistudio.knockoutalarm.model.AlarmSession
import com.aistudio.knockoutalarm.model.Difficulty
import com.aistudio.knockoutalarm.model.SubscriptionTier
import com.aistudio.knockoutalarm.model.UserStats
import com.aistudio.knockoutalarm.subscription.SubscriptionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class KnockoutViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AlarmRepository(application)
    private val scheduler = AlarmScheduler(application)
    val subscriptionService = SubscriptionService(application)

    val alarms: StateFlow<List<Alarm>> = repository.alarmsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        emptyList()
    )

    val sessions: StateFlow<List<AlarmSession>> = repository.sessionsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        emptyList()
    )

    val stats: StateFlow<UserStats> = repository.statsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        UserStats()
    )

    val subscriptionTier: StateFlow<SubscriptionTier> = subscriptionService.tierState

    private val _lastCompletedSession = MutableStateFlow<AlarmSession?>(null)
    val lastCompletedSession: StateFlow<AlarmSession?> = _lastCompletedSession.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    fun saveAlarm(alarm: Alarm, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val savedId = repository.saveAlarm(alarm)
            val updatedAlarm = alarm.copy(id = savedId)
            if (updatedAlarm.enabled) {
                scheduler.scheduleAlarm(updatedAlarm)
            } else {
                scheduler.cancelAlarm(updatedAlarm)
            }
            onComplete()
        }
    }

    fun toggleAlarm(alarm: Alarm) {
        viewModelScope.launch {
            val newEnabled = !alarm.enabled
            repository.toggleAlarm(alarm.id, newEnabled)
            val updated = alarm.copy(enabled = newEnabled)
            if (newEnabled) {
                scheduler.scheduleAlarm(updated)
            } else {
                scheduler.cancelAlarm(updated)
            }
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            scheduler.cancelAlarm(alarm)
            repository.deleteAlarm(alarm)
        }
    }

    fun recordCompletedFight(
        alarmId: Long,
        targetPunches: Int,
        completedPunches: Int,
        leftPunches: Int,
        rightPunches: Int,
        durationMs: Long,
        averageVelocity: Float,
        averageCadence: Float,
        emergencyStopped: Boolean
    ) {
        viewModelScope.launch {
            val session = repository.recordCompletedSession(
                alarmId = alarmId,
                targetPunches = targetPunches,
                completedPunches = completedPunches,
                leftPunches = leftPunches,
                rightPunches = rightPunches,
                durationMs = durationMs,
                averageVelocity = averageVelocity,
                averageCadence = averageCadence,
                emergencyStopped = emergencyStopped
            )
            _lastCompletedSession.value = session
            // Stop foreground alarm ringing service if running
            AlarmRingingService.instance?.stopAlarmAndService()
        }
    }

    fun toggleSubscriptionTier() {
        val current = subscriptionService.tierState.value
        val next = if (current == SubscriptionTier.PRO) SubscriptionTier.FREE else SubscriptionTier.PRO
        subscriptionService.setTier(next)
    }

    // --- Alarm Test & Simulation ---

    private val _testAlarmCountdownSeconds = MutableStateFlow<Int?>(null)
    val testAlarmCountdownSeconds: StateFlow<Int?> = _testAlarmCountdownSeconds.asStateFlow()

    private var testCountdownJob: kotlinx.coroutines.Job? = null

    fun triggerDelayedAlarmTest(delaySeconds: Int, punchTarget: Int) {
        scheduler.scheduleTestAlarm(delaySeconds, punchTarget)
        testCountdownJob?.cancel()
        testCountdownJob = viewModelScope.launch {
            for (sec in delaySeconds downTo 1) {
                _testAlarmCountdownSeconds.value = sec
                kotlinx.coroutines.delay(1000L)
            }
            _testAlarmCountdownSeconds.value = null
        }
    }

    fun cancelDelayedAlarmTest() {
        testCountdownJob?.cancel()
        testCountdownJob = null
        _testAlarmCountdownSeconds.value = null
        scheduler.cancelTestAlarm()
    }
}
