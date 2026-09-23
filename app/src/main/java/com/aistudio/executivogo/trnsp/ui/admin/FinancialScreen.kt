package com.aistudio.executivogo.trnsp.ui.admin

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
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
import com.aistudio.executivogo.trnsp.data.*
import com.aistudio.executivogo.trnsp.navigation.Screen
import com.aistudio.executivogo.trnsp.ui.FinancialFilter
import com.aistudio.executivogo.trnsp.ui.MainViewModel
import com.aistudio.executivogo.trnsp.ui.PeriodType
import com.aistudio.executivogo.trnsp.ui.components.AdminTopBar
import com.aistudio.executivogo.trnsp.ui.components.ExecutivePullRefreshBox
import com.aistudio.executivogo.trnsp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialScreen(
    viewModel: MainViewModel,
    onNavigate: ((String) -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Dashboard Geral", "A Receber (Empresas)", "Comissões (Motoristas)")

    val trips by viewModel.trips.collectAsState()
    val companies by viewModel.companies.collectAsState()
    val drivers by viewModel.drivers.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val filter by viewModel.financialFilter.collectAsState()

    // Status / Feedback Snack
    val statusMessage by viewModel.statusMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        topBar = {
            AdminTopBar(
                title = "Módulo Financeiro",
                subtitle = "Faturamento & Comissões",
                currentScreen = Screen.Financial.route,
                onNavigate = onNavigate,
                onLogout = onLogout,
                onRefresh = { viewModel.refreshTrips() }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = SlateLight,
        modifier = modifier
    ) { innerPadding ->
        ExecutivePullRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshTrips() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Tab Navigation
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = NavyPrimary,
                    contentColor = Color.White
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == index) EmeraldLight else Color.White.copy(alpha = 0.7f)
                                )
                            }
                        )
                    }
                }

                // Filter Period Selector
                PeriodSelectorBar(
                    filter = filter,
                    companies = companies,
                    drivers = drivers,
                    onSelectPeriod = { type, sMs, eMs -> viewModel.setFinancialFilter(type, sMs, eMs) },
                    onSelectCompany = { viewModel.setFinancialCompanyFilter(it) },
                    onSelectDriver = { viewModel.setFinancialDriverFilter(it) },
                    onClearFilters = { viewModel.clearFinancialFilters() }
                )

                // Tab Content
                when (selectedTab) {
                    0 -> FinancialDashboardTab(
                        trips = trips,
                        companies = companies,
                        drivers = drivers,
                        filter = filter,
                        viewModel = viewModel
                    )
                    1 -> CompanyReceivablesTab(
                        trips = trips,
                        companies = companies,
                        filter = filter,
                        viewModel = viewModel
                    )
                    2 -> DriverCommissionsTab(
                        trips = trips,
                        drivers = drivers,
                        filter = filter,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
fun PeriodSelectorBar(
    filter: FinancialFilter,
    companies: List<Company>,
    drivers: List<AppUser>,
    onSelectPeriod: (PeriodType, Long?, Long?) -> Unit,
    onSelectCompany: (String?) -> Unit,
    onSelectDriver: (String?) -> Unit,
    onClearFilters: () -> Unit
) {
    val context = LocalContext.current
    var companyDropdownOpen by remember { mutableStateOf(false) }
    var driverDropdownOpen by remember { mutableStateOf(false) }
    val selectedCompany = companies.find { it.id == filter.selectedCompanyId }
    val selectedDriver = drivers.find { it.id == filter.selectedDriverId }
    val hasActiveFilters = filter.type != PeriodType.ESTE_MES || filter.selectedCompanyId != null || filter.selectedDriverId != null

    Surface(
        color = Color(0xFF0F2644),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            // Linha 1: Chips de Período (Hoje, Este Mês, Mês Anterior, Este Ano, Todas, Calendário/Personalizado)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Período:",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                PeriodChip("Hoje", filter.type == PeriodType.HOJE) { onSelectPeriod(PeriodType.HOJE, null, null) }
                PeriodChip("Este mês", filter.type == PeriodType.ESTE_MES) { onSelectPeriod(PeriodType.ESTE_MES, null, null) }
                PeriodChip("Mês anterior", filter.type == PeriodType.MES_ANTERIOR) { onSelectPeriod(PeriodType.MES_ANTERIOR, null, null) }
                PeriodChip("Este ano", filter.type == PeriodType.ESTE_ANO) { onSelectPeriod(PeriodType.ESTE_ANO, null, null) }
                PeriodChip("Todas", filter.type == PeriodType.TODOS) { onSelectPeriod(PeriodType.TODOS, null, null) }

                // Botão de Calendário (Personalizado)
                Surface(
                    onClick = {
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
                                        onSelectPeriod(PeriodType.PERSONALIZADO, startCal.timeInMillis, endCal.timeInMillis)
                                    },
                                    startYear,
                                    startMonth,
                                    startDay
                                ).apply {
                                    setTitle("Data Final do Relatório")
                                    show()
                                }
                            },
                            nowCal.get(Calendar.YEAR),
                            nowCal.get(Calendar.MONTH),
                            nowCal.get(Calendar.DAY_OF_MONTH)
                        ).apply {
                            setTitle("Data Inicial do Relatório")
                            show()
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = if (filter.type == PeriodType.PERSONALIZADO) GoldAccent else Color.White.copy(alpha = 0.2f),
                    contentColor = if (filter.type == PeriodType.PERSONALIZADO) Color(0xFF0F172A) else Color.White
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (filter.type == PeriodType.PERSONALIZADO && filter.customStartMs != null && filter.customEndMs != null) {
                                val df = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
                                "${df.format(Date(filter.customStartMs))} até ${df.format(Date(filter.customEndMs))}"
                            } else "Por Calendário",
                            fontSize = 11.sp,
                            fontWeight = if (filter.type == PeriodType.PERSONALIZADO) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Linha 2: Dropdowns de Filtro por Empresa e Motorista + Botão de Limpar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dropdown Empresa
                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        onClick = { companyDropdownOpen = true },
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.12f),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedCompany?.name ?: "Empresa: Todas",
                                fontSize = 11.sp,
                                color = if (selectedCompany != null) GoldAccent else Color.White,
                                fontWeight = if (selectedCompany != null) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }

                    DropdownMenu(
                        expanded = companyDropdownOpen,
                        onDismissRequest = { companyDropdownOpen = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Todas as Empresas", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
                            onClick = {
                                onSelectCompany(null)
                                companyDropdownOpen = false
                            }
                        )
                        companies.forEach { comp ->
                            DropdownMenuItem(
                                text = { Text(comp.name, color = Color(0xFF0F172A)) },
                                onClick = {
                                    onSelectCompany(comp.id)
                                    companyDropdownOpen = false
                                }
                            )
                        }
                    }
                }

                // Dropdown Motorista
                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        onClick = { driverDropdownOpen = true },
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.12f),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedDriver?.name ?: "Motorista: Todos",
                                fontSize = 11.sp,
                                color = if (selectedDriver != null) EmeraldLight else Color.White,
                                fontWeight = if (selectedDriver != null) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }

                    DropdownMenu(
                        expanded = driverDropdownOpen,
                        onDismissRequest = { driverDropdownOpen = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Todos os Motoristas", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
                            onClick = {
                                onSelectDriver(null)
                                driverDropdownOpen = false
                            }
                        )
                        drivers.forEach { drv ->
                            DropdownMenuItem(
                                text = { Text(drv.name, color = Color(0xFF0F172A)) },
                                onClick = {
                                    onSelectDriver(drv.id)
                                    driverDropdownOpen = false
                                }
                            )
                        }
                    }
                }

                if (hasActiveFilters) {
                    IconButton(
                        onClick = onClearFilters,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Limpar Filtros", tint = Color(0xFFFCA5A5), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun PeriodChip(
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

// ==========================================
// ABA 0: DASHBOARD GERAL
// ==========================================
@Composable
fun FinancialDashboardTab(
    trips: List<Trip>,
    companies: List<Company>,
    drivers: List<AppUser>,
    filter: FinancialFilter,
    viewModel: MainViewModel
) {
    val now = System.currentTimeMillis()
    val completedTrips = remember(trips, filter) {
        trips.filter { it.status == TripStatus.COMPLETED && viewModel.isTripInFinancialPeriod(it, filter) }
    }

    val totalBilled = remember(completedTrips) { completedTrips.sumOf { it.price } }
    val companyPending = remember(completedTrips) {
        completedTrips.filter { it.companyPaymentStatus == PaymentStatus.PENDING }.sumOf { it.price }
    }
    val companyPaid = remember(completedTrips) {
        completedTrips.filter { it.companyPaymentStatus == PaymentStatus.PAID }.sumOf { it.price }
    }

    val totalCommissions = remember(completedTrips) { completedTrips.sumOf { it.driverCommission } }
    val commissionLiberated = remember(completedTrips, now) {
        completedTrips.filter {
            it.paymentStatus == PaymentStatus.PENDING && (it.commissionAvailableAt ?: 0L) <= now
        }.sumOf { it.driverCommission }
    }
    val commissionInGracePeriod = remember(completedTrips, now) {
        completedTrips.filter {
            it.paymentStatus == PaymentStatus.PENDING && (it.commissionAvailableAt ?: 0L) > now
        }.sumOf { it.driverCommission }
    }
    val commissionPaid = remember(completedTrips) {
        completedTrips.filter { it.paymentStatus == PaymentStatus.PAID }.sumOf { it.driverCommission }
    }

    val netResult = totalBilled - totalCommissions

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Net Result Master Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NavyDark),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(3.dp, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "RESULTADO LÍQUIDO DO PERÍODO",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldLight,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "R$ %.2f".format(netResult),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Faturado (R$ %.2f) − Comissões Totais (R$ %.2f)".format(totalBilled, totalCommissions),
                        fontSize = 12.sp,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }
        }

        // Faturamento Section Header
        item {
            Text(
                text = "Faturamento Corporativo (Empresas)",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = NavyPrimary
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FinancialMetricCard(
                    title = "Total Faturado",
                    amount = totalBilled,
                    subtitle = "${completedTrips.size} viagens",
                    icon = Icons.Default.TrendingUp,
                    color = EmeraldAccent,
                    modifier = Modifier.weight(1f)
                )
                FinancialMetricCard(
                    title = "A Receber",
                    amount = companyPending,
                    subtitle = "Em aberto",
                    icon = Icons.Default.HourglassEmpty,
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
                FinancialMetricCard(
                    title = "Já Recebido",
                    amount = companyPaid,
                    subtitle = "Liquidado",
                    icon = Icons.Default.CheckCircle,
                    color = EmeraldDark,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Comissões Section Header
        item {
            Text(
                text = "Repasses & Comissões (Motoristas)",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = NavyPrimary
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FinancialMetricCard(
                    title = "A Pagar Liberado",
                    amount = commissionLiberated,
                    subtitle = "Prazo expirado",
                    icon = Icons.Default.Payments,
                    color = RedDanger,
                    modifier = Modifier.weight(1f)
                )
                FinancialMetricCard(
                    title = "A Pagar No Prazo",
                    amount = commissionInGracePeriod,
                    subtitle = "Aguardando venc.",
                    icon = Icons.Default.Schedule,
                    color = Color(0xFF3B82F6),
                    modifier = Modifier.weight(1f)
                )
                FinancialMetricCard(
                    title = "Já Pago",
                    amount = commissionPaid,
                    subtitle = "Comissões pagas",
                    icon = Icons.Default.DoneAll,
                    color = EmeraldDark,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Resumo por Empresa Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Faturamento por Empresa no Período",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = NavyPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    companies.forEach { company ->
                        val companyTrips = completedTrips.filter { it.companyId == company.id }
                        val total = companyTrips.sumOf { it.price }
                        val pending = companyTrips.filter { it.companyPaymentStatus == PaymentStatus.PENDING }.sumOf { it.price }
                        val received = companyTrips.filter { it.companyPaymentStatus == PaymentStatus.PAID }.sumOf { it.price }

                        if (total > 0) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(company.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = NavyPrimary)
                                    Text(
                                        "Prazo: ${PaymentTerms.getLabel(company.paymentTerm)} • Meio: ${company.paymentMeans}",
                                        fontSize = 11.sp,
                                        color = SlateTextSecondary
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("R$ %.2f".format(total), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NavyPrimary)
                                    Text(
                                        "Pend: R$ %.2f | Rec: R$ %.2f".format(pending, received),
                                        fontSize = 10.sp,
                                        color = if (pending > 0) RedDanger else EmeraldDark
                                    )
                                }
                            }
                            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }

        // Resumo por Motorista Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Repasse por Motorista no Período",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = NavyPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    drivers.forEach { driver ->
                        val driverTrips = completedTrips.filter { it.driverId == driver.id }
                        val totalComm = driverTrips.sumOf { it.driverCommission }
                        val liberated = driverTrips.filter {
                            it.paymentStatus == PaymentStatus.PENDING && (it.commissionAvailableAt ?: 0L) <= now
                        }.sumOf { it.driverCommission }
                        val paid = driverTrips.filter { it.paymentStatus == PaymentStatus.PAID }.sumOf { it.driverCommission }

                        if (totalComm > 0) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(driver.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = NavyPrimary)
                                    Text("Contrato: ${driver.commissionPercentage}%", fontSize = 11.sp, color = SlateTextSecondary)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("R$ %.2f".format(totalComm), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NavyPrimary)
                                    Text(
                                        "Liberado: R$ %.2f | Pago: R$ %.2f".format(liberated, paid),
                                        fontSize = 10.sp,
                                        color = if (liberated > 0) RedDanger else EmeraldDark
                                    )
                                }
                            }
                            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// ABA 1: A RECEBER (EMPRESAS)
// ==========================================
@Composable
fun CompanyReceivablesTab(
    trips: List<Trip>,
    companies: List<Company>,
    filter: FinancialFilter,
    viewModel: MainViewModel
) {
    val now = System.currentTimeMillis()
    var selectedStatusFilter by remember { mutableStateOf("TODOS") } // TODOS, PENDENTES, PAGAS, VENCIDAS
    var selectedCompanyId by remember { mutableStateOf<String?>(null) }
    var selectedTripIds by remember { mutableStateOf(setOf<String>()) }
    var tripToUndo by remember { mutableStateOf<Trip?>(null) }

    val completedTrips = remember(trips, filter) {
        trips.filter { it.status == TripStatus.COMPLETED && viewModel.isTripInFinancialPeriod(it, filter) }
    }

    val filteredList = remember(completedTrips, selectedStatusFilter, selectedCompanyId) {
        completedTrips.filter { trip ->
            val matchCompany = selectedCompanyId == null || trip.companyId == selectedCompanyId
            val isOverdue = trip.companyPaymentStatus == PaymentStatus.PENDING && (trip.dueDate ?: Long.MAX_VALUE) < now
            val matchStatus = when (selectedStatusFilter) {
                "PENDENTES" -> trip.companyPaymentStatus == PaymentStatus.PENDING
                "PAGAS" -> trip.companyPaymentStatus == PaymentStatus.PAID
                "VENCIDAS" -> isOverdue
                else -> true
            }
            matchCompany && matchStatus
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Filter sub-bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterBadge("Todas (${completedTrips.size})", selectedStatusFilter == "TODOS") { selectedStatusFilter = "TODOS" }
            FilterBadge("Pendentes", selectedStatusFilter == "PENDENTES") { selectedStatusFilter = "PENDENTES" }
            FilterBadge("Recebidas", selectedStatusFilter == "PAGAS") { selectedStatusFilter = "PAGAS" }
            FilterBadge("Vencidas", selectedStatusFilter == "VENCIDAS") { selectedStatusFilter = "VENCIDAS" }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Batch Action Header if items selected
        if (selectedTripIds.isNotEmpty()) {
            Surface(
                color = EmeraldContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedTripIds.size} selecionadas",
                        fontWeight = FontWeight.Bold,
                        color = EmeraldDark,
                        fontSize = 13.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = {
                                viewModel.markBatchCompanyPaid(selectedTripIds.toList())
                                selectedTripIds = emptySet()
                            },
                            colors = executiveButtonPrimaryColors(),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Baixar em Lote", fontSize = 12.sp, color = Color.White)
                        }
                        OutlinedButton(
                            onClick = { selectedTripIds = emptySet() },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Limpar", fontSize = 12.sp)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (filteredList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nenhuma viagem localizada com os filtros selecionados.", color = SlateTextSecondary, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList, key = { it.id }) { trip ->
                    val isPaid = trip.companyPaymentStatus == PaymentStatus.PAID
                    val isOverdue = !isPaid && (trip.dueDate ?: Long.MAX_VALUE) < now
                    val isSelected = selectedTripIds.contains(trip.id)

                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(1.dp, RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (!isPaid) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { checked ->
                                                selectedTripIds = if (checked) selectedTripIds + trip.id else selectedTripIds - trip.id
                                            }
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = trip.companyName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = NavyPrimary
                                        )
                                        Text(
                                            text = "Passageiro: ${trip.passengerName}",
                                            fontSize = 12.sp,
                                            color = SlateTextSecondary
                                        )
                                    }
                                }

                                Surface(
                                    color = if (isPaid) EmeraldContainer else if (isOverdue) RedContainer else Color(0xFFFEF3C7),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (isPaid) "RECEBIDO" else if (isOverdue) "VENCIDO" else "PENDENTE",
                                        color = if (isPaid) EmeraldDark else if (isOverdue) RedDanger else Color(0xFFB45309),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Trajeto: ${trip.origin} ➔ ${trip.destination}",
                                fontSize = 12.sp,
                                color = NavyPrimary
                            )
                            Text(
                                text = "Motorista: ${trip.driverName.ifBlank { "Não informado" }}",
                                fontSize = 12.sp,
                                color = SlateTextSecondary
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "R$ %.2f".format(trip.price),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = EmeraldAccent
                                    )
                                    Text(
                                        text = "Venc: ${formatDate(trip.dueDate)} (${PaymentTerms.getLabel(trip.paymentTermSnapshot)})",
                                        fontSize = 11.sp,
                                        color = if (isOverdue) RedDanger else SlateTextSecondary
                                    )
                                }

                                if (!isPaid) {
                                    Button(
                                        onClick = { viewModel.markCompanyPaid(trip) },
                                        colors = executiveButtonPrimaryColors(),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("Marcar Recebido", fontSize = 12.sp, color = Color.White)
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { tripToUndo = trip },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text("Desfazer", fontSize = 11.sp, color = RedDanger)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (tripToUndo != null) {
        AlertDialog(
            onDismissRequest = { tripToUndo = null },
            title = { Text("Desfazer Recebimento?") },
            text = { Text("A viagem de ${tripToUndo?.companyName} voltará ao status PENDENTE de recebimento.") },
            confirmButton = {
                Button(
                    onClick = {
                        tripToUndo?.let { viewModel.undoCompanyPaid(it) }
                        tripToUndo = null
                    },
                    colors = executiveButtonDangerColors()
                ) {
                    Text("Sim, Desfazer", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { tripToUndo = null }) { Text("Cancelar") }
            }
        )
    }
}

// ==========================================
// ABA 2: COMISSÕES (MOTORISTAS)
// ==========================================
@Composable
fun DriverCommissionsTab(
    trips: List<Trip>,
    drivers: List<AppUser>,
    filter: FinancialFilter,
    viewModel: MainViewModel
) {
    val now = System.currentTimeMillis()
    var selectedStatusFilter by remember { mutableStateOf("TODOS") } // TODOS, A_RECEBER, VENCIDAS, PAGAS
    var selectedDriverId by remember { mutableStateOf<String?>(null) }
    var selectedTripIds by remember { mutableStateOf(setOf<String>()) }
    var tripToForcePay by remember { mutableStateOf<Trip?>(null) }
    var tripToUndo by remember { mutableStateOf<Trip?>(null) }

    val completedTrips = remember(trips, filter) {
        trips.filter { it.status == TripStatus.COMPLETED && viewModel.isTripInFinancialPeriod(it, filter) }
    }

    val filteredList = remember(completedTrips, selectedStatusFilter, selectedDriverId) {
        completedTrips.filter { trip ->
            val matchDriver = selectedDriverId == null || trip.driverId == selectedDriverId
            val isPaid = trip.paymentStatus == PaymentStatus.PAID
            val termDays = PaymentTerms.getDays(trip.paymentTermSnapshot)
            val baseTime = trip.completedAt ?: trip.scheduledTime
            val calculatedDue = trip.dueDate ?: trip.commissionAvailableAt ?: (baseTime + termDays * 86_400_000L)
            val isOverdue = !isPaid && calculatedDue < now
            val isOnSchedule = !isPaid && calculatedDue >= now

            val matchStatus = when (selectedStatusFilter) {
                "A_RECEBER" -> isOnSchedule
                "VENCIDAS" -> isOverdue
                "PAGAS" -> isPaid
                else -> true
            }
            matchDriver && matchStatus
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Filter sub-bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterBadge("Todas (${completedTrips.size})", selectedStatusFilter == "TODOS") { selectedStatusFilter = "TODOS" }
            FilterBadge("A Receber", selectedStatusFilter == "A_RECEBER") { selectedStatusFilter = "A_RECEBER" }
            FilterBadge("Vencidas", selectedStatusFilter == "VENCIDAS") { selectedStatusFilter = "VENCIDAS" }
            FilterBadge("Pagas", selectedStatusFilter == "PAGAS") { selectedStatusFilter = "PAGAS" }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Batch Action Header if items selected
        if (selectedTripIds.isNotEmpty()) {
            Surface(
                color = EmeraldContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedTripIds.size} comissões marcadas",
                        fontWeight = FontWeight.Bold,
                        color = EmeraldDark,
                        fontSize = 13.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = {
                                viewModel.markBatchCommissionPaid(selectedTripIds.toList())
                                selectedTripIds = emptySet()
                            },
                            colors = executiveButtonDarkColors(),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Pagar Selecionadas", fontSize = 12.sp, color = Color.White)
                        }
                        OutlinedButton(
                            onClick = { selectedTripIds = emptySet() },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Limpar", fontSize = 12.sp)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (filteredList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nenhuma comissão encontrada para este filtro.", color = SlateTextSecondary, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList, key = { it.id }) { trip ->
                    val isPaid = trip.paymentStatus == PaymentStatus.PAID
                    val termDays = PaymentTerms.getDays(trip.paymentTermSnapshot)
                    val baseTime = trip.completedAt ?: trip.scheduledTime
                    val calculatedDue = trip.dueDate ?: trip.commissionAvailableAt ?: (baseTime + termDays * 86_400_000L)
                    val isOverdue = !isPaid && calculatedDue < now
                    val isOnSchedule = !isPaid && calculatedDue >= now
                    val daysOverdue = if (isOverdue) ((now - calculatedDue) / 86_400_000L).toInt().coerceAtLeast(1) else 0
                    val daysRemaining = if (isOnSchedule) ((calculatedDue - now) / 86_400_000L).toInt() else 0
                    val isSelected = selectedTripIds.contains(trip.id)

                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        shape = RoundedCornerShape(14.dp),
                        border = if (isOverdue) BorderStroke(1.dp, RedDanger.copy(alpha = 0.5f)) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(1.dp, RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (!isPaid) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { checked ->
                                                selectedTripIds = if (checked) selectedTripIds + trip.id else selectedTripIds - trip.id
                                            }
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = trip.driverName.ifBlank { "Motorista não definido" },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = NavyPrimary
                                        )
                                        Text(
                                            text = "${trip.origin} ➔ ${trip.destination}",
                                            fontSize = 12.sp,
                                            color = SlateTextSecondary
                                        )
                                    }
                                }

                                Surface(
                                    color = when {
                                        isPaid -> EmeraldContainer
                                        isOverdue -> Color(0xFFFEE2E2)
                                        else -> Color(0xFFDBEAFE)
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = when {
                                            isPaid -> "PAGA"
                                            isOverdue -> if (daysOverdue == 1) "VENCIDA HÁ 1 DIA" else "VENCIDA HÁ $daysOverdue DIAS"
                                            daysRemaining == 0 -> "VENCE HOJE"
                                            daysRemaining == 1 -> "VENCE AMANHÃ"
                                            else -> "A RECEBER ($daysRemaining D)"
                                        },
                                        color = when {
                                            isPaid -> EmeraldDark
                                            isOverdue -> RedDanger
                                            else -> Color(0xFF1D4ED8)
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Comissão: R$ %.2f".format(trip.driverCommission),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = when {
                                            isPaid -> EmeraldDark
                                            isOverdue -> RedDanger
                                            else -> Color(0xFF0284C7)
                                        }
                                    )
                                    Text(
                                        text = when {
                                            isPaid -> "Paga em: ${formatDate(trip.paidAt ?: trip.completedAt ?: trip.scheduledTime)}"
                                            isOverdue -> "⚠️ Venceu em: ${formatDate(calculatedDue)} (em atraso há $daysOverdue dia(s))"
                                            else -> "⏳ Vencimento: ${formatDate(calculatedDue)} (faltam $daysRemaining dias)"
                                        },
                                        fontSize = 11.sp,
                                        color = if (isOverdue) RedDanger else SlateTextSecondary
                                    )
                                }

                                if (!isPaid) {
                                    Button(
                                        onClick = {
                                            if (isOverdue) {
                                                viewModel.markCommissionPaid(trip)
                                            } else {
                                                tripToForcePay = trip
                                            }
                                        },
                                        colors = if (isOverdue) executiveButtonPrimaryColors() else executiveButtonDarkColors(containerColor = NavySecondary),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(if (isOverdue) "Pagar Vencida" else "Pagar (No Prazo)", fontSize = 12.sp, color = Color.White)
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { tripToUndo = trip },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text("Desfazer", fontSize = 11.sp, color = RedDanger)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (tripToForcePay != null) {
        AlertDialog(
            onDismissRequest = { tripToForcePay = null },
            title = { Text("Forçar Pagamento de Comissão?") },
            text = {
                Text(
                    "Esta comissão vence em ${formatDate(tripToForcePay?.commissionAvailableAt)}. " +
                            "O prazo da empresa ainda não expirou. Deseja efetuar o pagamento antecipado agora?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        tripToForcePay?.let { viewModel.markCommissionPaid(it, force = true) }
                        tripToForcePay = null
                    },
                    colors = executiveButtonDarkColors()
                ) {
                    Text("Confirmar Pagamento", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { tripToForcePay = null }) { Text("Cancelar") }
            }
        )
    }

    if (tripToUndo != null) {
        AlertDialog(
            onDismissRequest = { tripToUndo = null },
            title = { Text("Desfazer Pagamento de Comissão?") },
            text = { Text("A comissão de R$ %.2f de ${tripToUndo?.driverName} voltará a constar como PENDENTE.".format(tripToUndo?.driverCommission ?: 0.0)) },
            confirmButton = {
                Button(
                    onClick = {
                        tripToUndo?.let { viewModel.undoCommissionPaid(it) }
                        tripToUndo = null
                    },
                    colors = executiveButtonDangerColors()
                ) {
                    Text("Sim, Desfazer", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { tripToUndo = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun FilterBadge(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) NavyPrimary else Color(0xFFE2E8F0),
        contentColor = if (isSelected) Color.White else NavyPrimary
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else NavyPrimary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun FinancialMetricCard(
    title: String,
    amount: Double,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.shadow(1.dp, RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SlateTextSecondary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "R$ %.2f".format(amount),
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NavyPrimary
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = SlateTextSecondary
            )
        }
    }
}

private fun formatDate(millis: Long?): String {
    if (millis == null || millis <= 0) return "Imediato"
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return sdf.format(Date(millis))
}
