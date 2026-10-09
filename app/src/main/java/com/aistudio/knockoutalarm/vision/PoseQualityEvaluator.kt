package com.aistudio.knockoutalarm.vision

/**
 * Anti-cheat evaluator ensuring physical presence and rejecting phone shaking or false postures.
 */
class PoseQualityEvaluator(private val config: PunchDetectionConfig) {

    private var previousTorsoMidpoint: Point3D? = null
    private var previousTimestampMs: Long = 0L

    fun evaluate(frame: PoseFrame): PoseQuality {
        val leftShoulder = frame.get(PoseLandmarkType.LEFT_SHOULDER)
        val rightShoulder = frame.get(PoseLandmarkType.RIGHT_SHOULDER)
        val leftElbow = frame.get(PoseLandmarkType.LEFT_ELBOW)
        val rightElbow = frame.get(PoseLandmarkType.RIGHT_ELBOW)
        val leftWrist = frame.get(PoseLandmarkType.LEFT_WRIST)
        val rightWrist = frame.get(PoseLandmarkType.RIGHT_WRIST)

        // 1. Mandatory landmarks check
        if (leftShoulder == null || rightShoulder == null ||
            leftElbow == null || rightElbow == null ||
            leftWrist == null || rightWrist == null
        ) {
            return PoseQuality.BODY_NOT_VISIBLE
        }

        // 2. Confidence threshold
        val minConf = listOf(
            leftShoulder.confidence, rightShoulder.confidence,
            leftElbow.confidence, rightElbow.confidence,
            leftWrist.confidence, rightWrist.confidence
        ).minOrNull() ?: 0f

        if (minConf < config.minLandmarkConfidence) {
            return PoseQuality.LOW_CONFIDENCE
        }

        // 3. Distance / Screen occupancy check
        val shoulderWidth = JointMath.calculateShoulderWidth(leftShoulder, rightShoulder)
        if (shoulderWidth < config.minShoulderDistanceRatio) {
            return PoseQuality.TOO_FAR
        }
        if (shoulderWidth > config.maxShoulderDistanceRatio) {
            return PoseQuality.TOO_CLOSE
        }

        // 4. Torso stability / Phone shake rejection
        val currentTorsoMidpoint = Point3D(
            x = (leftShoulder.x + rightShoulder.x) / 2f,
            y = (leftShoulder.y + rightShoulder.y) / 2f,
            z = (leftShoulder.z + rightShoulder.z) / 2f,
            timestampMs = frame.timestampMs
        )

        val prevTorso = previousTorsoMidpoint
        val prevTime = previousTimestampMs
        var isTorsoUnstable = false

        if (prevTorso != null && prevTime > 0L) {
            val dtMs = frame.timestampMs - prevTime
            if (dtMs in 10..400) {
                val torsoShiftVelocity = JointMath.calculateVelocity(
                    currentTorsoMidpoint,
                    prevTorso,
                    dtMs,
                    shoulderWidth
                )
                // If the entire body/phone is shaking violently at excessive speed:
                if (torsoShiftVelocity > config.torsoStabilityThreshold * 10f) {
                    isTorsoUnstable = true
                }
            }
        }

        previousTorsoMidpoint = currentTorsoMidpoint
        previousTimestampMs = frame.timestampMs

        if (isTorsoUnstable) {
            return PoseQuality.TORSO_UNSTABLE
        }

        return PoseQuality.GOOD
    }

    fun reset() {
        previousTorsoMidpoint = null
        previousTimestampMs = 0L
    }
}
