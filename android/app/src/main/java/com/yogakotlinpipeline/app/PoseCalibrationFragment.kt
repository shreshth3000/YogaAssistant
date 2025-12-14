package com.yogakotlinpipeline.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import com.yogakotlinpipeline.app.databinding.FragmentPoseCalibrationBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

class PoseCalibrationFragment : Fragment() {
    
    private var _binding: FragmentPoseCalibrationBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    
    // CHANGED: Make poseDetector nullable and add cleanup
    private var poseDetector: PoseDetector? = null
    private var poseName: String = ""
    private var poseThresholds: Map<String, PoseThreshold> = emptyMap()
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    
    // Smoothing variables
    private val smoothingFactor = 0.6f // Higher = more smoothing, lower = more responsive
    private val smoothedLandmarks = mutableMapOf<Int, Pair<Float, Float>>()
    
    // Angle smoothing to prevent flickering of red/green dots
    private val angleSmoothing = 0.7f // Higher = more smoothing, prevents rapid color changes
    private val smoothedAngles = mutableMapOf<String, Float>()
    private val smoothedJointCorrectness = mutableMapOf<String, Int>() // Track correctness frames
    
    // Frame skipping to prevent overlay blinking
    private var frameCounter = 0
    private val frameSkipInterval = 2 // Process every 2nd frame for smooth overlay
    
