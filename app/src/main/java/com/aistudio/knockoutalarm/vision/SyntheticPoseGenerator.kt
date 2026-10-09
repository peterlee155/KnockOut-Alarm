package com.aistudio.knockoutalarm.vision

/**
 * Generates synthetic landmark sequences for unit testing and simulator test sparring.
 */
object SyntheticPoseGenerator {

    /**
     * Base neutral guard stance.
     * User is centered with hands up near chin.
     */
    fun createGuardFrame(timestampMs: Long, confidence: Float = 0.95f): PoseFrame {
        val landmarks = mapOf(
            PoseLandmarkType.NOSE to Point3D(0.50f, 0.25f, 0f, confidence, timestampMs),
            PoseLandmarkType.LEFT_SHOULDER to Point3D(0.38f, 0.38f, 0f, confidence, timestampMs),
            PoseLandmarkType.RIGHT_SHOULDER to Point3D(0.62f, 0.38f, 0f, confidence, timestampMs),
            // Guard: elbows down, wrists up near chin
            PoseLandmarkType.LEFT_ELBOW to Point3D(0.38f, 0.52f, 0.05f, confidence, timestampMs),
            PoseLandmarkType.RIGHT_ELBOW to Point3D(0.62f, 0.52f, 0.05f, confidence, timestampMs),
            PoseLandmarkType.LEFT_WRIST to Point3D(0.38f, 0.38f, 0.10f, confidence, timestampMs),
            PoseLandmarkType.RIGHT_WRIST to Point3D(0.62f, 0.38f, 0.10f, confidence, timestampMs),
            PoseLandmarkType.LEFT_HIP to Point3D(0.42f, 0.75f, 0f, confidence, timestampMs),
            PoseLandmarkType.RIGHT_HIP to Point3D(0.58f, 0.75f, 0f, confidence, timestampMs)
        )
        return PoseFrame(landmarks, timestampMs)
    }

    /**
     * Generates a realistic sequence of frames for a boxing punch (Jab or Cross).
     * Follows: Guard -> Extension -> Peak strike (160-180° elbow) -> Retraction (<100° elbow) -> Guard
     */
    fun generatePunchSequence(
        arm: PunchArm,
        startMs: Long = 1000L,
        durationMs: Long = 320L,
        peakAngle: Float = 170f,
        frameCount: Int = 14
    ): List<PoseFrame> {
        val frames = mutableListOf<PoseFrame>()
        val dt = durationMs / frameCount
        val sx = if (arm == PunchArm.LEFT) 0.38f else 0.62f

        for (i in 0 until frameCount) {
            val t = i.toFloat() / (frameCount - 1) // 0.0 -> 1.0
            val time = startMs + (i * dt)

            // Bell curve stroke progress: 0.0 -> 1.0 (at midpoint) -> 0.0
            val strokeProgress = if (t <= 0.5f) {
                t * 2f
            } else {
                (1f - t) * 2f
            }

            // At strokeProgress = 0 (guard):
            // Elbow = (sx, 0.52), Wrist = (sx, 0.38)
            // At strokeProgress = 1 (peak extension):
            // Elbow = (sx, 0.50), Wrist = (sx, 0.66)
            // Vector Shoulder-Elbow = (0, -0.12), Vector Wrist-Elbow = (0, +0.16) -> 180° straight arm!
            val curElbowY = 0.52f - (0.02f * strokeProgress)
            val curWristY = 0.38f + (0.28f * strokeProgress)
            val curWristZ = -0.30f * strokeProgress

            val landmarks = mutableMapOf<PoseLandmarkType, Point3D>()
            landmarks[PoseLandmarkType.NOSE] = Point3D(0.50f, 0.25f, 0f, 0.95f, time)
            landmarks[PoseLandmarkType.LEFT_SHOULDER] = Point3D(0.38f, 0.38f, 0f, 0.95f, time)
            landmarks[PoseLandmarkType.RIGHT_SHOULDER] = Point3D(0.62f, 0.38f, 0f, 0.95f, time)
            landmarks[PoseLandmarkType.LEFT_HIP] = Point3D(0.42f, 0.75f, 0f, 0.95f, time)
            landmarks[PoseLandmarkType.RIGHT_HIP] = Point3D(0.58f, 0.75f, 0f, 0.95f, time)

            if (arm == PunchArm.LEFT) {
                landmarks[PoseLandmarkType.LEFT_ELBOW] = Point3D(sx, curElbowY, 0.02f, 0.95f, time)
                landmarks[PoseLandmarkType.LEFT_WRIST] = Point3D(sx, curWristY, curWristZ, 0.95f, time)
                // Right arm in guard
                landmarks[PoseLandmarkType.RIGHT_ELBOW] = Point3D(0.62f, 0.52f, 0.05f, 0.95f, time)
                landmarks[PoseLandmarkType.RIGHT_WRIST] = Point3D(0.62f, 0.38f, 0.10f, 0.95f, time)
            } else {
                landmarks[PoseLandmarkType.RIGHT_ELBOW] = Point3D(sx, curElbowY, 0.02f, 0.95f, time)
                landmarks[PoseLandmarkType.RIGHT_WRIST] = Point3D(sx, curWristY, curWristZ, 0.95f, time)
                // Left arm in guard
                landmarks[PoseLandmarkType.LEFT_ELBOW] = Point3D(0.38f, 0.52f, 0.05f, 0.95f, time)
                landmarks[PoseLandmarkType.LEFT_WRIST] = Point3D(0.38f, 0.38f, 0.10f, 0.95f, time)
            }

            frames.add(PoseFrame(landmarks, time))
        }

        return frames
    }

    /**
     * Generates a sequence where the user extends arm and holds it extended without retracting.
     */
    fun generateHeldExtendedSequence(arm: PunchArm, startMs: Long = 1000L): List<PoseFrame> {
        val extension = generatePunchSequence(arm, startMs, 200L, 170f, 7).take(5)
        val heldFrames = mutableListOf<PoseFrame>()
        val peakFrame = extension.last()
        for (i in 1..10) {
            val t = startMs + 200L + (i * 100L)
            heldFrames.add(PoseFrame(peakFrame.landmarks, t))
        }
        return extension + heldFrames
    }

    /**
     * Generates a twitch / jitter where wrist barely moves (under distance threshold).
     */
    fun generateWristTwitchSequence(arm: PunchArm, startMs: Long = 1000L): List<PoseFrame> {
        val frames = mutableListOf<PoseFrame>()
        for (i in 0..5) {
            val time = startMs + (i * 30L)
            val base = createGuardFrame(time)
            val modified = base.landmarks.toMutableMap()
            val wristType = if (arm == PunchArm.LEFT) PoseLandmarkType.LEFT_WRIST else PoseLandmarkType.RIGHT_WRIST
            val oldW = modified[wristType]!!
            modified[wristType] = oldW.copy(x = oldW.x + 0.01f * (i % 2))
            frames.add(PoseFrame(modified, time))
        }
        return frames
    }

    /**
     * Generates phone shake simulation where entire body violently shifts back and forth.
     */
    fun generatePhoneShakeSequence(startMs: Long = 1000L): List<PoseFrame> {
        val frames = mutableListOf<PoseFrame>()
        for (i in 0..8) {
            val time = startMs + (i * 30L)
            val base = createGuardFrame(time)
            val shift = if (i % 2 == 0) 0.25f else -0.25f
            val shifted = base.landmarks.mapValues { (_, pt) ->
                pt.copy(x = pt.x + shift)
            }
            frames.add(PoseFrame(shifted, time))
        }
        return frames
    }
}
