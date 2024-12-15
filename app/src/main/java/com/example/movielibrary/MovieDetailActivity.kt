package com.example.movielibrary

import android.os.Bundle
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.movielibrary.datastore.cachedMovies
import com.example.movielibrary.models.Movie
import timber.log.Timber

class MovieDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_movie_detail)

        try {
            val selectedMovieId = intent.getIntExtra("selectedMovie", -1)

            if (selectedMovieId == -1) {
                Timber.e("Movie ID not passed to MovieDetailActivity")
                finish()
                return
            }

            val selectedMovie = findMovieById(selectedMovieId)
            if (selectedMovie == null) {
                Timber.e("Movie not found in cache for ID: $selectedMovieId")
                finish()
                return
            }

            displayMovieDetails(selectedMovie)
        } catch (e: Exception) {
            Timber.e(e, "Error initializing MovieDetailActivity")
        }
    }

    private fun findMovieById(movieId: Int): Movie? {
        cachedMovies.values.flatten().forEach { movieStore ->
            val movie = movieStore.results.find { it.id == movieId }
            if (movie != null) {
                return movie
            }
        }
        return null
    }

    private fun displayMovieDetails(movie: Movie) {
        try {
            val titleTextView: TextView = findViewById(R.id.titleTextView)
            val descriptionTextView: TextView = findViewById(R.id.descriptionTextView)
            val dateTextView: TextView = findViewById(R.id.dateTextView)
            val platformsTextView: TextView = findViewById(R.id.platformsTextView)
            val alreadySeenTextView: TextView = findViewById(R.id.alreadySeenTextView)
            val posterImageView: ImageView = findViewById(R.id.posterImageView)

            val ratingBar: RatingBar = findViewById(R.id.ratingBar)
            val myRatingBar: RatingBar = findViewById(R.id.myRatingBar)

            titleTextView.text = movie.title
            descriptionTextView.text = movie.description
            dateTextView.text = "Release Date: ${movie.date}"
            platformsTextView.text = "Available on: ${movie.platforms?.joinToString(", ") ?: "Not available"}"
            alreadySeenTextView.text = if (movie.alreadySeen) "Already Seen: Yes" else "Already Seen: No"

            ratingBar.rating = movie.rating / 2
            myRatingBar.rating = movie.myRating / 2

            Glide.with(this)
                .load(movie.imageUrl)
                .placeholder(android.R.color.darker_gray)
                .into(posterImageView)
        } catch (e: Exception) {
            Timber.e(e, "Error displaying movie details")
        }
    }
}
