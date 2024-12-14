package com.example.movielibrary.workers

import com.example.movielibrary.models.Genre
import com.example.movielibrary.models.Movie
import com.example.movielibrary.network.NetworkClient
import com.example.movielibrary.network.TMDBApi
import com.example.movielibrary.BuildConfig
import retrofit2.HttpException
import timber.log.Timber

class TMDBWorker {

    suspend fun fetchGenres(): List<Genre>? {
        return try {
            Timber.d("Fetching genres from TMDB API...")
            val tmdbApiKey = BuildConfig.TMDB_API
            val tmdbApi = NetworkClient.getTMDBClient().create(TMDBApi::class.java)

            val response = tmdbApi.getAllGenres(tmdbApiKey)
            val genres = response.genres
            Timber.d("Fetched Genres: $genres")

            genres
        } catch (e: HttpException) {
            Timber.e(e, "HTTP error while fetching genres")
            null
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error while fetching genres")
            null
        }
    }

    suspend fun fetchMoviesByGenre(genreId: Int): List<Movie>? {
        return try {
            Timber.d("Fetching movies for genre ID: $genreId")
            val tmdbApiKey = BuildConfig.TMDB_API
            val tmdbApi = NetworkClient.getTMDBClient().create(TMDBApi::class.java)

            val response = tmdbApi.getMoviesByGenre(
                apiKey = tmdbApiKey,
                genreId = genreId
            )
            val baseUrl = "https://image.tmdb.org/t/p/w500"

            var movies = response.results
            movies.forEach { movie ->
                movie.imageUrl = baseUrl + movie.imageUrl
            }
            Timber.d("Fetched Movies: $movies")

            movies
        } catch (e: HttpException) {
            Timber.e(e, "HTTP error while fetching movies")
            null
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error while fetching movies")
            null
        }
    }
}
