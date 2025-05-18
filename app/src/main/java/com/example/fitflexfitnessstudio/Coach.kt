package com.example.fitflexfitnessstudio

data class Coach(
    val name: String,
    val specialty: String,
    val schedule: String,
    val facebookUrl: String // Link to the coach's Facebook profile
)
// Data class for Class (replacing Coach)
data class FitnessClass(
    val id: String = "",
    val name: String,
    val specialty: String,
    val schedule: String,
    val location: String,
    val coachName: String = ""
)