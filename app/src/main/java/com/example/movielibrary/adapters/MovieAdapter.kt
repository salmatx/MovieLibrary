package com.example.movielibrary.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.movielibrary.R
import com.example.movielibrary.models.Movie

class MovieAdapter(
    private var movies: MutableList<Movie>,
    private val onMovieClicked: (Movie) -> Unit
) : RecyclerView.Adapter<MovieAdapter.MovieViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_movie, parent, false)
        return MovieViewHolder(view)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        holder.bind(movies[position])
    }

    override fun getItemCount(): Int = movies.size

    inner class MovieViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val posterImage: ImageView = itemView.findViewById(R.id.posterImage)
        private val title: TextView = itemView.findViewById(R.id.title)
        private val description: TextView = itemView.findViewById(R.id.description)
        private val movieRatingBar: RatingBar = itemView.findViewById(R.id.movieRatingBar)

        fun bind(movie: Movie) {
            title.text = movie.title

            description.text = if (movie.description.length > 100) {
                "${movie.description.take(100)}..."
            } else {
                movie.description
            }

            movieRatingBar.rating = movie.rating / 2

            Glide.with(itemView.context)
                .load(movie.imageUrl)
                .placeholder(android.R.color.darker_gray)
                .into(posterImage)

            itemView.setOnClickListener {
                onMovieClicked(movie)
            }
        }
    }
}
