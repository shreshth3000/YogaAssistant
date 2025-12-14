package com.yogakotlinpipeline.app.utils

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface RecommendationApiService {
    @POST("recommend/")
    suspend fun getRecommendations(@Body userInput: UserInputRequest): Response<RecommendationResponse>
}

data class UserInputRequest(
    val age: Int,
    val height: Int,
    val weight: Int,
    val goals: List<String>,
    val physical_issues: List<String>,
    val mental_issues: List<String>,
    val level: String
)

data class RecommendationResponse(
    val recommended_asanas: List<AsanaRecommendation>
)

data class AsanaRecommendation(
    val name: String,
    val score: Double,
    val benefits: String,
    val contraindications: String
)

