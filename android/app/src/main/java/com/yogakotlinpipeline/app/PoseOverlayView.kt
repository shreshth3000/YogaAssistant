package com.yogakotlinpipeline.app

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.google.mlkit.vision.pose.PoseLandmark

class PoseOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val poseLandmarks = mutableListOf<PoseLandmark>()
    private var smoothedCoordinates = mapOf<Int, Pair<Float, Float>>()
    // Raw camera buffer dimensions (before rotation compensation)
    private var imageWidth = 1
    private var imageHeight = 1
    // Effective source dimensions used for mapping after rotation swap
    private var srcWidth = 1f
    private var srcHeight = 1f
    // View (PreviewView) dimensions
    private var screenWidth = 1f
    private var screenHeight = 1f
    // Camera properties
    private var isFlipped = false
    private var rotationDegrees = 0
    // Derived mapping values (FILL_CENTER behavior)
    private var scale = 1f
    private var offsetX = 0f
    private var offsetY = 0f
    
    // Paint settings for clear visibility
    private val landmarkPaint = Paint().apply {
        // Use subtle neutral color for base landmarks to avoid visual clutter
        color = Color.argb(140, 255, 255, 255) // semi‑transparent white
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val connectionPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 3f // Optimal thickness for visibility
        isAntiAlias = true
    }

    fun updatePoseLandmarks(landmarks: List<PoseLandmark>, width: Int, height: Int, canvasWidth: Float, canvasHeight: Float) {
        poseLandmarks.clear()
        poseLandmarks.addAll(landmarks)
        imageWidth = width
        imageHeight = height
        screenWidth = canvasWidth
        screenHeight = canvasHeight
        // Recompute transform with current values
        computeTransform()
        invalidate()
    }
    
    fun updatePoseLandmarksWithSmoothing(landmarks: List<PoseLandmark>, smoothedCoords: Map<Int, Pair<Float, Float>>, width: Int, height: Int, canvasWidth: Float, canvasHeight: Float) {
        poseLandmarks.clear()
        poseLandmarks.addAll(landmarks)
        imageWidth = width
        imageHeight = height
        screenWidth = canvasWidth
        screenHeight = canvasHeight
        // Store smoothed coordinates for use in drawing
        smoothedCoordinates = smoothedCoords
        // Recompute transform with current values
        computeTransform()
        invalidate()
    }
    
    // Method to update pose with feedback (like in calibration.ipynb)
    fun updatePoseWithFeedback(landmarks: List<PoseLandmark>, feedback: PoseFeedback?, thresholds: Map<String, PoseThreshold>) {
        android.util.Log.d("PoseOverlayView", "=== OVERLAY UPDATE ===")
        android.util.Log.d("PoseOverlayView", "Received ${landmarks.size} landmarks")
        android.util.Log.d("PoseOverlayView", "Feedback: ${feedback?.jointFeedback?.size ?: 0} joints")
        android.util.Log.d("PoseOverlayView", "Thresholds: ${thresholds.size} joints")
        
        poseLandmarks.clear()
        poseLandmarks.addAll(landmarks)
        // Store feedback for drawing joint angles
        this.feedback = feedback
        this.thresholds = thresholds
        
        // Log feedback details
        feedback?.jointFeedback?.forEach { (jointName, jointFeedback) ->
            android.util.Log.d("PoseOverlayView", "  $jointName: ${jointFeedback.angle.toInt()}° (${if (jointFeedback.isCorrect) "GREEN" else "RED"})")
        }
        
        invalidate()
        android.util.Log.d("PoseOverlayView", "Overlay invalidated")
    }
    
    fun updateDimensions(previewWidth: Int, previewHeight: Int) {
        screenWidth = previewWidth.toFloat()
        screenHeight = previewHeight.toFloat()
        computeTransform()
        invalidate()
    }

    /**
     * Provide camera/image metadata so we can align overlay exactly like ML Kit Examples.
     * - [bufferWidth], [bufferHeight]: ImageProxy width/height
     * - [rotationDeg]: imageInfo.rotationDegrees
     * - [mirror]: true for front camera
     */
    fun setImageSourceInfo(bufferWidth: Int, bufferHeight: Int, rotationDeg: Int, mirror: Boolean) {
        imageWidth = bufferWidth
        imageHeight = bufferHeight
        rotationDegrees = rotationDeg
        isFlipped = mirror
        computeTransform()
        invalidate()
    }

    private fun computeTransform() {
        // Effective source dimensions: swap when rotated 90/270
        val rotated = (rotationDegrees % 180 != 0)
        srcWidth = if (rotated) imageHeight.toFloat() else imageWidth.toFloat()
        srcHeight = if (rotated) imageWidth.toFloat() else imageHeight.toFloat()

        if (screenWidth <= 0f || screenHeight <= 0f || srcWidth <= 0f || srcHeight <= 0f) {
            scale = 1f
            offsetX = 0f
            offsetY = 0f
            return
        }

        // PreviewView default ScaleType is FILL_CENTER → use max scale and center-crop offsets
        val scaleX = screenWidth / srcWidth
        val scaleY = screenHeight / srcHeight
        scale = maxOf(scaleX, scaleY)
        offsetX = (screenWidth - srcWidth * scale) / 2f
        offsetY = (screenHeight - srcHeight * scale) / 2f
    }

    private fun toSrcX(rawX: Float, rawY: Float): Float {
        // Accept normalized or pixel inputs; normalize to source pixels
        return if (rawX <= 1f && rawY <= 1f) rawX * srcWidth else rawX
    }

    private fun toSrcY(rawX: Float, rawY: Float): Float {
        return if (rawX <= 1f && rawY <= 1f) rawY * srcHeight else rawY
    }

    private fun translateX(srcX: Float): Float {
        val vx = offsetX + srcX * scale
        return if (isFlipped) screenWidth - vx else vx
    }

    private fun translateY(srcY: Float): Float = offsetY + srcY * scale
    
    private var feedback: PoseFeedback? = null
    private var thresholds: Map<String, PoseThreshold> = emptyMap()
    
    // ADDED: Drawing optimization flags
    private var lastDrawTime = 0L
    private val minDrawInterval = 16L // ~60fps max

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (poseLandmarks.isEmpty()) return
        
        // ADDED: Throttle drawing to prevent excessive CPU usage
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastDrawTime < minDrawInterval) {
            return
        }
        lastDrawTime = currentTime
        
        // Keep screenWidth/Height up to date in case view size changed
        if (screenWidth != width.toFloat() || screenHeight != height.toFloat()) {
            screenWidth = width.toFloat()
            screenHeight = height.toFloat()
            computeTransform()
        }

        // Draw connections first (behind landmarks) using translate helpers
        drawConnections(canvas)
        
        // Draw landmarks on top
        for (landmark in poseLandmarks) {
            try {
                val smoothed = smoothedCoordinates[landmark.landmarkType]
                val rawX = smoothed?.first ?: landmark.position.x
                val rawY = smoothed?.second ?: landmark.position.y
                val sx = toSrcX(rawX, rawY)
                val sy = toSrcY(rawX, rawY)
                val adjustedX = translateX(sx)
                val adjustedY = translateY(sy)
                
                // Check if coordinates are within screen bounds
                if (adjustedX >= 0 && adjustedX <= width && 
                    adjustedY >= 0 && adjustedY <= height) {
                    // Same radius as ML Kit Examples (8f)
                    canvas.drawCircle(adjustedX, adjustedY, 8f, landmarkPaint)
                }
            } catch (e: Exception) {
                // Skip this landmark if there's an error
                continue
            }
        }
        
        // Draw joint feedback (like in calibration.ipynb)
        drawJointFeedback(canvas)
    }
    
    private fun drawConnections(canvas: Canvas) {
        val connections = listOf(
            // Face connections (same as ML Kit Examples)
            PoseLandmark.LEFT_EYE to PoseLandmark.RIGHT_EYE,
            PoseLandmark.LEFT_EYE to PoseLandmark.LEFT_EAR,
            PoseLandmark.RIGHT_EYE to PoseLandmark.RIGHT_EAR,
            PoseLandmark.NOSE to PoseLandmark.LEFT_EYE,
            PoseLandmark.NOSE to PoseLandmark.RIGHT_EYE,
            PoseLandmark.NOSE to PoseLandmark.LEFT_MOUTH,
            PoseLandmark.NOSE to PoseLandmark.RIGHT_MOUTH,
            
            // Torso - main body structure
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.RIGHT_SHOULDER,
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_HIP,
            PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_HIP,
            PoseLandmark.LEFT_HIP to PoseLandmark.RIGHT_HIP,
            
            // Arms - main arm structure
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_ELBOW,
            PoseLandmark.LEFT_ELBOW to PoseLandmark.LEFT_WRIST,
            PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_ELBOW,
            PoseLandmark.RIGHT_ELBOW to PoseLandmark.RIGHT_WRIST,
            
            // Hands & Fingers (Basic) - same as ML Kit Examples
            PoseLandmark.LEFT_WRIST to PoseLandmark.LEFT_INDEX,
            PoseLandmark.LEFT_WRIST to PoseLandmark.LEFT_PINKY,
            PoseLandmark.LEFT_WRIST to PoseLandmark.LEFT_THUMB,
            PoseLandmark.RIGHT_WRIST to PoseLandmark.RIGHT_INDEX,
            PoseLandmark.RIGHT_WRIST to PoseLandmark.RIGHT_PINKY,
            PoseLandmark.RIGHT_WRIST to PoseLandmark.RIGHT_THUMB,
            
            // Legs - main leg structure
            PoseLandmark.LEFT_HIP to PoseLandmark.LEFT_KNEE,
            PoseLandmark.LEFT_KNEE to PoseLandmark.LEFT_ANKLE,
            PoseLandmark.RIGHT_HIP to PoseLandmark.RIGHT_KNEE,
            PoseLandmark.RIGHT_KNEE to PoseLandmark.RIGHT_ANKLE,
            
            // Feet - same as ML Kit Examples
            PoseLandmark.LEFT_ANKLE to PoseLandmark.LEFT_HEEL,
            PoseLandmark.LEFT_ANKLE to PoseLandmark.LEFT_FOOT_INDEX,
            PoseLandmark.RIGHT_ANKLE to PoseLandmark.RIGHT_HEEL,
            PoseLandmark.RIGHT_ANKLE to PoseLandmark.RIGHT_FOOT_INDEX
        )
        
        for ((start, end) in connections) {
            try {
                val startLandmark = poseLandmarks.find { it.landmarkType == start }
                val endLandmark = poseLandmarks.find { it.landmarkType == end }
                
                if (startLandmark != null && endLandmark != null) {
                    val startSmoothed = smoothedCoordinates[startLandmark.landmarkType]
                    val endSmoothed = smoothedCoordinates[endLandmark.landmarkType]
                    val srx = startSmoothed?.first ?: startLandmark.position.x
                    val sry = startSmoothed?.second ?: startLandmark.position.y
                    val erx = endSmoothed?.first ?: endLandmark.position.x
                    val ery = endSmoothed?.second ?: endLandmark.position.y

                    val sx = translateX(toSrcX(srx, sry))
                    val sy = translateY(toSrcY(srx, sry))
                    val ex = translateX(toSrcX(erx, ery))
                    val ey = translateY(toSrcY(erx, ery))
                    
                    // Check if coordinates are within screen bounds
                    if (sx >= 0 && sx <= width && sy >= 0 && sy <= height &&
                        ex >= 0 && ex <= width && ey >= 0 && ey <= height) {
                        canvas.drawLine(sx, sy, ex, ey, connectionPaint)
                    }
                }
            } catch (e: Exception) {
                // Skip this connection if there's an error
                continue
            }
        }
    }
    
    private fun drawJointFeedback(canvas: Canvas) {
        android.util.Log.d("PoseOverlayView", "=== DRAWING JOINT FEEDBACK ===")
        android.util.Log.d("PoseOverlayView", "Feedback available: ${feedback != null}")
        android.util.Log.d("PoseOverlayView", "Joint feedback count: ${feedback?.jointFeedback?.size ?: 0}")
        
        feedback?.jointFeedback?.forEach { entry ->
            val (jointName, jointFeedback) = entry
            android.util.Log.d("PoseOverlayView", "Processing joint: $jointName")
            
            val jointLandmark = getJointLandmark(jointName)
            android.util.Log.d("PoseOverlayView", "  Landmark type: $jointLandmark")
            
            if (jointLandmark == null) {
                android.util.Log.w("PoseOverlayView", "  No landmark mapping for: $jointName")
                return@forEach
            }
            
            val landmark = poseLandmarks.find { it.landmarkType == jointLandmark }
            if (landmark == null) {
                android.util.Log.w("PoseOverlayView", "  Landmark not found for type: $jointLandmark")
                return@forEach
            }
            
            android.util.Log.d("PoseOverlayView", "  Drawing joint: $jointName at (${landmark.position.x}, ${landmark.position.y})")
            
            // Map landmark to view space using the same transform as connections
            val smoothed = smoothedCoordinates[landmark.landmarkType]
            val rawX = smoothed?.first ?: landmark.position.x
            val rawY = smoothed?.second ?: landmark.position.y
            val sx = translateX(toSrcX(rawX, rawY))
            val sy = translateY(toSrcY(rawX, rawY))
            
            // Check if coordinates are within screen bounds
            android.util.Log.d("PoseOverlayView", "  Screen coordinates: ($sx, $sy)")
            android.util.Log.d("PoseOverlayView", "  Screen bounds: (0,0) to ($width, $height)")
            android.util.Log.d("PoseOverlayView", "  Within bounds: ${sx >= 0 && sx <= width && sy >= 0 && sy <= height}")
            
            if (sx >= 0 && sx <= width && sy >= 0 && sy <= height) {
                // Draw circle at joint (like in calibration.ipynb)
                val jointPaint = Paint().apply {
                    style = Paint.Style.FILL
                    color = if (jointFeedback.isCorrect) Color.GREEN else Color.RED
                    isAntiAlias = true
                }
                android.util.Log.d("PoseOverlayView", "  Drawing ${if (jointFeedback.isCorrect) "GREEN" else "RED"} circle at ($sx, $sy)")
                canvas.drawCircle(sx, sy, 15f, jointPaint)
                
                // Draw angle text (like in calibration.ipynb)
                val textPaint = Paint().apply {
                    textSize = 30f
                    color = if (jointFeedback.isCorrect) Color.GREEN else Color.RED
                    isAntiAlias = true
                    typeface = Typeface.DEFAULT_BOLD
                }
                // Clamp label inside the view
                val tx = sx - 20f
                val ty = sy - 20f
                val clampedX = tx.coerceIn(8f, width - 8f)
                val clampedY = ty.coerceIn(24f, height - 8f)
                android.util.Log.d("PoseOverlayView", "  Drawing angle text: ${jointFeedback.angle.toInt()}° at ($clampedX, $clampedY)")
                canvas.drawText("${jointFeedback.angle.toInt()}°", clampedX, clampedY, textPaint)
            } else {
                android.util.Log.w("PoseOverlayView", "  Joint outside screen bounds, skipping draw")
            }
        }
    }
    
    private fun getJointLandmark(jointName: String): Int? {
        return when (jointName) {
            "left_shoulder_angle" -> PoseLandmark.LEFT_SHOULDER
            "left_elbow_angle" -> PoseLandmark.LEFT_ELBOW
            "right_shoulder_angle" -> PoseLandmark.RIGHT_SHOULDER
            "right_elbow_angle" -> PoseLandmark.RIGHT_ELBOW
            "left_hip_angle" -> PoseLandmark.LEFT_HIP
            "left_knee_angle" -> PoseLandmark.LEFT_KNEE
            "right_hip_angle" -> PoseLandmark.RIGHT_HIP
            "right_knee_angle" -> PoseLandmark.RIGHT_KNEE
            else -> null
        }
    }
}
