package com.example.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.UserEntity
import com.example.data.entity.UserRole
import com.example.ui.admin.*
import com.example.ui.auth.InitialSetupScreen
import com.example.ui.auth.LoginScreen
import com.example.ui.company.CompanyAdminScreen
import com.example.ui.components.RoleBadge
import com.example.ui.driver.DriverPortalScreen
import com.example.ui.requester.UserTripRequestScreen
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavRoot(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val userCount by viewModel.userCount.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val selectedTab by viewModel.selectedNavTab.collectAsState()

    var showUserSwitcherMenu by remember { mutableStateOf(false) }
    var showNewTripDialogFromDashboard by remember { mutableStateOf(false) }

    val companies by viewModel.companies.collectAsState()
    val drivers by viewModel.drivers.collectAsState()
    val paymentMethods by viewModel.paymentMethods.collectAsState()
    val users by viewModel.users.collectAsState()

    var showSetupScreen by remember { mutableStateOf(false) }

    // 1. Initial State Loading
    if (userCount == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(NavyDark, NavyPrimary))),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(EmeraldAccent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator(color = EmeraldAccent, strokeWidth = 3.dp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Iniciando ExecutivoGo...",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        return
    }

    // 2. Unauthenticated State -> Show Login Screen by default, with option to go to Super Admin setup or quick role access
    if (currentUser == null) {
        if (showSetupScreen) {
            InitialSetupScreen(
                viewModel = viewModel,
                onSuperAdminCreated = {
                    showSetupScreen = false
                },
                onNavigateToLogin = {
                    showSetupScreen = false
                }
            )
        } else {
            LoginScreen(
                viewModel = viewModel,
                onNavigateToSetup = {
                    showSetupScreen = true
                }
            )
        }
        return
    }

    // 4. Authenticated App Session
    val user = currentUser!!

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "ExecutivoGo",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                RoleBadge(user.role)
                            }
                            Text(
                                text = user.name + (if (user.jobTitle != null) " • ${user.jobTitle}" else ""),
                                fontSize = 11.sp,
                                color = EmeraldContainer
                            )
                        }
                    }
                },
                actions = {
                    // Switch user dropdown amongst registered users in the database
                    if (users.size > 1) {
                        Box {
                            IconButton(onClick = { showUserSwitcherMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = "Alternar Usuário",
                                    tint = Color.White
                                )
                            }
                            DropdownMenu(
                                expanded = showUserSwitcherMenu,
                                onDismissRequest = { showUserSwitcherMenu = false }
                            ) {
                                Text(
                                    text = "Alternar entre Usuários Cadastrados",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyPrimary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                                Divider()
                                users.forEach { registeredUser ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = registeredUser.name,
                                                    fontWeight = if (registeredUser.id == user.id) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 13.sp
                                                )
                                                Text(
                                                    text = "${registeredUser.role} • ${registeredUser.email}",
                                                    fontSize = 10.sp,
                                                    color = SlateTextSecondary
                                                )
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = when (registeredUser.role) {
                                                    UserRole.SUPER_ADMIN.name -> Icons.Default.AdminPanelSettings
                                                    UserRole.COMPANY_ADMIN.name -> Icons.Default.Business
                                                    UserRole.DRIVER.name -> Icons.Default.DirectionsCar
                                                    else -> Icons.Default.Person
                                                },
                                                contentDescription = null,
                                                tint = if (registeredUser.id == user.id) EmeraldAccent else NavySecondary
                                            )
                                        },
                                        onClick = {
                                            try {
                                                viewModel.switchProfile(UserRole.valueOf(registeredUser.role))
                                            } catch (e: Exception) {
                                                // ignore
                                            }
                                            showUserSwitcherMenu = false
                                        }
                                    )
                                }
                                Divider()
                                DropdownMenuItem(
                                    text = { Text("Encerrar Sessão", color = RedDanger) },
                                    leadingIcon = { Icon(Icons.Default.ExitToApp, contentDescription = null, tint = RedDanger) },
                                    onClick = {
                                        viewModel.logout()
                                        showUserSwitcherMenu = false
                                    }
                                )
                            }
                        }
                    }

                    IconButton(onClick = { viewModel.logout() }) {
                        Icon(Icons.Default.Logout, contentDescription = "Sair", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavyPrimary,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            // Super Admin navigation bar
            if (user.role == UserRole.SUPER_ADMIN.name) {
                NavigationBar(
                    containerColor = SlateCard,
                    tonalElevation = 8.dp,
                    modifier = Modifier.shadow(8.dp)
                ) {
                    NavigationBarItem(
                        selected = selectedTab == "dashboard",
                        onClick = { viewModel.setNavTab("dashboard") },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Início") },
                        label = { Text("Início", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldAccent,
                            selectedTextColor = NavyPrimary,
                            indicatorColor = EmeraldContainer
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == "trips",
                        onClick = { viewModel.setNavTab("trips") },
                        icon = { Icon(Icons.Default.Route, contentDescription = "Viagens") },
                        label = { Text("Viagens", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldAccent,
                            selectedTextColor = NavyPrimary,
                            indicatorColor = EmeraldContainer
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == "drivers",
                        onClick = { viewModel.setNavTab("drivers") },
                        icon = { Icon(Icons.Default.DirectionsCar, contentDescription = "Frotas") },
                        label = { Text("Frotas", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldAccent,
                            selectedTextColor = NavyPrimary,
                            indicatorColor = EmeraldContainer
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == "companies",
                        onClick = { viewModel.setNavTab("companies") },
                        icon = { Icon(Icons.Default.Business, contentDescription = "Empresas") },
                        label = { Text("Empresas", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldAccent,
                            selectedTextColor = NavyPrimary,
                            indicatorColor = EmeraldContainer
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == "users",
                        onClick = { viewModel.setNavTab("users") },
                        icon = { Icon(Icons.Default.People, contentDescription = "Usuários") },
                        label = { Text("Usuários", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldAccent,
                            selectedTextColor = NavyPrimary,
                            indicatorColor = EmeraldContainer
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == "financial",
                        onClick = { viewModel.setNavTab("financial") },
                        icon = { Icon(Icons.Default.Paid, contentDescription = "Financeiro") },
                        label = { Text("Financeiro", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldAccent,
                            selectedTextColor = NavyPrimary,
                            indicatorColor = EmeraldContainer
                        )
                    )
                }
            }
        },
        containerColor = SlateLight
    ) { innerPadding ->
        Crossfade(
            targetState = user.role,
            modifier = modifier.padding(innerPadding),
            label = "role_crossfade"
        ) { role ->
            when (role) {
                UserRole.SUPER_ADMIN.name -> {
                    when (selectedTab) {
                        "dashboard" -> AdminDashboardScreen(
                            viewModel = viewModel,
                            onNavigateToTrips = { viewModel.setNavTab("trips") },
                            onNavigateToDrivers = { viewModel.setNavTab("drivers") },
                            onNavigateToFinancial = { viewModel.setNavTab("financial") },
                            onNavigateToCompanies = { viewModel.setNavTab("companies") },
                            onNavigateToUsers = { viewModel.setNavTab("users") },
                            onNavigateToPaymentMethods = { viewModel.setNavTab("payment_methods") },
                            onOpenNewTripDialog = { showNewTripDialogFromDashboard = true }
                        )
                        "trips" -> TripsScreen(viewModel = viewModel)
                        "drivers" -> DriversScreen(viewModel = viewModel)
                        "companies" -> CompaniesScreen(viewModel = viewModel)
                        "users" -> UsersScreen(viewModel = viewModel)
                        "financial" -> FinancialScreen(viewModel = viewModel)
                        "payment_methods" -> PaymentMethodsScreen(viewModel = viewModel)
                        else -> AdminDashboardScreen(
                            viewModel = viewModel,
                            onNavigateToTrips = { viewModel.setNavTab("trips") },
                            onNavigateToDrivers = { viewModel.setNavTab("drivers") },
                            onNavigateToFinancial = { viewModel.setNavTab("financial") },
                            onNavigateToCompanies = { viewModel.setNavTab("companies") },
                            onNavigateToUsers = { viewModel.setNavTab("users") },
                            onNavigateToPaymentMethods = { viewModel.setNavTab("payment_methods") },
                            onOpenNewTripDialog = { showNewTripDialogFromDashboard = true }
                        )
                    }
                }
                UserRole.COMPANY_ADMIN.name -> {
                    CompanyAdminScreen(
                        viewModel = viewModel,
                        currentUser = user
                    )
                }
                UserRole.DRIVER.name -> {
                    DriverPortalScreen(
                        viewModel = viewModel,
                        currentUser = user
                    )
                }
                UserRole.COMPANY_USER.name -> {
                    UserTripRequestScreen(
                        viewModel = viewModel,
                        currentUser = user
                    )
                }
                else -> {
                    AdminDashboardScreen(
                        viewModel = viewModel,
                        onNavigateToTrips = { viewModel.setNavTab("trips") },
                        onNavigateToDrivers = { viewModel.setNavTab("drivers") },
                        onNavigateToFinancial = { viewModel.setNavTab("financial") },
                        onNavigateToCompanies = { viewModel.setNavTab("companies") },
                        onNavigateToUsers = { viewModel.setNavTab("users") },
                        onNavigateToPaymentMethods = { viewModel.setNavTab("payment_methods") },
                        onOpenNewTripDialog = { showNewTripDialogFromDashboard = true }
                    )
                }
            }
        }
    }

    if (showNewTripDialogFromDashboard) {
        TripEditDialog(
            trip = null,
            companies = companies,
            drivers = drivers,
            paymentMethods = paymentMethods,
            users = users,
            onDismiss = { showNewTripDialogFromDashboard = false },
            onSave = { comp, reqName, reqId, origin, dest, price, pm, driver, notes ->
                val now = System.currentTimeMillis()
                val cal = java.util.Calendar.getInstance()
                cal.timeInMillis = now
                cal.add(java.util.Calendar.DAY_OF_MONTH, 15)
                val clientDue = cal.timeInMillis
                cal.timeInMillis = now
                cal.add(java.util.Calendar.DAY_OF_MONTH, 5)
                val driverDue = cal.timeInMillis

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
                showNewTripDialogFromDashboard = false
            }
        )
    }
}
