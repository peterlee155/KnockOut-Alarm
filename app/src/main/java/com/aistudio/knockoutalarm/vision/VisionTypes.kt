package com.aistudio.knockoutalarm.vision

/**
 * 3D coordinate representing a body landmark with tracking confidence and timestamp.
 */
data class Point3D(
    val x: Float,
    val y: Float,
    val z: Float = 0f,
    val confidence: Float = 1f,
    val timestampMs: Long = System.currentTimeMillis()
)

/**
 * Upper body landmarks tracked for boxing punch detection.
 */
enum class PoseLandmarkType {
    NOSE,
    LEFT_SHOULDER,
    RIGHT_SHOULDER,
    LEFT_ELBOW,
    RIGHT_ELBOW,
    LEFT_WRIST,
    RIGHT_WRIST,
    LEFT_HIP,
    RIGHT_HIP
}

/**
 * Pose frame container with normalized coordinates [0.0 .. 1.0].
 */
data class PoseFrame(
    val landmarks: Map<PoseLandmarkType, Point3D>,
    val timestampMs: Long = System.currentTimeMillis()
) {
    fun get(type: PoseLandmarkType): Point3D? = landmarks[type]
}

/**
 * Anti-cheat and pose positioning quality assessment.
 */
enum class PoseQuality(val messageResKey: String) {
    GOOD("arena_pose_ok"),
    BODY_NOT_VISIBLE("arena_pose_upper_body"),
    TOO_CLOSE("arena_pose_step_back"),
    TOO_FAR("arena_pose_step_back"),
    LOW_CONFIDENCE("arena_pose_lost"),
    TORSO_UNSTABLE("arena_pose_shaking"),
    HANDS_NOT_GUARDED("arena_pose_raise_hands")
}

/**
 * Punching arm classification.
 */
enum class PunchArm {
    LEFT,
    RIGHT
}

/**
 * Punch detection Finite State Machine states.
 */
enum class PunchState {
    GUARD,
    EXTENDING,
    PEAK,
    RETRACTING,
    PUNCH_REGISTERED,
    COOLDOWN
}

/**
 * Event triggered when a full extension and retraction punch cycle is validated.
 */
data class PunchEvent(
    val arm: PunchArm,
    val timestampMs: Long,
    val peakAngle: Float,
    val peakVelocity: Float,
    val normalizedTravel: Float,
    val durationMs: Long
)

/**
 * Configuration parameters for the punch detection FSM and anti-cheat checks.
 */
data class PunchDetectionConfig(
    val guardMaxAngle: Float = 110f,
    val peakMinAngle: Float = 150f,
    val retractionMaxAngle: Float = 115f,
    val minExtensionRatio: Float = 0.52f,
    val minPunchVelocity: Float = 1.1f,
    val minPunchDurationMs: Long = 100L,
    val maxPunchDurationMs: Long = 2500L,
    val cooldownMs: Long = 180L,
    val smoothingAlpha: Float = 0.65f,
    val minLandmarkConfidence: Float = 0.45f,
    val torsoStabilityThreshold: Float = 0.35f,
    val minShoulderDistanceRatio: Float = 0.12f,
    val maxShoulderDistanceRatio: Float = 0.88f
) {
    companion object {
        fun forDifficulty(difficulty: com.aistudio.knockoutalarm.model.Difficulty): PunchDetectionConfig {
            return when (difficulty) {
                com.aistudio.knockoutalarm.model.Difficulty.EASY -> PunchDetectionConfig(
                    guardMaxAngle = 115f,
                    peakMinAngle = 142f,
                    retractionMaxAngle = 120f,
                    minExtensionRatio = 0.40f,
                    minPunchVelocity = 0.8f
                )
                com.aistudio.knockoutalarm.model.Difficulty.NORMAL -> PunchDetectionConfig(
                    guardMaxAngle = 110f,
                    peakMinAngle = 150f,
                    retractionMaxAngle = 115f,
                    minExtensionRatio = 0.52f,
                    minPunchVelocity = 1.1f
                )
                com.aistudio.knockoutalarm.model.Difficulty.HARD -> PunchDetectionConfig(
                    guardMaxAngle = 105f,
                    peakMinAngle = 160f,
                    retractionMaxAngle = 110f,
                    minExtensionRatio = 0.65f,
                    minPunchVelocity = 1.5f
                )
            }
        }
    }
}

/**
 * Real-time telemetry emitted by the vision pipeline for HUD rendering.
 */
data class PunchTelemetry(
    val leftArmState: PunchState = PunchState.GUARD,
    val rightArmState: PunchState = PunchState.GUARD,
    val leftElbowAngle: Float = 90f,
    val rightElbowAngle: Float = 90f,
    val leftWristVelocity: Float = 0f,
    val rightWristVelocity: Float = 0f,
    val poseQuality: PoseQuality = PoseQuality.GOOD,
    val lastPunchArm: PunchArm? = null,
    val lastPunchTimestampMs: Long = 0L,
    val fps: Int = 30
)
