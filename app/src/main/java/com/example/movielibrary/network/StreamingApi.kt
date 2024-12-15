package com.example.movielibrary.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query

interface StreamingApi {
    @Headers("Content-Type: application/json")
    @GET("countries")
    suspend fun getCountries(
        @Header("X-RapidAPI-Key") apiKey: String
    ): Response<ResponseBody>

    @Headers("Content-Type: application/json")
    @GET("shows/movie/{tmdb_id}")
    suspend fun getStreamingServices(
        @Path("tmdb_id") tmdbId: String,
        @Query("output_language") outputLanguage: String = "en",
        @Query("country") country: String,
        @Header("x-rapidapi-host") host: String = "streaming-availability.p.rapidapi.com",
        @Header("X-RapidAPI-Key") apiKey: String
    ): Response<StreamingServiceResponse>
}

data class StreamingServiceResponse(
    val streamingOptions: Map<String, List<StreamingOption>>
)

data class StreamingOption(
    val service: StreamingService
)

data class StreamingService(
    val id: String,
    val name: String
)
