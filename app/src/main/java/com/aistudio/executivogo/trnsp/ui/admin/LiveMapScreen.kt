package com.aistudio.executivogo.trnsp.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.executivogo.trnsp.data.TripStatus
import com.aistudio.executivogo.trnsp.navigation.Screen
import com.aistudio.executivogo.trnsp.ui.MainViewModel
import com.aistudio.executivogo.trnsp.ui.components.AdminTopBar
import com.aistudio.executivogo.trnsp.ui.maps.DriverMarkerData
import com.aistudio.executivogo.trnsp.ui.maps.GoogleMapLiveView
import com.aistudio.executivogo.trnsp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveMapScreen(
    viewModel: MainViewModel,
    initialDriverId: String? = null,
    onNavigate: ((String) -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val drivers by viewModel.drivers.collectAsState()
    val driverLocations by viewModel.driverLocations.collectAsState()
    val trips by viewModel.trips.collectAsState()

    val activeTrips = remember(trips) {
        trips.filter {
            it.status.equals(TripStatus.ACCEPTED, ignoreCase = true) ||
            it.status.equals(TripStatus.IN_PROGRESS, ignoreCase = true)
        }
    }

    var selectedFilter by remember { mutableStateOf("TODOS") } // TODOS, ONLINE, EM_VIAGEM
    var selectedDriverId by remember(initialDriverId) { mutableStateOf(initialDriverId) }

    // Constrói lista de marcadores mesclando informações de AppUser + DriverLocation em tempo real
    val markerList = remember(drivers, driverLocations, activeTrips) {
        val locMap = driverLocations.associateBy { it.driverId }
        drivers.map { driver ->
            val loc = locMap[driver.id]
            val trip = activeTrips.find { it.driverId == driver.id }
            val inTrip = trip != null

            // Prioriza coordenadas de telemetria contínua, senão usa as do usuário (sem inventar coordenadas falsas)
            val lat = when {
                loc != null && loc.latitude != 0.0 -> loc.latitude
                driver.latitude != 0.0 -> driver.latitude
                else -> 0.0
            }
            val lng = when {
                loc != null && loc.longitude != 0.0 -> loc.longitude
                driver.longitude != 0.0 -> driver.longitude
                else -> 0.0
            }
            val speed = loc?.speed ?: driver.speed
            val bearing = loc?.bearing ?: driver.bearing
            val isOnline = loc?.isOnline ?: driver.isOnline

            DriverMarkerData(
                id = driver.id,
                name = driver.name.ifBlank { "Motorista Executivo" },
                phone = driver.phone,
                vehicle = "${driver.vehicleModel} ${driver.vehicleColor}".trim(),
                plate = driver.vehiclePlate,
                lat = lat,
                lng = lng,
                speed = speed,
                bearing = bearing,
                isOnline = isOnline,
                inTrip = inTrip,
                currentPassenger = trip?.passengerName ?: "",
                destination = trip?.destination ?: "",
                lastUpdate = loc?.updatedAt ?: driver.lastLocationUpdate
            )
        }
    }

    val filteredMarkers = remember(markerList, selectedFilter) {
        when (selectedFilter) {
            "ONLINE" -> markerList.filter { it.isOnline }
            "EM_VIAGEM" -> markerList.filter { it.inTrip }
            else -> markerList
        }
    }

    Scaffold(
        topBar = {
            AdminTopBar(
                title = "Frota no Google Maps",
                subtitle = "Rastreamento Contínuo em Tempo Real",
                currentScreen = Screen.LiveMap.route,
                onNavigate = onNavigate,
                onLogout = onLogout,
                onRefresh = {
                    viewModel.refreshTrips()
                    viewModel.refreshUsers()
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(NavyPrimary)
        ) {
            // Barra de filtros e status da central
            Surface(
                color = SlateCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldAccent)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Google Maps • GPS Contínuo",
                                color = EmeraldAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "${markerList.count { it.isOnline }} online • ${activeTrips.size} em corrida",
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Filtros
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedFilter == "TODOS",
                            onClick = { selectedFilter = "TODOS" },
                            label = { Text("Todos (${markerList.size})", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldAccent,
                                selectedLabelColor = NavyPrimary,
                                containerColor = NavyPrimary,
                                labelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = selectedFilter == "ONLINE",
                            onClick = { selectedFilter = "ONLINE" },
                            label = { Text("Online (${markerList.count { it.isOnline }})", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldAccent,
                                selectedLabelColor = Color.White,
                                containerColor = NavyPrimary,
                                labelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = selectedFilter == "EM_VIAGEM",
                            onClick = { selectedFilter = "EM_VIAGEM" },
                            label = { Text("Em Corrida (${markerList.count { it.inTrip }})", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF3B82F6),
                                selectedLabelColor = Color.White,
                                containerColor = NavyPrimary,
                                labelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Exibição do Google Maps
            GoogleMapLiveView(
                drivers = filteredMarkers,
                selectedDriverId = selectedDriverId,
                onDriverSelected = { id -> selectedDriverId = id },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                showDriverSelectorBar = true
            )
        }
    }
}
