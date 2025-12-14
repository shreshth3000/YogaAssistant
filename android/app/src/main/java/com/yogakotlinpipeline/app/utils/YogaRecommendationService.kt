package com.yogakotlinpipeline.app.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class YogaRecommendation(
    val name: String,
    val score: Double,
    val benefits: String,
    val contraindications: String,
    val level: String,
    val description: String
)

class YogaRecommendationService(private val context: Context) {
    
    companion object {
        private const val TAG = "YogaRecommendationService"
    }
    
    private val networkService = NetworkService.getInstance()
    
    /**
     * Get yoga recommendations for a user profile from the API
     */
    suspend fun getRecommendations(userProfile: UserProfile): List<YogaRecommendation> {
        return try {
            Log.d(TAG, "Getting recommendations for user: ${userProfile.age} years old, level: ${userProfile.level}")
            withContext(Dispatchers.IO) {
                networkService.getRecommendations(userProfile, context.applicationContext)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting recommendations: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Get top 4 recommendations for display on home screen
     */
    suspend fun getTopRecommendations(userProfile: UserProfile): List<YogaRecommendation> {
        return getRecommendations(userProfile).take(4)
    }
    
    /**
     * Check if the service is ready (always true for API-based service)
     */
    fun isReady(): Boolean = true
}
