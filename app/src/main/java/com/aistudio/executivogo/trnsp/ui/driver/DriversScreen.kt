package com.aistudio.executivogo.trnsp.ui.driver

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.executivogo.trnsp.data.AppUser
import com.aistudio.executivogo.trnsp.data.UserRole
import com.aistudio.executivogo.trnsp.navigation.Screen
import com.aistudio.executivogo.trnsp.ui.MainViewModel
import com.aistudio.executivogo.trnsp.ui.components.AdminTopBar
import com.aistudio.executivogo.trnsp.ui.components.ExecutivePullRefreshBox
import com.aistudio.executivogo.trnsp.ui.executiveTextFieldColors
import com.aistudio.executivogo.trnsp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriversScreen(
    viewModel: MainViewModel,
    onNavigate: ((String) -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val drivers by viewModel.drivers.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedDriverToEdit by remember { mutableStateOf<AppUser?>(null) }
    var driverToDelete by remember { mutableStateOf<AppUser?>(null) }
    var deletePasswordInput by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    val filteredDrivers = remember(drivers, searchQuery) {
        if (searchQuery.isBlank()) drivers
        else drivers.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.vehiclePlate.contains(searchQuery, ignoreCase = true) ||
            it.vehicleModel.contains(searchQuery, ignoreCase = true) ||
            it.phone.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            AdminTopBar(
                title = "Motoristas Executivos",
                subtitle = "Gestão de Frota e Comissões",
                currentScreen = Screen.Drivers.route,
                onNavigate = onNavigate,
                onLogout = onLogout,
                onRefresh = { viewModel.refreshUsers() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = EmeraldAccent,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Cadastrar Motorista", tint = Color.White)
            }
        },
        modifier = modifier
    ) { paddingValues ->
        ExecutivePullRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshUsers() },
            modifier = Modifier
                .fillMaxSize()
                .background(SlateLight)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Buscar motorista por nome, placa ou modelo...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NavySecondary) },
                colors = executiveTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Summary bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredDrivers.size} motoristas cadastrados",
                    fontSize = 14.sp,
                    color = SlateTextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(EmeraldLight))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${filteredDrivers.count { it.isOnline }} online",
                        fontSize = 13.sp,
                        color = EmeraldDark,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredDrivers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = SlateTextSecondary,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "Nenhum motorista cadastrado ainda." else "Nenhum resultado para a busca.",
                            color = SlateTextSecondary,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Toque no botão '+' para adicionar um motorista.",
                            color = SlateTextSecondary.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredDrivers, key = { it.id }) { driver ->
                        DriverCard(
                            driver = driver,
                            onEdit = { selectedDriverToEdit = driver },
                            onDelete = {
                                driverToDelete = driver
                                deletePasswordInput = ""
                            }
                        )
                    }
                }
            }
        }
    }
    }

    if (showAddDialog) {
        DriverDialog(
            driver = null,
            onDismiss = { showAddDialog = false },
            onSave = { newDriver, password ->
                viewModel.saveUser(
                    user = newDriver,
                    password = password,
                    onComplete = { showAddDialog = false }
                )
            }
        )
    }

    if (selectedDriverToEdit != null) {
        DriverDialog(
            driver = selectedDriverToEdit,
            onDismiss = { selectedDriverToEdit = null },
            onSave = { updatedDriver, _ ->
                viewModel.saveUser(
                    user = updatedDriver,
                    password = null,
                    onComplete = { selectedDriverToEdit = null }
                )
            }
        )
    }

    if (driverToDelete != null) {
        val targetDriver = driverToDelete!!
        AlertDialog(
            onDismissRequest = { driverToDelete = null },
            title = {
                Text(
                    text = "Excluir Motorista",
                    fontWeight = FontWeight.Bold,
                    color = RedDanger
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Tem certeza que deseja excluir o motorista ${targetDriver.name}?",
                        fontWeight = FontWeight.SemiBold,
                        color = NavyPrimary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "• Remove os dados cadastrais do banco de dados.\n• Exclui a conta no Firebase Authentication, liberando o e-mail (${targetDriver.email}) para novos cadastros.",
                        fontSize = 12.sp,
                        color = SlateTextSecondary,
                        lineHeight = 18.sp
                    )
                    if (targetDriver.authKey.isBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = deletePasswordInput,
                            onValueChange = { deletePasswordInput = it },
                            label = { Text("Senha do motorista (opcional se customizada)") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = executiveTextFieldColors()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pass = deletePasswordInput.ifBlank { null }
                        viewModel.deleteUser(targetDriver.id, explicitPassword = pass)
                        driverToDelete = null
                    },
                    colors = executiveButtonDangerColors()
                ) {
                    Text("Excluir Definitivamente", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { driverToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun DriverCard(
    driver: AppUser,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SlateCard)
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
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (driver.isOnline) EmeraldLight else Color(0xFF94A3B8))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = driver.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                }

                Surface(
                    color = if (driver.isOnline) EmeraldContainer else Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (driver.isOnline) "ONLINE" else "OFFLINE",
                        color = if (driver.isOnline) EmeraldDark else SlateTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Vehicle info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = NavySecondary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${if (driver.vehicleModel.isNotBlank()) driver.vehicleModel else "Modelo não informado"} • Placa: ${driver.vehiclePlate.ifBlank { "N/A" }}",
                    fontSize = 13.sp,
                    color = SlateTextPrimary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Contact & commission
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = SlateTextSecondary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = driver.phone.ifBlank { "Sem tel" },
                        fontSize = 12.sp,
                        color = SlateTextSecondary
                    )
                }
                Text(
                    text = "Comissão: ${driver.commissionPercentage}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EmeraldAccent
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = SlateBorder)
            Spacer(modifier = Modifier.height(6.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = NavySecondary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = RedDanger)
                }
            }
        }
    }
}

