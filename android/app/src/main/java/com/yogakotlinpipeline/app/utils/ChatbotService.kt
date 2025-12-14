package com.yogakotlinpipeline.app.utils

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ChatbotService {
    companion object {
        private const val TAG = "ChatbotService"
        // Update this to your backend URL after deployment
        private const val BASE_URL = "https://yoga-recommender-chat-692893069544.europe-west1.run.app/"
        
        @Volatile
        private var INSTANCE: ChatbotService? = null
        
        fun getInstance(): ChatbotService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ChatbotService().also { INSTANCE = it }
            }
        }
    }
    
    private val chatApiService: ChatApiService by lazy {
        createApiService()
    }
    
    private val conversationHistory = mutableListOf<Map<String, String>>()
    
    private fun createApiService(): ChatApiService {
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Log.d(TAG, message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
        
        return retrofit.create(ChatApiService::class.java)
    }
    
    suspend fun sendMessage(message: String): String? {
        return try {
            Log.d(TAG, "Sending message: $message")
            
            val response = chatApiService.chat(ChatRequest(message = message))
            
            return if (response.isSuccessful) {
                val responseBody = response.body()
                val botResponse = responseBody?.response ?: "No response received"
                Log.d(TAG, "Received response: $botResponse")
                botResponse
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "API Error: ${response.code()} - $errorBody")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in sendMessage: ${e.message}", e)
            null
        }
    }
    
    fun clearConversationHistory() {
        conversationHistory.clear()
        Log.d(TAG, "Conversation history cleared")
    }
}
