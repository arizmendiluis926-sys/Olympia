package com.example.olympia_.model

data class ExerciseDetail(
    val slug: String?,
    val primaryMuscles: List<TextoIdioma>?,
    val secondaryMuscles: List<TextoIdioma>?,
    val instructions: Map<String, List<String>>?,
    val mistakes: Map<String, List<String>>?,
    val alternatives: Map<String, List<String>>?
)

data class TextoIdioma(
    val id: String?,
    val es: String?,
    val en: String?
)