package com.yogakotlinpipeline.app.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class LoginCache private constructor(context: Context) {
    
    companion object {
        private const val PREF_NAME = "login_cache"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_LOGIN_TIMESTAMP = "login_timestamp"
        private const val KEY_REMEMBER_ME = "remember_me"
        
        // User Profile Keys
        private const val KEY_USER_AGE = "user_age"
        private const val KEY_USER_HEIGHT = "user_height"
        private const val KEY_USER_WEIGHT = "user_weight"
        private const val KEY_USER_LEVEL = "user_level"
        private const val KEY_USER_PREGNANT = "user_pregnant"
        private const val KEY_USER_PROBLEM_AREAS = "user_problem_areas"
        private const val KEY_USER_GOALS = "user_goals"
        private const val KEY_USER_MENTAL_ISSUES = "user_mental_issues"
        
        // Recommendations Keys
        private const val KEY_YOGA_RECOMMENDATIONS = "yoga_recommendations"
        private const val KEY_RECOMMENDATIONS_TIMESTAMP = "recommendations_timestamp"
        
        @Volatile
        private var INSTANCE: LoginCache? = null
        
        fun getInstance(context: Context): LoginCache {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LoginCache(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    /**
     * Save login state and user information
     */
    fun saveLoginState(
        email: String,
        name: String = "",
        rememberMe: Boolean = false
    ) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.putString(KEY_USER_EMAIL, email)
        editor.putString(KEY_USER_NAME, name)
        editor.putLong(KEY_LOGIN_TIMESTAMP, System.currentTimeMillis())
        editor.putBoolean(KEY_REMEMBER_ME, rememberMe)
        editor.apply()
    }
    
    /**
     * Check if user is currently logged in
     */
    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }
    
    /**
     * Get the logged in user's email
     */
    fun getUserEmail(): String? {
        return sharedPreferences.getString(KEY_USER_EMAIL, null)
    }
    
    /**
     * Get the logged in user's name
     */
    fun getUserName(): String? {
        return sharedPreferences.getString(KEY_USER_NAME, null)
    }
    
    /**
     * Check if remember me is enabled
     */
    fun isRememberMeEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_REMEMBER_ME, false)
    }
    
    /**
     * Get login timestamp
     */
    fun getLoginTimestamp(): Long {
        return sharedPreferences.getLong(KEY_LOGIN_TIMESTAMP, 0L)
    }
    
    /**
     * Save user profile data
     */
    fun saveUserProfile(profile: UserProfile) {
        val editor = sharedPreferences.edit()
        editor.putInt(KEY_USER_AGE, profile.age)
        editor.putInt(KEY_USER_HEIGHT, profile.height)
        editor.putInt(KEY_USER_WEIGHT, profile.weight)
        editor.putString(KEY_USER_LEVEL, profile.level)
        editor.putBoolean(KEY_USER_PREGNANT, profile.pregnant)
        editor.putStringSet(KEY_USER_PROBLEM_AREAS, profile.problemAreas.toSet())
        editor.putStringSet(KEY_USER_GOALS, profile.goals.toSet())
        editor.putStringSet(KEY_USER_MENTAL_ISSUES, profile.mentalIssues.toSet())
        editor.apply()
    }
    
    /**
     * Get user profile data
     */
    fun getUserProfile(): UserProfile {
        return UserProfile(
            age = sharedPreferences.getInt(KEY_USER_AGE, 0),
            height = sharedPreferences.getInt(KEY_USER_HEIGHT, 0),
            weight = sharedPreferences.getInt(KEY_USER_WEIGHT, 0),
            level = sharedPreferences.getString(KEY_USER_LEVEL, "beginner") ?: "beginner",
            pregnant = sharedPreferences.getBoolean(KEY_USER_PREGNANT, false),
            problemAreas = sharedPreferences.getStringSet(KEY_USER_PROBLEM_AREAS, emptySet())?.toList() ?: emptyList(),
            goals = sharedPreferences.getStringSet(KEY_USER_GOALS, emptySet())?.toList() ?: emptyList(),
            mentalIssues = sharedPreferences.getStringSet(KEY_USER_MENTAL_ISSUES, emptySet())?.toList() ?: emptyList()
        )
    }
    
    /**
     * Save yoga recommendations
     */
    fun saveRecommendations(recommendations: List<YogaRecommendation>) {
        val editor = sharedPreferences.edit()
        val recommendationsJson = gson.toJson(recommendations)
        editor.putString(KEY_YOGA_RECOMMENDATIONS, recommendationsJson)
        editor.putLong(KEY_RECOMMENDATIONS_TIMESTAMP, System.currentTimeMillis())
        editor.apply()
    }
    
    /**
     * Get saved yoga recommendations
     */
    fun getRecommendations(): List<YogaRecommendation> {
        val recommendationsJson = sharedPreferences.getString(KEY_YOGA_RECOMMENDATIONS, null)
        return if (recommendationsJson != null) {
            try {
                val type = object : TypeToken<List<YogaRecommendation>>() {}.type
                gson.fromJson(recommendationsJson, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }
    
    /**
     * Check if recommendations exist and are recent (within 7 days)
     */
    fun hasRecentRecommendations(): Boolean {
        val timestamp = sharedPreferences.getLong(KEY_RECOMMENDATIONS_TIMESTAMP, 0L)
        val currentTime = System.currentTimeMillis()
        val sevenDaysInMillis = 7 * 24 * 60 * 60 * 1000L
        
        return timestamp > 0 && (currentTime - timestamp) < sevenDaysInMillis
    }
    
    /**
     * Check if user profile is complete
     */
    fun isUserProfileComplete(): Boolean {
        val profile = getUserProfile()
        return profile.age > 0 && profile.height > 0 && profile.weight > 0
    }
    
    /**
     * Update specific profile fields
     */
    fun updateProfileFields(
        age: Int? = null,
        height: Int? = null,
        weight: Int? = null,
        level: String? = null,
        pregnant: Boolean? = null,
        problemAreas: List<String>? = null,
        goals: List<String>? = null,
        mentalIssues: List<String>? = null
    ) {
        val currentProfile = getUserProfile()
        val updatedProfile = currentProfile.copy(
            age = age ?: currentProfile.age,
            height = height ?: currentProfile.height,
            weight = weight ?: currentProfile.weight,
            level = level ?: currentProfile.level,
            pregnant = pregnant ?: currentProfile.pregnant,
            problemAreas = problemAreas ?: currentProfile.problemAreas,
            goals = goals ?: currentProfile.goals,
            mentalIssues = mentalIssues ?: currentProfile.mentalIssues
        )
        saveUserProfile(updatedProfile)
    }
    
    /**
     * Clear login state (logout)
     */
    fun clearLoginState() {
        val editor = sharedPreferences.edit()
        editor.remove(KEY_IS_LOGGED_IN)
        editor.remove(KEY_USER_EMAIL)
        editor.remove(KEY_USER_NAME)
        editor.remove(KEY_LOGIN_TIMESTAMP)
        editor.remove(KEY_REMEMBER_ME)
        
        // Clear user profile data
        editor.remove(KEY_USER_AGE)
        editor.remove(KEY_USER_HEIGHT)
        editor.remove(KEY_USER_WEIGHT)
        editor.remove(KEY_USER_LEVEL)
        editor.remove(KEY_USER_PREGNANT)
        editor.remove(KEY_USER_PROBLEM_AREAS)
        editor.remove(KEY_USER_GOALS)
        editor.remove(KEY_USER_MENTAL_ISSUES)
        
        // Clear recommendations
        editor.remove(KEY_YOGA_RECOMMENDATIONS)
        editor.remove(KEY_RECOMMENDATIONS_TIMESTAMP)
        
        editor.apply()
    }
    
    /**
     * Logout user and clear all cached data
     */
    fun logout() {
        clearLoginState()
    }
    
    /**
     * Check if login session is still valid (within 24 hours if remember me is disabled)
     */
    fun isLoginSessionValid(): Boolean {
        if (!isLoggedIn()) return false
        
        if (isRememberMeEnabled()) return true
        
        val loginTime = getLoginTimestamp()
        val currentTime = System.currentTimeMillis()
        val sessionDuration = 24 * 60 * 60 * 1000L // 24 hours in milliseconds
        
        return (currentTime - loginTime) < sessionDuration
    }
    
    /**
     * Auto-login if session is valid
     */
    fun shouldAutoLogin(): Boolean {
        return isLoginSessionValid()
    }
    
    /**
     * Get session duration in hours
     */
    fun getSessionDurationHours(): Long {
        if (isRememberMeEnabled()) return -1L // Infinite
        
        val loginTime = getLoginTimestamp()
        val currentTime = System.currentTimeMillis()
        val remainingTime = (24 * 60 * 60 * 1000L) - (currentTime - loginTime)
        
        return remainingTime / (60 * 60 * 1000L)
    }
}
