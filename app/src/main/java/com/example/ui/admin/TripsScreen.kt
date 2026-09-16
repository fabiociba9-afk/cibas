package com.example.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.*
import com.example.ui.MainViewModel
import com.example.ui.components.ReceiptDialog
import com.example.ui.components.TripStatusBadge
import com.example.ui.theme.*
import com.example.util.Formatters
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val trips by viewModel.trips.collectAsState()
    val companies by viewModel.companies.collectAsState()
    val drivers by viewModel.drivers.collectAsState()
    val paymentMethods by viewModel.paymentMethods.collectAsState()
    val users by viewModel.users.collectAsState()

    val statusFilter by viewModel.tripStatusFilter.collectAsState()
    val companyFilter by viewModel.tripCompanyFilter.collectAsState()
    val driverFilter by viewModel.tripDriverFilter.collectAsState()
    val searchQuery by viewModel.tripSearchQuery.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var tripToEdit by remember { mutableStateOf<TripEntity?>(null) }
    var tripForReceipt by remember { mutableStateOf<TripEntity?>(null) }

    val filteredTrips = trips.filter { trip ->
        val matchesStatus = statusFilter == null || statusFilter == "TODOS" || trip.status == statusFilter
        val matchesCompany = companyFilter == null || trip.companyId == companyFilter
        val matchesDriver = driverFilter == null || trip.driverId == driverFilter
        val matchesSearch = searchQuery.isBlank() ||
                trip.companyName.contains(searchQuery, ignoreCase = true) ||
                trip.driverName.contains(searchQuery, ignoreCase = true) ||
                trip.requesterName.contains(searchQuery, ignoreCase = true) ||
                trip.origin.contains(searchQuery, ignoreCase = true) ||
                trip.destination.contains(searchQuery, ignoreCase = true)

        matchesStatus && matchesCompany && matchesDriver && matchesSearch
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    tripToEdit = null
                    showCreateDialog = true
                },
                containerColor = EmeraldAccent,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.AddRoad, contentDescription = "Nova Viagem")
            }
        },
        containerColor = SlateLight
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setTripSearchQuery(it) },
                label = { Text("Buscar viagens por trajeto, passageiro, motorista...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SlateCard,
                    unfocusedContainerColor = SlateCard
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Status Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("TODOS", TripStatus.AGENDADA.name, TripStatus.EM_ANDAMENTO.name, TripStatus.CONCLUIDA.name).forEach { st ->
                    val isSelected = (statusFilter == st) || (statusFilter == null && st == "TODOS")
                    val label = when (st) {
                        "TODOS" -> "Todas"
                        TripStatus.AGENDADA.name -> "Agendadas"
                        TripStatus.EM_ANDAMENTO.name -> "Em Andamento"
                        TripStatus.CONCLUIDA.name -> "Concluídas"
                        else -> st
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setTripStatusFilter(st) },
                        label = { Text(label, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${filteredTrips.size} Viagens Encontradas",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = SlateTextSecondary
            )

            Spacer(modifier = Modifier.height(6.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (filteredTrips.isEmpty()) {
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
                                Text("Nenhuma viagem corresponde aos filtros aplicados.", color = SlateTextSecondary, fontSize = 13.sp)
                            }
                        }
                    }
                }

                items(filteredTrips) { trip ->
                    Card(
                        modifier = Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateCard)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Header: Company + Status
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = trip.companyName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = NavyPrimary
                                    )
                                    Text(
                                        text = "Solicitante: ${trip.requesterName} • ${Formatters.formatDateTime(trip.dateTimeMillis)}",
                                        fontSize = 11.sp,
                                        color = SlateTextSecondary
                                    )
                                }
                                TripStatusBadge(trip.status)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Route Section
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.TripOrigin, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Origem: ${trip.origin}", fontSize = 12.sp, color = SlateTextPrimary)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Destino: ${trip.destination}", fontSize = 12.sp, color = SlateTextPrimary)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Driver & Financial Info
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Motorista Responsável:", fontSize = 11.sp, color = SlateTextSecondary)
                                    Text(trip.driverName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NavyPrimary)
                                    Text(
                                        "Comissão: ${Formatters.formatCurrency(trip.driverCommissionAmount)}",
                                        fontSize = 11.sp,
                                        color = if (trip.isDriverPaid) EmeraldDark else GoldWarning
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Valor da Viagem:", fontSize = 11.sp, color = SlateTextSecondary)
                                    Text(
                                        text = Formatters.formatCurrency(trip.price),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldAccent
                                    )
                                    Text(
                                        "${trip.paymentMethodName}",
                                        fontSize = 11.sp,
                                        color = SlateTextSecondary
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

                            // Status Actions / Receipt / Edit
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Status Transitions
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (trip.status == TripStatus.AGENDADA.name) {
                                        Button(
                                            onClick = { viewModel.updateTripStatus(trip.id, TripStatus.EM_ANDAMENTO) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("Iniciar", fontSize = 11.sp)
                                        }
                                    }
                                    if (trip.status == TripStatus.EM_ANDAMENTO.name) {
                                        Button(
                                            onClick = { viewModel.updateTripStatus(trip.id, TripStatus.CONCLUIDA) },
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("Concluir", fontSize = 11.sp)
                                        }
                                    }
                                    if (trip.status == TripStatus.CONCLUIDA.name) {
                                        Button(
                                            onClick = { tripForReceipt = trip },
                                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Recibo", fontSize = 11.sp)
                                        }
                                    }
                                }

                                Row {
                                    IconButton(
                                        onClick = {
                                            tripToEdit = trip
                                            showCreateDialog = true
                                        }
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = NavySecondary, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteTrip(trip) }
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Excluir", tint = RedDanger, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        TripEditDialog(
            trip = tripToEdit,
            companies = companies,
            drivers = drivers,
            paymentMethods = paymentMethods,
            users = users,
            onDismiss = { showCreateDialog = false },
            onSave = { comp, reqName, reqId, origin, dest, price, pm, driver, notes ->
                val now = System.currentTimeMillis()
                val cal = Calendar.getInstance()
                cal.timeInMillis = now
                cal.add(Calendar.DAY_OF_MONTH, 15)
                val clientDue = cal.timeInMillis

                cal.timeInMillis = now
                cal.add(Calendar.DAY_OF_MONTH, 5)
                val driverDue = cal.timeInMillis

                if (tripToEdit == null) {
                    viewModel.createTrip(
                        company = comp,
                        requesterName = reqName,
                        requesterUserId = reqId,
                        dateTimeMillis = now,
                        origin = origin,
                        destination = dest,
                        price = price,
                        paymentMethod = pm,
                        clientPaymentDueDateMillis = clientDue,
                        driver = driver,
                        driverCommissionDueDateMillis = driverDue,
                        notes = notes
                    )
                } else {
                    val commAmount = price * (driver.commissionPercentage / 100.0)
                    viewModel.updateTrip(
                        tripToEdit!!.copy(
                            companyId = comp.id,
                            companyName = comp.name,
                            requesterName = reqName,
                            requesterUserId = reqId,
                            origin = origin,
                            destination = dest,
                            price = price,
                            paymentMethodId = pm.id,
                            paymentMethodName = pm.name,
                            driverId = driver.id,
                            driverName = driver.fullName,
                            driverCommissionAmount = commAmount,
                            notes = notes
                        )
                    )
                }
                showCreateDialog = false
            }
        )
    }

    if (tripForReceipt != null) {
        ReceiptDialog(
            trip = tripForReceipt!!,
            onDismiss = { tripForReceipt = null }
        )
    }
}

@Composable
fun TripEditDialog(
    trip: TripEntity?,
    companies: List<CompanyEntity>,
    drivers: List<DriverEntity>,
    paymentMethods: List<PaymentMethodEntity>,
    users: List<UserEntity>,
    onDismiss: () -> Unit,
    onSave: (
        company: CompanyEntity,
        requesterName: String,
        requesterUserId: Long,
        origin: String,
        destination: String,
        price: Double,
        paymentMethod: PaymentMethodEntity,
        driver: DriverEntity,
        notes: String
    ) -> Unit
) {
    var selectedCompany by remember {
        mutableStateOf(companies.firstOrNull { it.id == trip?.companyId } ?: companies.firstOrNull())
    }
    var requesterName by remember {
        mutableStateOf(trip?.requesterName ?: "")
    }
    var origin by remember { mutableStateOf(trip?.origin ?: "") }
    var destination by remember { mutableStateOf(trip?.destination ?: "") }
    var priceInput by remember { mutableStateOf(trip?.price?.toString() ?: "") }

    var selectedPaymentMethod by remember {
        mutableStateOf(paymentMethods.firstOrNull { it.id == trip?.paymentMethodId } ?: paymentMethods.firstOrNull())
    }
    var selectedDriver by remember {
        mutableStateOf(drivers.firstOrNull { it.id == trip?.driverId } ?: drivers.firstOrNull { it.isActive })
    }
    var notes by remember { mutableStateOf(trip?.notes ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (trip == null) "Agendar Nova Viagem" else "Editar Viagem", fontWeight = FontWeight.Bold)
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (companies.isEmpty() || drivers.isEmpty() || paymentMethods.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Pré-requisitos para Agendamento:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF92400E)
                                )
                                if (companies.isEmpty()) Text("• Cadastre pelo menos 1 Empresa", fontSize = 11.sp, color = Color(0xFF92400E))
                                if (drivers.isEmpty()) Text("• Cadastre pelo menos 1 Motorista", fontSize = 11.sp, color = Color(0xFF92400E))
                                if (paymentMethods.isEmpty()) Text("• Cadastre pelo menos 1 Forma de Pagamento", fontSize = 11.sp, color = Color(0xFF92400E))
                            }
                        }
                    }
                }

                item {
                    Text("Empresa Solicitante *:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    if (companies.isEmpty()) {
                        Text("Nenhuma empresa cadastrada no momento.", fontSize = 12.sp, color = RedDanger)
                    } else {
                        companies.forEach { c ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedCompany = c }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(selected = selectedCompany?.id == c.id, onClick = { selectedCompany = c })
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(c.name, fontSize = 13.sp)
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = requesterName,
                        onValueChange = { requesterName = it },
                        label = { Text("Nome do Passageiro / Solicitante *") },
                        placeholder = { Text("Ex: Marcelo Ramos") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = origin,
                        onValueChange = { origin = it },
                        label = { Text("Endereço de Origem *") },
                        placeholder = { Text("Ex: Aeroporto de Congonhas") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = destination,
                        onValueChange = { destination = it },
                        label = { Text("Endereço de Destino *") },
                        placeholder = { Text("Ex: Av. Paulista, 1000") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = priceInput,
                        onValueChange = { priceInput = it },
                        label = { Text("Valor da Viagem (R$) *") },
                        placeholder = { Text("Ex: 180.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text("Motorista Responsável *:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    val activeDrivers = drivers.filter { it.isActive }
                    if (activeDrivers.isEmpty()) {
                        Text("Nenhum motorista ativo cadastrado no momento.", fontSize = 12.sp, color = RedDanger)
                    } else {
                        activeDrivers.take(8).forEach { d ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedDriver = d }
                                    .padding(vertical = 2.dp)
                            ) {
                                RadioButton(selected = selectedDriver?.id == d.id, onClick = { selectedDriver = d })
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("${d.fullName} (${d.vehicleModel})", fontSize = 12.sp)
                            }
                        }
                    }
                }

                item {
                    Text("Forma de Pagamento *:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    if (paymentMethods.isEmpty()) {
                        Text("Nenhuma forma de pagamento cadastrada.", fontSize = 12.sp, color = RedDanger)
                    } else {
                        paymentMethods.forEach { pm ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedPaymentMethod = pm }
                                    .padding(vertical = 2.dp)
                            ) {
                                RadioButton(selected = selectedPaymentMethod?.id == pm.id, onClick = { selectedPaymentMethod = pm })
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(pm.name, fontSize = 12.sp)
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Observações (voo, placa, preferências)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (errorMessage != null) {
                    item {
                        Text(errorMessage ?: "", color = RedDanger, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceInput.toDoubleOrNull()
                    if (selectedCompany == null) {
                        errorMessage = "Selecione uma empresa solicitante."
                        return@Button
                    }
                    if (requesterName.isBlank()) {
                        errorMessage = "Informe o nome do passageiro."
                        return@Button
                    }
                    if (origin.isBlank() || destination.isBlank()) {
                        errorMessage = "Informe os endereços de origem e destino."
                        return@Button
                    }
                    if (price == null || price <= 0.0) {
                        errorMessage = "Informe um valor numérico válido para a corrida."
                        return@Button
                    }
                    if (selectedDriver == null) {
                        errorMessage = "Selecione um motorista responsável."
                        return@Button
                    }
                    if (selectedPaymentMethod == null) {
                        errorMessage = "Selecione uma forma de pagamento."
                        return@Button
                    }

                    onSave(
                        selectedCompany!!,
                        requesterName.trim(),
                        1L,
                        origin.trim(),
                        destination.trim(),
                        price,
                        selectedPaymentMethod!!,
                        selectedDriver!!,
                        notes.trim()
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Text("Salvar Viagem")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
