package com.example.movielibrary

import android.app.Application
import timber.log.Timber

class MovieLibraryApp : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Timber.d("Timber initialized")
    }
}
