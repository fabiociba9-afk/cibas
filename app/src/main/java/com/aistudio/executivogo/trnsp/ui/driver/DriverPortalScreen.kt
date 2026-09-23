package com.aistudio.executivogo.trnsp.ui.driver

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aistudio.executivogo.trnsp.data.PaymentMeans
import com.aistudio.executivogo.trnsp.data.PaymentStatus
import com.aistudio.executivogo.trnsp.data.PaymentTerms
import com.aistudio.executivogo.trnsp.data.Trip
import com.aistudio.executivogo.trnsp.data.TripStatus
import com.aistudio.executivogo.trnsp.ui.FinancialFilter
import com.aistudio.executivogo.trnsp.ui.MainViewModel
import com.aistudio.executivogo.trnsp.ui.NavigationHelper
import com.aistudio.executivogo.trnsp.ui.PeriodType
import com.aistudio.executivogo.trnsp.ui.TripStatusBadge
import com.aistudio.executivogo.trnsp.ui.WhatsAppHelper
import com.aistudio.executivogo.trnsp.ui.components.ExecutivePullRefreshBox
import com.aistudio.executivogo.trnsp.ui.executiveTextFieldColors
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.aistudio.executivogo.trnsp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverPortalScreen(
    viewModel: MainViewModel,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val driverTrips by viewModel.driverTrips.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var selectedSection by remember { mutableIntStateOf(0) } // 0: Corridas, 1: Meu Financeiro
    var locationPermissionGranted by remember { mutableStateOf(false) }
    var backgroundLocationGranted by remember { mutableStateOf(false) }
    var notificationPermissionGranted by remember { mutableStateOf(false) }

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        backgroundLocationGranted = granted
        if (granted) {
            try {
                viewModel.startDriverTracking(context)
            } catch (_: Exception) {}
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationPermissionGranted = granted
        try {
            com.aistudio.executivogo.trnsp.MyFirebaseMessagingService.createNotificationChannel(context)
            viewModel.startDriverTracking(context)
        } catch (_: Exception) {}

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && !backgroundLocationGranted) {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    backgroundLocationLauncher.launch(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                } catch (_: Exception) {}
            }, 400)
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        locationPermissionGranted = granted
        if (granted) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU && !notificationPermissionGranted) {
                try {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                } catch (_: Exception) {}
            } else {
                try {
                    com.aistudio.executivogo.trnsp.MyFirebaseMessagingService.createNotificationChannel(context)
                    viewModel.startDriverTracking(context)
                } catch (_: Exception) {}
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && !backgroundLocationGranted) {
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        try {
                            backgroundLocationLauncher.launch(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        } catch (_: Exception) {}
                    }, 400)
                }
            }
        }
    }

    LaunchedEffect(currentUser?.id) {
        val uid = currentUser?.id
        if (!uid.isNullOrBlank()) {
            viewModel.loadTripsForDriver(uid)

            val hasFineCoarse = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            locationPermissionGranted = hasFineCoarse

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                backgroundLocationGranted = ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionGranted = ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            }

            if (hasFineCoarse) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU && !notificationPermissionGranted) {
                    try {
                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    } catch (_: Exception) {}
                } else {
                    try {
                        com.aistudio.executivogo.trnsp.MyFirebaseMessagingService.createNotificationChannel(context)
                        viewModel.startDriverTracking(context)
                    } catch (_: Exception) {}
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && !backgroundLocationGranted) {
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            try {
                                backgroundLocationLauncher.launch(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                            } catch (_: Exception) {}
                        }, 400)
                    }
                }
            } else {
                try {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                } catch (_: Exception) {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Portal do Motorista", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = currentUser?.name ?: "Motorista Executivo",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshDriverTrips() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atualizar", tint = Color.White)
                    }
                    IconButton(onClick = { viewModel.logout(onLogout) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Sair",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavyPrimary,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = SlateLight,
        modifier = modifier
    ) { paddingValues ->
        ExecutivePullRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshDriverTrips() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                val needBg = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && !backgroundLocationGranted
                val needNotif = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU && !notificationPermissionGranted

                if (!locationPermissionGranted || needBg || needNotif) {
                    val warningText = when {
                        !locationPermissionGranted -> "Ative a localização para aparecer no mapa"
                        needBg -> "Para rastreamento contínuo, escolha 'Permitir o tempo todo' / 'Allow all the time'."
                        else -> "Ative as notificações para receber alertas de novas corridas."
                    }
                    Surface(
                        color = Color(0xFFFEF3C7),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFB45309))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = warningText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E)
                                )
                            }
                            TextButton(onClick = {
                                when {
                                    !locationPermissionGranted -> {
                                        try {
                                            locationPermissionLauncher.launch(
                                                arrayOf(
                                                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                                                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                                                )
                                            )
                                        } catch (_: Exception) {}
                                    }
                                    needBg -> {
                                        try {
                                            backgroundLocationLauncher.launch(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                                        } catch (_: Exception) {}
                                    }
                                    needNotif -> {
                                        try {
                                            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                        } catch (_: Exception) {}
                                    }
                                }
                            }) {
                                Text("Permitir", fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                            }
                        }
                    }
                }

                // Persistent Active Status Bar (No switch)
                Surface(
                    color = NavyDark,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldLight)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ONLINE • Conectado à central",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = "Repasse: ${currentUser?.commissionPercentage ?: 20.0}%",
                            color = EmeraldLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                // Section Tabs
                TabRow(
                    selectedTabIndex = selectedSection,
                    containerColor = NavyPrimary,
                    contentColor = Color.White
                ) {
                    Tab(
                        selected = selectedSection == 0,
                        onClick = { selectedSection = 0 },
                        text = {
                            Text(
                                "Minhas Corridas",
                                fontWeight = if (selectedSection == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedSection == 0) EmeraldLight else Color(0xFFCBD5E1)
                            )
                        }
                    )
                    Tab(
                        selected = selectedSection == 1,
                        onClick = { selectedSection = 1 },
                        text = {
                            Text(
                                "Meu Financeiro",
                                fontWeight = if (selectedSection == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedSection == 1) EmeraldLight else Color(0xFFCBD5E1)
                            )
                        }
                    )
                }

                if (selectedSection == 0) {
                    DriverTripsView(
                        trips = driverTrips,
                        driverName = currentUser?.name ?: "Motorista",
                        onSeekPassenger = { trip -> viewModel.updateTripStatus(trip.id, TripStatus.ACCEPTED) },
                        onStartTrip = { trip -> viewModel.updateTripStatus(trip.id, TripStatus.IN_PROGRESS) },
                        onConfirmStop = { trip -> viewModel.confirmTripStop(trip) },
                        onCompleteTrip = { trip -> viewModel.completeTrip(trip) },
                        onRejectTrip = { trip, reason -> viewModel.rejectTrip(trip, reason) }
                    )
                } else {
                    DriverFinancialView(
                        trips = driverTrips,
                        currentDriverId = currentUser?.id,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

// ==========================================
// SEÇÃO: CORRIDAS DO MOTORISTA
// ==========================================
@Composable
fun DriverTripsView(
    trips: List<Trip>,
    driverName: String = "",
    onSeekPassenger: (Trip) -> Unit,
    onStartTrip: (Trip) -> Unit,
    onConfirmStop: (Trip) -> Unit,
    onCompleteTrip: (Trip) -> Unit,
    onRejectTrip: (Trip, String) -> Unit
) {
    val context = LocalContext.current
    var selectedStatusFilter by remember { mutableStateOf("TODAS") }
    var tripToReject by remember { mutableStateOf<Trip?>(null) }
    var tripForWhatsApp by remember { mutableStateOf<Trip?>(null) }
    var stopToConfirm by remember { mutableStateOf<Pair<Trip, Int>?>(null) }

    val pendingCount = remember(trips) { trips.count { it.status == TripStatus.PENDING || it.status == TripStatus.ACCEPTED } }
    val inProgressCount = remember(trips) { trips.count { it.status == TripStatus.IN_PROGRESS } }
    val completedCount = remember(trips) { trips.count { it.status == TripStatus.COMPLETED } }
    val cancelledCount = remember(trips) { trips.count { it.status == TripStatus.CANCELLED } }

    val filteredTrips = remember(trips, selectedStatusFilter) {
        when (selectedStatusFilter) {
            "PENDING" -> trips.filter { it.status == TripStatus.PENDING || it.status == TripStatus.ACCEPTED }
            "IN_PROGRESS" -> trips.filter { it.status == TripStatus.IN_PROGRESS }
            "COMPLETED" -> trips.filter { it.status == TripStatus.COMPLETED }
            "CANCELLED" -> trips.filter { it.status == TripStatus.CANCELLED }
            else -> trips
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Filtro de Viagens por Status
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedStatusFilter == "TODAS",
                onClick = { selectedStatusFilter = "TODAS" },
                label = { Text("Todas (${trips.size})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NavyPrimary,
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = selectedStatusFilter == "PENDING",
                onClick = { selectedStatusFilter = "PENDING" },
                label = { Text("A Fazer ($pendingCount)") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AmberWarning,
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = selectedStatusFilter == "IN_PROGRESS",
                onClick = { selectedStatusFilter = "IN_PROGRESS" },
                label = { Text("Em Andamento ($inProgressCount)") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF0284C7),
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = selectedStatusFilter == "COMPLETED",
                onClick = { selectedStatusFilter = "COMPLETED" },
                label = { Text("Concluídas ($completedCount)") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EmeraldAccent,
                    selectedLabelColor = Color.White
                )
            )
            if (cancelledCount > 0) {
                FilterChip(
                    selected = selectedStatusFilter == "CANCELLED",
                    onClick = { selectedStatusFilter = "CANCELLED" },
                    label = { Text("Canceladas ($cancelledCount)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = RedDanger,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        if (filteredTrips.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = SlateTextSecondary,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (trips.isEmpty()) "Você não possui viagens atribuídas no momento."
                               else "Nenhuma viagem encontrada para este filtro.",
                        color = SlateTextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredTrips, key = { it.id }) { trip ->
                    val validStops = remember(trip.stops) {
                        trip.stops.map { it.trim() }.filter { it.isNotBlank() }
                    }
                    val hasStops = validStops.isNotEmpty()
                    val isStarted = trip.status == TripStatus.IN_PROGRESS
                    val isCompleted = trip.status == TripStatus.COMPLETED
                    val hasPendingStops = hasStops && trip.currentStopIndex < validStops.size

                    // Endereço e rótulo de navegação dinâmica para Waze e Google Maps
                    val navAddress: String
                    val navTargetLabel: String
                    val gpsIndicatorText: String

                    if (!isStarted && !isCompleted) {
                        navAddress = trip.origin
                        navTargetLabel = if (trip.status == TripStatus.ACCEPTED) "Passageiro" else "Origem"
                        gpsIndicatorText = if (trip.status == TripStatus.ACCEPTED) "GPS direciona para BUSCAR PASSAGEIRO (Embarque)"
                                          else "GPS direciona para ORIGEM (Embarque)"
                    } else if (hasPendingStops) {
                        val stopNum = trip.currentStopIndex + 1
                        navAddress = validStops[trip.currentStopIndex]
                        navTargetLabel = "Parada $stopNum"
                        gpsIndicatorText = "GPS direciona para PARADA $stopNum"
                    } else {
                        navAddress = trip.destination
                        navTargetLabel = "Destino"
                        gpsIndicatorText = if (hasStops) "GPS direciona para DESTINO FINAL (Desembarque)"
                                          else "GPS direciona para DESTINO (Desembarque)"
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(16.dp)),
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
                                        text = trip.passengerName.ifBlank { "Passageiro Executivo" },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = NavyPrimary
                                    )
                                    Text(
                                        text = "Tel: ${trip.passengerPhone.ifBlank { "Não informado" }}",
                                        fontSize = 12.sp,
                                        color = SlateTextSecondary
                                    )
                                }
                                TripStatusBadge(status = trip.status)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Agendamento de Data e Horário
                            if (trip.scheduledTime > 0) {
                                val formattedSchedule = remember(trip.scheduledTime) {
                                    val sdf = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR"))
                                    sdf.format(Date(trip.scheduledTime))
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = EmeraldDark,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Horário: $formattedSchedule",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldDark
                                    )
                                }
                            }

                            // 1. Progresso Visual da Rota (✓ Origem → Parada 1 → Destino Final)
                            TripProgressTracker(
                                trip = trip,
                                validStops = validStops
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // 2. Destaque da Etapa Atual (PRÓXIMA PARADA ou DESTINO FINAL)
                            CurrentStageHighlightCard(
                                isStarted = isStarted,
                                isCompleted = isCompleted,
                                hasStops = hasStops,
                                currentStopIndex = trip.currentStopIndex,
                                validStops = validStops,
                                tripOrigin = trip.origin,
                                tripDestination = trip.destination
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Somente comissão - motorista NUNCA vê price total
                            Surface(
                                color = EmeraldContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Sua Comissão:",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = EmeraldDark
                                    )
                                    Text(
                                        text = "R$ %.2f".format(trip.driverCommission),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = EmeraldDark
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Info da Navegação GPS Inteligente
                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Navigation,
                                        contentDescription = null,
                                        tint = if (!isStarted) EmeraldDark else if (hasPendingStops) Color(0xFFD97706) else Color(0xFF0284C7),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = gpsIndicatorText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!isStarted) EmeraldDark else if (hasPendingStops) Color(0xFFB45309) else Color(0xFF0284C7)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Botões de Navegação GPS (Waze e Maps direcionando dinamicamente)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { NavigationHelper.openWaze(context, navAddress) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NavyPrimary),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp), tint = NavyPrimary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Waze ($navTargetLabel)", fontSize = 12.sp, color = NavyPrimary, fontWeight = FontWeight.SemiBold)
                                }
                                OutlinedButton(
                                    onClick = { NavigationHelper.openGoogleMaps(context, navAddress) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NavyPrimary),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp), tint = NavyPrimary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Maps ($navTargetLabel)", fontSize = 12.sp, color = NavyPrimary, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Botão WhatsApp Passageiro
                            Button(
                                onClick = { tripForWhatsApp = trip },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Chat,
                                    contentDescription = "WhatsApp",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Mensagem WhatsApp ao Passageiro",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Ações de Corrida
                            when (trip.status) {
                                TripStatus.PENDING -> {
                                    // 1. Viagem acabou de ser atribuída -> Motorista deve clicar primeiro em "Buscar Passageiro"
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                onSeekPassenger(trip)
                                                NavigationHelper.openGoogleMaps(context, trip.origin)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1.3f)
                                        ) {
                                            Icon(
                                                Icons.Default.DirectionsCar,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                "Buscar Passageiro",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = { tripToReject = trip },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RedDanger),
                                            border = BorderStroke(1.dp, RedDanger),
                                            modifier = Modifier.weight(0.9f)
                                        ) {
                                            Icon(Icons.Default.Cancel, contentDescription = null, tint = RedDanger, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Recusar", color = RedDanger, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                                TripStatus.ACCEPTED -> {
                                    // 2. Motorista já clicou em "Buscar Passageiro" -> Agora o botão "Iniciar" fica liberado
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            color = Color(0xFFEFF6FF),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.Info,
                                                    contentDescription = null,
                                                    tint = Color(0xFF1D4ED8),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "A caminho do passageiro. Ao embarcar o passageiro, clique em Iniciar.",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF1E40AF),
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { onStartTrip(trip) },
                                                colors = executiveButtonPrimaryColors(),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1.3f)
                                            ) {
                                                Icon(
                                                    Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Iniciar", color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                            OutlinedButton(
                                                onClick = { tripToReject = trip },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = RedDanger),
                                                border = BorderStroke(1.dp, RedDanger),
                                                modifier = Modifier.weight(0.9f)
                                            ) {
                                                Icon(Icons.Default.Cancel, contentDescription = null, tint = RedDanger, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Recusar", color = RedDanger, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                                TripStatus.IN_PROGRESS -> {
                                    if (hasPendingStops) {
                                        val stopNum = trip.currentStopIndex + 1
                                        Button(
                                            onClick = { stopToConfirm = Pair(trip, trip.currentStopIndex) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Confirmar Parada $stopNum", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }
                                    } else {
                                        Button(
                                            onClick = { onCompleteTrip(trip) },
                                            colors = executiveButtonPrimaryColors(),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Concluir Viagem", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                TripStatus.COMPLETED -> {
                                    Surface(
                                        color = Color(0xFFF1F5F9),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Viagem Finalizada com Sucesso",
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            fontSize = 12.sp,
                                            color = SlateTextSecondary,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (stopToConfirm != null) {
        val (trip, stopIdx) = stopToConfirm!!
        ConfirmStopDialog(
            trip = trip,
            stopIndex = stopIdx,
            onDismiss = { stopToConfirm = null },
            onConfirm = {
                onConfirmStop(trip)
                stopToConfirm = null
            }
        )
    }

    if (tripToReject != null) {
        RejectTripDialog(
            trip = tripToReject!!,
            onDismiss = { tripToReject = null },
            onConfirmReject = { reason ->
                onRejectTrip(tripToReject!!, reason)
                tripToReject = null
            }
        )
    }

    if (tripForWhatsApp != null) {
        DriverTripWhatsAppDialog(
            trip = tripForWhatsApp!!,
            driverName = driverName,
            onDismiss = { tripForWhatsApp = null }
        )
    }
}

// ==========================================
// COMPONENTES AUXILIARES DE PARADAS E NAVEGAÇÃO
// ==========================================

enum class ProgressStepState {
    COMPLETED, // ✓
    CURRENT,   // ➔
    PENDING    // ○
}

private data class ProgressRowStyle(
    val icon: String,
    val iconColor: Color,
    val textColor: Color,
    val textWeight: FontWeight,
    val bgColor: Color,
    val borderColor: Color
)

@Composable
fun TripProgressTracker(
    trip: Trip,
    validStops: List<String>,
    modifier: Modifier = Modifier
) {
    val isStarted = trip.status == TripStatus.IN_PROGRESS
    val isCompleted = trip.status == TripStatus.COMPLETED
    val currentStop = trip.currentStopIndex

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
            .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "PROGRESSO DA ROTA",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = SlateTextSecondary,
            letterSpacing = 0.5.sp
        )

        // 1. Origem
        val origemStatus = when {
            isCompleted || isStarted -> ProgressStepState.COMPLETED
            else -> ProgressStepState.CURRENT
        }
        ProgressStepRow(
            label = "Origem",
            address = trip.origin,
            state = origemStatus
        )

        // 2. Paradas Intermediárias (se houver)
        validStops.forEachIndexed { idx, stopAddress ->
            val stopState = when {
                isCompleted -> ProgressStepState.COMPLETED
                !isStarted -> ProgressStepState.PENDING
                idx < currentStop -> ProgressStepState.COMPLETED
                idx == currentStop -> ProgressStepState.CURRENT
                else -> ProgressStepState.PENDING
            }
            ProgressStepRow(
                label = "Parada ${idx + 1}",
                address = stopAddress,
                state = stopState
            )
        }

        // 3. Destino Final
        val destinoState = when {
            isCompleted -> ProgressStepState.COMPLETED
            !isStarted -> ProgressStepState.PENDING
            validStops.isEmpty() -> ProgressStepState.CURRENT
            currentStop >= validStops.size -> ProgressStepState.CURRENT
            else -> ProgressStepState.PENDING
        }
        ProgressStepRow(
            label = "Destino Final",
            address = trip.destination,
            state = destinoState
        )
    }
}

@Composable
fun ProgressStepRow(
    label: String,
    address: String,
    state: ProgressStepState
) {
    val style = when (state) {
        ProgressStepState.COMPLETED -> ProgressRowStyle(
            icon = "✓",
            iconColor = EmeraldAccent,
            textColor = NavyPrimary,
            textWeight = FontWeight.SemiBold,
            bgColor = Color.Transparent,
            borderColor = Color.Transparent
        )
        ProgressStepState.CURRENT -> ProgressRowStyle(
            icon = "➔",
            iconColor = Color(0xFF2563EB),
            textColor = NavyPrimary,
            textWeight = FontWeight.Bold,
            bgColor = Color(0xFFEFF6FF),
            borderColor = Color(0xFF93C5FD)
        )
        ProgressStepState.PENDING -> ProgressRowStyle(
            icon = "○",
            iconColor = Color(0xFF94A3B8),
            textColor = SlateTextSecondary,
            textWeight = FontWeight.Normal,
            bgColor = Color.Transparent,
            borderColor = Color.Transparent
        )
    }

    Surface(
        color = style.bgColor,
        shape = RoundedCornerShape(6.dp),
        border = if (state == ProgressStepState.CURRENT) BorderStroke(1.dp, style.borderColor) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(
                        when (state) {
                            ProgressStepState.COMPLETED -> Color(0xFFDCFCE7)
                            ProgressStepState.CURRENT -> Color(0xFFDBEAFE)
                            ProgressStepState.PENDING -> Color(0xFFF1F5F9)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = style.icon,
                    color = style.iconColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = style.textWeight,
                        color = if (state == ProgressStepState.CURRENT) Color(0xFF1D4ED8) else style.textColor
                    )
                    if (state == ProgressStepState.CURRENT) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Color(0xFF2563EB),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "ETAPA ATUAL",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    } else if (state == ProgressStepState.COMPLETED) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(Concluída)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Normal,
                            color = EmeraldDark
                        )
                    }
                }
                Text(
                    text = address,
                    fontSize = 11.sp,
                    color = if (state == ProgressStepState.CURRENT) NavyPrimary else SlateTextSecondary,
                    fontWeight = if (state == ProgressStepState.CURRENT) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun CurrentStageHighlightCard(
    isStarted: Boolean,
    isCompleted: Boolean,
    hasStops: Boolean,
    currentStopIndex: Int,
    validStops: List<String>,
    tripOrigin: String,
    tripDestination: String
) {
    if (isCompleted) {
        Surface(
            color = Color(0xFFF1F5F9),
            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("VIAGEM FINALIZADA", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = EmeraldDark)
                    Text("Todas as etapas foram concluídas com sucesso.", fontSize = 12.sp, color = SlateTextSecondary)
                }
            }
        }
        return
    }

    if (!isStarted) {
        // Antes de iniciar -> Embarque do passageiro
        Surface(
            color = Color(0xFFEFF6FF),
            border = BorderStroke(1.5.dp, Color(0xFF3B82F6)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = Color(0xFF1D4ED8), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PRÓXIMO EMBARQUE", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1D4ED8), letterSpacing = 0.5.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "📍 $tripOrigin",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )
            }
        }
        return
    }

    // Em andamento
    val hasPendingStops = hasStops && currentStopIndex < validStops.size
    if (hasPendingStops) {
        val stopNum = currentStopIndex + 1
        val stopAddress = validStops[currentStopIndex]

        Surface(
            color = Color(0xFFFFFBEB),
            border = BorderStroke(1.5.dp, Color(0xFFF59E0B)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "PRÓXIMA PARADA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFB45309),
                            letterSpacing = 0.5.sp
                        )
                    }
                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B))
                    ) {
                        Text(
                            text = "Parada $stopNum de ${validStops.size}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Parada $stopNum: $stopAddress",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            }
        }
    } else {
        // Destino Final
        Surface(
            color = Color(0xFFECFDF5),
            border = BorderStroke(1.5.dp, EmeraldAccent),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Flag, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "DESTINO FINAL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldDark,
                            letterSpacing = 0.5.sp
                        )
                    }
                    if (hasStops) {
                        Surface(
                            color = Color(0xFFD1FAE5),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, EmeraldLight)
                        ) {
                            Text(
                                text = "Paradas Concluídas ✓",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldDark,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "📍 $tripDestination",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            }
        }
    }
}

@Composable
fun ConfirmStopDialog(
    trip: Trip,
    stopIndex: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val validStops = remember(trip.stops) { trip.stops.map { it.trim() }.filter { it.isNotBlank() } }
    val stopNum = stopIndex + 1
    val stopAddress = validStops.getOrElse(stopIndex) { "" }
    val isLastStop = stopIndex >= validStops.size - 1
    val nextDestination = if (isLastStop) "Destino Final (${trip.destination})" else "Parada ${stopNum + 1} (${validStops.getOrElse(stopIndex + 1) { "" }})"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = EmeraldAccent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Confirmar Parada $stopNum",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = NavyPrimary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Você confirma que concluiu a parada intermediária:",
                    fontSize = 13.sp,
                    color = SlateTextSecondary
                )
                Surface(
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "PARADA $stopNum",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF92400E)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "📍 $stopAddress",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF78350F)
                        )
                    }
                }
                Surface(
                    color = Color(0xFFEFF6FF),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF93C5FD)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "PRÓXIMA ETAPA DO GPS:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E40AF)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "➔ $nextDestination",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NavyPrimary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Confirmar Parada", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Voltar", color = SlateTextSecondary)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun RejectTripDialog(
    trip: Trip,
    onDismiss: () -> Unit,
    onConfirmReject: (String) -> Unit
) {
    val reasons = listOf(
        "Agenda ocupada",
        "Valor baixo",
        "Veículo em manutenção",
        "Outro motivo"
    )
    var selectedReason by remember { mutableStateOf(reasons.first()) }
    var customDetails by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Surface(
                color = RedDanger,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Cancel,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Recusar Viagem",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 17.sp
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Selecione o motivo da recusa desta corrida para que a central possa reatribuí-la:",
                    fontSize = 13.sp,
                    color = SlateTextPrimary
                )

                reasons.forEach { reason ->
                    Surface(
                        color = if (selectedReason == reason) Color(0xFFFEF2F2) else SlateCard,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            1.dp,
                            if (selectedReason == reason) RedDanger else SlateBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedReason == reason,
                                onClick = { selectedReason = reason },
                                colors = RadioButtonDefaults.colors(selectedColor = RedDanger)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = reason,
                                fontSize = 13.sp,
                                fontWeight = if (selectedReason == reason) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedReason == reason) RedDanger else SlateTextPrimary
                            )
                        }
                    }
                }

                if (selectedReason == "Outro motivo") {
                    OutlinedTextField(
                        value = customDetails,
                        onValueChange = { customDetails = it },
                        label = { Text("Descreva o motivo *") },
                        colors = executiveTextFieldColors(),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalReason = if (selectedReason == "Outro motivo" && customDetails.isNotBlank()) {
                        "Outro motivo: ${customDetails.trim()}"
                    } else {
                        selectedReason
                    }
                    onConfirmReject(finalReason)
                },
                colors = ButtonDefaults.buttonColors(containerColor = RedDanger),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Confirmar Recusa", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Voltar", color = SlateTextSecondary)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverTripWhatsAppDialog(
    trip: Trip,
    driverName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var passengerPhone by remember(trip.passengerPhone) { mutableStateOf(trip.passengerPhone) }
    val tripWithUpdatedPhone = remember(trip, passengerPhone) {
        trip.copy(passengerPhone = passengerPhone)
    }
    val effectiveDriverName = if (driverName.isNotBlank()) driverName else trip.driverName.ifBlank { "Seu Motorista" }
    val formattedMessage = remember(tripWithUpdatedPhone, effectiveDriverName) {
        WhatsAppHelper.formatDriverTripNotification(tripWithUpdatedPhone, effectiveDriverName)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .widthIn(max = 540.dp)
                .shadow(16.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = SlateLight,
            border = BorderStroke(1.dp, SlateBorder)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Surface(
                    color = NavyPrimary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF25D366).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Chat,
                                    contentDescription = null,
                                    tint = Color(0xFF25D366),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Confirmar Atendimento",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Mensagem do motorista via WhatsApp",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }

                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
                        }
                    }
                }

                // Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Recipient card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        border = BorderStroke(1.dp, SlateBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "PASSAGEIRO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary
                            )
                            Text(
                                text = trip.passengerName.ifBlank { "Passageiro" },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary
                            )
                            if (trip.additionalPassengers.isNotEmpty()) {
                                Text(
                                    text = "Passageiros Adicionais: ${trip.additionalPassengers.joinToString(", ")}",
                                    fontSize = 12.sp,
                                    color = BlueInfo,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            OutlinedTextField(
                                value = passengerPhone,
                                onValueChange = { passengerPhone = it },
                                label = { Text("Telefone / WhatsApp") },
                                placeholder = { Text("(XX) 9XXXX-XXXX") },
                                leadingIcon = {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF25D366))
                                },
                                colors = executiveTextFieldColors(),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Message preview
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "MENSAGEM AO PASSAGEIRO",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NavySecondary
                                )
                                TextButton(
                                    onClick = { WhatsAppHelper.copyToClipboard(context, formattedMessage) },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = NavyPrimary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copiar", fontSize = 11.sp, color = NavyPrimary, fontWeight = FontWeight.Bold)
                                }
                            }

                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = formattedMessage,
                                    fontSize = 12.sp,
                                    color = Color(0xFF1E293B),
                                    lineHeight = 17.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }

                // Footer
                Surface(
                    color = Color.White,
                    border = BorderStroke(1.dp, SlateBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Fechar", color = SlateTextSecondary, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                WhatsAppHelper.openWhatsApp(context, passengerPhone, formattedMessage)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Enviar no WhatsApp", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SEÇÃO: MEU FINANCEIRO (MOTORISTA)
// ==========================================

data class DriverCommissionItem(
    val trip: Trip,
    val isPaid: Boolean,
    val isOverdue: Boolean,
    val isOnSchedule: Boolean,
    val effectiveDueDate: Long,
    val daysRemaining: Int,
    val daysOverdue: Int,
    val paidDate: Long?
)

fun calculateDriverCommissionItem(trip: Trip, now: Long = System.currentTimeMillis()): DriverCommissionItem {
    val isPaid = trip.paymentStatus == PaymentStatus.PAID
    val termDays = PaymentTerms.getDays(trip.paymentTermSnapshot)
    val baseTime = trip.completedAt ?: trip.scheduledTime
    val calculatedDue = trip.dueDate ?: trip.commissionAvailableAt ?: (baseTime + termDays * 86_400_000L)

    if (isPaid) {
        val paidDate = trip.paidAt ?: trip.completedAt ?: trip.scheduledTime
        return DriverCommissionItem(
            trip = trip,
            isPaid = true,
            isOverdue = false,
            isOnSchedule = false,
            effectiveDueDate = calculatedDue,
            daysRemaining = 0,
            daysOverdue = 0,
            paidDate = paidDate
        )
    }

    val isOverdue = calculatedDue < now
    if (isOverdue) {
        val diffMs = now - calculatedDue
        val days = (diffMs / 86_400_000L).toInt().coerceAtLeast(1)
        return DriverCommissionItem(
            trip = trip,
            isPaid = false,
            isOverdue = true,
            isOnSchedule = false,
            effectiveDueDate = calculatedDue,
            daysRemaining = 0,
            daysOverdue = days,
            paidDate = null
        )
    } else {
        val diffMs = calculatedDue - now
        val days = (diffMs / 86_400_000L).toInt()
        return DriverCommissionItem(
            trip = trip,
            isPaid = false,
            isOverdue = false,
            isOnSchedule = true,
            effectiveDueDate = calculatedDue,
            daysRemaining = days,
            daysOverdue = 0,
            paidDate = null
        )
    }
}

@Composable
fun DriverFinancialView(
    trips: List<Trip>,
    currentDriverId: String?,
    viewModel: MainViewModel
) {
    val now = System.currentTimeMillis()
    var selectedPeriod by remember { mutableStateOf(PeriodType.ESTE_MES) }
    var customStartDateMs by remember { mutableStateOf<Long?>(null) }
    var customEndDateMs by remember { mutableStateOf<Long?>(null) }
    var showCustomDateDialog by remember { mutableStateOf(false) }

    var selectedStatusFilter by remember { mutableStateOf("TODAS") } // TODAS, A_RECEBER, VENCIDAS, PAGAS
    var selectedCompanyId by remember { mutableStateOf<String?>(null) }
    var companyDropdownOpen by remember { mutableStateOf(false) }

    val distinctCompanies = remember(trips) {
        trips.filter { it.companyId.isNotBlank() && it.companyName.isNotBlank() }
            .distinctBy { it.companyId }
            .map { it.companyId to it.companyName }
    }

    // Filtro de período alinhado ao critério do financeiro admin
    val currentFinancialFilter = remember(selectedPeriod, customStartDateMs, customEndDateMs) {
        FinancialFilter(
            type = selectedPeriod,
            customStartMs = customStartDateMs,
            customEndMs = customEndDateMs
        )
    }

    // Filtrar somente viagens concluídas do motorista logado no período apurado e empresa selecionada
    val completedTripsInPeriod = remember(trips, currentFinancialFilter, currentDriverId, selectedCompanyId) {
        trips.filter { trip ->
            trip.status == TripStatus.COMPLETED &&
            (currentDriverId.isNullOrBlank() || trip.driverId == currentDriverId) &&
            (selectedCompanyId == null || trip.companyId == selectedCompanyId) &&
            viewModel.isTripInFinancialPeriod(trip, currentFinancialFilter)
        }.sortedByDescending { it.completedAt ?: it.scheduledTime }
    }

    // Mapeamento com cálculo de prazos e atrasos
    val commissionItems = remember(completedTripsInPeriod, now) {
        completedTripsInPeriod.map { calculateDriverCommissionItem(it, now) }
    }

    val onScheduleItems = remember(commissionItems) { commissionItems.filter { it.isOnSchedule } }
    val overdueItems = remember(commissionItems) { commissionItems.filter { it.isOverdue } }
    val paidItems = remember(commissionItems) { commissionItems.filter { it.isPaid } }

    val onScheduleCommissions = remember(onScheduleItems) { onScheduleItems.sumOf { it.trip.driverCommission } }
    val overdueCommissions = remember(overdueItems) { overdueItems.sumOf { it.trip.driverCommission } }
    val paidCommissions = remember(paidItems) { paidItems.sumOf { it.trip.driverCommission } }
    val totalGenerated = remember(commissionItems) { commissionItems.sumOf { it.trip.driverCommission } }

    // Lista filtrada por status selecionado
    val displayList = remember(commissionItems, selectedStatusFilter) {
        when (selectedStatusFilter) {
            "A_RECEBER" -> commissionItems.filter { it.isOnSchedule }
            "VENCIDAS" -> commissionItems.filter { it.isOverdue }
            "PAGAS" -> commissionItems.filter { it.isPaid }
            else -> commissionItems
        }
    }

    // Rótulo legível do período selecionado
    val periodLabel = remember(selectedPeriod, customStartDateMs, customEndDateMs) {
        when (selectedPeriod) {
            PeriodType.HOJE -> "Hoje"
            PeriodType.ESTA_SEMANA -> "Esta semana"
            PeriodType.ESTE_MES -> "Este mês"
            PeriodType.MES_ANTERIOR -> "Mês anterior"
            PeriodType.ESTE_ANO -> "Este ano"
            PeriodType.PERSONALIZADO -> {
                val s = customStartDateMs?.let { formatDate(it) } ?: "Início"
                val e = customEndDateMs?.let { formatDate(it) } ?: "Hoje"
                "$s até $e"
            }
            PeriodType.TODOS -> "Todo o histórico"
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Barra de Seleção de Período
        item {
            Surface(
                color = NavyDark,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = EmeraldLight,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Período de Apuração",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        if (selectedPeriod == PeriodType.PERSONALIZADO && (customStartDateMs != null || customEndDateMs != null)) {
                            TextButton(
                                onClick = { showCustomDateDialog = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = EmeraldLight,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ajustar", color = EmeraldLight, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DriverPeriodChip("Hoje", selectedPeriod == PeriodType.HOJE) {
                            selectedPeriod = PeriodType.HOJE
                        }
                        DriverPeriodChip("Semanal", selectedPeriod == PeriodType.ESTA_SEMANA) {
                            selectedPeriod = PeriodType.ESTA_SEMANA
                        }
                        DriverPeriodChip("Mensal", selectedPeriod == PeriodType.ESTE_MES) {
                            selectedPeriod = PeriodType.ESTE_MES
                        }
                        DriverPeriodChip("Mês anterior", selectedPeriod == PeriodType.MES_ANTERIOR) {
                            selectedPeriod = PeriodType.MES_ANTERIOR
                        }
                        DriverPeriodChip("Este ano", selectedPeriod == PeriodType.ESTE_ANO) {
                            selectedPeriod = PeriodType.ESTE_ANO
                        }
                        DriverPeriodChip(
                            label = if (selectedPeriod == PeriodType.PERSONALIZADO && customStartDateMs != null) {
                                val s = formatDate(customStartDateMs)
                                val e = customEndDateMs?.let { formatDate(it) } ?: "Hoje"
                                "$s a $e"
                            } else "Personalizado",
                            isSelected = selectedPeriod == PeriodType.PERSONALIZADO
                        ) {
                            selectedPeriod = PeriodType.PERSONALIZADO
                            showCustomDateDialog = true
                        }
                        DriverPeriodChip("Todas", selectedPeriod == PeriodType.TODOS) {
                            selectedPeriod = PeriodType.TODOS
                        }
                    }

                    if (distinctCompanies.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val currentCompanyName = distinctCompanies.find { it.first == selectedCompanyId }?.second
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                onClick = { companyDropdownOpen = true },
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White.copy(alpha = 0.12f),
                                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (currentCompanyName != null) "Empresa: $currentCompanyName" else "Todas as Empresas",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (currentCompanyName != null) EmeraldLight else Color.White
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }

                            DropdownMenu(
                                expanded = companyDropdownOpen,
                                onDismissRequest = { companyDropdownOpen = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Todas as Empresas", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
                                    onClick = {
                                        selectedCompanyId = null
                                        companyDropdownOpen = false
                                    }
                                )
                                distinctCompanies.forEach { (cId, cName) ->
                                    DropdownMenuItem(
                                        text = { Text(cName, color = Color(0xFF0F172A)) },
                                        onClick = {
                                            selectedCompanyId = cId
                                            companyDropdownOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Cards Resumidos (Métricas do Motorista: A Receber, Vencidas, Pagas e Total Gerado)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Linha 1: A Receber no Prazo e Vencidas em Atraso
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DriverFinancialMetricCard(
                        title = "A Receber (No Prazo)",
                        value = "R$ %.2f".format(onScheduleCommissions),
                        subtitle = if (onScheduleItems.isEmpty()) "Nenhum repasse no prazo" else "${onScheduleItems.size} corrida(s) no prazo",
                        icon = Icons.Default.HourglassTop,
                        accentColor = Color(0xFF0284C7),
                        modifier = Modifier.weight(1f)
                    )
                    DriverFinancialMetricCard(
                        title = "Vencidas (Em Atraso)",
                        value = "R$ %.2f".format(overdueCommissions),
                        subtitle = if (overdueItems.isEmpty()) "Nenhum repasse vencido" else "${overdueItems.size} corrida(s) vencida(s)",
                        icon = Icons.Default.WarningAmber,
                        accentColor = if (overdueItems.isNotEmpty()) RedDanger else SlateTextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Linha 2: Pagas / Quitadas e Total Gerado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DriverFinancialMetricCard(
                        title = "Corridas Pagas",
                        value = "R$ %.2f".format(paidCommissions),
                        subtitle = if (paidItems.isEmpty()) "Nenhum repasse efetuado" else "${paidItems.size} repasse(s) quitado(s)",
                        icon = Icons.Default.CheckCircle,
                        accentColor = EmeraldDark,
                        modifier = Modifier.weight(1f)
                    )
                    DriverFinancialMetricCard(
                        title = "Total Gerado",
                        value = "R$ %.2f".format(totalGenerated),
                        subtitle = "${commissionItems.size} corrida(s) concluída(s)",
                        icon = Icons.Default.Payments,
                        accentColor = NavyPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Alerta de Repasses Vencidos (se houver)
        if (overdueItems.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, RedDanger.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(RedDanger.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = RedDanger,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Atenção: ${overdueItems.size} repasse(s) vencido(s)!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = RedDanger
                            )
                            val maxDays = overdueItems.maxOfOrNull { it.daysOverdue } ?: 1
                            Text(
                                text = "Total vencido: R$ %.2f • O repasse mais antigo venceu há $maxDays dia(s).".format(overdueCommissions),
                                fontSize = 11.sp,
                                color = Color(0xFF991B1B)
                            )
                        }
                    }
                }
            }
        }

        // 3. Filtros do Relatório: Todas, A Receber, Vencidas e Pagas
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DriverFilterPill(
                    label = "Todas (${commissionItems.size})",
                    isSelected = selectedStatusFilter == "TODAS"
                ) {
                    selectedStatusFilter = "TODAS"
                }

                DriverFilterPill(
                    label = "A Receber (${onScheduleItems.size})",
                    isSelected = selectedStatusFilter == "A_RECEBER",
                    dotColor = Color(0xFF0284C7)
                ) {
                    selectedStatusFilter = "A_RECEBER"
                }

                DriverFilterPill(
                    label = "Vencidas (${overdueItems.size})",
                    isSelected = selectedStatusFilter == "VENCIDAS",
                    dotColor = if (overdueItems.isNotEmpty()) RedDanger else null,
                    highlightAlert = overdueItems.isNotEmpty()
                ) {
                    selectedStatusFilter = "VENCIDAS"
                }

                DriverFilterPill(
                    label = "Pagas (${paidItems.size})",
                    isSelected = selectedStatusFilter == "PAGAS",
                    dotColor = EmeraldDark
                ) {
                    selectedStatusFilter = "PAGAS"
                }
            }
        }

        // 4. Lista de Corridas do Relatório
        if (displayList.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateCard)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = SlateTextSecondary,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Nenhuma comissão encontrada para este filtro",
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Filtro selecionado no período: $periodLabel",
                            color = SlateTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            items(displayList, key = { it.trip.id }) { item ->
                val trip = item.trip
                val stripeColor = when {
                    item.isPaid -> EmeraldAccent
                    item.isOverdue -> RedDanger
                    else -> Color(0xFF0284C7)
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        1.dp,
                        when {
                            item.isPaid -> EmeraldLight.copy(alpha = 0.35f)
                            item.isOverdue -> RedDanger.copy(alpha = 0.5f)
                            else -> Color(0xFF93C5FD).copy(alpha = 0.5f)
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(14.dp))
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        // Tarja lateral de status
                        Box(
                            modifier = Modifier
                                .width(5.dp)
                                .fillMaxHeight()
                                .background(stripeColor)
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(14.dp)
                        ) {
                            // Linha Superior: Data da Conclusão + Badge de Status
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Event,
                                        contentDescription = null,
                                        tint = SlateTextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = formatDateTime(trip.completedAt ?: trip.scheduledTime),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = NavyPrimary
                                    )
                                }

                                // Badge de Status
                                Surface(
                                    color = when {
                                        item.isPaid -> EmeraldContainer
                                        item.isOverdue -> Color(0xFFFEE2E2)
                                        else -> Color(0xFFDBEAFE)
                                    },
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        val (badgeText, badgeColor, badgeIcon) = when {
                                            item.isPaid -> Triple(
                                                "PAGA em ${formatDate(item.paidDate)}",
                                                EmeraldDark,
                                                Icons.Default.CheckCircle
                                            )
                                            item.isOverdue -> Triple(
                                                if (item.daysOverdue == 1) "VENCIDA HÁ 1 DIA" else "VENCIDA HÁ ${item.daysOverdue} DIAS",
                                                RedDanger,
                                                Icons.Default.WarningAmber
                                            )
                                            else -> Triple(
                                                when (item.daysRemaining) {
                                                    0 -> "VENCE HOJE"
                                                    1 -> "VENCE AMANHÃ"
                                                    else -> "A RECEBER (${item.daysRemaining} DIAS)"
                                                },
                                                Color(0xFF1D4ED8),
                                                Icons.Default.Schedule
                                            )
                                        }

                                        Icon(
                                            imageVector = badgeIcon,
                                            contentDescription = null,
                                            tint = badgeColor,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = badgeText,
                                            color = badgeColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.5.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Trajeto: Origem -> Destino
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    tint = stripeColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${trip.origin} ➔ ${trip.destination}",
                                    fontSize = 12.sp,
                                    color = SlateTextPrimary,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Passageiro e Empresa
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Business,
                                    contentDescription = null,
                                    tint = SlateTextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${trip.companyName} • ${trip.passengerName.ifBlank { "Passageiro Executivo" }}",
                                    fontSize = 11.5.sp,
                                    color = SlateTextSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // ==========================================
                            // CAIXA ESPECÍFICA: DETALHAMENTO DO PRAZO / ATRASO
                            // ==========================================
                            Surface(
                                color = when {
                                    item.isPaid -> Color(0xFFF0FDF4)
                                    item.isOverdue -> Color(0xFFFEF2F2)
                                    else -> Color(0xFFF0F9FF)
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(
                                    1.dp,
                                    when {
                                        item.isPaid -> EmeraldLight.copy(alpha = 0.4f)
                                        item.isOverdue -> RedDanger.copy(alpha = 0.4f)
                                        else -> Color(0xFFBAE6FD)
                                    }
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when {
                                            item.isPaid -> Icons.Default.CheckCircle
                                            item.isOverdue -> Icons.Default.ErrorOutline
                                            else -> Icons.Default.HourglassTop
                                        },
                                        contentDescription = null,
                                        tint = when {
                                            item.isPaid -> EmeraldDark
                                            item.isOverdue -> RedDanger
                                            else -> Color(0xFF0369A1)
                                        },
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        when {
                                            item.isPaid -> {
                                                Text(
                                                    text = "Repasse Liquidado com Sucesso",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.5.sp,
                                                    color = EmeraldDark
                                                )
                                                Text(
                                                    text = "Pago em ${formatDate(item.paidDate)} via ${PaymentMeans.getLabel(trip.paymentMeansSnapshot)}.",
                                                    fontSize = 11.sp,
                                                    color = SlateTextSecondary
                                                )
                                            }
                                            item.isOverdue -> {
                                                Text(
                                                    text = "Repasse Vencido há ${item.daysOverdue} dia(s)!",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.5.sp,
                                                    color = RedDanger
                                                )
                                                Text(
                                                    text = "Data limite de repasse era ${formatDate(item.effectiveDueDate)} (${PaymentTerms.getLabel(trip.paymentTermSnapshot)}).",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF991B1B)
                                                )
                                            }
                                            else -> {
                                                val prazoTexto = when (item.daysRemaining) {
                                                    0 -> "Prazo de repasse vence hoje (${formatDate(item.effectiveDueDate)})!"
                                                    1 -> "Prazo de repasse vence amanhã (${formatDate(item.effectiveDueDate)})."
                                                    else -> "Prazo para recebimento: ${formatDate(item.effectiveDueDate)} (faltam ${item.daysRemaining} dias)."
                                                }
                                                Text(
                                                    text = prazoTexto,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.5.sp,
                                                    color = Color(0xFF0369A1)
                                                )
                                                Text(
                                                    text = "Condição contratual da corrida: ${PaymentTerms.getLabel(trip.paymentTermSnapshot)}.",
                                                    fontSize = 11.sp,
                                                    color = SlateTextSecondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.8.dp)
                            Spacer(modifier = Modifier.height(8.dp))

                            // Rodapé: Comissão do Motorista e Valor da Corrida
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Sua Comissão",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = SlateTextSecondary
                                    )
                                    Text(
                                        text = "R$ %.2f".format(trip.driverCommission),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = when {
                                            item.isPaid -> EmeraldDark
                                            item.isOverdue -> RedDanger
                                            else -> Color(0xFF0284C7)
                                        }
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Total da Corrida",
                                        fontSize = 10.sp,
                                        color = SlateTextSecondary
                                    )
                                    Text(
                                        text = "R$ %.2f".format(trip.price),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NavyPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo de Período Personalizado
    if (showCustomDateDialog) {
        DriverCustomPeriodDialog(
            initialStartMs = customStartDateMs,
            initialEndMs = customEndDateMs,
            onApply = { startMs, endMs ->
                customStartDateMs = startMs
                customEndDateMs = endMs
                selectedPeriod = PeriodType.PERSONALIZADO
                showCustomDateDialog = false
            },
            onDismiss = { showCustomDateDialog = false }
        )
    }
}

// ==========================================
// COMPONENTES AUXILIARES DO MEU FINANCEIRO
// ==========================================
@Composable
fun DriverFinancialMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.shadow(1.dp, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(11.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SlateTextSecondary
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 9.5.sp,
                color = SlateTextSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
fun DriverPeriodChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) EmeraldAccent else Color.White.copy(alpha = 0.15f),
        contentColor = Color.White
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun DriverCustomPeriodDialog(
    initialStartMs: Long?,
    initialEndMs: Long?,
    onApply: (startMs: Long, endMs: Long) -> Unit,
    onDismiss: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    var startDateText by remember {
        mutableStateOf(if (initialStartMs != null) sdf.format(Date(initialStartMs)) else "")
    }
    var endDateText by remember {
        mutableStateOf(if (initialEndMs != null) sdf.format(Date(initialEndMs)) else "")
    }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Período Personalizado", fontWeight = FontWeight.Bold, color = NavyPrimary, fontSize = 16.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Defina o intervalo de datas para calcular suas comissões:",
                    fontSize = 12.sp,
                    color = SlateTextSecondary
                )

                // Atalhos rápidos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance()
                            val end = sdf.format(cal.time)
                            cal.add(Calendar.DAY_OF_YEAR, -7)
                            val start = sdf.format(cal.time)
                            startDateText = start
                            endDateText = end
                            errorText = null
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NavyPrimary),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        Text("7 dias", fontSize = 11.sp, color = NavyPrimary)
                    }
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance()
                            val end = sdf.format(cal.time)
                            cal.add(Calendar.DAY_OF_YEAR, -15)
                            val start = sdf.format(cal.time)
                            startDateText = start
                            endDateText = end
                            errorText = null
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NavyPrimary),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        Text("15 dias", fontSize = 11.sp, color = NavyPrimary)
                    }
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance()
                            val end = sdf.format(cal.time)
                            cal.add(Calendar.DAY_OF_YEAR, -30)
                            val start = sdf.format(cal.time)
                            startDateText = start
                            endDateText = end
                            errorText = null
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NavyPrimary),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        Text("30 dias", fontSize = 11.sp, color = NavyPrimary)
                    }
                }

                OutlinedTextField(
                    value = startDateText,
                    onValueChange = {
                        startDateText = it
                        errorText = null
                    },
                    label = { Text("Data Inicial (dd/mm/aaaa)") },
                    placeholder = { Text("Ex: 01/09/2026") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = endDateText,
                    onValueChange = {
                        endDateText = it
                        errorText = null
                    },
                    label = { Text("Data Final (dd/mm/aaaa)") },
                    placeholder = { Text("Ex: 19/09/2026") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorText != null) {
                    Text(
                        text = errorText ?: "",
                        color = RedDanger,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sMs = parseDateStart(startDateText)
                    val eMs = parseDateEnd(endDateText)
                    if (sMs == null || eMs == null) {
                        errorText = "Informe datas válidas no formato dd/MM/yyyy"
                    } else if (sMs > eMs) {
                        errorText = "Data inicial não pode ser maior que a data final"
                    } else {
                        onApply(sMs, eMs)
                    }
                },
                colors = executiveButtonPrimaryColors()
            ) {
                Text("Aplicar Filtro", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = SlateTextSecondary)
            }
        }
    )
}

@Composable
fun DriverFilterPill(
    label: String,
    isSelected: Boolean,
    dotColor: Color? = null,
    highlightAlert: Boolean = false,
    onClick: () -> Unit
) {
    val containerColor = when {
        isSelected && highlightAlert -> RedDanger
        isSelected -> NavyPrimary
        highlightAlert -> Color(0xFFFEE2E2)
        else -> Color(0xFFE2E8F0)
    }

    val contentColor = when {
        isSelected -> Color.White
        highlightAlert -> RedDanger
        else -> NavyPrimary
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        contentColor = contentColor,
        border = if (!isSelected && highlightAlert) BorderStroke(1.dp, RedDanger.copy(alpha = 0.5f)) else null
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            if (dotColor != null && !isSelected) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Spacer(modifier = Modifier.width(5.dp))
            }
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

private fun parseDateStart(dateStr: String): Long? {
    return try {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply { isLenient = false }
        val date = sdf.parse(dateStr.trim()) ?: return null
        val cal = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        cal.timeInMillis
    } catch (e: Exception) {
        null
    }
}

private fun parseDateEnd(dateStr: String): Long? {
    return try {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply { isLenient = false }
        val date = sdf.parse(dateStr.trim()) ?: return null
        val cal = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        cal.timeInMillis
    } catch (e: Exception) {
        null
    }
}

private fun formatDate(millis: Long?): String {
    if (millis == null || millis <= 0) return "Imediato"
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return sdf.format(Date(millis))
}

private fun formatDateShort(millis: Long?): String {
    if (millis == null || millis <= 0) return "Imediato"
    val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())
    return sdf.format(Date(millis))
}

private fun formatDateTime(millis: Long?): String {
    if (millis == null || millis <= 0) return "--"
    val sdf = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.getDefault())
    return sdf.format(Date(millis))
}
