package com.example.movielibrary

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.movielibrary.adapters.MovieAdapter
import com.example.movielibrary.models.Country
import com.example.movielibrary.models.Genre
import com.example.movielibrary.models.Movie
import com.example.movielibrary.workers.StreamingWorker
import com.example.movielibrary.workers.TMDBWorker
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private lateinit var searchView: SearchView
    private lateinit var genresChipGroup: ChipGroup
    private lateinit var moviesRecyclerView: RecyclerView
    private lateinit var movieAdapter: MovieAdapter
    private lateinit var viewWatchlistButton: Button
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private var currentGenreId: Int? = null
    private var isLoading = false
    private val movies: MutableList<Movie> = mutableListOf()
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Initialize views
        searchView = findViewById(R.id.searchView)
        genresChipGroup = findViewById(R.id.genresChipGroup)
        moviesRecyclerView = findViewById(R.id.moviesRecyclerView)
        viewWatchlistButton = findViewById(R.id.viewWatchlistButton)
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)

        moviesRecyclerView.layoutManager = LinearLayoutManager(this)
        movieAdapter = MovieAdapter(movies) { selectedMovie ->
            Toast.makeText(this, "Selected: ${selectedMovie.title}", Toast.LENGTH_SHORT).show()
            openMovieDetailActivity(selectedMovie)
        }
        moviesRecyclerView.adapter = movieAdapter

        setupDrawer()
        setupSearchView()
        setupWatchlistButton()
        fetchAllGenres()
    }

    private fun openMovieDetailActivity(selectedMovie: Movie) {
        val intent = Intent(this, MovieDetailActivity::class.java)
        intent.putExtra("selectedMovie", selectedMovie.id)
        startActivity(intent)
    }

    private fun setupDrawer() {
        lifecycleScope.launch {
            val countries = StreamingWorker.getSavedCountries(applicationContext)

            if (countries.isNotEmpty()) {
                displayCountriesInDrawer(countries)
            } else {
                Toast.makeText(this@MainActivity, "No countries available", Toast.LENGTH_SHORT).show()
            }
        }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            val selectedCountryName = menuItem.title.toString()

            lifecycleScope.launch {
                val countries = StreamingWorker.getSavedCountries(applicationContext)

                val selectedCountryCode = countries.find { it.name == selectedCountryName }?.countryCode
                if (selectedCountryCode != null) {
                    saveCountryCodeToPreferences(selectedCountryCode)
                    Toast.makeText(this@MainActivity, "Selected Country Code: $selectedCountryCode", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Country code not found for $selectedCountryName", Toast.LENGTH_SHORT).show()
                }
            }

            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun saveCountryCodeToPreferences(countryCode: String) {
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("SelectedCountryCode", countryCode)
        editor.apply()
        Timber.d("Saved country code: $countryCode")
    }


    private fun displayCountriesInDrawer(countries: List<Country>) {
        val menu = navigationView.menu
        menu.clear()

        for (country in countries) {
            val countryName = country.name ?: "Unknown Country"
            menu.add(countryName)
        }
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                searchView.isIconified = false
            }
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    searchMoviesByTitle(it)
                }
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { query ->
                    searchJob?.cancel()
                    searchJob = lifecycleScope.launch {
                        delay(300)
                        if (query.isNotBlank()) {
                            searchMoviesByTitle(query)
                        } else {
                            clearMovies()
                        }
                    }
                }
                return true
            }
        })
    }

    private fun setupWatchlistButton() {
        viewWatchlistButton.setOnClickListener {
            val intent = Intent(this, WatchlistActivity::class.java)
            startActivity(intent)
        }
    }

    private fun searchMoviesByTitle(query: String) {
        if (isLoading) return
        isLoading = true

        lifecycleScope.launch {
            val worker = TMDBWorker()
            val searchResults = worker.searchMoviesByTitle(query)

            isLoading = false
            if (searchResults != null && searchResults.isNotEmpty()) {
                clearMovies()
                movies.addAll(searchResults)
                movieAdapter.notifyDataSetChanged()
            } else {
                Toast.makeText(this@MainActivity, "No results found for '$query'", Toast.LENGTH_SHORT).show()
            }
        }
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
                currentGenreId = checkedId
                clearMovies()
                fetchMoviesByGenre(checkedId)
            } else {
                Toast.makeText(this, "No genre selected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun clearMovies() {
        movies.clear()
        movieAdapter.notifyDataSetChanged()
    }

    private fun fetchMoviesByGenre(genreId: Int) {
        if (isLoading) return
        isLoading = true

        val nextPage = (movies.size / 20) + 1
        lifecycleScope.launch {
            val worker = TMDBWorker()
            val newMovies = worker.fetchMoviesByGenre(genreId, nextPage)

            isLoading = false
            if (newMovies != null) {
                movies.addAll(newMovies)
                movieAdapter.notifyDataSetChanged()
            } else {
                Toast.makeText(this@MainActivity, "Failed to fetch movies", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
