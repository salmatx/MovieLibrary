package com.example.movielibrary.datastore

import com.example.movielibrary.models.Genre

data class GenresStores(
    val genres: List<Genre>
)

var cachedGenres: GenresStores? = null
