package com.example.ui.admin

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
import com.example.data.entity.DriverEntity
import com.example.data.entity.TripEntity
import com.example.data.entity.TripStatus
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.Formatters

@Composable
fun AdminDashboardScreen(
    viewModel: MainViewModel,
    onNavigateToTrips: () -> Unit,
    onNavigateToDrivers: () -> Unit,
    onNavigateToFinancial: () -> Unit,
    onNavigateToCompanies: () -> Unit,
    onNavigateToUsers: () -> Unit = {},
    onNavigateToPaymentMethods: () -> Unit = {},
    onOpenNewTripDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val trips by viewModel.trips.collectAsState()
    val drivers by viewModel.drivers.collectAsState()
    val companies by viewModel.companies.collectAsState()
    val users by viewModel.users.collectAsState()

    var selectedDriverOnMap by remember { mutableStateOf<DriverEntity?>(null) }
    var selectedTripForReceipt by remember { mutableStateOf<TripEntity?>(null) }

    // Financial Metrics
    val completedTrips = trips.filter { it.status == TripStatus.CONCLUIDA.name }
    val totalBilled = completedTrips.sumOf { it.price }
    val totalCommissions = completedTrips.sumOf { it.driverCommissionAmount }
    val netProfit = totalBilled - totalCommissions
    val activeTripsCount = trips.count { it.status == TripStatus.EM_ANDAMENTO.name || it.status == TripStatus.AGENDADA.name }
    val activeDriversCount = drivers.count { it.isActive }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SlateLight),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Executive Summary Header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NavyPrimary),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().shadow(3.dp, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Visão Geral ExecutivoGo",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                        IconButton(
                            onClick = onOpenNewTripDialog,
                            colors = IconButtonDefaults.iconButtonColors(containerColor = EmeraldAccent)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Nova Viagem", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Faturamento Realizado", color = SlateTextSecondary, fontSize = 11.sp)
                            Text(
                                text = Formatters.formatCurrency(totalBilled),
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Lucro Líquido", color = SlateTextSecondary, fontSize = 11.sp)
                            Text(
                                text = Formatters.formatCurrency(netProfit),
                                color = EmeraldLight,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Quick Admin Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateToUsers,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = SlateCard),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(16.dp), tint = NavySecondary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Usuários (${users.size})", fontSize = 11.sp, color = NavyPrimary, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onNavigateToPaymentMethods,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = SlateCard),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldAccent)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Formas Pagto", fontSize = 11.sp, color = NavyPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 4 KPI Grid Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExecutiveStatCard(
                    title = "Comissões",
                    value = Formatters.formatCurrency(totalCommissions),
                    subtitle = "Repasses aos motoristas",
                    icon = Icons.Default.Payments,
                    accentColor = Color(0xFF0284C7),
                    modifier = Modifier.weight(1f)
                )
                ExecutiveStatCard(
                    title = "Viagens em Aberto",
                    value = "$activeTripsCount ativas",
                    subtitle = "${trips.size} registradas",
                    icon = Icons.Default.Route,
                    accentColor = GoldWarning,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExecutiveStatCard(
                    title = "Frota Executiva",
                    value = "$activeDriversCount motoristas",
                    subtitle = "${drivers.size} cadastrados",
                    icon = Icons.Default.DirectionsCar,
                    accentColor = EmeraldAccent,
                    modifier = Modifier.weight(1f)
                )
                ExecutiveStatCard(
                    title = "Empresas Clientes",
                    value = "${companies.count { it.isActive }} ativas",
                    subtitle = "${companies.size} convênios",
                    icon = Icons.Default.Business,
                    accentColor = NavySecondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Live Radar Fleet Map Canvas
        item {
            DriverRadarMapCanvas(
                drivers = drivers,
                selectedDriver = selectedDriverOnMap,
                onSelectDriver = { selectedDriverOnMap = it }
            )
        }

        // Section: Recent Trips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Últimas Viagens",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )
                TextButton(onClick = onNavigateToTrips) {
                    Text("Ver Todas", color = EmeraldAccent, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        val recentTrips = trips.take(5)
        if (recentTrips.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Nenhuma viagem registrada ainda.",
                        color = SlateTextSecondary,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            items(recentTrips) { trip ->
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
                                text = trip.companyName,
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary,
                                fontSize = 14.sp
                            )
                            TripStatusBadge(trip.status)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "De: ${trip.origin}",
                            fontSize = 12.sp,
                            color = SlateTextPrimary
                        )
                        Text(
                            text = "Para: ${trip.destination}",
                            fontSize = 12.sp,
                            color = SlateTextPrimary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Motorista: ${trip.driverName}",
                                fontSize = 12.sp,
                                color = SlateTextSecondary
                            )
                            Text(
                                text = Formatters.formatCurrency(trip.price),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = EmeraldAccent
                            )
                        }

                        // If completed, show receipt button
                        if (trip.status == TripStatus.CONCLUIDA.name) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { selectedTripForReceipt = trip },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Visualizar / Enviar Recibo", fontSize = 12.sp)
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
