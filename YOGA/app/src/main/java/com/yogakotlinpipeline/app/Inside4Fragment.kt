package com.yogakotlinpipeline.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.yogakotlinpipeline.app.databinding.FragmentInside4Binding
import kotlin.random.Random

class Inside4Fragment : Fragment() {

    private var _binding: FragmentInside4Binding? = null
    private val binding get() = _binding!!

    // Simulated data for professional yoga app
    private val weeklyData = listOf(
        "Mon" to Random.nextInt(20, 80),
        "Tue" to Random.nextInt(20, 80),
        "Wed" to Random.nextInt(20, 80),
        "Thu" to Random.nextInt(20, 80),
        "Fri" to Random.nextInt(20, 80),
        "Sat" to Random.nextInt(20, 80),
        "Sun" to Random.nextInt(20, 80)
    )

    private val currentStreak = Random.nextInt(5, 15)
    private val longestStreak = Random.nextInt(20, 45)
    private val weeklyWorkouts = Random.nextInt(3, 8)
    private val totalTimeMinutes = Random.nextInt(180, 300)
    private val caloriesBurned = Random.nextInt(800, 1500)
    private val avgDuration = Random.nextInt(25, 45)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInside4Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        updateProgressData()
    }

    private fun updateProgressData() {
        // Update weekly summary stats
        updateWeeklyStats()
        
        // Update streaks
        updateStreaks()
        
        // Update goals
        updateGoals()
    }

    private fun updateWeeklyStats() {
        // Update workout count
        binding.tvWorkoutCount.text = weeklyWorkouts.toString()
        
        // Format total time
        val hours = totalTimeMinutes / 60
        val minutes = totalTimeMinutes % 60
        val timeText = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
        binding.tvTotalTime.text = timeText
        
        // Update calories
        binding.tvCalories.text = caloriesBurned.toString()
        
        // Update average duration
        binding.tvAvgDuration.text = "${avgDuration}m"
    }

    private fun updateStreaks() {
        // Update current streak
        binding.tvCurrentStreak.text = currentStreak.toString()
        
        // Update longest streak
        binding.tvLongestStreak.text = longestStreak.toString()
    }

    private fun updateGoals() {
        // Goals are already well implemented in the layout
        // We could add more dynamic goal tracking here if needed
    }

    private fun setupClickListeners() {
        // Back Button
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Footer Navigation
        binding.btnHome.setOnClickListener {
            findNavController().navigate(R.id.action_inside4Fragment_to_inside1Fragment)
        }

        binding.btnExplore.setOnClickListener {
            findNavController().navigate(R.id.action_inside4Fragment_to_inside2Fragment)
        }
        
        binding.btnAi.setOnClickListener {
            findNavController().navigate(R.id.action_inside4Fragment_to_inside3Fragment)
        }

        binding.btnProgress.setOnClickListener {
            // Already on progress screen, do nothing
        }

        binding.btnProfile.setOnClickListener {
            findNavController().navigate(R.id.action_inside4Fragment_to_profileFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

