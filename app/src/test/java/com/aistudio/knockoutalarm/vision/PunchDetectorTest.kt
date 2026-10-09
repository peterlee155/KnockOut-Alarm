package com.aistudio.knockoutalarm.vision

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PunchDetectorTest {

    private lateinit var detector: PunchDetector
    private val config = PunchDetectionConfig(
        guardMaxAngle = 115f,
        peakMinAngle = 145f,
        retractionMaxAngle = 120f,
        minExtensionRatio = 0.35f,
        minPunchVelocity = 0.8f,
        minPunchDurationMs = 80L,
        maxPunchDurationMs = 2500L,
        cooldownMs = 150L,
        smoothingAlpha = 0.8f,
        minLandmarkConfidence = 0.5f
    )

    @Before
    fun setUp() {
        detector = PunchDetector(config)
    }

    @Test
    fun testValidLeftJab() {
        // Initial neutral guard frames to initialize baseline
        detector.processFrame(SyntheticPoseGenerator.createGuardFrame(100L))
        detector.processFrame(SyntheticPoseGenerator.createGuardFrame(200L))

        val punchFrames = SyntheticPoseGenerator.generatePunchSequence(
            arm = PunchArm.LEFT,
            startMs = 300L,
            durationMs = 320L,
            peakAngle = 160f,
            frameCount = 14
        )

        var registeredPunch: PunchEvent? = null
        for (frame in punchFrames) {
            val (event, _) = detector.processFrame(frame)
            if (event != null) {
                registeredPunch = event
            }
        }

        assertNotNull("Valid left jab must be registered", registeredPunch)
        assertEquals(PunchArm.LEFT, registeredPunch?.arm)
        assertTrue("Peak angle should exceed minimum threshold", registeredPunch!!.peakAngle >= 140f)
    }

    @Test
    fun testValidRightJab() {
        detector.processFrame(SyntheticPoseGenerator.createGuardFrame(100L))
        detector.processFrame(SyntheticPoseGenerator.createGuardFrame(200L))

        val punchFrames = SyntheticPoseGenerator.generatePunchSequence(
            arm = PunchArm.RIGHT,
            startMs = 300L,
            durationMs = 320L,
            peakAngle = 160f,
            frameCount = 14
        )

        var registeredPunch: PunchEvent? = null
        for (frame in punchFrames) {
            val (event, _) = detector.processFrame(frame)
            if (event != null) {
                registeredPunch = event
            }
        }

        assertNotNull("Valid right jab must be registered", registeredPunch)
        assertEquals(PunchArm.RIGHT, registeredPunch?.arm)
    }

    @Test
    fun testArmHeldExtendedDoesNotCountMultipleTimes() {
        detector.processFrame(SyntheticPoseGenerator.createGuardFrame(100L))
        detector.processFrame(SyntheticPoseGenerator.createGuardFrame(200L))

        val heldFrames = SyntheticPoseGenerator.generateHeldExtendedSequence(
            arm = PunchArm.LEFT,
            startMs = 300L
        )

        var punchCount = 0
        for (frame in heldFrames) {
            val (event, _) = detector.processFrame(frame)
            if (event != null) punchCount++
        }

        // Arm was never retracted back to guard, so no punch should register!
        assertEquals("Holding arm extended without retracting must NOT register a punch", 0, punchCount)
    }

    @Test
    fun testWristTwitchRejected() {
        detector.processFrame(SyntheticPoseGenerator.createGuardFrame(100L))
        detector.processFrame(SyntheticPoseGenerator.createGuardFrame(200L))

        val twitchFrames = SyntheticPoseGenerator.generateWristTwitchSequence(
            arm = PunchArm.LEFT,
            startMs = 300L
        )

        var punchCount = 0
        for (frame in twitchFrames) {
            val (event, _) = detector.processFrame(frame)
            if (event != null) punchCount++
        }

        assertEquals("Minor wrist jitter without joint extension must be rejected", 0, punchCount)
    }

    @Test
    fun testLowConfidencePoseRejected() {
        detector.processFrame(SyntheticPoseGenerator.createGuardFrame(100L))

        // Create low confidence frames (e.g. 0.2 < minLandmarkConfidence)
        val lowConfGuard = SyntheticPoseGenerator.createGuardFrame(200L, confidence = 0.2f)
        val (_, telemetry) = detector.processFrame(lowConfGuard)

        assertEquals("Low confidence frame must report LOW_CONFIDENCE quality", PoseQuality.LOW_CONFIDENCE, telemetry.poseQuality)
    }

    @Test
    fun testLostPoseHandling() {
        detector.processFrame(SyntheticPoseGenerator.createGuardFrame(100L))

        // Create frame with missing shoulders
        val emptyFrame = PoseFrame(emptyMap(), 200L)
        val (event, telemetry) = detector.processFrame(emptyFrame)

        assertNull("Empty frame cannot produce punch event", event)
        assertEquals(PoseQuality.BODY_NOT_VISIBLE, telemetry.poseQuality)
    }

    @Test
    fun testPhoneShakeRejection() {
        detector.processFrame(SyntheticPoseGenerator.createGuardFrame(100L))

        val shakeFrames = SyntheticPoseGenerator.generatePhoneShakeSequence(200L)
        var sawUnstableQuality = false

        for (frame in shakeFrames) {
            val (_, telemetry) = detector.processFrame(frame)
            if (telemetry.poseQuality == PoseQuality.TORSO_UNSTABLE) {
                sawUnstableQuality = true
            }
        }

        assertTrue("Violent camera movement must trigger TORSO_UNSTABLE", sawUnstableQuality)
    }

    @Test
    fun testAlternatingLeftRightCombo() {
        detector.processFrame(SyntheticPoseGenerator.createGuardFrame(100L))

        // Left punch
        val leftFrames = SyntheticPoseGenerator.generatePunchSequence(
            PunchArm.LEFT, startMs = 200L, durationMs = 280L, frameCount = 12
        )
        var leftPunch: PunchEvent? = null
        for (f in leftFrames) {
            val (ev, _) = detector.processFrame(f)
            if (ev != null) leftPunch = ev
        }
        assertNotNull("Left punch in combo should register", leftPunch)

        // Return to neutral guard
        detector.processFrame(SyntheticPoseGenerator.createGuardFrame(600L))
        detector.processFrame(SyntheticPoseGenerator.createGuardFrame(700L))

        // Right punch
        val rightFrames = SyntheticPoseGenerator.generatePunchSequence(
            PunchArm.RIGHT, startMs = 800L, durationMs = 280L, frameCount = 12
        )
        var rightPunch: PunchEvent? = null
        for (f in rightFrames) {
            val (ev, _) = detector.processFrame(f)
            if (ev != null) rightPunch = ev
        }
        assertNotNull("Right punch in combo should register", rightPunch)
    }
}
