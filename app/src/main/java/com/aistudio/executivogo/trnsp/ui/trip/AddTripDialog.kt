package com.aistudio.executivogo.trnsp.ui.trip

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aistudio.executivogo.trnsp.data.*
import com.aistudio.executivogo.trnsp.ui.executiveTextFieldColors
import com.aistudio.executivogo.trnsp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

data class AdditionalPassengerEntry(
    val name: String = "",
    val phone: String = ""
)

@Composable
private fun selectorFieldColors(): TextFieldColors {
    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color(0xFFF8FAFC),
        disabledContainerColor = Color(0xFFF8FAFC),
        errorContainerColor = Color.White,
        focusedTextColor = Color(0xFF0F172A),
        unfocusedTextColor = Color(0xFF0F172A),
        disabledTextColor = Color(0xFF0F172A),
        errorTextColor = Color(0xFF0F172A),
        focusedBorderColor = EmeraldAccent,
        unfocusedBorderColor = Color(0xFFCBD5E1),
        disabledBorderColor = Color(0xFFCBD5E1),
        errorBorderColor = RedDanger,
        focusedLabelColor = EmeraldAccent,
        unfocusedLabelColor = Color(0xFF334155),
        disabledLabelColor = Color(0xFF334155),
        focusedLeadingIconColor = NavyPrimary,
        unfocusedLeadingIconColor = NavySecondary,
        disabledLeadingIconColor = NavySecondary,
        focusedTrailingIconColor = NavyPrimary,
        unfocusedTrailingIconColor = NavySecondary,
        disabledTrailingIconColor = NavySecondary,
        cursorColor = EmeraldAccent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTripDialog(
    companies: List<Company>,
    passengers: List<Passenger>,
    drivers: List<AppUser>,
    routes: List<Route> = emptyList(),
    fareBands: List<FareBand> = emptyList(),
    tripToEdit: Trip? = null,
    onDismiss: () -> Unit,
    onSave: (Trip) -> Unit
) {
    val context = LocalContext.current
    var scheduledCalendar by remember {
        mutableStateOf(
            Calendar.getInstance().apply {
                if (tripToEdit != null && tripToEdit.scheduledTime > 0) {
                    timeInMillis = tripToEdit.scheduledTime
                } else {
                    add(Calendar.MINUTE, 30)
                }
            }
        )
    }
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale("pt", "BR")) }

    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = scheduledCalendar.clone() as Calendar
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                scheduledCalendar = cal
            },
            scheduledCalendar.get(Calendar.YEAR),
            scheduledCalendar.get(Calendar.MONTH),
            scheduledCalendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    val timePickerDialog = remember {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val cal = scheduledCalendar.clone() as Calendar
                cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                cal.set(Calendar.MINUTE, minute)
                scheduledCalendar = cal
            },
            scheduledCalendar.get(Calendar.HOUR_OF_DAY),
            scheduledCalendar.get(Calendar.MINUTE),
            true
        )
    }

    var selectedCompany by remember {
        mutableStateOf(
            if (tripToEdit != null && tripToEdit.companyId.isNotBlank()) {
                companies.find { it.id == tripToEdit.companyId } ?: Company(
                    id = tripToEdit.companyId,
                    name = tripToEdit.companyName.ifBlank { "Empresa" },
                    paymentTerm = tripToEdit.paymentTermSnapshot.ifBlank { PaymentTerms.A_VISTA },
                    paymentMeans = tripToEdit.paymentMeansSnapshot.ifBlank { PaymentMeans.PIX }
                )
            } else {
                companies.firstOrNull()
            }
        )
    }
    var selectedPaymentTerm by remember {
        mutableStateOf(
            tripToEdit?.paymentTermSnapshot?.ifBlank { null }
                ?: companies.firstOrNull()?.paymentTerm?.ifBlank { PaymentTerms.A_VISTA }
                ?: PaymentTerms.A_VISTA
        )
    }
    var selectedPaymentMeans by remember {
        mutableStateOf(
            tripToEdit?.paymentMeansSnapshot?.ifBlank { null }
                ?: companies.firstOrNull()?.paymentMeans?.ifBlank { PaymentMeans.PIX }
                ?: PaymentMeans.PIX
        )
    }
    var selectedDriver by remember {
        mutableStateOf(
            if (tripToEdit != null && !tripToEdit.driverId.isNullOrBlank()) {
                drivers.find { it.id == tripToEdit.driverId } ?: drivers.firstOrNull()
            } else {
                drivers.firstOrNull()
            }
        )
    }
    var passengerName by remember { mutableStateOf(tripToEdit?.passengerName ?: "") }
    var passengerPhone by remember { mutableStateOf(tripToEdit?.passengerPhone ?: "") }
    var additionalPassengersList by remember {
        mutableStateOf(
            tripToEdit?.additionalPassengers?.map { raw ->
                if (raw.contains("(") && raw.contains(")")) {
                    val n = raw.substringBefore("(").trim()
                    val p = raw.substringAfter("(").substringBefore(")").trim()
                    AdditionalPassengerEntry(name = n, phone = p)
                } else {
                    AdditionalPassengerEntry(name = raw.trim(), phone = "")
                }
            } ?: listOf<AdditionalPassengerEntry>()
        )
    }
    var pricingMethod by remember { mutableStateOf(0) }
    var selectedPredefinedRoute by remember { mutableStateOf<Route?>(null) }
    var routeDropdownOpen by remember { mutableStateOf(false) }
    var distanceKmInput by remember { mutableStateOf("") }
    var selectedFareBandItem by remember { mutableStateOf<FareBand?>(null) }
    var fareBandDropdownOpen by remember { mutableStateOf(false) }

    var origin by remember { mutableStateOf(tripToEdit?.origin ?: "") }
    var stops by remember { mutableStateOf(tripToEdit?.stops ?: listOf<String>()) }
    var destination by remember { mutableStateOf(tripToEdit?.destination ?: "") }
    var priceText by remember {
        mutableStateOf(
            if (tripToEdit != null && tripToEdit.price > 0) "%.2f".format(Locale.US, tripToEdit.price) else ""
        )
    }
    var commissionText by remember {
        mutableStateOf(
            if (tripToEdit != null && tripToEdit.driverCommission > 0) "%.2f".format(Locale.US, tripToEdit.driverCommission) else ""
        )
    }
    var notes by remember { mutableStateOf(tripToEdit?.notes ?: "") }
    var tripStatus by remember { mutableStateOf(tripToEdit?.status ?: TripStatus.PENDING) }

    var companyDropdownExpanded by remember { mutableStateOf(false) }
    var termDropdownExpanded by remember { mutableStateOf(false) }
    var meansDropdownExpanded by remember { mutableStateOf(false) }
    var driverDropdownExpanded by remember { mutableStateOf(false) }

    val totalPrice = priceText.toDoubleOrNull() ?: 0.0
    val defaultCommPct = selectedDriver?.commissionPercentage ?: 0.0
    val parsedCustomComm = commissionText.toDoubleOrNull()
    val driverCommission = if (parsedCustomComm != null && commissionText.isNotBlank()) {
        parsedCustomComm
    } else {
        totalPrice * (defaultCommPct / 100.0)
    }
    val companyRevenue = (totalPrice - driverCommission).coerceAtLeast(0.0)

    // Filtered passengers for company suggestions
    val suggestedPassengers = remember(selectedCompany, passengers) {
        if (selectedCompany != null) {
            passengers.filter { it.companyId == selectedCompany!!.id }
        } else {
            passengers.take(5)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f)
                .widthIn(max = 600.dp)
                .shadow(16.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = SlateLight,
            border = BorderStroke(1.dp, SlateBorder)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar with High Contrast Navy & Emerald
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(EmeraldAccent.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (tripToEdit != null) Icons.Default.EditRoad else Icons.Default.AddRoad,
                                    contentDescription = null,
                                    tint = EmeraldLight,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (tripToEdit != null) "Editar Viagem Completa" else "Agendar Viagem Executiva",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 17.sp
                                )
                                Text(
                                    text = if (tripToEdit != null) "Atualização cadastral, itinerário e financeiro" else "Despacho operacional e faturamento",
                                    fontSize = 11.sp,
                                    color = EmeraldContainer
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Fechar",
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Scrollable Form Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // SECTION 1: DATA E HORÁRIO (Emerald & Navy Theme)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        border = BorderStroke(1.dp, EmeraldLight.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Event,
                                    contentDescription = null,
                                    tint = EmeraldDark,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "DATA E HORÁRIO DO EMBARQUE *",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = EmeraldDark,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Date Picker Card
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { datePickerDialog.show() },
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, SlateBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.CalendarMonth,
                                            contentDescription = "Calendário",
                                            tint = NavySecondary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                "Data (Toque p/ mudar)",
                                                fontSize = 10.sp,
                                                color = SlateTextSecondary,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                dateFormatter.format(scheduledCalendar.time),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = NavyPrimary
                                            )
                                        }
                                    }
                                }

                                // Time Picker Card
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { timePickerDialog.show() },
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, SlateBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Schedule,
                                            contentDescription = "Relógio",
                                            tint = EmeraldDark,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                "Horário (Toque p/ mudar)",
                                                fontSize = 10.sp,
                                                color = SlateTextSecondary,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                timeFormatter.format(scheduledCalendar.time),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = NavyPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // SECTION 2: EMPRESA E CONDIÇÕES DE FATURAMENTO (Navy Theme)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        border = BorderStroke(1.dp, SlateBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Business,
                                    contentDescription = null,
                                    tint = NavyPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "CLIENTE CORPORATIVO & FATURAMENTO *",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = NavyPrimary,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            // Company Dropdown
                            ExposedDropdownMenuBox(
                                expanded = companyDropdownExpanded,
                                onExpandedChange = { companyDropdownExpanded = !companyDropdownExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedCompany?.name ?: "",
                                    placeholder = { Text("Selecione a Empresa Cliente...", color = Color(0xFF64748B)) },
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Empresa Cliente *", color = Color(0xFF334155), fontWeight = FontWeight.SemiBold) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Apartment, contentDescription = null, tint = NavySecondary)
                                    },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = companyDropdownExpanded) },
                                    textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                                    colors = selectorFieldColors(),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = companyDropdownExpanded,
                                    onDismissRequest = { companyDropdownExpanded = false },
                                    modifier = Modifier.background(Color.White)
                                ) {
                                    companies.forEach { company ->
                                        DropdownMenuItem(
                                            text = {
                                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    Text(company.name, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 14.sp)
                                                    Text(
                                                        "Padrão: ${PaymentTerms.getLabel(company.paymentTerm)} • ${PaymentMeans.getLabel(company.paymentMeans)}",
                                                        fontSize = 12.sp,
                                                        color = Color(0xFF475569)
                                                    )
                                                }
                                            },
                                            onClick = {
                                                selectedCompany = company
                                                if (company.paymentTerm.isNotBlank()) {
                                                    selectedPaymentTerm = company.paymentTerm
                                                }
                                                if (company.paymentMeans.isNotBlank()) {
                                                    selectedPaymentMeans = company.paymentMeans
                                                }
                                                companyDropdownExpanded = false
                                            },
                                            colors = MenuDefaults.itemColors(
                                                textColor = Color(0xFF0F172A)
                                            )
                                        )
                                    }
                                }
                            }

                            // Prazo de Faturamento Dropdown (Full width to avoid squished labels)
                            ExposedDropdownMenuBox(
                                expanded = termDropdownExpanded,
                                onExpandedChange = { termDropdownExpanded = !termDropdownExpanded }
                            ) {
                                OutlinedTextField(
                                    value = PaymentTerms.getLabel(selectedPaymentTerm),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Prazo de Faturamento / Pagamento *", color = Color(0xFF334155), fontWeight = FontWeight.SemiBold) },
                                    leadingIcon = {
                                        Icon(Icons.Default.HourglassBottom, contentDescription = null, tint = NavySecondary)
                                    },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = termDropdownExpanded) },
                                    textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                    colors = selectorFieldColors(),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = termDropdownExpanded,
                                    onDismissRequest = { termDropdownExpanded = false },
                                    modifier = Modifier.background(Color.White)
                                ) {
                                    listOf(
                                        PaymentTerms.A_VISTA,
                                        PaymentTerms.D7,
                                        PaymentTerms.D15,
                                        PaymentTerms.D30,
                                        PaymentTerms.D60
                                    ).forEach { term ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Surface(
                                                        color = Color(0xFFF1F5F9),
                                                        shape = RoundedCornerShape(6.dp),
                                                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                                                    ) {
                                                        Text(
                                                            text = PaymentTerms.getLabel(term),
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp,
                                                            color = Color(0xFF0F172A)
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                selectedPaymentTerm = term
                                                termDropdownExpanded = false
                                            },
                                            colors = MenuDefaults.itemColors(
                                                textColor = Color(0xFF0F172A)
                                            )
                                        )
                                    }
                                }
                            }

                            // Forma de Pagamento Dropdown (Full width)
                            ExposedDropdownMenuBox(
                                expanded = meansDropdownExpanded,
                                onExpandedChange = { meansDropdownExpanded = !meansDropdownExpanded }
                            ) {
                                OutlinedTextField(
                                    value = PaymentMeans.getLabel(selectedPaymentMeans),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Forma de Pagamento Oficial *", color = Color(0xFF334155), fontWeight = FontWeight.SemiBold) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Payment, contentDescription = null, tint = Color(0xFF0284C7))
                                    },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = meansDropdownExpanded) },
                                    textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                    colors = selectorFieldColors(),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = meansDropdownExpanded,
                                    onDismissRequest = { meansDropdownExpanded = false },
                                    modifier = Modifier.background(Color.White)
                                ) {
                                    listOf(
                                        PaymentMeans.PIX,
                                        PaymentMeans.DEBITO,
                                        PaymentMeans.CREDITO,
                                        PaymentMeans.FATURAMENTO,
                                        PaymentMeans.DINHEIRO
                                    ).forEach { mean ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Surface(
                                                        color = Color(0xFFE0F2FE),
                                                        shape = RoundedCornerShape(6.dp),
                                                        border = BorderStroke(1.dp, Color(0xFF7DD3FC))
                                                    ) {
                                                        Text(
                                                            text = PaymentMeans.getLabel(mean),
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp,
                                                            color = Color(0xFF0369A1)
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                selectedPaymentMeans = mean
                                                meansDropdownExpanded = false
                                            },
                                            colors = MenuDefaults.itemColors(
                                                textColor = Color(0xFF0F172A)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // SECTION 3: PASSAGEIRO (Blue / Sky Theme)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        border = BorderStroke(1.dp, SlateBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = BlueInfo,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "DADOS DO PASSAGEIRO *",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = BlueInfo,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            // Quick Passenger Suggestions if available
                            if (suggestedPassengers.isNotEmpty()) {
                                Text(
                                    "Passageiros Cadastrados (toque p/ preencher):",
                                    fontSize = 11.sp,
                                    color = SlateTextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    suggestedPassengers.take(3).forEach { p ->
                                        SuggestionChip(
                                            onClick = {
                                                passengerName = p.name
                                                if (p.phone.isNotBlank()) passengerPhone = p.phone
                                            },
                                            label = { Text(p.name, fontSize = 11.sp, maxLines = 1) },
                                            colors = SuggestionChipDefaults.suggestionChipColors(
                                                containerColor = Color(0xFFEFF6FF),
                                                labelColor = BlueInfo
                                            ),
                                            border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = passengerName,
                                onValueChange = { passengerName = it },
                                label = { Text("Nome Completo do Passageiro *") },
                                leadingIcon = {
                                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = NavySecondary)
                                },
                                colors = executiveTextFieldColors(),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = passengerPhone,
                                onValueChange = { passengerPhone = it },
                                label = { Text("Telefone / WhatsApp do Passageiro") },
                                leadingIcon = {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = EmeraldDark)
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                colors = executiveTextFieldColors(),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Passageiros Adicionais
                            if (additionalPassengersList.isNotEmpty()) {
                                Text(
                                    text = "Passageiros Adicionais (${additionalPassengersList.size}):",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyPrimary
                                )
                                additionalPassengersList.forEachIndexed { index, extraPassenger ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = Color(0xFFF8FAFC),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(22.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFFDBEAFE)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "${index + 2}",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = BlueInfo
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "Passageiro Adicional ${index + 2}",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = NavyPrimary
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {
                                                        val updated = additionalPassengersList.toMutableList()
                                                        updated.removeAt(index)
                                                        additionalPassengersList = updated
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.DeleteOutline,
                                                        contentDescription = "Remover passageiro adicional",
                                                        tint = RedDanger,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }

                                            // Campo 1: Nome do Passageiro Adicional
                                            OutlinedTextField(
                                                value = extraPassenger.name,
                                                onValueChange = { newName ->
                                                    val updated = additionalPassengersList.toMutableList()
                                                    updated[index] = extraPassenger.copy(name = newName)
                                                    additionalPassengersList = updated
                                                },
                                                label = { Text("Nome do Passageiro *", color = Color(0xFF334155)) },
                                                placeholder = { Text("Nome completo", color = Color(0xFF94A3B8)) },
                                                leadingIcon = {
                                                    Icon(Icons.Default.Person, contentDescription = null, tint = NavySecondary)
                                                },
                                                textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.Medium),
                                                colors = selectorFieldColors(),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            // Campo 2: WhatsApp / Telefone do Passageiro Adicional
                                            OutlinedTextField(
                                                value = extraPassenger.phone,
                                                onValueChange = { newPhone ->
                                                    val updated = additionalPassengersList.toMutableList()
                                                    updated[index] = extraPassenger.copy(phone = newPhone)
                                                    additionalPassengersList = updated
                                                },
                                                label = { Text("WhatsApp / Telefone", color = Color(0xFF334155)) },
                                                placeholder = { Text("(XX) 9XXXX-XXXX", color = Color(0xFF94A3B8)) },
                                                leadingIcon = {
                                                    Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF25D366))
                                                },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                                textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.Medium),
                                                colors = selectorFieldColors(),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }

                            // Botão para adicionar outro passageiro
                            OutlinedButton(
                                onClick = {
                                    additionalPassengersList = additionalPassengersList + AdditionalPassengerEntry()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = BlueInfo),
                                border = BorderStroke(1.dp, BlueInfo.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = BlueInfo
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "+ Adicionar Passageiro",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BlueInfo
                                )
                            }
                        }
                    }

                    // SECTION 4: MOTORISTA EXECUTIVO (Teal / Emerald Theme)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        border = BorderStroke(1.dp, SlateBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    tint = EmeraldDark,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "MOTORISTA EXECUTIVO DESIGNADO",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = EmeraldDark,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            ExposedDropdownMenuBox(
                                expanded = driverDropdownExpanded,
                                onExpandedChange = { driverDropdownExpanded = !driverDropdownExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedDriver?.let { "${it.name} • ${it.vehicleModel} (${it.vehiclePlate})" } ?: "",
                                    placeholder = { Text("Selecione o motorista da frota...", color = Color(0xFF64748B)) },
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Motorista da Frota", color = Color(0xFF334155), fontWeight = FontWeight.SemiBold) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Badge, contentDescription = null, tint = NavySecondary)
                                    },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = driverDropdownExpanded) },
                                    textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                                    colors = selectorFieldColors(),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = driverDropdownExpanded,
                                    onDismissRequest = { driverDropdownExpanded = false },
                                    modifier = Modifier.background(Color.White)
                                ) {
                                    drivers.forEach { driver ->
                                        DropdownMenuItem(
                                            text = {
                                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    Text(
                                                        "${driver.name} • ${driver.vehicleModel} (${driver.vehiclePlate})",
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF0F172A),
                                                        fontSize = 13.sp
                                                    )
                                                    Text(
                                                        "Comissão Padrão: ${driver.commissionPercentage.toInt()}% • Telefone: ${driver.phone.ifBlank { "Não informado" }}",
                                                        fontSize = 11.sp,
                                                        color = Color(0xFF047857),
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            },
                                            onClick = {
                                                selectedDriver = driver
                                                val p = priceText.toDoubleOrNull() ?: 0.0
                                                val comm = p * (driver.commissionPercentage / 100.0)
                                                commissionText = if (p > 0) "%.2f".format(comm) else ""
                                                driverDropdownExpanded = false
                                            },
                                            colors = MenuDefaults.itemColors(
                                                textColor = Color(0xFF0F172A)
                                            )
                                        )
                                    }
                                }
                            }

                            if (selectedDriver != null) {
                                Surface(
                                    color = Color(0xFFF0FDF4),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, EmeraldLight.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = "Veículo: ${selectedDriver!!.vehicleModel} • ${selectedDriver!!.vehicleColor}",
                                                fontSize = 11.sp,
                                                color = NavyPrimary,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "Placa: ${selectedDriver!!.vehiclePlate}",
                                                fontSize = 11.sp,
                                                color = SlateTextSecondary
                                            )
                                        }
                                        Surface(
                                            color = EmeraldContainer,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "Comissão: ${selectedDriver!!.commissionPercentage.toInt()}%",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = EmeraldDark,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // SECTION 5: ITINERÁRIO (Origem Verde, Paradas Âmbar, Destino Vermelho)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        border = BorderStroke(1.dp, SlateBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Route,
                                    contentDescription = null,
                                    tint = NavySecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ITINERÁRIO E PARADAS *",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = NavyPrimary,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            // Origem (Embarque)
                            OutlinedTextField(
                                value = origin,
                                onValueChange = { origin = it },
                                label = { Text("Origem (Local de Embarque) *") },
                                leadingIcon = {
                                    Icon(Icons.Default.TripOrigin, contentDescription = null, tint = EmeraldAccent)
                                },
                                placeholder = { Text("ex: Av. Paulista, 1000 - Bela Vista") },
                                colors = executiveTextFieldColors(),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Intermediate stops
                            if (stops.isNotEmpty()) {
                                Text(
                                    "Paradas Intermediárias:",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = GoldWarning
                                )
                                stops.forEachIndexed { index, stopText ->
                                    Surface(
                                        color = Color(0xFFFFFBEB),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = stopText,
                                                onValueChange = { updated ->
                                                    stops = stops.toMutableList().also { it[index] = updated }
                                                },
                                                label = { Text("🛑 Parada ${index + 1}") },
                                                placeholder = { Text("Endereço da parada intermediária") },
                                                colors = executiveTextFieldColors(),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(
                                                onClick = {
                                                    stops = stops.toMutableList().also { it.removeAt(index) }
                                                },
                                                modifier = Modifier.padding(start = 4.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Remover Parada",
                                                    tint = RedDanger
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Add stop button
                            OutlinedButton(
                                onClick = { stops = stops + "" },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NavyPrimary),
                                border = BorderStroke(1.dp, GoldWarning.copy(alpha = 0.6f))
                            ) {
                                Icon(
                                    Icons.Default.AddLocationAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = GoldWarning
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Adicionar Parada Intermediária",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = NavyPrimary
                                )
                            }

                            // Destino Final (Desembarque)
                            OutlinedTextField(
                                value = destination,
                                onValueChange = { destination = it },
                                label = { Text("Destino Final (Desembarque) *") },
                                leadingIcon = {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = RedDanger)
                                },
                                placeholder = { Text("ex: Aeroporto de Guarulhos - Terminal 3") },
                                colors = executiveTextFieldColors(),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Notes / Observations
                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = { Text("Observações (Voo, Portão, Bagagens, etc.)") },
                                leadingIcon = {
                                    Icon(Icons.Default.Notes, contentDescription = null, tint = SlateTextSecondary)
                                },
                                colors = executiveTextFieldColors(),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // SECTION 6: VALORES E DEMONSTRATIVO FINANCEIRO (High Contrast)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        border = BorderStroke(1.dp, EmeraldLight.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AttachMoney,
                                    contentDescription = null,
                                    tint = EmeraldAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "VALORES & REPASSE FINANCEIRO *",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = EmeraldDark,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Text(
                                text = "Como deseja definir o valor da viagem?",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NavyPrimary
                            )

                            // 3 Pricing Options Selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    onClick = { pricingMethod = 0 },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (pricingMethod == 0) NavyPrimary else Color(0xFFE2E8F0),
                                    border = BorderStroke(1.dp, if (pricingMethod == 0) NavyPrimary else SlateBorder),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (pricingMethod == 0) GoldAccent else NavyPrimary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            "Livre / Manual",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (pricingMethod == 0) Color.White else NavyPrimary
                                        )
                                    }
                                }

                                Surface(
                                    onClick = { pricingMethod = 1 },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (pricingMethod == 1) NavyPrimary else Color(0xFFE2E8F0),
                                    border = BorderStroke(1.dp, if (pricingMethod == 1) NavyPrimary else SlateBorder),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.AltRoute,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (pricingMethod == 1) GoldAccent else NavyPrimary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            "Rota Cadastrada",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (pricingMethod == 1) Color.White else NavyPrimary
                                        )
                                    }
                                }

                                Surface(
                                    onClick = { pricingMethod = 2 },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (pricingMethod == 2) NavyPrimary else Color(0xFFE2E8F0),
                                    border = BorderStroke(1.dp, if (pricingMethod == 2) NavyPrimary else SlateBorder),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.LinearScale,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (pricingMethod == 2) GoldAccent else NavyPrimary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            "Faixa KM",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (pricingMethod == 2) Color.White else NavyPrimary
                                        )
                                    }
                                }
                            }

                            // Sub-Painel: Rota Pré-estabelecida
                            if (pricingMethod == 1) {
                                Surface(
                                    color = Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, SlateBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            "Selecione uma Rota Cadastrada:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NavyPrimary
                                        )

                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            OutlinedTextField(
                                                value = selectedPredefinedRoute?.let { "${it.origin} → ${it.destination} (R$ %.2f)".format(it.price) }
                                                    ?: "Toque para escolher uma rota...",
                                                onValueChange = {},
                                                readOnly = true,
                                                trailingIcon = {
                                                    IconButton(onClick = { routeDropdownOpen = !routeDropdownOpen }) {
                                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = NavyPrimary)
                                                    }
                                                },
                                                colors = selectorFieldColors(),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { routeDropdownOpen = true }
                                            )

                                            DropdownMenu(
                                                expanded = routeDropdownOpen,
                                                onDismissRequest = { routeDropdownOpen = false },
                                                modifier = Modifier
                                                    .background(Color.White)
                                                    .fillMaxWidth(0.9f)
                                            ) {
                                                if (routes.isEmpty()) {
                                                    DropdownMenuItem(
                                                        text = { Text("Nenhuma rota cadastrada", color = SlateTextSecondary) },
                                                        onClick = { routeDropdownOpen = false }
                                                    )
                                                } else {
                                                    routes.forEach { r ->
                                                        DropdownMenuItem(
                                                            text = {
                                                                Column {
                                                                    Text("${r.origin} → ${r.destination}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = NavyPrimary)
                                                                    Text("R$ %.2f • %.1f km".format(r.price, r.distanceKm), fontSize = 11.sp, color = EmeraldDark)
                                                                }
                                                            },
                                                            onClick = {
                                                                selectedPredefinedRoute = r
                                                                routeDropdownOpen = false
                                                                if (origin.isBlank()) origin = r.origin
                                                                destination = r.destination
                                                                priceText = "%.2f".format(Locale.US, r.price)
                                                                val p = r.price
                                                                val commPct = selectedDriver?.commissionPercentage ?: 0.0
                                                                val comm = p * (commPct / 100.0)
                                                                commissionText = if (p > 0) "%.2f".format(Locale.US, comm) else ""
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Sub-Painel: Faixa de Preço KM
                            if (pricingMethod == 2) {
                                Surface(
                                    color = Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, SlateBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            "Cálculo por Faixa de KM / Multiplicador:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NavyPrimary
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = distanceKmInput,
                                                onValueChange = { input ->
                                                    distanceKmInput = input
                                                    val km = input.toDoubleOrNull() ?: 0.0
                                                    val band = selectedFareBandItem ?: fareBands.find { km >= it.minKm && (it.maxKm <= 0.0 || km <= it.maxKm) } ?: fareBands.firstOrNull()
                                                    if (band != null) {
                                                        selectedFareBandItem = band
                                                        val calc = km * band.multiplier
                                                        priceText = "%.2f".format(Locale.US, calc)
                                                        val commPct = selectedDriver?.commissionPercentage ?: 0.0
                                                        val comm = calc * (commPct / 100.0)
                                                        commissionText = if (calc > 0) "%.2f".format(Locale.US, comm) else ""
                                                    }
                                                },
                                                label = { Text("Distância (KM)", fontSize = 11.sp) },
                                                placeholder = { Text("ex: 45.0") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                colors = executiveTextFieldColors(),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f)
                                            )

                                            Box(modifier = Modifier.weight(1f)) {
                                                OutlinedTextField(
                                                    value = selectedFareBandItem?.let { "${it.name} (R$ %.2f/km)".format(it.multiplier) } ?: "Faixa KM...",
                                                    onValueChange = {},
                                                    readOnly = true,
                                                    trailingIcon = {
                                                        IconButton(onClick = { fareBandDropdownOpen = !fareBandDropdownOpen }) {
                                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = NavyPrimary)
                                                        }
                                                    },
                                                    colors = selectorFieldColors(),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.fillMaxWidth().clickable { fareBandDropdownOpen = true }
                                                )

                                                DropdownMenu(
                                                    expanded = fareBandDropdownOpen,
                                                    onDismissRequest = { fareBandDropdownOpen = false },
                                                    modifier = Modifier.background(Color.White)
                                                ) {
                                                    fareBands.forEach { b ->
                                                        DropdownMenuItem(
                                                            text = {
                                                                Text("${b.name}: %.0f-%.0f km (R$ %.2f/km)".format(b.minKm, b.maxKm, b.multiplier), fontSize = 11.sp)
                                                            },
                                                            onClick = {
                                                                selectedFareBandItem = b
                                                                fareBandDropdownOpen = false
                                                                val km = distanceKmInput.toDoubleOrNull() ?: 0.0
                                                                val calc = km * b.multiplier
                                                                priceText = "%.2f".format(Locale.US, calc)
                                                                val commPct = selectedDriver?.commissionPercentage ?: 0.0
                                                                val comm = calc * (commPct / 100.0)
                                                                commissionText = if (calc > 0) "%.2f".format(Locale.US, comm) else ""
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Preço Total
                            OutlinedTextField(
                                value = priceText,
                                onValueChange = {
                                    priceText = it
                                    val p = it.toDoubleOrNull() ?: 0.0
                                    val commPct = selectedDriver?.commissionPercentage ?: 0.0
                                    val comm = p * (commPct / 100.0)
                                    commissionText = if (p > 0) "%.2f".format(comm) else ""
                                },
                                label = { Text("Valor Total da Corrida (R$) *") },
                                leadingIcon = {
                                    Text("R$", fontWeight = FontWeight.Bold, color = EmeraldAccent, fontSize = 14.sp)
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = executiveTextFieldColors(),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Comissão do Motorista
                            OutlinedTextField(
                                value = commissionText,
                                onValueChange = { commissionText = it },
                                label = {
                                    val pct = selectedDriver?.commissionPercentage
                                    Text(if (pct != null) "Repasse do Motorista (${pct.toInt()}%) R$" else "Repasse do Motorista (R$)")
                                },
                                leadingIcon = {
                                    Text("R$", fontWeight = FontWeight.Bold, color = NavySecondary, fontSize = 14.sp)
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = executiveTextFieldColors(),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // High-Contrast Financial Division Box
                            Surface(
                                color = NavyPrimary,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        "Demonstrativo da Divisão Financeira",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = EmeraldContainer
                                    )
                                    HorizontalDivider(color = NavyContainer)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Valor Total Faturado:", fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f))
                                        Text(
                                            "R$ %.2f".format(totalPrice),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color.White
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Repasse Motorista:", fontSize = 13.sp, color = EmeraldLight)
                                        Text(
                                            "R$ %.2f".format(driverCommission),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = EmeraldLight
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Receita Líquida Empresa:", fontSize = 13.sp, color = Color(0xFF93C5FD))
                                        Text(
                                            "R$ %.2f".format(companyRevenue),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color(0xFF93C5FD)
                                        )
                                    }
                                }
                            }
                        }

                        // Status da Viagem (Edição Completa pelo Administrador)
                        if (tripToEdit != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Tune,
                                            contentDescription = null,
                                            tint = NavyPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Status da Viagem (Administrador)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = NavyPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val statuses = listOf(
                                            TripStatus.PENDING to "Pendente",
                                            TripStatus.IN_PROGRESS to "Andamento",
                                            TripStatus.COMPLETED to "Concluída",
                                            TripStatus.CANCELLED to "Cancelada"
                                        )
                                        statuses.forEach { (st, label) ->
                                            val isSelected = tripStatus.equals(st, ignoreCase = true)
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { tripStatus = st },
                                                label = {
                                                    Text(
                                                        text = label,
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = when (st) {
                                                        TripStatus.COMPLETED -> EmeraldAccent
                                                        TripStatus.CANCELLED -> RedDanger
                                                        TripStatus.IN_PROGRESS -> Color(0xFF0284C7)
                                                        else -> NavyPrimary
                                                    },
                                                    selectedLabelColor = Color.White
                                                ),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Sticky Bottom Action Bar
                Surface(
                    color = Color.White,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, SlateBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, SlateBorder),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Cancelar", color = SlateTextSecondary, fontWeight = FontWeight.SemiBold)
                        }

                        val canSave = passengerName.isNotBlank() && origin.isNotBlank() && destination.isNotBlank() && totalPrice > 0

                        Button(
                            onClick = {
                                val p = priceText.toDoubleOrNull() ?: 0.0
                                val commPct = selectedDriver?.commissionPercentage ?: 0.0
                                val calculatedComm = p * (commPct / 100.0)
                                val parsedComm = commissionText.toDoubleOrNull()
                                val c = if (parsedComm != null && parsedComm >= 0.0 && commissionText.isNotBlank()) {
                                    parsedComm
                                } else {
                                    calculatedComm
                                }
                                val validStops = stops.map { it.trim() }.filter { it.isNotBlank() }
                                val validAdditional = additionalPassengersList
                                    .filter { it.name.isNotBlank() }
                                    .map { item ->
                                        val trimmedName = item.name.trim()
                                        val trimmedPhone = item.phone.trim()
                                        if (trimmedPhone.isNotBlank()) {
                                            "$trimmedName ($trimmedPhone)"
                                        } else {
                                            trimmedName
                                        }
                                    }
                                val baseTrip = tripToEdit ?: Trip()
                                val tripToSave = baseTrip.copy(
                                    id = tripToEdit?.id ?: "",
                                    companyId = selectedCompany?.id ?: "",
                                    companyName = selectedCompany?.name ?: "Avulso",
                                    paymentTermSnapshot = selectedPaymentTerm,
                                    paymentMeansSnapshot = selectedPaymentMeans,
                                    driverId = selectedDriver?.id,
                                    driverName = selectedDriver?.name ?: "",
                                    driverPhone = selectedDriver?.phone ?: "",
                                    vehicleModel = selectedDriver?.vehicleModel ?: "",
                                    vehiclePlate = selectedDriver?.vehiclePlate ?: "",
                                    vehicleColor = selectedDriver?.vehicleColor ?: "",
                                    passengerName = passengerName.trim(),
                                    passengerPhone = passengerPhone.trim(),
                                    additionalPassengers = validAdditional,
                                    origin = origin.trim(),
                                    stops = validStops,
                                    destination = destination.trim(),
                                    price = p,
                                    driverCommission = c,
                                    notes = notes.trim(),
                                    scheduledTime = scheduledCalendar.timeInMillis,
                                    status = if (tripToEdit != null) tripStatus else TripStatus.PENDING
                                )
                                onSave(tripToSave)
                            },
                            enabled = canSave,
                            colors = executiveButtonPrimaryColors(),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (tripToEdit != null) "Salvar Alterações" else "Agendar Viagem",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
