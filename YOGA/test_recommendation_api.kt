import com.yogakotlinpipeline.app.utils.UserProfile
import com.yogakotlinpipeline.app.utils.NetworkService
import kotlinx.coroutines.runBlocking

fun main() {
    println("Testing Recommendation API Integration...")
    
    // Create a test user profile
    val testUserProfile = UserProfile(
        age = 25,
        height = 170,
        weight = 65,
        level = "beginner",
        pregnant = false,
        problemAreas = listOf("back pain", "stress"),
        goals = listOf("flexibility", "stress relief"),
        mentalIssues = listOf("stress", "anxiety")
    )
    
    println("Test User Profile:")
    println("Age: ${testUserProfile.age}")
    println("Height: ${testUserProfile.height}cm")
    println("Weight: ${testUserProfile.weight}kg")
    println("Level: ${testUserProfile.level}")
    println("Problem Areas: ${testUserProfile.problemAreas}")
    println("Goals: ${testUserProfile.goals}")
    println("Mental Issues: ${testUserProfile.mentalIssues}")
    println()
    
    // Test the API call
    runBlocking {
        try {
            println("Calling recommendation API...")
            val networkService = NetworkService.getInstance()
            val recommendations = networkService.getRecommendations(testUserProfile)
            
            println("Received ${recommendations.size} recommendations:")
            recommendations.forEachIndexed { index, recommendation ->
                println("${index + 1}. ${recommendation.name} (Score: ${recommendation.score})")
                println("   Benefits: ${recommendation.benefits}")
                println("   Level: ${recommendation.level}")
                println()
            }
            
            println("✅ API integration test successful!")
            
        } catch (e: Exception) {
            println("❌ API integration test failed: ${e.message}")
            e.printStackTrace()
        }
    }
}

