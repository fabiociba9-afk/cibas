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
import com.example.data.entity.TripStatus
import com.example.ui.MainViewModel
import com.example.ui.components.ExecutiveStatCard
import com.example.ui.theme.*
import com.example.util.Formatters

@Composable
fun FinancialScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val trips by viewModel.trips.collectAsState()
    val drivers by viewModel.drivers.collectAsState()
    val companies by viewModel.companies.collectAsState()
    val periodFilter by viewModel.financialPeriodFilter.collectAsState()

    var selectedSection by remember { mutableStateOf("GERAL") } // "GERAL", "MOTORISTAS", "EMPRESAS"

    // Calculate Global Financials
    val completedTrips = trips.filter { it.status == TripStatus.CONCLUIDA.name }
    val totalBilled = completedTrips.sumOf { it.price }
    val totalCommissions = completedTrips.sumOf { it.driverCommissionAmount }
    val netProfit = totalBilled - totalCommissions

    val clientPaidTotal = completedTrips.filter { it.isClientPaid }.sumOf { it.price }
    val clientPendingTotal = completedTrips.filter { !it.isClientPaid }.sumOf { it.price }

    val driverPaidCommissions = completedTrips.filter { it.isDriverPaid }.sumOf { it.driverCommissionAmount }
    val driverPendingCommissions = completedTrips.filter { !it.isDriverPaid }.sumOf { it.driverCommissionAmount }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SlateLight),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Period Filter Bar
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                modifier = Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Período:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NavyPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("HOJE" to "Hoje", "SEMANA" to "Semana", "MES" to "Este Mês", "TODOS" to "Todos").forEach { (code, label) ->
                            FilterChip(
                                selected = periodFilter == code,
                                onClick = { viewModel.setFinancialPeriod(code) },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }

        // Section Tabs: Geral / Por Motorista / Por Empresa
        item {
            TabRow(
                selectedTabIndex = when (selectedSection) {
                    "GERAL" -> 0
                    "MOTORISTAS" -> 1
                    else -> 2
                },
                containerColor = SlateCard,
                contentColor = NavyPrimary,
                modifier = Modifier.shadow(1.dp, RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedSection == "GERAL",
                    onClick = { selectedSection = "GERAL" },
                    text = { Text("Visão Geral", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedSection == "MOTORISTAS",
                    onClick = { selectedSection = "MOTORISTAS" },
                    text = { Text("Por Motorista", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedSection == "EMPRESAS",
                    onClick = { selectedSection = "EMPRESAS" },
                    text = { Text("Por Empresa", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        if (selectedSection == "GERAL") {
            // High level Profit Box
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().shadow(3.dp, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = NavyPrimary)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "LUCRO LÍQUIDO EXECUTIVOGO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldContainer
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = Formatters.formatCurrency(netProfit),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Margem líquida de ${(if (totalBilled > 0) (netProfit / totalBilled * 100).toInt() else 0)}% sobre o faturamento total",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Grid of 4 Cards: Billed / Commissions / To Receive / Received
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ExecutiveStatCard(
                        title = "Total Faturado",
                        value = Formatters.formatCurrency(totalBilled),
                        subtitle = "${completedTrips.size} viagens concluídas",
                        icon = Icons.Default.Paid,
                        accentColor = EmeraldAccent,
                        modifier = Modifier.weight(1f)
                    )
                    ExecutiveStatCard(
                        title = "Total de Comissões",
                        value = Formatters.formatCurrency(totalCommissions),
                        subtitle = "Repasse aos parceiros",
                        icon = Icons.Default.Payments,
                        accentColor = Color(0xFF0284C7),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ExecutiveStatCard(
                        title = "Valores a Receber",
                        value = Formatters.formatCurrency(clientPendingTotal),
                        subtitle = "Faturados pendentes",
                        icon = Icons.Default.PendingActions,
                        accentColor = GoldWarning,
                        modifier = Modifier.weight(1f)
                    )
                    ExecutiveStatCard(
                        title = "Valores Recebidos",
                        value = Formatters.formatCurrency(clientPaidTotal),
                        subtitle = "Liquidados pelo cliente",
                        icon = Icons.Default.CheckCircleOutline,
                        accentColor = EmeraldAccent,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Repasse a Motoristas Summary
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateCard)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Status dos Repasses aos Motoristas",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Comissões Pagas", fontSize = 11.sp, color = SlateTextSecondary)
                                Text(
                                    Formatters.formatCurrency(driverPaidCommissions),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldDark
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Comissões Pendentes", fontSize = 11.sp, color = SlateTextSecondary)
                                Text(
                                    Formatters.formatCurrency(driverPendingCommissions),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldWarning
                                )
                            }
                        }
                    }
                }
            }
        }

        if (selectedSection == "MOTORISTAS") {
            item {
                Text(
                    text = "Controle Financeiro por Motorista",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )
            }

            items(drivers) { driver ->
                val driverTrips = trips.filter { it.driverId == driver.id }
                val driverCompleted = driverTrips.filter { it.status == TripStatus.CONCLUIDA.name }
                val driverCommissionEarned = driverCompleted.sumOf { it.driverCommissionAmount }
                val driverPending = driverCompleted.filter { !it.isDriverPaid }.sumOf { it.driverCommissionAmount }

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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(driver.fullName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NavyPrimary)
                                Text("${driver.vehicleModel} • ${driver.vehiclePlate}", fontSize = 11.sp, color = SlateTextSecondary)
                            }
                            Surface(
                                color = EmeraldContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    "${driver.commissionPercentage}% comissão",
                                    fontSize = 10.sp,
                                    color = EmeraldDark,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Viagens Concluídas: ${driverCompleted.size}", fontSize = 12.sp, color = SlateTextPrimary)
                                Text("Total Ganho: ${Formatters.formatCurrency(driverCommissionEarned)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = EmeraldAccent)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("A Repassar:", fontSize = 11.sp, color = SlateTextSecondary)
                                Text(
                                    Formatters.formatCurrency(driverPending),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (driverPending > 0) GoldWarning else EmeraldDark
                                )
                            }
                        }

                        // List trips for this driver that can be toggled "Marcar como Pago"
                        if (driverCompleted.isNotEmpty()) {
                            Divider(modifier = Modifier.padding(vertical = 8.dp), color = SlateBorder)
                            Text("Viagens Realizadas:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SlateTextSecondary)
                            driverCompleted.take(3).forEach { tr ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("#${tr.id} - ${tr.destination}", fontSize = 11.sp, maxLines = 1)
                                        Text(
                                            "Comissão: ${Formatters.formatCurrency(tr.driverCommissionAmount)} • Venc: ${Formatters.formatDate(tr.driverCommissionDueDateMillis)}",
                                            fontSize = 10.sp,
                                            color = SlateTextSecondary
                                        )
                                    }
                                    Button(
                                        onClick = { viewModel.toggleDriverPaid(tr) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (tr.isDriverPaid) EmeraldContainer else Color(0xFFFEF3C7)
                                        ),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (tr.isDriverPaid) "Pago ✓" else "Pagar",
                                            color = if (tr.isDriverPaid) EmeraldDark else Color(0xFFB45309),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (selectedSection == "EMPRESAS") {
            item {
                Text(
                    text = "Controle Financeiro por Empresa",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )
            }

            items(companies) { company ->
                val compTrips = trips.filter { it.companyId == company.id }
                val compCompleted = compTrips.filter { it.status == TripStatus.CONCLUIDA.name }
                val compTotalBilled = compCompleted.sumOf { it.price }
                val compPending = compCompleted.filter { !it.isClientPaid }.sumOf { it.price }
                val compPaid = compCompleted.filter { it.isClientPaid }.sumOf { it.price }

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
                            Text(company.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NavyPrimary)
                            Text("CNPJ: ${company.cnpj}", fontSize = 11.sp, color = SlateTextSecondary)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Total Faturado:", fontSize = 11.sp, color = SlateTextSecondary)
                                Text(
                                    Formatters.formatCurrency(compTotalBilled),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyPrimary
                                )
                            }
                            Column {
                                Text("Já Pago:", fontSize = 11.sp, color = SlateTextSecondary)
                                Text(
                                    Formatters.formatCurrency(compPaid),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = EmeraldAccent
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("A Receber:", fontSize = 11.sp, color = SlateTextSecondary)
                                Text(
                                    Formatters.formatCurrency(compPending),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (compPending > 0) GoldWarning else EmeraldDark
                                )
                            }
                        }

                        // Toggle client payment
                        if (compCompleted.isNotEmpty()) {
                            Divider(modifier = Modifier.padding(vertical = 8.dp), color = SlateBorder)
                            Text("Faturas / Viagens:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SlateTextSecondary)
                            compCompleted.take(3).forEach { tr ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("#${tr.id} • ${Formatters.formatCurrency(tr.price)} (${tr.paymentMethodName})", fontSize = 11.sp)
                                    Button(
                                        onClick = { viewModel.toggleClientPaid(tr) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (tr.isClientPaid) EmeraldContainer else Color(0xFFFEF3C7)
                                        ),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (tr.isClientPaid) "Recebido ✓" else "Pendente",
                                            color = if (tr.isClientPaid) EmeraldDark else Color(0xFFB45309),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
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
}
