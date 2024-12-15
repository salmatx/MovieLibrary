package com.example.movielibrary.workers

import android.content.Context
import android.content.SharedPreferences
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.movielibrary.BuildConfig
import com.example.movielibrary.models.Country
import com.example.movielibrary.network.NetworkClient
import com.example.movielibrary.network.StreamingApi
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import okhttp3.ResponseBody
import retrofit2.HttpException
import timber.log.Timber

class StreamingWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val sharedPreferences: SharedPreferences =
        applicationContext.getSharedPreferences("StreamingData", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        fun getSavedCountries(context: Context): List<Country> {
            val sharedPreferences = context.getSharedPreferences("StreamingData", Context.MODE_PRIVATE)
            val countriesJson = sharedPreferences.getString("countries", null)
            return if (countriesJson != null) {
                val type = object : TypeToken<List<Country>>() {}.type
                Gson().fromJson(countriesJson, type)
            } else {
                emptyList()
            }
        }

        suspend fun getStreamingServices(
            context: Context,
            tmdbId: String,
            countryCode: String
        ): List<String> {
            val streamingApi = NetworkClient.getStreamingClient().create(StreamingApi::class.java)
            val apiKey = BuildConfig.STREAMING_API_KEY

            return try {
                val response = streamingApi.getStreamingServices(
                    tmdbId = tmdbId,
                    country = countryCode,
                    apiKey = apiKey
                )

                if (response.isSuccessful) {
                    response.body()?.streamingOptions?.get(countryCode)?.map { it.service.name } ?: emptyList()
                } else {
                    Timber.e("Failed to fetch streaming services: ${response.errorBody()?.string()}")
                    emptyList()
                }
            } catch (e: HttpException) {
                Timber.e("HTTP Exception while fetching streaming services: ${e.message}")
                emptyList()
            } catch (e: Exception) {
                Timber.e("Exception while fetching streaming services: ${e.message}")
                emptyList()
            }
        }
    }

    override suspend fun doWork(): Result {
        try {
            val streamingApi = NetworkClient.getStreamingClient().create(StreamingApi::class.java)

            if (!sharedPreferences.contains("countries")) {
                val streamingApiKey = BuildConfig.STREAMING_API_KEY
                val response = streamingApi.getCountries(streamingApiKey)
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null) {
                        val countries = parseCountries(responseBody)
                        saveCountriesToPreferences(countries)
                        Timber.d("Countries fetched and saved: $countries")
                    } else {
                        Timber.e("Failed to parse countries response: Response body is null")
                        return Result.failure()
                    }
                } else {
                    Timber.e("Failed to fetch countries: ${response.errorBody()?.string()}")
                    return Result.retry()
                }
            } else {
                Timber.d("Countries already saved. Skipping API call.")
            }

            return Result.success()
        } catch (e: HttpException) {
            Timber.e("HTTP Exception while fetching countries: ${e.message}")
            return Result.retry()
        } catch (e: Exception) {
            Timber.e("Exception while fetching countries: ${e.message}")
            return Result.failure()
        }
    }

    private fun parseCountries(responseBody: ResponseBody): List<Country> {
        val countries = mutableListOf<Country>()
        val jsonObject = JsonParser.parseString(responseBody.string()).asJsonObject

        for ((_, value) in jsonObject.entrySet()) {
            val countryObject = value.asJsonObject
            val country = Country(
                countryCode = countryObject.get("countryCode").asString,
                name = countryObject.get("name").asString
            )
            countries.add(country)
        }

        return countries
    }

    private fun saveCountriesToPreferences(countries: List<Country>) {
        val editor = sharedPreferences.edit()
        val countriesJson = gson.toJson(countries)
        editor.putString("countries", countriesJson)
        editor.apply()
    }
}

