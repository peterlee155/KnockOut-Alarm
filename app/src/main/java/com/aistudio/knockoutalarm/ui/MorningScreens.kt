package com.aistudio.knockoutalarm.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aistudio.knockoutalarm.domain.MorningProgressLogic
import com.aistudio.knockoutalarm.domain.OptionalRoundState
import com.aistudio.knockoutalarm.domain.PracticeFocus
import com.aistudio.knockoutalarm.model.AlarmSession
import com.aistudio.knockoutalarm.model.UserStats
import com.example.ui.theme.ArenaBackground
import com.example.ui.theme.ArenaSurface
import com.example.ui.theme.ArenaSurfaceVariant
import com.example.ui.theme.BoxingGreen
import com.example.ui.theme.BoxingMuted
import com.example.ui.theme.BoxingRed
import com.example.ui.theme.BoxingWhite
import com.example.ui.theme.ChampionshipGold
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MorningProgressScreen(
    stats: UserStats,
    sessions: List<AlarmSession>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val xpInfo = remember(sessions) { MorningProgressLogic.calculateXpAndRank(sessions) }
    val sevenDaySummary = remember(sessions) { MorningProgressLogic.calculateSevenDaySummary(sessions) }
    val todayFocus = remember { PracticeFocus.getFocusForDate() }

    // Optional 3-Minute Round state (preserved across orientation changes)
    var isTimerRunning by rememberSaveable { mutableStateOf(false) }
    var totalRemainingSeconds by rememberSaveable { mutableIntStateOf(180) }

    // Lifecycle observer to pause when backgrounded
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                isTimerRunning = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            // Navigating away from Progress ends the optional round
            isTimerRunning = false
        }
    }

    LaunchedEffect(isTimerRunning) {
        while (isTimerRunning && totalRemainingSeconds > 0) {
            delay(1000L)
            if (isTimerRunning && totalRemainingSeconds > 0) {
                totalRemainingSeconds--
            }
        }
        if (totalRemainingSeconds == 0) {
            isTimerRunning = false
        }
    }

    val roundState = remember(totalRemainingSeconds, isTimerRunning) {
        OptionalRoundState(
            isRunning = isTimerRunning,
            totalRemainingSeconds = totalRemainingSeconds,
            isCompleted = totalRemainingSeconds == 0
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ArenaBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "MORNING PROGRESS",
                        color = BoxingWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = BoxingWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ArenaBackground)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // HERO POSITIONING BANNER
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ArenaSurface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(BoxingRed.copy(alpha = 0.35f), ChampionshipGold.copy(alpha = 0.2f))
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(BoxingRed)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "DAILY HABIT LOOP",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Win your first round before breakfast.",
                                color = BoxingWhite,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                lineHeight = 28.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Wake-up challenge  ➔  Optional movement  ➔  Visible progress  ➔  Return tomorrow.",
                                color = BoxingWhite.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // XP & FIGHTER RANK CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ArenaSurface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = xpInfo.rank.badgeEmoji, fontSize = 28.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = xpInfo.rank.displayName.uppercase(),
                                        color = ChampionshipGold,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = xpInfo.rank.description,
                                        color = BoxingMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${xpInfo.totalXp} XP",
                                    color = BoxingWhite,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "${xpInfo.distinctCompletedDays} days won",
                                    color = BoxingGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Rank progress bar
                        LinearProgressIndicator(
                            progress = { xpInfo.progressToNextRank },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = ChampionshipGold,
                            trackColor = ArenaSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Current: ${xpInfo.rank.displayName}",
                                color = BoxingMuted,
                                fontSize = 11.sp
                            )
                            Text(
                                text = if (xpInfo.nextRank != null)
                                    "Next: ${xpInfo.nextRank.displayName} (${xpInfo.nextRank.minXp} XP)"
                                else "Max Rank Achieved",
                                color = ChampionshipGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "ℹ️ 100 XP per distinct completed day. Repeated alarms cannot farm extra XP. Emergency stops grant 0 XP.",
                            color = BoxingMuted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // DAILY PRACTICE FOCUS CARD (Rotating 7 prompts)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ArenaSurface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    tint = BoxingRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "TODAY'S PRACTICE FOCUS",
                                    color = BoxingRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            }
                            Text(
                                text = "Focus #${todayFocus.dayNumber} of 7",
                                color = BoxingMuted,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = todayFocus.title,
                            color = BoxingWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = todayFocus.prompt,
                            color = BoxingWhite.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(ArenaSurfaceVariant)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "💡 Tip: ${todayFocus.tip}",
                                color = ChampionshipGold,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Note: Straight punches only (Jabs & Crosses). Hooks, slips, and rolls are not tracked.",
                            color = BoxingMuted,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // SEVEN-DAY LOCAL SUMMARY CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ArenaSurface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SEVEN-DAY SUMMARY",
                                color = BoxingWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Local Room History",
                                color = BoxingMuted,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "${sevenDaySummary.completedMorningsOutOfSeven} / 7",
                                    color = ChampionshipGold,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text("Mornings Won", color = BoxingMuted, fontSize = 11.sp)
                            }
                            Column {
                                Text(
                                    text = "${sevenDaySummary.confirmedPunches}",
                                    color = BoxingWhite,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text("Confirmed Punches", color = BoxingMuted, fontSize = 11.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                val min = sevenDaySummary.validatedDurationSeconds / 60
                                val sec = sevenDaySummary.validatedDurationSeconds % 60
                                Text(
                                    text = "${min}m ${sec}s",
                                    color = BoxingWhite,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text("Active Duration", color = BoxingMuted, fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = sevenDaySummary.encouragingMessage,
                            color = BoxingWhite.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // OPTIONAL THREE-MINUTE ROUND CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ArenaSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "OPTIONAL 3-MINUTE ROUND",
                                color = ChampionshipGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(ArenaSurfaceVariant)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "3x (30s Box / 30s Rest)",
                                    color = BoxingMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Active Phase Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (roundState.isShadowboxingPhase) BoxingRed else BoxingGreen.copy(alpha = 0.3f))
                                .border(
                                    1.dp,
                                    if (roundState.isShadowboxingPhase) BoxingRed else BoxingGreen,
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "ROUND ${roundState.currentCycle} • ${roundState.phaseLabel}",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Countdown Timer
                        Text(
                            text = "${roundState.phaseRemainingSeconds}s",
                            color = BoxingWhite,
                            fontSize = 54.sp,
                            fontWeight = FontWeight.Black
                        )

                        Text(
                            text = "Phase Remaining  •  Total: ${roundState.totalRemainingSeconds / 60}:${(roundState.totalRemainingSeconds % 60).toString().padStart(2, '0')}",
                            color = BoxingMuted,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Timer Controls
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { isTimerRunning = !isTimerRunning },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isTimerRunning) ArenaSurfaceVariant else BoxingRed
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("optional_round_toggle_button")
                            ) {
                                Icon(
                                    imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isTimerRunning) "PAUSE" else if (totalRemainingSeconds < 180) "RESUME" else "START ROUND",
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    isTimerRunning = false
                                    totalRemainingSeconds = 180
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = BoxingWhite)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("RESET", fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Optional shadowboxing to wake your body. Alarm is already stopped. Does not change punch counts, XP or streaks.",
                            color = BoxingMuted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
