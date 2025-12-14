package com.yogakotlinpipeline.app

// Data classes for pose feedback used across the app
data class PoseThreshold(val minAngle: Float, val maxAngle: Float)
data class JointFeedback(val angle: Float, val isCorrect: Boolean, val threshold: PoseThreshold)
data class PoseFeedback(val jointFeedback: MutableMap<String, JointFeedback> = mutableMapOf())
