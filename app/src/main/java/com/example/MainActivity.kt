package com.example

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aistudio.knockoutalarm.model.Alarm
import com.aistudio.knockoutalarm.model.AlarmSession
import com.aistudio.knockoutalarm.model.Difficulty
import com.aistudio.knockoutalarm.ui.MorningProgressScreen
import com.aistudio.knockoutalarm.ui.screens.AlarmEditorScreen
import com.aistudio.knockoutalarm.ui.screens.FightArenaScreen
import com.aistudio.knockoutalarm.ui.screens.HomeScreen
import com.aistudio.knockoutalarm.ui.screens.StatsScreen
import com.aistudio.knockoutalarm.ui.screens.VictoryScreen
import com.aistudio.knockoutalarm.ui.viewmodel.KnockoutViewModel
import com.example.ui.theme.KnockoutAlarmTheme

sealed class Screen {
    object Home : Screen()
    data class Editor(val alarm: Alarm?) : Screen()
    data class Arena(val alarm: Alarm) : Screen()
    object Victory : Screen()
    object Stats : Screen()
    object MorningProgress : Screen()
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureLockScreenVisibility()

        val initialRoute = intent?.getStringExtra("route")
        val initialAlarmId = intent?.getLongExtra("alarm_id", 0L) ?: 0L
        val initialPunchTarget = intent?.getIntExtra("punch_target", 30) ?: 30
        val initialDifficultyStr = intent?.getStringExtra("difficulty") ?: "NORMAL"
        val initialDiff = try { Difficulty.valueOf(initialDifficultyStr) } catch (e: Exception) { Difficulty.NORMAL }

