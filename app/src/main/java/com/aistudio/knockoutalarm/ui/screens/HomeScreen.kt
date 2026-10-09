package com.aistudio.knockoutalarm.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.knockoutalarm.model.Alarm
import com.aistudio.knockoutalarm.model.AlarmSession
import com.aistudio.knockoutalarm.model.SubscriptionTier
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import com.aistudio.knockoutalarm.ui.components.TestAlarmBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    alarms: List<Alarm>,
    stats: UserStats,
    sessions: List<AlarmSession>,
    subscriptionTier: SubscriptionTier,
    countdownSeconds: Int? = null,
    onToggleAlarm: (Alarm) -> Unit,
    onEditAlarm: (Alarm) -> Unit,
    onDeleteAlarm: (Alarm) -> Unit,
    onAddNewAlarm: () -> Unit,
    onLaunchFightArena: (Alarm) -> Unit,
    onViewStats: () -> Unit,
    onOpenMorningProgress: () -> Unit = onViewStats,
    onTriggerInstantAlarm: (punchTarget: Int) -> Unit = {},
    onTriggerDelayedAlarm: (delaySeconds: Int, punchTarget: Int) -> Unit = { _, _ -> },
    onCancelDelayedAlarm: () -> Unit = {},
    onToggleDevTier: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTimeStr by remember { mutableStateOf("") }
    var isTestSheetOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        while (true) {
            currentTimeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            delay(1000L)
        }
    }

    val nextAlarm = alarms.firstOrNull { it.enabled } ?: alarms.firstOrNull()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ArenaBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddNewAlarm,
                containerColor = BoxingRed,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_alarm_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Alarm")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                // Top Header with Digital Clock & Subscription Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "KNOCKOUT ALARM",
                            color = BoxingRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = currentTimeStr,
                            color = BoxingWhite,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Test & Debug Alarm Trigger Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(BoxingRed.copy(alpha = 0.2f))
                                .border(
                                    1.dp,
                                    BoxingRed,
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable { isTestSheetOpen = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("open_test_alarm_sheet_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Science,
                                    contentDescription = "Test Alarm",
                                    tint = BoxingRed,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (countdownSeconds != null) "TEST ($countdownSeconds s)" else "TEST ALARM",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Subscription Badge / Dev Toggle
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (subscriptionTier == SubscriptionTier.PRO)
                                        ChampionshipGold.copy(alpha = 0.2f)
                                    else ArenaSurfaceVariant
                                )
                                .border(
                                    1.dp,
                                    if (subscriptionTier == SubscriptionTier.PRO) ChampionshipGold else Color.White.copy(alpha = 0.2f),
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable(onClick = onToggleDevTier)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("subscription_tier_chip")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (subscriptionTier == SubscriptionTier.PRO) ChampionshipGold else BoxingMuted,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (subscriptionTier == SubscriptionTier.PRO) "PRO" else "FREE",
                                    color = if (subscriptionTier == SubscriptionTier.PRO) ChampionshipGold else BoxingWhite,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Next Alarm Hero Card
            item {
                if (nextAlarm != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, BoxingRed.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
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
                                        imageVector = Icons.Default.Alarm,
                                        contentDescription = null,
                                        tint = BoxingRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "NEXT BOUT",
                                        color = BoxingMuted,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }

                                Switch(
                                    checked = nextAlarm.enabled,
                                    onCheckedChange = { onToggleAlarm(nextAlarm) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = BoxingRed
                                    ),
                                    modifier = Modifier.testTag("next_alarm_switch")
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    text = nextAlarm.formattedTime,
                                    color = BoxingWhite,
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.Black
                                )

                                Column(horizontalAlignment = Alignment.End) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(ChampionshipGold.copy(alpha = 0.2f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${nextAlarm.punchTarget} PUNCHES",
                                            color = ChampionshipGold,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = nextAlarm.repeatDaysSummary,
                                        color = BoxingMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Test Fight Arena Action Button
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(BoxingRed, ChampionshipGold)
                                        )
                                    )
                                    .clickable { onLaunchFightArena(nextAlarm) }
                                    .testTag("launch_test_arena_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "SPARRING / TEST FIGHT ARENA",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Streak & Morning Progress Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenMorningProgress)
                        .testTag("morning_progress_card"),
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
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(BoxingRed.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalFireDepartment,
                                        contentDescription = "Streak",
                                        tint = BoxingRed,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "${stats.currentStreak} DAY STREAK",
                                        color = BoxingWhite,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = "${stats.totalPunches} total wake-up punches",
                                        color = BoxingMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "BEST",
                                    color = BoxingMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${stats.longestStreak} DAYS",
                                    color = ChampionshipGold,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(ArenaSurfaceVariant)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🥊 Win your first round before breakfast",
                                    color = ChampionshipGold,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Ranks & 3-Min Round ➔",
                                    color = BoxingWhite,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Alarms List Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SCHEDULED ALARMS",
                        color = BoxingWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${alarms.size} ALARM(S)",
                        color = BoxingMuted,
                        fontSize = 12.sp
                    )
                }
            }

            // Alarm Cards
            items(alarms) { alarm ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditAlarm(alarm) }
                        .testTag("alarm_card_${alarm.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ArenaSurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = alarm.formattedTime,
                                color = if (alarm.enabled) BoxingWhite else BoxingMuted,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(ArenaSurfaceVariant)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${alarm.punchTarget} PUNCHES",
                                        color = ChampionshipGold,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = alarm.repeatDaysSummary,
                                    color = BoxingMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onDeleteAlarm(alarm) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Alarm",
                                    tint = BoxingMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Switch(
                                checked = alarm.enabled,
                                onCheckedChange = { onToggleAlarm(alarm) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = BoxingRed
                                )
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }

        if (isTestSheetOpen) {
            TestAlarmBottomSheet(
                sheetState = sheetState,
                countdownSeconds = countdownSeconds,
                onDismiss = { isTestSheetOpen = false },
                onTriggerInstantAlarm = { target ->
                    onTriggerInstantAlarm(target)
                },
                onTriggerDelayedAlarm = { delaySec, target ->
                    onTriggerDelayedAlarm(delaySec, target)
                },
                onCancelDelayedAlarm = onCancelDelayedAlarm
            )
        }
    }
}
