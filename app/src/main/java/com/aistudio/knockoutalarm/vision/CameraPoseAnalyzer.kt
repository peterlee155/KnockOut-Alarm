package com.aistudio.knockoutalarm.vision

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions

/**
 * CameraX ImageAnalysis analyzer that feeds front camera video frames to ML Kit Pose Landmarker.
 * Converts native ML Kit landmarks to normalized [PoseFrame] representations for the [PunchDetector].
 */
class CameraPoseAnalyzer(
    private val onPoseFrame: (PoseFrame) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = PoseDetectorOptions.Builder()
        .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
        .setPreferredHardwareConfigs(PoseDetectorOptions.CPU_GPU)
        .build()

    private val poseDetector: PoseDetector = PoseDetection.getClient(options)

    @Volatile
    private var isBusy = false

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null || isBusy) {
            imageProxy.close()
            return
        }

        isBusy = true
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val width = if (rotationDegrees == 90 || rotationDegrees == 270) imageProxy.height else imageProxy.width
        val height = if (rotationDegrees == 90 || rotationDegrees == 270) imageProxy.width else imageProxy.height

        val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        poseDetector.process(inputImage)
            .addOnSuccessListener { pose ->
                val frame = convertMlKitPoseToFrame(pose, width.toFloat(), height.toFloat())
                onPoseFrame(frame)
            }
            .addOnFailureListener {
                // If model inference drops a frame, continue pipeline smoothly
            }
            .addOnCompleteListener {
                isBusy = false
                imageProxy.close()
            }
    }

    private fun convertMlKitPoseToFrame(pose: Pose, frameWidth: Float, frameHeight: Float): PoseFrame {
        val landmarks = mutableMapOf<PoseLandmarkType, Point3D>()
        val time = System.currentTimeMillis()

        fun extract(mlKitType: Int, targetType: PoseLandmarkType) {
            val lm = pose.getPoseLandmark(mlKitType)
            if (lm != null && frameWidth > 0f && frameHeight > 0f) {
                // Front camera preview is mirrored for user; normalize x & y to [0..1]
                val normX = (lm.position.x / frameWidth).coerceIn(0f, 1f)
                val normY = (lm.position.y / frameHeight).coerceIn(0f, 1f)
                val normZ = lm.position3D.z / frameWidth
                landmarks[targetType] = Point3D(
                    x = normX,
                    y = normY,
                    z = normZ,
                    confidence = lm.inFrameLikelihood,
                    timestampMs = time
                )
            }
        }

        extract(PoseLandmark.NOSE, PoseLandmarkType.NOSE)
        extract(PoseLandmark.LEFT_SHOULDER, PoseLandmarkType.LEFT_SHOULDER)
        extract(PoseLandmark.RIGHT_SHOULDER, PoseLandmarkType.RIGHT_SHOULDER)
        extract(PoseLandmark.LEFT_ELBOW, PoseLandmarkType.LEFT_ELBOW)
        extract(PoseLandmark.RIGHT_ELBOW, PoseLandmarkType.RIGHT_ELBOW)
        extract(PoseLandmark.LEFT_WRIST, PoseLandmarkType.LEFT_WRIST)
        extract(PoseLandmark.RIGHT_WRIST, PoseLandmarkType.RIGHT_WRIST)
        extract(PoseLandmark.LEFT_HIP, PoseLandmarkType.LEFT_HIP)
        extract(PoseLandmark.RIGHT_HIP, PoseLandmarkType.RIGHT_HIP)

        return PoseFrame(landmarks, time)
    }

    fun close() {
        try {
            poseDetector.close()
        } catch (e: Exception) {
            // Ignore
        }
    }
}
