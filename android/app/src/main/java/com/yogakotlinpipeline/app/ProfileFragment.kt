package com.yogakotlinpipeline.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.yogakotlinpipeline.app.databinding.FragmentProfileBinding
import com.yogakotlinpipeline.app.utils.LoginCache

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var loginCache: LoginCache

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // CHANGED: Use applicationContext to prevent Activity Context leaks
        loginCache = LoginCache.getInstance(requireContext().applicationContext)
        
        setupClickListeners()
        loadProfileData()
    }

    private fun setupClickListeners() {
        // Back Button
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Footer Navigation
        binding.btnHome.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_inside1Fragment)
        }

        binding.btnAi.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_inside3Fragment)
        }

        binding.btnExplore.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_inside2Fragment)
        }

        binding.btnProgress.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_inside4Fragment)
        }

        // Profile Actions
        binding.btnEditProfile.setOnClickListener {
            // Navigate to onboarding flow for editing preferences
            findNavController().navigate(R.id.action_profileFragment_to_preference1Fragment)
        }

        binding.btnNotifications.setOnClickListener {
            Toast.makeText(context, "Notifications settings coming soon!", Toast.LENGTH_SHORT).show()
        }

        binding.btnPrivacy.setOnClickListener {
            Toast.makeText(context, "Privacy settings coming soon!", Toast.LENGTH_SHORT).show()
        }

        binding.btnLogout.setOnClickListener {
            performLogout()
        }
    }

    private fun loadProfileData() {
        // Load user data from cache
        val userEmail = loginCache.getUserEmail()
        val userName = loginCache.getUserName()
        
        // You can load more profile data here from a database or API
        // For now, we'll use the cached data
    }

    private fun performLogout() {
        // Clear login cache
        loginCache.logout()
        
        Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
        
        // Navigate back to login screen
        findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}



