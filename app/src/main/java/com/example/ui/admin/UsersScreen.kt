package com.example.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.CompanyEntity
import com.example.data.entity.DriverEntity
import com.example.data.entity.UserEntity
import com.example.data.entity.UserRole
import com.example.ui.MainViewModel
import com.example.ui.components.RoleBadge
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val users by viewModel.users.collectAsState()
    val companies by viewModel.companies.collectAsState()
    val drivers by viewModel.drivers.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedRoleFilter by remember { mutableStateOf<UserRole?>(null) }
    var userToEdit by remember { mutableStateOf<UserEntity?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var userToDelete by remember { mutableStateOf<UserEntity?>(null) }

    val filteredUsers = users.filter { user ->
        val matchesQuery = searchQuery.isBlank() ||
                user.name.contains(searchQuery, ignoreCase = true) ||
                user.email.contains(searchQuery, ignoreCase = true) ||
                user.phone.contains(searchQuery, ignoreCase = true) ||
                (user.jobTitle?.contains(searchQuery, ignoreCase = true) == true)

        val matchesRole = selectedRoleFilter == null || user.role == selectedRoleFilter?.name

        matchesQuery && matchesRole
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = EmeraldAccent,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text("Novo Usuário", fontWeight = FontWeight.Bold) }
            )
        },
        containerColor = SlateLight
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Screen Title & Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Gestão de Usuários",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                    Text(
                        text = "${users.size} usuário(s) cadastrado(s) no sistema",
                        fontSize = 13.sp,
                        color = SlateTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar por nome, e-mail, telefone...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SlateTextSecondary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpar busca")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SlateCard,
                    unfocusedContainerColor = SlateCard,
                    unfocusedBorderColor = Color(0xFFE2E8F0)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Role Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedRoleFilter == null,
                        onClick = { selectedRoleFilter = null },
                        label = { Text("Todos (${users.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NavyPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                item {
                    val count = users.count { it.role == UserRole.SUPER_ADMIN.name }
                    FilterChip(
                        selected = selectedRoleFilter == UserRole.SUPER_ADMIN,
                        onClick = { selectedRoleFilter = UserRole.SUPER_ADMIN },
                        label = { Text("Super Admin ($count)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NavySecondary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                item {
                    val count = users.count { it.role == UserRole.COMPANY_ADMIN.name }
                    FilterChip(
                        selected = selectedRoleFilter == UserRole.COMPANY_ADMIN,
                        onClick = { selectedRoleFilter = UserRole.COMPANY_ADMIN },
                        label = { Text("Admin Empresa ($count)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0284C7),
                            selectedLabelColor = Color.White
                        )
                    )
                }
                item {
                    val count = users.count { it.role == UserRole.DRIVER.name }
                    FilterChip(
                        selected = selectedRoleFilter == UserRole.DRIVER,
                        onClick = { selectedRoleFilter = UserRole.DRIVER },
                        label = { Text("Motoristas ($count)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldAccent,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                item {
                    val count = users.count { it.role == UserRole.COMPANY_USER.name }
                    FilterChip(
                        selected = selectedRoleFilter == UserRole.COMPANY_USER,
                        onClick = { selectedRoleFilter = UserRole.COMPANY_USER },
                        label = { Text("Solicitantes ($count)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF64748B),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (filteredUsers.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateCard)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.PeopleOutline,
                            contentDescription = null,
                            tint = SlateTextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (users.isEmpty()) "Nenhum usuário cadastrado" else "Nenhum usuário encontrado",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = NavyPrimary
                        )
                        Text(
                            text = if (users.isEmpty())
                                "Toque no botão abaixo para adicionar o primeiro usuário ao sistema."
                            else
                                "Tente ajustar os filtros ou termo de busca.",
                            fontSize = 13.sp,
                            color = SlateTextSecondary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredUsers, key = { it.id }) { user ->
                        val company = companies.firstOrNull { it.id == user.companyId }
                        val driver = drivers.firstOrNull { it.id == user.driverId }

                        UserCardItem(
                            user = user,
                            company = company,
                            driver = driver,
                            onToggleActive = { viewModel.toggleUserStatus(user) },
                            onEdit = { userToEdit = user },
                            onDelete = { userToDelete = user }
                        )
                    }
                }
            }
        }
    }

    // Dialog Create / Edit
    if (showCreateDialog || userToEdit != null) {
        UserEditDialog(
            user = userToEdit,
            companies = companies,
            drivers = drivers,
            onDismiss = {
                showCreateDialog = false
                userToEdit = null
            },
            onSave = { id, name, email, phone, role, compId, jobTitle, driverId, pass, isActive ->
                viewModel.saveUser(
                    id = id,
                    name = name,
                    email = email,
                    phone = phone,
                    role = role,
                    companyId = compId,
                    jobTitle = jobTitle,
                    driverId = driverId,
                    password = pass,
                    isActive = isActive
                )
                showCreateDialog = false
                userToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    if (userToDelete != null) {
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text("Excluir Usuário") },
            text = { Text("Tem certeza que deseja excluir '${userToDelete?.name}'? Esta ação não pode ser desfeita.") },
            confirmButton = {
                Button(
                    onClick = {
                        userToDelete?.let { viewModel.deleteUser(it) }
                        userToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedDanger)
                ) {
                    Text("Excluir", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun UserCardItem(
    user: UserEntity,
    company: CompanyEntity?,
    driver: DriverEntity?,
    onToggleActive: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (user.isActive) SlateCard else Color(0xFFF1F5F9)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Role Icon
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            when (user.role) {
                                UserRole.SUPER_ADMIN.name -> NavySecondary
                                UserRole.COMPANY_ADMIN.name -> Color(0xFF0284C7)
                                UserRole.DRIVER.name -> EmeraldAccent
                                else -> Color(0xFF64748B)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (user.role) {
                            UserRole.SUPER_ADMIN.name -> Icons.Default.AdminPanelSettings
                            UserRole.COMPANY_ADMIN.name -> Icons.Default.Business
                            UserRole.DRIVER.name -> Icons.Default.DirectionsCar
                            else -> Icons.Default.Person
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (user.isActive) NavyPrimary else SlateTextSecondary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        RoleBadge(user.role)
                    }

                    if (!user.jobTitle.isNullOrBlank()) {
                        Text(
                            text = user.jobTitle,
                            fontSize = 12.sp,
                            color = SlateTextSecondary
                        )
                    }

                    if (company != null) {
                        Text(
                            text = "Empresa: ${company.name}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0284C7)
                        )
                    }

                    if (driver != null) {
                        Text(
                            text = "Veículo: ${driver.vehicleModel} • Placa ${driver.vehiclePlate}",
                            fontSize = 12.sp,
                            color = EmeraldDark
                        )
                    }
                }

                // Active Switch
                Column(horizontalAlignment = Alignment.End) {
                    Switch(
                        checked = user.isActive,
                        onCheckedChange = { onToggleActive() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = EmeraldAccent
                        )
                    )
                    Text(
                        text = if (user.isActive) "Ativo" else "Inativo",
                        fontSize = 10.sp,
                        color = if (user.isActive) EmeraldDark else RedDanger,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = Color(0xFFE2E8F0))
            Spacer(modifier = Modifier.height(8.dp))

            // Contact info and action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Email, contentDescription = null, tint = SlateTextSecondary, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = user.email, fontSize = 11.sp, color = SlateTextSecondary)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = SlateTextSecondary, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = user.phone, fontSize = 11.sp, color = SlateTextSecondary)
                    }
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = NavySecondary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Excluir", tint = RedDanger, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserEditDialog(
    user: UserEntity?,
    companies: List<CompanyEntity>,
    drivers: List<DriverEntity>,
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        name: String,
        email: String,
        phone: String,
        role: UserRole,
        companyId: Long?,
        jobTitle: String?,
        driverId: Long?,
        password: String,
        isActive: Boolean
    ) -> Unit
) {
    var name by remember { mutableStateOf(user?.name ?: "") }
    var email by remember { mutableStateOf(user?.email ?: "") }
    var phone by remember { mutableStateOf(user?.phone ?: "") }
    var selectedRole by remember {
        mutableStateOf(
            user?.let { u ->
                try {
                    UserRole.valueOf(u.role)
                } catch (e: Exception) {
                    UserRole.COMPANY_USER
                }
            } ?: UserRole.COMPANY_USER
        )
    }
    var selectedCompanyId by remember { mutableStateOf(user?.companyId ?: companies.firstOrNull()?.id) }
    var selectedDriverId by remember { mutableStateOf(user?.driverId ?: drivers.firstOrNull()?.id) }
    var jobTitle by remember { mutableStateOf(user?.jobTitle ?: "") }
    var password by remember { mutableStateOf(user?.password ?: "123456") }
    var isActive by remember { mutableStateOf(user?.isActive ?: true) }

    var companyMenuExpanded by remember { mutableStateOf(false) }
    var roleMenuExpanded by remember { mutableStateOf(false) }
    var driverMenuExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SlateCard)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (user == null) "Novo Usuário" else "Editar Usuário",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )
                Text(
                    text = "Configure os dados e o tipo de acesso no sistema.",
                    fontSize = 12.sp,
                    color = SlateTextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Role Selector
                Text(text = "Tipo de Usuário *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NavyPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                ExposedDropdownMenuBox(
                    expanded = roleMenuExpanded,
                    onExpandedChange = { roleMenuExpanded = !roleMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = when (selectedRole) {
                            UserRole.SUPER_ADMIN -> "1. Super Administrador"
                            UserRole.COMPANY_ADMIN -> "2. Administrador da Empresa"
                            UserRole.DRIVER -> "3. Motorista Executivo"
                            UserRole.COMPANY_USER -> "4. Usuário da Empresa (Solicitante)"
                        },
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleMenuExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = roleMenuExpanded,
                        onDismissRequest = { roleMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("1. Super Administrador (Acesso Total)") },
                            onClick = {
                                selectedRole = UserRole.SUPER_ADMIN
                                roleMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("2. Administrador da Empresa") },
                            onClick = {
                                selectedRole = UserRole.COMPANY_ADMIN
                                roleMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("3. Motorista Executivo") },
                            onClick = {
                                selectedRole = UserRole.DRIVER
                                roleMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("4. Usuário da Empresa (Solicitante)") },
                            onClick = {
                                selectedRole = UserRole.COMPANY_USER
                                roleMenuExpanded = false
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // If Company Admin or User, show Company selector
                if (selectedRole == UserRole.COMPANY_ADMIN || selectedRole == UserRole.COMPANY_USER) {
                    Text(text = "Empresa Vinculada *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NavyPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    if (companies.isEmpty()) {
                        Text(
                            text = "Nenhuma empresa cadastrada. Cadastre uma empresa primeiro no menu Empresas.",
                            fontSize = 11.sp,
                            color = RedDanger
                        )
                    } else {
                        val currentCompany = companies.firstOrNull { it.id == selectedCompanyId } ?: companies.first()
                        ExposedDropdownMenuBox(
                            expanded = companyMenuExpanded,
                            onExpandedChange = { companyMenuExpanded = !companyMenuExpanded }
                        ) {
                            OutlinedTextField(
                                value = currentCompany.name,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = companyMenuExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = companyMenuExpanded,
                                onDismissRequest = { companyMenuExpanded = false }
                            ) {
                                companies.forEach { comp ->
                                    DropdownMenuItem(
                                        text = { Text(comp.name) },
                                        onClick = {
                                            selectedCompanyId = comp.id
                                            companyMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // If Driver, show Driver entity selector (optional linkage)
                if (selectedRole == UserRole.DRIVER && drivers.isNotEmpty()) {
                    Text(text = "Vincular a Cadastro de Motorista (Opcional)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NavyPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    val currentDriver = drivers.firstOrNull { it.id == selectedDriverId }
                    ExposedDropdownMenuBox(
                        expanded = driverMenuExpanded,
                        onExpandedChange = { driverMenuExpanded = !driverMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = currentDriver?.let { "${it.fullName} (${it.vehicleModel})" } ?: "Nenhum vínculo",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = driverMenuExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = driverMenuExpanded,
                            onDismissRequest = { driverMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Nenhum (criar autônomo)") },
                                onClick = {
                                    selectedDriverId = null
                                    driverMenuExpanded = false
                                }
                            )
                            drivers.forEach { d ->
                                DropdownMenuItem(
                                    text = { Text("${d.fullName} • ${d.vehiclePlate}") },
                                    onClick = {
                                        selectedDriverId = d.id
                                        driverMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Nome
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome Completo *") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // E-mail / Login
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("E-mail / Login de Acesso *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Telefone
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Telefone / WhatsApp *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Cargo
                OutlinedTextField(
                    value = jobTitle,
                    onValueChange = { jobTitle = it },
                    label = { Text("Cargo / Função") },
                    placeholder = { Text("Ex: Gerente Financeiro, Diretor, Motorista") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Senha
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Senha de Acesso *") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Status Ativo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Usuário Ativo",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = NavyPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = EmeraldAccent
                        )
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = RedDanger,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = SlateTextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                errorMessage = "O nome é obrigatório."
                                return@Button
                            }
                            if (email.isBlank()) {
                                errorMessage = "O e-mail/login é obrigatório."
                                return@Button
                            }
                            if (phone.isBlank()) {
                                errorMessage = "O telefone é obrigatório."
                                return@Button
                            }
                            if (password.length < 4) {
                                errorMessage = "A senha deve ter pelo menos 4 caracteres."
                                return@Button
                            }
                            val compId = if (selectedRole == UserRole.COMPANY_ADMIN || selectedRole == UserRole.COMPANY_USER) {
                                selectedCompanyId
                            } else null

                            val drvId = if (selectedRole == UserRole.DRIVER) selectedDriverId else null

                            onSave(
                                user?.id ?: 0L,
                                name,
                                email,
                                phone,
                                selectedRole,
                                compId,
                                jobTitle.ifBlank { null },
                                drvId,
                                password,
                                isActive
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent)
                    ) {
                        Text("Salvar Usuário", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
