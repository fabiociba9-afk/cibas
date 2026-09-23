package com.aistudio.executivogo.trnsp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aistudio.executivogo.trnsp.data.AppUser
import com.aistudio.executivogo.trnsp.navigation.Screen
import com.aistudio.executivogo.trnsp.ui.components.AdminTopBar
import com.aistudio.executivogo.trnsp.ui.components.ExecutivePullRefreshBox
import com.aistudio.executivogo.trnsp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreen(
    viewModel: MainViewModel,
    onNavigate: ((String) -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val usersList by viewModel.users.collectAsState()
    val companiesList by viewModel.companies.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedUserToEdit by remember { mutableStateOf<AppUser?>(null) }
    var userToDelete by remember { mutableStateOf<AppUser?>(null) }
    var deletePasswordInput by remember { mutableStateOf("") }
    var selectedRoleFilter by remember { mutableStateOf("TODOS") }

    val filteredUsers = remember(usersList, selectedRoleFilter) {
        if (selectedRoleFilter == "TODOS") usersList
        else usersList.filter { it.role.equals(selectedRoleFilter, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            AdminTopBar(
                title = "Gestão de Usuários",
                subtitle = "Controle de Acessos do Sistema",
                currentScreen = Screen.Users.route,
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
                Icon(Icons.Default.Add, contentDescription = "Adicionar Novo", tint = Color.White)
            }
        },
        containerColor = SlateLight
    ) { innerPadding ->
        ExecutivePullRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshUsers() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("TODOS", "ADMIN", "DRIVER", "COMPANY", "OPERATOR").forEach { r ->
                        val label = when (r) {
                            "TODOS" -> "Todos"
                            "ADMIN" -> "Admin"
                            "DRIVER" -> "Motoristas"
                            "COMPANY" -> "Empresas"
                            "OPERATOR" -> "Operadores"
                            else -> r
                        }
                        val isSelected = selectedRoleFilter == r
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedRoleFilter = r },
                            label = {
                                Text(
                                    label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else NavyPrimary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.White,
                                selectedContainerColor = NavyPrimary
                            ),
                            border = BorderStroke(1.dp, if (isSelected) NavyPrimary else Color(0xFFCBD5E1))
                        )
                    }
                }

                if (filteredUsers.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nenhum cadastro encontrado para o filtro selecionado.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = SlateTextSecondary
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredUsers, key = { it.id }) { user ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = SlateCard),
                                border = BorderStroke(1.dp, SlateBorder),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    // Top Row: Avatar + Name / Email + Role Pill
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when (user.role.uppercase()) {
                                                        "COMPANY" -> NavyPrimary.copy(alpha = 0.12f)
                                                        "DRIVER" -> EmeraldContainer
                                                        "ADMIN" -> RedDanger.copy(alpha = 0.12f)
                                                        else -> SlateBorder
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = when (user.role.uppercase()) {
                                                    "COMPANY" -> Icons.Default.Business
                                                    "DRIVER" -> Icons.Default.DirectionsCar
                                                    "ADMIN" -> Icons.Default.AdminPanelSettings
                                                    else -> Icons.Default.Person
                                                },
                                                contentDescription = null,
                                                modifier = Modifier.size(22.dp),
                                                tint = when (user.role.uppercase()) {
                                                    "COMPANY" -> NavyPrimary
                                                    "DRIVER" -> EmeraldDark
                                                    "ADMIN" -> RedDanger
                                                    else -> SlateTextPrimary
                                                }
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = user.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = user.email,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Surface(
                                            color = when (user.role.uppercase()) {
                                                "COMPANY" -> Color(0xFFE0E7FF)
                                                "DRIVER" -> EmeraldContainer
                                                "ADMIN" -> Color(0xFFFEE2E2)
                                                else -> SlateBorder
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = when (user.role.uppercase()) {
                                                    "COMPANY" -> "EMPRESA"
                                                    "DRIVER" -> "MOTORISTA"
                                                    "ADMIN" -> "ADMIN"
                                                    "OPERATOR" -> "OPERADOR"
                                                    else -> user.role.uppercase()
                                                },
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when (user.role.uppercase()) {
                                                    "COMPANY" -> NavyPrimary
                                                    "DRIVER" -> EmeraldDark
                                                    "ADMIN" -> RedDanger
                                                    else -> SlateTextPrimary
                                                },
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }

                                    // Phone if present
                                    if (user.phone.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Phone,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = SlateTextSecondary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = user.phone,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = SlateTextSecondary
                                            )
                                        }
                                    }

                                    // COMPANY specific details
                                    if (user.role.equals("COMPANY", ignoreCase = true)) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        if (user.companyId.isBlank()) {
                                            Surface(
                                                color = RedDanger.copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(6.dp),
                                                border = BorderStroke(1.dp, RedDanger.copy(alpha = 0.5f))
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Warning,
                                                        contentDescription = null,
                                                        tint = RedDanger,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "Sem empresa vinculada (Editar para vincular)",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = RedDanger
                                                    )
                                                }
                                            }
                                        } else {
                                            val resolvedName = user.companyName.ifBlank {
                                                companiesList.find { it.id == user.companyId }?.name ?: "Vinculada"
                                            }
                                            Surface(
                                                color = NavyPrimary.copy(alpha = 0.08f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Business,
                                                        contentDescription = null,
                                                        tint = NavyPrimary,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "Empresa: $resolvedName",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = NavyPrimary
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // DRIVER specific details
                                    if (user.role.equals("DRIVER", ignoreCase = true)) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = if (user.isOnline) "🟢 ONLINE" else "⚪ OFFLINE",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (user.isOnline) EmeraldDark else SlateTextSecondary
                                            )
                                            Text(
                                                text = "Comissão: ${user.commissionPercentage.toInt()}%",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        if (user.vehicleModel.isNotBlank() || user.vehiclePlate.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Veículo: ${user.vehicleModel} ${if (user.vehicleColor.isNotBlank()) "(${user.vehicleColor})" else ""} ${if (user.vehicleYear.isNotBlank()) "- ${user.vehicleYear}" else ""}".trim(),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            if (user.vehiclePlate.isNotBlank()) {
                                                Text(
                                                    text = "Placa: ${user.vehiclePlate}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = SlateBorder.copy(alpha = 0.6f))
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Action buttons with fixed height and no text wrapping
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedButton(
                                            onClick = { selectedUserToEdit = user },
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Editar",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        OutlinedButton(
                                            onClick = {
                                                userToDelete = user
                                                deletePasswordInput = ""
                                            },
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = RedDanger
                                            ),
                                            border = BorderStroke(1.dp, RedDanger.copy(alpha = 0.5f)),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp),
                                                tint = RedDanger
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Excluir",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = RedDanger,
                                                maxLines = 1,
                                                softWrap = false
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

        if (showAddDialog) {
            AddUserWithVehicleDialog(
                user = null,
                companies = companiesList,
                onDismiss = { showAddDialog = false },
                onConfirm = { newUser, password ->
                    viewModel.saveUser(
                        user = newUser,
                        password = password,
                        onComplete = { showAddDialog = false }
                    )
                }
            )
        }

        if (selectedUserToEdit != null) {
            AddUserWithVehicleDialog(
                user = selectedUserToEdit,
                companies = companiesList,
                onDismiss = { selectedUserToEdit = null },
                onConfirm = { updatedUser, _ ->
                    viewModel.saveUser(
                        user = updatedUser,
                        password = null,
                        onComplete = { selectedUserToEdit = null }
                    )
                }
            )
        }

        if (userToDelete != null) {
            val targetUser = userToDelete!!
            AlertDialog(
                onDismissRequest = { userToDelete = null },
                title = {
                    Text(
                        text = "Excluir Usuário",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Tem certeza que deseja excluir ${targetUser.name} (${targetUser.role})?",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "• Remove os dados do banco de dados (Firestore).\n• Exclui a conta de login no Firebase Authentication, liberando o e-mail (${targetUser.email}) para novos cadastros.",
                            fontSize = 12.sp,
                            color = SlateTextSecondary,
                            lineHeight = 18.sp
                        )
                        if (targetUser.authKey.isBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = deletePasswordInput,
                                onValueChange = { deletePasswordInput = it },
                                label = { Text("Senha do usuário (opcional se customizada)") },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val pass = deletePasswordInput.ifBlank { null }
                            viewModel.deleteUser(targetUser.id, explicitPassword = pass)
                            userToDelete = null
                        },
                        colors = executiveButtonDangerColors(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Excluir",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { userToDelete = null },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Cancelar",
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUserWithVehicleDialog(
    user: AppUser? = null,
    companies: List<com.aistudio.executivogo.trnsp.data.Company> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (AppUser, String?) -> Unit
) {
    val isNewUser = (user == null || user.id.isBlank())

    var name by remember { mutableStateOf(user?.name ?: "") }
    var email by remember { mutableStateOf(user?.email ?: "") }
    var phone by remember { mutableStateOf(user?.phone ?: "") }
    var role by remember { mutableStateOf(user?.role ?: "DRIVER") }
    var selectedCompanyId by remember { mutableStateOf(user?.companyId ?: "") }
    val initialCompName = remember(user, companies) {
        user?.companyName?.ifBlank { null }
            ?: companies.find { it.id == user?.companyId }?.name
            ?: ""
    }
    var selectedCompanyName by remember { mutableStateOf(initialCompName) }
    var companyDropdownExpanded by remember { mutableStateOf(false) }
    
    // Senha inicial (obrigatória para novo usuário)
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    // Campos do veículo
    var vehicleModel by remember { mutableStateOf(user?.vehicleModel ?: "") }
    var vehicleColor by remember { mutableStateOf(user?.vehicleColor ?: "") }
    var vehicleYear by remember { mutableStateOf(user?.vehicleYear ?: "") }
    var vehiclePlate by remember { mutableStateOf(user?.vehiclePlate ?: "") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 520.dp)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 10.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Surface(
                    color = NavyPrimary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldAccent.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isNewUser) Icons.Default.PersonAdd else Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = EmeraldLight,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isNewUser) "Cadastrar Usuário" else "Editar Usuário",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isNewUser) "Crie o perfil e credenciais de acesso" else "Atualize os dados cadastrais",
                                    fontSize = 11.sp,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fechar",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Scrollable Form Body
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            validationError = null
                        },
                        label = { Text("Nome Completo *") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            validationError = null
                        },
                        label = { Text("E-mail *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        enabled = isNewUser,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    if (isNewUser) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                validationError = null
                            },
                            label = { Text("Senha Inicial * (mínimo 6 dígitos)") },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (passwordVisible) "Ocultar senha" else "Ver senha"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
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
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(
                                        imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (confirmPasswordVisible) "Ocultar senha" else "Ver senha"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Telefone / WhatsApp") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Perfil de Acesso",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("DRIVER" to "Motorista", "COMPANY" to "Empresa", "ADMIN" to "Admin", "OPERATOR" to "Operador").forEach { (rKey, rLabel) ->
                            FilterChip(
                                selected = role.equals(rKey, ignoreCase = true),
                                onClick = { role = rKey },
                                label = { Text(rLabel, fontSize = 11.sp, maxLines = 1, softWrap = false) }
                            )
                        }
                    }

                    if (role.equals("COMPANY", ignoreCase = true)) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            text = "Vínculo com Empresa Corporativa *",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        ExposedDropdownMenuBox(
                            expanded = companyDropdownExpanded,
                            onExpandedChange = { companyDropdownExpanded = !companyDropdownExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedCompanyName.ifBlank { "Selecione a empresa vinculada..." },
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = companyDropdownExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = companyDropdownExpanded,
                                onDismissRequest = { companyDropdownExpanded = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                companies.forEach { comp ->
                                    DropdownMenuItem(
                                        text = { Text(comp.name, color = Color(0xFF0F172A)) },
                                        onClick = {
                                            selectedCompanyId = comp.id
                                            selectedCompanyName = comp.name
                                            companyDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (role.equals("DRIVER", ignoreCase = true)) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            text = "Dados do Veículo Executivo",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        OutlinedTextField(
                            value = vehicleModel,
                            onValueChange = { vehicleModel = it },
                            label = { Text("Modelo (ex: Logan, Corolla, Onix Plus)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = vehicleColor,
                            onValueChange = { vehicleColor = it },
                            label = { Text("Cor") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = vehicleYear,
                            onValueChange = { vehicleYear = it },
                            label = { Text("Ano") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = vehiclePlate,
                            onValueChange = { vehiclePlate = it },
                            label = { Text("Placa") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    validationError?.let { err ->
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Sticky Bottom Action Bar with dedicated non-wrapping buttons
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        HorizontalDivider(color = SlateBorder.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "Cancelar",
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            Button(
                                onClick = {
                                    if (name.isBlank()) {
                                        validationError = "O nome é obrigatório."
                                        return@Button
                                    }
                                    if (email.isBlank() || !email.contains("@")) {
                                        validationError = "Informe um e-mail válido."
                                        return@Button
                                    }
                                    if (isNewUser) {
                                        if (password.length < 6) {
                                            validationError = "A senha deve ter no mínimo 6 caracteres."
                                            return@Button
                                        }
                                        if (password != confirmPassword) {
                                            validationError = "As senhas digitadas não coincidem."
                                            return@Button
                                        }
                                    }

                                    if (role.equals("COMPANY", ignoreCase = true) && selectedCompanyId.isBlank()) {
                                        validationError = "Selecione uma empresa vinculada para o perfil Empresa."
                                        return@Button
                                    }

                                    val baseUser = user ?: AppUser()
                                    val userToSave = baseUser.copy(
                                        name = name.trim(),
                                        email = email.trim().lowercase(),
                                        phone = phone.trim(),
                                        role = role.uppercase().trim(),
                                        companyId = if (role.equals("COMPANY", ignoreCase = true)) selectedCompanyId else "",
                                        companyName = if (role.equals("COMPANY", ignoreCase = true)) selectedCompanyName else "",
                                        vehicleModel = vehicleModel.trim(),
                                        vehicleColor = vehicleColor.trim(),
                                        vehicleYear = vehicleYear.trim(),
                                        vehiclePlate = vehiclePlate.trim(),
                                        active = true
                                    )

                                    onConfirm(userToSave, if (isNewUser) password.trim() else null)
                                },
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = executiveButtonPrimaryColors()
                            ) {
                                Text(
                                    text = if (isNewUser) "Salvar Cadastro" else "Salvar Alterações",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}