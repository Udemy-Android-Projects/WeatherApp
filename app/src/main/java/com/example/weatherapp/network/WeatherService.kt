package com.example.weatherapp.network

import com.example.weatherapp.models.WeatherResponse
import retrofit.Call
import retrofit.http.GET
import retrofit.http.Query

// TODO Creating the API call with Retrofit and Getting a Response (STEP 3: Create a WeatherService interface. It defines the query section of the API call)

// START
/**
 * An Interface which defines the HTTP operations Functions.
 */
interface WeatherService {
    // Part of the link
    @GET("2.5/weather")
    fun getWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("units") units: String?,
        @Query("appid") appid: String?
    ): Call<WeatherResponse> // Will result with a GSON object defined by the class WeatherResponse. This class will contain all the values that could be found by the API and they will be set to the variables in the WeatherResponse class
}
