package com.aistudio.executivogo.trnsp.ui.admin

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.executivogo.trnsp.data.FareBand
import com.aistudio.executivogo.trnsp.navigation.Screen
import com.aistudio.executivogo.trnsp.ui.MainViewModel
import com.aistudio.executivogo.trnsp.ui.components.AdminTopBar
import com.aistudio.executivogo.trnsp.ui.components.ExecutivePullRefreshBox
import com.aistudio.executivogo.trnsp.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FareBandsScreen(
    viewModel: MainViewModel,
    onNavigate: (String) -> Unit = {},
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val fareBands by viewModel.fareBands.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var showAddEditDialog by remember { mutableStateOf(false) }
    var bandToEdit by remember { mutableStateOf<FareBand?>(null) }
    var bandToDelete by remember { mutableStateOf<FareBand?>(null) }

    Scaffold(
        topBar = {
            AdminTopBar(
                title = "Faixas de KM",
                subtitle = "Tabela de multiplicadores R$/km por distância",
                currentScreen = Screen.FareBands.route,
                onNavigate = onNavigate,
                onLogout = onLogout,
                onRefresh = { viewModel.refreshFareBands() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    bandToEdit = null
                    showAddEditDialog = true
                },
                containerColor = EmeraldPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_fare_band_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nova Faixa de KM")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        ExecutivePullRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshFareBands() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (fareBands.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SlateLight)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.LinearScale,
                            contentDescription = null,
                            tint = NavyLight.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Nenhuma faixa de KM cadastrada",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyDark
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Clique no botão '+' para definir faixas de distância e multiplicador R$/km para cálculo automático de rotas.",
                            fontSize = 13.sp,
                            color = SlateDark,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SlateLight)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = NavyDark),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = EmeraldAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Regra de Precificação por KM",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Para rotas em modo POR KM, o preço é calculado como: Distância (km) × Multiplicador da faixa aplicável (menor ordem).",
                                        fontSize = 12.sp,
                                        color = Color(0xFFE2E8F0),
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    items(fareBands, key = { it.id }) { band ->
                        FareBandItemCard(
                            band = band,
                            onEdit = {
                                bandToEdit = band
                                showAddEditDialog = true
                            },
                            onDelete = {
                                bandToDelete = band
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }
    }

    if (showAddEditDialog) {
        FareBandAddEditDialog(
            initialBand = bandToEdit,
            onDismiss = {
                showAddEditDialog = false
                bandToEdit = null
            },
            onSave = { band ->
                viewModel.saveFareBand(
                    band = band,
                    onSuccess = {
                        showAddEditDialog = false
                        bandToEdit = null
                    }
                )
            }
        )
    }

    if (bandToDelete != null) {
        AlertDialog(
            onDismissRequest = { bandToDelete = null },
            title = { Text("Excluir Faixa de KM") },
            text = {
                Text("Deseja realmente excluir a faixa '${bandToDelete?.name}' (${bandToDelete?.minKm} a ${bandToDelete?.maxKm} km)?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        bandToDelete?.id?.let { id ->
                            viewModel.deleteFareBand(id)
                        }
                        bandToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Excluir", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { bandToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun FareBandItemCard(
    band: FareBand,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().testTag("fare_band_card_${band.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (band.active) EmeraldPrimary.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${band.sortOrder}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (band.active) EmeraldPrimary else Color.Gray
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = band.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = NavyDark
                        )
                        Text(
                            text = "${String.format(Locale.getDefault(), "%.1f", band.minKm)} km até ${String.format(Locale.getDefault(), "%.1f", band.maxKm)} km",
                            fontSize = 12.sp,
                            color = SlateDark
                        )
                    }
                }

                Surface(
                    color = if (band.active) EmeraldPrimary.copy(alpha = 0.12f) else Color.Gray.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (band.active) "Ativa" else "Inativa",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (band.active) EmeraldPrimary else Color.Gray,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = SlateLight)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Multiplicador",
                        fontSize = 11.sp,
                        color = SlateDark
                    )
                    Text(
                        text = "R$ ${String.format(Locale.getDefault(), "%.2f", band.multiplier)} / km",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyDark
                    )
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = NavyPrimary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Excluir",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FareBandAddEditDialog(
    initialBand: FareBand?,
    onDismiss: () -> Unit,
    onSave: (FareBand) -> Unit
) {
    var name by remember { mutableStateOf(initialBand?.name ?: "") }
    var minKmStr by remember { mutableStateOf(if (initialBand != null) initialBand.minKm.toString() else "0.0") }
    var maxKmStr by remember { mutableStateOf(if (initialBand != null) initialBand.maxKm.toString() else "10.0") }
    var multiplierStr by remember { mutableStateOf(if (initialBand != null) initialBand.multiplier.toString() else "5.50") }
    var sortOrderStr by remember { mutableStateOf(if (initialBand != null) initialBand.sortOrder.toString() else "1") }
    var active by remember { mutableStateOf(initialBand?.active ?: true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialBand == null) "Nova Faixa de KM" else "Editar Faixa de KM")
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
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome da Faixa (ex: Até 10km)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = minKmStr,
                        onValueChange = { minKmStr = it },
                        label = { Text("KM Mín") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = maxKmStr,
                        onValueChange = { maxKmStr = it },
                        label = { Text("KM Máx") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = multiplierStr,
                        onValueChange = { multiplierStr = it },
                        label = { Text("R$/km") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sortOrderStr,
                        onValueChange = { sortOrderStr = it },
                        label = { Text("Ordem") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Faixa Ativa", fontSize = 14.sp)
                    Switch(checked = active, onCheckedChange = { active = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val minKm = minKmStr.replace(",", ".").toDoubleOrNull()
                    val maxKm = maxKmStr.replace(",", ".").toDoubleOrNull()
                    val multiplier = multiplierStr.replace(",", ".").toDoubleOrNull()
                    val sortOrder = sortOrderStr.toIntOrNull() ?: 0

                    if (name.isBlank()) {
                        errorMessage = "Informe o nome da faixa."
                        return@Button
                    }
                    if (minKm == null || maxKm == null || maxKm <= minKm) {
                        errorMessage = "KM Máx deve ser maior que KM Mín."
                        return@Button
                    }
                    if (multiplier == null || multiplier <= 0.0) {
                        errorMessage = "Multiplicador (R$/km) deve ser maior que zero."
                        return@Button
                    }

                    val toSave = (initialBand ?: FareBand()).copy(
                        name = name.trim(),
                        minKm = minKm,
                        maxKm = maxKm,
                        multiplier = multiplier,
                        sortOrder = sortOrder,
                        active = active
                    )
                    onSave(toSave)
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
