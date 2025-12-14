package com.yogakotlinpipeline.app

import android.content.ComponentCallbacks2
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.yogakotlinpipeline.app.databinding.ActivityMainBinding
import com.yogakotlinpipeline.app.utils.LoginCache

class MainActivity : AppCompatActivity(), ComponentCallbacks2 {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var loginCache: LoginCache
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // CHANGED: Use applicationContext to prevent Activity Context leak
        loginCache = LoginCache.getInstance(applicationContext)
        
        // Delay navigation to ensure NavHostFragment is ready
        binding.root.post {
            checkAndNavigate()
        }
    }
    
    // ADDED: Memory management for OOM prevention
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                // App is in background, clear image cache
                AssetImageHelper.clearCache()
                android.util.Log.d("MainActivity", "Cleared image cache - UI hidden")
            }
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                // App is running but memory is low
                AssetImageHelper.clearCache()
                android.util.Log.d("MainActivity", "Cleared image cache - memory pressure")
            }
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND,
            ComponentCallbacks2.TRIM_MEMORY_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                // App is backgrounded, clear all caches
                AssetImageHelper.clearCache()
                android.util.Log.d("MainActivity", "Cleared all caches - backgrounded")
            }
        }
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        // Clear all caches when system is low on memory
        AssetImageHelper.clearCache()
        android.util.Log.w("MainActivity", "Low memory detected - cleared all caches")
    }
    
    private fun checkAndNavigate() {
        // Navigation is handled by the NavHostFragment in the layout
        // CHANGED: Safe cast to prevent ClassCastException
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        if (navHostFragment !is NavHostFragment) {
            android.util.Log.e("MainActivity", "NavHostFragment not found or wrong type")
            return
        }
        val navController = navHostFragment.navController
        
        // Check if user has completed onboarding and has cached preferences
        if (shouldSkipOnboarding()) {
            android.util.Log.d("MainActivity", "Skipping onboarding, navigating to home")
            // Navigate directly to home screen and clear back stack
            val navOptions = androidx.navigation.NavOptions.Builder()
                .setPopUpTo(R.id.onboarding1Fragment, true)
                .build()
            navController.navigate(R.id.inside1Fragment, null, navOptions)
        } else {
            android.util.Log.d("MainActivity", "User needs to complete onboarding")
        }
    }
    
    private fun shouldSkipOnboarding(): Boolean {
        // Check if user is logged in and has completed profile
        val isLoggedIn = loginCache.isLoggedIn()
        val isProfileComplete = loginCache.isUserProfileComplete()
        val hasRecommendations = loginCache.hasRecentRecommendations()
        
        android.util.Log.d("MainActivity", "Login check: $isLoggedIn")
        android.util.Log.d("MainActivity", "Profile complete: $isProfileComplete")
        android.util.Log.d("MainActivity", "Has recommendations: $hasRecommendations")
        
        // Skip onboarding if user is logged in and has complete profile
        // Recommendations are optional and can be generated later
        return isLoggedIn && isProfileComplete
    }
}

