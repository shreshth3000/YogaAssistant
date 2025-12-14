package com.yogakotlinpipeline.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.yogakotlinpipeline.app.databinding.FragmentPreference2Binding
import com.yogakotlinpipeline.app.utils.LoginCache
import com.yogakotlinpipeline.app.utils.UserProfile

class Preference2Fragment : Fragment() {
    
    private var _binding: FragmentPreference2Binding? = null
    private val binding get() = _binding!!
    private lateinit var loginCache: LoginCache
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPreference2Binding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // CHANGED: Use applicationContext to prevent Activity Context leak
        loginCache = LoginCache.getInstance(requireContext().applicationContext)
        
        setupClickListeners()
        setupRadioButtons()
        loadExistingSelections()
    }
    
    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        
        binding.btnContinue.setOnClickListener {
            saveGoals()
            // Navigate to next preference screen
            findNavController().navigate(R.id.action_preference2Fragment_to_preference3Fragment)
        }
    }
    
    private fun setupRadioButtons() {
        // Set up radio button listeners for visual feedback
        val radioButtons = listOf(
            binding.rbWeightLoss,
            binding.rbFlexibility,
            binding.rbCoreStrength,
            binding.rbStressRelief,
            binding.rbBetterPosture,
            binding.rbDigestion,
            binding.rbEndurance,
            binding.rbRelaxation
        )
        
        radioButtons.forEach { radioButton ->
            radioButton.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    // Uncheck all other radio buttons
                    radioButtons.forEach { rb ->
                        if (rb != radioButton) {
                            rb.isChecked = false
                        }
                    }
                }
            }
        }
    }
    
    private fun loadExistingSelections() {
        val existingProfile = loginCache.getUserProfile()
        if (existingProfile.goals.isNotEmpty()) {
            // Restore previous selections
            val selectedGoal = existingProfile.goals.firstOrNull()
            when (selectedGoal) {
                "weight loss" -> binding.rbWeightLoss.isChecked = true
                "flexibility" -> binding.rbFlexibility.isChecked = true
                "core strength" -> binding.rbCoreStrength.isChecked = true
                "stress relief" -> binding.rbStressRelief.isChecked = true
                "better posture" -> binding.rbBetterPosture.isChecked = true
                "digestion" -> binding.rbDigestion.isChecked = true
                "endurance" -> binding.rbEndurance.isChecked = true
                "relaxation" -> binding.rbRelaxation.isChecked = true
            }
        }
    }
    
    private fun saveGoals() {
        val goals = mutableListOf<String>()
        
        // Collect selected goal
        when {
            binding.rbWeightLoss.isChecked -> goals.add("weight loss")
            binding.rbFlexibility.isChecked -> goals.add("flexibility")
            binding.rbCoreStrength.isChecked -> goals.add("core strength")
            binding.rbStressRelief.isChecked -> goals.add("stress relief")
            binding.rbBetterPosture.isChecked -> goals.add("better posture")
            binding.rbDigestion.isChecked -> goals.add("digestion")
            binding.rbEndurance.isChecked -> goals.add("endurance")
            binding.rbRelaxation.isChecked -> goals.add("relaxation")
        }
        
        // Update existing profile
        val existingProfile = loginCache.getUserProfile()
        val updatedProfile = existingProfile.copy(goals = goals)
        loginCache.saveUserProfile(updatedProfile)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

