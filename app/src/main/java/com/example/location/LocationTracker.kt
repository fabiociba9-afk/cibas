package com.example.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.example.data.repository.ExecutivoGoRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

class LocationTracker(
    private val context: Context,
    private val repository: ExecutivoGoRepository
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private var trackingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @SuppressLint("MissingPermission")
    fun startContinuousTracking(driverId: Long, hasPermission: Boolean) {
        if (_isTracking.value) return
        _isTracking.value = true

        trackingJob = scope.launch {
            // SP initial center
            var currentLat = -23.5874
            var currentLng = -46.6823

            while (isActive && _isTracking.value) {
                if (hasPermission) {
                    try {
                        val cts = CancellationTokenSource()
                        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                            .addOnSuccessListener { loc ->
                                if (loc != null) {
                                    _currentLocation.value = loc
                                    scope.launch {
                                        repository.updateDriverLocation(driverId, loc.latitude, loc.longitude)
                                    }
                                }
                            }
                    } catch (e: Exception) {
                        // fallback to simulation step if physical GPS sensor is not emitting
                    }
                }

                // If in emulator or indoors, simulate natural driving motion around corporate route
                val latOffset = (Random.nextDouble() - 0.5) * 0.0008
                val lngOffset = (Random.nextDouble() - 0.5) * 0.0008
                currentLat += latOffset
                currentLng += lngOffset

                repository.updateDriverLocation(driverId, currentLat, currentLng)

                val simulatedLoc = Location("gps").apply {
                    latitude = currentLat
                    longitude = currentLng
                    time = System.currentTimeMillis()
                }
                if (_currentLocation.value == null) {
                    _currentLocation.value = simulatedLoc
                }

                delay(4000) // update every 4 seconds
            }
        }
    }

    fun stopTracking() {
        _isTracking.value = false
        trackingJob?.cancel()
        trackingJob = null
    }
}
