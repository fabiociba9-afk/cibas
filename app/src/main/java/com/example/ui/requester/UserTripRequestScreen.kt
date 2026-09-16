package com.example.ui.requester

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.TripEntity
import com.example.data.entity.TripStatus
import com.example.data.entity.UserEntity
import com.example.ui.MainViewModel
import com.example.ui.components.ReceiptDialog
import com.example.ui.components.TripStatusBadge
import com.example.ui.theme.*
import com.example.util.Formatters
import java.util.Calendar

@Composable
fun UserTripRequestScreen(
    viewModel: MainViewModel,
    currentUser: UserEntity,
    modifier: Modifier = Modifier
) {
    val companies by viewModel.companies.collectAsState()
    val drivers by viewModel.drivers.collectAsState()
    val paymentMethods by viewModel.paymentMethods.collectAsState()
    val trips by viewModel.trips.collectAsState()

    val myCompany = companies.firstOrNull { it.id == currentUser.companyId } ?: companies.firstOrNull()
    // User sees trips requested by themselves or their company
    val myTrips = trips.filter { it.requesterUserId == currentUser.id || it.companyId == myCompany?.id }

    var showRequestDialog by remember { mutableStateOf(false) }
    var selectedTripForReceipt by remember { mutableStateOf<TripEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showRequestDialog = true },
                containerColor = EmeraldAccent,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.DirectionsCar, contentDescription = null) },
                text = { Text("Solicitar Viagem Executiva", fontWeight = FontWeight.Bold) }
            )
        },
        containerColor = SlateLight
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Welcome Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().shadow(3.dp, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = NavyPrimary)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Olá, ${currentUser.name}",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${currentUser.jobTitle ?: "Colaborador"} • ${myCompany?.name ?: "Empresa Conveniada"}",
                            color = EmeraldContainer,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Solicite transporte corporativo executivo com motoristas credenciados e acompanhe suas viagens em tempo real.",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Section: My requested trips
            item {
                Text(
                    text = "Suas Viagens Solicitadas (${myTrips.size})",
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
                            Text("Você ainda não solicitou nenhuma viagem.", color = SlateTextSecondary, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { showRequestDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent)
                            ) {
                                Text("Solicitar Agora")
                            }
                        }
                    }
                }
            } else {
                items(myTrips) { trip ->
                    val driver = drivers.firstOrNull { it.id == trip.driverId }

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
                                Text(
                                    text = Formatters.formatDateTime(trip.dateTimeMillis),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = NavyPrimary
                                )
                                TripStatusBadge(trip.status)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Route
                            Text("📍 Origem: ${trip.origin}", fontSize = 12.sp, color = SlateTextPrimary)
                            Text("🏁 Destino: ${trip.destination}", fontSize = 12.sp, color = SlateTextPrimary)

                            Spacer(modifier = Modifier.height(10.dp))

                            // Driver info card
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Motorista Atribuído:", fontSize = 10.sp, color = SlateTextSecondary)
                                        Text(trip.driverName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = NavyPrimary)
                                        if (driver != null) {
                                            Text("${driver.vehicleModel} • Placa ${driver.vehiclePlate}", fontSize = 11.sp, color = SlateTextSecondary)
                                        }
                                    }
                                    if (trip.status == TripStatus.EM_ANDAMENTO.name) {
                                        Surface(
                                            color = EmeraldContainer,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                "Em Trânsito",
                                                color = EmeraldDark,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (trip.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Obs: ${trip.notes}", fontSize = 11.sp, color = SlateTextSecondary)
                            }

                            if (trip.status == TripStatus.CONCLUIDA.name) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { selectedTripForReceipt = trip },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Visualizar / Compartilhar Recibo", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRequestDialog && myCompany != null) {
        NewRequestDialog(
            company = myCompany,
            currentUser = currentUser,
            paymentMethods = paymentMethods,
            drivers = drivers,
            onDismiss = { showRequestDialog = false },
            onSave = { origin, dest, notes, pm, driver ->
                val now = System.currentTimeMillis()
                val cal = Calendar.getInstance()
                cal.timeInMillis = now
                cal.add(Calendar.DAY_OF_MONTH, 15)
                val clientDue = cal.timeInMillis
                cal.timeInMillis = now
                cal.add(Calendar.DAY_OF_MONTH, 5)
                val driverDue = cal.timeInMillis

                val estimatedPrice = 180.0

                viewModel.createTrip(
                    company = myCompany,
                    requesterName = currentUser.name,
                    requesterUserId = currentUser.id,
                    dateTimeMillis = now,
                    origin = origin,
                    destination = dest,
                    price = estimatedPrice,
                    paymentMethod = pm,
                    clientPaymentDueDateMillis = clientDue,
                    driver = driver,
                    driverCommissionDueDateMillis = driverDue,
                    notes = notes
                )
                showRequestDialog = false
            }
        )
    }

    if (selectedTripForReceipt != null) {
        ReceiptDialog(
            trip = selectedTripForReceipt!!,
            onDismiss = { selectedTripForReceipt = null }
        )
    }
}

@Composable
fun NewRequestDialog(
    company: com.example.data.entity.CompanyEntity,
    currentUser: UserEntity,
    paymentMethods: List<com.example.data.entity.PaymentMethodEntity>,
    drivers: List<com.example.data.entity.DriverEntity>,
    onDismiss: () -> Unit,
    onSave: (
        origin: String,
        destination: String,
        notes: String,
        pm: com.example.data.entity.PaymentMethodEntity,
        driver: com.example.data.entity.DriverEntity
    ) -> Unit
) {
    var origin by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedPm by remember { mutableStateOf(paymentMethods.firstOrNull()) }
    var selectedDriver by remember { mutableStateOf(drivers.firstOrNull { it.isActive }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Solicitar Viagem Executiva", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Empresa: ${company.name}", fontSize = 12.sp, color = SlateTextSecondary)
                Text("Passageiro: ${currentUser.name}", fontSize = 12.sp, color = SlateTextSecondary)

                OutlinedTextField(
                    value = origin,
                    onValueChange = { origin = it },
                    label = { Text("Endereço de Embarque / Origem") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = destination,
                    onValueChange = { destination = it },
                    label = { Text("Endereço de Desembarque / Destino") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observações (ex: Terminal 2 GRU, Portão B)") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (selectedPm != null) {
                    Text("Forma de Faturamento:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    paymentMethods.forEach { pm ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPm = pm }
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(selected = selectedPm?.id == pm.id, onClick = { selectedPm = pm })
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(pm.name, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (origin.isNotBlank() && destination.isNotBlank() && selectedPm != null && selectedDriver != null) {
                        onSave(origin, destination, notes, selectedPm!!, selectedDriver!!)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent)
            ) {
                Text("Confirmar Solicitação")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
