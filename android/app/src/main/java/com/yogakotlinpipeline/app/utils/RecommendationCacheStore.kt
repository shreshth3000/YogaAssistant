package com.yogakotlinpipeline.app.utils

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object RecommendationCacheStore {
    private const val PREF_NAME = "recommendations_cache"
    private const val TAG = "RecommendationCacheStore"
    private const val TTL_MS: Long = 7L * 24 * 60 * 60 * 1000L // 7 days

    private val gson: Gson by lazy { Gson() }

    private fun buildKey(userProfile: UserProfile): String {
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

    fun loadIfFresh(context: Context, userProfile: UserProfile): List<YogaRecommendation>? {
        return try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val key = buildKey(userProfile)
            val tsKey = "${key}__ts"
            val data = prefs.getString(key, null) ?: return null
            val ts = prefs.getLong(tsKey, 0L)
            val age = System.currentTimeMillis() - ts
            if (age !in 0..TTL_MS) return null
            val type = object : TypeToken<List<YogaRecommendation>>() {}.type
            gson.fromJson<List<YogaRecommendation>>(data, type)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load cache: ${e.message}", e)
            null
        }
    }

    fun save(context: Context, userProfile: UserProfile, recommendations: List<YogaRecommendation>) {
        try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val key = buildKey(userProfile)
            val tsKey = "${key}__ts"
            val json = gson.toJson(recommendations)
            prefs.edit()
                .putString(key, json)
                .putLong(tsKey, System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save cache: ${e.message}", e)
        }
    }
}




