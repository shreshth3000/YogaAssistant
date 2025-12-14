package com.yogakotlinpipeline.app

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.yogakotlinpipeline.app.databinding.FragmentInside3Binding
import com.yogakotlinpipeline.app.utils.NetworkService
import com.yogakotlinpipeline.app.utils.UserProfile
import com.yogakotlinpipeline.app.utils.RecommendationCacheStore
import com.yogakotlinpipeline.app.utils.LoginCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope

class Inside3Fragment : Fragment() {

    private var _binding: FragmentInside3Binding? = null
    private val binding get() = _binding!!
    
    // REMOVED: Custom CoroutineScope to prevent memory leaks

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInside3Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        loadRecommendations()
    }

    private fun setupClickListeners() {
        // Menu Button
        binding.btnMenu.setOnClickListener {
            Toast.makeText(context, "Menu", Toast.LENGTH_SHORT).show()
        }

        // Bottom navigation
        binding.btnHome.setOnClickListener {
            findNavController().navigate(R.id.action_inside3Fragment_to_inside1Fragment)
        }
        
        binding.btnExplore.setOnClickListener {
            findNavController().navigate(R.id.action_inside3Fragment_to_inside2Fragment)
        }
        
        binding.btnProgress.setOnClickListener {
            findNavController().navigate(R.id.action_inside3Fragment_to_inside4Fragment)
        }
        
        binding.btnProfile.setOnClickListener {
            findNavController().navigate(R.id.action_inside3Fragment_to_profileFragment)
        }
    }

    private fun loadRecommendations() {
        // REMOVED: recommendationJob?.cancel() - no longer needed
        
        binding.progressLoading.visibility = View.VISIBLE
        
        // CHANGED: Use viewLifecycleOwner.lifecycleScope for automatic cancellation
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Prefer saved user profile; fallback to sample if incomplete
                val loginCache = LoginCache.getInstance(requireContext().applicationContext)
                val savedProfile = loginCache.getUserProfile()
                val userProfile = if (savedProfile.age > 0 && savedProfile.height > 0 && savedProfile.weight > 0) {
                    savedProfile
                } else {
                    UserProfile(
                        age = 25,
                        height = 170,
                        weight = 65,
                        level = "beginner",
                        pregnant = false,
                        problemAreas = listOf("back pain", "stress"),
                        goals = listOf("flexibility", "stress relief"),
                        mentalIssues = listOf("stress", "anxiety")
                    )
                }
                
                Log.d("Inside3Fragment", "UserProfile created: age=${userProfile.age}, height=${userProfile.height}, weight=${userProfile.weight}")
                Log.d("Inside3Fragment", "Goals: ${userProfile.goals}")
                Log.d("Inside3Fragment", "Physical issues: ${userProfile.getPhysicalIssues()}")
                Log.d("Inside3Fragment", "Mental issues: ${userProfile.getAllMentalIssues()}")
                Log.d("Inside3Fragment", "Level: ${userProfile.level}")

                val networkService = NetworkService.getInstance()
                // Check in-memory cache first for fastest hits
                networkService.getCachedRecommendations(userProfile)?.let { cached ->
                    if (cached.isNotEmpty()) {
                        Log.d("Inside3Fragment", "Loaded ${cached.size} recommendations from in-memory cache")
                        withContext(Dispatchers.Main) {
                            if (_binding != null && isAdded) {
                                binding.progressLoading.visibility = View.GONE
                                displayRecommendations(cached)
                            }
                        }
                        return@launch
                    }
                }
                // Try persistent cache to avoid API calls on restart
                RecommendationCacheStore.loadIfFresh(requireContext().applicationContext, userProfile)?.let { cached ->
                    if (cached.isNotEmpty()) {
                        Log.d("Inside3Fragment", "Loaded ${cached.size} recommendations from persistent cache")
                        withContext(Dispatchers.Main) {
                            if (_binding != null && isAdded) {
                                binding.progressLoading.visibility = View.GONE
                                displayRecommendations(cached)
                            }
                        }
                        return@launch
                    }
                }
                
                Log.d("Inside3Fragment", "Starting API call for recommendations")
                val recommendations = networkService.getRecommendations(userProfile, context = requireContext().applicationContext)
                Log.d("Inside3Fragment", "Received ${recommendations.size} recommendations")
                
                withContext(Dispatchers.Main) {
                    if (_binding != null && isAdded) {
                        binding.progressLoading.visibility = View.GONE
                        displayRecommendations(recommendations)
                    }
                }
                
            } catch (e: Exception) {
                Log.e("Inside3Fragment", "Error loading recommendations: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    if (_binding != null && isAdded) {
                        binding.progressLoading.visibility = View.GONE
                        Toast.makeText(requireContext(), "Failed to load recommendations", Toast.LENGTH_SHORT).show()
                        // Show fallback recommendations
                        displayFallbackRecommendations()
                    }
                }
            }
        }
    }

    private fun displayRecommendations(recommendations: List<com.yogakotlinpipeline.app.utils.YogaRecommendation>) {
        if (_binding == null || !isAdded) return
        
        val container = binding.recommendationsContainer
        container.removeAllViews()
        
        if (recommendations.isEmpty()) {
            displayFallbackRecommendations()
            return
        }
        
        // Ensure we show at least 8 recommendations
        val recommendationsToShow = if (recommendations.size >= 8) {
            recommendations.take(8)
        } else {
            // If we have fewer than 8, pad with fallback recommendations
            val fallbackRecommendations = listOf(
                Triple("Mountain Pose", "Grounding, improves posture", "BEGINNER"),
                Triple("Warrior II", "Builds strength and stamina", "INTERMEDIATE"),
                Triple("Crow Pose", "Builds arm strength and focus", "ADVANCED"),
                Triple("Tree Pose", "Improves balance and concentration", "INTERMEDIATE"),
                Triple("Downward Dog", "Stretches entire body, builds strength", "BEGINNER"),
                Triple("Child's Pose", "Relaxing, relieves stress", "BEGINNER"),
                Triple("Triangle Pose", "Opens hips, strengthens legs", "INTERMEDIATE"),
                Triple("Corpse Pose", "Deep relaxation and meditation", "BEGINNER")
            )
            
            val apiRecommendations = recommendations.map { rec ->
                Triple(rec.name, rec.benefits, rec.level)
            }
            
            val combined = (apiRecommendations + fallbackRecommendations).distinctBy { it.first }.take(8)
            combined.map { (name, description, level) ->
                com.yogakotlinpipeline.app.utils.YogaRecommendation(
                    name = name,
                    score = 0.8,
                    benefits = description,
                    contraindications = "",
                    level = level,
                    description = description
                )
            }
        }
        
        recommendationsToShow.forEach { recommendation ->
            val cardView = createRecommendationCard(recommendation)
            container.addView(cardView)
        }
    }

    private fun displayFallbackRecommendations() {
        if (_binding == null || !isAdded) return
        
        val container = binding.recommendationsContainer
        container.removeAllViews()
        
        val fallbackRecommendations = listOf(
            Triple("Mountain Pose", "Grounding, improves posture", "BEGINNER"),
            Triple("Warrior II", "Builds strength and stamina", "INTERMEDIATE"),
            Triple("Crow Pose", "Builds arm strength and focus", "ADVANCED"),
            Triple("Tree Pose", "Improves balance and concentration", "INTERMEDIATE"),
            Triple("Downward Dog", "Stretches entire body, builds strength", "BEGINNER"),
            Triple("Child's Pose", "Relaxing, relieves stress", "BEGINNER"),
            Triple("Triangle Pose", "Opens hips, strengthens legs", "INTERMEDIATE"),
            Triple("Corpse Pose", "Deep relaxation and meditation", "BEGINNER")
        )
        
        fallbackRecommendations.forEach { (name, description, level) ->
            val cardView = createFallbackCard(name, description, level)
            container.addView(cardView)
        }
    }

    private fun createRecommendationCard(recommendation: com.yogakotlinpipeline.app.utils.YogaRecommendation): View {
        val cardLayout = LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16 // Reduced spacing
            }
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 16, 16, 16) // Reduced padding
            setBackgroundResource(R.drawable.asana_card_background)
        }
        
        // Image
        val imageView = ImageView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(120, 120).apply { // Smaller image size
                marginEnd = 16 // Reduced margin
            }
            setImageResource(R.drawable.recommendation_placeholder)
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundResource(R.drawable.rounded_image_background)
        }
        cardLayout.addView(imageView)
        
        // Text content
        val textLayout = LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            orientation = LinearLayout.VERTICAL
        }
        
        // Level badge
        val levelText = TextView(requireContext()).apply {
            text = recommendation.level.uppercase()
            setTextColor(resources.getColor(R.color.primary_color, null))
            textSize = 12f
            try {
                typeface = resources.getFont(R.font.inter_semibold)
            } catch (e: Exception) {
                Log.w("Inside3Fragment", "inter_semibold font not found, using default: ${e.message}")
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            setPadding(0, 0, 0, 16) // 4dp bottom margin
        }
        textLayout.addView(levelText)
        
        // Name
        val nameText = TextView(requireContext()).apply {
            text = recommendation.name
            setTextColor(resources.getColor(R.color.text_primary, null))
            textSize = 18f
            try {
                typeface = resources.getFont(R.font.inter_bold)
            } catch (e: Exception) {
                Log.w("Inside3Fragment", "inter_bold font not found, using default: ${e.message}")
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            setPadding(0, 0, 0, 16) // 4dp bottom margin
        }
        textLayout.addView(nameText)
        
        // Description
        val descText = TextView(requireContext()).apply {
            text = recommendation.benefits
            setTextColor(resources.getColor(R.color.text_secondary, null))
            textSize = 14f
            try {
                typeface = resources.getFont(R.font.inter_regular)
            } catch (e: Exception) {
                Log.w("Inside3Fragment", "inter_regular font not found, using default: ${e.message}")
                typeface = android.graphics.Typeface.DEFAULT
            }
        }
        textLayout.addView(descText)
        
        cardLayout.addView(textLayout)
        
        // Play button
        val playButton = ImageButton(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setImageResource(R.drawable.ic_play_circle_small)
            // Use attribute correctly via setBackgroundResource requires a resource id.
            // Fall back to transparent background if attribute id is not a valid resource.
            try {
                setBackgroundResource(android.R.drawable.list_selector_background)
            } catch (e: Exception) {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
            // Icon is already white, no need for color filter
            scaleX = 1.0f
            scaleY = 1.0f
            setOnClickListener {
                navigateToPoseCalibration(
                    recommendation.name.lowercase(),
                    recommendation.name,
                    recommendation.benefits,
                    recommendation.level
                )
            }
        }
        cardLayout.addView(playButton)
        
        return cardLayout
    }

    private fun createFallbackCard(name: String, description: String, level: String): View {
        val cardLayout = LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16 // Reduced spacing
            }
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 16, 16, 16) // Reduced padding
            setBackgroundResource(R.drawable.asana_card_background)
        }
        
        // Image
        val imageView = ImageView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(120, 120).apply { // Smaller image size
                marginEnd = 16 // Reduced margin
            }
            setImageResource(R.drawable.recommendation_placeholder)
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundResource(R.drawable.rounded_image_background)
        }
        cardLayout.addView(imageView)
        
        // Text content
        val textLayout = LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            orientation = LinearLayout.VERTICAL
        }
        
        // Level badge
        val levelText = TextView(requireContext()).apply {
            text = level
            setTextColor(resources.getColor(R.color.primary_color, null))
            textSize = 12f
            try {
                typeface = resources.getFont(R.font.inter_semibold)
            } catch (e: Exception) {
                Log.w("Inside3Fragment", "inter_semibold font not found, using default: ${e.message}")
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            setPadding(0, 0, 0, 16) // 4dp bottom margin
        }
        textLayout.addView(levelText)
        
        // Name
        val nameText = TextView(requireContext()).apply {
            text = name
            setTextColor(resources.getColor(R.color.text_primary, null))
            textSize = 18f
            try {
                typeface = resources.getFont(R.font.inter_bold)
            } catch (e: Exception) {
                Log.w("Inside3Fragment", "inter_bold font not found, using default: ${e.message}")
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            setPadding(0, 0, 0, 16) // 4dp bottom margin
        }
        textLayout.addView(nameText)
        
        // Description
        val descText = TextView(requireContext()).apply {
            text = description
            setTextColor(resources.getColor(R.color.text_secondary, null))
            textSize = 14f
            try {
                typeface = resources.getFont(R.font.inter_regular)
            } catch (e: Exception) {
                Log.w("Inside3Fragment", "inter_regular font not found, using default: ${e.message}")
                typeface = android.graphics.Typeface.DEFAULT
            }
        }
        textLayout.addView(descText)
        
        cardLayout.addView(textLayout)
        
        // Play button
        val playButton = ImageButton(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setImageResource(R.drawable.ic_play_circle_purple)
            try {
                setBackgroundResource(android.R.drawable.list_selector_background)
            } catch (e: Exception) {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
            // Icon is already white, no need for color filter
            scaleX = 1.0f
            scaleY = 1.0f
            setOnClickListener {
                navigateToPoseCalibration(
                    name.lowercase(),
                    name,
                    description,
                    level.lowercase()
                )
            }
        }
        cardLayout.addView(playButton)
        
        return cardLayout
    }
    
    private fun navigateToPoseCalibration(poseName: String, displayName: String, description: String, difficulty: String) {
        Log.d("Inside3Fragment", "=== Navigating to pose calibration ===")
        Log.d("Inside3Fragment", "Pose: $poseName, Display: $displayName")
        
        val bundle = Bundle().apply {
            putString("pose_name", poseName)
            putString("pose_display_name", displayName)
            putString("pose_description", description)
            putString("pose_difficulty", difficulty)
        }
        
        try {
            findNavController().navigate(R.id.poseCalibrationFragment, bundle)
            Log.d("Inside3Fragment", "Navigation successful!")
        } catch (e: Exception) {
            Log.e("Inside3Fragment", "Navigation failed: ${e.message}", e)
            android.widget.Toast.makeText(context, "Navigation failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // REMOVED: recommendationJob?.cancel() - automatic with lifecycleScope
        _binding = null
    }
}

