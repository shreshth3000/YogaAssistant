package com.yogakotlinpipeline.app.utils

import android.content.Context
import android.util.Log
// TensorFlow Lite disabled for this build variant
// import org.tensorflow.lite.Interpreter
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer

data class TFLiteYogaRecommendation(
    val name: String,
    val score: Float,
    val benefits: String,
    val contraindications: String,
    val level: String
)

class TensorFlowYogaRecommender(private val context: Context) {
    
    companion object {
        private const val TAG = "TFLiteYogaRecommender"
        private const val MODEL_FILE = "yoga_recommendation_model.tflite"
        private const val INPUT_SIZE = 64 // Simplified input size
        private const val OUTPUT_SIZE = 10 // Number of yoga poses
    }
    
    private var interpreter: Any? = null
    private val yogaPoses = listOf(
        "Supta Baddha Konasana",
        "Balasana", 
        "Adho Mukha Svanasana",
        "Tadasana",
        "Virabhadrasana II",
        "Bhujangasana",
        "Marjaryasana",
        "Sukhasana",
        "Paschimottanasana",
        "Sarvangasana"
    )
    
    init {
        loadModel()
    }
    
    private fun loadModel() {
        try {
            val modelFile = File(context.getExternalFilesDir(null), MODEL_FILE)
            if (!modelFile.exists()) {
                Log.w(TAG, "TFLite model not found, using fallback recommender")
                return
            }
            
            // interpreter = Interpreter(modelFile, Interpreter.Options())
            Log.d(TAG, "TFLite model loaded successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading TFLite model: ${e.message}")
        }
    }
    
    fun getRecommendations(userProfile: UserProfile): List<TFLiteYogaRecommendation> {
        return if (interpreter != null) {
            getTFLiteRecommendations(userProfile)
        } else {
            getFallbackRecommendations(userProfile)
        }
    }
    
    private fun getTFLiteRecommendations(userProfile: UserProfile): List<TFLiteYogaRecommendation> {
        try {
            // Convert user profile to input tensor
            val inputBuffer = createInputTensor(userProfile)
            val outputBuffer = ByteBuffer.allocateDirect(OUTPUT_SIZE * 4) // 4 bytes per float
            outputBuffer.order(ByteOrder.nativeOrder())
            
            // Run inference (disabled in this build)
            // interpreter?.run(inputBuffer, outputBuffer)
            
            // Parse results
            val scores = FloatArray(OUTPUT_SIZE)
            outputBuffer.rewind()
            for (i in 0 until OUTPUT_SIZE) {
                scores[i] = outputBuffer.float
            }
            
            // Create recommendations
            val recommendations = mutableListOf<TFLiteYogaRecommendation>()
            for (i in scores.indices) {
                if (scores[i] > 0.1f) { // Threshold for meaningful recommendations
                    recommendations.add(
                        TFLiteYogaRecommendation(
                            name = yogaPoses[i],
                            score = scores[i],
                            benefits = getBenefitsForPose(yogaPoses[i]),
                            contraindications = getContraindicationsForPose(yogaPoses[i]),
                            level = getLevelForPose(yogaPoses[i])
                        )
                    )
                }
            }
            
            return recommendations.sortedByDescending { it.score }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error running TFLite inference: ${e.message}")
            return getFallbackRecommendations(userProfile)
        }
    }
    
    private fun createInputTensor(userProfile: UserProfile): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(INPUT_SIZE * 4) // 4 bytes per float
        buffer.order(ByteOrder.nativeOrder())
        
        // Create a simple feature vector from user profile
        val features = FloatArray(INPUT_SIZE)
        var index = 0
        
        // Age (normalized)
        features[index++] = (userProfile.age - 25f) / 50f // Normalize around 25 years
        
        // Height (normalized)
        features[index++] = (userProfile.height - 170f) / 50f // Normalize around 170cm
        
        // Weight (normalized)
        features[index++] = (userProfile.weight - 70f) / 50f // Normalize around 70kg
        
        // Level encoding
        features[index++] = when (userProfile.level.lowercase()) {
            "beginner" -> 0f
            "intermediate" -> 0.5f
            "advanced" -> 1f
            else -> 0f
        }
        
        // Pregnancy
        features[index++] = if (userProfile.pregnant) 1f else 0f
        
        // Goals encoding
        val goals = userProfile.goals.toSet()
        features[index++] = if (goals.contains("flexibility")) 1f else 0f
        features[index++] = if (goals.contains("strength")) 1f else 0f
        features[index++] = if (goals.contains("stress relief")) 1f else 0f
        features[index++] = if (goals.contains("weight loss")) 1f else 0f
        features[index++] = if (goals.contains("better posture")) 1f else 0f
        features[index++] = if (goals.contains("digestion")) 1f else 0f
        
