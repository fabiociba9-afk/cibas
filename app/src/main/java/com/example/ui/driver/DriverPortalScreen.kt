package com.example.ui.driver

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.entity.TripEntity
import com.example.data.entity.TripStatus
import com.example.data.entity.UserEntity
import com.example.ui.MainViewModel
import com.example.ui.components.ReceiptDialog
import com.example.ui.components.TripStatusBadge
import com.example.ui.theme.*
import com.example.util.Formatters

@Composable
fun DriverPortalScreen(
    viewModel: MainViewModel,
    currentUser: UserEntity,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val drivers by viewModel.drivers.collectAsState()
    val trips by viewModel.trips.collectAsState()
    val isTracking by viewModel.locationTracker.isTracking.collectAsState()
    val currentLocation by viewModel.locationTracker.currentLocation.collectAsState()

    val myDriver = drivers.firstOrNull { it.id == currentUser.driverId } ?: drivers.firstOrNull()
    val myTrips = trips.filter { it.driverId == myDriver?.id }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
        if (isGranted && myDriver != null) {
            viewModel.locationTracker.startContinuousTracking(myDriver.id, true)
        }
    }

    LaunchedEffect(myDriver) {
        if (myDriver != null) {
            viewModel.locationTracker.startContinuousTracking(myDriver.id, hasLocationPermission)
        }
    }

    var selectedTripForReceipt by remember { mutableStateOf<TripEntity?>(null) }

    val completedTrips = myTrips.filter { it.status == TripStatus.CONCLUIDA.name }
    val totalCommissionEarned = completedTrips.sumOf { it.driverCommissionAmount }
    val pendingCommission = completedTrips.filter { !it.isDriverPaid }.sumOf { it.driverCommissionAmount }
    val activeTrips = myTrips.filter { it.status == TripStatus.AGENDADA.name || it.status == TripStatus.EM_ANDAMENTO.name }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SlateLight),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Driver Profile Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth().shadow(3.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NavyPrimary)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    tint = EmeraldDark,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = myDriver?.fullName ?: currentUser.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                                Text(
                                    text = "${myDriver?.vehicleModel} • ${myDriver?.vehiclePlate}",
                                    color = EmeraldContainer,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Surface(
                            color = EmeraldAccent,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${myDriver?.commissionPercentage ?: 75}% COMISSÃO",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Real-Time GPS Tracking Card Section
                    Card(
                        colors = CardDefaults.cardColors(containerColor = NavySecondary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isTracking) EmeraldLight else Color.Gray)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isTracking) "GPS Transmitindo em Tempo Real" else "Transmissor em Pausa",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = "Lat: ${String.format("%.4f", currentLocation?.latitude ?: myDriver?.latitude ?: -23.5874)} | Lng: ${String.format("%.4f", currentLocation?.longitude ?: myDriver?.longitude ?: -46.6823)}",
                                    color = SlateTextSecondary,
                                    fontSize = 11.sp
                                )
                            }

                            if (!hasLocationPermission) {
                                Button(
                                    onClick = {
                                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Ativar GPS", fontSize = 11.sp)
                                }
                            } else {
                                IconButton(
                                    onClick = {
                                        if (myDriver != null) {
                                            Formatters.openInGoogleMaps(
                                                context,
                                                currentLocation?.latitude ?: myDriver.latitude,
                                                currentLocation?.longitude ?: myDriver.longitude,
                                                "Minha Localização"
                                            )
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Map, contentDescription = "Ver no Mapa", tint = EmeraldLight)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Commission Summary Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f).shadow(1.dp, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Total em Comissões", fontSize = 11.sp, color = SlateTextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = Formatters.formatCurrency(totalCommissionEarned),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldAccent
                        )
                        Text("${completedTrips.size} viagens realizadas", fontSize = 10.sp, color = SlateTextSecondary)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f).shadow(1.dp, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("A Receber / Em Aberto", fontSize = 11.sp, color = SlateTextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = Formatters.formatCurrency(pendingCommission),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (pendingCommission > 0) GoldWarning else EmeraldDark
                        )
                        Text("Repasse quinzenal", fontSize = 10.sp, color = SlateTextSecondary)
                    }
                }
            }
        }

        // Section: Active Trips needing action
        item {
            Text(
                text = "Minhas Viagens (${myTrips.size})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = NavyPrimary
            )
        }

        if (myTrips.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Route, contentDescription = null, tint = SlateTextSecondary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Nenhuma viagem atribuída para você no momento.", color = SlateTextSecondary, fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(myTrips) { trip ->
                Card(
                    modifier = Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateCard)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = trip.companyName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = NavyPrimary
                                )
                                Text(
                                    text = "Passageiro: ${trip.requesterName} • ${Formatters.formatDateTime(trip.dateTimeMillis)}",
                                    fontSize = 11.sp,
                                    color = SlateTextSecondary
                                )
                            }
                            TripStatusBadge(trip.status)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Route
                        Text("📍 Origem: ${trip.origin}", fontSize = 12.sp, color = SlateTextPrimary)
                        Text("🏁 Destino: ${trip.destination}", fontSize = 12.sp, color = SlateTextPrimary)

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Sua Comissão:", fontSize = 11.sp, color = SlateTextSecondary)
                                Text(
                                    text = Formatters.formatCurrency(trip.driverCommissionAmount),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldAccent
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Prazo de Recebimento:", fontSize = 11.sp, color = SlateTextSecondary)
                                Text(
                                    text = Formatters.formatDate(trip.driverCommissionDueDateMillis),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NavyPrimary
                                )
                            }
                        }

                        if (trip.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Obs: ${trip.notes}",
                                fontSize = 11.sp,
                                color = SlateTextSecondary,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 10.dp), color = SlateBorder)

                        // Driver Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Status change buttons
                            when (trip.status) {
                                TripStatus.AGENDADA.name -> {
                                    Button(
                                        onClick = { viewModel.updateTripStatus(trip.id, TripStatus.EM_ANDAMENTO) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Iniciar Viagem")
                                    }
                                }
                                TripStatus.EM_ANDAMENTO.name -> {
                                    Button(
                                        onClick = { viewModel.updateTripStatus(trip.id, TripStatus.CONCLUIDA) },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Concluir Viagem")
                                    }
                                }
                                TripStatus.CONCLUIDA.name -> {
                                    Button(
                                        onClick = { selectedTripForReceipt = trip },
                                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Gerar e Enviar Recibo ao Cliente")
                                    }
                                }
                                else -> {
                                    Text("Viagem Cancelada", fontSize = 12.sp, color = RedDanger)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedTripForReceipt != null) {
        ReceiptDialog(
            trip = selectedTripForReceipt!!,
            onDismiss = { selectedTripForReceipt = null }
        )
    }
}