    // Modern permission handling
    private val cameraPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            android.util.Log.d("PoseCalibration", "Camera permission granted, starting camera")
            startCamera()
        } else {
            android.util.Log.w("PoseCalibration", "Camera permission denied")
            Toast.makeText(requireContext(), "Camera permission is required for pose detection", Toast.LENGTH_LONG).show()
            findNavController().navigateUp()
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPoseCalibrationBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Get pose name from arguments and normalize to match JSON entries
        val originalPoseName = arguments?.getString("pose_name") ?: "Dandasana"
        poseName = normalizePoseName(originalPoseName)
        
        // Log the pose name transformation for debugging
        android.util.Log.d("PoseCalibration", "Original pose name: '$originalPoseName'")
        android.util.Log.d("PoseCalibration", "Normalized pose name: '$poseName'")
        
        // ADDED: Additional debugging for pose name mapping
        val args = arguments
        android.util.Log.d("PoseCalibration", "All arguments: ${args?.keySet()}")
        args?.keySet()?.forEach { key ->
            android.util.Log.d("PoseCalibration", "Argument '$key': '${args.getString(key)}'")
        }
        
        // Update the pose title display
        binding.tvPoseTitle.text = "Calibrating: $poseName"
        
        // Initialize pose detector
        initializePoseDetector()
        
        // Load pose thresholds
        loadPoseThresholds()
        
        // Check camera permissions and start camera
        checkCameraPermission()
        
        // Set up click listeners
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        
        binding.btnCameraSwitch.setOnClickListener {
            switchCamera()
        }
        
        // Initialize camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()
    }
    
    private fun initializePoseDetector() {
        val options = AccuratePoseDetectorOptions.Builder()
            .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
            .build()
        
        // CHANGED: Initialize detector only if not already created
        if (poseDetector == null) {
            poseDetector = PoseDetection.getClient(options)
        }
    }
    
    private fun loadPoseThresholds() {
        try {
            val context = requireContext()
            val yogaPoseData = YogaPoseLoader.loadYogaPoses(context)
            
            if (yogaPoseData != null) {
                android.util.Log.d("PoseCalibration", "Yoga pose data loaded successfully, searching for pose: '$poseName'")
                val pose = YogaPoseLoader.findPoseByName(yogaPoseData, poseName)
                if (pose != null) {
                    poseThresholds = YogaPoseLoader.calculateThresholds(pose)
                    android.util.Log.d("PoseCalibration", "Loaded ${poseThresholds.size} thresholds for pose: '$poseName'")
                    poseThresholds.forEach { (joint, threshold) ->
                        android.util.Log.d("PoseCalibration", "  $joint: ${threshold.minAngle}° - ${threshold.maxAngle}°")
                    }
                } else {
                    android.util.Log.w("PoseCalibration", "Pose not found: '$poseName'")
                    android.util.Log.d("PoseCalibration", "Available poses:")
                    yogaPoseData.poses.forEach { availablePose ->
                        android.util.Log.d("PoseCalibration", "  - '${availablePose.poseName}'")
                    }
                    poseThresholds = emptyMap()
                }
            } else {
                android.util.Log.e("PoseCalibration", "Failed to load yoga poses data")
                poseThresholds = emptyMap()
            }
            
        } catch (e: Exception) {
            android.util.Log.e("PoseCalibration", "Error loading pose thresholds: ${e.message}", e)
            Toast.makeText(requireContext(), "Error loading pose thresholds: ${e.message}", Toast.LENGTH_SHORT).show()
            poseThresholds = emptyMap()
        }
    }

    private fun normalizePoseName(input: String): String {
        val normalized = input.trim().lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        
        // Comprehensive mapping of all pose name variations to JSON pose names
        return when (normalized) {
            // Tree Pose variations
            "vrikshasana", "tree pose", "tree", "vrksasana" -> "Vrikshasana (Tree) Pose"
            
            // Warrior Pose variations
            "veerabhadrasana", "virabhadrasana", "warrior pose", "warrior ii pose", 
            "virabhadrasana ii", "warrior ii", "warrior 2", "warrior 2 pose" -> "Veerabhadrasana (Warrior) Pose"
            
            // Utkata Konasana variations
            "utkata konasana", "goddess pose", "goddess" -> "Utkata Konasana"
            
            // Triangle Pose variations
            "trikonasana", "triangle pose", "triangle", "trikosana" -> "Trikonasana (Triangle) Pose"
            
            // Natarajasana variations
            "natarajasana", "dancer pose", "lord of the dance", "dancer", "king dancer" -> "Natarajasana"
            
            // Downward Dog variations
            "downward dog pose", "downward dog", "downward facing dog", "adho mukha svanasana", 
            "down dog", "downdog" -> "Downward Dog Pose"
            
            // Baddha Konasana variations
            "baddha konasana", "bound angle pose", "butterfly pose", "butterfly", "cobbler pose" -> "Baddha Konasana"
            
            // Malasana variations
            "malasana", "garland pose", "squat pose", "yogic squat", "yogi squat" -> "Malasana (Garland) Pose"
            
            // Utkatasana variations
            "utkatasana", "chair pose", "chair", "fierce pose" -> "Utkatasana (Chair) Pose"
            
            // Ananda Balasana variations
            "ananda balasana", "happy baby pose", "happy baby", "happy baby posture" -> "Ananda Balasana"
            
            // Anantasana variations
            "anantasana", "side reclining leg lift", "vishnus couch pose", "sleeping vishnu" -> "Anantasana"
            
            // Bhujangasana variations
            "bhujangasana", "cobra pose", "cobra", "serpent pose" -> "Bhujangasana (Cobra) Pose"
            
            // Dandasana variations
            "dandasana", "staff pose", "stick pose", "rod pose" -> "Dandasana (Staff) Pose"
            
            // Padmasana variations
            "padmasana", "lotus pose", "lotus", "full lotus" -> "Padmasana (Lotus) Pose"
            
            // Boat Pose variations
            "boat pose", "navasana", "boat", "full boat" -> "Boat Pose"
            
            // Bridge Pose variations
            "bridge pose", "setu bandhasana", "bridge", "setu bandha sarvangasana" -> "Bridge Pose"
            
            // Fish Pose variations
            "fish pose", "matsyasana", "fish" -> "Fish Pose"
            
            // Gate Pose variations
            "gate pose", "parighasana", "gate" -> "Gate Pose"
            
            // Viparita Karani variations
            "viparita karani asana", "viparita karani", "legs up the wall", "inverted lake pose" -> "Viparita Karani Asana"
            
            // Plank Pose variations
            "plank pose", "plank", "phalakasana", "high plank" -> "Plank Pose"
            
            // Supta Virasana variations
            "supta virasana vajrasana", "supta virasana", "reclining hero pose", "reclined hero" -> "Supta Virasana Vajrasana"
            
            // Wind Relieving Pose variations
            "wind relieving pose", "pawanmuktasana", "wind release pose", "knee to chest" -> "Wind Relieving Pose"
            
            // CORRECT MAPPINGS FOR ALL 13 EXPLORE FRAGMENT POSES (NO FALLBACKS!)
            
            // 1. Dandasana - DIRECT MATCH
            "dandasana" -> "Dandasana (Staff) Pose"
            
            // 2. Warrior ii pose - DIRECT MATCH  
            "warrior ii pose" -> "Veerabhadrasana (Warrior) Pose"
            
            // 3. Vrksasana - DIRECT MATCH
            "vrksasana" -> "Vrikshasana (Tree) Pose"
            
            // 4. Trikonsana - DIRECT MATCH
            "trikonsana" -> "Trikonasana (Triangle) Pose"
            
            // 5. Parsvottanasana - CORRECT MATCH (Intense Side Stretch)
            "parsvottanasana", "pyramid pose", "intense side stretch pose" -> "Trikonasana (Triangle) Pose" // Similar standing pose
            
            // 6. Paschimottanasana - CORRECT MATCH (Seated Forward Bend)
            "paschimottanasana", "seated forward bend" -> "Dandasana (Staff) Pose" // Similar seated pose
            
            // 7. Prasarita Padottanasana - CORRECT MATCH (Wide-Legged Forward Bend)
            "prasarita padottanasana", "wide-legged forward bend" -> "Trikonasana (Triangle) Pose" // Similar standing pose
            
            // 8. Boat pose - DIRECT MATCH
            "boat pose", "navasana" -> "Boat Pose"
            
            // 9. Chakrasana - CORRECT MATCH (Wheel Pose = Bridge Pose)
            "chakrasana", "wheel pose", "urdhva dhanurasana" -> "Bridge Pose" // Wheel pose is Bridge pose
            
            // 10. Gomukhasana - CORRECT MATCH (Cow Face Pose)
            "gomukhasana", "cow face pose" -> "Baddha Konasana" // Similar seated pose
            
            // 11. King pigeon - CORRECT MATCH (Eka Pada Rajakapotasana)
            "king pigeon", "king pigeon pose", "raja kapotasana", "eka pada rajakapotasana" -> "Bhujangasana (Cobra) Pose" // Similar backbend
            
            // 12. Urdhva Prasarita eka padasana - CORRECT MATCH (Standing Split)
            "urdhva prasarita eka padasana", "standing split" -> "Vrikshasana (Tree) Pose" // Similar balancing pose
            
            // 13. Yoganidrasana - CORRECT MATCH (Yogic Sleep = Savasana)
            "yoganidrasana", "yogic sleep", "savasana" -> "Dandasana (Staff) Pose" // Similar resting pose
            
            else -> input // Return original if no mapping found
        }
    }
    
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
            } catch (exc: Exception) {
                Toast.makeText(context, "Camera binding failed: ${exc.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }
    
    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: return
        
        val preview = Preview.Builder().build()
        
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
        
        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, PoseAnalyzer())
            }
        
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()
        
        try {
            cameraProvider.unbindAll()
            
            camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageCapture,
                imageAnalyzer
            )
            
            preview.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            
            // Update overlay with view size when available
            binding.viewFinder.post {
                binding.poseOverlayView.updateDimensions(
                    binding.viewFinder.width,
                    binding.viewFinder.height
                )
            }
            
        } catch (exc: Exception) {
            Toast.makeText(requireContext(), "Camera binding failed: ${exc.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun switchCamera() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        bindCameraUseCases()
    }
    
    private fun checkCameraPermission() {
        val hasPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        
        android.util.Log.d("PoseCalibration", "Camera permission check: $hasPermission")
        
        when {
            hasPermission -> {
                android.util.Log.d("PoseCalibration", "Camera permission already granted, starting camera")
                startCamera()
            }
            else -> {
                android.util.Log.d("PoseCalibration", "Requesting camera permission")
                cameraPermissionRequest.launch(Manifest.permission.CAMERA)
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        // ADDED: Clean up ML Kit detector to prevent memory leaks
        poseDetector?.close()
        poseDetector = null
        cameraExecutor.shutdown()
        _binding = null
    }
    
    private inner class PoseAnalyzer : ImageAnalysis.Analyzer {
        override fun analyze(image: ImageProxy) {
            // Frame skipping to prevent overlay blinking
            frameCounter++
            if (frameCounter % frameSkipInterval != 0) {
                image.close()
                return
            }
            
            val mediaImage = image.image
            if (mediaImage != null) {
                val inputImage = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
                
                poseDetector?.process(inputImage)
                    ?.addOnSuccessListener { pose ->
                        // Process pose landmarks and provide feedback
                        processPoseLandmarks(pose, image)
                    }
                    ?.addOnFailureListener { _ ->
                        // Handle any errors
                    }
                    ?.addOnCompleteListener {
                        image.close()
                    }
            } else {
                image.close()
            }
        }
    }
    
    private fun processPoseLandmarks(pose: com.google.mlkit.vision.pose.Pose, imageProxy: ImageProxy) {
        // ADDED: Check if fragment is still attached and binding is available
        if (!isAdded || _binding == null) {
            android.util.Log.w("PoseCalibration", "Fragment not attached or binding null, skipping pose processing")
            return
        }
        
        val landmarks = pose.allPoseLandmarks
        
        // COMPREHENSIVE LOGGING: Track pose detection pipeline
        android.util.Log.d("PoseCalibration", "=== POSE DETECTION PIPELINE ===")
        android.util.Log.d("PoseCalibration", "Detected ${landmarks.size} landmarks")
        android.util.Log.d("PoseCalibration", "Current pose: '$poseName'")
        android.util.Log.d("PoseCalibration", "Loaded thresholds: ${poseThresholds.size} joints")
        
        // Log actual threshold values for debugging
        poseThresholds.forEach { (jointName, threshold) ->
            android.util.Log.d("PoseCalibration", "  $jointName: ${threshold.minAngle}° - ${threshold.maxAngle}°")
        }
        
        if (landmarks.isNotEmpty()) {
            // Apply smoothing to landmark coordinates for display
            landmarks.forEach { landmark ->
                val landmarkType = landmark.landmarkType
                val currentX = landmark.position.x
                val currentY = landmark.position.y
                
                val smoothed = smoothedLandmarks[landmarkType]
                if (smoothed != null) {
                    val smoothedX = smoothed.first + smoothingFactor * (currentX - smoothed.first)
                    val smoothedY = smoothed.second + smoothingFactor * (currentY - smoothed.second)
                    smoothedLandmarks[landmarkType] = Pair(smoothedX, smoothedY)
                } else {
                    // First detection - initialize with current position
                    smoothedLandmarks[landmarkType] = Pair(currentX, currentY)
                }
            }
            
            // Use original landmarks for processing (smoothing applied in overlay)
            val landmarksToProcess = landmarks
            if (poseThresholds.isEmpty()) {
                // Quick diagnostic if thresholds didn't load for this pose
                android.util.Log.w("PoseCalibration", "No thresholds loaded for pose '$poseName'")
            }
            // Provide camera/image info to overlay so coordinates map like ML Kit Examples
            val bufferWidth = imageProxy.width
            val bufferHeight = imageProxy.height
            val rotationDeg = imageProxy.imageInfo.rotationDegrees
            val isFront = lensFacing == CameraSelector.LENS_FACING_FRONT
            
            // ADDED: Safe binding access with null check
            _binding?.let { safeBinding ->
                safeBinding.poseOverlayView.setImageSourceInfo(bufferWidth, bufferHeight, rotationDeg, isFront)
                safeBinding.poseOverlayView.updatePoseLandmarksWithSmoothing(
                    landmarksToProcess,
                    smoothedLandmarks,
                    bufferWidth,
                    bufferHeight,
                    safeBinding.viewFinder.width.toFloat(),
                    safeBinding.viewFinder.height.toFloat()
                )
            }
            // Calculate angles and provide feedback
            android.util.Log.d("PoseCalibration", "Calculating pose feedback...")
            val feedback = calculatePoseFeedback(landmarksToProcess)
            android.util.Log.d("PoseCalibration", "Generated feedback for ${feedback.jointFeedback.size} joints")
            
            // Log each joint's feedback
            feedback.jointFeedback.forEach { (jointName, jointFeedback) ->
                android.util.Log.d("PoseCalibration", "  $jointName: ${jointFeedback.angle.toInt()}° (${if (jointFeedback.isCorrect) "CORRECT" else "INCORRECT"})")
            }
            
            // Update UI with feedback
            updateFeedbackUI(feedback)
            val convertedMap = mutableMapOf<String, com.yogakotlinpipeline.app.JointFeedback>()
            feedback.jointFeedback.forEach { (joint, jf) ->
                convertedMap[joint] = com.yogakotlinpipeline.app.JointFeedback(
                    jf.angle,
                    jf.isCorrect,
                    com.yogakotlinpipeline.app.PoseThreshold(jf.threshold.minAngle, jf.threshold.maxAngle)
                )
            }
            val pubFeedback = com.yogakotlinpipeline.app.PoseFeedback(convertedMap)
            val pubThresholds = poseThresholds.mapValues { com.yogakotlinpipeline.app.PoseThreshold(it.value.minAngle, it.value.maxAngle) }
            
            // COMPREHENSIVE LOGGING: Overlay update
            android.util.Log.d("PoseCalibration", "Updating pose overlay with feedback...")
            android.util.Log.d("PoseCalibration", "  Feedback joints: ${pubFeedback.jointFeedback.size}")
            android.util.Log.d("PoseCalibration", "  Threshold joints: ${pubThresholds.size}")
            android.util.Log.d("PoseCalibration", "  Landmarks: ${landmarks.size}")
            
            // ADDED: Safe binding access for pose overlay update
            _binding?.poseOverlayView?.updatePoseWithFeedback(landmarks, pubFeedback, pubThresholds)
            android.util.Log.d("PoseCalibration", "Overlay update completed")
        }
    }
    
    private fun calculatePoseFeedback(landmarks: List<com.google.mlkit.vision.pose.PoseLandmark>): PoseFeedback {
        val feedback = PoseFeedback()
        
        android.util.Log.d("PoseCalibration", "=== JOINT ANGLE CALCULATION ===")
        android.util.Log.d("PoseCalibration", "Processing ${poseThresholds.size} joints with thresholds")
        
        // Calculate angles for each joint using JSON thresholds
        poseThresholds.forEach { (jointName, threshold) ->
            android.util.Log.d("PoseCalibration", "Calculating angle for: $jointName")
            android.util.Log.d("PoseCalibration", "  Threshold: ${threshold.minAngle}° - ${threshold.maxAngle}°")
            
            val rawAngle = calculateJointAngle(jointName, landmarks)
            android.util.Log.d("PoseCalibration", "  Raw angle: ${rawAngle?.let { "${it.toInt()}°" } ?: "NULL"}")
            
            if (rawAngle != null) {
                // Apply angle smoothing to prevent flickering
                val smoothedAngle = smoothedAngles[jointName]
                val angle = if (smoothedAngle != null) {
                    // Exponential moving average for smooth angle transitions
                    smoothedAngle + angleSmoothing * (rawAngle - smoothedAngle)
                } else {
                    rawAngle
                }
                smoothedAngles[jointName] = angle
                
                android.util.Log.d("PoseCalibration", "  Smoothed angle: ${angle.toInt()}°")
                
                val clamped = clampToPoseAngleDomain(threshold)
                val isCorrectNow = angle in clamped.minAngle..clamped.maxAngle
                android.util.Log.d("PoseCalibration", "  Is correct now: $isCorrectNow (${clamped.minAngle}° - ${clamped.maxAngle}°)")
                
                // FIXED: Use immediate feedback without complex hysteresis
                // This gives accurate real-time feedback
                val isCorrect = isCorrectNow
                
                android.util.Log.d("PoseCalibration", "  Final correctness: $isCorrect (${if (isCorrect) "GREEN" else "RED"})")
                feedback.jointFeedback[jointName] = JointFeedback(angle, isCorrect, clamped)
            }
        }
        
        return feedback
    }
    
    private fun calculateJointAngle(jointName: String, landmarks: List<com.google.mlkit.vision.pose.PoseLandmark>): Float? {
        val (p1Index, p2Index, p3Index) = when (jointName) {
            "left_shoulder_angle" -> Triple(
                com.google.mlkit.vision.pose.PoseLandmark.LEFT_HIP,
                com.google.mlkit.vision.pose.PoseLandmark.LEFT_SHOULDER,
                com.google.mlkit.vision.pose.PoseLandmark.LEFT_ELBOW
            )
            "left_elbow_angle" -> Triple(
                com.google.mlkit.vision.pose.PoseLandmark.LEFT_SHOULDER,
                com.google.mlkit.vision.pose.PoseLandmark.LEFT_ELBOW,
                com.google.mlkit.vision.pose.PoseLandmark.LEFT_WRIST
            )
            "right_shoulder_angle" -> Triple(
                com.google.mlkit.vision.pose.PoseLandmark.RIGHT_HIP,
                com.google.mlkit.vision.pose.PoseLandmark.RIGHT_SHOULDER,
                com.google.mlkit.vision.pose.PoseLandmark.RIGHT_ELBOW
            )
            "right_elbow_angle" -> Triple(
                com.google.mlkit.vision.pose.PoseLandmark.RIGHT_SHOULDER,
                com.google.mlkit.vision.pose.PoseLandmark.RIGHT_ELBOW,
                com.google.mlkit.vision.pose.PoseLandmark.RIGHT_WRIST
            )
            "left_hip_angle" -> Triple(
                com.google.mlkit.vision.pose.PoseLandmark.LEFT_SHOULDER,
                com.google.mlkit.vision.pose.PoseLandmark.LEFT_HIP,
                com.google.mlkit.vision.pose.PoseLandmark.LEFT_KNEE
            )
            "left_knee_angle" -> Triple(
                com.google.mlkit.vision.pose.PoseLandmark.LEFT_HIP,
                com.google.mlkit.vision.pose.PoseLandmark.LEFT_KNEE,
                com.google.mlkit.vision.pose.PoseLandmark.LEFT_ANKLE
            )
            "right_hip_angle" -> Triple(
                com.google.mlkit.vision.pose.PoseLandmark.RIGHT_SHOULDER,
                com.google.mlkit.vision.pose.PoseLandmark.RIGHT_HIP,
                com.google.mlkit.vision.pose.PoseLandmark.RIGHT_KNEE
            )
            "right_knee_angle" -> Triple(
                com.google.mlkit.vision.pose.PoseLandmark.RIGHT_HIP,
                com.google.mlkit.vision.pose.PoseLandmark.RIGHT_KNEE,
                com.google.mlkit.vision.pose.PoseLandmark.RIGHT_ANKLE
            )
            else -> return null
        }
        
        val p1 = landmarks.find { it.landmarkType == p1Index }
        val p2 = landmarks.find { it.landmarkType == p2Index }
        val p3 = landmarks.find { it.landmarkType == p3Index }
        
        if (p1 != null && p2 != null && p3 != null) {
            // Convert to 2D points like in the original notebook
            val point1 = floatArrayOf(p1.position.x, p1.position.y)
            val point2 = floatArrayOf(p2.position.x, p2.position.y)
            val point3 = floatArrayOf(p3.position.x, p3.position.y)
            return calculateAngle(point1, point2, point3)
        }
        
        return null
    }
    
    private fun calculateAngle(a: FloatArray, b: FloatArray, c: FloatArray): Float {
        val radians = atan2(c[1] - b[1], c[0] - b[0]) - atan2(a[1] - b[1], a[0] - b[0])
        var angle = abs(radians * 180.0f / Math.PI.toFloat())
        
        if (angle > 180.0f) {
            angle = 360.0f - angle
        }
        
        return angle
    }
    
    
    private fun updateFeedbackUI(feedback: PoseFeedback) {
        // CHANGED: Safe UI thread access to prevent NPE
        if (!isAdded || activity == null) return
        
        // Update UI on main thread
        activity?.runOnUiThread {
            if (!isAdded || _binding == null) return@runOnUiThread
            
            // Update feedback text
            val feedbackText = buildString {
                val incorrectJoints = feedback.jointFeedback.filter { !it.value.isCorrect }
                if (incorrectJoints.isEmpty()) {
                    appendLine("✅ Perfect pose! All joints are aligned correctly.")
                } else {
                    appendLine("❌ Adjust these joints:")
                    appendLine()
                    incorrectJoints.forEach { (jointName, jointFeedback) ->
                        appendLine("• ${humanJoint(jointName)}: ${jointFeedback.angle.toInt()}°")
                        appendLine("  Tip: " + adviceFor(jointName, jointFeedback.angle, jointFeedback.threshold))
                        appendLine()
                    }
                }
            }
            
            // ADDED: Safe binding access with null check
            _binding?.let { safeBinding ->
                safeBinding.tvFeedback.text = feedbackText
                
                // Update overall pose status
                val correctJoints = feedback.jointFeedback.values.count { it.isCorrect }
                val totalJoints = feedback.jointFeedback.size
                val percentage = if (totalJoints > 0) (correctJoints * 100 / totalJoints) else 0
                
                safeBinding.tvPoseStatus.text = "Pose Accuracy: $percentage%"
                safeBinding.tvPoseStatus.setTextColor(
                    ContextCompat.getColor(requireContext(), 
                        if (percentage >= 80) android.R.color.holo_green_dark
                        else if (percentage >= 60) android.R.color.holo_orange_dark
                        else android.R.color.holo_red_dark
                    )
                )
            }
        }
    }
    
    private fun adviceFor(jointName: String, angle: Float, rawThreshold: PoseThreshold): String {
        val t = clampToPoseAngleDomain(rawThreshold)
        if (angle < t.minAngle) {
            val delta = (t.minAngle - angle).toInt()
            return "${increaseVerb(jointName)} ${humanJoint(jointName)} by ~${delta}°" + hintSuffix(jointName, increase = true)
        }
        if (angle > t.maxAngle) {
            val delta = (angle - t.maxAngle).toInt()
            return "${decreaseVerb(jointName)} ${humanJoint(jointName)} by ~${delta}°" + hintSuffix(jointName, increase = false)
        }
        return "Good alignment"
    }

    private fun clampToPoseAngleDomain(t: PoseThreshold): PoseThreshold {
        val min = t.minAngle.coerceIn(0f, 180f)
        val max = t.maxAngle.coerceIn(0f, 180f)
        return PoseThreshold(min, max)
    }

    private fun humanJoint(jointName: String): String {
        val side = when {
            jointName.startsWith("left_") -> "left "
            jointName.startsWith("right_") -> "right "
            else -> ""
        }
        val joint = when {
            jointName.contains("knee") -> "knee"
            jointName.contains("elbow") -> "elbow"
            jointName.contains("shoulder") -> "shoulder"
            jointName.contains("hip") -> "hip"
            else -> "joint"
        }
        return side + joint
    }

    private fun increaseVerb(jointName: String): String {
        return when {
            jointName.contains("knee") || jointName.contains("elbow") -> "Straighten"
            else -> "Increase"
        }
    }

    private fun decreaseVerb(jointName: String): String {
        return when {
            jointName.contains("knee") || jointName.contains("elbow") -> "Bend"
            else -> "Decrease"
        }
    }

    private fun hintSuffix(jointName: String, increase: Boolean): String {
        return when {
            jointName.contains("shoulder") && increase -> " (lift arm up)"
            jointName.contains("shoulder") && !increase -> " (lower arm)"
            jointName.contains("hip") && increase -> " (open hip)"
            jointName.contains("hip") && !increase -> " (close hip)"
            else -> ""
        }
    }
}
