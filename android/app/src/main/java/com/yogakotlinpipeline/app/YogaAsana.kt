package com.yogakotlinpipeline.app

data class YogaAsana(
    val id: Int,
    val name: String,
    val sanskritName: String,
    val difficultyLevel: DifficultyLevel,
    val imageResource: String, // Asset file name
    val description: String = ""
)

enum class DifficultyLevel {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED
}

