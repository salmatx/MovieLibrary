package com.example.movielibrary.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkClient {
    private const val TMDB_BASE_URL = "https://api.themoviedb.org/3/"
    private const val STREAMING_BASE_URL = "https://streaming-availability.p.rapidapi.com/"

    fun getTMDBClient(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(TMDB_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun getStreamingClient(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(STREAMING_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}