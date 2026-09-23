package com.aistudio.executivogo.trnsp.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.executivogo.trnsp.data.AppUser
import com.aistudio.executivogo.trnsp.data.Trip
import com.aistudio.executivogo.trnsp.data.TripStatus
import com.aistudio.executivogo.trnsp.data.UserRole
import com.aistudio.executivogo.trnsp.ui.theme.*

@Composable
fun RoleBadge(role: String, modifier: Modifier = Modifier) {
    val (bg, textColor, text) = when (role.uppercase()) {
        UserRole.ADMIN -> Triple(NavyPrimary, Color.White, "Administrador")
        UserRole.DRIVER -> Triple(EmeraldAccent, Color.White, "Motorista")
        UserRole.OPERATOR -> Triple(NavySecondary, Color.White, "Operador")
        else -> Triple(Color.Gray, Color.White, role)
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun TripStatusBadge(status: String, modifier: Modifier = Modifier) {
    val (bg, textColor, label) = when (status.uppercase()) {
        TripStatus.PENDING -> Triple(Color(0xFFE0F2FE), Color(0xFF0369A1), "Pendente")
        TripStatus.ACCEPTED -> Triple(Color(0xFFEDE9FE), Color(0xFF6D28D9), "Buscando Passageiro")
        TripStatus.IN_PROGRESS -> Triple(Color(0xFFFEF3C7), Color(0xFFB45309), "Em Andamento")
        TripStatus.COMPLETED -> Triple(Color(0xFFD1FAE5), Color(0xFF065F46), "Concluída")
        TripStatus.CANCELLED -> Triple(Color(0xFFFEE2E2), Color(0xFF991B1B), "Cancelada")
        else -> Triple(Color(0xFFF1F5F9), Color(0xFF475569), status)
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun ExecutiveStatCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector,
    accentColor: Color = EmeraldAccent,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SlateCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = SlateTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = NavyPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = SlateTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ReceiptDialog(
    trip: Trip,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    val receiptText = """
        ====================================
           EXECUTIVOGO - RECIBO DE VIAGEM
        ====================================
        Recibo Nº: #TRIP-${trip.id.take(8)}
        Status: ${trip.status}
        
        [EMPRESA CLIENTE]
        Empresa: ${trip.companyName}
        Passageiro: ${trip.passengerName} (Tel: ${trip.passengerPhone})
        
        [TRAJETO]
        Origem: ${trip.origin}
        Destino: ${trip.destination}
        
        [MOTORISTA]
        Motorista: ${trip.driverName}
        
        [VALORES]
        Valor Total: R$ ${"%.2f".format(trip.price)}
        Comissão Motorista: R$ ${"%.2f".format(trip.driverCommission)}
        ====================================
        ExecutivoGo - Transporte Corporativo
    """.trimIndent()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(20.dp),
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = executiveButtonDarkColors(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Fechar", color = Color.White)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = EmeraldAccent)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Recibo ExecutivoGo", fontWeight = FontWeight.Bold, color = NavyPrimary)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Recibo #${trip.id.take(6)}", fontWeight = FontWeight.Bold, color = NavyPrimary)
                            TripStatusBadge(trip.status)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = SlateBorder)

                        Text("Empresa: ${trip.companyName}", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = NavyPrimary)
                        Text("Passageiro: ${trip.passengerName}", fontSize = 12.sp, color = SlateTextSecondary)

                        Spacer(modifier = Modifier.height(6.dp))
                        Text("📍 Origem: ${trip.origin}", fontSize = 12.sp, color = SlateTextPrimary)
                        Text("🏁 Destino: ${trip.destination}", fontSize = 12.sp, color = SlateTextPrimary)

                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Motorista: ${trip.driverName}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = NavyPrimary)

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = SlateBorder)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("VALOR TOTAL:", fontWeight = FontWeight.Bold, color = NavyPrimary)
                            Text(
                                text = "R$ %.2f".format(trip.price),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = EmeraldAccent
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                TextButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(receiptText))
                        copied = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (copied) EmeraldAccent else NavyPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (copied) "Recibo Copiado!" else "Copiar Texto do Recibo",
                        color = if (copied) EmeraldAccent else NavyPrimary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    )
}

@Composable
fun executiveTextFieldColors(
    containerColor: Color = Color(0xFFF8FAFC),
    focusedBorderColor: Color = EmeraldAccent,
    unfocusedBorderColor: Color = SlateBorder,
    textColor: Color = Color(0xFF0F172A),
    labelColor: Color = Color(0xFF475569),
    placeholderColor: Color = Color(0xFF94A3B8),
    cursorColor: Color = EmeraldAccent
): TextFieldColors {
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = textColor,
        unfocusedTextColor = textColor,
        disabledTextColor = textColor,
        errorTextColor = textColor,
        focusedContainerColor = containerColor,
        unfocusedContainerColor = containerColor,
        disabledContainerColor = containerColor,
        errorContainerColor = containerColor,
        focusedBorderColor = focusedBorderColor,
        unfocusedBorderColor = unfocusedBorderColor,
        disabledBorderColor = unfocusedBorderColor,
        errorBorderColor = RedDanger,
        focusedLabelColor = focusedBorderColor,
        unfocusedLabelColor = labelColor,
        disabledLabelColor = labelColor,
        focusedPlaceholderColor = placeholderColor,
        unfocusedPlaceholderColor = placeholderColor,
        disabledPlaceholderColor = placeholderColor,
        focusedLeadingIconColor = focusedBorderColor,
        unfocusedLeadingIconColor = labelColor,
        disabledLeadingIconColor = labelColor,
        focusedTrailingIconColor = focusedBorderColor,
        unfocusedTrailingIconColor = labelColor,
        disabledTrailingIconColor = labelColor,
        cursorColor = cursorColor
    )
}
