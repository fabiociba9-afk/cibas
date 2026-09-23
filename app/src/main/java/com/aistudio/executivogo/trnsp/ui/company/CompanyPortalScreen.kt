package com.aistudio.executivogo.trnsp.ui.company

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.executivogo.trnsp.data.*
import com.aistudio.executivogo.trnsp.ui.MainViewModel
import com.aistudio.executivogo.trnsp.ui.TripStatusBadge
import com.aistudio.executivogo.trnsp.ui.WhatsAppHelper
import com.aistudio.executivogo.trnsp.ui.components.ExecutivePullRefreshBox
import com.aistudio.executivogo.trnsp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyPortalScreen(
    viewModel: MainViewModel,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val companyTrips by viewModel.companyTrips.collectAsState()
    val companyPassengers by viewModel.companyPassengers.collectAsState()
    val routes by viewModel.routes.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val companies by viewModel.companies.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var mapInitialDriverId by remember { mutableStateOf<String?>(null) }
    val tabs = listOf(
        Triple("Início", Icons.Default.Dashboard, 0),
        Triple("Viagens", Icons.Default.DirectionsCar, 1),
        Triple("Solicitar", Icons.Default.AddCircle, 2),
        Triple("Passageiros", Icons.Default.People, 3),
        Triple("Financeiro", Icons.Default.AccountBalanceWallet, 4),
        Triple("Mapa", Icons.Default.Map, 5)
    )

    val currentCompany = remember(currentUser, companies) {
        val cid = currentUser?.companyId
        if (!cid.isNullOrBlank()) {
            companies.find { it.id == cid }
        } else null
    }

    LaunchedEffect(currentUser?.companyId) {
        currentUser?.companyId?.let { cid ->
            if (cid.isNotBlank()) {
                viewModel.loadCompanyData(cid)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentUser?.companyName?.ifBlank { "Portal Corporativo" } ?: "Portal Corporativo",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Acesso Corporativo • ${currentUser?.name ?: ""}",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshCompanyData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atualizar", tint = Color.White)
                    }
                    IconButton(onClick = { viewModel.logout(onLogout) }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sair", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyPrimary)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                tabs.forEach { (label, icon, index) ->
                    val isSelected = selectedTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                icon,
                                contentDescription = label,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Clip,
                                letterSpacing = (-0.3).sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NavyPrimary,
                            selectedTextColor = NavyPrimary,
                            indicatorColor = EmeraldAccent.copy(alpha = 0.18f),
                            unselectedIconColor = SlateTextSecondary,
                            unselectedTextColor = SlateTextSecondary
                        )
                    )
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        ExecutivePullRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshCompanyData() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(SlateLight)
        ) {
            when (selectedTab) {
                0 -> CompanyDashboardTab(
                    company = currentCompany,
                    userName = currentUser?.name ?: "",
                    companyName = currentUser?.companyName ?: "",
                    trips = companyTrips,
                    passengers = companyPassengers,
                    onRequestTripClick = { selectedTab = 2 },
                    onViewTripsClick = { selectedTab = 1 },
                    onViewPassengersClick = { selectedTab = 3 }
                )
                1 -> CompanyTripsTab(
                    trips = companyTrips,
                    onRequestNewTrip = { selectedTab = 2 },
                    onTrackTrip = { driverId ->
                        mapInitialDriverId = driverId
                        selectedTab = 5
                    }
                )
                2 -> CompanyRequestTripTab(
                    viewModel = viewModel,
                    passengers = companyPassengers,
                    routes = routes,
                    onTripRequested = {
                        selectedTab = 1
                    }
                )
                3 -> CompanyPassengersTab(
                    viewModel = viewModel,
                    companyId = currentUser?.companyId ?: "",
                    passengers = companyPassengers
                )
                4 -> CompanyFinanceTab(
                    company = currentCompany,
                    trips = companyTrips
                )
                5 -> CompanyMapTab(
                    viewModel = viewModel,
                    companyId = currentUser?.companyId ?: "",
                    initialDriverId = mapInitialDriverId
                )
            }
        }
    }
}

