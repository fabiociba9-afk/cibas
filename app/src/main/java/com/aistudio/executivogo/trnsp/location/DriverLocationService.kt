package com.aistudio.executivogo.trnsp.location

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.aistudio.executivogo.trnsp.MainActivity
import com.aistudio.executivogo.trnsp.R
import com.aistudio.executivogo.trnsp.data.DriverLocation
import com.google.android.gms.location.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class DriverLocationService : Service() {

    companion object {
        private const val TAG = "DriverLocationService"
        const val CHANNEL_ID = "executivogo_driver_location_channel"
        const val NOTIFICATION_ID = 90210

        const val ACTION_START = "com.aistudio.executivogo.trnsp.action.START_LOCATION_TRACKING"
        const val ACTION_STOP = "com.aistudio.executivogo.trnsp.action.STOP_LOCATION_TRACKING"

        const val EXTRA_DRIVER_ID = "extra_driver_id"
        const val EXTRA_DRIVER_NAME = "extra_driver_name"
        const val EXTRA_DRIVER_PHONE = "extra_driver_phone"
        const val EXTRA_VEHICLE_MODEL = "extra_vehicle_model"
        const val EXTRA_VEHICLE_PLATE = "extra_vehicle_plate"
        const val EXTRA_COMPANY_ID = "extra_company_id"

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        private val _currentLocation = MutableStateFlow<Location?>(null)
        val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

        private val _lastUpdateTimestamp = MutableStateFlow(0L)
        val lastUpdateTimestamp: StateFlow<Long> = _lastUpdateTimestamp.asStateFlow()

        fun startTracking(
            context: Context,
            driverId: String,
            driverName: String = "",
            driverPhone: String = "",
            vehicleModel: String = "",
            vehiclePlate: String = "",
            companyId: String = ""
        ) {
            try {
                val appContext = context.applicationContext
                val intent = Intent(appContext, DriverLocationService::class.java).apply {
                    action = ACTION_START
                    putExtra(EXTRA_DRIVER_ID, driverId)
                    putExtra(EXTRA_DRIVER_NAME, driverName)
                    putExtra(EXTRA_DRIVER_PHONE, driverPhone)
                    putExtra(EXTRA_VEHICLE_MODEL, vehicleModel)
                    putExtra(EXTRA_VEHICLE_PLATE, vehiclePlate)
                    putExtra(EXTRA_COMPANY_ID, companyId)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appContext.startForegroundService(intent)
                } else {
                    appContext.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao iniciar DriverLocationService: ${e.message}", e)
            }
        }

        fun stopTracking(context: Context) {
            val intent = Intent(context, DriverLocationService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var wakeLock: PowerManager.WakeLock? = null

    private var driverId: String = ""
    private var driverName: String = ""
    private var driverPhone: String = ""
    private var vehicleModel: String = ""
    private var vehiclePlate: String = ""
    private var companyId: String = ""

    private val firestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()

        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "ExecutivoGo:DriverLocationWakeLock"
        )?.apply {
            setReferenceCounted(false)
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    onNewLocation(location)
                }
            }

            override fun onLocationAvailability(availability: LocationAvailability) {
                super.onLocationAvailability(availability)
                Log.d(TAG, "Location availability: ${availability.isLocationAvailable}")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_START -> {
                driverId = intent?.getStringExtra(EXTRA_DRIVER_ID) ?: driverId
                driverName = intent?.getStringExtra(EXTRA_DRIVER_NAME) ?: driverName
                driverPhone = intent?.getStringExtra(EXTRA_DRIVER_PHONE) ?: driverPhone
                vehicleModel = intent?.getStringExtra(EXTRA_VEHICLE_MODEL) ?: vehicleModel
                vehiclePlate = intent?.getStringExtra(EXTRA_VEHICLE_PLATE) ?: vehiclePlate
                companyId = intent?.getStringExtra(EXTRA_COMPANY_ID) ?: companyId

                startForegroundTracking()
            }
            ACTION_STOP -> {
                stopForegroundTracking()
            }
        }

        return START_STICKY
    }

    private fun startForegroundTracking() {
        if (_isServiceRunning.value) {
            Log.d(TAG, "Service already running in foreground")
            return
        }

        try {
            wakeLock?.acquire(12 * 60 * 60 * 1000L) // até 12 horas de turno
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao adquirir WakeLock: ${e.message}")
        }

        try {
            createNotificationChannel()
            val notification = buildNotification("Iniciando GPS...", "Transmitindo posição em tempo real")

            val foregroundType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            } else {
                0
            }

            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, foregroundType)
            _isServiceRunning.value = true

            requestLocationUpdates()
            Log.i(TAG, "Rastreamento em segundo plano iniciado com sucesso para motorista: $driverId ($driverName)")
        } catch (e: Exception) {
            Log.e(TAG, "Erro em startForegroundTracking: ${e.message}", e)
            _isServiceRunning.value = false
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
            .setMinUpdateIntervalMillis(2500L)
            .setMinUpdateDistanceMeters(2.0f)
            .setWaitForAccurateLocation(false)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Permissão de localização ausente ao solicitar atualizações", e)
            stopForegroundTracking()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao iniciar requestLocationUpdates", e)
        }
    }

    private fun onNewLocation(location: Location) {
        _currentLocation.value = location
        val now = System.currentTimeMillis()
        _lastUpdateTimestamp.value = now

        val speedKmh = if (location.hasSpeed()) (location.speed * 3.6).toDouble() else 0.0
        val bearingDeg = if (location.hasBearing()) location.bearing.toDouble() else 0.0

        // Atualiza a notificação com os dados mais recentes
        val speedStr = if (speedKmh > 1.0) " • %.0f km/h".format(speedKmh) else ""
        val notification = buildNotification(
            title = "ExecutivoGo • Rastreamento Ativo",
            content = "Lat: %.5f, Lng: %.5f%s".format(location.latitude, location.longitude, speedStr)
        )
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)

        // Grava no Firebase Firestore em tempo real
        if (driverId.isNotBlank()) {
            serviceScope.launch {
                try {
                    // 1. Atualiza coleção 'users' (coleção padrão do app)
                    val userUpdates = mapOf<String, Any>(
                        "latitude" to location.latitude,
                        "longitude" to location.longitude,
                        "speed" to speedKmh,
                        "bearing" to bearingDeg,
                        "isOnline" to true,
                        "lastLocationUpdate" to now
                    )
                    firestore.collection("users").document(driverId)
                        .set(userUpdates, SetOptions.merge())
                        .await()

                    // 2. Atualiza coleção 'driver_locations' (telemetria em tempo real)
                    val driverLocation = DriverLocation(
                        id = driverId,
                        driverId = driverId,
                        driverName = driverName,
                        driverPhone = driverPhone,
                        vehicleModel = vehicleModel,
                        vehiclePlate = vehiclePlate,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        speed = speedKmh,
                        bearing = bearingDeg,
                        isOnline = true,
                        companyId = companyId,
                        updatedAt = now
                    )
                    firestore.collection("driver_locations").document(driverId)
                        .set(driverLocation, SetOptions.merge())
                        .await()

                    Log.d(TAG, "Localização gravada no Firebase: Lat ${location.latitude}, Lng ${location.longitude}, Speed $speedKmh km/h")
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao gravar localização no Firestore: ${e.message}")
                }
            }
        }
    }

    private fun stopForegroundTracking() {
        Log.i(TAG, "Parando rastreamento de motorista em segundo plano")
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Erro ao remover locationCallback: ${e.message}")
        }

        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Erro ao liberar WakeLock: ${e.message}")
        }

        // Se houver driverId, atualiza status
        if (driverId.isNotBlank()) {
            serviceScope.launch {
                try {
                    firestore.collection("driver_locations").document(driverId)
                        .update("updatedAt", System.currentTimeMillis())
                        .await()
                } catch (_: Exception) {}
            }
        }

        _isServiceRunning.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(title: String, content: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Rastreamento de Motorista Executivo",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificação permanente de transmissão de GPS contínuo para a central ExecutivoGo"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopForegroundTracking()
        serviceScope.cancel()
    }
}