        // Problem areas encoding
        val problemAreas = userProfile.problemAreas.toSet()
        features[index++] = if (problemAreas.contains("back pain")) 1f else 0f
        features[index++] = if (problemAreas.contains("knee pain")) 1f else 0f
        features[index++] = if (problemAreas.contains("shoulder pain")) 1f else 0f
        features[index++] = if (problemAreas.contains("neck pain")) 1f else 0f
        features[index++] = if (problemAreas.contains("stress")) 1f else 0f
        features[index++] = if (problemAreas.contains("anxiety")) 1f else 0f
        
        // Fill remaining features with zeros
        while (index < INPUT_SIZE) {
            features[index++] = 0f
        }
        
        // Write to buffer
        for (feature in features) {
            buffer.putFloat(feature)
        }
        buffer.rewind()
        
        return buffer
    }
    
    private fun getFallbackRecommendations(userProfile: UserProfile): List<TFLiteYogaRecommendation> {
        // Use simple logic when TFLite model is not available
        val recommendations = mutableListOf<TFLiteYogaRecommendation>()
        
        // Simple scoring based on user profile
        val scores = FloatArray(yogaPoses.size)
        
        for (i in yogaPoses.indices) {
            var score = 0f
            
            // Level matching
            val poseLevel = getLevelForPose(yogaPoses[i])
            when (userProfile.level.lowercase()) {
                "beginner" -> if (poseLevel == "Beginner") score += 0.5f
                "intermediate" -> if (poseLevel in listOf("Beginner", "Intermediate")) score += 0.5f
                "advanced" -> score += 0.5f
            }
            
            // Goal matching
            if (userProfile.goals.contains("flexibility") && getBenefitsForPose(yogaPoses[i]).contains("flexibility")) {
                score += 0.3f
            }
            if (userProfile.goals.contains("stress relief") && getBenefitsForPose(yogaPoses[i]).contains("relaxation")) {
                score += 0.3f
            }
            
            // Problem area matching
            if (userProfile.problemAreas.contains("back pain") && getBenefitsForPose(yogaPoses[i]).contains("back")) {
                score += 0.4f
            }
            
            scores[i] = score
        }
        
        // Create recommendations
        for (i in scores.indices) {
            if (scores[i] > 0.1f) {
                recommendations.add(
                    TFLiteYogaRecommendation(
                        name = yogaPoses[i],
                        score = scores[i],
                        benefits = getBenefitsForPose(yogaPoses[i]),
                        contraindications = getContraindicationsForPose(yogaPoses[i]),
                        level = getLevelForPose(yogaPoses[i])
                    )
                )
            }
        }
        
        return recommendations.sortedByDescending { it.score }
    }
    
    private fun getBenefitsForPose(poseName: String): String {
        return when (poseName) {
            "Supta Baddha Konasana" -> "Stretches hips and thighs, promotes relaxation"
            "Balasana" -> "Promotes relaxation, relieves stress"
            "Adho Mukha Svanasana" -> "Strengthens arms and shoulders, improves circulation"
            "Tadasana" -> "Improves posture, strengthens core"
            "Virabhadrasana II" -> "Strengthens legs and arms, improves stamina"
            "Bhujangasana" -> "Strengthens back muscles, improves posture"
            "Marjaryasana" -> "Stretches back and spine, improves flexibility"
            "Sukhasana" -> "Opens hips, improves posture"
            "Paschimottanasana" -> "Stretches hamstrings and back, calms mind"
            "Sarvangasana" -> "Improves circulation, strengthens shoulders"
            else -> "General yoga benefits"
        }
    }
    
    private fun getContraindicationsForPose(poseName: String): String {
        return when (poseName) {
            "Supta Baddha Konasana" -> "Not for hip injuries or high BP"
            "Balasana" -> "Not for knee problems or high BP"
            "Adho Mukha Svanasana" -> "Not for wrist injuries or high BP"
            "Tadasana" -> "None for most people"
            "Virabhadrasana II" -> "Not for high BP, knee problems"
            "Bhujangasana" -> "Not for back injuries or pregnancy"
            "Marjaryasana" -> "None for most people"
            "Sukhasana" -> "Not for knee injuries"
            "Paschimottanasana" -> "Not for back injuries or pregnancy"
            "Sarvangasana" -> "Not for neck injuries, high BP, or pregnancy"
            else -> "Consult with instructor"
        }
    }
    
    private fun getLevelForPose(poseName: String): String {
        return when (poseName) {
            "Supta Baddha Konasana", "Balasana", "Adho Mukha Svanasana", "Tadasana", "Bhujangasana", "Marjaryasana", "Sukhasana" -> "Beginner"
            "Virabhadrasana II", "Paschimottanasana" -> "Intermediate"
            "Sarvangasana" -> "Advanced"
            else -> "Beginner"
        }
    }
    
    fun getTopRecommendations(userProfile: UserProfile): List<TFLiteYogaRecommendation> {
        return getRecommendations(userProfile).take(4)
    }
    
    fun close() {
        // no-op
    }
}
