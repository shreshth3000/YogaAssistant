package com.yogakotlinpipeline.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.yogakotlinpipeline.app.databinding.FragmentPreference3Binding
import com.yogakotlinpipeline.app.utils.LoginCache
import com.yogakotlinpipeline.app.utils.UserProfile
import com.yogakotlinpipeline.app.utils.YogaRecommendationService
import kotlinx.coroutines.*
import androidx.lifecycle.lifecycleScope

class Preference3Fragment : Fragment() {
    
    private var _binding: FragmentPreference3Binding? = null
    private val binding get() = _binding!!
    private lateinit var loginCache: LoginCache
    private lateinit var yogaRecommendationService: YogaRecommendationService
    // REMOVED: Custom CoroutineScope to prevent memory leaks
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPreference3Binding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // CHANGED: Use applicationContext to prevent Activity Context leaks
        loginCache = LoginCache.getInstance(requireContext().applicationContext)
        yogaRecommendationService = YogaRecommendationService(requireContext().applicationContext)
        
        setupClickListeners()
        setupRadioButtons()
        loadExistingProfile()
    }
    
    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        
        binding.btnLogout.setOnClickListener {
            performLogout()
        }
        
        binding.btnComplete.setOnClickListener {
            completeProfile()
        }
    }
    
    private fun performLogout() {
        // Clear login cache
        loginCache.logout()
        
        Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
        
        // Navigate back to login screen
        findNavController().navigate(R.id.action_preference3Fragment_to_loginFragment)
    }
    
    private fun setupRadioButtons() {
        // Set up level radio buttons
        val levelRadioButtons = listOf(
            binding.rbBeginner,
            binding.rbIntermediate,
            binding.rbAdvanced
        )
        
        levelRadioButtons.forEach { radioButton ->
            radioButton.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    levelRadioButtons.forEach { rb ->
                        if (rb != radioButton) {
                            rb.isChecked = false
                        }
                    }
                }
            }
        }
        
        // Set up pregnancy radio buttons
        val pregnancyRadioButtons = listOf(
            binding.rbPregnantYes,
            binding.rbPregnantNo
        )
        
        pregnancyRadioButtons.forEach { radioButton ->
            radioButton.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    pregnancyRadioButtons.forEach { rb ->
                        if (rb != radioButton) {
                            rb.isChecked = false
                        }
                    }
                }
            }
        }
    }
    
    private fun loadExistingProfile() {
        val existingProfile = loginCache.getUserProfile()
        
        // Only load if we have some data
        if (existingProfile.age > 0) {
            binding.etAge.setText(existingProfile.age.toString())
            binding.etHeight.setText(existingProfile.height.toString())
            binding.etWeight.setText(existingProfile.weight.toString())
            binding.etMentalIssues.setText(existingProfile.mentalIssues.joinToString(", "))
            
            // Set level radio button
            when (existingProfile.level.lowercase()) {
                "beginner" -> binding.rbBeginner.isChecked = true
                "intermediate" -> binding.rbIntermediate.isChecked = true
                "advanced" -> binding.rbAdvanced.isChecked = true
            }
            
            // Set pregnancy radio button
            if (existingProfile.pregnant) {
                binding.rbPregnantYes.isChecked = true
            } else {
                binding.rbPregnantNo.isChecked = true
            }
        }
    }
    
    private fun completeProfile() {
        // Validate all required fields
        val age = binding.etAge.text.toString().trim()
        val height = binding.etHeight.text.toString().trim()
        val weight = binding.etWeight.text.toString().trim()
        val mentalIssues = binding.etMentalIssues.text.toString().trim()
        
        // Check required fields
        if (age.isEmpty()) {
            Toast.makeText(context, "Please enter your age", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (height.isEmpty()) {
            Toast.makeText(context, "Please enter your height", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (weight.isEmpty()) {
            Toast.makeText(context, "Please enter your weight", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Get selected level
        val level = when {
            binding.rbBeginner.isChecked -> "beginner"
            binding.rbIntermediate.isChecked -> "intermediate"
            binding.rbAdvanced.isChecked -> "advanced"
            else -> "beginner"
        }
        
        // Get pregnancy status
        val pregnant = binding.rbPregnantYes.isChecked
        
        // Parse comma-separated values for mental issues
        val mentalIssuesList = if (mentalIssues.isNotEmpty()) {
            mentalIssues.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            emptyList()
        }
        
        try {
            val ageInt = age.toInt()
            val heightInt = height.toInt()
            val weightInt = weight.toInt()
            
            // Get existing problem areas and goals
            val existingProfile = loginCache.getUserProfile()
            
            // Create complete user profile
            val userProfile = UserProfile(
                age = ageInt,
                height = heightInt,
                weight = weightInt,
                level = level,
                pregnant = pregnant,
                problemAreas = existingProfile.problemAreas,
                goals = existingProfile.goals,
                mentalIssues = mentalIssuesList
            )
            
            // Save user profile
            loginCache.saveUserProfile(userProfile)
            
            // Show loading message
            Toast.makeText(context, "Generating personalized recommendations...", Toast.LENGTH_LONG).show()
            
            // CHANGED: Use viewLifecycleOwner.lifecycleScope for automatic cancellation
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val recommendations = yogaRecommendationService.getTopRecommendations(userProfile)
                    
                    if (recommendations.isNotEmpty()) {
                        // Save recommendations to cache for later use
                        loginCache.saveRecommendations(recommendations)
                        
                        Toast.makeText(requireContext(), "Profile completed! Personalized recommendations ready!", Toast.LENGTH_LONG).show()
                        
                        // Navigate to the main app (inside1Fragment)
                        findNavController().navigate(R.id.action_preference3Fragment_to_inside1Fragment)
                    } else {
                        Toast.makeText(requireContext(), "Profile completed! Could not generate recommendations.", Toast.LENGTH_LONG).show()
                        findNavController().navigate(R.id.action_preference3Fragment_to_inside1Fragment)
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Profile completed! Service error: ${e.message}", Toast.LENGTH_LONG).show()
                    findNavController().navigate(R.id.action_preference3Fragment_to_inside1Fragment)
                }
            }
            
        } catch (e: NumberFormatException) {
            Toast.makeText(requireContext(), "Please enter valid numbers for age, height, and weight", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        // REMOVED: coroutineScope.cancel() - no longer needed with lifecycleScope
        _binding = null
    }
}
