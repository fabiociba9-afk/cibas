package com.aistudio.executivogo.trnsp.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aistudio.executivogo.trnsp.ui.*
import com.aistudio.executivogo.trnsp.ui.admin.FareBandsScreen
import com.aistudio.executivogo.trnsp.ui.admin.FinancialScreen
import com.aistudio.executivogo.trnsp.ui.admin.LiveMapScreen
import com.aistudio.executivogo.trnsp.ui.admin.RoutesScreen
import com.aistudio.executivogo.trnsp.ui.auth.LoginScreen
import com.aistudio.executivogo.trnsp.ui.company.CompanyPortalScreen
import com.aistudio.executivogo.trnsp.ui.driver.DriverPortalScreen
import com.aistudio.executivogo.trnsp.ui.driver.DriversScreen
import com.aistudio.executivogo.trnsp.ui.trip.TripsScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Dashboard : Screen("dashboard")
    object DriverPortal : Screen("driver_portal")
    object CompanyPortal : Screen("company_portal")
    object Users : Screen("users")
    object Drivers : Screen("drivers")
    object Companies : Screen("companies")
    object Passengers : Screen("passengers")
    object Trips : Screen("trips")
    object LiveMap : Screen("live_map?driverId={driverId}") {
        fun createRoute(driverId: String? = null) = if (driverId != null) "live_map?driverId=$driverId" else "live_map"
    }
    object Financial : Screen("financial")
    object PaymentMethods : Screen("payment_methods")
    object FareBands : Screen("fare_bands")
    object Routes : Screen("routes")
}

@Composable
fun AppNavRoot(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage by viewModel.errorMessage.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val pendingTripId by viewModel.pendingTripId.collectAsState()

    LaunchedEffect(errorMessage) {
        errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearErrorMessage()
        }
    }

    LaunchedEffect(currentUser, pendingTripId) {
        val tripId = pendingTripId
        if (!tripId.isNullOrBlank() && currentUser != null) {
            try {
                val role = currentUser?.role.orEmpty()
                if (role.equals("driver", ignoreCase = true)) {
                    navController.navigate(Screen.DriverPortal.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                } else if (role.equals("company", ignoreCase = true)) {
                    navController.navigate(Screen.CompanyPortal.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                } else if (role.equals("admin", ignoreCase = true)) {
                    navController.navigate(Screen.Trips.route) {
                        popUpTo(Screen.Dashboard.route) { saveState = true }
                        launchSingleTop = true
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AppNavRoot", "Error navigating from pending notification: ${e.message}")
            } finally {
                viewModel.clearPendingNotification()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { contentPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Login.route,
            modifier = Modifier.padding(contentPadding)
        ) {
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = viewModel,
                onNavigateToAdmin = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToDriver = {
                    navController.navigate(Screen.DriverPortal.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToCompany = {
                    navController.navigate(Screen.CompanyPortal.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            AdminDashboardScreen(
                viewModel = viewModel,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(Screen.Dashboard.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToTrips = { navController.navigate(Screen.Trips.route) { popUpTo(Screen.Dashboard.route) { saveState = true }; launchSingleTop = true; restoreState = true } },
                onNavigateToDrivers = { navController.navigate(Screen.Drivers.route) { popUpTo(Screen.Dashboard.route) { saveState = true }; launchSingleTop = true; restoreState = true } },
                onNavigateToUsers = { navController.navigate(Screen.Users.route) { popUpTo(Screen.Dashboard.route) { saveState = true }; launchSingleTop = true; restoreState = true } },
                onNavigateToCompanies = { navController.navigate(Screen.Companies.route) { popUpTo(Screen.Dashboard.route) { saveState = true }; launchSingleTop = true; restoreState = true } },
                onNavigateToPassengers = { navController.navigate(Screen.Passengers.route) { popUpTo(Screen.Dashboard.route) { saveState = true }; launchSingleTop = true; restoreState = true } },
                onNavigateToFinancial = { navController.navigate(Screen.Financial.route) { popUpTo(Screen.Dashboard.route) { saveState = true }; launchSingleTop = true; restoreState = true } },
                onNavigateToPaymentMethods = { navController.navigate(Screen.PaymentMethods.route) { popUpTo(Screen.Dashboard.route) { saveState = true }; launchSingleTop = true; restoreState = true } },
                onNavigateToFareBands = { navController.navigate(Screen.FareBands.route) { popUpTo(Screen.Dashboard.route) { saveState = true }; launchSingleTop = true; restoreState = true } },
                onNavigateToRoutes = { navController.navigate(Screen.Routes.route) { popUpTo(Screen.Dashboard.route) { saveState = true }; launchSingleTop = true; restoreState = true } },
                onNavigateToLiveMap = { navController.navigate(Screen.LiveMap.route) { popUpTo(Screen.Dashboard.route) { saveState = true }; launchSingleTop = true; restoreState = true } },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.DriverPortal.route) {
            DriverPortalScreen(
                viewModel = viewModel,
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.CompanyPortal.route) {
            CompanyPortalScreen(
                viewModel = viewModel,
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        val navigateToAdminScreen: (String) -> Unit = { route ->
            navController.navigate(route) {
                popUpTo(Screen.Dashboard.route) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
        val adminLogoutAction: () -> Unit = {
            viewModel.logout {
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }

        composable(Screen.Trips.route) {
            TripsScreen(
                viewModel = viewModel,
                onNavigate = navigateToAdminScreen,
                onLogout = adminLogoutAction
            )
        }

        composable(
            route = Screen.LiveMap.route,
            arguments = listOf(navArgument("driverId") { nullable = true; defaultValue = null })
        ) { backStackEntry ->
            val driverId = backStackEntry.arguments?.getString("driverId")
            LiveMapScreen(
                viewModel = viewModel,
                initialDriverId = driverId,
                onNavigate = navigateToAdminScreen,
                onLogout = adminLogoutAction
            )
        }

        composable(Screen.Drivers.route) {
            DriversScreen(
                viewModel = viewModel,
                onNavigate = navigateToAdminScreen,
                onLogout = adminLogoutAction
            )
        }

        composable(Screen.Users.route) {
            UsersScreen(
                viewModel = viewModel,
                onNavigate = navigateToAdminScreen,
                onLogout = adminLogoutAction
            )
        }

        composable(Screen.Companies.route) {
            CompaniesScreen(
                viewModel = viewModel,
                onNavigate = navigateToAdminScreen,
                onLogout = adminLogoutAction
            )
        }

        composable(Screen.Passengers.route) {
            PassengersScreen(
                viewModel = viewModel,
                onNavigate = navigateToAdminScreen,
                onLogout = adminLogoutAction
            )
        }

        composable(Screen.Financial.route) {
            FinancialScreen(
                viewModel = viewModel,
                onNavigate = navigateToAdminScreen,
                onLogout = adminLogoutAction
            )
        }

        composable(Screen.PaymentMethods.route) {
            PaymentMethodsScreen(
                viewModel = viewModel,
                onNavigate = navigateToAdminScreen,
                onLogout = adminLogoutAction
            )
        }

        composable(Screen.FareBands.route) {
            FareBandsScreen(
                viewModel = viewModel,
                onNavigate = navigateToAdminScreen,
                onLogout = adminLogoutAction
            )
        }

        composable(Screen.Routes.route) {
            RoutesScreen(
                viewModel = viewModel,
                onNavigate = navigateToAdminScreen,
                onLogout = adminLogoutAction
            )
        }
    }
    }
}
