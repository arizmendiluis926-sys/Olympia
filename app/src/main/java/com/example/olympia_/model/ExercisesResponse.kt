package com.example.olympia_.model

data class ExercisesResponse(
    val group: Map<String, String>?,
    val count: Int?,
    val exercises: List<ExerciseItem>?
)