package com.example.taxiproject

import LocationPermissionHelper
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private lateinit var tvDistance: TextView
    private lateinit var tvTime: TextView
    private lateinit var tvFare: TextView
    private lateinit var btnStartRide: Button

    private lateinit var locationPermissionHelper: LocationPermissionHelper
    private lateinit var locationService: LocationService

    private val taxiMeterViewModel: TaxiMeterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        Configuration.getInstance().load(
            applicationContext,
            getPreferences(MODE_PRIVATE)
        )

        setContentView(R.layout.activity_main)


        mapView = findViewById(R.id.mapView)
        tvDistance = findViewById(R.id.tvDistance)
        tvTime = findViewById(R.id.tvTime)
        tvFare = findViewById(R.id.tvFare)
        btnStartRide = findViewById(R.id.btnStartRide)


        setupMap()


        setupLocationServices()


        observeViewModel()


        setupButtonListeners()
    }

    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
    }

    private fun setupLocationServices() {
        locationPermissionHelper = LocationPermissionHelper(this)
        locationService = LocationService(this)

        locationPermissionHelper.onPermissionResult = { isGranted ->
            if (isGranted) {
                startLocationTracking()
            } else {
                Toast.makeText(this, "Location permissions are required", Toast.LENGTH_LONG).show()
            }
        }

        locationPermissionHelper.checkAndRequestLocationPermissions()
    }

    private fun startLocationTracking() {
        locationService.startLocationUpdates()

        // Observe location changes and update ViewModel
        lifecycleScope.launch {
            locationService.currentLocation.collect { location ->
                location?.let {
                    taxiMeterViewModel.updateLocation(it)

                    // Optional: Update map with current location
                    mapView.controller.setCenter(
                        org.osmdroid.util.GeoPoint(location.latitude, location.longitude)
                    )
                }
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            taxiMeterViewModel.totalDistance.collect { distance ->
                tvDistance.text = "Distance: %.2f km".format(distance)
            }
        }

        lifecycleScope.launch {
            taxiMeterViewModel.elapsedTime.collect { minutes ->
                val hours = TimeUnit.MINUTES.toHours(minutes)
                val remainingMinutes = minutes % 60
                tvTime.text = "Time: %02d:%02d".format(hours, remainingMinutes)
            }
        }

        lifecycleScope.launch {
            taxiMeterViewModel.totalFare.collect { fare ->
                tvFare.text = "Fare: %.2f DH".format(fare)
            }
        }
    }

    private fun setupButtonListeners() {
        btnStartRide.setOnClickListener {
            if (btnStartRide.text == "Start Ride") {
                taxiMeterViewModel.startRide()
                btnStartRide.text = "End Ride"
                btnStartRide.setBackgroundColor(getColor(R.color.red))
            } else {
                taxiMeterViewModel.endRide()
                btnStartRide.text = "Start Ride"
                btnStartRide.setBackgroundColor(getColor(R.color.green))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        locationService.stopLocationUpdates()
    }
}