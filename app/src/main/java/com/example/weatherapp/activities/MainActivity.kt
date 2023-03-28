/** Polymorphism has 2 scopes: 1. The calling class scope 1. The functionality class scope
 * Not certain but this phenomena can be seen when in the functionality class scope either a method or interface or even the constructor
 * takes as a parameter an object of the functionality scope
 * This results in the capability of the methods in the functionality class to be defined by the calling class
 * */

package com.example.weatherapp.activities

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import androidx.appcompat.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.example.weatherapp.R
import com.example.weatherapp.databinding.ActivityMainBinding
import com.example.weatherapp.models.WeatherResponse
import com.example.weatherapp.network.WeatherService
import com.example.weatherapp.utils.Constants
import com.google.android.gms.location.*
import com.google.gson.Gson
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import retrofit.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    // TODO Storing Data via Shared Preferences (STEP 1: Add a variable for SharedPreferences)
    // START
    // A global variable for the SharedPreferences
    // We are using this in order to store the result locally in order to ensure even when there is no internet the values are still fed to the field
    private lateinit var mSharedPreferences: SharedPreferences
    // END

    lateinit var binding: ActivityMainBinding
    // TODO Preparing Permission request Using Dexter (STEP 4: Add a variable for FusedLocationProviderClient.)
    // START
    // A fused location client variable which is further used to get the user's current location
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    // END

    // TODO Custom Dialog and Testing Different Internet Speeds (STEP 2: Create a global variable for ProgressDialog.)
    // A global variable for the Progress Dialog
    private var mProgressDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // TODO Storing Data via Shared Preferences (STEP 3: Initialize the SharedPreferences variable.)
        // START
        // Initialize the SharedPreferences variable
        mSharedPreferences = getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE) // Context.MODE_PRIVATE makes it impossible for other applications to steal your goods
        // END

        // TODO Storing Data via Shared Preferences (STEP 7: Call the UI method to populate the data in
        //  the UI which are already stored in sharedPreferences earlier.
        //  At first run it will be blank.)
        // START
        setupUI()
        // END

        // TODO Preparing Permission request Using Dexter (STEP 5: Initialize the fusedLocationProviderClient variable.)
        // START
        // Initialize the Fused location variable
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        // END

        // TODO Preparing The Project And Checking If Location Provider Is Turned On (STEP 4: Check here whether GPS is ON or OFF using the method which we have created)
        // START
        if(!isLocationEnabled()) {
            Toast.makeText(
                this,
                "Your location provider is turned off. Please turn it on.",
                Toast.LENGTH_SHORT
            ).show()

            // TODO Preparing The Project And Checking If Location Provider Is Turned On (STEP 5: This will redirect the user to settings from where they need to turn on the location provider)
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        } else { // GPS present/ON, ask permission for other services
            // TODO Preparing Permission request Using Dexter (STEP 1: Asking the location permission on runtime.)
            // START
            Dexter.withActivity(this)
                .withPermissions(
                    ACCESS_FINE_LOCATION,
                    ACCESS_COARSE_LOCATION
                ).withListener(object : MultiplePermissionsListener {
                    // Check is permissions are accepted
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        if (report!!.areAllPermissionsGranted()) {
                            // TODO Preparing Permission request Using Dexter (STEP 8: Call the location request function here since all necessary permissions have been granted)
                            // START
                            requestLocationData()
                            // END
                        }
                        if (report.isAnyPermissionPermanentlyDenied) {
                            Toast.makeText(
                                this@MainActivity,
                                "You have denied location permission. Please allow it is mandatory.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    // Follow up if permissions are not accepted
                    override fun onPermissionRationaleShouldBeShown(permissions: MutableList<PermissionRequest>?, token: PermissionToken?)
                    {
                        // TODO Preparing Permission request Using Dexter (STEP 3: showRationaleDialogForPermissions placed here)
                        showRationaleDialogForPermissions()
                    }
                }).onSameThread().check()
        }
        // END
    }

    // TODO preparing the Internet Connection for RETROFIT and API calls (STEP 5: Create a function to make an api call using Retrofit Network Library.)
    // START
    /**
     * Function is used to get the weather details of the current location based on the latitude longitude
     */
    private fun getLocationWeatherDetails(longitude: Double,latitude: Double){
        // TODO preparing the Internet Connection for RETROFIT and API calls (STEP 6: Here we will check whether the internet
        //  connection is available or not using the method which
        //  we have created in the Constants object.)
        // START
        if (Constants.isNetworkAvailable(this@MainActivity)) {
            // TODO Creating the API call with Retrofit and Getting a Response (STEP 1: Make an api call using retrofit.This step prepares the URL)
            // Base part of API -> https://api.openweathermap.org/data/2.5/weather
            // START
            val retrofit: Retrofit = Retrofit.Builder()
                // API base URL.
                .baseUrl(Constants.BASE_URL)
                /** Add converter factory for serialization and deserialization of objects. */
                /**
                 GSON is used to convert the result of the API call to the right format
                 */
                .addConverterFactory(GsonConverterFactory.create())
                /** Create the Retrofit instances. */
                .build()
            // END

            // TODO Creating the API call with Retrofit and Getting a Response (STEP 4: Further step for API call. Prepare the service by completing the API call by adding or defining the query values{lat,long,APIkey})
            // Query part of an API -> ?lat=44.34&lon=10.99&appid=ca6ca097dffd1fcacb09116a48b93760
            // START
            /**
            Weather service interface is used to make the API call complete by adding the query
             */
            val service : WeatherService = retrofit.create(WeatherService::class.java)
            /**
            An invocation of a Retrofit method that sends a request to a web-server and returns a response. This is what the Call type does
            Here we pass the required param in the service
             */
            val listCall: Call<WeatherResponse> = service.getWeather(
                latitude, longitude, Constants.METRIC_UNIT, Constants.APP_ID)

            // TODO Custom Dialog and Testing Different Internet Speeds (STEP 4: Show the progress dialog just before the background activities start)
            // START
            showCustomProgressDialog() // Used to show the progress dialog
            // END

            /** enqueue is used to execute the call functionality */
            listCall.enqueue(object: Callback<WeatherResponse> {
                override fun onResponse(response: Response<WeatherResponse>?, retrofit: Retrofit?) {
                    // TODO Creating the API call with Retrofit and Getting a Response (STEP 5:Check weather the response is success or not and act accordingly)
                    if (response!!.isSuccess) {
                        // TODO (STEP 5: Hide the progress dialog since the response has been received)
                        // START
                        hideProgressDialog() // Hides the progress dialog
                        // END

                        /** The de-serialized response body of a successful response. */
                        val weatherList: WeatherResponse = response.body()
                        // TODO Adding the UI and Setting it Up (STEP 3: all the setup UI method here and pass the response object as a parameter to it to get the individual values.)

                        // TODO Storing Data via Shared Preferences (STEP 4: Here we convert the response object to string and store the string in the SharedPreference.)
                        // START
                        val weatherResponseJsonString = Gson().toJson(weatherList) // JSON object converted to string
                        //END
                        // TODO Storing Data via Shared Preferences (STEP 5: Create the sharedPreferences editor that will enable storage of data
                        val editor = mSharedPreferences.edit()
                                            // Key                              //DATA
                        editor.putString(Constants.WEATHER_RESPONSE_DATA, weatherResponseJsonString)
                        editor.apply()

                        setupUI()
                        Log.i("Response Result", "$weatherList")
                    } else {
                        // If the response is not success then we check the response code.
                        val sc = response.code()
                        when (sc) {
                            400 -> {
                                Log.e("Error 400", "Bad Request")
                            }
                            404 -> {
                                Log.e("Error 404", "Not Found")
                            }
                            else -> {
                                Log.e("Error", "Generic Error")
                            }
                        }
                    }
                }
                override fun onFailure(t: Throwable?) {
                    Log.e("Errorrrrr", t!!.message.toString())

                    // TODO (STEP 5: Hide the progress dialog since the response has been received)
                    // START
                    hideProgressDialog() // Hides the progress dialog
                    // END
                }
            })
        } else {
            Toast.makeText(
                this@MainActivity,
                "No internet connection available.",
                Toast.LENGTH_SHORT
            ).show()
        }
        // END
    }
    // END


    // TODO Preparing Permission request Using Dexter (STEP 6: Add a function to get the location of the device using the fusedLocationProviderClient.)
    // START
    /**
     * A function to request the current location. Using the fused location provider client.
     */
    @SuppressLint("MissingPermission")
    private fun requestLocationData() {
        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()
        )
    }
    // END

    // TODO Preparing Permission request Using Dexter (STEP 7: Register a request location callback to get the location.)
    // START
    /**
     * A location callback object of fused location provider client where we will get the current location details.
     */
    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location? = locationResult.lastLocation
            val latitude = mLastLocation?.latitude
            Log.i("Current Latitude", "$latitude")
            val longitude = mLastLocation?.longitude
            Log.i("Current Longitude", "$longitude")

            // TODO preparing the Internet Connection for RETROFIT and API calls (STEP 7: Call the api calling function here since after getting the longitude and latitude they will be used to create an api call that will return the weather .)
            getLocationWeatherDetails(longitude as Double,latitude as Double)
        }
    }
    // END

    // TODO Preparing Permission request Using Dexter (STEP 2: An alert dialog for denied permissions and if needed to allow it from the settings app info.)

    private fun showRationaleDialogForPermissions() {
        AlertDialog.Builder(this)
            .setMessage("It Looks like you have turned off permissions required for this feature. It can be enabled under Application Settings")
            .setPositiveButton(
                "GO TO SETTINGS")
            { _,_ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                }catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel")
            { dialog,_ ->
                dialog.dismiss()
            }.show()
    }

    // TODO Preparing The Project And Checking If Location Provider Is Turned On (STEP 3: Check whether the GPS is ON or OFF)
    // START
    /**
     * A function which is used to verify that the location or GPS is enabled or not.
     */
    private fun isLocationEnabled(): Boolean {
        // This provides access to the system location services.
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
    // END

    // TODO Custom Dialog and Testing Different Internet Speeds (STEP 3: Create a functions for SHOW and HIDE progress dialog.)
    // START
    /**
     * Method is used to show the Custom Progress Dialog.
     */
    private fun showCustomProgressDialog() {
        mProgressDialog = Dialog(this)

        /*Set the screen content from a layout resource.
        The resource will be inflated, adding all top-level views to the screen.*/
        mProgressDialog!!.setContentView(R.layout.dialog_custom_progress)

        //Start the dialog and display it on screen.
        mProgressDialog!!.show()
    }

    /**
     * This function is used to dismiss the progress dialog if it is visible to user.
     */
    private fun hideProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog!!.dismiss()
        }
    }
    // END
    // TODO Adding the UI and Setting it Up (STEP 2: We have set the values to the UI and also added some required methods for Unit and Time below.)
    /**
     * Function is used to set the result in the UI elements.
     */
    @SuppressLint("SetTextI18n")
    private fun setupUI() {

        // TODO Storing Data via Shared Preferences (STEP 6: Here we get the stored response from
        //  SharedPreferences and again convert back to data object
        //  to populate the data in the UI.)
        val weatherResponseJsonString = mSharedPreferences.getString(Constants.WEATHER_RESPONSE_DATA, "")
        if (!weatherResponseJsonString.isNullOrEmpty()) {
            val weatherList = Gson().fromJson(weatherResponseJsonString, WeatherResponse::class.java) // Convert string to JSON
            // For loop to get the required data. And all are populated in the UI.
            for (z in weatherList.weather.indices) {
                Log.i("NAMEEEEEEEE", weatherList.weather[z].main)

                binding.tvMain.text = weatherList.weather[z].main
                binding.tvMainDescription.text = weatherList.weather[z].description
                binding.tvTemp.text =
                    weatherList.main.temp.toString() + getUnit(application.resources.configuration.locales.toString())
                binding.tvHumidity.text = weatherList.main.humidity.toString() + " per cent"
                binding.tvMin.text = weatherList.main.temp_min.toString() + " min"
                binding.tvMax.text = weatherList.main.temp_max.toString() + " max"
                binding.tvSpeed.text = weatherList.wind.speed.toString()
                binding.tvName.text = weatherList.name
                binding.tvCountry.text = weatherList.sys.country
                binding.tvSunriseTime.text = unixTime(weatherList.sys.sunrise.toLong())
                binding.tvSunsetTime.text = unixTime(weatherList.sys.sunset.toLong())

                // Here we update the main icon
                when (weatherList.weather[z].icon) {
                    "01d" -> binding.ivMain.setImageResource(R.drawable.sunny)
                    "02d" -> binding.ivMain.setImageResource(R.drawable.cloud)
                    "03d" -> binding.ivMain.setImageResource(R.drawable.cloud)
                    "04d" -> binding.ivMain.setImageResource(R.drawable.cloud)
                    "04n" -> binding.ivMain.setImageResource(R.drawable.cloud)
                    "10d" -> binding.ivMain.setImageResource(R.drawable.rain)
                    "11d" -> binding.ivMain.setImageResource(R.drawable.storm)
                    "13d" -> binding.ivMain.setImageResource(R.drawable.snowflake)
                    "01n" -> binding.ivMain.setImageResource(R.drawable.cloud)
                    "02n" -> binding.ivMain.setImageResource(R.drawable.cloud)
                    "03n" -> binding.ivMain.setImageResource(R.drawable.cloud)
                    "10n" -> binding.ivMain.setImageResource(R.drawable.cloud)
                    "11n" -> binding.ivMain.setImageResource(R.drawable.rain)
                    "13n" -> binding.ivMain.setImageResource(R.drawable.snowflake)
                }
            }
        }
    }

    /**
     * Function is used to get the temperature unit value.
     */
    private fun getUnit(value: String): String {
        Log.i("unitttttt", value)
        var value = "°C"
        // If the value passed is imperial change to imperial units, otherwise remain with the metric system
        if ("US" == value || "LR" == value || "MM" == value) {
            value = "°F"
        }
        return value
    }

    /**
     * The function is used to get the formatted time based on the Format and the LOCALE we pass to it.
     */
    private fun unixTime(timex: Long): String? {
        val date = Date(timex * 1000L) // Convert to milliseconds for Date to use properly
        @SuppressLint("SimpleDateFormat") val sdf =
            SimpleDateFormat("HH:mm",Locale.UK) //  HH gives 24hr format
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(date)
    }
    // END

    // TODO Adding A Refresh Button  (STEP 2: Now add the override methods to load the menu file and perform the selection on item click.)
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    // TODO Adding A Refresh Button (STEP 3: Now create a method that wil handle the click events of the created menu item.)
    // START
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            // TODO Adding A Refresh Button (STEP 4: Now finally, make an api call on item selection.)
            // START
            R.id.action_refresh -> {
                requestLocationData()
                true

            }
            else -> super.onOptionsItemSelected(item)
            // END
        }
    }
    // END
}