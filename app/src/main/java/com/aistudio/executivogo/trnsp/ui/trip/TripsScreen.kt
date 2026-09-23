package com.aistudio.executivogo.trnsp.ui.trip

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aistudio.executivogo.trnsp.data.*
import com.aistudio.executivogo.trnsp.navigation.Screen
import com.aistudio.executivogo.trnsp.ui.MainViewModel
import com.aistudio.executivogo.trnsp.ui.ReceiptDialog
import com.aistudio.executivogo.trnsp.ui.TripStatusBadge
import com.aistudio.executivogo.trnsp.ui.WhatsAppHelper
import com.aistudio.executivogo.trnsp.ui.components.AdminTopBar
import com.aistudio.executivogo.trnsp.ui.components.ExecutivePullRefreshBox
import com.aistudio.executivogo.trnsp.ui.executiveTextFieldColors
import com.aistudio.executivogo.trnsp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripsScreen(
    viewModel: MainViewModel,
    onNavigate: ((String) -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val trips by viewModel.trips.collectAsState()
    val companies by viewModel.companies.collectAsState()
    val passengers by viewModel.passengers.collectAsState()
    val drivers by viewModel.drivers.collectAsState()
    val routes by viewModel.routes.collectAsState()
    val fareBands by viewModel.fareBands.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }

    val pendingCount = remember(trips) { trips.count { it.status.equals(TripStatus.PENDING, ignoreCase = true) } }
    val inProgressCount = remember(trips) { trips.count { it.status.equals(TripStatus.IN_PROGRESS, ignoreCase = true) } }
    val completedCount = remember(trips) { trips.count { it.status.equals(TripStatus.COMPLETED, ignoreCase = true) } }
    val cancelledCount = remember(trips) { trips.count { it.status.equals(TripStatus.CANCELLED, ignoreCase = true) } }

    var showAddDialog by remember { mutableStateOf(false) }
    var tripToEdit by remember { mutableStateOf<Trip?>(null) }
    var selectedTripForReceipt by remember { mutableStateOf<Trip?>(null) }
    var selectedTripForWhatsApp by remember { mutableStateOf<Trip?>(null) }
    var newlyCreatedTripForWhatsApp by remember { mutableStateOf<Trip?>(null) }
    var tripToReassignDriver by remember { mutableStateOf<Trip?>(null) }
    var tripToCancel by remember { mutableStateOf<Trip?>(null) }
    var tripToDelete by remember { mutableStateOf<Trip?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredTrips = remember(trips, selectedTab, searchQuery) {
        trips.filter { trip ->
            val matchesTab = when (selectedTab) {
                1 -> trip.status.equals(TripStatus.PENDING, ignoreCase = true)
                2 -> trip.status.equals(TripStatus.IN_PROGRESS, ignoreCase = true)
                3 -> trip.status.equals(TripStatus.COMPLETED, ignoreCase = true)
                4 -> trip.status.equals(TripStatus.CANCELLED, ignoreCase = true)
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                trip.passengerName.contains(searchQuery, ignoreCase = true) ||
                trip.companyName.contains(searchQuery, ignoreCase = true) ||
                trip.driverName.contains(searchQuery, ignoreCase = true) ||
                trip.origin.contains(searchQuery, ignoreCase = true) ||
                trip.destination.contains(searchQuery, ignoreCase = true)
            }
            matchesTab && matchesSearch
        }
    }

    val totalFilteredValue = remember(filteredTrips) {
        filteredTrips.sumOf { it.price }
    }

    val unassignedCount = remember(trips) {
        trips.count { it.driverName.isBlank() && it.status != TripStatus.CANCELLED }
    }

    Scaffold(
        topBar = {
            AdminTopBar(
                title = "Gestão de Viagens",
                subtitle = "Agendamento, Itinerário e Despacho",
                currentScreen = Screen.Trips.route,
                onNavigate = onNavigate,
                onLogout = onLogout,
                onRefresh = { viewModel.refreshTrips() }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = EmeraldAccent,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(6.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = null, tint = Color.White) },
                text = { Text("Agendar Viagem", fontWeight = FontWeight.Bold) }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        ExecutivePullRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshTrips() },
            modifier = Modifier
                .fillMaxSize()
                .background(SlateLight)
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Enhanced Status Tab Row with Counter Badges
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = NavyPrimary,
                    contentColor = Color.White,
                    edgePadding = 12.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = EmeraldLight,
                            height = 3.dp
                        )
                    }
                ) {
                    val tabs = listOf(
                        Triple("Todas", trips.size, Color.White),
                        Triple("Pendentes", pendingCount, GoldWarning),
                        Triple("Em Andamento", inProgressCount, Color(0xFF60A5FA)),
                        Triple("Concluídas", completedCount, EmeraldLight),
                        Triple("Canceladas", cancelledCount, RedDanger)
                    )

                    tabs.forEachIndexed { index, (title, count, dotColor) ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                ) {
                                    if (index > 0) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(dotColor)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = "$title ($count)",
                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedTab == index) Color.White else Color.White.copy(alpha = 0.75f),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    // Search Bar with High Contrast & Clear Button
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Buscar por passageiro, empresa, motorista, endereço...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = NavySecondary)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Limpar busca", tint = SlateTextSecondary)
                                }
                            }
                        },
                        colors = executiveTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Operational Metrics Summary Strip
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, SlateBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    tint = NavySecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${filteredTrips.size} viagens listadas",
                                    fontSize = 12.sp,
                                    color = SlateTextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Total: ",
                                    fontSize = 12.sp,
                                    color = SlateTextSecondary
                                )
                                Text(
                                    text = "R$ %.2f".format(totalFilteredValue),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldDark
                                )
                            }
                        }
                    }

                    // Alert if there are unassigned pending trips
                    if (unassignedCount > 0 && selectedTab in listOf(0, 1)) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = Color(0xFFFFFBEB),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = GoldWarning,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$unassignedCount viagem(ns) aguardando atribuição de motorista.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF92400E),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (filteredTrips.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(20.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE2E8F0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.FlightTakeoff,
                                        contentDescription = null,
                                        tint = NavySecondary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Nenhuma viagem encontrada.",
                                    fontWeight = FontWeight.Bold,
                                    color = NavyPrimary,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Toque em '+ Agendar Viagem' para criar um novo itinerário.",
                                    color = SlateTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(filteredTrips, key = { it.id }) { trip ->
                                TripCard(
                                    trip = trip,
                                    onOpenReceipt = { selectedTripForReceipt = trip },
                                    onOpenWhatsApp = { selectedTripForWhatsApp = trip },
                                    onEditTrip = { tripToEdit = trip },
                                    onUpdateStatus = { newStatus ->
                                        viewModel.updateTripStatus(trip.id, newStatus)
                                    },
                                    onReassignDriver = {
                                        tripToReassignDriver = trip
                                    },
                                    onCancel = { tripToCancel = trip },
                                    onDelete = { tripToDelete = trip },
                                    onTrackTrip = {
                                        onNavigate?.invoke(Screen.LiveMap.createRoute(trip.driverId))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (tripToEdit != null) {
        AddTripDialog(
            companies = companies,
            passengers = passengers,
            drivers = drivers,
            routes = routes,
            fareBands = fareBands,
            tripToEdit = tripToEdit,
            onDismiss = { tripToEdit = null },
            onSave = { updatedTrip ->
                viewModel.saveTrip(updatedTrip)
                tripToEdit = null
            }
        )
    }

    if (showAddDialog) {
        AddTripDialog(
            companies = companies,
            passengers = passengers,
            drivers = drivers,
            routes = routes,
            fareBands = fareBands,
            onDismiss = { showAddDialog = false },
            onSave = { newTrip ->
                viewModel.saveTrip(newTrip)
                showAddDialog = false
                newlyCreatedTripForWhatsApp = newTrip
            }
        )
    }

    if (newlyCreatedTripForWhatsApp != null) {
        AdminTripWhatsAppDialog(
            trip = newlyCreatedTripForWhatsApp!!,
            drivers = drivers,
            isNewlyCreated = true,
            onDismiss = { newlyCreatedTripForWhatsApp = null }
        )
    }

    if (selectedTripForWhatsApp != null) {
        AdminTripWhatsAppDialog(
            trip = selectedTripForWhatsApp!!,
            drivers = drivers,
            isNewlyCreated = false,
            onDismiss = { selectedTripForWhatsApp = null }
        )
    }

    if (tripToReassignDriver != null) {
        ReassignDriverDialog(
            trip = tripToReassignDriver!!,
            drivers = drivers,
            onDismiss = { tripToReassignDriver = null },
            onConfirm = { newDriver ->
                viewModel.reassignDriver(tripToReassignDriver!!.id, newDriver)
                tripToReassignDriver = null
            }
        )
    }

    if (selectedTripForReceipt != null) {
        ReceiptDialog(
            trip = selectedTripForReceipt!!,
            onDismiss = { selectedTripForReceipt = null }
        )
    }

    if (tripToCancel != null) {
        AlertDialog(
            onDismissRequest = { tripToCancel = null },
            title = {
                Text("Confirmar Cancelamento", fontWeight = FontWeight.Bold, color = RedDanger)
            },
            text = {
                Text(
                    "Deseja realmente cancelar a viagem de ${tripToCancel!!.passengerName} (${tripToCancel!!.companyName})?\n\n" +
                    "Origem: ${tripToCancel!!.origin}\n" +
                    "Destino: ${tripToCancel!!.destination}\n\n" +
                    "Como Administrador, você pode cancelar qualquer viagem (inclusive corporativas criadas via web). Notificações serão enviadas à empresa e motorista.",
                    color = SlateTextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        tripToCancel?.let { viewModel.cancelTrip(it.id) }
                        tripToCancel = null
                    },
                    colors = executiveButtonDangerColors()
                ) {
                    Text("Sim, Cancelar Viagem", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { tripToCancel = null }) {
                    Text("Voltar", color = SlateTextSecondary)
                }
            }
        )
    }

    if (tripToDelete != null) {
        AlertDialog(
            onDismissRequest = { tripToDelete = null },
            title = {
                Text("Confirmar Exclusão", fontWeight = FontWeight.Bold, color = RedDanger)
            },
            text = {
                Text(
                    "Deseja realmente excluir a viagem de ${tripToDelete!!.passengerName} (${tripToDelete!!.companyName})? Esta ação não pode ser desfeita e removerá a viagem permanentemente do sistema.",
                    color = SlateTextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        tripToDelete?.let { viewModel.deleteTrip(it.id) }
                        tripToDelete = null
                    },
                    colors = executiveButtonDangerColors()
                ) {
                    Text("Excluir", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { tripToDelete = null }) {
                    Text("Cancelar", color = SlateTextSecondary)
                }
            }
        )
    }
}

@Composable
fun TripCard(
    trip: Trip,
    onOpenReceipt: () -> Unit,
    onOpenWhatsApp: () -> Unit,
    onEditTrip: () -> Unit,
    onUpdateStatus: (String) -> Unit,
    onReassignDriver: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    onDelete: () -> Unit,
    onTrackTrip: (() -> Unit)? = null
) {
    val statusColor = when (trip.status.uppercase()) {
        TripStatus.PENDING -> GoldWarning
        TripStatus.IN_PROGRESS -> Color(0xFF2563EB)
        TripStatus.COMPLETED -> EmeraldAccent
        TripStatus.CANCELLED -> RedDanger
        else -> SlateTextSecondary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        border = BorderStroke(1.dp, SlateBorder)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Left Status Stripe
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(statusColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header: Company + Scheduled Time + Status Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Business,
                            contentDescription = null,
                            tint = NavySecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = trip.companyName.ifBlank { "Empresa Avulsa" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = NavyPrimary,
                            maxLines = 1
                        )
                    }

                    TripStatusBadge(trip.status)
                }

                // Scheduled Date & Time Pill (High Contrast Emerald)
                if (trip.scheduledTime > 0) {
                    val formattedSchedule = remember(trip.scheduledTime) {
                        val sdf = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR"))
                        sdf.format(Date(trip.scheduledTime))
                    }
                    Surface(
                        color = Color(0xFFECFDF5),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, EmeraldLight.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Event,
                                contentDescription = null,
                                tint = EmeraldDark,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Agendamento: $formattedSchedule",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldDark
                            )
                        }
                    }
                }

                // Passenger Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = NavyPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = trip.passengerName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NavyPrimary
                    )
                    if (trip.passengerPhone.isNotBlank()) {
                        Text(
                            text = " • ${trip.passengerPhone}",
                            fontSize = 12.sp,
                            color = SlateTextSecondary
                        )
                    }
                    if (trip.additionalPassengers.isNotEmpty()) {
                        Surface(
                            color = Color(0xFFEFF6FF),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                        ) {
                            Text(
                                text = "+${trip.additionalPassengers.size} pass.",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = BlueInfo,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Route Visualization: Origem -> Paradas -> Destino (Complete text, high contrast)
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, SlateBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Origem (Embarque)
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                Icons.Default.TripOrigin,
                                contentDescription = null,
                                tint = EmeraldAccent,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Origem (Embarque)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldDark
                                )
                                Text(
                                    text = trip.origin,
                                    fontSize = 12.sp,
                                    color = SlateTextPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Intermediate Stops
                        if (trip.stops.isNotEmpty()) {
                            trip.stops.forEachIndexed { i, stop ->
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        Icons.Default.Flag,
                                        contentDescription = null,
                                        tint = GoldWarning,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "Parada ${i + 1}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFB45309)
                                        )
                                        Text(
                                            text = stop,
                                            fontSize = 12.sp,
                                            color = Color(0xFF78350F),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        // Destino Final (Desembarque)
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = RedDanger,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Destino Final",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RedDanger
                                )
                                Text(
                                    text = trip.destination,
                                    fontSize = 12.sp,
                                    color = SlateTextPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Notes if any
                        if (trip.notes.isNotBlank() && !trip.notes.contains("[RECUSA")) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    Icons.Default.Notes,
                                    contentDescription = null,
                                    tint = SlateTextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Obs: ${trip.notes}",
                                    fontSize = 11.sp,
                                    color = SlateTextSecondary
                                )
                            }
                        }
                    }
                }

                // Driver Refusal Alert if applicable
                if (trip.notes.contains("[RECUSA")) {
                    Surface(
                        color = Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, RedDanger.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = RedDanger,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = trip.notes,
                                fontSize = 11.sp,
                                color = RedDanger,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Driver & Vehicle Card
                Surface(
                    color = if (trip.driverName.isNotBlank()) Color(0xFFF1F5F9) else Color(0xFFFFFBEB),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(
                        1.dp,
                        if (trip.driverName.isNotBlank()) SlateBorder else Color(0xFFFDE68A)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = if (trip.driverName.isNotBlank()) NavyPrimary else GoldWarning,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = if (trip.driverName.isNotBlank()) "Motorista: ${trip.driverName}" else "Motorista: Não atribuído",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (trip.driverName.isNotBlank()) NavyPrimary else Color(0xFFB45309)
                                )
                                Text(
                                    text = "Repasse Motorista: R$ %.2f".format(trip.driverCommission),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = EmeraldDark
                                )
                            }
                        }

                        if (trip.driverName.isBlank() && (trip.status == TripStatus.PENDING || trip.status == TripStatus.ACCEPTED)) {
                            FilledTonalButton(
                                onClick = { onReassignDriver?.invoke() },
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = GoldWarning,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Atribuir Motorista", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Payment Snapshots & Values Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Prazo Badge
                        Surface(
                            color = Color(0xFFEDE9FE),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = PaymentTerms.getLabel(trip.paymentTermSnapshot),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6D28D9),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                        // Meio Badge
                        Surface(
                            color = Color(0xFFE0F2FE),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = PaymentMeans.getLabel(trip.paymentMeansSnapshot),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0369A1),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Price
                    Text(
                        text = "R$ %.2f".format(trip.price),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldAccent
                    )
                }

                HorizontalDivider(color = SlateBorder)

                // Actions Row (Clean, spacious, high contrast)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left actions: Edit, Receipt & WhatsApp
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        OutlinedButton(
                            onClick = onEditTrip,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            border = BorderStroke(1.dp, NavyPrimary.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NavyPrimary),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = NavyPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Editar",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary
                            )
                        }

                        IconButton(
                            onClick = onOpenReceipt,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.ReceiptLong,
                                contentDescription = "Ver Recibo",
                                tint = EmeraldAccent
                            )
                        }

                        IconButton(
                            onClick = onOpenWhatsApp,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Chat,
                                contentDescription = "Enviar WhatsApp ao Passageiro",
                                tint = Color(0xFF25D366)
                            )
                        }
                    }

                    // Right actions: Swap Driver, Status Change, Delete
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (trip.status.equals(TripStatus.ACCEPTED, ignoreCase = true) || trip.status.equals(TripStatus.IN_PROGRESS, ignoreCase = true)) {
                            OutlinedButton(
                                onClick = { onTrackTrip?.invoke() },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                border = BorderStroke(1.dp, Color(0xFF0284C7))
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = Color(0xFF0284C7)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Acompanhar", fontSize = 11.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.Bold)
                            }
                        }

                        if (trip.status == TripStatus.PENDING || trip.status == TripStatus.ACCEPTED) {
                            OutlinedButton(
                                onClick = { onReassignDriver?.invoke() },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                border = BorderStroke(1.dp, NavySecondary.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    Icons.Default.SwapHoriz,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = NavyPrimary
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Trocar", fontSize = 11.sp, color = NavyPrimary, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (trip.status == TripStatus.PENDING) {
                            Button(
                                onClick = { onUpdateStatus(TripStatus.IN_PROGRESS) },
                                colors = executiveButtonPrimaryColors(),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("Iniciar", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        } else if (trip.status == TripStatus.IN_PROGRESS) {
                            Button(
                                onClick = { onUpdateStatus(TripStatus.COMPLETED) },
                                colors = executiveButtonDarkColors(),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("Concluir", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (trip.status != TripStatus.CANCELLED) {
                            OutlinedButton(
                                onClick = { onCancel?.invoke() ?: onUpdateStatus(TripStatus.CANCELLED) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                border = BorderStroke(1.dp, RedDanger.copy(alpha = 0.5f))
                            ) {
                                Text("Cancelar", fontSize = 11.sp, color = RedDanger, fontWeight = FontWeight.Bold)
                            }
                        }

                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Excluir", tint = SlateTextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReassignDriverDialog(
    trip: Trip,
    drivers: List<AppUser>,
    onDismiss: () -> Unit,
    onConfirm: (AppUser) -> Unit
) {
    var selectedDriver by remember {
        mutableStateOf(drivers.firstOrNull { it.id != trip.driverId } ?: drivers.firstOrNull())
    }
    var dropdownExpanded by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 500.dp)
                .shadow(16.dp, RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            color = SlateLight,
            border = BorderStroke(1.dp, SlateBorder)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Surface(
                    color = NavyPrimary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = EmeraldAccent,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            if (trip.driverName.isBlank()) "Atribuir Motorista" else "Reatribuir Motorista",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 17.sp
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Current Trip Info Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        border = BorderStroke(1.dp, SlateBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Passageiro: ${trip.passengerName}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NavyPrimary)
                            Text("Origem: ${trip.origin}", fontSize = 12.sp, color = SlateTextSecondary)
                            Text("Destino: ${trip.destination}", fontSize = 12.sp, color = SlateTextSecondary)
                            Text("Valor Total: R$ %.2f".format(trip.price), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = EmeraldDark)
                            Text(
                                "Motorista Atual: ${trip.driverName.ifBlank { "Nenhum atribuído" }}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = NavySecondary
                            )
                        }
                    }

                    Text(
                        "Selecione o Novo Motorista:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = NavyPrimary
                    )

                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedDriver?.let { "${it.name} (${it.vehiclePlate})" } ?: "Selecione o motorista",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Novo Motorista Designado") },
                            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = NavySecondary) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                            colors = executiveTextFieldColors(),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            drivers.forEach { driver ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text("${driver.name} • ${driver.vehicleModel} (${driver.vehiclePlate})", fontWeight = FontWeight.SemiBold)
                                            Text("Comissão: ${driver.commissionPercentage.toInt()}% • Online 24h", fontSize = 11.sp, color = EmeraldDark)
                                        }
                                    },
                                    onClick = {
                                        selectedDriver = driver
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (selectedDriver != null) {
                        val newCommission = trip.price * (selectedDriver!!.commissionPercentage / 100.0)
                        Surface(
                            color = Color(0xFFF0FDF4),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, EmeraldLight.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "Nova Comissão Repasse (${selectedDriver!!.commissionPercentage.toInt()}%):",
                                        fontSize = 12.sp,
                                        color = EmeraldDark,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "Veículo: ${selectedDriver!!.vehicleModel} (${selectedDriver!!.vehiclePlate})",
                                        fontSize = 11.sp,
                                        color = SlateTextSecondary
                                    )
                                }
                                Text(
                                    "R$ %.2f".format(newCommission),
                                    fontSize = 15.sp,
                                    color = EmeraldDark,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Buttons
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
                            Text("Cancelar", color = SlateTextSecondary, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                selectedDriver?.let { onConfirm(it) }
                            },
                            enabled = selectedDriver != null,
                            colors = executiveButtonPrimaryColors(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                if (trip.driverName.isBlank()) "Atribuir Motorista" else "Confirmar Troca",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTripWhatsAppDialog(
    trip: Trip,
    drivers: List<AppUser>,
    isNewlyCreated: Boolean = false,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var passengerPhone by remember(trip.passengerPhone) { mutableStateOf(trip.passengerPhone) }
    val assignedDriver = remember(trip.driverId, drivers) {
        drivers.find { it.id == trip.driverId }
    }

    val tripWithUpdatedPhone = remember(trip, passengerPhone) {
        trip.copy(passengerPhone = passengerPhone)
    }

    val formattedMessage = remember(tripWithUpdatedPhone, assignedDriver) {
        WhatsAppHelper.formatAdminTripNotification(tripWithUpdatedPhone, assignedDriver)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .widthIn(max = 560.dp)
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
                                    text = if (isNewlyCreated) "Viagem Criada com Sucesso!" else "Notificar Passageiro (WhatsApp)",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Envio de confirmação de agendamento",
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

                // Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (isNewlyCreated) {
                        Surface(
                            color = Color(0xFFDCFCE7),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF15803D),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "A viagem foi agendada com sucesso! Envie agora a confirmação ao passageiro pelo WhatsApp:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF166534)
                                )
                            }
                        }
                    }

                    // Destinatário
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
                                text = "DESTINATÁRIO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Passageiro: ${trip.passengerName.ifBlank { "Passageiro Executivo" }}",
                                fontSize = 14.sp,
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
                                label = { Text("Telefone / WhatsApp do Passageiro") },
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

                    // Prévia da Mensagem
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
                                    text = "PRÉ-VISUALIZAÇÃO DA MENSAGEM PADRÃO",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NavySecondary,
                                    letterSpacing = 0.5.sp
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

                // Footer Buttons
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
                            Text(if (isNewlyCreated) "Concluir" else "Fechar", color = SlateTextSecondary, fontWeight = FontWeight.SemiBold)
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
