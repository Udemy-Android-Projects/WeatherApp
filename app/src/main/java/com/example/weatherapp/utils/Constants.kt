package com.example.weatherapp.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build


// TODO Preparing the Internet Connection for RETROFIT and API calls (STEP 2: Create the activities package and utils package and add the MainActivity class to it and create the constant object in utils.)
object Constants {

    // TODO Creating the API call with Retrofit and Getting a Response  (STEP 2: Add the API key and Base URL and Metric unit here from openweathermap.)
    const val APP_ID: String = "ca6ca097dffd1fcacb09116a48b93760"
    const val BASE_URL: String = "http://api.openweathermap.org/data/"
    const val METRIC_UNIT: String = "metric"
    // TODO Storing Data via Shared Preferences (STEP 2: Add the SharedPreferences name and key name for storing the response data in it.)
    // START
    const val PREFERENCE_NAME = "WeatherAppPreference"
    const val WEATHER_RESPONSE_DATA = "weather_response_data"
    // END

    // TODO Preparing the Internet Connection for RETROFIT and API calls (STEP 3: Add a function to check the network connection is available or not.)
    /**
     * This function is used check the whether the device is connected to the Internet or not.
     */
    fun isNetworkAvailable(context: Context): Boolean {
        // It answers the queries about the state of network connectivity.
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        // Check if the application is connected to the internet depending on the build version of the application
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // If can't get access to the active network return false, otherwise continue with execution
            val network = connectivityManager.activeNetwork ?: return false
            // If the network capabilities is empty return false, otherwise continue with execution
            val activeNetWork = connectivityManager.getNetworkCapabilities(network) ?: return false

            // Think of this as an if statement as shown below
            // when activeNetwork.hasTransport via wifi return true
            // if all the statements about activeNetwork are false just return false
            return when {
                // Check the multiple ways that the device can connect to the internet, if any are satisfied return true, otherwise return false
                activeNetWork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetWork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                activeNetWork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else { // Very old versions
            // Returns details about the currently active default data network.
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnectedOrConnecting
        }
    }

}