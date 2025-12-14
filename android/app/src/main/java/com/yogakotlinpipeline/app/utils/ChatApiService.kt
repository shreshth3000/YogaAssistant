package com.yogakotlinpipeline.app.utils

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import com.google.gson.annotations.SerializedName

interface ChatApiService {
    @POST("chat/")
    suspend fun chat(@Body request: ChatRequest): Response<ChatResponse>
}

data class ChatRequest(
    val message: String
)

data class ChatResponse(
    val response: String
)
