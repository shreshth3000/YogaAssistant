package com.yogakotlinpipeline.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import android.util.Log
import android.widget.TextView
import com.yogakotlinpipeline.app.utils.NetworkService
import com.yogakotlinpipeline.app.utils.UserProfile
import com.yogakotlinpipeline.app.utils.RecommendationCacheStore
import androidx.navigation.fragment.findNavController
import com.yogakotlinpipeline.app.databinding.FragmentInside1Binding
import com.yogakotlinpipeline.app.utils.LoginCache
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Inside1Fragment : Fragment() {

    private var _binding: FragmentInside1Binding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInside1Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        android.util.Log.d("Inside1Fragment", "onViewCreated called")
        setupClickListeners()
        updateStatsCards()
        android.util.Log.d("Inside1Fragment", "About to call populateHomeRecommendations")
        populateHomeRecommendations()
        android.util.Log.d("Inside1Fragment", "populateHomeRecommendations called")
    }

    private fun updateStatsCards() {
        // Simulate dynamic data for stats cards
        val dayStreak = kotlin.random.Random.nextInt(3, 15)
        val sessionsCount = kotlin.random.Random.nextInt(8, 25)
        val weekTimeHours = kotlin.random.Random.nextDouble(2.0, 8.0)
        
        // Update day streak
        binding.tvDayStreak.text = dayStreak.toString()
        
        // Update sessions count
        binding.tvSessionsCount.text = sessionsCount.toString()
        
        // Update week time (format to 1 decimal place)
        binding.tvWeekTime.text = String.format("%.1fh", weekTimeHours)
    }

    private fun setupClickListeners() {
        // Start Yoga Session Button
        binding.btnStartSession.setOnClickListener {
            // Navigate to explore fragment
            findNavController().navigate(R.id.action_inside1Fragment_to_inside2Fragment)
        }

        // Notification Button
        binding.btnNotifications.setOnClickListener {
            android.widget.Toast.makeText(context, "Notifications", android.widget.Toast.LENGTH_SHORT).show()
        }

        // Chatbot Button (if exists in layout)
        try {
            binding.btnChatbot?.setOnClickListener {
                findNavController().navigate(R.id.action_inside1Fragment_to_chatbotFragment)
            }
        } catch (e: Exception) {
            Log.w("Inside1Fragment", "btnChatbot not found in layout")
        }

        // Footer Navigation
        binding.btnHome.setOnClickListener {
            // Already on home, do nothing
        }

        binding.btnExplore.setOnClickListener {
            // Navigate to original Explore (inside2) with images and Sanskrit names
            findNavController().navigate(R.id.action_inside1Fragment_to_inside2Fragment)
        }

        binding.btnAi.setOnClickListener {
            // Navigate to AI section (inside3) for recommendations
            findNavController().navigate(R.id.action_inside1Fragment_to_inside3Fragment)
        }

        binding.btnProgress.setOnClickListener {
            // Navigate to progress screen (inside4)
            findNavController().navigate(R.id.action_inside1Fragment_to_inside4Fragment)
        }

        binding.btnProfile.setOnClickListener {
            // Navigate to profile fragment
            findNavController().navigate(R.id.action_inside1Fragment_to_profileFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun populateHomeRecommendations() {
        try {
            val appContext = requireContext().applicationContext
            // Prefer saved user profile; fallback to sample if incomplete
            val loginCache = LoginCache.getInstance(appContext)
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

            val networkService = NetworkService.getInstance()

            // Show fallback recommendations immediately
            val fallbackRecommendations = listOf(
                com.yogakotlinpipeline.app.utils.YogaRecommendation(
                    name = "Mountain Pose",
                    score = 0.9,
                    benefits = "Grounding, improves posture",
                    contraindications = "",
                    level = "BEGINNER",
                    description = "Grounding, improves posture"
                ),
                com.yogakotlinpipeline.app.utils.YogaRecommendation(
                    name = "Warrior II",
                    score = 0.8,
                    benefits = "Builds strength and stamina",
                    contraindications = "",
                    level = "INTERMEDIATE",
                    description = "Builds strength and stamina"
                ),
                com.yogakotlinpipeline.app.utils.YogaRecommendation(
                    name = "Tree Pose",
                    score = 0.7,
                    benefits = "Improves balance and concentration",
                    contraindications = "",
                    level = "INTERMEDIATE",
                    description = "Improves balance and concentration"
                ),
                com.yogakotlinpipeline.app.utils.YogaRecommendation(
                    name = "Downward Dog",
                    score = 0.6,
                    benefits = "Stretches entire body, builds strength",
                    contraindications = "",
                    level = "BEGINNER",
                    description = "Stretches entire body, builds strength"
                )
            )
            
            // Show fallback recommendations immediately
            setHomeRecommendationTitles(fallbackRecommendations)

            // Launch background load so home can hydrate from cache or API
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                try {
                    // 1) In-memory cache
                    val inMemory = networkService.getCachedRecommendations(userProfile)
                    val cachedOrPersisted = inMemory ?: RecommendationCacheStore.loadIfFresh(appContext, userProfile)

                    if (!cachedOrPersisted.isNullOrEmpty()) {
                        withContext(Dispatchers.Main) {
                            if (_binding != null && isAdded) {
                                // Show top 4 recommendations from the same list used by AI tab
                                setHomeRecommendationTitles(cachedOrPersisted.take(4))
                            }
                        }
                    } else {
                        // 2) Hit API which will also persist cache via NetworkService
                        val fresh = networkService.getRecommendations(userProfile, context = appContext)
                        withContext(Dispatchers.Main) {
                            if (_binding != null && isAdded && fresh.isNotEmpty()) {
                                // Show top 4 recommendations from the same list used by AI tab
                                setHomeRecommendationTitles(fresh.take(4))
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w("Inside1Fragment", "Background load failed: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.w("Inside1Fragment", "Failed to populate home recommendations: ${e.message}", e)
        }
    }

    private fun setHomeRecommendationTitles(recommendations: List<com.yogakotlinpipeline.app.utils.YogaRecommendation>) {
        val tv1: TextView = binding.tvRecommendation1Title
        val tv2: TextView = binding.tvRecommendation2Title
        val tv3: TextView = binding.tvRecommendation3Title
        val tv4: TextView = binding.tvRecommendation4Title
        
        val duration1: TextView = binding.tvRecommendation1Duration
        val duration2: TextView = binding.tvRecommendation2Duration
        val duration3: TextView = binding.tvRecommendation3Duration
        val duration4: TextView = binding.tvRecommendation4Duration

        android.util.Log.d("Inside1Fragment", "Setting home recommendations: ${recommendations.size} items")
        
        if (recommendations.isNotEmpty()) {
            val rec1 = recommendations[0]
            tv1.text = rec1.name
            duration1.text = "15 min" // Default duration since YogaRecommendation doesn't have duration
            android.util.Log.d("Inside1Fragment", "Set recommendation 1: ${rec1.name}")
        }
        if (recommendations.size > 1) {
            val rec2 = recommendations[1]
            tv2.text = rec2.name
            duration2.text = "20 min" // Default duration
            Log.d("Inside1Fragment", "Set recommendation 2: ${rec2.name}")
        }
        if (recommendations.size > 2) {
            val rec3 = recommendations[2]
            tv3.text = rec3.name
            duration3.text = "25 min" // Default duration
            Log.d("Inside1Fragment", "Set recommendation 3: ${rec3.name}")
        }
        if (recommendations.size > 3) {
            val rec4 = recommendations[3]
            tv4.text = rec4.name
            duration4.text = "30 min" // Default duration
            Log.d("Inside1Fragment", "Set recommendation 4: ${rec4.name}")
        }
    }
}

