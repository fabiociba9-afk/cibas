package com.example.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.PaymentMethodEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.*

@Composable
fun PaymentMethodsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val paymentMethods by viewModel.paymentMethods.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var methodToEdit by remember { mutableStateOf<PaymentMethodEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    methodToEdit = null
                    showDialog = true
                },
                containerColor = EmeraldAccent,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.AddCard, contentDescription = "Nova Forma de Pagamento")
            }
        },
        containerColor = SlateLight
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Header Info Card
            Card(
                colors = CardDefaults.cardColors(containerColor = NavyPrimary),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Formas de Pagamento & Faturamento",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = "Condições comerciais para clientes corporativos e prazos de repasse para motoristas.",
                        color = EmeraldContainer,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "${paymentMethods.size} Modalidades Configuradas",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = SlateTextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (paymentMethods.isEmpty()) {
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
                            Icons.Default.CreditCardOff,
                            contentDescription = null,
                            tint = SlateTextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Nenhuma forma de pagamento configurada",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = NavyPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Cadastre as condições comerciais para permitir o agendamento de viagens corporativas e controle de faturamento.",
                            fontSize = 13.sp,
                            color = SlateTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = {
                                viewModel.savePaymentMethod(name = "Faturado 15 Dias", description = "Fechamento quinzenal corporativo via boleto")
                                viewModel.savePaymentMethod(name = "PIX Corporativo", description = "Pagamento instantâneo via chave corporativa")
                                viewModel.savePaymentMethod(name = "Cartão de Crédito Corporativo", description = "Faturamento direto no cartão da empresa")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Adicionar Modalidades Sugeridas", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(paymentMethods) { method ->
                        Card(
                            modifier = Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(14.dp)),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = SlateCard)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(if (method.isActive) EmeraldContainer else Color(0xFFF1F5F9)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when {
                                                method.name.contains("PIX", ignoreCase = true) -> Icons.Default.QrCode
                                                method.name.contains("Faturado", ignoreCase = true) -> Icons.Default.Receipt
                                                method.name.contains("Cartão", ignoreCase = true) -> Icons.Default.CreditCard
                                                method.name.contains("Transferência", ignoreCase = true) -> Icons.Default.AccountBalance
                                                else -> Icons.Default.Payments
                                            },
                                            contentDescription = null,
                                            tint = if (method.isActive) EmeraldDark else Color.Gray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = method.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = NavyPrimary
                                        )
                                        if (method.description.isNotBlank()) {
                                            Text(
                                                text = method.description,
                                                fontSize = 12.sp,
                                                color = SlateTextSecondary
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            methodToEdit = method
                                            showDialog = true
                                        }
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = NavySecondary)
                                    }
                                    IconButton(
                                        onClick = { viewModel.deletePaymentMethod(method) }
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Excluir", tint = RedDanger)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        PaymentMethodDialog(
            method = methodToEdit,
            onDismiss = { showDialog = false },
            onSave = { name, desc, isActive ->
                viewModel.savePaymentMethod(
                    id = methodToEdit?.id ?: 0L,
                    name = name,
                    description = desc,
                    isActive = isActive
                )
                showDialog = false
            }
        )
    }
}

@Composable
fun PaymentMethodDialog(
    method: PaymentMethodEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, desc: String, isActive: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(method?.name ?: "") }
    var description by remember { mutableStateOf(method?.description ?: "") }
    var isActive by remember { mutableStateOf(method?.isActive ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (method == null) "Nova Forma de Pagamento" else "Editar Forma de Pagamento", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome (ex: PIX, Faturado 15D, Cartão)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição / Instruções de Faturamento") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Disponível para seleção")
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name, description, isActive)
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
