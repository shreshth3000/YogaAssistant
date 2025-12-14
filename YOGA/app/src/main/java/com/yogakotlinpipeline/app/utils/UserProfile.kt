package com.yogakotlinpipeline.app.utils

data class UserProfile(
    val age: Int,
    val height: Int, // in cm
    val weight: Int, // in kg
    val level: String, // beginner, intermediate, advanced
    val pregnant: Boolean,
    val problemAreas: List<String>, // from preference1
    val goals: List<String>, // from preference2
    val mentalIssues: List<String> // mental health considerations
) {
    companion object {
        fun createEmpty(): UserProfile {
            return UserProfile(
                age = 0,
                height = 0,
                weight = 0,
                level = "beginner",
                pregnant = false,
                problemAreas = emptyList(),
                goals = emptyList(),
                mentalIssues = emptyList()
            )
        }
    }
    
    /**
     * Convert UserProfile to a map for JSON serialization
     */
    fun toMap(): Map<String, Any> {
        return mapOf(
            "age" to age,
            "height" to height,
            "weight" to weight,
            "level" to level,
            "pregnant" to pregnant,
            "problemAreas" to problemAreas,
            "goals" to goals,
            "mentalIssues" to mentalIssues
        )
    }
    
    /**
     * Get physical issues from problem areas
     */
    fun getPhysicalIssues(): List<String> {
        return problemAreas.filter { issue ->
            !issue.equals("stress", ignoreCase = true) && 
            !issue.equals("anxiety", ignoreCase = true) &&
            !issue.equals("depression", ignoreCase = true)
        }
    }
    
    /**
     * Get mental issues (combining problem areas and mental issues)
     */
    fun getAllMentalIssues(): List<String> {
        val mentalProblemAreas = problemAreas.filter { issue ->
            issue.equals("stress", ignoreCase = true) || 
            issue.equals("anxiety", ignoreCase = true) ||
            issue.equals("depression", ignoreCase = true)
        }
        return (mentalIssues + mentalProblemAreas).distinct()
    }
}



