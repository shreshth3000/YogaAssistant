package com.yogakotlinpipeline.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.yogakotlinpipeline.app.databinding.FragmentSignupBinding

class SignupFragment : Fragment() {
    
    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupClickListeners()
    }
    
    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        
        binding.btnSignup.setOnClickListener {
            performSignup()
        }
        
        binding.tvLoginLink.setOnClickListener {
            findNavController().navigate(R.id.action_signupFragment_to_loginFragment)
        }
    }
    
    private fun performSignup() {
        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        
        if (firstName.isEmpty()) {
            binding.firstNameLayout.error = "First name is required"
            return
        }
        
        if (lastName.isEmpty()) {
            binding.lastNameLayout.error = "Last name is required"
            return
        }
        
        if (email.isEmpty()) {
            binding.emailLayout.error = "Email is required"
            return
        }
        
        if (password.isEmpty()) {
            binding.passwordLayout.error = "Password is required"
            return
        }
        
        if (password.length < 6) {
            binding.passwordLayout.error = "Password must be at least 6 characters"
            return
        }
        
        // TODO: Implement actual signup logic
        Toast.makeText(requireContext(), "Signup functionality coming soon!", Toast.LENGTH_SHORT).show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

