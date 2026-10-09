package com.aistudio.knockoutalarm.ui.screens

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.aistudio.knockoutalarm.alarm.AlarmRingingService
import com.aistudio.knockoutalarm.audio.AlarmAudioEngine
import com.aistudio.knockoutalarm.model.Alarm
import com.aistudio.knockoutalarm.ui.components.CameraXPreview
import com.aistudio.knockoutalarm.ui.components.CountdownOverlay
import com.aistudio.knockoutalarm.ui.components.FightHUD
import com.aistudio.knockoutalarm.ui.components.SkeletonOverlay
import com.aistudio.knockoutalarm.ui.components.SparringSimulatorBar
import com.aistudio.knockoutalarm.vision.PoseFrame
import com.aistudio.knockoutalarm.vision.PoseQuality
import com.aistudio.knockoutalarm.vision.PunchArm
import com.aistudio.knockoutalarm.vision.PunchDetectionConfig
import com.aistudio.knockoutalarm.vision.PunchDetector
import com.aistudio.knockoutalarm.vision.PunchState
import com.aistudio.knockoutalarm.vision.SyntheticPoseGenerator
import com.example.ui.theme.ArenaBackground
import com.example.ui.theme.BoxingRed
import com.example.ui.theme.ChampionshipGold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FightArenaScreen(
    alarm: Alarm,
    onFightCompleted: (
        targetPunches: Int,
        completedPunches: Int,
        leftPunches: Int,
        rightPunches: Int,
        durationMs: Long,
        averageVelocity: Float,
        averageCadence: Float,
        emergencyStopped: Boolean
    ) -> Unit,
    onEmergencyStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Standalone fallback audio engine if service is not active
    val fallbackAudioEngine = remember { AlarmAudioEngine(context) }

    DisposableEffect(Unit) {
        if (AlarmRingingService.instance == null) {
            fallbackAudioEngine.startAlarm()
        }
        onDispose {
            fallbackAudioEngine.release()
        }
    }

    // Camera permission state
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Vision detector configured for selected difficulty
    val detector = remember(alarm.difficulty) {
        PunchDetector(PunchDetectionConfig.forDifficulty(alarm.difficulty))
    }

    // Fight challenge state
    val targetPunches = alarm.punchTarget
    var completedPunches by remember { mutableIntStateOf(0) }
    var leftPunches by remember { mutableIntStateOf(0) }
    var rightPunches by remember { mutableIntStateOf(0) }
    var recentPunchArm by remember { mutableStateOf<PunchArm?>(null) }
    var punchPulseTrigger by remember { mutableLongStateOf(0L) }
    var lastVelocity by remember { mutableFloatStateOf(0f) }
    var currentPoseQuality by remember { mutableStateOf(PoseQuality.GOOD) }
    var leftArmState by remember { mutableStateOf(PunchState.GUARD) }
    var rightArmState by remember { mutableStateOf(PunchState.GUARD) }
    var currentPoseFrame by remember { mutableStateOf<PoseFrame?>(null) }

    // Pre-fight countdown state
    var isFightActive by remember { mutableStateOf(false) }
    var countdownValue by remember { mutableIntStateOf(3) }
    var isUserPositioned by remember { mutableStateOf(false) }

    val fightStartTimeMs = remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Vibrator
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun triggerHapticImpact() {
        if (!alarm.vibrate) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(50L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(50L)
            }
        } catch (e: Exception) {
            // Safe ignore
        }
    }

    fun registerPunchHit(arm: PunchArm, velocity: Float) {
        if (!isFightActive) return

        completedPunches++
        if (arm == PunchArm.LEFT) leftPunches++ else rightPunches++
        recentPunchArm = arm
        punchPulseTrigger = System.currentTimeMillis()
        lastVelocity = velocity

        triggerHapticImpact()

        // Audio feedback: punch impact and siren ducking
        val service = AlarmRingingService.instance
        if (service != null) {
            service.onPunchLanded()
        } else {
            fallbackAudioEngine.onPunchLanded()
        }

        // Announcer vocal cues
        if (alarm.announcerEnabled) {
            val remaining = targetPunches - completedPunches
            val cue = when (remaining) {
                20 -> "Twenty left!"
                10 -> "Ten more punches!"
                5 -> "Five more!"
                1 -> "Final punch!"
                else -> null
            }
            if (cue != null) {
                if (service != null) service.enqueueAnnouncerCue(cue)
                else fallbackAudioEngine.enqueueAnnouncerCue(cue)
            }
        }

        // Check victory condition
        if (completedPunches >= targetPunches) {
            val totalDurationMs = System.currentTimeMillis() - fightStartTimeMs.longValue
            val totalSeconds = (totalDurationMs / 1000f).coerceAtLeast(1f)
            val cadence = completedPunches / totalSeconds

            // Stop alarm & play knockout bell
            if (service != null) {
                service.playKnockoutBell()
                service.stopAlarmAndService()
            } else {
                fallbackAudioEngine.playKnockoutBell()
                fallbackAudioEngine.stopAlarm()
            }

            onFightCompleted(
                targetPunches,
                completedPunches,
                leftPunches,
                rightPunches,
                totalDurationMs,
                lastVelocity,
                cadence,
                false
            )
        }
    }

    // Callback for incoming pose frames (from CameraX or simulator)
    val handlePoseFrame: (PoseFrame) -> Unit = { rawFrame ->
        currentPoseFrame = rawFrame
        val (event, telemetry) = detector.processFrame(rawFrame)

        currentPoseQuality = telemetry.poseQuality
        leftArmState = telemetry.leftArmState
        rightArmState = telemetry.rightArmState
        if (telemetry.leftWristVelocity > 0.1f) lastVelocity = telemetry.leftWristVelocity
        if (telemetry.rightWristVelocity > 0.1f) lastVelocity = telemetry.rightWristVelocity

        // Ready detector
        if (!isUserPositioned && telemetry.poseQuality == PoseQuality.GOOD) {
            isUserPositioned = true
        }

        // Only count punches if fight has officially begun
        if (isFightActive && event != null) {
            registerPunchHit(event.arm, event.peakVelocity)
        }
    }

    // Pre-fight countdown sequence once user is positioned
    LaunchedEffect(isUserPositioned) {
        if (isUserPositioned && !isFightActive) {
            delay(600L)
            countdownValue = 3
            delay(800L)
            countdownValue = 2
            delay(800L)
            countdownValue = 1
            delay(800L)
            countdownValue = 0 // FIGHT!
            delay(500L)
            isFightActive = true
            fightStartTimeMs.longValue = System.currentTimeMillis()
        }
    }

    // Emulator / Sparring simulator triggers
    fun runSyntheticFrames(frames: List<PoseFrame>) {
        coroutineScope.launch {
            for (f in frames) {
                handlePoseFrame(f)
                delay(30L)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ArenaBackground)
    ) {
        if (hasCameraPermission) {
            // Live CameraX Preview
            CameraXPreview(
                onPoseFrame = handlePoseFrame,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Permission Fallback UI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(100.dp))
                Text(
                    text = "CAMERA ACCESS REQUIRED",
                    color = BoxingRed,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "KnockOut Alarm requires front camera access to track your boxing punches on-device. No images are saved or uploaded.",
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = BoxingRed)
                ) {
                    Text("GRANT CAMERA PERMISSION", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Skeleton Bone Overlay
        SkeletonOverlay(
            poseFrame = currentPoseFrame,
            leftArmState = leftArmState,
            rightArmState = rightArmState,
            modifier = Modifier.fillMaxSize()
        )

        // Fight HUD (Punch Counter, Progress, Emergency Stop, Safety Warning)
        FightHUD(
            completedPunches = completedPunches,
            targetPunches = targetPunches,
            recentPunchArm = recentPunchArm,
            punchPulseTrigger = punchPulseTrigger,
            lastVelocity = lastVelocity,
            poseQuality = currentPoseQuality,
            onEmergencyStop = {
                val totalDurationMs = System.currentTimeMillis() - fightStartTimeMs.longValue
                val totalSeconds = (totalDurationMs / 1000f).coerceAtLeast(1f)
                val cadence = completedPunches / totalSeconds

                AlarmRingingService.instance?.stopAlarmAndService()
                fallbackAudioEngine.stopAlarm()

                onFightCompleted(
                    targetPunches,
                    completedPunches,
                    leftPunches,
                    rightPunches,
                    totalDurationMs,
                    lastVelocity,
                    cadence,
                    true
                )
                onEmergencyStop()
            },
            practiceFocus = remember { com.aistudio.knockoutalarm.domain.PracticeFocus.getFocusForDate() },
            modifier = Modifier.fillMaxSize()
        )

        // Pre-Fight Countdown Overlay (shown before fight begins)
        if (!isFightActive) {
            CountdownOverlay(
                isReady = isUserPositioned,
                countdownValue = countdownValue,
                poseQualityPrompt = when (currentPoseQuality) {
                    PoseQuality.GOOD -> "READY! STAY IN FRAME"
                    PoseQuality.BODY_NOT_VISIBLE -> "STEP IN FRONT OF CAMERA"
                    PoseQuality.TOO_CLOSE -> "STEP BACK FROM CAMERA"
                    PoseQuality.TOO_FAR -> "STEP CLOSER TO CAMERA"
                    PoseQuality.TORSO_UNSTABLE -> "HOLD DEVICE STEADY"
                    PoseQuality.LOW_CONFIDENCE -> "ADJUST LIGHTING / ANGLE"
                    PoseQuality.HANDS_NOT_GUARDED -> "RAISE HANDS TO CHIN"
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Sparring Simulator Tools (Floating tool bar for live emulator & unit testing)
        SparringSimulatorBar(
            onTriggerLeftJab = {
                if (!isFightActive) {
                    isUserPositioned = true
                    isFightActive = true
                }
                val now = System.currentTimeMillis()
                val seq = SyntheticPoseGenerator.generatePunchSequence(PunchArm.LEFT, now, 240L, 170f, 12)
                runSyntheticFrames(seq)
            },
            onTriggerRightCross = {
                if (!isFightActive) {
                    isUserPositioned = true
                    isFightActive = true
                }
                val now = System.currentTimeMillis()
                val seq = SyntheticPoseGenerator.generatePunchSequence(PunchArm.RIGHT, now, 240L, 170f, 12)
                runSyntheticFrames(seq)
            },
            onTriggerRapidCombo = {
                if (!isFightActive) {
                    isUserPositioned = true
                    isFightActive = true
                }
                coroutineScope.launch {
                    val now1 = System.currentTimeMillis()
                    val left1 = SyntheticPoseGenerator.generatePunchSequence(PunchArm.LEFT, now1, 200L, 170f, 10)
                    for (f in left1) { handlePoseFrame(f); delay(20L) }
                    delay(120L)
                    val now2 = System.currentTimeMillis()
                    val right1 = SyntheticPoseGenerator.generatePunchSequence(PunchArm.RIGHT, now2, 200L, 170f, 10)
                    for (f in right1) { handlePoseFrame(f); delay(20L) }
                    delay(120L)
                    val now3 = System.currentTimeMillis()
                    val left2 = SyntheticPoseGenerator.generatePunchSequence(PunchArm.LEFT, now3, 200L, 170f, 10)
                    for (f in left2) { handlePoseFrame(f); delay(20L) }
                }
            },
            onTriggerHoldExtended = {
                val now = System.currentTimeMillis()
                val seq = SyntheticPoseGenerator.generateHeldExtendedSequence(PunchArm.LEFT, now)
                runSyntheticFrames(seq)
            },
            onTriggerPhoneShake = {
                val now = System.currentTimeMillis()
                val seq = SyntheticPoseGenerator.generatePhoneShakeSequence(now)
                runSyntheticFrames(seq)
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 96.dp)
        )
    }
}
