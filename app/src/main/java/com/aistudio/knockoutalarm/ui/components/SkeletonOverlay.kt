package com.aistudio.knockoutalarm.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import com.aistudio.knockoutalarm.vision.PoseFrame
import com.aistudio.knockoutalarm.vision.PoseLandmarkType
import com.aistudio.knockoutalarm.vision.PunchArm
import com.aistudio.knockoutalarm.vision.PunchState
import com.example.ui.theme.BoxingGreen
import com.example.ui.theme.BoxingRed
import com.example.ui.theme.ChampionshipGold

@Composable
fun SkeletonOverlay(
    poseFrame: PoseFrame?,
    leftArmState: PunchState,
    rightArmState: PunchState,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        if (poseFrame == null || poseFrame.landmarks.isEmpty()) return@Canvas

        val w = size.width
        val h = size.height

        fun pt(type: PoseLandmarkType): Offset? {
            val point = poseFrame.get(type) ?: return null
            if (point.confidence < 0.35f) return null
            // Mirror X for front camera selfie orientation
            val mirroredX = (1f - point.x) * w
            val y = point.y * h
            return Offset(mirroredX, y)
        }

        val nose = pt(PoseLandmarkType.NOSE)
        val ls = pt(PoseLandmarkType.LEFT_SHOULDER)
        val rs = pt(PoseLandmarkType.RIGHT_SHOULDER)
        val le = pt(PoseLandmarkType.LEFT_ELBOW)
        val re = pt(PoseLandmarkType.RIGHT_ELBOW)
        val lw = pt(PoseLandmarkType.LEFT_WRIST)
        val rw = pt(PoseLandmarkType.RIGHT_WRIST)
        val lh = pt(PoseLandmarkType.LEFT_HIP)
        val rh = pt(PoseLandmarkType.RIGHT_HIP)

        val baseBoneColor = Color.White.copy(alpha = 0.5f)
        val boneStrokeWidth = 8f

        fun drawBone(p1: Offset?, p2: Offset?, color: Color = baseBoneColor) {
            if (p1 != null && p2 != null) {
                drawLine(
                    color = color,
                    start = p1,
                    end = p2,
                    strokeWidth = boneStrokeWidth,
                    cap = StrokeCap.Round
                )
            }
        }

        // Color bones by arm strike state
        val leftArmColor = when (leftArmState) {
            PunchState.PEAK -> ChampionshipGold
            PunchState.EXTENDING, PunchState.RETRACTING -> BoxingRed
            PunchState.PUNCH_REGISTERED -> ChampionshipGold
            else -> BoxingGreen.copy(alpha = 0.8f)
        }

        val rightArmColor = when (rightArmState) {
            PunchState.PEAK -> ChampionshipGold
            PunchState.EXTENDING, PunchState.RETRACTING -> BoxingRed
            PunchState.PUNCH_REGISTERED -> ChampionshipGold
            else -> BoxingGreen.copy(alpha = 0.8f)
        }

        // Torso & Head bones
        drawBone(ls, rs, baseBoneColor)
        drawBone(ls, lh, baseBoneColor.copy(alpha = 0.4f))
        drawBone(rs, rh, baseBoneColor.copy(alpha = 0.4f))
        drawBone(lh, rh, baseBoneColor.copy(alpha = 0.4f))
        if (ls != null && rs != null && nose != null) {
            val neck = Offset((ls.x + rs.x) / 2f, (ls.y + rs.y) / 2f)
            drawBone(nose, neck, baseBoneColor)
        }

        // Left Arm bones
        drawBone(ls, le, leftArmColor)
        drawBone(le, lw, leftArmColor)

        // Right Arm bones
        drawBone(rs, re, rightArmColor)
        drawBone(re, rw, rightArmColor)

        // Joint points
        listOfNotNull(nose, ls, rs, lh, rh).forEach {
            drawCircle(color = Color.White, radius = 7f, center = it)
        }
        le?.let { drawCircle(color = leftArmColor, radius = 9f, center = it) }
        lw?.let { drawCircle(color = leftArmColor, radius = 12f, center = it) }
        re?.let { drawCircle(color = rightArmColor, radius = 9f, center = it) }
        rw?.let { drawCircle(color = rightArmColor, radius = 12f, center = it) }
    }
}
