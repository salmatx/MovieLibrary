package com.example.movielibrary.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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

    fun updateMovies(newMovies: List<Movie>) {
        movies.clear()
        movies.addAll(newMovies)
        notifyDataSetChanged()
    }

    fun addMovies(newMovies: List<Movie>) {
        val startPosition = movies.size
        movies.addAll(newMovies)
        notifyItemRangeInserted(startPosition, newMovies.size)
    }

    inner class MovieViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val posterImage: ImageView = itemView.findViewById(R.id.posterImage)
        private val title: TextView = itemView.findViewById(R.id.title)

        fun bind(movie: Movie) {
            title.text = movie.title
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
