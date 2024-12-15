package com.example.movielibrary.workers

import com.example.movielibrary.datastore.GenresStores
import com.example.movielibrary.datastore.MovieStores
import com.example.movielibrary.datastore.cachedGenres
import com.example.movielibrary.datastore.cachedMovies
import com.example.movielibrary.models.Genre
import com.example.movielibrary.models.Movie
import com.example.movielibrary.network.NetworkClient
import com.example.movielibrary.network.TMDBApi
import com.example.movielibrary.BuildConfig
import retrofit2.HttpException
import timber.log.Timber

class TMDBWorker {

    suspend fun fetchGenres(): List<Genre>? {
        cachedGenres?.let {
            Timber.d("Using cached genres: ${it.genres}")
            return it.genres
        }

        return try {
            Timber.d("Fetching genres from TMDB API...")
            val tmdbApiKey = BuildConfig.TMDB_API
            val tmdbApi = NetworkClient.getTMDBClient().create(TMDBApi::class.java)

            val response = tmdbApi.getAllGenres(tmdbApiKey)
            if(response.isSuccessful) {
                val genres = response.body()!!.genres
                Timber.d("Fetched Genres: $genres")

                cachedGenres = GenresStores(genres)
            }
            cachedGenres?.genres
        } catch (e: HttpException) {
            Timber.e(e, "HTTP error while fetching genres")
            null
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error while fetching genres")
            null
        }
    }

    suspend fun fetchMoviesByGenre(genreId: Int, page: Int = 1): List<Movie>? {
        cachedMovies[genreId]?.let { pages ->
            pages.find { it.page == page }?.let { movieStore ->
                Timber.d("Using cached movies for genreId $genreId, page $page: ${movieStore.results}")
                return movieStore.results
            }
        }

        return try {
            Timber.d("Fetching movies for genre ID: $genreId, page: $page")
            val tmdbApiKey = BuildConfig.TMDB_API
            val tmdbApi = NetworkClient.getTMDBClient().create(TMDBApi::class.java)

            val response = tmdbApi.getMoviesByGenre(
                apiKey = tmdbApiKey,
                genreId = genreId,
                page = page
            )
            if (response.isSuccessful && response.body() != null) {
                val baseUrl = "https://image.tmdb.org/t/p/w500"
                val movies = response.body()!!.results.map { movie ->
                    movie.apply { imageUrl = baseUrl + imageUrl }
                }
                Timber.d("Fetched Movies: $movies")

                val movieStore = MovieStores(movies, page)
                cachedMovies.getOrPut(genreId) { mutableListOf() }.add(movieStore)
                movies
            } else {
                Timber.e("Failed to fetch movies. Response: ${response.message()}")
                null
            }
        } catch (e: HttpException) {
            Timber.e(e, "HTTP error while fetching movies")
            null
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error while fetching movies")
            null
        }
    }

    fun getAllCachedMoviesByGenre(genreId: Int): List<Movie>? {
        return cachedMovies[genreId]?.flatMap { it.results }
    }
}
