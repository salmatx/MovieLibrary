package com.example.movielibrary.models

import com.google.gson.annotations.SerializedName
import java.util.Date

data class Movie(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("overview") val description: String,
    @SerializedName("release_date") val date: String,
    @SerializedName("vote_average") val rating: Float,
    @SerializedName("poster_path") var imageUrl: String?,
    val platforms: List<String>,
    val myRating: Float,
    val alreadySeen: Boolean
)