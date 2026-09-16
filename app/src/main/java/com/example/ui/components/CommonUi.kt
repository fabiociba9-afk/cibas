package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.DriverEntity
import com.example.data.entity.TripEntity
import com.example.data.entity.TripStatus
import com.example.data.entity.UserRole
import com.example.ui.theme.*
import com.example.util.Formatters

@Composable
fun RoleBadge(role: String, modifier: Modifier = Modifier) {
    val (bg, textColor, text) = when (role) {
        UserRole.SUPER_ADMIN.name -> Triple(NavyPrimary, Color.White, "Super Admin")
        UserRole.COMPANY_ADMIN.name -> Triple(Color(0xFF0284C7), Color.White, "Admin Empresa")
        UserRole.DRIVER.name -> Triple(EmeraldAccent, Color.White, "Motorista")
        UserRole.COMPANY_USER.name -> Triple(Color(0xFF64748B), Color.White, "Solicitante")
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
    val (bg, textColor, label) = when (status) {
        TripStatus.AGENDADA.name -> Triple(Color(0xFFE0F2FE), Color(0xFF0369A1), "Agendada")
        TripStatus.EM_ANDAMENTO.name -> Triple(Color(0xFFFEF3C7), Color(0xFFB45309), "Em andamento")
        TripStatus.CONCLUIDA.name -> Triple(Color(0xFFD1FAE5), Color(0xFF065F46), "Concluída")
        TripStatus.CANCELADA.name -> Triple(Color(0xFFFEE2E2), Color(0xFF991B1B), "Cancelada")
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
fun DriverRadarMapCanvas(
    drivers: List<DriverEntity>,
    selectedDriver: DriverEntity?,
    onSelectDriver: (DriverEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    // Reference center in SP: (-23.5700, -46.6600)
    val centerLat = -23.5700
    val centerLng = -46.6600
    val latRange = 0.22
    val lngRange = 0.30

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NavyDark)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NavyPrimary)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(EmeraldLight)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mapa de Frotas em Tempo Real",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Text(
                    text = "${drivers.count { it.isActive }} motoristas ativos",
                    color = EmeraldContainer,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Radar Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(Color(0xFF071220))
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(drivers) {
                            detectTapGestures { tapOffset ->
                                val w = size.width
                                val h = size.height
                                drivers.forEach { d ->
                                    val nx = ((d.longitude - (centerLng - lngRange / 2)) / lngRange).coerceIn(0.08, 0.92)
                                    val ny = (1.0 - (d.latitude - (centerLat - latRange / 2)) / latRange).coerceIn(0.08, 0.92)
                                    val dx = nx.toFloat() * w
                                    val dy = ny.toFloat() * h
                                    val dist = kotlin.math.hypot(tapOffset.x - dx, tapOffset.y - dy)
                                    if (dist < 40f) {
                                        onSelectDriver(d)
                                    }
                                }
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height

                    // Grid Lines
                    val gridColor = Color(0xFF132F54).copy(alpha = 0.5f)
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

                    for (i in 1..5) {
                        val y = h * (i / 6f)
                        drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f, pathEffect = dashEffect)
                    }
                    for (i in 1..7) {
                        val x = w * (i / 8f)
                        drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f, pathEffect = dashEffect)
                    }

                    // Concentric Radar Rings
                    val center = Offset(w / 2, h / 2)
                    drawCircle(Color(0xFF10B981).copy(alpha = 0.08f), radius = w * 0.2f, center = center, style = Stroke(1f))
                    drawCircle(Color(0xFF10B981).copy(alpha = 0.06f), radius = w * 0.35f, center = center, style = Stroke(1f))

                    // Draw Drivers
                    drivers.forEach { driver ->
                        val nx = ((driver.longitude - (centerLng - lngRange / 2)) / lngRange).coerceIn(0.08, 0.92)
                        val ny = (1.0 - (driver.latitude - (centerLat - latRange / 2)) / latRange).coerceIn(0.08, 0.92)
                        val pos = Offset(nx.toFloat() * w, ny.toFloat() * h)

                        val isSelected = selectedDriver?.id == driver.id
                        val pinColor = if (driver.isActive) {
                            if (driver.isOnline) EmeraldLight else Color(0xFF94A3B8)
                        } else Color(0xFFEF4444)

                        // Pulse if online & active
                        if (driver.isActive && driver.isOnline) {
                            drawCircle(
                                color = pinColor.copy(alpha = pulseAlpha),
                                radius = (12f * pulseScale),
                                center = pos
                            )
                        }

                        // Outer ring if selected
                        if (isSelected) {
                            drawCircle(
                                color = Color.White,
                                radius = 16f,
                                center = pos,
                                style = Stroke(3f)
                            )
                        }

                        // Driver core circle
                        drawCircle(
                            color = pinColor,
                            radius = if (isSelected) 10f else 7f,
                            center = pos
                        )
                    }
                }

                // Radar Legend / Tip
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .background(NavyDark.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(EmeraldLight))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Online", color = Color.White, fontSize = 10.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF94A3B8)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Offline", color = Color.White, fontSize = 10.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("(Toque para ver motorista)", color = Color(0xFF94A3B8), fontSize = 10.sp)
                }
            }

            // Selected Driver Drawer inside card
            if (selectedDriver != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NavySecondary)
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedDriver.fullName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "${selectedDriver.vehicleModel} • Placa: ${selectedDriver.vehiclePlate}",
                                color = EmeraldContainer,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Comissão: ${selectedDriver.commissionPercentage}% • Tel: ${selectedDriver.phone}",
                                color = Color.LightGray,
                                fontSize = 11.sp
                            )
                        }
                        Button(
                            onClick = {
                                Formatters.openInGoogleMaps(
                                    context,
                                    selectedDriver.latitude,
                                    selectedDriver.longitude,
                                    "Motorista: ${selectedDriver.fullName}"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Abrir GPS", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReceiptDialog(
    trip: TripEntity,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Text("Fechar")
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = EmeraldAccent)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Recibo ExecutivoGo", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Recibo #${trip.id}", fontWeight = FontWeight.Bold, color = NavyPrimary)
                            TripStatusBadge(trip.status)
                        }
                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = SlateBorder)

                        Text("Empresa: ${trip.companyName}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("Solicitante: ${trip.requesterName}", fontSize = 12.sp, color = SlateTextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Data: ${Formatters.formatDateTime(trip.dateTimeMillis)}", fontSize = 12.sp, color = SlateTextSecondary)

                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Trajeto:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("📍 De: ${trip.origin}", fontSize = 12.sp, color = SlateTextPrimary)
                        Text("🏁 Para: ${trip.destination}", fontSize = 12.sp, color = SlateTextPrimary)

                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Motorista: ${trip.driverName}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text("Forma de Pagamento: ${trip.paymentMethodName}", fontSize = 12.sp, color = SlateTextSecondary)
                        Text("Vencimento: ${Formatters.formatDate(trip.clientPaymentDueDateMillis)}", fontSize = 12.sp, color = SlateTextSecondary)

                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = SlateBorder)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("VALOR TOTAL:", fontWeight = FontWeight.Bold, color = NavyPrimary)
                            Text(
                                text = Formatters.formatCurrency(trip.price),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = EmeraldAccent
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons: WhatsApp & General Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            Formatters.shareReceiptWhatsApp(context, trip)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WhatsApp", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            Formatters.shareReceiptGeneric(context, trip)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Compartilhar", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                TextButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(Formatters.generateReceiptText(trip)))
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