        setContent {
            KnockoutAlarmTheme {
                val viewModel: KnockoutViewModel = viewModel()
                val alarms by viewModel.alarms.collectAsState()
                val stats by viewModel.stats.collectAsState()
                val sessions by viewModel.sessions.collectAsState()
                val subscriptionTier by viewModel.subscriptionTier.collectAsState()
                val lastCompletedSession by viewModel.lastCompletedSession.collectAsState()
                val testAlarmCountdownSeconds by viewModel.testAlarmCountdownSeconds.collectAsState()

                var currentScreen by remember {
                    mutableStateOf<Screen>(
                        if (initialRoute == "arena") {
                            val alarm = alarms.firstOrNull { it.id == initialAlarmId }
                                ?: Alarm(id = initialAlarmId, punchTarget = initialPunchTarget, difficulty = initialDiff)
                            Screen.Arena(alarm)
                        } else {
                            Screen.Home
                        }
                    )
                }

                // If intent changes while activity is open (e.g. onNewIntent)
                LaunchedEffect(intent) {
                    if (intent?.getStringExtra("route") == "arena") {
                        val alarm = alarms.firstOrNull { it.id == initialAlarmId }
                            ?: Alarm(id = initialAlarmId, punchTarget = initialPunchTarget, difficulty = initialDiff)
                        currentScreen = Screen.Arena(alarm)
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (val screen = currentScreen) {
                        is Screen.Home -> {
                            HomeScreen(
                                alarms = alarms,
                                stats = stats,
                                sessions = sessions,
                                subscriptionTier = subscriptionTier,
                                countdownSeconds = testAlarmCountdownSeconds,
                                onToggleAlarm = { viewModel.toggleAlarm(it) },
                                onEditAlarm = { currentScreen = Screen.Editor(it) },
                                onDeleteAlarm = { viewModel.deleteAlarm(it) },
                                onAddNewAlarm = { currentScreen = Screen.Editor(null) },
                                onLaunchFightArena = { currentScreen = Screen.Arena(it) },
                                onViewStats = { currentScreen = Screen.Stats },
                                onOpenMorningProgress = { currentScreen = Screen.MorningProgress },
                                onTriggerInstantAlarm = { testTarget ->
                                    val testAlarm = Alarm(
                                        id = com.aistudio.knockoutalarm.alarm.AlarmScheduler.TEST_ALARM_ID,
                                        punchTarget = testTarget,
                                        difficulty = Difficulty.NORMAL,
                                        soundId = "SIREN"
                                    )
                                    // Start foreground ringing service immediately
                                    val serviceIntent = Intent(this@MainActivity, com.aistudio.knockoutalarm.alarm.AlarmRingingService::class.java).apply {
                                        putExtra(com.aistudio.knockoutalarm.alarm.AlarmScheduler.EXTRA_ALARM_ID, testAlarm.id)
                                        putExtra(com.aistudio.knockoutalarm.alarm.AlarmScheduler.EXTRA_PUNCH_TARGET, testAlarm.punchTarget)
                                        putExtra(com.aistudio.knockoutalarm.alarm.AlarmScheduler.EXTRA_DIFFICULTY, testAlarm.difficulty.name)
                                        putExtra(com.aistudio.knockoutalarm.alarm.AlarmScheduler.EXTRA_SOUND_ID, testAlarm.soundId)
                                    }
                                    androidx.core.content.ContextCompat.startForegroundService(this@MainActivity, serviceIntent)
                                    currentScreen = Screen.Arena(testAlarm)
                                },
                                onTriggerDelayedAlarm = { delaySec, testTarget ->
                                    viewModel.triggerDelayedAlarmTest(delaySec, testTarget)
                                },
                                onCancelDelayedAlarm = {
                                    viewModel.cancelDelayedAlarmTest()
                                },
                                onToggleDevTier = { viewModel.toggleSubscriptionTier() }
                            )
                        }

                        is Screen.Editor -> {
                            BackHandler { currentScreen = Screen.Home }
                            AlarmEditorScreen(
                                initialAlarm = screen.alarm,
                                subscriptionTier = subscriptionTier,
                                onSave = { alarm ->
                                    viewModel.saveAlarm(alarm) {
                                        currentScreen = Screen.Home
                                    }
                                },
                                onDelete = { alarm ->
                                    viewModel.deleteAlarm(alarm)
                                    currentScreen = Screen.Home
                                },
                                onBack = { currentScreen = Screen.Home }
                            )
                        }

                        is Screen.Arena -> {
                            BackHandler {
                                // Prevent accidental back press during active fight; returns safely
                                currentScreen = Screen.Home
                            }
                            FightArenaScreen(
                                alarm = screen.alarm,
                                onFightCompleted = { targetPunches, completedPunches, leftPunches, rightPunches, durationMs, avgVel, cadence, wasAborted ->
                                    viewModel.recordCompletedFight(
                                        alarmId = screen.alarm.id,
                                        targetPunches = targetPunches,
                                        completedPunches = completedPunches,
                                        leftPunches = leftPunches,
                                        rightPunches = rightPunches,
                                        durationMs = durationMs,
                                        averageVelocity = avgVel,
                                        averageCadence = cadence,
                                        emergencyStopped = wasAborted
                                    )
                                    currentScreen = Screen.Victory
                                },
                                onEmergencyStop = {
                                    currentScreen = Screen.Victory
                                }
                            )
                        }

                        is Screen.Victory -> {
                            BackHandler { currentScreen = Screen.Home }
                            VictoryScreen(
                                session = lastCompletedSession,
                                stats = stats,
                                onDone = { currentScreen = Screen.Home },
                                onViewStats = { currentScreen = Screen.Stats },
                                onOpenMorningProgress = { currentScreen = Screen.MorningProgress }
                            )
                        }

                        is Screen.Stats -> {
                            BackHandler { currentScreen = Screen.Home }
                            StatsScreen(
                                stats = stats,
                                sessions = sessions,
                                onBack = { currentScreen = Screen.Home }
                            )
                        }

                        is Screen.MorningProgress -> {
                            BackHandler { currentScreen = Screen.Home }
                            MorningProgressScreen(
                                stats = stats,
                                sessions = sessions,
                                onBack = { currentScreen = Screen.Home }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun configureLockScreenVisibility() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }
}
