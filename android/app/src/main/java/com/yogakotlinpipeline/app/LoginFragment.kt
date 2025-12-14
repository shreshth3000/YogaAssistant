package com.yogakotlinpipeline.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.yogakotlinpipeline.app.databinding.FragmentLoginBinding
import com.yogakotlinpipeline.app.utils.LoginCache

class LoginFragment : Fragment() {
    
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var loginCache: LoginCache
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // CHANGED: Use applicationContext to prevent Activity Context leak
        loginCache = LoginCache.getInstance(requireContext().applicationContext)
        
        setupClickListeners()
        checkAutoLogin()
        restoreSavedCredentials()
    }
    
    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        
        binding.btnLogin.setOnClickListener {
            performLogin()
        }
        
        binding.tvForgotPassword.setOnClickListener {
            // TODO: Implement forgot password functionality
            Toast.makeText(requireContext(), "Forgot password functionality coming soon!", Toast.LENGTH_SHORT).show()
        }
        
        binding.tvSignupLink.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_signupFragment)
        }
    }
    
    private fun checkAutoLogin() {
        // Check if user should be auto-logged in
        if (loginCache.shouldAutoLogin()) {
            val userEmail = loginCache.getUserEmail()
            val userName = loginCache.getUserName()
            
            Toast.makeText(requireContext(), "Welcome back, ${userName ?: userEmail ?: "User"}!", Toast.LENGTH_SHORT).show()
            
            // Navigate directly to the main app
            navigateToMainApp()
        }
    }
    
    private fun restoreSavedCredentials() {
        // Restore email if remember me was enabled
        if (loginCache.isRememberMeEnabled()) {
            val savedEmail = loginCache.getUserEmail()
            if (!savedEmail.isNullOrEmpty()) {
                binding.etEmail.setText(savedEmail)
                binding.cbRememberMe.isChecked = true
            }
        }
    }
    
    private fun performLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val rememberMe = binding.cbRememberMe.isChecked
        
        if (email.isEmpty()) {
            binding.emailLayout.error = "Email is required"
            return
        }
        
        if (password.isEmpty()) {
            binding.passwordLayout.error = "Password is required"
            return
        }
        
        // Clear previous errors
        binding.emailLayout.error = null
        binding.passwordLayout.error = null
        
        // Mock admin login
        if (email == "admin" && password == "1234") {
            // Save login state
            loginCache.saveLoginState(
                email = email,
                name = "Admin User",
                rememberMe = rememberMe
            )
            
            Toast.makeText(requireContext(), "Login successful! Welcome Admin!", Toast.LENGTH_SHORT).show()
            
            // Navigate to main app
            navigateToMainApp()
        } else {
            Toast.makeText(requireContext(), "Invalid credentials. Use admin/1234", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun navigateToMainApp() {
        // Navigate to first preference screen
        findNavController().navigate(R.id.action_loginFragment_to_preference1Fragment)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
