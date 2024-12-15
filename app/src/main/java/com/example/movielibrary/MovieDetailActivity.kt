package com.example.movielibrary

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.movielibrary.datastore.cachedMovies
import com.example.movielibrary.models.Movie
import com.example.movielibrary.workers.StreamingWorker
import com.example.movielibrary.workers.TMDBWorker
import com.google.android.material.chip.ChipGroup
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import timber.log.Timber

class MovieDetailActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_movie_detail)

        sharedPreferences = getSharedPreferences("SavedMovies", Context.MODE_PRIVATE)

        val selectedMovieId = intent.getIntExtra("selectedMovie", -1)

        if (selectedMovieId == -1) {
            Timber.e("Movie ID not passed to MovieDetailActivity")
            finish()
            return
        }

        lifecycleScope.launch {
            val selectedMovie = fetchMovieDetails(selectedMovieId)

            if (selectedMovie == null) {
                Timber.e("Movie not found in cache or via API for ID: $selectedMovieId")
                finish()
                return@launch
            }

            displayMovieDetails(selectedMovie)
            fetchAndDisplayStreamingPlatforms(selectedMovieId)

            setupWatchlistButtons(selectedMovie)
        }
    }

    private suspend fun fetchMovieDetails(movieId: Int): Movie? {
        var selectedMovie = findMovieById(movieId)

        if (selectedMovie == null) {
            Timber.d("Fetching movie details from TMDB API for ID: $movieId")
            val worker = TMDBWorker()
            selectedMovie = worker.fetchMovieById(movieId)
        }

        selectedMovie?.let { movie ->
            val savedMovie = getSavedMovieById(movie.id)
            if (savedMovie != null) {
                movie.myRating = savedMovie.myRating
                movie.alreadySeen = savedMovie.alreadySeen
            }
        }

        return selectedMovie
    }

    private suspend fun fetchAndDisplayStreamingPlatforms(movieId: Int) {
        val countryCode = getSelectedCountryCode()
        val streamingPlatforms = StreamingWorker.getStreamingServices(
            context = this,
            tmdbId = movieId.toString(),
            countryCode = countryCode
        ).distinct()

        val chipGroup: ChipGroup = findViewById(R.id.streamingPlatformsChipGroup)
        chipGroup.removeAllViews()

        if (streamingPlatforms.isNotEmpty()) {
            streamingPlatforms.forEach { platform ->
                val chip = com.google.android.material.chip.Chip(this).apply {
                    text = platform
                    isClickable = false
                    isCheckable = false
                }
                chipGroup.addView(chip)
            }
        } else {
            val noPlatformsChip = com.google.android.material.chip.Chip(this).apply {
                text = "Not available in your region"
                isClickable = false
                isCheckable = false
            }
            chipGroup.addView(noPlatformsChip)
        }
    }

    private fun setupWatchlistButtons(selectedMovie: Movie) {
        val addToWatchlistButton: Button = findViewById(R.id.addToWatchlistButton)
        val removeFromWatchlistButton: Button = findViewById(R.id.removeFromWatchlistButton)
        val saveButton: Button = findViewById(R.id.saveButton)

        val isInWatchlist = isInWatchlist(selectedMovie.id)

        addToWatchlistButton.isEnabled = !isInWatchlist
        removeFromWatchlistButton.isEnabled = isInWatchlist

        addToWatchlistButton.setOnClickListener {
            addToWatchlist(selectedMovie.id)
            addToWatchlistButton.isEnabled = false
            removeFromWatchlistButton.isEnabled = true
        }

        removeFromWatchlistButton.setOnClickListener {
            removeFromWatchlist(selectedMovie.id)
            addToWatchlistButton.isEnabled = true
            removeFromWatchlistButton.isEnabled = false

            val resultIntent = Intent()
            resultIntent.putExtra("removedMovieId", selectedMovie.id)
            setResult(RESULT_OK, resultIntent)
            finish()
        }

        saveButton.setOnClickListener {
            saveMovieToPreferences(selectedMovie)
        }
    }

    private fun getSelectedCountryCode(): String {
        val sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        return sharedPreferences.getString("SelectedCountryCode", "us") ?: "us"
    }

    private fun isInWatchlist(movieId: Int): Boolean {
        val watchlist = getWatchlist()
        return watchlist.contains(movieId.toString())
    }

    private fun findMovieById(movieId: Int): Movie? {
        cachedMovies.values.flatten().forEach { movieStore ->
            val movie = movieStore.results.find { it.id == movieId }
            if (movie != null) return movie
        }
        return null
    }

    private fun displayMovieDetails(movie: Movie) {
        val titleTextView: TextView = findViewById(R.id.titleTextView)
        val descriptionTextView: TextView = findViewById(R.id.descriptionTextView)
        val dateTextView: TextView = findViewById(R.id.dateTextView)
        val posterImageView: ImageView = findViewById(R.id.posterImageView)
        val ratingBar: RatingBar = findViewById(R.id.ratingBar)
        val myRatingBar: RatingBar = findViewById(R.id.myRatingBar)
        val radioGroup: RadioGroup = findViewById(R.id.alreadySeenRadioGroup)
        val radioButtonYes: RadioButton = findViewById(R.id.radioButtonYes)
        val radioButtonNo: RadioButton = findViewById(R.id.radioButtonNo)

        titleTextView.text = movie.title
        descriptionTextView.text = movie.description
        dateTextView.text = "Release Date: ${movie.date}"
        ratingBar.rating = movie.rating / 2
        myRatingBar.rating = movie.myRating / 2

        Glide.with(this)
            .load(movie.imageUrl)
            .placeholder(android.R.color.darker_gray)
            .into(posterImageView)

        when (movie.alreadySeen) {
            true -> radioButtonYes.isChecked = true
            false -> radioButtonNo.isChecked = true
        }

        myRatingBar.setOnRatingBarChangeListener { _, rating, _ ->
            movie.myRating = rating * 2
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            movie.alreadySeen = (checkedId == R.id.radioButtonYes)
        }
    }

    private fun saveMovieToPreferences(movie: Movie) {
        val savedMovies = getSavedMovies()
        savedMovies[movie.id] = movie

        sharedPreferences.edit()
            .putString("saved_movies", gson.toJson(savedMovies))
            .apply()

        Timber.d("Movie saved: ${movie.title}")
    }

    private fun addToWatchlist(movieId: Int) {
        val watchlist = getWatchlist().toMutableSet()
        watchlist.add(movieId.toString())

        sharedPreferences.edit()
            .putStringSet("watchlist", watchlist)
            .apply()

        Timber.d("Movie added to watchlist: $movieId")
    }

    private fun removeFromWatchlist(movieId: Int) {
        val watchlist = getWatchlist().toMutableSet()
        watchlist.remove(movieId.toString())

        sharedPreferences.edit()
            .putStringSet("watchlist", watchlist)
            .apply()

        Timber.d("Movie removed from watchlist: $movieId")
    }

    private fun getSavedMovies(): MutableMap<Int, Movie> {
        val savedMoviesJson = sharedPreferences.getString("saved_movies", null)
        return if (savedMoviesJson != null) {
            val type = object : TypeToken<MutableMap<Int, Movie>>() {}.type
            gson.fromJson(savedMoviesJson, type)
        } else {
            mutableMapOf()
        }
    }

    private fun getSavedMovieById(movieId: Int): Movie? {
        return getSavedMovies()[movieId]
    }

    private fun getWatchlist(): Set<String> {
        return sharedPreferences.getStringSet("watchlist", emptySet()) ?: emptySet()
    }
}
