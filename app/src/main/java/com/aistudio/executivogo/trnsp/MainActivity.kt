package com.aistudio.executivogo.trnsp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.aistudio.executivogo.trnsp.data.ExecutivoGoRepository
import com.aistudio.executivogo.trnsp.navigation.AppNavRoot
import com.aistudio.executivogo.trnsp.ui.MainViewModel
import com.aistudio.executivogo.trnsp.ui.MainViewModelFactory
import com.aistudio.executivogo.trnsp.ui.theme.ExecutivoGoTheme
import com.aistudio.executivogo.trnsp.ui.theme.SlateLight

class MainActivity : ComponentActivity() {

    private val repository by lazy { ExecutivoGoRepository() }
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(application, repository)
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            // Se a permissão básica foi concedida, verificar background se necessário em Android 10+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // Opcional: solicitar background location
                }
            }
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permissão de notificação concedida ou negada
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleNotificationIntent(intent)

        setContent {
            ExecutivoGoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SlateLight
                ) {
                    AppNavRoot(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: android.content.Intent?) {
        if (intent == null) return
        try {
            val tripId = intent.getStringExtra("EXTRA_TRIP_ID")
            val type = intent.getStringExtra("EXTRA_NOTIF_TYPE") ?: "NEW_TRIP"
            if (!tripId.isNullOrBlank()) {
                android.util.Log.d("MainActivity", "Notificação recebida - tripId: $tripId, type: $type")
                viewModel.setPendingNotification(tripId, type)
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Erro ao processar intent de notificação: ${e.message}")
        }
    }

    private fun requestLocationPermissions() {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        val notGranted = permissionsToRequest.any {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted) {
            locationPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
