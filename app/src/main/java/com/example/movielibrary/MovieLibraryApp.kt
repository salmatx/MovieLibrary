package com.example.movielibrary

import android.app.Application
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.movielibrary.workers.StreamingWorker
import timber.log.Timber

class MovieLibraryApp : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Timber.d("Timber initialized")

        fetchCountriesOnAppStart()
    }

    private fun fetchCountriesOnAppStart() {
        val fetchCountriesWork = OneTimeWorkRequestBuilder<StreamingWorker>().build()
        WorkManager.getInstance(this).enqueue(fetchCountriesWork)
    }
}
