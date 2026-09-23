package com.aistudio.executivogo.trnsp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.executivogo.trnsp.data.AppUser
import com.aistudio.executivogo.trnsp.data.Trip
import com.aistudio.executivogo.trnsp.data.TripStatus
import com.aistudio.executivogo.trnsp.navigation.Screen
import com.aistudio.executivogo.trnsp.ui.components.AdminTopBar
import com.aistudio.executivogo.trnsp.ui.components.ExecutivePullRefreshBox
import com.aistudio.executivogo.trnsp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: MainViewModel,
    onNavigate: (String) -> Unit = {},
    onNavigateToTrips: () -> Unit = {},
    onNavigateToDrivers: () -> Unit = {},
    onNavigateToUsers: () -> Unit = {},
    onNavigateToCompanies: () -> Unit = {},
    onNavigateToPassengers: () -> Unit = {},
    onNavigateToFinancial: () -> Unit = {},
    onNavigateToPaymentMethods: () -> Unit = {},
    onNavigateToFareBands: () -> Unit = {},
    onNavigateToRoutes: () -> Unit = {},
    onNavigateToLiveMap: () -> Unit = {},
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val trips by viewModel.trips.collectAsState()
    val drivers by viewModel.drivers.collectAsState()
    val companies by viewModel.companies.collectAsState()
    val passengers by viewModel.passengers.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var selectedTripForReceipt by remember { mutableStateOf<Trip?>(null) }

    val activeTrips = remember(trips) {
        trips.count { it.status == TripStatus.PENDING || it.status == TripStatus.IN_PROGRESS }
    }
    val totalRevenue = remember(trips) {
        trips.filter { it.status == TripStatus.COMPLETED }.sumOf { it.price }
    }

    Scaffold(
        topBar = {
            AdminTopBar(
                title = "Painel Administrativo",
                subtitle = "ExecutivoGo • ${currentUser?.name ?: "Administrador"}",
                currentScreen = Screen.Dashboard.route,
                onNavigate = { route ->
                    when (route) {
                        Screen.Trips.route -> onNavigateToTrips()
                        Screen.Drivers.route -> onNavigateToDrivers()
                        Screen.Users.route -> onNavigateToUsers()
                        Screen.Companies.route -> onNavigateToCompanies()
                        Screen.Passengers.route -> onNavigateToPassengers()
                        Screen.Financial.route -> onNavigateToFinancial()
                        Screen.PaymentMethods.route -> onNavigateToPaymentMethods()
                        Screen.FareBands.route -> onNavigateToFareBands()
                        Screen.Routes.route -> onNavigateToRoutes()
                        Screen.LiveMap.route -> onNavigateToLiveMap()
                        else -> onNavigate(route)
                    }
                },
                onLogout = { viewModel.logout(onLogout) },
                onRefresh = { viewModel.refreshAll() }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        ExecutivePullRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshAll() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SlateLight)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
            // Metrics Row 1
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ExecutiveStatCard(
                        title = "Corridas Ativas",
                        value = "$activeTrips em curso",
                        subtitle = "${trips.size} registradas",
                        icon = Icons.Default.DirectionsCar,
                        accentColor = EmeraldAccent,
                        modifier = Modifier.weight(1f)
                    )
                    ExecutiveStatCard(
                        title = "Motoristas Cadastrados",
                        value = "${drivers.size} motoristas",
                        subtitle = "Online 24h prontos",
                        icon = Icons.Default.GpsFixed,
                        accentColor = NavyContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Metrics Row 2
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ExecutiveStatCard(
                        title = "Empresas Clientes",
                        value = "${companies.size} corporativas",
                        subtitle = "${passengers.size} passageiros",
                        icon = Icons.Default.Business,
                        accentColor = NavySecondary,
                        modifier = Modifier.weight(1f)
                    )
                    ExecutiveStatCard(
                        title = "Faturamento",
                        value = "R$ %.2f".format(totalRevenue),
                        subtitle = "Corridas concluídas",
                        icon = Icons.Default.AttachMoney,
                        accentColor = EmeraldLight,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick Navigation Hub
            item {
                Text(
                    text = "Módulos de Gestão",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onNavigateToTrips,
                            colors = executiveButtonDarkColors(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Default.Route, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Viagens", fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Button(
                            onClick = onNavigateToDrivers,
                            colors = executiveButtonPrimaryColors(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Motoristas", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onNavigateToCompanies,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NavyPrimary),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(16.dp), tint = NavyPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Empresas", fontSize = 12.sp, color = NavyPrimary)
                        }

                        OutlinedButton(
                            onClick = onNavigateToPassengers,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NavyPrimary),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(16.dp), tint = NavyPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Passageiros", fontSize = 12.sp, color = NavyPrimary)
                        }

                        OutlinedButton(
                            onClick = onNavigateToUsers,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NavyPrimary),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp), tint = NavyPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Usuários", fontSize = 12.sp, color = NavyPrimary)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onNavigateToFinancial,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NavyPrimary),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp), tint = NavyPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Financeiro", fontSize = 12.sp, color = NavyPrimary)
                        }

                        OutlinedButton(
                            onClick = onNavigateToPaymentMethods,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NavyPrimary),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(16.dp), tint = NavyPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pagamentos", fontSize = 12.sp, color = NavyPrimary)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onNavigateToFareBands,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NavyPrimary),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Icon(Icons.Default.PriceChange, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldDark)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Faixas de Preço", fontSize = 12.sp, color = NavyPrimary)
                        }

                        OutlinedButton(
                            onClick = onNavigateToRoutes,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NavyPrimary),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Icon(Icons.Default.AltRoute, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldDark)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rotas & Tabela", fontSize = 12.sp, color = NavyPrimary)
                        }
                    }
                }
            }

            // Recent Trips Title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Últimas Viagens em Tempo Real",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                    TextButton(onClick = onNavigateToTrips) {
                        Text("Ver todas", color = EmeraldAccent, fontSize = 13.sp)
                    }
                }
            }

            // Recent Trips List
            if (trips.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateCard)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Nenhuma viagem agendada na nuvem.", color = SlateTextSecondary, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                items(trips.take(4), key = { it.id }) { trip ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(1.dp, RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateCard)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = trip.passengerName.ifBlank { "Passageiro" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = NavyPrimary
                                )
                                Text(
                                    text = "${trip.companyName} • Motorista: ${trip.driverName.ifBlank { "Aguardando" }}",
                                    fontSize = 12.sp,
                                    color = SlateTextSecondary
                                )
                                Text(
                                    text = "R$ %.2f".format(trip.price),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldAccent
                                )
                            }
                            TripStatusBadge(trip.status)
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