@Composable
fun DriverDialog(
    driver: AppUser?,
    onDismiss: () -> Unit,
    onSave: (AppUser, String?) -> Unit
) {
    val isNew = (driver == null || driver.id.isBlank())

    var name by remember { mutableStateOf(driver?.name ?: "") }
    var email by remember { mutableStateOf(driver?.email ?: "") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    var phone by remember { mutableStateOf(driver?.phone ?: "") }
    var vehicleModel by remember { mutableStateOf(driver?.vehicleModel ?: "") }
    var vehiclePlate by remember { mutableStateOf(driver?.vehiclePlate ?: "") }
    var vehicleColor by remember { mutableStateOf(driver?.vehicleColor ?: "") }
    var commission by remember { mutableStateOf(driver?.commissionPercentage?.toString() ?: "20.0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isNew) "Cadastrar Motorista" else "Editar Motorista",
                fontWeight = FontWeight.Bold,
                color = NavyPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        validationError = null
                    },
                    label = { Text("Nome do Motorista *") },
                    colors = executiveTextFieldColors(),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        validationError = null
                    },
                    label = { Text("E-mail * (Login de acesso)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    enabled = isNew,
                    colors = executiveTextFieldColors(),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (isNew) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            validationError = null
                        },
                        label = { Text("Senha Inicial * (mínimo 6 dígitos)") },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = NavySecondary) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Ocultar senha" else "Ver senha",
                                    tint = NavySecondary
                                )
                            }
                        },
                        colors = executiveTextFieldColors(),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            validationError = null
                        },
                        label = { Text("Confirmar Senha *") },
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = NavySecondary) },
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (confirmPasswordVisible) "Ocultar senha" else "Ver senha",
                                    tint = NavySecondary
                                )
                            }
                        },
                        colors = executiveTextFieldColors(),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Telefone / WhatsApp *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = executiveTextFieldColors(),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = vehicleModel,
                        onValueChange = { vehicleModel = it },
                        label = { Text("Modelo do Carro") },
                        colors = executiveTextFieldColors(),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = vehiclePlate,
                        onValueChange = { vehiclePlate = it },
                        label = { Text("Placa") },
                        colors = executiveTextFieldColors(),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = vehicleColor,
                        onValueChange = { vehicleColor = it },
                        label = { Text("Cor") },
                        colors = executiveTextFieldColors(),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = commission,
                        onValueChange = { commission = it },
                        label = { Text("Comissão (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = executiveTextFieldColors(),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                validationError?.let { err ->
                    Text(
                        text = err,
                        color = RedDanger,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        validationError = "O nome do motorista é obrigatório."
                        return@Button
                    }
                    if (email.isBlank() || !email.contains("@")) {
                        validationError = "Informe um e-mail válido para criar o login."
                        return@Button
                    }
                    if (isNew) {
                        if (password.length < 6) {
                            validationError = "A senha inicial deve conter pelo menos 6 dígitos."
                            return@Button
                        }
                        if (password != confirmPassword) {
                            validationError = "As senhas informadas não coincidem."
                            return@Button
                        }
                    }

                    val commVal = commission.toDoubleOrNull() ?: 20.0
                    val updated = (driver ?: AppUser()).copy(
                        name = name.trim(),
                        email = email.trim().lowercase(),
                        phone = phone.trim(),
                        role = UserRole.DRIVER,
                        vehicleModel = vehicleModel.trim(),
                        vehiclePlate = vehiclePlate.trim(),
                        vehicleColor = vehicleColor.trim(),
                        commissionPercentage = commVal,
                        isOnline = if (isNew) true else (driver?.isOnline ?: true),
                        active = true
                    )
                    onSave(updated, if (isNew) password.trim() else null)
                },
                colors = executiveButtonPrimaryColors(),
                enabled = name.isNotBlank()
            ) {
                Text(
                    text = if (isNew) "Cadastrar e Criar Acesso" else "Salvar",
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
