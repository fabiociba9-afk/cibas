package com.aistudio.executivogo.trnsp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.aistudio.executivogo.trnsp.data.Company
import com.aistudio.executivogo.trnsp.data.Passenger
import com.aistudio.executivogo.trnsp.navigation.Screen
import com.aistudio.executivogo.trnsp.ui.components.AdminTopBar
import com.aistudio.executivogo.trnsp.ui.components.ExecutivePullRefreshBox
import com.aistudio.executivogo.trnsp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengersScreen(
    viewModel: MainViewModel,
    onNavigate: ((String) -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val passengersList by viewModel.passengers.collectAsState()
    val companiesList by viewModel.companies.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var selectedPassengerToEdit by remember { mutableStateOf<Passenger?>(null) }
    var passengerToDelete by remember { mutableStateOf<Passenger?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredPassengers = remember(passengersList, searchQuery) {
        if (searchQuery.isBlank()) passengersList
        else passengersList.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.phone.contains(searchQuery, ignoreCase = true) ||
                    it.email.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            AdminTopBar(
                title = "Passageiros Corporativos",
                subtitle = "Cadastro e Gestão de Passageiros",
                currentScreen = Screen.Passengers.route,
                onNavigate = onNavigate,
                onLogout = onLogout,
                onRefresh = { viewModel.refreshPassengers() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedPassengerToEdit = null
                    showDialog = true
                },
                containerColor = EmeraldAccent,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Passageiro", tint = Color.White)
            }
        },
        containerColor = SlateLight
    ) { innerPadding ->
        ExecutivePullRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshPassengers() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Buscar passageiro por nome ou telefone...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NavySecondary) },
                    colors = executiveTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (filteredPassengers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) "Nenhum passageiro cadastrado na nuvem." else "Nenhum resultado encontrado.",
                            color = SlateTextSecondary,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredPassengers, key = { it.id }) { passenger ->
                            val companyName = companiesList.find { it.id == passenger.companyId }?.name ?: "Avulso / Não informada"
                            PassengerCard(
                                passenger = passenger,
                                companyName = companyName,
                                onEdit = {
                                    selectedPassengerToEdit = passenger
                                    showDialog = true
                                },
                                onDelete = { passengerToDelete = passenger }
                            )
                        }
                    }
                }
            }
        }

        if (showDialog) {
            PassengerDialog(
                passenger = selectedPassengerToEdit,
                companies = companiesList,
                onDismiss = { showDialog = false },
                onConfirm = { savedPassenger ->
                    viewModel.savePassenger(savedPassenger)
                    showDialog = false
                }
            )
        }

        if (passengerToDelete != null) {
            AlertDialog(
                onDismissRequest = { passengerToDelete = null },
                title = { Text("Excluir Passageiro?") },
                text = { Text("Deseja realmente remover ${passengerToDelete?.name} do Firestore?") },
                confirmButton = {
                    Button(
                        onClick = {
                            passengerToDelete?.let { viewModel.deletePassenger(it.id) }
                            passengerToDelete = null
                        },
                        colors = executiveButtonDangerColors()
                    ) {
                        Text("Sim, Excluir", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { passengerToDelete = null }) { Text("Cancelar") }
                }
            )
        }
    }
}

@Composable
fun PassengerCard(
    passenger: Passenger,
    companyName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SlateCard)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        color = EmeraldAccent.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = EmeraldDark, modifier = Modifier.size(22.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = passenger.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NavyPrimary)
                        Text(text = "Empresa: $companyName", fontSize = 12.sp, color = SlateTextSecondary)
                    }
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = NavySecondary, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = RedDanger, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(text = "Telefone: ${passenger.phone.ifBlank { "Não informado" }}", fontSize = 12.sp, color = NavyPrimary)
            if (passenger.email.isNotBlank()) {
                Text(text = "E-mail: ${passenger.email}", fontSize = 11.sp, color = SlateTextSecondary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengerDialog(
    passenger: Passenger?,
    companies: List<Company>,
    onDismiss: () -> Unit,
    onConfirm: (Passenger) -> Unit
) {
    val isNew = passenger == null

    var name by remember { mutableStateOf(passenger?.name ?: "") }
    var phone by remember { mutableStateOf(passenger?.phone ?: "") }
    var email by remember { mutableStateOf(passenger?.email ?: "") }
    var selectedCompanyId by remember {
        mutableStateOf(passenger?.companyId ?: companies.firstOrNull()?.id ?: "")
    }
    var expandedCompanyDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "Novo Passageiro" else "Editar Passageiro", fontWeight = FontWeight.Bold, color = NavyPrimary) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome Completo *") },
                    colors = executiveTextFieldColors(),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Telefone / WhatsApp") },
                    colors = executiveTextFieldColors(),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("E-mail") },
                    colors = executiveTextFieldColors(),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Dropdown Seleção de Empresa
                ExposedDropdownMenuBox(
                    expanded = expandedCompanyDropdown,
                    onExpandedChange = { expandedCompanyDropdown = !expandedCompanyDropdown }
                ) {
                    val currentCompanyName = companies.find { it.id == selectedCompanyId }?.name ?: "Selecione uma empresa"
                    OutlinedTextField(
                        value = currentCompanyName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Empresa Vinculada") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCompanyDropdown) },
                        colors = executiveTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCompanyDropdown,
                        onDismissRequest = { expandedCompanyDropdown = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        companies.forEach { company ->
                            DropdownMenuItem(
                                text = { Text(company.name, color = Color(0xFF0F172A)) },
                                onClick = {
                                    selectedCompanyId = company.id
                                    expandedCompanyDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(
                            Passenger(
                                id = passenger?.id ?: "",
                                companyId = selectedCompanyId,
                                name = name.trim(),
                                phone = phone.trim(),
                                email = email.trim(),
                                active = true
                            )
                        )
                    }
                },
                colors = executiveButtonPrimaryColors(),
                enabled = name.isNotBlank()
            ) {
                Text(
                    text = if (isNew) "Cadastrar Passageiro" else "Salvar Alterações",
                    color = Color.White
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
