package com.example.olympia_.network

import com.example.olympia_.model.ExercisesResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface ExerciseApiService {

    @GET("v1/by-group/{group}.json")
    fun fetchExercisesByGroup(
        @Path("group") categoryGroup: String
    ): Call<ExercisesResponse>
}