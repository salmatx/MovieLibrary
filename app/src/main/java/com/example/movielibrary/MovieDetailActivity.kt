package com.example.movielibrary

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.movielibrary.datastore.cachedMovies
import com.example.movielibrary.models.Movie
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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

        val selectedMovie = findMovieById(selectedMovieId)?.apply {
            val savedMovie = getSavedMovieById(id)
            if (savedMovie != null) {
                myRating = savedMovie.myRating
                alreadySeen = savedMovie.alreadySeen
            }
        }

        if (selectedMovie == null) {
            Timber.e("Movie not found in cache for ID: $selectedMovieId")
            finish()
            return
        }

        displayMovieDetails(selectedMovie)

        val saveButton: Button = findViewById(R.id.saveButton)
        saveButton.setOnClickListener {
            saveMovieToPreferences(selectedMovie)
        }

        val addToWatchlistButton: Button = findViewById(R.id.addToWatchlistButton)
        addToWatchlistButton.setOnClickListener {
            addToWatchlist(selectedMovie.id)
        }
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
        val platformsTextView: TextView = findViewById(R.id.platformsTextView)
        val posterImageView: ImageView = findViewById(R.id.posterImageView)
        val ratingBar: RatingBar = findViewById(R.id.ratingBar)
        val myRatingBar: RatingBar = findViewById(R.id.myRatingBar)
        val radioGroup: RadioGroup = findViewById(R.id.alreadySeenRadioGroup)
        val radioButtonYes: RadioButton = findViewById(R.id.radioButtonYes)
        val radioButtonNo: RadioButton = findViewById(R.id.radioButtonNo)

        titleTextView.text = movie.title
        descriptionTextView.text = movie.description
        dateTextView.text = "Release Date: ${movie.date}"
        platformsTextView.text = "Available on: ${movie.platforms?.joinToString(", ") ?: "Not available"}"
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
