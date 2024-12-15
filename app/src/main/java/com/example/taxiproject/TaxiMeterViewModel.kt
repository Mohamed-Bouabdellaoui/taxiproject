package com.example.taxiproject

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.Duration

class TaxiMeterViewModel : ViewModel() {

    // Pricing Constants
    private val BASE_FARE = 2.5 // 2.5 DH
    private val RATE_PER_KM = 1.5 // 1.5 DH per km
    private val RATE_PER_MINUTE = 0.5 // 0.5 DH per minute

    private val _totalDistance = MutableStateFlow(0.0)
    val totalDistance = _totalDistance.asStateFlow()

    private val _elapsedTime = MutableStateFlow(0L)
    val elapsedTime = _elapsedTime.asStateFlow()

    private val _totalFare = MutableStateFlow(0.0)
    val totalFare = _totalFare.asStateFlow()

    private var startLocation: Location? = null
    private var startTime: LocalDateTime? = null
    private var isRideInProgress = false

    fun startRide() {
        isRideInProgress = true
        startTime = LocalDateTime.now()
    }

    fun updateLocation(newLocation: Location) {
        if (!isRideInProgress) return

        startLocation?.let { previousLocation ->
            val distanceIncrement = previousLocation.distanceTo(newLocation) / 1000.0
            _totalDistance.value += distanceIncrement
        }

        startLocation = newLocation

        // Calculate elapsed time
        startTime?.let { start ->
            val currentTime = LocalDateTime.now()
            val minutesElapsed = Duration.between(start, currentTime).toMinutes()
            _elapsedTime.value = minutesElapsed

            // Calculate total fare
            val fareCalculation = BASE_FARE +
                    (RATE_PER_KM * _totalDistance.value) +
                    (RATE_PER_MINUTE * minutesElapsed)
            _totalFare.value = fareCalculation
        }
    }

    fun endRide() {
        isRideInProgress = false
        viewModelScope.launch {
            // Reset or save ride data
            _totalDistance.value = 0.0
            _elapsedTime.value = 0
            _totalFare.value = 0.0
            startLocation = null
            startTime = null
        }
    }
}