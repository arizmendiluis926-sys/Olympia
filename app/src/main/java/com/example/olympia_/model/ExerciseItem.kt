package com.example.olympia_.model

data class ExerciseItem(
    val slug: String?,
    val name: Map<String, String>?,
    val group: Map<String, String>?,
    val equipment: Map<String, String>?,
    val image: Map<String, String>?,
    val url: String?
)