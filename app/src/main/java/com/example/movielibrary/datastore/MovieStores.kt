package com.example.movielibrary.datastore

import com.example.movielibrary.models.Movie

data class MovieStores(
    val results: List<Movie>,
    val page: Int
)

var cachedMovies: MutableMap<Int, MutableList<MovieStores>> = mutableMapOf()
