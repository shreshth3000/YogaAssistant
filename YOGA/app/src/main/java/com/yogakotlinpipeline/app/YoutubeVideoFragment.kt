package com.yogakotlinpipeline.app

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.yogakotlinpipeline.app.databinding.FragmentYoutubeVideoBinding

class YoutubeVideoFragment : Fragment() {
    
    private var _binding: FragmentYoutubeVideoBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentYoutubeVideoBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        Log.d("YoutubeVideoFragment", "onViewCreated called")
        
        // Get pose information from arguments
        val poseName = arguments?.getString("pose_name") ?: "Dandasana"
        val poseDisplayName = arguments?.getString("pose_display_name") ?: "Staff Pose"
        val poseDescription = arguments?.getString("pose_description") ?: ""
        val poseDifficulty = arguments?.getString("pose_difficulty") ?: "Beginner"
        
        Log.d("YoutubeVideoFragment", "Pose: $poseDisplayName ($poseName)")
        
        // Set the title
        binding.tvPoseTitle.text = poseDisplayName
        binding.tvSanskritName.text = poseName
        binding.tvPoseDescription.text = poseDescription
        binding.tvPoseDifficulty.text = poseDifficulty
        
        // Get video ID and optional start time for the pose
        val (videoId, startSeconds) = getVideoInfoForPose(poseName)
        Log.d("YoutubeVideoFragment", "Video ID: $videoId start: $startSeconds")
        
        // Load YouTube video directly
        loadYouTubeVideo(videoId, startSeconds)
        
        // Add a fallback button in case video doesn't load
        binding.btnSkipVideo.text = "Watched Tutorial? Let's Do It"
        
        // Set up back button
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        
        // Set up action buttons
        setupActionButtons()
        
