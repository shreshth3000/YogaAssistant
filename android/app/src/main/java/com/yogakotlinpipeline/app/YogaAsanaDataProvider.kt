package com.yogakotlinpipeline.app

object YogaAsanaDataProvider {
    
    val allAsanas = listOf(
        // Beginner Level Asanas
        YogaAsana(
            id = 1,
            name = "Wide-Legged Forward Bend",
            sanskritName = "Prasarita Padottanasana",
            difficultyLevel = DifficultyLevel.BEGINNER,
            imageResource = "iloveimg-compressed/Prasarita Padottanasana.png",
            description = "A gentle forward fold that stretches the hamstrings and inner thighs"
        ),
        YogaAsana(
            id = 2,
            name = "Staff Pose",
            sanskritName = "Dandasana",
            difficultyLevel = DifficultyLevel.BEGINNER,
            imageResource = "iloveimg-compressed/dandasana.png",
            description = "A seated pose that improves posture and strengthens the back muscles"
        ),
        YogaAsana(
            id = 3,
            name = "Boat Pose",
            sanskritName = "Naukasana",
            difficultyLevel = DifficultyLevel.BEGINNER,
            imageResource = "iloveimg-compressed/Boat pose.png",
            description = "A core-strengthening pose that improves balance and concentration"
        ),
        
        // Intermediate Level Asanas
        YogaAsana(
            id = 4,
            name = "Standing Split",
            sanskritName = "Urdhva Prasarita Eka Padasana",
            difficultyLevel = DifficultyLevel.INTERMEDIATE,
            imageResource = "iloveimg-compressed/Urdhva Prasarita eka padasana.png",
            description = "An intermediate pose that requires balance and flexibility"
        ),
        YogaAsana(
            id = 5,
            name = "Tree Pose",
            sanskritName = "Vrksasana",
            difficultyLevel = DifficultyLevel.INTERMEDIATE,
            imageResource = "iloveimg-compressed/VRKSASANA.png",
            description = "A balancing pose that improves focus and strengthens the legs"
        ),
        YogaAsana(
            id = 6,
            name = "Warrior II Pose",
            sanskritName = "Virabhadrasana II",
            difficultyLevel = DifficultyLevel.INTERMEDIATE,
            imageResource = "warrior_ii_pose.png", // Updated with new image
            description = "A powerful standing pose that builds strength and stamina"
        ),
        YogaAsana(
            id = 7,
            name = "Seated Forward Bend",
            sanskritName = "Paschimottanasana",
            difficultyLevel = DifficultyLevel.INTERMEDIATE,
            imageResource = "iloveimg-compressed/Paschimottanasana.png",
            description = "A deep forward fold that stretches the entire back body"
        ),
        YogaAsana(
            id = 8,
            name = "Triangle Pose",
            sanskritName = "Trikonasana",
            difficultyLevel = DifficultyLevel.INTERMEDIATE,
            imageResource = "iloveimg-compressed/Trikosana.png",
            description = "A standing pose that improves balance and stretches the sides of the body"
        ),
        YogaAsana(
            id = 9,
            name = "Cow Face Pose",
            sanskritName = "Gomukhasana",
            difficultyLevel = DifficultyLevel.INTERMEDIATE,
            imageResource = "iloveimg-compressed/gomukhasana.png",
            description = "A seated pose that opens the shoulders and hips"
        ),
        
        // Advanced Level Asanas
        YogaAsana(
            id = 10,
            name = "Wheel Pose",
            sanskritName = "Chakrasana",
            difficultyLevel = DifficultyLevel.ADVANCED,
            imageResource = "iloveimg-compressed/chakrasana.png",
            description = "An advanced backbend that requires significant back flexibility and strength"
        ),
        YogaAsana(
            id = 11,
            name = "Pyramid Pose",
            sanskritName = "Parsvottanasana",
            difficultyLevel = DifficultyLevel.ADVANCED,
            imageResource = "iloveimg-compressed/parvottasana.png",
            description = "An advanced forward fold that requires deep flexibility"
        ),
        YogaAsana(
            id = 12,
            name = "Yogic Sleep Pose",
            sanskritName = "Yoganidrasana",
            difficultyLevel = DifficultyLevel.ADVANCED,
            imageResource = "iloveimg-compressed/dandasana.png", // Using available asset as placeholder
            description = "An advanced pose that requires extreme hip flexibility"
        ),
        YogaAsana(
            id = 13,
            name = "King Pigeon Pose",
            sanskritName = "Raja Kapotasana",
            difficultyLevel = DifficultyLevel.ADVANCED,
            imageResource = "iloveimg-compressed/King PIGEON.png",
            description = "An advanced backbend that requires exceptional back flexibility"
        )
    )
    
    fun getAsanasByLevel(level: DifficultyLevel): List<YogaAsana> {
        return allAsanas.filter { it.difficultyLevel == level }
    }
    
    fun searchAsanas(query: String): List<YogaAsana> {
        return if (query.isEmpty()) {
            allAsanas
        } else {
            allAsanas.filter { asana ->
                asana.name.contains(query, ignoreCase = true) ||
                asana.sanskritName.contains(query, ignoreCase = true) ||
                asana.description.contains(query, ignoreCase = true)
            }
        }
    }
}
