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
import com.aistudio.executivogo.trnsp.data.PaymentMeans
import com.aistudio.executivogo.trnsp.data.PaymentTerms
import com.aistudio.executivogo.trnsp.navigation.Screen
import com.aistudio.executivogo.trnsp.ui.components.AdminTopBar
import com.aistudio.executivogo.trnsp.ui.components.ExecutivePullRefreshBox
import com.aistudio.executivogo.trnsp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompaniesScreen(
    viewModel: MainViewModel,
    onNavigate: ((String) -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val companiesList by viewModel.companies.collectAsState()
    val usersList by viewModel.users.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var selectedCompanyToEdit by remember { mutableStateOf<Company?>(null) }
    var companyToDelete by remember { mutableStateOf<Company?>(null) }
    var companyForNewUser by remember { mutableStateOf<Company?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredCompanies = remember(companiesList, searchQuery) {
        if (searchQuery.isBlank()) companiesList
        else companiesList.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.fantasyName.contains(searchQuery, ignoreCase = true) ||
                    it.cnpj.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            AdminTopBar(
                title = "Empresas Corporativas",
                subtitle = "Gestão de Clientes e Faturamento",
                currentScreen = Screen.Companies.route,
                onNavigate = onNavigate,
                onLogout = onLogout,
                onRefresh = { viewModel.refreshCompanies() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedCompanyToEdit = null
                    showDialog = true
                },
                containerColor = EmeraldAccent,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Empresa", tint = Color.White)
            }
        },
        containerColor = SlateLight
    ) { innerPadding ->
        ExecutivePullRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshCompanies() },
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
                    label = { Text("Buscar empresa por nome ou CNPJ...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NavySecondary) },
                    colors = executiveTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (filteredCompanies.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) "Nenhuma empresa cadastrada na nuvem." else "Nenhum resultado encontrado.",
                            color = SlateTextSecondary,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredCompanies, key = { it.id }) { company ->
                            val linkedUsers = usersList.filter { it.companyId == company.id }
                            CompanyCard(
                                company = company,
                                linkedUsersCount = linkedUsers.size,
                                onEdit = {
                                    selectedCompanyToEdit = company
                                    showDialog = true
                                },
                                onDelete = { companyToDelete = company },
                                onCreateUser = { companyForNewUser = company }
                            )
                        }
                    }
                }
            }
        }

        if (companyForNewUser != null) {
            CreateCompanyUserDialog(
                company = companyForNewUser!!,
                onDismiss = { companyForNewUser = null },
                onConfirm = { name, email, pass, phone ->
                    viewModel.createCompanyUser(
                        name = name,
                        email = email,
                        password = pass,
                        phone = phone,
                        companyId = companyForNewUser!!.id,
                        companyName = companyForNewUser!!.name,
                        onSuccess = {
                            companyForNewUser = null
                        }
                    )
                }
            )
        }

        if (showDialog) {
            CompanyDialog(
                company = selectedCompanyToEdit,
                onDismiss = { showDialog = false },
                onConfirm = { savedCompany ->
                    viewModel.saveCompany(savedCompany)
                    showDialog = false
                }
            )
        }

        if (companyToDelete != null) {
            AlertDialog(
                onDismissRequest = { companyToDelete = null },
                title = { Text("Excluir Empresa?") },
                text = { Text("Deseja realmente excluir ${companyToDelete?.name}? Essa ação removerá a empresa do Firestore.") },
                confirmButton = {
                    Button(
                        onClick = {
                            companyToDelete?.let { viewModel.deleteCompany(it.id) }
                            companyToDelete = null
                        },
                        colors = executiveButtonDangerColors()
                    ) {
                        Text("Sim, Excluir", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { companyToDelete = null }) { Text("Cancelar") }
                }
            )
        }
    }
}

@Composable
fun CompanyCard(
    company: Company,
    linkedUsersCount: Int = 0,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCreateUser: () -> Unit = {}
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
                        color = NavyPrimary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Business, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(22.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = company.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NavyPrimary)
                        if (company.fantasyName.isNotBlank()) {
                            Text(text = company.fantasyName, fontSize = 12.sp, color = SlateTextSecondary)
                        }
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

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "CNPJ: ${company.cnpj}", fontSize = 12.sp, color = NavyPrimary)
            Text(text = "Contato: ${company.contactPerson} • ${company.phone}", fontSize = 12.sp, color = SlateTextSecondary)
            Text(text = "E-mail: ${company.email}", fontSize = 11.sp, color = SlateTextSecondary)

            Spacer(modifier = Modifier.height(8.dp))

            // Financial Terms Snapshot
            Surface(
                color = Color(0xFFF1F5F9),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Prazo: ${PaymentTerms.getLabel(company.paymentTerm)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NavyPrimary
                    )
                    Text(
                        text = "Meio: ${PaymentMeans.getLabel(company.paymentMeans)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (linkedUsersCount == 0) "Nenhum usuário de acesso" else "$linkedUsersCount usuário(s) corporativo(s)",
                    fontSize = 11.sp,
                    color = if (linkedUsersCount > 0) EmeraldDark else SlateTextSecondary,
                    fontWeight = if (linkedUsersCount > 0) FontWeight.SemiBold else FontWeight.Normal
                )

                OutlinedButton(
                    onClick = onCreateUser,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = NavyPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Criar Acesso", fontSize = 12.sp, color = NavyPrimary)
                }
            }
        }
    }
}