        // Set up bottom navigation
        setupBottomNavigation()
    }
    
    private fun getVideoInfoForPose(poseName: String): Pair<String, Int> {
        Log.d("YoutubeVideoFragment", "Getting video info for pose: '$poseName' (lowercase: '${poseName.lowercase()}')")
        val url = when (poseName.lowercase()) {
            // Provided links
            "gomukh", "gomukhasana", "cow face pose" -> "https://www.youtube.com/watch?v=CwUw_2HpTdM"
            "vrikshasan", "vrksasana", "tree pose" -> "https://www.youtube.com/watch?v=SPJYQDkaZ3w"
            "urdhva prasarita eka padasana", "standing split", "standing splits" -> "https://www.youtube.com/watch?v=c30T6q7AVsU"
            "warrior pose 2", "warrior ii pose", "virabhadrasana ii", "warrior 2" -> "https://youtu.be/azCEB_BDWxg?t=106"
            "paschimottanasana", "pashimottanasana", "seated forward bend" -> "https://www.youtube.com/watch?v=qsJMLBCvcU0"
            "boat pose", "navasana", "boat", "naukasana", "naukasan" -> "https://www.youtube.com/watch?v=SfzxXr-If68"
            "dandasana", "staff pose" -> "https://www.youtube.com/watch?v=yIt2INAcVeY"
            "trikonasan", "trikonasana", "triangle pose" -> "https://www.youtube.com/shorts/wRqvn2N9V7g"
            "chakrasan", "chakrasana", "wheel pose" -> "https://www.youtube.com/shorts/C5clWWOm-Yc"
            "parsvottanasana", "pyramid pose" -> "https://www.youtube.com/shorts/cQJTNwWEH-Y"
            "yogaindrasana", "yoganidrasana" -> "https://www.youtube.com/shorts/htGkI9ALWow"
            "king pigeon", "king pigeon pose", "eka pada rajakapotasana", "rajakapotasana", "pigeon pose" -> "https://www.youtube.com/shorts/TfR3e-5PGJU"
            "prasarita padottanasana", "wide-legged forward bend" -> "https://www.youtube.com/watch?v=cnyUaieabic"
            else -> "https://www.youtube.com/watch?v=CwUw_2HpTdM"
        }
        Log.d("YoutubeVideoFragment", "Selected URL for pose '$poseName': $url")
        return parseYouTubeUrl(url)
    }

    private fun parseYouTubeUrl(url: String): Pair<String, Int> {
        return try {
            val lower = url.lowercase()
            var videoId = ""
            var startSeconds = 0

            // Extract start time (t= or start=) if present
            val uri = android.net.Uri.parse(url)
            val tParam = uri.getQueryParameter("t")
            val startParam = uri.getQueryParameter("start")
            val parsedT = tParam?.toIntOrNull()
            val parsedStart = startParam?.toIntOrNull()
            if (parsedT != null && parsedT >= 0) startSeconds = parsedT
            if (parsedStart != null && parsedStart >= 0) startSeconds = parsedStart

            // Extract ID patterns
            when {
                // Standard watch URL
                lower.contains("youtube.com/watch") -> {
                    videoId = uri.getQueryParameter("v") ?: ""
                }
                // Short link youtu.be/<id>
                lower.contains("youtu.be/") -> {
                    val path = uri.path ?: ""
                    videoId = path.trim('/').split('/').firstOrNull().orEmpty()
                }
                // Shorts URL youtube.com/shorts/<id>
                lower.contains("youtube.com/shorts/") -> {
                    val segments = uri.pathSegments
                    val idx = segments.indexOf("shorts")
                    if (idx >= 0 && idx + 1 < segments.size) {
                        videoId = segments[idx + 1]
                    }
                }
            }

            if (videoId.isBlank()) videoId = "CwUw_2HpTdM"
            videoId to startSeconds
        } catch (_: Exception) {
            "CwUw_2HpTdM" to 0
        }
    }
    
    private fun loadTestPage() {
        _binding?.webViewVideo?.loadUrl("file:///android_asset/test_video.html")
    }
    
    private fun loadYouTubeVideo(videoId: String, startSeconds: Int = 0) {
        val startParam = if (startSeconds > 0) "&start=$startSeconds" else ""
        val embedUrl = "https://www.youtube.com/embed/$videoId?autoplay=1&rel=0&showinfo=0&enablejsapi=1&origin=https://www.youtube.com&playsinline=1$startParam"
        
        _binding?.webViewVideo?.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.allowContentAccess = true
            settings.allowFileAccess = true
            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            settings.loadsImagesAutomatically = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.setSupportZoom(false)
            settings.builtInZoomControls = false
            settings.displayZoomControls = false
            
            // Add JavaScript interface for video completion detection
            addJavascriptInterface(object {
                @android.webkit.JavascriptInterface
                fun onVideoCompleted() {
                    this@YoutubeVideoFragment.onVideoCompleted()
                }
            }, "Android")
            
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    _binding?.progressLoading?.visibility = View.VISIBLE
                    Log.d("YoutubeVideoFragment", "Starting to load video: $url")
                }
                
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d("YoutubeVideoFragment", "YouTube video loaded successfully")
                    _binding?.progressLoading?.visibility = View.GONE
                    // Inject JavaScript to detect video completion
                    if (_binding != null && isAdded) {
                        injectVideoCompletionDetection()
                    }
                }
                
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    // Allow YouTube URLs to load
                    Log.d("YoutubeVideoFragment", "Loading URL: $url")
                    return false
                }
                
                override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                    super.onReceivedError(view, errorCode, description, failingUrl)
                    Log.e("YoutubeVideoFragment", "WebView error: $errorCode - $description")
                    _binding?.progressLoading?.visibility = View.GONE
                    android.widget.Toast.makeText(context, "Failed to load video: $description", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            
            loadUrl(embedUrl)
        }
    }
    
    private fun injectVideoCompletionDetection() {
        val javascript = """
            javascript:
            (function() {
                // Function to check if video is completed
                function checkVideoCompletion() {
                    var video = document.querySelector('video');
                    if (video) {
                        // Check if video has ended
                        if (video.ended || video.currentTime >= video.duration - 1) {
                            // Video is completed
                            window.Android.onVideoCompleted();
                            return;
                        }
                        
                        // Check for YouTube's end screen
                        var endScreen = document.querySelector('.ytp-endscreen-content');
                        if (endScreen) {
                            window.Android.onVideoCompleted();
                            return;
                        }
                    }
                    
                    // Check again in 2 seconds
                    setTimeout(checkVideoCompletion, 2000);
                }
                
                // Start checking after a delay to ensure video is loaded
                setTimeout(checkVideoCompletion, 3000);
            })();
        """.trimIndent()
        
        _binding?.webViewVideo?.loadUrl(javascript)
    }
    
    private fun setupActionButtons() {
        // Skip Video Button
        binding.btnSkipVideo.setOnClickListener {
            Log.d("YoutubeVideoFragment", "Skip video button clicked")
            // Navigate directly to pose correction
            navigateToPoseCorrection()
        }
        
        // Start Pose Correction Button
        binding.btnStartPoseCorrection.setOnClickListener {
            Log.d("YoutubeVideoFragment", "Start pose correction button clicked")
            navigateToPoseCorrection()
        }
        
        // Initially hide the "Start Practice" button until video completes
        binding.btnStartPoseCorrection.visibility = View.GONE
    }
    
    // This method will be called from JavaScript when video completes
    fun onVideoCompleted() {
        if (_binding == null || !isAdded || viewLifecycleOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.DESTROYED) && !viewLifecycleOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) return
        activity?.runOnUiThread {
            Log.d("YoutubeVideoFragment", "Video completed!")
            // Show the "Start Practice" button and make it more prominent
            _binding?.btnStartPoseCorrection?.visibility = View.VISIBLE
            _binding?.btnStartPoseCorrection?.text = "Start Practice Now!"
            _binding?.btnStartPoseCorrection?.setBackgroundResource(R.drawable.neumorphic_button_primary)
            
            // Show a completion message
            android.widget.Toast.makeText(
                context,
                "Video completed! Ready to practice?",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    private fun navigateToPoseCorrection() {
        val poseName = arguments?.getString("pose_name") ?: "Dandasana"
        val poseDisplayName = arguments?.getString("pose_display_name") ?: "Staff Pose"
        val poseDescription = arguments?.getString("pose_description") ?: ""
        val poseDifficulty = arguments?.getString("pose_difficulty") ?: "Beginner"
        
        val bundle = Bundle().apply {
            putString("pose_name", poseName)
            putString("pose_display_name", poseDisplayName)
            putString("pose_description", poseDescription)
            putString("pose_difficulty", poseDifficulty)
        }
        
        try {
            findNavController().navigate(R.id.action_youtubeVideoFragment_to_poseCalibrationFragment, bundle)
            Log.d("YoutubeVideoFragment", "Navigation to pose correction successful!")
        } catch (e: Exception) {
            Log.e("YoutubeVideoFragment", "Navigation to pose correction failed: ${e.message}", e)
            // Fallback to home screen
            findNavController().navigate(R.id.action_youtubeVideoFragment_to_inside1Fragment)
        }
    }
    
    private fun setupBottomNavigation() {
        binding.btnHome.setOnClickListener {
            findNavController().navigate(R.id.action_youtubeVideoFragment_to_inside1Fragment)
        }
        
        binding.btnAi.setOnClickListener {
            findNavController().navigate(R.id.action_youtubeVideoFragment_to_inside3Fragment)
        }
        
        binding.btnProgress.setOnClickListener {
            findNavController().navigate(R.id.action_youtubeVideoFragment_to_inside4Fragment)
        }
        
        binding.btnProfile.setOnClickListener {
            findNavController().navigate(R.id.action_youtubeVideoFragment_to_profileFragment)
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        try {
            _binding?.webViewVideo?.apply {
                stopLoading()
                webViewClient = WebViewClient()
                loadUrl("about:blank")
                removeAllViews()
                destroy()
            }
        } catch (_: Throwable) {}
        _binding = null
    }
}
