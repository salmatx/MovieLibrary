package com.example.movielibrary

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.movielibrary.adapters.MovieAdapter
import com.example.movielibrary.models.Genre
import com.example.movielibrary.models.Movie
import com.example.movielibrary.workers.TMDBWorker
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private lateinit var genresChipGroup: ChipGroup
    private lateinit var moviesRecyclerView: RecyclerView
    private lateinit var movieAdapter: MovieAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        genresChipGroup = findViewById(R.id.genresChipGroup)
        moviesRecyclerView = findViewById(R.id.moviesRecyclerView)

        // Set RecyclerView layout manager
        moviesRecyclerView.layoutManager = LinearLayoutManager(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        Timber.d("MainActivity started")

        fetchAllGenres()
    }

    private fun fetchAllGenres() {
        Timber.d("Fetching genres...")
        lifecycleScope.launch {
            val worker = TMDBWorker()
            val genres = worker.fetchGenres()

            if (genres != null) {
                displayGenresAsChips(genres)
            } else {
                Toast.makeText(this@MainActivity, "Failed to fetch genres", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayGenresAsChips(genres: List<Genre>) {
        Timber.d("Displaying genres: $genres")
        for (genre in genres) {
            val chip = Chip(this).apply {
                text = genre.name
                isClickable = true
                isCheckable = true
                id = genre.id
            }
            genresChipGroup.addView(chip)
        }

        genresChipGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId != View.NO_ID) {
                fetchMoviesByGenre(checkedId)
            } else {
                Toast.makeText(this, "No genre selected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchMoviesByGenre(genreId: Int) {
        Timber.d("Fetching movies for genre ID: $genreId")
        lifecycleScope.launch {
            val worker = TMDBWorker()
            val movies = worker.fetchMoviesByGenre(genreId)

            if (movies != null) {
                displayMovies(movies)
            } else {
                Toast.makeText(this@MainActivity, "Failed to fetch movies", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayMovies(movies: List<Movie>) {
        Timber.d("Displaying movies: $movies")
        movieAdapter = MovieAdapter(movies) { selectedMovie ->
            Toast.makeText(this, "Selected: ${selectedMovie.title}", Toast.LENGTH_SHORT).show()
        }
        moviesRecyclerView.adapter = movieAdapter
    }
}
