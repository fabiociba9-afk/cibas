package com.example.ui.company

import androidx.compose.foundation.background
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
import com.example.data.entity.CompanyEntity
import com.example.data.entity.TripEntity
import com.example.data.entity.TripStatus
import com.example.data.entity.UserEntity
import com.example.data.entity.UserRole
import com.example.ui.MainViewModel
import com.example.ui.admin.TripEditDialog
import com.example.ui.admin.CompanyUserDialog
import com.example.ui.components.ReceiptDialog
import com.example.ui.components.TripStatusBadge
import com.example.ui.theme.*
import com.example.util.Formatters
import java.util.Calendar

@Composable
fun CompanyAdminScreen(
    viewModel: MainViewModel,
    currentUser: UserEntity,
    modifier: Modifier = Modifier
) {
    val companies by viewModel.companies.collectAsState()
    val trips by viewModel.trips.collectAsState()
    val users by viewModel.users.collectAsState()
    val drivers by viewModel.drivers.collectAsState()
    val paymentMethods by viewModel.paymentMethods.collectAsState()

    val company = companies.firstOrNull { it.id == currentUser.companyId } ?: companies.firstOrNull()
    val companyTrips = trips.filter { it.companyId == company?.id }
    val companyUsers = users.filter { it.companyId == company?.id }

    var selectedTab by remember { mutableStateOf("VIAGENS") } // "VIAGENS", "COLABORADORES", "FATURAS"
    var showNewTripDialog by remember { mutableStateOf(false) }
    var showNewUserDialog by remember { mutableStateOf(false) }
    var selectedTripForReceipt by remember { mutableStateOf<TripEntity?>(null) }

    val completedTrips = companyTrips.filter { it.status == TripStatus.CONCLUIDA.name }
    val totalBilled = completedTrips.sumOf { it.price }
    val pendingPayment = completedTrips.filter { !it.isClientPaid }.sumOf { it.price }

    Scaffold(
        floatingActionButton = {
            if (selectedTab == "VIAGENS") {
                FloatingActionButton(
                    onClick = { showNewTripDialog = true },
                    containerColor = EmeraldAccent,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.AddRoad, contentDescription = "Solicitar Viagem")
                }
            } else if (selectedTab == "COLABORADORES") {
                FloatingActionButton(
                    onClick = { showNewUserDialog = true },
                    containerColor = NavyPrimary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Cadastrar Usuário")
                }
            }
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
            // Company Header Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = NavyPrimary)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Painel Corporativo",
                            color = EmeraldContainer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = company?.name ?: "Empresa Conveniada",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "CNPJ: ${company?.cnpj ?: ""} • Gestor: ${currentUser.name}",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Faturamento Total:", color = SlateTextSecondary, fontSize = 11.sp)
                                Text(Formatters.formatCurrency(totalBilled), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Faturas em Aberto:", color = SlateTextSecondary, fontSize = 11.sp)
                                Text(Formatters.formatCurrency(pendingPayment), color = GoldWarning, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }

            // Tabs: Viagens / Colaboradores / Faturas
            item {
                TabRow(
                    selectedTabIndex = when (selectedTab) {
                        "VIAGENS" -> 0
                        "COLABORADORES" -> 1
                        else -> 2
                    },
                    containerColor = SlateCard,
                    contentColor = NavyPrimary,
                    modifier = Modifier.shadow(1.dp, RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == "VIAGENS",
                        onClick = { selectedTab = "VIAGENS" },
                        text = { Text("Viagens (${companyTrips.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == "COLABORADORES",
                        onClick = { selectedTab = "COLABORADORES" },
                        text = { Text("Equipe (${companyUsers.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == "FATURAS",
                        onClick = { selectedTab = "FATURAS" },
                        text = { Text("Faturamento", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            if (selectedTab == "VIAGENS") {
                if (companyTrips.isEmpty()) {
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
                                Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = SlateTextSecondary, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Nenhuma viagem solicitada ainda para sua empresa.", color = SlateTextSecondary, fontSize = 13.sp)
                            }
                        }
                    }
                } else {
                    items(companyTrips) { trip ->
                        Card(
                            modifier = Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(14.dp)),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = SlateCard)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Solicitante: ${trip.requesterName}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = NavyPrimary
                                    )
                                    TripStatusBadge(trip.status)
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text("De: ${trip.origin}", fontSize = 12.sp, color = SlateTextPrimary)
                                Text("Para: ${trip.destination}", fontSize = 12.sp, color = SlateTextPrimary)

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Motorista: ${trip.driverName}", fontSize = 11.sp, color = SlateTextSecondary)
                                    Text(Formatters.formatCurrency(trip.price), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = EmeraldAccent)
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

            if (selectedTab == "COLABORADORES") {
                items(companyUsers) { u ->
                    Card(
                        modifier = Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateCard)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(u.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NavyPrimary)
                                Text(
                                    text = "${u.jobTitle ?: "Colaborador"} • ${if (u.role == UserRole.COMPANY_ADMIN.name) "Administrador" else "Solicitante"}",
                                    fontSize = 11.sp,
                                    color = EmeraldDark,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text("${u.email} • ${u.phone}", fontSize = 11.sp, color = SlateTextSecondary)
                            }
                            Switch(
                                checked = u.isActive,
                                onCheckedChange = { viewModel.toggleUserStatus(u) }
                            )
                        }
                    }
                }
            }

            if (selectedTab == "FATURAS") {
                items(completedTrips) { trip ->
                    Card(
                        modifier = Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateCard)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Fatura #${trip.id} • ${Formatters.formatDate(trip.dateTimeMillis)}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Surface(
                                    color = if (trip.isClientPaid) EmeraldContainer else Color(0xFFFEF3C7),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (trip.isClientPaid) "PAGO" else "A PAGAR",
                                        color = if (trip.isClientPaid) EmeraldDark else Color(0xFFB45309),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Forma: ${trip.paymentMethodName} • Vencimento: ${Formatters.formatDate(trip.clientPaymentDueDateMillis)}", fontSize = 11.sp, color = SlateTextSecondary)
                            Text("Valor: ${Formatters.formatCurrency(trip.price)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = EmeraldAccent)
                        }
                    }
                }
            }
        }
    }

    if (showNewTripDialog && company != null) {
        TripEditDialog(
            trip = null,
            companies = listOf(company),
            drivers = drivers,
            paymentMethods = paymentMethods,
            users = companyUsers,
            onDismiss = { showNewTripDialog = false },
            onSave = { comp, reqName, reqId, origin, dest, price, pm, driver, notes ->
                val now = System.currentTimeMillis()
                val cal = Calendar.getInstance()
                cal.timeInMillis = now
                cal.add(Calendar.DAY_OF_MONTH, 15)
                val clientDue = cal.timeInMillis
                cal.timeInMillis = now
                cal.add(Calendar.DAY_OF_MONTH, 5)
                val driverDue = cal.timeInMillis

                viewModel.createTrip(
                    company = comp,
                    requesterName = reqName,
                    requesterUserId = currentUser.id,
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
                showNewTripDialog = false
            }
        )
    }

    if (showNewUserDialog && company != null) {
        CompanyUserDialog(
            company = company,
            onDismiss = { showNewUserDialog = false },
            onSave = { name, email, phone, role, jobTitle, pass ->
                viewModel.saveUser(
                    name = name,
                    email = email,
                    phone = phone,
                    role = role,
                    companyId = company.id,
                    jobTitle = jobTitle,
                    password = pass
                )
                showNewUserDialog = false
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
