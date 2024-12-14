package com.example.movielibrary.network

import retrofit2.http.GET
import retrofit2.http.Query

interface StreamingApi {
    @GET("get/basic")
    suspend fun getMovieAvailability(
        @Query("tmdb_id") tmdbId: String,
        @Query("api_key") apiKey: String
    ): StreamingResponse
}

data class StreamingResponse(
    val tmdbId: String,
    val title: String,
    val streamingInfo: Map<String, List<StreamingPlatform>>
)

data class StreamingPlatform(
    val provider: String,
    val url: String
)
