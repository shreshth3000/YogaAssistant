package com.yogakotlinpipeline.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.yogakotlinpipeline.app.databinding.FragmentPreference1Binding
import com.yogakotlinpipeline.app.utils.LoginCache
import com.yogakotlinpipeline.app.utils.UserProfile

class Preference1Fragment : Fragment() {
    
    private var _binding: FragmentPreference1Binding? = null
    private val binding get() = _binding!!
    private lateinit var loginCache: LoginCache
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPreference1Binding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // CHANGED: Use applicationContext to prevent Activity Context leak
        loginCache = LoginCache.getInstance(requireContext().applicationContext)
        
        setupClickListeners()
        setupCheckBoxes()
        loadExistingSelections()
    }
    
    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        
        binding.btnContinue.setOnClickListener {
            saveProblemAreas()
            // Navigate to next preference screen
            findNavController().navigate(R.id.action_preference1Fragment_to_preference2Fragment)
        }
    }
    
    private fun setupCheckBoxes() {
        // Set up checkbox listeners for visual feedback
        val checkBoxes = listOf(
            binding.cbBackPain,
            binding.cbKneePain,
            binding.cbShoulderPain,
            binding.cbNeckPain,
            binding.cbJointStiffness,
            binding.cbStress,
            binding.cbLowFlexibility,
            binding.cbDigestiveIssues,
            binding.cbBalanceIssues
        )
        
        checkBoxes.forEach { checkBox ->
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                // Update visual state
                checkBox.isChecked = isChecked
            }
        }
    }
    
    private fun loadExistingSelections() {
        val existingProfile = loginCache.getUserProfile()
        if (existingProfile.problemAreas.isNotEmpty()) {
            // Restore previous selections
            binding.cbBackPain.isChecked = existingProfile.problemAreas.contains("back pain")
            binding.cbKneePain.isChecked = existingProfile.problemAreas.contains("knee pain")
            binding.cbShoulderPain.isChecked = existingProfile.problemAreas.contains("shoulder pain")
            binding.cbNeckPain.isChecked = existingProfile.problemAreas.contains("neck pain")
            binding.cbJointStiffness.isChecked = existingProfile.problemAreas.contains("joint stiffness")
            binding.cbStress.isChecked = existingProfile.problemAreas.contains("stress")
            binding.cbLowFlexibility.isChecked = existingProfile.problemAreas.contains("low flexibility")
            binding.cbDigestiveIssues.isChecked = existingProfile.problemAreas.contains("digestive issues")
            binding.cbBalanceIssues.isChecked = existingProfile.problemAreas.contains("balance issues")
        }
    }
    
    private fun saveProblemAreas() {
        val problemAreas = mutableListOf<String>()
        
        // Collect selected problem areas
        if (binding.cbBackPain.isChecked) problemAreas.add("back pain")
        if (binding.cbKneePain.isChecked) problemAreas.add("knee pain")
        if (binding.cbShoulderPain.isChecked) problemAreas.add("shoulder pain")
        if (binding.cbNeckPain.isChecked) problemAreas.add("neck pain")
        if (binding.cbJointStiffness.isChecked) problemAreas.add("joint stiffness")
        if (binding.cbStress.isChecked) problemAreas.add("stress")
        if (binding.cbLowFlexibility.isChecked) problemAreas.add("low flexibility")
        if (binding.cbDigestiveIssues.isChecked) problemAreas.add("digestive issues")
        if (binding.cbBalanceIssues.isChecked) problemAreas.add("balance issues")
        
        // Update existing profile or create new one
        val existingProfile = loginCache.getUserProfile()
        val updatedProfile = existingProfile.copy(problemAreas = problemAreas)
        loginCache.saveUserProfile(updatedProfile)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