// =========================================================================
// TAB 0: DASHBOARD / HOME
// =========================================================================
@Composable
private fun CompanyDashboardTab(
    company: Company?,
    userName: String,
    companyName: String,
    trips: List<Trip>,
    passengers: List<Passenger>,
    onRequestTripClick: () -> Unit,
    onViewTripsClick: () -> Unit,
    onViewPassengersClick: () -> Unit
) {
    val pendingTrips = remember(trips) { trips.count { it.status == TripStatus.PENDING || it.status == TripStatus.ACCEPTED } }
    val inProgressTrips = remember(trips) { trips.count { it.status == TripStatus.IN_PROGRESS } }
    val completedTrips = remember(trips) { trips.count { it.status == TripStatus.COMPLETED } }
    val activePassengers = remember(passengers) { passengers.count { it.active } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Welcome Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NavyPrimary)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(EmeraldAccent.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Business, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "Bem-vindo(a), $userName", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(text = companyName.ifBlank { "Conta Corporativa" }, fontSize = 12.sp, color = Color(0xFFE2E8F0))
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Gerencie seus colaboradores, solicite novas viagens executivas e acompanhe itinerários em tempo real.",
                    fontSize = 12.sp,
                    color = Color(0xFFF8FAFC),
                    lineHeight = 16.sp
                )
            }
        }

        // Quick Action: Solicitar Viagem
        Button(
            onClick = onRequestTripClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent)
        ) {
            Icon(Icons.Default.AddLocationAlt, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Solicitar Nova Viagem Executiva", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
        }

        // Stats Grid
        Text("Resumo da Operação", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NavyPrimary)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DashboardMetricCard(
                title = "Agendadas",
                count = pendingTrips.toString(),
                subtitle = "Aguardando",
                icon = Icons.Default.Schedule,
                accentColor = GoldWarning,
                modifier = Modifier.weight(1f),
                onClick = onViewTripsClick
            )
            DashboardMetricCard(
                title = "Em Corrida",
                count = inProgressTrips.toString(),
                subtitle = "Em trânsito",
                icon = Icons.Default.DirectionsCar,
                accentColor = BlueInfo,
                modifier = Modifier.weight(1f),
                onClick = onViewTripsClick
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DashboardMetricCard(
                title = "Concluídas",
                count = completedTrips.toString(),
                subtitle = "Histórico",
                icon = Icons.Default.CheckCircle,
                accentColor = EmeraldDark,
                modifier = Modifier.weight(1f),
                onClick = onViewTripsClick
            )
            DashboardMetricCard(
                title = "Passageiros",
                count = activePassengers.toString(),
                subtitle = "Cadastrados",
                icon = Icons.Default.People,
                accentColor = NavySecondary,
                modifier = Modifier.weight(1f),
                onClick = onViewPassengersClick
            )
        }

        // Recent Trips Section
        if (trips.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Últimas Viagens", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NavyPrimary)
                TextButton(onClick = onViewTripsClick) {
                    Text("Ver todas", fontSize = 12.sp, color = EmeraldDark, fontWeight = FontWeight.SemiBold)
                }
            }

            trips.take(3).forEach { trip ->
                CompanyTripCard(trip = trip)
            }
        }
    }
}

@Composable
private fun DashboardMetricCard(
    title: String,
    count: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .shadow(2.dp, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 12.sp, color = SlateTextSecondary, fontWeight = FontWeight.SemiBold)
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = count, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
            Text(text = subtitle, fontSize = 11.sp, color = SlateTextSecondary)
        }
    }
}

// =========================================================================
// TAB 1: LISTA DE VIAGENS
// =========================================================================
@Composable
private fun CompanyTripsTab(
    trips: List<Trip>,
    onRequestNewTrip: () -> Unit,
    onTrackTrip: (String) -> Unit
) {
    var selectedFilter by remember { mutableStateOf("TODAS") }
    val filtered = remember(trips, selectedFilter) {
        when (selectedFilter) {
            "PENDING" -> trips.filter { it.status == TripStatus.PENDING || it.status == TripStatus.ACCEPTED }
            "IN_PROGRESS" -> trips.filter { it.status == TripStatus.IN_PROGRESS }
            "COMPLETED" -> trips.filter { it.status == TripStatus.COMPLETED }
            "CANCELLED" -> trips.filter { it.status == TripStatus.CANCELLED }
            else -> trips
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "TODAS" to "Todas (${trips.size})",
                "PENDING" to "Agendadas (${trips.count { it.status == TripStatus.PENDING || it.status == TripStatus.ACCEPTED }})",
                "IN_PROGRESS" to "Em Curso (${trips.count { it.status == TripStatus.IN_PROGRESS }})",
                "COMPLETED" to "Concluídas (${trips.count { it.status == TripStatus.COMPLETED }})"
            ).forEach { (key, label) ->
                FilterChip(
                    selected = selectedFilter == key,
                    onClick = { selectedFilter = key },
                    label = { Text(label, fontSize = 11.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FlightTakeoff, contentDescription = null, tint = SlateTextSecondary, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Nenhuma viagem encontrada nesta categoria.", color = SlateTextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(onClick = onRequestNewTrip) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Solicitar Viagem Agora")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) { trip ->
                    CompanyTripCard(
                        trip = trip,
                        onTrackTrip = if (!trip.driverId.isNullOrBlank()) { { onTrackTrip(trip.driverId!!) } } else null
                    )
                }
            }
        }
    }
}

