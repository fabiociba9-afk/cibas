package com.example.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.CompanyEntity
import com.example.data.entity.UserEntity
import com.example.data.entity.UserRole
import com.example.ui.MainViewModel
import com.example.ui.components.RoleBadge
import com.example.ui.theme.*

@Composable
fun CompaniesScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val companies by viewModel.companies.collectAsState()
    val users by viewModel.users.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showCompanyDialog by remember { mutableStateOf(false) }
    var companyToEdit by remember { mutableStateOf<CompanyEntity?>(null) }

    var showUserDialog by remember { mutableStateOf(false) }
    var selectedCompanyForUser by remember { mutableStateOf<CompanyEntity?>(null) }

    val filteredCompanies = companies.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.cnpj.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    companyToEdit = null
                    showCompanyDialog = true
                },
                containerColor = EmeraldAccent,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.AddBusiness, contentDescription = "Cadastrar Empresa")
            }
        },
        containerColor = SlateLight
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Buscar empresas por nome ou CNPJ...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SlateCard,
                    unfocusedContainerColor = SlateCard
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "${filteredCompanies.size} Empresas Cadastradas",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = SlateTextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredCompanies.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateCard)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.BusinessCenter,
                            contentDescription = null,
                            tint = SlateTextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "Nenhuma empresa cadastrada" else "Nenhuma empresa encontrada",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = NavyPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (searchQuery.isBlank())
                                "Cadastre a primeira empresa cliente corporativa para gerenciar colaboradores, centros de custo e viagens."
                            else
                                "Tente buscar por outro termo ou limpe a busca.",
                            fontSize = 13.sp,
                            color = SlateTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        if (searchQuery.isBlank()) {
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = {
                                    companyToEdit = null
                                    showCompanyDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.AddBusiness, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Cadastrar Primeira Empresa", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredCompanies) { company ->
                    val companyUsers = users.filter { it.companyId == company.id }

                    Card(
                        modifier = Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateCard)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = company.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = NavyPrimary
                                    )
                                    Text(
                                        text = "CNPJ: ${company.cnpj}",
                                        fontSize = 12.sp,
                                        color = SlateTextSecondary
                                    )
                                }
                                Surface(
                                    color = if (company.isActive) EmeraldContainer else Color(0xFFFEE2E2),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (company.isActive) "Ativa" else "Inativa",
                                        color = if (company.isActive) EmeraldDark else RedDanger,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = SlateTextSecondary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = company.phone, fontSize = 12.sp, color = SlateTextPrimary)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = SlateTextSecondary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = company.address, fontSize = 12.sp, color = SlateTextSecondary)
                            }

                            Divider(modifier = Modifier.padding(vertical = 10.dp), color = SlateBorder)

                            // Associated Users Section
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Colaboradores (${companyUsers.size}):",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NavyPrimary
                                )
                                TextButton(
                                    onClick = {
                                        selectedCompanyForUser = company
                                        showUserDialog = true
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+ Adicionar Usuário", fontSize = 11.sp)
                                }
                            }

                            if (companyUsers.isEmpty()) {
                                Text("Nenhum usuário cadastrado nesta empresa.", fontSize = 11.sp, color = SlateTextSecondary)
                            } else {
                                companyUsers.forEach { u ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("${u.name} (${u.jobTitle ?: "Colaborador"})", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                            Text("${u.email} • ${u.phone}", fontSize = 11.sp, color = SlateTextSecondary)
                                        }
                                        Switch(
                                            checked = u.isActive,
                                            onCheckedChange = { viewModel.toggleUserStatus(u) },
                                            modifier = Modifier.scale(0.7f)
                                        )
                                    }
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 10.dp), color = SlateBorder)

                            // Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.toggleCompanyStatus(company) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(if (company.isActive) "Desativar" else "Ativar", fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        companyToEdit = company
                                        showCompanyDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
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

    if (showCompanyDialog) {
        CompanyEditDialog(
            company = companyToEdit,
            onDismiss = { showCompanyDialog = false },
            onSave = { name, cnpj, phone, address, isActive ->
                viewModel.saveCompany(
                    id = companyToEdit?.id ?: 0L,
                    name = name,
                    cnpj = cnpj,
                    phone = phone,
                    address = address,
                    isActive = isActive
                )
                showCompanyDialog = false
            }
        )
    }

    if (showUserDialog && selectedCompanyForUser != null) {
        CompanyUserDialog(
            company = selectedCompanyForUser!!,
            onDismiss = { showUserDialog = false },
            onSave = { name, email, phone, role, jobTitle, password ->
                viewModel.saveUser(
                    name = name,
                    email = email,
                    phone = phone,
                    role = role,
                    companyId = selectedCompanyForUser!!.id,
                    jobTitle = jobTitle,
                    password = password
                )
                showUserDialog = false
            }
        )
    }
}

@Composable
fun CompanyEditDialog(
    company: CompanyEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, cnpj: String, phone: String, address: String, isActive: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(company?.name ?: "") }
    var cnpj by remember { mutableStateOf(company?.cnpj ?: "") }
    var phone by remember { mutableStateOf(company?.phone ?: "") }
    var address by remember { mutableStateOf(company?.address ?: "") }
    var isActive by remember { mutableStateOf(company?.isActive ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (company == null) "Nova Empresa" else "Editar Empresa", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome da Empresa / Razão Social") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = cnpj,
                    onValueChange = { cnpj = it },
                    label = { Text("CNPJ") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Telefone Corporativo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Endereço Completo") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Empresa Ativa no Sistema")
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && cnpj.isNotBlank()) {
                        onSave(name, cnpj, phone, address, isActive)
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

@Composable
fun CompanyUserDialog(
    company: CompanyEntity,
    onDismiss: () -> Unit,
    onSave: (name: String, email: String, phone: String, role: UserRole, jobTitle: String, pass: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var jobTitle by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("123456") }
    var isCompanyAdmin by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Novo Usuário - ${company.name}", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome Completo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("E-mail Corporativo") },
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
                OutlinedTextField(
                    value = jobTitle,
                    onValueChange = { jobTitle = it },
                    label = { Text("Cargo (ex: Diretor, Gerente)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Senha de Acesso") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Perfil Administrador da Empresa")
                    Switch(checked = isCompanyAdmin, onCheckedChange = { isCompanyAdmin = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && email.isNotBlank()) {
                        val role = if (isCompanyAdmin) UserRole.COMPANY_ADMIN else UserRole.COMPANY_USER
                        onSave(name, email, phone, role, jobTitle, password)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Text("Cadastrar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
