package com.example.weatherapp.models

import java.io.Serializable

// TODO Preparing Models Based on The API (STEP 1: Create a data model class for using it for the api response. And also create all the models used in this model class.)
// Set as serializable to enable movements btwn classes
data class WeatherResponse(
    val coord: Coord,
    val weather: List<Weather>,
    val base: String,
    val main: Main,
    val visibility: Int,
    val wind: Wind,
    val clouds: Clouds,
    val dt: Int,
    val sys: Sys,
    val timezone: Int,
    val id: Int,
    val name: String,
    val cod: Int
) : Serializable // Enables the objects formed from this class to be converted to strings
