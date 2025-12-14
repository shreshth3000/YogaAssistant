package com.yogakotlinpipeline.app

import android.Manifest
import android.content.ComponentCallbacks2
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.findFragment
import androidx.navigation.fragment.findNavController
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import com.yogakotlinpipeline.app.databinding.FragmentPoseDetectionBinding
import java.util.concurrent.Executors
import kotlin.math.*

class PoseDetectionFragment : Fragment() {

    private var _binding: FragmentPoseDetectionBinding? = null
    private val binding get() = _binding!!

    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    // ADDED: Frame counter for skipping frames to reduce CPU load
    private var frameCounter = 0
    // ADDED: Adaptive frame skipping based on memory pressure
    private var frameSkipInterval = 3 // Start with every 3rd frame
    // ADDED: Reuse detector instance to prevent memory leaks
    private var poseDetector: com.google.mlkit.vision.pose.PoseDetector? = null
    // ADDED: Reuse executor to prevent thread leaks
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    
    // ADDED: Pose name and thresholds for joint feedback
    private var poseName: String = "Dandasana"
    private var poseThresholds: Map<String, PoseThreshold> = emptyMap()

    private val cameraPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("PoseDetection", "Camera permission granted, starting camera")
            startCamera()
        } else {
            Log.w("PoseDetection", "Camera permission denied")
            Toast.makeText(requireContext(), "Camera permission is required for pose detection", Toast.LENGTH_LONG).show()
            findNavController().navigateUp()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPoseDetectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Get pose name from arguments
        poseName = arguments?.getString("pose_name") ?: "Dandasana"
        poseName = normalizePoseName(poseName)
        
        // Load pose thresholds
        loadPoseThresholds()
        
        setupClickListeners()
        checkCameraPermission()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSwitchCamera.setOnClickListener {
            switchCamera()
        }

        binding.btnStopSession.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun checkCameraPermission() {
        val hasPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        
        Log.d("PoseDetection", "Camera permission check: $hasPermission")
        
        when {
            hasPermission -> {
                Log.d("PoseDetection", "Camera permission already granted, starting camera")
                startCamera()
            }
            else -> {
                Log.d("PoseDetection", "Requesting camera permission")
                cameraPermissionRequest.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun startCamera() {
        Log.d("PoseDetection", "Starting camera initialization")
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                Log.d("PoseDetection", "Camera provider obtained, binding use cases")
                bindCameraUseCases()
            } catch (exc: Exception) {
                Log.e("PoseDetection", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: return

        Log.d("PoseDetection", "Binding camera use cases")

        val preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .build()

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
            processImage(imageProxy)
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
                imageAnalysis
            )

            preview.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            Log.d("PoseDetection", "Camera successfully bound and preview started")
        } catch (exc: Exception) {
            Log.e("PoseDetection", "Use case binding failed", exc)
        }
    }

    private fun processImage(imageProxy: ImageProxy) {
        // ADDED: Adaptive frame skipping based on memory pressure
        frameCounter++
        if (frameCounter % frameSkipInterval != 0) {
            imageProxy.close()
            return
        }
        
        val mediaImage = imageProxy.image ?: return
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        // CHANGED: Reuse detector instance instead of creating new one
        if (poseDetector == null) {
            val poseDetectorOptions = AccuratePoseDetectorOptions.Builder()
                .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
                .setPreferredHardwareConfigs(AccuratePoseDetectorOptions.CPU_GPU)
                .build()
            poseDetector = PoseDetection.getClient(poseDetectorOptions)
        }

        poseDetector!!.process(image)
            .addOnSuccessListener { pose ->
                val landmarks = pose.allPoseLandmarks
                // CHANGED: Safe UI thread access to prevent Fragment lifecycle crashes
                if (!isAdded || activity == null) return@addOnSuccessListener
                activity?.runOnUiThread {
                    if (!isAdded || _binding == null) return@runOnUiThread
                    // Use the working approach from ML Kit Examples:
                    // Pass actual camera image dimensions and screen dimensions
                    binding.poseOverlay.updatePoseLandmarks(
                        landmarks,
                        mediaImage.width,    // Actual camera image width
                        mediaImage.height,   // Actual camera image height
                        resources.displayMetrics.widthPixels.toFloat(),  // Screen width
                        resources.displayMetrics.heightPixels.toFloat()  // Screen height
                    )
                    binding.tvLandmarkCount.text = "Detected: ${landmarks.size} landmarks"
                }
            }
            .addOnFailureListener { e ->
                Log.e("PoseDetection", "Detection failed", e)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
    
    // ADDED: Method to adjust frame skipping based on memory pressure
    fun adjustFrameSkipInterval(memoryPressure: Int) {
        frameSkipInterval = when (memoryPressure) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE -> 4 // Skip more frames
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> 6
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> 10 // Skip many frames
            else -> 3 // Normal operation
        }
        Log.d("PoseDetection", "Adjusted frame skip interval to: $frameSkipInterval")
    }

    private fun switchCamera() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        bindCameraUseCases()
    }

    private fun loadPoseThresholds() {
        try {
            val context = requireContext()
            val yogaPoseData = YogaPoseLoader.loadYogaPoses(context)
            
            if (yogaPoseData != null) {
                val pose = YogaPoseLoader.findPoseByName(yogaPoseData, poseName)
                if (pose != null) {
                    poseThresholds = YogaPoseLoader.calculateThresholds(pose)
                    Log.d("PoseDetection", "Loaded thresholds for pose: $poseName")
                } else {
                    Log.w("PoseDetection", "Pose not found: $poseName")
                    poseThresholds = emptyMap()
                }
            } else {
                Log.e("PoseDetection", "Failed to load yoga poses data")
                poseThresholds = emptyMap()
            }
            
        } catch (e: Exception) {
            Log.e("PoseDetection", "Error loading pose thresholds: ${e.message}", e)
            Toast.makeText(requireContext(), "Error loading pose thresholds: ${e.message}", Toast.LENGTH_SHORT).show()
            poseThresholds = emptyMap()
        }
    }
    
    private fun normalizePoseName(input: String): String {
        return input.trim().lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // ADDED: Clean up detector to prevent memory leaks
        poseDetector?.close()
        poseDetector = null
        // ADDED: Clean up executor to prevent thread leaks
        cameraExecutor.shutdown()
        _binding = null
    }
}
