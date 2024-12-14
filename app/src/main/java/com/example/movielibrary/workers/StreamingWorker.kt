package com.example.movielibrary.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.movielibrary.network.NetworkClient
import com.example.movielibrary.network.StreamingApi
import com.example.movielibrary.BuildConfig
import retrofit2.HttpException

class StreamingWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        try {
            val streamingApiKey = BuildConfig.STREAMING_API_KEY

            val tmdbId = inputData.getString("TMDB_ID") ?: return Result.failure()

            val streamingApi = NetworkClient.getStreamingClient().create(StreamingApi::class.java)

            val response = streamingApi.getMovieAvailability(tmdbId, streamingApiKey)
            return Result.success()
        } catch (e: HttpException) {
            return Result.retry()
        } catch (e: Exception) {
            return Result.failure()
        }
    }
}
