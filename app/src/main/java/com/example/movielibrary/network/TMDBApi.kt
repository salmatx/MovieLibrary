package com.example.movielibrary.network

import com.example.movielibrary.models.Genre
import com.example.movielibrary.models.Movie
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query

interface TMDBApi {

    @Headers("Content-Type: application/json")
    @GET("genre/movie/list")
    suspend fun getAllGenres(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en"
    ): Response<GenreResponse>

    @Headers("Content-Type: application/json")
    @GET("discover/movie")
    suspend fun getMoviesByGenre(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US",
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("include_adult") includeAdult: Boolean = true,
        @Query("include_video") includeVideo: Boolean = false,
        @Query("page") page: Int = 1,
        @Query("with_genres") genreId: Int
    ): Response<MovieResponse>

    @Headers("Content-Type: application/json")
    @GET("movie/{movie_id}")
    suspend fun getMovieById(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en"
    ): Response<Movie>
}

data class GenreResponse(
    val genres: List<Genre>
)

data class MovieResponse(
    val results: List<Movie>
)
