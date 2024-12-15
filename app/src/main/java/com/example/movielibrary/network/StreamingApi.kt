package com.example.movielibrary.network

import com.example.movielibrary.models.Country
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Query

interface StreamingApi {
    @Headers("Content-Type: application/json")
    @GET("countries")
    suspend fun getCountries(
        @Header("X-RapidAPI-Key") apiKey: String
    ): Response<ResponseBody>

    @Headers("Content-Type: application/json")
    @GET("get/basic")
    suspend fun getMovieAvailability(
        @Header("X-RapidAPI-Key") apiKey: String,
        @Query("tmdb_id") tmdbId: String
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
