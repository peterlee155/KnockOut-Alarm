package com.aistudio.knockoutalarm.vision

import kotlin.math.max

/**
 * Pure Kotlin punch detector implementing independent dual-arm Finite State Machines (FSM).
 *
 * Enforces legitimate boxing punches:
 * GUARD -> EXTENDING -> PEAK -> RETRACTING -> PUNCH_REGISTERED -> COOLDOWN -> GUARD
 *
 * Never registers a punch merely because a coordinate threshold was crossed.
 * Requires full extension AND retraction cycle, with velocity, angle, and temporal verification.
 */
class PunchDetector(
    var config: PunchDetectionConfig = PunchDetectionConfig()
) {
    private val poseSmoother = PoseSmoother(config.smoothingAlpha)
    private val qualityEvaluator = PoseQualityEvaluator(config)

    // Independent arm trackers
    private val leftArmTracker = ArmTracker(PunchArm.LEFT)
    private val rightArmTracker = ArmTracker(PunchArm.RIGHT)

    private var lastGlobalPunchTimestampMs: Long = 0L
    private var lastGlobalPunchArm: PunchArm? = null

    /**
     * Internal state machine tracker for an individual arm.
     */
    private inner class ArmTracker(val arm: PunchArm) {
        var state: PunchState = PunchState.GUARD
        var stateEnterTimeMs: Long = 0L
        var extensionStartTimeMs: Long = 0L

        var guardWristDist: Float = 0f
        var peakAngleReached: Float = 0f
        var peakVelocityReached: Float = 0f
        var maxTravelReached: Float = 0f

        var previousWrist: Point3D? = null
        var previousTimestampMs: Long = 0L
        var currentElbowAngle: Float = 90f
        var currentWristVelocity: Float = 0f

        fun reset() {
            state = PunchState.GUARD
            stateEnterTimeMs = 0L
            extensionStartTimeMs = 0L
            guardWristDist = 0f
            peakAngleReached = 0f
            peakVelocityReached = 0f
            maxTravelReached = 0f
            previousWrist = null
            previousTimestampMs = 0L
            currentElbowAngle = 90f
            currentWristVelocity = 0f
        }

        fun update(
            shoulder: Point3D,
            elbow: Point3D,
            wrist: Point3D,
            shoulderWidth: Float,
            timestampMs: Long,
            quality: PoseQuality
        ): PunchEvent? {
            // Calculate joint angle
            val angle = JointMath.calculateJointAngle(shoulder, elbow, wrist)
            currentElbowAngle = angle

            // Calculate instantaneous wrist velocity
            val prevW = previousWrist
            val prevTime = previousTimestampMs
            var velocity = 0f
            if (prevW != null && prevTime > 0L) {
                val dtMs = timestampMs - prevTime
                if (dtMs in 5..300) {
                    velocity = JointMath.calculateVelocity(wrist, prevW, dtMs, shoulderWidth)
                }
            }
            currentWristVelocity = velocity
            previousWrist = wrist
            previousTimestampMs = timestampMs

            // If body quality is compromised, reset FSM to GUARD
            if (quality != PoseQuality.GOOD) {
                if (state != PunchState.GUARD) {
                    state = PunchState.GUARD
                    stateEnterTimeMs = timestampMs
                }
                return null
            }

            val currentWristDist = JointMath.normalizedWristDistance(wrist, shoulder, shoulderWidth)
            var eventToEmit: PunchEvent? = null

            when (state) {
                PunchState.GUARD -> {
                    // Update guard baseline only while arm is in guard angle
                    if (angle <= config.guardMaxAngle) {
                        guardWristDist = currentWristDist
                    }
                    peakVelocityReached = 0f
                    peakAngleReached = 0f
                    maxTravelReached = 0f

                    // Detect initiation of punch extension:
                    val isExtendingAngle = angle > config.guardMaxAngle + 5f
                    val hasPunchVelocity = velocity >= config.minPunchVelocity * 0.5f
                    val isMovingOutward = currentWristDist > guardWristDist + 0.10f

                    if (isExtendingAngle && (isMovingOutward || hasPunchVelocity)) {
                        state = PunchState.EXTENDING
                        stateEnterTimeMs = timestampMs
                        extensionStartTimeMs = timestampMs
                    }
                }

                PunchState.EXTENDING -> {
                    peakVelocityReached = max(peakVelocityReached, velocity)
                    peakAngleReached = max(peakAngleReached, angle)
                    val travel = currentWristDist - guardWristDist
                    maxTravelReached = max(maxTravelReached, travel)

                    val timeInExtension = timestampMs - extensionStartTimeMs

                    // Check timeout
                    if (timeInExtension > config.maxPunchDurationMs) {
                        state = PunchState.GUARD
                        stateEnterTimeMs = timestampMs
                        return null
                    }

                    // Check if peak reached:
                    // Elbow opened sufficiently AND wrist traveled away from guard
                    val reachedPeakAngle = angle >= config.peakMinAngle
                    val reachedMinTravel = maxTravelReached >= config.minExtensionRatio
                    val reachedMinVelocity = peakVelocityReached >= config.minPunchVelocity

                    if (reachedPeakAngle && reachedMinTravel && reachedMinVelocity) {
                        state = PunchState.PEAK
                        stateEnterTimeMs = timestampMs
                    } else if (angle < config.guardMaxAngle && timeInExtension > 150) {
                        // Aborted early back to guard without reaching peak
                        state = PunchState.GUARD
                        stateEnterTimeMs = timestampMs
                    }
                }

                PunchState.PEAK -> {
                    // Arm is at full extension. Waiting for retraction.
                    val timeAtPeak = timestampMs - stateEnterTimeMs

                    // If arm is held extended too long (> 1500ms), prevent false multiple triggers
                    if (timeAtPeak > 1500L) {
                        state = PunchState.GUARD
                        stateEnterTimeMs = timestampMs
                        return null
                    }

                    // Look for return stroke: angle decreasing or wrist moving back towards shoulder
                    val isRetracting = angle < config.peakMinAngle - 10f || currentWristDist < maxTravelReached - 0.15f
                    if (isRetracting) {
                        state = PunchState.RETRACTING
                        stateEnterTimeMs = timestampMs
                    }
                }

                PunchState.RETRACTING -> {
                    val totalDuration = timestampMs - extensionStartTimeMs

                    // If retraction takes too long (> maxPunchDurationMs), invalidate
                    if (totalDuration > config.maxPunchDurationMs) {
                        state = PunchState.GUARD
                        stateEnterTimeMs = timestampMs
                        return null
                    }

                    // Check if elbow has returned to guard angle
                    if (angle <= config.retractionMaxAngle) {
                        // Valid full punch cycle completed!
                        val duration = timestampMs - extensionStartTimeMs
                        if (duration >= config.minPunchDurationMs) {
                            state = PunchState.PUNCH_REGISTERED
                            stateEnterTimeMs = timestampMs

                            eventToEmit = PunchEvent(
                                arm = arm,
                                timestampMs = timestampMs,
                                peakAngle = peakAngleReached,
                                peakVelocity = peakVelocityReached,
                                normalizedTravel = maxTravelReached,
                                durationMs = duration
                            )
                        } else {
                            // Impossibly fast (e.g. noise/flicker)
                            state = PunchState.GUARD
                            stateEnterTimeMs = timestampMs
                        }
                    }
                }

                PunchState.PUNCH_REGISTERED -> {
                    // Immediately transition to cooldown
                    state = PunchState.COOLDOWN
                    stateEnterTimeMs = timestampMs
                }

                PunchState.COOLDOWN -> {
                    if (timestampMs - stateEnterTimeMs >= config.cooldownMs) {
                        state = PunchState.GUARD
                        stateEnterTimeMs = timestampMs
                    }
                }
            }

            return eventToEmit
        }
    }

    /**
     * Process an incoming raw pose frame.
     * Returns a valid [PunchEvent] if a punch completed, along with current [PunchTelemetry].
     */
    fun processFrame(rawFrame: PoseFrame): Pair<PunchEvent?, PunchTelemetry> {
        val smoothedFrame = poseSmoother.smooth(rawFrame)
        val quality = qualityEvaluator.evaluate(smoothedFrame)

        val leftShoulder = smoothedFrame.get(PoseLandmarkType.LEFT_SHOULDER)
        val rightShoulder = smoothedFrame.get(PoseLandmarkType.RIGHT_SHOULDER)
        val leftElbow = smoothedFrame.get(PoseLandmarkType.LEFT_ELBOW)
        val rightElbow = smoothedFrame.get(PoseLandmarkType.RIGHT_ELBOW)
        val leftWrist = smoothedFrame.get(PoseLandmarkType.LEFT_WRIST)
        val rightWrist = smoothedFrame.get(PoseLandmarkType.RIGHT_WRIST)

        var detectedEvent: PunchEvent? = null

        if (leftShoulder != null && rightShoulder != null &&
            leftElbow != null && rightElbow != null &&
            leftWrist != null && rightWrist != null
        ) {
            val shoulderWidth = JointMath.calculateShoulderWidth(leftShoulder, rightShoulder)

            // Update both arm FSMs
            val leftEvent = leftArmTracker.update(
                leftShoulder, leftElbow, leftWrist,
                shoulderWidth, smoothedFrame.timestampMs, quality
            )
            val rightEvent = rightArmTracker.update(
                rightShoulder, rightElbow, rightWrist,
                shoulderWidth, smoothedFrame.timestampMs, quality
            )

            // Prevent simultaneous double counting (e.g. within 120ms debounce)
            val candidateEvent = leftEvent ?: rightEvent
            if (candidateEvent != null) {
                val timeSinceLastPunch = candidateEvent.timestampMs - lastGlobalPunchTimestampMs
                if (timeSinceLastPunch >= 120L || lastGlobalPunchArm != candidateEvent.arm) {
                    detectedEvent = candidateEvent
                    lastGlobalPunchTimestampMs = candidateEvent.timestampMs
                    lastGlobalPunchArm = candidateEvent.arm
                }
            }
        }

        val telemetry = PunchTelemetry(
            leftArmState = leftArmTracker.state,
            rightArmState = rightArmTracker.state,
            leftElbowAngle = leftArmTracker.currentElbowAngle,
            rightElbowAngle = rightArmTracker.currentElbowAngle,
            leftWristVelocity = leftArmTracker.currentWristVelocity,
            rightWristVelocity = rightArmTracker.currentWristVelocity,
            poseQuality = quality,
            lastPunchArm = lastGlobalPunchArm,
            lastPunchTimestampMs = lastGlobalPunchTimestampMs
        )

        return Pair(detectedEvent, telemetry)
    }

    /**
     * Reset tracking state (e.g., when fight starts or restarts).
     */
    fun reset() {
        poseSmoother.reset()
        qualityEvaluator.reset()
        leftArmTracker.reset()
        rightArmTracker.reset()
        lastGlobalPunchTimestampMs = 0L
        lastGlobalPunchArm = null
    }
}
