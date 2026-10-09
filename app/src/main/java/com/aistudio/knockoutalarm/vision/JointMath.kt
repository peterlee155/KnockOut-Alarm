package com.aistudio.knockoutalarm.vision

import kotlin.math.acos
import kotlin.math.sqrt

/**
 * Pure mathematical functions for body landmark joint angles, distances, and velocities.
 */
object JointMath {

    /**
     * Euclidean 2D distance between two points.
     */
    fun distance2D(p1: Point3D, p2: Point3D): Float {
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        return sqrt(dx * dx + dy * dy)
    }

    /**
     * Euclidean 3D distance between two points.
     */
    fun distance3D(p1: Point3D, p2: Point3D): Float {
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        val dz = p1.z - p2.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    /**
     * Computes the joint angle in degrees (0..180) at vertex [b], formed by vectors (a - b) and (c - b).
     * For example, for elbow angle: a = Shoulder, b = Elbow, c = Wrist.
     */
    fun calculateJointAngle(a: Point3D, b: Point3D, c: Point3D): Float {
        val v1x = a.x - b.x
        val v1y = a.y - b.y
        val v2x = c.x - b.x
        val v2y = c.y - b.y

        val mag1 = sqrt(v1x * v1x + v1y * v1y)
        val mag2 = sqrt(v2x * v2x + v2y * v2y)

        if (mag1 <= 1e-6f || mag2 <= 1e-6f) return 0f

        val dot = (v1x * v2x + v1y * v2y)
        val cosTheta = (dot / (mag1 * mag2)).coerceIn(-1f, 1f)
        return Math.toDegrees(acos(cosTheta.toDouble())).toFloat()
    }

    /**
     * Calculates the anatomical shoulder width to use as a normalization reference.
     */
    fun calculateShoulderWidth(leftShoulder: Point3D, rightShoulder: Point3D): Float {
        val dist = distance2D(leftShoulder, rightShoulder)
        return if (dist > 0.01f) dist else 0.25f // Safe fallback reference
    }

    /**
     * Computes the displacement of a wrist relative to its shoulder, normalized by shoulder width.
     */
    fun normalizedWristDistance(wrist: Point3D, shoulder: Point3D, shoulderWidth: Float): Float {
        val rawDist = distance2D(wrist, shoulder)
        val safeWidth = if (shoulderWidth > 0.01f) shoulderWidth else 0.25f
        return rawDist / safeWidth
    }

    /**
     * Calculates the normalized velocity (body units per second) of a landmark between two frames.
     */
    fun calculateVelocity(
        current: Point3D,
        previous: Point3D,
        deltaTimeMs: Long,
        shoulderWidth: Float
    ): Float {
        if (deltaTimeMs <= 0) return 0f
        val rawDist = distance2D(current, previous)
        val safeWidth = if (shoulderWidth > 0.01f) shoulderWidth else 0.25f
        val normalizedDist = rawDist / safeWidth
        val dtSec = deltaTimeMs / 1000f
        return normalizedDist / dtSec
    }
}