@Composable
fun CreateCompanyUserDialog(
    company: Company,
    onDismiss: () -> Unit,
    onConfirm: (name: String, email: String, pass: String, phone: String) -> Unit
) {
    var name by remember { mutableStateOf(company.contactPerson.ifBlank { "Responsável ${company.name}" }) }
    var email by remember { mutableStateOf(company.email) }
    var password by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf(company.phone) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Criar Login Corporativo", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(company.name, fontSize = 12.sp, color = NavySecondary)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (errorMessage != null) {
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do Usuário") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("E-mail de Login") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Senha de Acesso (mín. 6 dígitos)") },
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
                Surface(
                    color = EmeraldAccent.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Este login terá perfil EMPRESA vinculado exclusivamente a ${company.name} e poderá solicitar viagens e gerenciar passageiros corporativos.",
                        fontSize = 11.sp,
                        color = EmeraldDark,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || email.isBlank() || password.length < 6) {
                        errorMessage = "Preencha nome, e-mail e senha (mínimo 6 caracteres)."
                        return@Button
                    }
                    onConfirm(name.trim(), email.trim(), password.trim(), phone.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Text("Criar Acesso", color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyDialog(
    company: Company?,
    onDismiss: () -> Unit,
    onConfirm: (Company) -> Unit
) {
    val isNew = company == null

    var name by remember { mutableStateOf(company?.name ?: "") }
    var fantasyName by remember { mutableStateOf(company?.fantasyName ?: "") }
    var cnpj by remember { mutableStateOf(company?.cnpj ?: "") }
    var contactPerson by remember { mutableStateOf(company?.contactPerson ?: "") }
    var phone by remember { mutableStateOf(company?.phone ?: "") }
    var email by remember { mutableStateOf(company?.email ?: "") }

    var paymentTerm by remember { mutableStateOf(company?.paymentTerm ?: PaymentTerms.A_VISTA) }
    var paymentMeans by remember { mutableStateOf(company?.paymentMeans ?: PaymentMeans.PIX) }

    var termDropdownExpanded by remember { mutableStateOf(false) }
    var meansDropdownExpanded by remember { mutableStateOf(false) }

    val termsList: List<String> = listOf(
        PaymentTerms.A_VISTA,
        PaymentTerms.D7,
        PaymentTerms.D15,
        PaymentTerms.D30,
        PaymentTerms.D60
    )
    val meansList: List<String> = listOf(
        PaymentMeans.PIX,
        PaymentMeans.DEBITO,
        PaymentMeans.CREDITO,
        PaymentMeans.FATURAMENTO,
        PaymentMeans.DINHEIRO
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Surface(
                color = NavyPrimary,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isNew) Icons.Default.Business else Icons.Default.Edit,
                        contentDescription = null,
                        tint = EmeraldAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        if (isNew) "Nova Empresa" else "Editar Empresa",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 17.sp
                    )
                }
            }
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
                    onValueChange = { name = it },
                    label = { Text("Razão Social *") },
                    colors = executiveTextFieldColors(),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = fantasyName,
                    onValueChange = { fantasyName = it },
                    label = { Text("Nome Fantasia") },
                    colors = executiveTextFieldColors(),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = cnpj,
                    onValueChange = { cnpj = it },
                    label = { Text("CNPJ *") },
                    colors = executiveTextFieldColors(),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = contactPerson,
                    onValueChange = { contactPerson = it },
                    label = { Text("Pessoa de Contato") },
                    colors = executiveTextFieldColors(),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Telefone") },
                    colors = executiveTextFieldColors(),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("E-mail Financeiro") },
                    colors = executiveTextFieldColors(),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Prazo de Pagamento Dropdown
                ExposedDropdownMenuBox(
                    expanded = termDropdownExpanded,
                    onExpandedChange = { termDropdownExpanded = !termDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = PaymentTerms.getLabel(paymentTerm),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Prazo de Pagamento") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = termDropdownExpanded) },
                        colors = executiveTextFieldColors(),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = termDropdownExpanded,
                        onDismissRequest = { termDropdownExpanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        termsList.forEach { term ->
                            DropdownMenuItem(
                                text = { Text(PaymentTerms.getLabel(term), color = Color(0xFF0F172A)) },
                                onClick = {
                                    paymentTerm = term
                                    termDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Meio de Pagamento Dropdown
                ExposedDropdownMenuBox(
                    expanded = meansDropdownExpanded,
                    onExpandedChange = { meansDropdownExpanded = !meansDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = PaymentMeans.getLabel(paymentMeans),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Meio de Pagamento Padrão") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = meansDropdownExpanded) },
                        colors = executiveTextFieldColors(),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = meansDropdownExpanded,
                        onDismissRequest = { meansDropdownExpanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        meansList.forEach { mean ->
                            DropdownMenuItem(
                                text = { Text(PaymentMeans.getLabel(mean), color = Color(0xFF0F172A)) },
                                onClick = {
                                    paymentMeans = mean
                                    meansDropdownExpanded = false
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
                    if (name.isNotBlank() && cnpj.isNotBlank()) {
                        onConfirm(
                            Company(
                                id = company?.id ?: "",
                                name = name.trim(),
                                fantasyName = fantasyName.trim(),
                                cnpj = cnpj.trim(),
                                contactPerson = contactPerson.trim(),
                                phone = phone.trim(),
                                email = email.trim(),
                                paymentTerm = paymentTerm,
                                paymentMeans = paymentMeans,
                                active = true
                            )
                        )
                    }
                },
                colors = executiveButtonPrimaryColors(),
                enabled = name.isNotBlank() && cnpj.isNotBlank()
            ) {
                Text(
                    text = if (isNew) "Cadastrar Empresa" else "Salvar Alterações",
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
