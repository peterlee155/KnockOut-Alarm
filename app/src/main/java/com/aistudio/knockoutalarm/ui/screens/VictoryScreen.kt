package com.aistudio.knockoutalarm.ui.screens

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.knockoutalarm.model.AlarmSession
import com.aistudio.knockoutalarm.model.UserStats
import com.example.ui.theme.ArenaBackground
import com.example.ui.theme.ArenaSurface
import com.example.ui.theme.BoxingMuted
import com.example.ui.theme.BoxingRed
import com.example.ui.theme.BoxingWhite
import com.example.ui.theme.ChampionshipGold

@Composable
fun VictoryScreen(
    session: AlarmSession?,
    stats: UserStats,
    onDone: () -> Unit,
    onViewStats: () -> Unit,
    onOpenMorningProgress: () -> Unit = onDone,
    modifier: Modifier = Modifier
) {
    val totalPunches = session?.completedPunches ?: 30
    val durationText = session?.formattedDuration ?: "00:38"
    val cadenceText = "%.2f /s".format(session?.averageCadence ?: 0.79f)
    val leftPunches = session?.leftPunches ?: 15
    val rightPunches = session?.rightPunches ?: 15
    val wasAborted = session?.emergencyStopped == true

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ArenaBackground)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Knockout Glove Badge
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(if (wasAborted) BoxingMuted.copy(alpha = 0.2f) else BoxingRed.copy(alpha = 0.2f))
                    .border(
                        2.dp,
                        if (wasAborted) BoxingMuted else BoxingRed,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (wasAborted) "🛑" else "🥊",
                    fontSize = 44.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = if (wasAborted) "EMERGENCY ABORT" else "KNOCKOUT!",
                color = if (wasAborted) ChampionshipGold else BoxingRed,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (wasAborted) "Alarm silenced via emergency bypass" else "Win your first round before breakfast.",
                color = BoxingWhite,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // STATS GRID CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ArenaSurface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatTile(
                            icon = Icons.Default.FitnessCenter,
                            title = "PUNCHES",
                            value = "$totalPunches",
                            tint = ChampionshipGold
                        )
                        StatTile(
                            icon = Icons.Default.Timer,
                            title = "DURATION",
                            value = durationText,
                            tint = BoxingWhite
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatTile(
                            icon = Icons.Default.Speed,
                            title = "CADENCE",
                            value = cadenceText,
                            tint = BoxingWhite
                        )
                        StatTile(
                            icon = Icons.Default.LocalFireDepartment,
                            title = "STREAK",
                            value = "${stats.currentStreak} DAYS",
                            tint = BoxingRed
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Left vs Right Distribution
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Left Jabs: $leftPunches",
                            color = BoxingMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "  •  ",
                            color = BoxingMuted,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Right Crosses: $rightPunches",
                            color = BoxingMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Action Buttons
            Button(
                onClick = onOpenMorningProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("victory_open_progress_button"),
                colors = ButtonDefaults.buttonColors(containerColor = BoxingRed),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "OPTIONAL 3-MIN ROUND & PROGRESS 🥊",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("victory_done_button"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = BoxingWhite),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "FINISH TO HOME",
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onViewStats,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ChampionshipGold),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "VIEW FIGHT HISTORY",
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun StatTile(
    icon: ImageVector,
    title: String,
    value: String,
    tint: Color
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                color = BoxingMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = BoxingWhite,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black
        )
    }
}