@Composable
private fun CompanyTripCard(
    trip: Trip,
    onTrackTrip: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR")) }
    val formattedDate = remember(trip.scheduledTime, trip.createdAt) {
        val time = if (trip.scheduledTime > 0) trip.scheduledTime else trip.createdAt
        sdf.format(Date(time))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = trip.passengerName.ifBlank { "Passageiro" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = NavyPrimary
                )
                TripStatusBadge(trip.status)
            }

            Text(
                text = "Data: $formattedDate",
                fontSize = 11.sp,
                color = SlateTextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, SlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(EmeraldAccent))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("De: ${trip.origin}", fontSize = 12.sp, color = NavyPrimary)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(RedDanger))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Para: ${trip.destination}", fontSize = 12.sp, color = NavyPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    if (trip.driverName.isNotBlank()) {
                        Text(
                            text = "Motorista: ${trip.driverName}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EmeraldDark
                        )
                    } else {
                        Text(
                            text = "Motorista: Aguardando escala",
                            fontSize = 11.sp,
                            color = SlateTextSecondary
                        )
                    }
                    Text(
                        text = "Valor: R$ %.2f".format(trip.price),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                }

                if (trip.passengerPhone.isNotBlank()) {
                    IconButton(
                        onClick = {
                            val msg = "Olá, ${trip.passengerName}! Sua viagem de ${trip.origin} para ${trip.destination} está registrada."
                            WhatsAppHelper.openWhatsApp(context, trip.passengerPhone, msg)
                        }
                    ) {
                        Icon(
                            Icons.Default.Chat,
                            contentDescription = "WhatsApp",
                            tint = Color(0xFF25D366),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if ((trip.status.equals(TripStatus.ACCEPTED, ignoreCase = true) || trip.status.equals(TripStatus.IN_PROGRESS, ignoreCase = true)) && onTrackTrip != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onTrackTrip.invoke() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Acompanhar no Mapa", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

// =========================================================================
// TAB 2: SOLICITAR VIAGEM CORPORATIVA (PREÇO BLOQUEADO PARA EDIÇÃO)
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompanyRequestTripTab(
    viewModel: MainViewModel,
    passengers: List<Passenger>,
    routes: List<Route>,
    onTripRequested: () -> Unit
) {
    val context = LocalContext.current
    var selectedPassenger by remember { mutableStateOf<Passenger?>(null) }
    var passengerDropdownExpanded by remember { mutableStateOf(false) }

    var manualPassengerName by remember { mutableStateOf("") }
    var manualPassengerPhone by remember { mutableStateOf("") }

    var origin by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var distanceKmText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var scheduledCalendar by remember { mutableStateOf<Calendar?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    // Preço calculado pelo sistema (READ ONLY)
    val distanceKm = distanceKmText.replace(",", ".").toDoubleOrNull() ?: 0.0
    val calculatedPrice = remember(distanceKm) {
        if (distanceKm > 0.0) {
            viewModel.calculateRoutePricePreview(distanceKm)
        } else {
            0.0
        }
    }

    val legibleFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color(0xFF0F172A),
        unfocusedTextColor = Color(0xFF0F172A),
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        focusedBorderColor = EmeraldAccent,
        unfocusedBorderColor = Color(0xFF94A3B8),
        focusedLabelColor = EmeraldDark,
        unfocusedLabelColor = Color(0xFF334155),
        focusedPlaceholderColor = Color(0xFF64748B),
        unfocusedPlaceholderColor = Color(0xFF64748B),
        cursorColor = EmeraldAccent
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Solicitar Viagem Executiva", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = NavyPrimary)
        Text(
            text = "O agendamento será encaminhado diretamente à central de despacho. O valor é calculado automaticamente pelas faixas de tabela da empresa.",
            fontSize = 12.sp,
            color = SlateTextSecondary,
            lineHeight = 16.sp
        )

        if (validationError != null) {
            Surface(
                color = Color(0xFFFEE2E2),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = validationError!!,
                    color = RedDanger,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        // 1. Passageiro
        Text("1. Selecione o Passageiro", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = NavyPrimary)

        if (passengers.isNotEmpty()) {
            ExposedDropdownMenuBox(
                expanded = passengerDropdownExpanded,
                onExpandedChange = { passengerDropdownExpanded = !passengerDropdownExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedPassenger?.name ?: if (manualPassengerName.isNotBlank()) manualPassengerName else "Selecione um passageiro cadastrado...",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Passageiro Cadastrado", fontWeight = FontWeight.Medium) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = passengerDropdownExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = legibleFieldColors
                )
                ExposedDropdownMenu(
                    expanded = passengerDropdownExpanded,
                    onDismissRequest = { passengerDropdownExpanded = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    passengers.forEach { p ->
                        val isSelected = selectedPassenger?.id == p.id
                        DropdownMenuItem(
                            text = { Text("${p.name} (${p.phone})", color = NavyPrimary, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            onClick = {
                                selectedPassenger = p
                                manualPassengerName = p.name
                                manualPassengerPhone = p.phone
                                passengerDropdownExpanded = false
                            },
                            modifier = if (isSelected) Modifier.background(EmeraldAccent.copy(alpha = 0.12f)) else Modifier
                        )
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = manualPassengerName,
                onValueChange = {
                    manualPassengerName = it
                    selectedPassenger = null
                },
                label = { Text("Nome do Passageiro *", fontWeight = FontWeight.Medium) },
                modifier = Modifier.weight(1.2f),
                shape = RoundedCornerShape(8.dp),
                colors = legibleFieldColors
            )
            OutlinedTextField(
                value = manualPassengerPhone,
                onValueChange = { manualPassengerPhone = it },
                label = { Text("Telefone / WhatsApp", fontWeight = FontWeight.Medium) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = legibleFieldColors
            )
        }

        HorizontalDivider(color = SlateBorder)

        // 2. Trajeto (Origem e Destino)
        Text("2. Origem e Destino", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = NavyPrimary)

        OutlinedTextField(
            value = origin,
            onValueChange = { origin = it },
            label = { Text("Endereço de Embarque (Origem) *", fontWeight = FontWeight.Medium) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = legibleFieldColors
        )

        OutlinedTextField(
            value = destination,
            onValueChange = { destination = it },
            label = { Text("Endereço de Desembarque (Destino) *", fontWeight = FontWeight.Medium) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = legibleFieldColors
        )

        OutlinedTextField(
            value = distanceKmText,
            onValueChange = { distanceKmText = it },
            label = { Text("Distância estimada em Km (para cálculo)", fontWeight = FontWeight.Medium) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = legibleFieldColors
        )

        // 3. Data e Horário
        Text("3. Agendamento", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = NavyPrimary)

        OutlinedButton(
            onClick = {
                val now = Calendar.getInstance()
                DatePickerDialog(
                    context,
                    { _, y, m, d ->
                        TimePickerDialog(
                            context,
                            { _, h, min ->
                                val cal = Calendar.getInstance()
                                cal.set(y, m, d, h, min, 0)
                                scheduledCalendar = cal
                            },
                            now.get(Calendar.HOUR_OF_DAY),
                            now.get(Calendar.MINUTE),
                            true
                        ).show()
                    },
                    now.get(Calendar.YEAR),
                    now.get(Calendar.MONTH),
                    now.get(Calendar.DAY_OF_MONTH)
                ).show()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White,
                contentColor = NavyPrimary
            ),
            border = BorderStroke(1.dp, Color(0xFF94A3B8))
        ) {
            Icon(Icons.Default.Event, contentDescription = null, tint = NavyPrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = scheduledCalendar?.let {
                    SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR")).format(it.time)
                } ?: "Selecionar Data e Horário (ou Imediato)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = NavyPrimary
            )
        }

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Observações (Centro de custo, vôo, etc.)", fontWeight = FontWeight.Medium) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = legibleFieldColors
        )

        // READ-ONLY Price Display Card (PROIBIDO EDITAR VALOR)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
            border = BorderStroke(1.dp, EmeraldLight.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Valor Previsto (Tabela Corporativa):",
                        fontSize = 12.sp,
                        color = EmeraldDark,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (distanceKm > 0) "Tarifa por km: $distanceKm km"
                               else "A ser confirmado pela central",
                        fontSize = 11.sp,
                        color = SlateTextSecondary
                    )
                }
                Text(
                    text = "R$ %.2f".format(calculatedPrice),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldDark
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Button(
            onClick = {
                if (manualPassengerName.isBlank()) {
                    validationError = "Informe o nome do passageiro."
                    return@Button
                }
                if (origin.isBlank() || destination.isBlank()) {
                    validationError = "Informe a origem e o destino da viagem."
                    return@Button
                }
                isSubmitting = true
                validationError = null

                val schedTime = scheduledCalendar?.timeInMillis ?: 0L

                val resolvedPassenger = selectedPassenger ?: Passenger(
                    name = manualPassengerName.trim(),
                    phone = manualPassengerPhone.trim()
                )

                val resolvedRoute = Route(
                    origin = origin.trim(),
                    destination = destination.trim(),
                    distanceKm = distanceKm,
                    price = if (calculatedPrice > 0.0) calculatedPrice else 50.0,
                    pricingMode = if (distanceKm > 0) PricingMode.PER_KM else PricingMode.FIXED
                )

                viewModel.requestTripByCompany(
                    passenger = resolvedPassenger,
                    route = resolvedRoute,
                    scheduledTime = schedTime,
                    notes = notes.trim(),
                    onSuccess = {
                        isSubmitting = false
                        onTripRequested()
                    },
                    onError = { err ->
                        isSubmitting = false
                        validationError = err
                    }
                )
            },
            enabled = !isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Enviando solicitação...", color = Color.White)
            } else {
                Text("Confirmar e Solicitar Viagem", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
            }
        }
    }
}

// =========================================================================
// TAB 3: PASSAGEIROS CORPORATIVOS
// =========================================================================
@Composable
private fun CompanyPassengersTab(
    viewModel: MainViewModel,
    companyId: String,
    passengers: List<Passenger>
) {
    var showDialog by remember { mutableStateOf(false) }
    var selectedPassengerToEdit by remember { mutableStateOf<Passenger?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(passengers, searchQuery) {
        if (searchQuery.isBlank()) passengers
        else passengers.filter { it.name.contains(searchQuery, ignoreCase = true) || it.email.contains(searchQuery, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Passageiros Cadastrados", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = NavyPrimary)
            Button(
                onClick = {
                    selectedPassengerToEdit = null
                    showDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Novo Passageiro", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Buscar colaborador...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("Nenhum passageiro encontrado.", color = SlateTextSecondary, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.id }) { p ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(p.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NavyPrimary)
                                if (p.phone.isNotBlank()) {
                                    Text("Tel: ${p.phone}", fontSize = 12.sp, color = SlateTextSecondary)
                                }
                                if (p.email.isNotBlank()) {
                                    Text("E-mail: ${p.email}", fontSize = 11.sp, color = SlateTextSecondary)
                                }
                            }
                            IconButton(
                                onClick = {
                                    selectedPassengerToEdit = p
                                    showDialog = true
                                }
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = NavySecondary)
                            }
                        }
                    }
                }
            }
        }

        if (showDialog) {
            CompanyPassengerDialog(
                passenger = selectedPassengerToEdit,
                onDismiss = { showDialog = false },
                onConfirm = { name, phone, email ->
                    viewModel.saveCompanyPassenger(
                        Passenger(
                            id = selectedPassengerToEdit?.id ?: "",
                            name = name,
                            phone = phone,
                            email = email,
                            companyId = companyId
                        ),
                        onSuccess = { showDialog = false }
                    )
                }
            )
        }
    }
}

@Composable
private fun CompanyPassengerDialog(
    passenger: Passenger?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, email: String) -> Unit
) {
    var name by remember { mutableStateOf(passenger?.name ?: "") }
    var phone by remember { mutableStateOf(passenger?.phone ?: "") }
    var email by remember { mutableStateOf(passenger?.email ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (passenger == null) "Novo Passageiro" else "Editar Passageiro", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (error != null) {
                    Text(error!!, color = RedDanger, fontSize = 12.sp)
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome Completo *") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Telefone / WhatsApp") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("E-mail do Colaborador") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        error = "O nome é obrigatório."
                        return@Button
                    }
                    onConfirm(name.trim(), phone.trim(), email.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// =========================================================================
// TAB 4: FINANCEIRO DA EMPRESA (STRICTLY READ-ONLY)
// =========================================================================
enum class CompanyFinancePeriod(val label: String) {
    ALL("Todas"),
    TODAY("Hoje"),
    MONTH("Mês"),
    YEAR("Ano"),
    CUSTOM("Período")
}

@Composable
private fun CompanyFinanceTab(
    company: Company?,
    trips: List<Trip>
) {
    val context = LocalContext.current
    var selectedPeriod by remember { mutableStateOf(CompanyFinancePeriod.ALL) }
    var customStartDateMs by remember { mutableStateOf<Long?>(null) }
    var customEndDateMs by remember { mutableStateOf<Long?>(null) }
    var selectedTripId by remember { mutableStateOf<String?>(null) }
    var tripDropdownOpen by remember { mutableStateOf(false) }

    val tripOrder = remember(trips) { trips.sortedBy { it.scheduledTime } }
    val dateTimeFormat = remember { SimpleDateFormat("dd/MM/yyyy • HH:mm", Locale("pt", "BR")) }

    fun getTripLabel(trip: Trip): String {
        val idx = tripOrder.indexOfFirst { it.id == trip.id }
        val num = if (idx >= 0) String.format("%03d", idx + 1) else trip.id.takeLast(4).uppercase()
        return "Viagem #$num — ${trip.passengerName.ifBlank { "Passageiro" }}"
    }

    val filteredTrips = remember(trips, selectedPeriod, customStartDateMs, customEndDateMs, selectedTripId) {
        trips.filter { trip ->
            val matchTrip = selectedTripId == null || trip.id == selectedTripId
            val tripTime = trip.completedAt ?: trip.scheduledTime

            val matchPeriod = if (selectedTripId != null) {
                true
            } else when (selectedPeriod) {
                CompanyFinancePeriod.ALL -> true
                CompanyFinancePeriod.TODAY -> {
                    val start = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    val end = start + 86400000L - 1
                    tripTime in start..end
                }
                CompanyFinancePeriod.MONTH -> {
                    val start = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    tripTime >= start
                }
                CompanyFinancePeriod.YEAR -> {
                    val start = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_YEAR, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    tripTime >= start
                }
                CompanyFinancePeriod.CUSTOM -> {
                    val s = customStartDateMs ?: 0L
                    val e = customEndDateMs ?: Long.MAX_VALUE
                    tripTime in s..e
                }
            }
            matchTrip && matchPeriod
        }
    }

    val completedTrips = remember(filteredTrips) { filteredTrips.filter { it.status == TripStatus.COMPLETED } }
    val totalBilled = remember(completedTrips) { completedTrips.sumOf { it.price } }
    val paidTrips = remember(filteredTrips) { filteredTrips.filter { it.companyPaymentStatus == PaymentStatus.PAID } }
    val totalPaid = remember(paidTrips) { paidTrips.sumOf { it.price } }
    val pendingTrips = remember(filteredTrips) {
        filteredTrips.filter { it.companyPaymentStatus != PaymentStatus.PAID }
    }
    val pendingTripsValue = remember(pendingTrips) { pendingTrips.sumOf { it.price } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Extrato Financeiro Corporativo", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = NavyPrimary)
        Text(
            text = "Visão transparente dos valores faturados, condições comerciais e conciliação por período e motorista.",
            fontSize = 12.sp,
            color = SlateTextSecondary,
            lineHeight = 16.sp
        )

        // Payment Terms Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Condições Comerciais Vigentes", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NavyPrimary)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Prazo de Pagamento:", fontSize = 12.sp, color = SlateTextSecondary, fontWeight = FontWeight.Medium)
                    Text(
                        PaymentTerms.getLabel(company?.paymentTerm ?: PaymentTerms.A_VISTA),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = NavyPrimary
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Meio de Cobrança:", fontSize = 12.sp, color = SlateTextSecondary, fontWeight = FontWeight.Medium)
                    Text(
                        PaymentMeans.getLabel(company?.paymentMeans ?: PaymentMeans.PIX),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = EmeraldDark
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("CNPJ Faturamento:", fontSize = 12.sp, color = SlateTextSecondary, fontWeight = FontWeight.Medium)
                    Text(
                        company?.cnpj?.ifBlank { "Não informado" } ?: "Não informado",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NavyPrimary
                    )
                }
            }
        }

        // Filtro de Período e Motorista Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Filtros do Relatório", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NavyPrimary)

                // Chips de período
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CompanyFinancePeriod.values().forEach { period ->
                        val isSelected = selectedPeriod == period
                        Surface(
                            onClick = {
                                if (period == CompanyFinancePeriod.CUSTOM) {
                                    val nowCal = Calendar.getInstance()
                                    DatePickerDialog(
                                        context,
                                        { _, startYear, startMonth, startDay ->
                                            val startCal = Calendar.getInstance().apply {
                                                set(startYear, startMonth, startDay, 0, 0, 0)
                                                set(Calendar.MILLISECOND, 0)
                                            }
                                            DatePickerDialog(
                                                context,
                                                { _, endYear, endMonth, endDay ->
                                                    val endCal = Calendar.getInstance().apply {
                                                        set(endYear, endMonth, endDay, 23, 59, 59)
                                                        set(Calendar.MILLISECOND, 999)
                                                    }
                                                    customStartDateMs = startCal.timeInMillis
                                                    customEndDateMs = endCal.timeInMillis
                                                    selectedPeriod = CompanyFinancePeriod.CUSTOM
                                                },
                                                startYear,
                                                startMonth,
                                                startDay
                                            ).apply {
                                                setTitle("Data Final")
                                                show()
                                            }
                                        },
                                        nowCal.get(Calendar.YEAR),
                                        nowCal.get(Calendar.MONTH),
                                        nowCal.get(Calendar.DAY_OF_MONTH)
                                    ).apply {
                                        setTitle("Data Inicial")
                                        show()
                                    }
                                } else {
                                    selectedPeriod = period
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) NavyPrimary else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (isSelected) NavyPrimary else Color(0xFFCBD5E1))
                        ) {
                            Text(
                                text = period.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFF1E293B),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
                            )
                        }
                    }
                }

                // Range indicator if CUSTOM
                if (selectedPeriod == CompanyFinancePeriod.CUSTOM && customStartDateMs != null && customEndDateMs != null) {
                    val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    Surface(
                        color = Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                    ) {
                        Text(
                            text = "Período apurado: ${df.format(Date(customStartDateMs!!))} até ${df.format(Date(customEndDateMs!!))}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D4ED8),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Filtro por Viagem Dropdown
                val currentTrip = trips.find { it.id == selectedTripId }
                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        onClick = { tripDropdownOpen = true },
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (currentTrip != null) getTripLabel(currentTrip) else "Todas as Viagens",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyPrimary
                                )
                                if (currentTrip != null) {
                                    Text(
                                        text = "${currentTrip.origin} → ${currentTrip.destination}",
                                        fontSize = 11.sp,
                                        color = SlateTextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val driverInfo = if (!currentTrip.driverName.isNullOrBlank()) " • Motorista: ${currentTrip.driverName}" else ""
                                    Text(
                                        text = "${dateTimeFormat.format(Date(currentTrip.scheduledTime))}$driverInfo",
                                        fontSize = 10.sp,
                                        color = SlateTextSecondary
                                    )
                                } else {
                                    Text(
                                        text = "Faturamento consolidado de todas as viagens corporativas",
                                        fontSize = 11.sp,
                                        color = SlateTextSecondary
                                    )
                                }
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Selecionar Viagem", tint = NavyPrimary)
                        }
                    }

                    DropdownMenu(
                        expanded = tripDropdownOpen,
                        onDismissRequest = { tripDropdownOpen = false },
                        modifier = Modifier
                            .background(Color.White)
                            .fillMaxWidth(0.92f)
                    ) {
                        val isAllSelected = selectedTripId == null
                        DropdownMenuItem(
                            text = {
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Text(
                                        text = "Todas as Viagens",
                                        fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = if (isAllSelected) EmeraldDark else NavyPrimary
                                    )
                                    Text(
                                        text = "Consolidado geral da empresa no período",
                                        fontSize = 11.sp,
                                        color = SlateTextSecondary
                                    )
                                }
                            },
                            onClick = {
                                selectedTripId = null
                                tripDropdownOpen = false
                            },
                            modifier = if (isAllSelected) Modifier.background(EmeraldAccent.copy(alpha = 0.12f)) else Modifier
                        )
                        HorizontalDivider(color = Color(0xFFE2E8F0))

                        trips.forEach { trip ->
                            val isSelected = selectedTripId == trip.id
                            val dateStr = dateTimeFormat.format(Date(trip.scheduledTime))
                            val driverInfo = if (!trip.driverName.isNullOrBlank()) " • Motorista: ${trip.driverName}" else ""
                            DropdownMenuItem(
                                text = {
                                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                        Text(
                                            text = getTripLabel(trip),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = if (isSelected) EmeraldDark else NavyPrimary
                                        )
                                        Text(
                                            text = "${trip.origin} → ${trip.destination}",
                                            fontSize = 11.sp,
                                            color = SlateTextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "$dateStr$driverInfo",
                                            fontSize = 10.sp,
                                            color = SlateTextSecondary
                                        )
                                    }
                                },
                                onClick = {
                                    selectedTripId = trip.id
                                    tripDropdownOpen = false
                                },
                                modifier = if (isSelected) Modifier.background(EmeraldAccent.copy(alpha = 0.12f)) else Modifier
                            )
                        }
                    }
                }
            }
        }

        // Totals Grid
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                border = BorderStroke(1.dp, EmeraldLight.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Total Faturado", fontSize = 11.sp, color = EmeraldDark, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("R$ %.2f".format(totalBilled), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = EmeraldDark)
                    Text("${completedTrips.size} viagens concluídas", fontSize = 10.sp, color = SlateTextSecondary)
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                border = BorderStroke(1.dp, Color(0xFFFDE68A))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("A Faturar / Pendente", fontSize = 11.sp, color = Color(0xFF92400E), fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("R$ %.2f".format(pendingTripsValue), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                    Text("${pendingTrips.size} lançamentos", fontSize = 10.sp, color = SlateTextSecondary)
                }
            }
        }

        // Detailed List of Trips for reconciliation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Histórico no Período", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NavyPrimary)
            Text("${filteredTrips.size} viagens", fontSize = 12.sp, color = SlateTextSecondary)
        }

        if (filteredTrips.isEmpty()) {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, SlateBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Nenhuma viagem encontrada no período selecionado (${selectedPeriod.label}).",
                    fontSize = 12.sp,
                    color = SlateTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            filteredTrips.forEach { trip ->
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, SlateBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(getTripLabel(trip), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NavyPrimary)
                            Text("${trip.origin} → ${trip.destination}", fontSize = 11.sp, color = SlateTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            val driverInfo = if (!trip.driverName.isNullOrBlank()) " • Motorista: ${trip.driverName}" else ""
                            Text(
                                "Status: ${trip.status} • Pagto: ${trip.companyPaymentStatus}$driverInfo",
                                fontSize = 10.sp,
                                color = if (trip.companyPaymentStatus == PaymentStatus.PAID) EmeraldDark else GoldWarning,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            "R$ %.2f".format(trip.price),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = NavyPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompanyMapTab(
    viewModel: MainViewModel,
    companyId: String,
    initialDriverId: String? = null
) {
    val drivers by viewModel.drivers.collectAsState()
    val driverLocations by viewModel.driverLocations.collectAsState()
    val trips by viewModel.trips.collectAsState()

    val activeTrips = remember(trips, companyId) {
        trips.filter { 
            (companyId.isBlank() || it.companyId == companyId) &&
            (it.status.equals(TripStatus.ACCEPTED, ignoreCase = true) || it.status.equals(TripStatus.IN_PROGRESS, ignoreCase = true))
        }
    }

    var selectedDriverId by remember(initialDriverId) { mutableStateOf(initialDriverId) }

    val activeDriverIds = remember(activeTrips) { activeTrips.mapNotNull { it.driverId }.toSet() }

    val markerList = remember(drivers, driverLocations, activeTrips, companyId) {
        val locMap = driverLocations.associateBy { it.driverId }
        val driverMap = drivers.associateBy { it.id }

        activeDriverIds.mapNotNull { driverId ->
            if (driverId.isBlank()) return@mapNotNull null
            val driver = driverMap[driverId]
            val loc = locMap[driverId]
            val trip = activeTrips.find { it.driverId == driverId }
            if (trip == null) return@mapNotNull null

            val lat = when {
                loc != null && loc.latitude != 0.0 -> loc.latitude
                driver != null && driver.latitude != 0.0 -> driver.latitude
                else -> -26.9000 // Fallback region coordinate so marker is visible on map
            }
            val lng = when {
                loc != null && loc.longitude != 0.0 -> loc.longitude
                driver != null && driver.longitude != 0.0 -> driver.longitude
                else -> -48.6500 // Fallback region coordinate so marker is visible on map
            }

            val name = driver?.name?.takeIf { it.isNotBlank() } ?: trip.driverName.takeIf { it.isNotBlank() } ?: "Motorista Executivo"
            val phone = driver?.phone?.takeIf { it.isNotBlank() } ?: trip.driverPhone
            val vehicleModel = driver?.vehicleModel?.takeIf { it.isNotBlank() } ?: trip.vehicleModel
            val vehicleColor = driver?.vehicleColor?.takeIf { it.isNotBlank() } ?: trip.vehicleColor
            val vehiclePlate = driver?.vehiclePlate?.takeIf { it.isNotBlank() } ?: trip.vehiclePlate

            com.aistudio.executivogo.trnsp.ui.maps.DriverMarkerData(
                id = driverId,
                name = name,
                phone = phone,
                vehicle = "$vehicleModel $vehicleColor".trim(),
                plate = vehiclePlate,
                lat = lat,
                lng = lng,
                speed = loc?.speed ?: driver?.speed ?: 0.0,
                bearing = loc?.bearing ?: driver?.bearing ?: 0.0,
                isOnline = loc?.isOnline ?: driver?.isOnline ?: true,
                inTrip = true,
                currentPassenger = trip.passengerName,
                destination = trip.destination,
                lastUpdate = loc?.updatedAt ?: driver?.lastLocationUpdate ?: System.currentTimeMillis()
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (markerList.isEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    Icons.Default.Map,
                    contentDescription = null,
                    tint = SlateTextSecondary,
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Nenhuma viagem em andamento",
                    color = SlateTextSecondary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            com.aistudio.executivogo.trnsp.ui.maps.GoogleMapLiveView(
                drivers = markerList,
                selectedDriverId = selectedDriverId,
                onDriverSelected = { id -> selectedDriverId = id },
                modifier = Modifier.fillMaxSize(),
                showDriverSelectorBar = true
            )
        }
    }
}
