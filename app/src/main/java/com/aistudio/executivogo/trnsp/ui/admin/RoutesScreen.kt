package com.aistudio.executivogo.trnsp.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.executivogo.trnsp.data.FareBand
import com.aistudio.executivogo.trnsp.data.PricingMode
import com.aistudio.executivogo.trnsp.data.Route
import com.aistudio.executivogo.trnsp.navigation.Screen
import com.aistudio.executivogo.trnsp.ui.MainViewModel
import com.aistudio.executivogo.trnsp.ui.components.AdminTopBar
import com.aistudio.executivogo.trnsp.ui.components.ExecutivePullRefreshBox
import com.aistudio.executivogo.trnsp.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutesScreen(
    viewModel: MainViewModel,
    onNavigate: (String) -> Unit = {},
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val routes by viewModel.routes.collectAsState()
    val fareBands by viewModel.fareBands.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddEditDialog by remember { mutableStateOf(false) }
    var routeToEdit by remember { mutableStateOf<Route?>(null) }
    var routeToDelete by remember { mutableStateOf<Route?>(null) }

    val filteredRoutes = remember(routes, searchQuery) {
        if (searchQuery.isBlank()) routes
        else {
            val q = searchQuery.trim().lowercase()
            routes.filter {
                it.origin.lowercase().contains(q) ||
                        it.destination.lowercase().contains(q) ||
                        it.notes.lowercase().contains(q)
            }
        }
    }

    Scaffold(
        topBar = {
            AdminTopBar(
                title = "Rotas / Tabela de Preços",
                subtitle = "Tabela de precificação fixa e por km",
                currentScreen = Screen.Routes.route,
                onNavigate = onNavigate,
                onLogout = onLogout,
                onRefresh = { viewModel.refreshRoutes() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    routeToEdit = null
                    showAddEditDialog = true
                },
                containerColor = EmeraldPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_route_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nova Rota")
            }
        },
        containerColor = SlateLight,
        modifier = modifier
    ) { innerPadding ->
        ExecutivePullRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshRoutes() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SlateLight)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar por origem ou destino...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SlateDark) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpar busca")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (filteredRoutes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.AltRoute,
                                contentDescription = null,
                                tint = NavyLight.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "Nenhuma rota encontrada" else "Nenhuma rota cadastrada",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyDark
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Cadastre trajetos fixos ou com cálculo dinâmico por KM para uso pela administração e pelas empresas.",
                                fontSize = 13.sp,
                                color = SlateDark,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredRoutes, key = { it.id }) { route ->
                            RouteItemCard(
                                route = route,
                                onEdit = {
                                    routeToEdit = route
                                    showAddEditDialog = true
                                },
                                onDelete = {
                                    routeToDelete = route
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(72.dp)) }
                    }
                }
            }
        }
    }

    if (showAddEditDialog) {
        RouteAddEditDialog(
            initialRoute = routeToEdit,
            fareBands = fareBands,
            onDismiss = {
                showAddEditDialog = false
                routeToEdit = null
            },
            onSave = { route ->
                viewModel.saveRoute(
                    route = route,
                    onSuccess = {
                        showAddEditDialog = false
                        routeToEdit = null
                    }
                )
            }
        )
    }

    if (routeToDelete != null) {
        AlertDialog(
            onDismissRequest = { routeToDelete = null },
            title = { Text("Excluir Rota") },
            text = {
                Text("Deseja realmente excluir a rota '${routeToDelete?.origin} → ${routeToDelete?.destination}'?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        routeToDelete?.id?.let { id ->
                            viewModel.deleteRoute(id)
                        }
                        routeToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Excluir", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { routeToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun RouteItemCard(
    route: Route,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SlateBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().testTag("route_card_${route.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (route.pricingMode == PricingMode.PER_KM) NavyPrimary.copy(alpha = 0.12f) else EmeraldPrimary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (route.pricingMode == PricingMode.PER_KM) "POR KM" else "PREÇO FIXO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (route.pricingMode == PricingMode.PER_KM) NavyPrimary else EmeraldPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (route.distanceKm > 0.0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = SlateLight,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${String.format(Locale.getDefault(), "%.1f", route.distanceKm)} km",
                                fontSize = 11.sp,
                                color = NavyDark,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Surface(
                    color = if (route.active) EmeraldPrimary.copy(alpha = 0.12f) else Color.Gray.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (route.active) "Ativa" else "Inativa",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (route.active) EmeraldPrimary else Color.Gray,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Origem -> Destino
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.TripOrigin,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = route.origin,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = NavyDark
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = CoralAlert,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = route.destination,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = NavyDark
                )
            }

            if (route.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Obs: ${route.notes}",
                    fontSize = 12.sp,
                    color = SlateDark
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = SlateLight)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (route.pricingMode == PricingMode.PER_KM) "Preço Calculado" else "Valor da Rota",
                        fontSize = 11.sp,
                        color = SlateDark
                    )
                    Text(
                        text = "R$ ${String.format(Locale.getDefault(), "%.2f", route.price)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar rota",
                            tint = NavyPrimary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Excluir rota",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RouteAddEditDialog(
    initialRoute: Route?,
    fareBands: List<FareBand>,
    onDismiss: () -> Unit,
    onSave: (Route) -> Unit
) {
    var origin by remember { mutableStateOf(initialRoute?.origin ?: "") }
    var destination by remember { mutableStateOf(initialRoute?.destination ?: "") }
    var pricingMode by remember { mutableStateOf(initialRoute?.pricingMode ?: PricingMode.FIXED) }
    var distanceKmStr by remember { mutableStateOf(if (initialRoute != null && initialRoute.distanceKm > 0) initialRoute.distanceKm.toString() else "") }
    var fixedPriceStr by remember { mutableStateOf(if (initialRoute != null && initialRoute.pricingMode == PricingMode.FIXED) initialRoute.price.toString() else "") }
    var notes by remember { mutableStateOf(initialRoute?.notes ?: "") }
    var active by remember { mutableStateOf(initialRoute?.active ?: true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Cálculo dinâmico para modo PER_KM
    val parsedDistance = distanceKmStr.replace(",", ".").toDoubleOrNull() ?: 0.0
    val matchingBand = remember(parsedDistance, fareBands) {
        if (parsedDistance > 0.0) {
            fareBands.filter { it.active && it.minKm <= parsedDistance && parsedDistance <= it.maxKm }
                .minByOrNull { it.sortOrder }
        } else null
    }
    val calculatedPrice = remember(parsedDistance, matchingBand) {
        if (matchingBand != null) {
            val raw = parsedDistance * matchingBand.multiplier
            kotlin.math.round(raw * 100.0) / 100.0
        } else 0.0
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialRoute == null) "Nova Rota / Preço" else "Editar Rota")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }

                OutlinedTextField(
                    value = origin,
                    onValueChange = { origin = it },
                    label = { Text("Origem (Ex: Aeroporto VCP / Hotel X)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = destination,
                    onValueChange = { destination = it },
                    label = { Text("Destino (Ex: Alphaville / Centro)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Modalidade de Precificação", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NavyDark)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = pricingMode == PricingMode.FIXED,
                        onClick = { pricingMode = PricingMode.FIXED },
                        label = { Text("Preço Fixo") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = pricingMode == PricingMode.PER_KM,
                        onClick = { pricingMode = PricingMode.PER_KM },
                        label = { Text("Por KM") },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (pricingMode == PricingMode.FIXED) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = fixedPriceStr,
                            onValueChange = { fixedPriceStr = it },
                            label = { Text("Preço Fixo (R$)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = distanceKmStr,
                            onValueChange = { distanceKmStr = it },
                            label = { Text("KM (Opcional)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    // PER_KM
                    OutlinedTextField(
                        value = distanceKmStr,
                        onValueChange = { distanceKmStr = it },
                        label = { Text("Distância em KM (obrigatório)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Surface(
                        color = NavyDark,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            if (parsedDistance <= 0.0) {
                                Text(
                                    text = "Informe a distância em KM acima para calcular o valor com base na tabela de faixas.",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            } else if (matchingBand != null) {
                                Text(
                                    text = "Faixa Aplicada: ${matchingBand.name} (R$ ${String.format(Locale.getDefault(), "%.2f", matchingBand.multiplier)}/km)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = EmeraldAccent
                                )
                                Text(
                                    text = "Preço Calculado: R$ ${String.format(Locale.getDefault(), "%.2f", calculatedPrice)}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            } else {
                                Text(
                                    text = "Nenhuma faixa de KM ativa cobre $parsedDistance km. Cadastre a faixa correspondente em Faixas de KM.",
                                    fontSize = 11.sp,
                                    color = CoralAlert
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observações (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Rota Ativa", fontSize = 14.sp)
                    Switch(checked = active, onCheckedChange = { active = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (origin.isBlank() || destination.isBlank()) {
                        errorMessage = "Origem e Destino são obrigatórios."
                        return@Button
                    }

                    val dist = distanceKmStr.replace(",", ".").toDoubleOrNull() ?: 0.0

                    if (pricingMode == PricingMode.PER_KM) {
                        if (dist <= 0.0) {
                            errorMessage = "Informe a distância em KM para o cálculo por km."
                            return@Button
                        }
                        if (matchingBand == null) {
                            errorMessage = "Nenhuma faixa de KM ativa encontrada para $dist km."
                            return@Button
                        }

                        val toSave = (initialRoute ?: Route()).copy(
                            origin = origin.trim(),
                            destination = destination.trim(),
                            pricingMode = PricingMode.PER_KM,
                            distanceKm = dist,
                            price = calculatedPrice,
                            fareBandId = matchingBand.id,
                            multiplierSnapshot = matchingBand.multiplier,
                            notes = notes.trim(),
                            active = active,
                            updatedAt = System.currentTimeMillis()
                        )
                        onSave(toSave)
                    } else {
                        // FIXED
                        val price = fixedPriceStr.replace(",", ".").toDoubleOrNull()
                        if (price == null || price <= 0.0) {
                            errorMessage = "Informe um preço fixo válido maior que zero."
                            return@Button
                        }
                        val toSave = (initialRoute ?: Route()).copy(
                            origin = origin.trim(),
                            destination = destination.trim(),
                            pricingMode = PricingMode.FIXED,
                            distanceKm = dist,
                            price = kotlin.math.round(price * 100.0) / 100.0,
                            fareBandId = "",
                            multiplierSnapshot = 0.0,
                            notes = notes.trim(),
                            active = active,
                            updatedAt = System.currentTimeMillis()
                        )
                        onSave(toSave)
                    }
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
