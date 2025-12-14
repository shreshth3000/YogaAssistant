package com.yogakotlinpipeline.app.utils

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

data class SimpleYogaPose(
    val name: String,
    val benefits: String,
    val contraindications: String,
    val level: String,
    val targetAreas: List<String>,
    val mentalBenefits: List<String>,
    val physicalBenefits: List<String>
)

class SimpleYogaRecommender(private val context: Context) {
    
    companion object {
        private const val TAG = "SimpleYogaRecommender"
    }
    
    private val yogaPoses = listOf(
        SimpleYogaPose(
            name = "Supta Baddha Konasana",
            benefits = "Stretches the hips and inner thighs, promotes relaxation, relieves stress",
            contraindications = "Not for hip injuries or high BP",
            level = "Beginner",
            targetAreas = listOf("hips", "thighs", "back"),
            mentalBenefits = listOf("relaxation", "stress relief", "calmness"),
            physicalBenefits = listOf("flexibility", "hip opening", "back relief")
        ),
        SimpleYogaPose(
            name = "Balasana",
            benefits = "Promotes relaxation, relieves stress, stretches the hips and thighs",
            contraindications = "Not for knee problems or high BP",
            level = "Beginner",
            targetAreas = listOf("back", "hips", "thighs"),
            mentalBenefits = listOf("relaxation", "stress relief", "peace"),
            physicalBenefits = listOf("back stretch", "hip opening", "relaxation")
        ),
        SimpleYogaPose(
            name = "Adho Mukha Svanasana",
            benefits = "Strengthens arms and shoulders, improves circulation, relieves back pain",
            contraindications = "Not for wrist injuries or high BP",
            level = "Beginner",
            targetAreas = listOf("arms", "shoulders", "back", "legs"),
            mentalBenefits = listOf("focus", "calmness", "energy"),
            physicalBenefits = listOf("strength", "flexibility", "circulation")
        ),
        SimpleYogaPose(
            name = "Tadasana",
            benefits = "Improves posture, strengthens thighs and core, enhances balance",
            contraindications = "None for most people",
            level = "Beginner",
            targetAreas = listOf("legs", "core", "posture"),
            mentalBenefits = listOf("focus", "grounding", "awareness"),
            physicalBenefits = listOf("posture", "balance", "strength")
        ),
        SimpleYogaPose(
            name = "Virabhadrasana II",
            benefits = "Strengthens legs and arms, improves stamina, enhances focus",
            contraindications = "Not for high BP, knee problems, or heart conditions",
            level = "Intermediate",
            targetAreas = listOf("legs", "arms", "core"),
            mentalBenefits = listOf("focus", "determination", "energy"),
            physicalBenefits = listOf("strength", "stamina", "balance")
        ),
        SimpleYogaPose(
            name = "Bhujangasana",
            benefits = "Strengthens back muscles, improves posture, relieves back pain",
            contraindications = "Not for back injuries or pregnancy",
            level = "Beginner",
            targetAreas = listOf("back", "chest", "shoulders"),
            mentalBenefits = listOf("confidence", "energy", "focus"),
            physicalBenefits = listOf("back strength", "posture", "flexibility")
        ),
        SimpleYogaPose(
            name = "Marjaryasana",
            benefits = "Stretches back and spine, improves flexibility, relieves tension",
            contraindications = "None for most people",
            level = "Beginner",
            targetAreas = listOf("back", "spine", "shoulders"),
            mentalBenefits = listOf("relaxation", "focus", "calmness"),
            physicalBenefits = listOf("flexibility", "mobility", "tension relief")
        ),
        SimpleYogaPose(
            name = "Sukhasana",
            benefits = "Opens hips, improves posture, promotes meditation",
            contraindications = "Not for knee injuries",
            level = "Beginner",
            targetAreas = listOf("hips", "back", "posture"),
            mentalBenefits = listOf("meditation", "calmness", "focus"),
            physicalBenefits = listOf("hip opening", "posture", "relaxation")
        ),
        SimpleYogaPose(
            name = "Paschimottanasana",
            benefits = "Stretches hamstrings and back, calms mind, improves digestion",
            contraindications = "Not for back injuries or pregnancy",
            level = "Intermediate",
            targetAreas = listOf("hamstrings", "back", "spine"),
            mentalBenefits = listOf("calmness", "relaxation", "focus"),
            physicalBenefits = listOf("flexibility", "digestion", "circulation")
        ),
        SimpleYogaPose(
            name = "Sarvangasana",
            benefits = "Improves circulation, strengthens shoulders, calms nervous system",
            contraindications = "Not for neck injuries, high BP, or pregnancy",
            level = "Advanced",
            targetAreas = listOf("shoulders", "neck", "core"),
            mentalBenefits = listOf("calmness", "focus", "energy"),
            physicalBenefits = listOf("circulation", "strength", "balance")
        )
    )
    
    fun getRecommendations(userProfile: UserProfile): List<YogaRecommendation> {
        val recommendations = mutableListOf<YogaRecommendation>()
        
        for (pose in yogaPoses) {
            val score = calculateScore(pose, userProfile)
            
            if (score > 0) {
                recommendations.add(
                    YogaRecommendation(
                        name = pose.name,
                        score = score,
                        benefits = pose.benefits,
                        contraindications = pose.contraindications,
                        level = pose.level,
                        description = pose.benefits
                    )
                )
            }
        }
        
        // Sort by score and return top recommendations
        return recommendations.sortedByDescending { it.score }.take(10)
    }
    
