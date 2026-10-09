package com.aistudio.knockoutalarm.vision

/**
 * Exponential Moving Average (EMA) smoother for real-time body landmarks.
 * Filters camera jitter while maintaining punch responsiveness.
 */
class PoseSmoother(private val alpha: Float = 0.65f) {

    private val previousPoints = mutableMapOf<PoseLandmarkType, Point3D>()

    /**
     * Smooths an incoming [PoseFrame] against previously buffered landmarks.
     */
    fun smooth(frame: PoseFrame): PoseFrame {
        val smoothedLandmarks = mutableMapOf<PoseLandmarkType, Point3D>()

        for ((type, current) in frame.landmarks) {
            val prev = previousPoints[type]
            if (prev == null) {
                smoothedLandmarks[type] = current
                previousPoints[type] = current
            } else {
                val smoothed = Point3D(
                    x = alpha * current.x + (1f - alpha) * prev.x,
                    y = alpha * current.y + (1f - alpha) * prev.y,
                    z = alpha * current.z + (1f - alpha) * prev.z,
                    confidence = current.confidence,
                    timestampMs = current.timestampMs
                )
                smoothedLandmarks[type] = smoothed
                previousPoints[type] = smoothed
            }
        }

        return PoseFrame(smoothedLandmarks, frame.timestampMs)
    }

    /**
     * Resets smoothing cache when tracking is lost or restarted.
     */
    fun reset() {
        previousPoints.clear()
    }
}
