package com.aistudio.knockoutalarm.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.knockoutalarm.vision.PoseQuality
import com.aistudio.knockoutalarm.vision.PunchArm
import com.example.ui.theme.BoxingGreen
import com.example.ui.theme.BoxingRed
import com.example.ui.theme.ChampionshipGold

import com.aistudio.knockoutalarm.domain.PracticeFocus

@Composable
fun FightHUD(
    completedPunches: Int,
    targetPunches: Int,
    recentPunchArm: PunchArm?,
    punchPulseTrigger: Long,
    lastVelocity: Float,
    poseQuality: PoseQuality,
    onEmergencyStop: () -> Unit,
    practiceFocus: PracticeFocus? = null,
    modifier: Modifier = Modifier
) {
    val countScale = remember { Animatable(1f) }

    LaunchedEffect(punchPulseTrigger) {
        if (punchPulseTrigger > 0) {
            countScale.animateTo(1.35f, tween(80, easing = FastOutSlowInEasing))
            countScale.animateTo(1.0f, tween(180, easing = FastOutSlowInEasing))
        }
    }

    val progress = (completedPunches.toFloat() / targetPunches.toFloat()).coerceIn(0f, 1f)

    Box(modifier = modifier.fillMaxSize()) {

        // TOP BROADCAST BANNER
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Knockout Brand Tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(BoxingRed)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "KNOCKOUT ARENA",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                // Round Tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .border(1.dp, ChampionshipGold.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "ROUND 1",
                        color = ChampionshipGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = ChampionshipGold,
                trackColor = Color.White.copy(alpha = 0.15f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Camera status pill
            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val (statusText, statusColor) = when (poseQuality) {
                    PoseQuality.GOOD -> "READY • TRACKING" to BoxingGreen
                    PoseQuality.BODY_NOT_VISIBLE -> "SHOW UPPER BODY" to BoxingRed
                    PoseQuality.TOO_CLOSE -> "STEP BACK" to ChampionshipGold
                    PoseQuality.TOO_FAR -> "STEP CLOSER" to ChampionshipGold
                    PoseQuality.TORSO_UNSTABLE -> "KEEP PHONE STEADY" to BoxingRed
                    PoseQuality.LOW_CONFIDENCE -> "CAMERA LOST YOU" to BoxingRed
                    PoseQuality.HANDS_NOT_GUARDED -> "RAISE HANDS" to ChampionshipGold
                }

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }

            if (practiceFocus != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(1.dp, ChampionshipGold.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FOCUS: ${practiceFocus.title.uppercase()} • ${practiceFocus.prompt}",
                        color = ChampionshipGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.4.sp,
                        maxLines = 1
                    )
                }
            }
        }

        // CENTER HUD: PUNCH COUNTER
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Punch Arm indicator banner (LEFT / RIGHT)
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .padding(bottom = 4.dp)
            ) {
                if (recentPunchArm != null && punchPulseTrigger > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(BoxingRed, ChampionshipGold)
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (recentPunchArm == PunchArm.LEFT) "LEFT JAB 🥊" else "RIGHT CROSS 🥊",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // Big Numbers
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.scale(countScale.value)
            ) {
                Text(
                    text = "$completedPunches",
                    color = ChampionshipGold,
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 80.sp
                )
                Text(
                    text = " / $targetPunches",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Text(
                text = "PUNCHES TO ESCAPE",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }

        // BOTTOM HUD: VELOCITY GAUGE, SAFETY WARNING, EMERGENCY STOP
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Velocity telemetry pill
            Row(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FlashOn,
                    contentDescription = "Velocity",
                    tint = ChampionshipGold,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Velocity: %.2f units/s".format(lastVelocity),
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Safety Warning
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .border(1.dp, BoxingRed.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "⚠️ PUNCH TOWARD CAMERA. DO NOT HIT THE DEVICE.",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 15-Second Hold Emergency Stop Button
            EmergencyStopButton(
                onEmergencyStopConfirmed = onEmergencyStop
            )
        }
    }
}