    private fun calculateScore(pose: SimpleYogaPose, userProfile: UserProfile): Double {
        var score = 0.0
        
        // Check contraindications first
        if (hasContraindications(pose, userProfile)) {
            return 0.0
        }
        
        // Level matching
        val levelMatch = when {
            userProfile.level.equals("beginner", ignoreCase = true) && pose.level.equals("Beginner", ignoreCase = true) -> 1.0
            userProfile.level.equals("intermediate", ignoreCase = true) && (pose.level.equals("Beginner", ignoreCase = true) || pose.level.equals("Intermediate", ignoreCase = true)) -> 1.0
            userProfile.level.equals("advanced", ignoreCase = true) -> 1.0
            else -> 0.5
        }
        score += levelMatch * 2
        
        // Goal matching
        for (goal in userProfile.goals) {
            when (goal.lowercase()) {
                "flexibility" -> {
                    if (pose.physicalBenefits.any { it.contains("flexibility", ignoreCase = true) }) {
                        score += 3
                    }
                }
                "strength", "core strength" -> {
                    if (pose.physicalBenefits.any { it.contains("strength", ignoreCase = true) }) {
                        score += 3
                    }
                }
                "stress relief", "relaxation" -> {
                    if (pose.mentalBenefits.any { it.contains("relaxation", ignoreCase = true) || it.contains("calmness", ignoreCase = true) }) {
                        score += 3
                    }
                }
                "weight loss" -> {
                    if (pose.physicalBenefits.any { it.contains("strength", ignoreCase = true) || it.contains("circulation", ignoreCase = true) }) {
                        score += 2
                    }
                }
                "better posture" -> {
                    if (pose.physicalBenefits.any { it.contains("posture", ignoreCase = true) }) {
                        score += 3
                    }
                }
                "digestion" -> {
                    if (pose.physicalBenefits.any { it.contains("digestion", ignoreCase = true) }) {
                        score += 3
                    }
                }
            }
        }
        
        // Problem area matching
        for (problemArea in userProfile.getPhysicalIssues()) {
            when (problemArea.lowercase()) {
                "back pain" -> {
                    if (pose.targetAreas.any { it.contains("back", ignoreCase = true) }) {
                        score += 4
                    }
                }
                "knee pain" -> {
                    if (pose.contraindications.contains("knee", ignoreCase = true)) {
                        score -= 5 // Avoid poses contraindicated for knee issues
                    }
                }
                "shoulder pain" -> {
                    if (pose.targetAreas.any { it.contains("shoulder", ignoreCase = true) }) {
                        score += 3
                    }
                }
                "neck pain" -> {
                    if (pose.targetAreas.any { it.contains("neck", ignoreCase = true) }) {
                        score += 3
                    }
                }
                "joint stiffness" -> {
                    if (pose.physicalBenefits.any { it.contains("flexibility", ignoreCase = true) || it.contains("mobility", ignoreCase = true) }) {
                        score += 3
                    }
                }
            }
        }
        
        // Mental issues matching
        for (mentalIssue in userProfile.getAllMentalIssues()) {
            when (mentalIssue.lowercase()) {
                "stress", "anxiety" -> {
                    if (pose.mentalBenefits.any { it.contains("relaxation", ignoreCase = true) || it.contains("calmness", ignoreCase = true) }) {
                        score += 4
                    }
                }
                "depression" -> {
                    if (pose.mentalBenefits.any { it.contains("energy", ignoreCase = true) || it.contains("confidence", ignoreCase = true) }) {
                        score += 3
                    }
                }
            }
        }
        
        // Pregnancy considerations
        if (userProfile.pregnant) {
            if (pose.contraindications.contains("pregnancy", ignoreCase = true)) {
                return 0.0
            }
            // Prefer gentle poses for pregnant women
            if (pose.level.equals("Beginner", ignoreCase = true)) {
                score += 2
            }
        }
        
        return score
    }
    
    private fun hasContraindications(pose: SimpleYogaPose, userProfile: UserProfile): Boolean {
        val contraindications = pose.contraindications.lowercase()
        
        // Check for specific contraindications
        for (problemArea in userProfile.getPhysicalIssues()) {
            when (problemArea.lowercase()) {
                "knee pain" -> {
                    if (contraindications.contains("knee")) return true
                }
                "back pain" -> {
                    if (contraindications.contains("back injury")) return true
                }
                "shoulder pain" -> {
                    if (contraindications.contains("shoulder injury")) return true
                }
            }
        }
        
        // Check for high BP
        if (contraindications.contains("high bp") || contraindications.contains("high blood pressure")) {
            // Assume high BP if not specified - in real app, this would be a separate field
            return false
        }
        
        // Check for pregnancy
        if (userProfile.pregnant && contraindications.contains("pregnancy")) {
            return true
        }
        
        return false
    }
    
    fun getTopRecommendations(userProfile: UserProfile): List<YogaRecommendation> {
        return getRecommendations(userProfile).take(4)
    }
}
