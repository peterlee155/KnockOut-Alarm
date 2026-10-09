package com.aistudio.knockoutalarm.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ArenaSurface
import com.example.ui.theme.ArenaSurfaceVariant
import com.example.ui.theme.BoxingGreen
import com.example.ui.theme.BoxingMuted
import com.example.ui.theme.BoxingRed
import com.example.ui.theme.BoxingWhite
import com.example.ui.theme.ChampionshipGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestAlarmBottomSheet(
    sheetState: SheetState,
    countdownSeconds: Int?,
    onDismiss: () -> Unit,
    onTriggerInstantAlarm: (punchTarget: Int) -> Unit,
    onTriggerDelayedAlarm: (delaySeconds: Int, punchTarget: Int) -> Unit,
    onCancelDelayedAlarm: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPunchTarget by remember { mutableIntStateOf(5) } // quick 5 punches default for testing!

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ArenaSurface,
        dragHandle = null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(BoxingRed.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null,
                            tint = BoxingRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "ALARM SIMULATION & TEST",
                            color = BoxingWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Test the exact alarm wake-up & boxing challenge",
                            color = BoxingMuted,
                            fontSize = 12.sp
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("dismiss_test_sheet_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = BoxingMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Active Countdown Banner if scheduled
            if (countdownSeconds != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = BoxingRed.copy(alpha = 0.2f)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BoxingRed))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.HourglassTop,
                                contentDescription = null,
                                tint = ChampionshipGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ALARM FIRING IN $countdownSeconds SECONDS",
                                color = ChampionshipGold,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Lock your screen or background the app now to test real wake-up!",
                            color = BoxingWhite,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onCancelDelayedAlarm,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BoxingRed),
                            modifier = Modifier.testTag("cancel_delayed_test_button")
                        ) {
                            Text("CANCEL TEST ALARM", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            // Punch Target for Testing
            Text(
                text = "TEST PUNCH TARGET",
                color = BoxingMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(5, 10, 30).forEach { target ->
                    val isSelected = selectedPunchTarget == target
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) ChampionshipGold else ArenaSurfaceVariant)
                            .clickable { selectedPunchTarget = target }
                            .testTag("test_punch_target_$target"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$target Punches",
                            color = if (isSelected) Color.Black else BoxingWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Test Mode 1: Instant Trigger
            Button(
                onClick = {
                    onDismiss()
                    onTriggerInstantAlarm(selectedPunchTarget)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("instant_test_alarm_button"),
                colors = ButtonDefaults.buttonColors(containerColor = BoxingRed),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "TRIGGER NOW (INSTANT SIREN & BOXING)",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Test Mode 2: 5s / 10s Delay (Test lock screen & background)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onTriggerDelayedAlarm(5, selectedPunchTarget)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("delayed_5s_test_button"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ChampionshipGold),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.HourglassTop, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RING IN 5 SEC", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        onTriggerDelayedAlarm(10, selectedPunchTarget)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("delayed_10s_test_button"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ChampionshipGold),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.HourglassTop, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RING IN 10 SEC", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Explanation Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(ArenaSurfaceVariant)
                    .padding(12.dp)
            ) {
                Text(
                    text = "💡 'Ring in 5s/10s' schedules a real exact Android alarm. Once pressed, lock your phone or go to the home screen. The device will sound the siren and launch full-screen fight mode to test waking you up.",
                    color = BoxingMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
