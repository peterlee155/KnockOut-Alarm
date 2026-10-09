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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.knockoutalarm.model.Alarm
import com.aistudio.knockoutalarm.model.Difficulty
import com.aistudio.knockoutalarm.model.SubscriptionTier
import com.example.ui.theme.ArenaBackground
import com.example.ui.theme.ArenaSurface
import com.example.ui.theme.ArenaSurfaceVariant
import com.example.ui.theme.BoxingMuted
import com.example.ui.theme.BoxingRed
import com.example.ui.theme.BoxingWhite
import com.example.ui.theme.ChampionshipGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditorScreen(
    initialAlarm: Alarm?,
    subscriptionTier: SubscriptionTier,
    onSave: (Alarm) -> Unit,
    onDelete: (Alarm) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alarmToEdit = initialAlarm ?: Alarm()

    var hour by remember { mutableIntStateOf(alarmToEdit.hour) }
    var minute by remember { mutableIntStateOf(alarmToEdit.minute) }
    var repeatDays by remember { mutableStateOf(alarmToEdit.repeatDays) }
    var punchTarget by remember { mutableIntStateOf(alarmToEdit.punchTarget) }
    var difficulty by remember { mutableStateOf(alarmToEdit.difficulty) }
    var soundId by remember { mutableStateOf(alarmToEdit.soundId) }
    var vibrate by remember { mutableStateOf(alarmToEdit.vibrate) }
    var announcerEnabled by remember { mutableStateOf(alarmToEdit.announcerEnabled) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ArenaBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (initialAlarm == null) "NEW BOUT ALARM" else "EDIT BOUT ALARM",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // TIME DIAL SELECTOR
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ArenaSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "WAKE UP TIME",
                        color = BoxingMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Hour Control
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(onClick = { hour = (hour + 1) % 24 }) {
                                Icon(Icons.Default.Add, contentDescription = "Add Hour", tint = ChampionshipGold)
                            }
                            Text(
                                text = hour.toString().padStart(2, '0'),
                                color = BoxingWhite,
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Black
                            )
                            IconButton(onClick = { hour = if (hour == 0) 23 else hour - 1 }) {
                                Icon(Icons.Default.Remove, contentDescription = "Subtract Hour", tint = ChampionshipGold)
                            }
                        }

                        Text(
                            text = ":",
                            color = BoxingRed,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        // Minute Control
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(onClick = { minute = (minute + 5) % 60 }) {
                                Icon(Icons.Default.Add, contentDescription = "Add Minute", tint = ChampionshipGold)
                            }
                            Text(
                                text = minute.toString().padStart(2, '0'),
                                color = BoxingWhite,
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Black
                            )
                            IconButton(onClick = { minute = if (minute < 5) 55 else minute - 5 }) {
                                Icon(Icons.Default.Remove, contentDescription = "Subtract Minute", tint = ChampionshipGold)
                            }
                        }
                    }
                }
            }

            // REPEAT DAYS SELECTOR
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ArenaSurface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "REPEAT DAYS",
                        color = BoxingMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (dayIdx in 1..7) {
                            val isSelected = repeatDays.contains(dayIdx)
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) BoxingRed else ArenaSurfaceVariant)
                                    .border(
                                        1.dp,
                                        if (isSelected) BoxingRed else Color.White.copy(alpha = 0.1f),
                                        CircleShape
                                    )
                                    .clickable {
                                        repeatDays = if (isSelected) {
                                            repeatDays - dayIdx
                                        } else {
                                            repeatDays + dayIdx
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayLabels[dayIdx - 1],
                                    color = if (isSelected) Color.White else BoxingMuted,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // PUNCH TARGET SELECTOR
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
                        Text(
                            text = "PUNCH TARGET",
                            color = BoxingMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        if (subscriptionTier == SubscriptionTier.FREE) {
                            Text(
                                text = "PRO for 50/100",
                                color = ChampionshipGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val targets = listOf(10, 30, 50, 100)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        targets.forEach { target ->
                            val isSelected = punchTarget == target
                            val isLocked = subscriptionTier == SubscriptionTier.FREE && target > 30

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) ChampionshipGold
                                        else if (isLocked) ArenaSurfaceVariant.copy(alpha = 0.5f)
                                        else ArenaSurfaceVariant
                                    )
                                    .clickable(enabled = !isLocked) { punchTarget = target },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isLocked) "$target 🔒" else "$target",
                                    color = if (isSelected) Color.Black else if (isLocked) BoxingMuted else BoxingWhite,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }

            // DIFFICULTY SELECTOR
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ArenaSurface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "DIFFICULTY LEVEL",
                        color = BoxingMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Difficulty.values().forEach { diff ->
                            val isSelected = difficulty == diff
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) BoxingRed else ArenaSurfaceVariant)
                                    .clickable { difficulty = diff },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = diff.displayName.uppercase(),
                                    color = if (isSelected) Color.White else BoxingMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = difficulty.description,
                        color = BoxingMuted,
                        fontSize = 12.sp
                    )
                }
            }

            // AUDIO & VIBRATION TOGGLES
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
                        Column {
                            Text(text = "FIGHT ANNOUNCER", color = BoxingWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = "Vocal countdown & cues during bout", color = BoxingMuted, fontSize = 11.sp)
                        }
                        Switch(
                            checked = announcerEnabled,
                            onCheckedChange = { announcerEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BoxingRed)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "HAPTIC IMPACT", color = BoxingWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = "Punch impact vibration feedback", color = BoxingMuted, fontSize = 11.sp)
                        }
                        Switch(
                            checked = vibrate,
                            onCheckedChange = { vibrate = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BoxingRed)
                        )
                    }
                }
            }

            // ACTION BUTTONS: SAVE & DELETE
            Button(
                onClick = {
                    val newAlarm = alarmToEdit.copy(
                        hour = hour,
                        minute = minute,
                        repeatDays = repeatDays,
                        punchTarget = punchTarget,
                        difficulty = difficulty,
                        soundId = soundId,
                        vibrate = vibrate,
                        announcerEnabled = announcerEnabled,
                        enabled = true
                    )
                    onSave(newAlarm)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_alarm_button"),
                colors = ButtonDefaults.buttonColors(containerColor = BoxingRed),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "SAVE ALARM",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }

            if (initialAlarm != null) {
                OutlinedButton(
                    onClick = { onDelete(alarmToEdit) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BoxingRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "DELETE ALARM",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
