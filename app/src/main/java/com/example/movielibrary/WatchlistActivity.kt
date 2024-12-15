package com.example.movielibrary

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.movielibrary.adapters.MovieAdapter
import com.example.movielibrary.models.Movie
import com.example.movielibrary.workers.TMDBWorker
import com.google.gson.Gson
import kotlinx.coroutines.launch
import timber.log.Timber

class WatchlistActivity : AppCompatActivity() {

    private lateinit var watchlistRecyclerView: RecyclerView
    private lateinit var movieAdapter: MovieAdapter
    private val watchlistMovies: MutableList<Movie> = mutableListOf()
    private val gson = Gson()

    private val detailActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val removedMovieId = result.data?.getIntExtra("removedMovieId", -1) ?: -1
                if (removedMovieId != -1) {
                    removeMovieFromWatchlist(removedMovieId)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_watchlist)

        watchlistRecyclerView = findViewById(R.id.watchlistRecyclerView)
        watchlistRecyclerView.layoutManager = LinearLayoutManager(this)

        movieAdapter = MovieAdapter(watchlistMovies) { selectedMovie ->
            Timber.d("Opening MovieDetailActivity for movie ID: ${selectedMovie.id}, Title: ${selectedMovie.title}")
            openMovieDetailActivity(selectedMovie)
        }
        watchlistRecyclerView.adapter = movieAdapter

        fetchWatchlistMovies()
    }

    private fun fetchWatchlistMovies() {
        lifecycleScope.launch {
            val worker = TMDBWorker()
            val watchlistIds = getWatchlistIds()

            if (watchlistIds.isEmpty()) {
                Toast.makeText(this@WatchlistActivity,
                    getString(R.string.watchlist_is_empty), Toast.LENGTH_SHORT).show()
                return@launch
            }

            watchlistMovies.clear()
            for (movieId in watchlistIds) {
                val movie = worker.fetchMovieById(movieId)
                if (movie != null) {
                    mergeCachedData(movie)
                    watchlistMovies.add(movie)
                } else {
                    Timber.e("Failed to fetch movie with ID: $movieId")
                }
            }

            movieAdapter.notifyDataSetChanged()
        }
    }

    private fun removeMovieFromWatchlist(movieId: Int) {
        val sharedPreferences = getSharedPreferences("SavedMovies", MODE_PRIVATE)
        val watchlistSet = sharedPreferences.getStringSet("watchlist", null)?.toMutableSet()
        if (watchlistSet != null) {
            watchlistSet.remove(movieId.toString())
            sharedPreferences.edit().putStringSet("watchlist", watchlistSet).apply()
        }

        val iterator = watchlistMovies.iterator()
        while (iterator.hasNext()) {
            val movie = iterator.next()
            if (movie.id == movieId) {
                iterator.remove()
                break
            }
        }
        movieAdapter.notifyDataSetChanged()

        if (watchlistMovies.isEmpty()) {
            Toast.makeText(this@WatchlistActivity,
                getString(R.string.watchlist_is_empty), Toast.LENGTH_SHORT).show()
        }
    }

    private fun mergeCachedData(movie: Movie) {
        val cachedMovies = getCachedMovies()
        val cachedMovie = cachedMovies[movie.id]
        if (cachedMovie != null) {
            movie.myRating = cachedMovie.myRating
            movie.alreadySeen = cachedMovie.alreadySeen
        }
    }

    private fun getWatchlistIds(): List<Int> {
        val sharedPreferences = getSharedPreferences("SavedMovies", MODE_PRIVATE)
        val watchlistSet = sharedPreferences.getStringSet("watchlist", null)
        return watchlistSet?.mapNotNull { it.toIntOrNull() } ?: emptyList()
    }

    private fun getCachedMovies(): Map<Int, Movie> {
        val sharedPreferences = getSharedPreferences("SavedMovies", MODE_PRIVATE)
        val savedMoviesJson = sharedPreferences.getString("saved_movies", null)
        return if (!savedMoviesJson.isNullOrEmpty()) {
            val type = object : com.google.gson.reflect.TypeToken<Map<Int, Movie>>() {}.type
            gson.fromJson(savedMoviesJson, type)
        } else {
            emptyMap()
        }
    }

    private fun openMovieDetailActivity(movie: Movie) {
        val intent = Intent(this, MovieDetailActivity::class.java)
        intent.putExtra("selectedMovie", movie.id)
        detailActivityLauncher.launch(intent)
    }
}
