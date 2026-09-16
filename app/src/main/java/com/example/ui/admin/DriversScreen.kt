package com.example.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.DriverEntity
import com.example.ui.MainViewModel
import com.example.ui.components.DriverRadarMapCanvas
import com.example.ui.theme.*
import com.example.util.Formatters

@Composable
fun DriversScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val drivers by viewModel.drivers.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var filterOnlyActive by remember { mutableStateOf(false) }
    var selectedDriverOnMap by remember { mutableStateOf<DriverEntity?>(null) }
    var showDriverDialog by remember { mutableStateOf(false) }
    var driverToEdit by remember { mutableStateOf<DriverEntity?>(null) }

    val filteredDrivers = drivers.filter {
        val matchesSearch = it.fullName.contains(searchQuery, ignoreCase = true) ||
                it.vehiclePlate.contains(searchQuery, ignoreCase = true) ||
                it.vehicleModel.contains(searchQuery, ignoreCase = true)
        val matchesActive = !filterOnlyActive || it.isActive
        matchesSearch && matchesActive
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    driverToEdit = null
                    showDriverDialog = true
                },
                containerColor = EmeraldAccent,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Cadastrar Motorista")
            }
        },
        containerColor = SlateLight
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Fleet Overview Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = NavyPrimary)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Frota de Motoristas Executivos",
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Total: ${drivers.size} motoristas cadastrados",
                                    color = EmeraldContainer,
                                    fontSize = 12.sp
                                )
                            }
                            Surface(
                                color = EmeraldLight,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${drivers.count { it.isActive }} ATIVOS",
                                    color = NavyDark,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Real-Time Fleet Radar Map
            item {
                DriverRadarMapCanvas(
                    drivers = drivers,
                    selectedDriver = selectedDriverOnMap,
                    onSelectDriver = { selectedDriverOnMap = it }
                )
            }

            // Search and Active Filter
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Buscar por nome, placa ou veículo...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SlateCard,
                            unfocusedContainerColor = SlateCard
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = filterOnlyActive,
                        onClick = { filterOnlyActive = !filterOnlyActive },
                        label = { Text("Só Ativos") },
                        leadingIcon = if (filterOnlyActive) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            // Driver Items (Supporting 20+ seamlessly)
            if (filteredDrivers.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateCard)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.DirectionsCarFilled,
                                contentDescription = null,
                                tint = SlateTextSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (searchQuery.isBlank()) "Nenhum motorista cadastrado" else "Nenhum motorista encontrado",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = NavyPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (searchQuery.isBlank())
                                    "Cadastre os motoristas da frota com veículo, placa, telefone e percentual de comissão."
                                else
                                    "Tente outro termo de busca.",
                                fontSize = 13.sp,
                                color = SlateTextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            if (searchQuery.isBlank()) {
                                Spacer(modifier = Modifier.height(18.dp))
                                Button(
                                    onClick = {
                                        driverToEdit = null
                                        showDriverDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Cadastrar Primeiro Motorista", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else {
                items(filteredDrivers) { driver ->
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (driver.isActive) EmeraldContainer else Color(0xFFF1F5F9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsCar,
                                        contentDescription = null,
                                        tint = if (driver.isActive) EmeraldDark else Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = driver.fullName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = NavyPrimary
                                    )
                                    Text(
                                        text = "${driver.vehicleModel} • Placa: ${driver.vehiclePlate}",
                                        fontSize = 12.sp,
                                        color = SlateTextSecondary
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Surface(
                                    color = if (driver.isActive) EmeraldContainer else Color(0xFFFEE2E2),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (driver.isActive) "Ativo" else "Inativo",
                                        color = if (driver.isActive) EmeraldDark else RedDanger,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Comissão: ${driver.commissionPercentage}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NavySecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = SlateTextSecondary, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = driver.phone, fontSize = 11.sp, color = SlateTextPrimary)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (driver.isOnline) EmeraldLight else Color.Gray)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (driver.isOnline) "GPS Online" else "Offline",
                                    fontSize = 11.sp,
                                    color = if (driver.isOnline) EmeraldDark else Color.Gray
                                )
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = SlateBorder)

                        // Bottom Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    Formatters.openInGoogleMaps(
                                        context,
                                        driver.latitude,
                                        driver.longitude,
                                        driver.fullName
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(14.dp), tint = EmeraldAccent)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ver no Google Maps", fontSize = 11.sp, color = EmeraldAccent)
                            }

                            Row {
                                OutlinedButton(
                                    onClick = { viewModel.toggleDriverStatus(driver) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(if (driver.isActive) "Desativar" else "Ativar", fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = {
                                        driverToEdit = driver
                                        showDriverDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Editar", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

    if (showDriverDialog) {
        DriverEditDialog(
            driver = driverToEdit,
            onDismiss = { showDriverDialog = false },
            onSave = { name, phone, plate, model, commission, isActive ->
                viewModel.saveDriver(
                    id = driverToEdit?.id ?: 0L,
                    fullName = name,
                    phone = phone,
                    vehiclePlate = plate,
                    vehicleModel = model,
                    commissionPercentage = commission,
                    isActive = isActive
                )
                showDriverDialog = false
            }
        )
    }
}

@Composable
fun DriverEditDialog(
    driver: DriverEntity?,
    onDismiss: () -> Unit,
    onSave: (fullName: String, phone: String, plate: String, model: String, commission: Double, isActive: Boolean) -> Unit
) {
    var fullName by remember { mutableStateOf(driver?.fullName ?: "") }
    var phone by remember { mutableStateOf(driver?.phone ?: "") }
    var vehiclePlate by remember { mutableStateOf(driver?.vehiclePlate ?: "") }
    var vehicleModel by remember { mutableStateOf(driver?.vehicleModel ?: "") }
    var commissionInput by remember { mutableStateOf((driver?.commissionPercentage ?: 75.0).toString()) }
    var isActive by remember { mutableStateOf(driver?.isActive ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (driver == null) "Novo Motorista Executivo" else "Editar Motorista", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Nome Completo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Telefone / WhatsApp") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = vehicleModel,
                        onValueChange = { vehicleModel = it },
                        label = { Text("Modelo do Veículo") },
                        singleLine = true,
                        modifier = Modifier.weight(1.3f)
                    )
                    OutlinedTextField(
                        value = vehiclePlate,
                        onValueChange = { vehiclePlate = it },
                        label = { Text("Placa") },
                        singleLine = true,
                        modifier = Modifier.weight(0.7f)
                    )
                }
                OutlinedTextField(
                    value = commissionInput,
                    onValueChange = { commissionInput = it },
                    label = { Text("Porcentagem de Comissão (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Motorista Ativo na Plataforma")
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fullName.isNotBlank() && phone.isNotBlank()) {
                        val commission = commissionInput.toDoubleOrNull() ?: 75.0
                        onSave(fullName, phone, vehiclePlate, vehicleModel, commission, isActive)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
