package com.yogakotlinpipeline.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.yogakotlinpipeline.app.databinding.FragmentOnboarding1Binding

class Onboarding1Fragment : Fragment() {
    
    private var _binding: FragmentOnboarding1Binding? = null
    private val binding get() = _binding!!
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboarding1Binding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupClickListeners()
    }
    
    private fun setupClickListeners() {
        binding.btnGetStarted.setOnClickListener {
            findNavController().navigate(R.id.action_onboarding1Fragment_to_onboarding2Fragment)
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

