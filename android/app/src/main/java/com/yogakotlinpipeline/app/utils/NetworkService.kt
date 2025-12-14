package com.yogakotlinpipeline.app.utils

import android.util.Log
import android.content.Context
import android.util.LruCache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class NetworkService {
    companion object {
        private const val TAG = "NetworkService"
        // UPDATED: Use unified backend for both recommender and chatbot
        // This is the same backend as ChatbotService (handles /recommend/ and /chat/)
        // Format: https://yoga-backend-{service-id}.run.app/
        // After deploying to Cloud Run, update this to your service URL
        private const val BASE_URL = "https://yoga-recommender-chat-692893069544.europe-west1.run.app/"
        private const val MAX_CACHE_SIZE = 10 // Limit cache entries to prevent memory leaks
        
        @Volatile
        private var INSTANCE: NetworkService? = null
        
        fun getInstance(): NetworkService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NetworkService().also { INSTANCE = it }
            }
        }
    }
    
    private val apiService: RecommendationApiService by lazy {
        createApiService()
    }
    
    // CHANGED: Replace unbounded ConcurrentHashMap with LruCache to prevent memory leaks
    private val recommendationCache: LruCache<String, CacheEntry> = LruCache(MAX_CACHE_SIZE)
    private val cacheTtlMs: Long = 7L * 24 * 60 * 60 * 1000L // 7 days

    // ADDED: Cache entry data class moved here for LruCache compatibility
    private data class CacheEntry(
        val timestampMs: Long,
        val recommendations: List<YogaRecommendation>
    )

    private fun buildCacheKey(userProfile: UserProfile): String {
        // Normalize lists by sorting to ensure consistent keys
        val goalsSorted = userProfile.goals.sorted()
        val physicalIssuesSorted = userProfile.getPhysicalIssues().sorted()
        val mentalIssuesSorted = userProfile.getAllMentalIssues().sorted()
        return listOf(
            "age=${userProfile.age}",
            "height=${userProfile.height}",
            "weight=${userProfile.weight}",
            "level=${userProfile.level}",
            "goals=$goalsSorted",
            "physical=$physicalIssuesSorted",
            "mental=$mentalIssuesSorted"
        ).joinToString("|")
    }

    fun getCachedRecommendations(userProfile: UserProfile): List<YogaRecommendation>? {
        val cacheKey = buildCacheKey(userProfile)
        val entry = recommendationCache.get(cacheKey) ?: return null
        val ageMs = System.currentTimeMillis() - entry.timestampMs
        return if (ageMs in 0..cacheTtlMs) {
            Log.d(TAG, "Cache hit for recommendations (ageMs=$ageMs)")
            entry.recommendations
        } else {
            Log.d(TAG, "Cache expired for recommendations (ageMs=$ageMs)")
            recommendationCache.remove(cacheKey) // ADDED: Remove expired entries to free memory
            null
        }
    }

    private fun createApiService(): RecommendationApiService {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        return retrofit.create(RecommendationApiService::class.java)
    }
    
    suspend fun getRecommendations(userProfile: UserProfile, context: Context? = null): List<YogaRecommendation> {
        return try {
            val cacheKey = buildCacheKey(userProfile)
            // Check cache first
            recommendationCache.get(cacheKey)?.let { entry ->
                val ageMs = System.currentTimeMillis() - entry.timestampMs
                if (ageMs in 0..cacheTtlMs) {
                    Log.d(TAG, "Cache hit for recommendations (ageMs=$ageMs)")
                    return entry.recommendations
                } else {
                    Log.d(TAG, "Cache expired for recommendations (ageMs=$ageMs)")
                    recommendationCache.remove(cacheKey) // ADDED: Remove expired entries
                }
            }

            // Try persistent cache if context provided
            if (context != null) {
                RecommendationCacheStore.loadIfFresh(context, userProfile)?.let { list ->
                    Log.d(TAG, "Persistent cache hit for recommendations")
                    // Populate in-memory for faster subsequent hits
                    recommendationCache.put(cacheKey, CacheEntry(System.currentTimeMillis(), list))
                    return list
                }
            }

            val userInput = UserInputRequest(
                age = userProfile.age,
                height = userProfile.height,
                weight = userProfile.weight,
                goals = userProfile.goals,
                physical_issues = userProfile.getPhysicalIssues(),
                mental_issues = userProfile.getAllMentalIssues(),
                level = userProfile.level
            )
            
            Log.d(TAG, "Sending request: $userInput")
            val response = apiService.getRecommendations(userInput)
            Log.d(TAG, "Response received: ${response.code()} - ${response.message()}")
            
            if (response.isSuccessful) {
                val recommendationResponse = response.body()
                Log.d(TAG, "Response body: $recommendationResponse")
                if (recommendationResponse != null) {
                    val recommendations = recommendationResponse.recommended_asanas.map { asana ->
                        YogaRecommendation(
                            name = asana.name,
                            score = asana.score,
                            benefits = asana.benefits,
                            contraindications = asana.contraindications,
                            level = userProfile.level,
                            description = asana.benefits
                        )
                    }
                    Log.d(TAG, "Successfully parsed ${recommendations.size} recommendations")
                    // Store in memory cache
                    recommendationCache.put(cacheKey, CacheEntry(
                        timestampMs = System.currentTimeMillis(),
                        recommendations = recommendations
                    ))
                    // Store persistent cache if context provided
                    if (context != null) {
                        RecommendationCacheStore.save(context, userProfile, recommendations)
                    }
                    recommendations
                } else {
                    Log.w(TAG, "Empty response body")
                    emptyList()
                }
            } else {
                Log.e(TAG, "API call failed: ${response.code()} - ${response.message()}")
                Log.e(TAG, "Error body: ${response.errorBody()?.string()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting recommendations: ${e.message}", e)
            emptyList()
        }
    }
}

