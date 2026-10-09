package com.aistudio.knockoutalarm.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BoxingRed
import kotlinx.coroutines.delay

@Composable
fun EmergencyStopButton(
    onEmergencyStopConfirmed: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    var secondsRemaining by remember { mutableIntStateOf(15) }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            secondsRemaining = 15
            while (secondsRemaining > 0 && isPressed) {
                delay(1000L)
                if (isPressed) {
                    secondsRemaining--
                }
            }
            if (isPressed && secondsRemaining == 0) {
                onEmergencyStopConfirmed()
            }
        } else {
            secondsRemaining = 15
        }
    }

    val progress = (15 - secondsRemaining) / 15f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "holdProgress")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isPressed) BoxingRed.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.6f))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            }
            .testTag("emergency_stop_button"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            if (isPressed) {
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    color = BoxingRed,
                    trackColor = Color.White.copy(alpha = 0.2f),
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "HOLD $secondsRemaining S TO ABORT",
                    color = BoxingRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = 1.sp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Emergency Abort",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "HOLD 15S FOR EMERGENCY STOP",
                    color = Color.White.copy(alpha = 0.85f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
