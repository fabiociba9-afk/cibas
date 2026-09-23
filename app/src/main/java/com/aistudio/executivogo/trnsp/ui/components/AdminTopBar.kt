package com.aistudio.executivogo.trnsp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.executivogo.trnsp.navigation.Screen
import com.aistudio.executivogo.trnsp.ui.theme.*

data class AdminMenuItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val subtitle: String
)

val ADMIN_PANEL_MENU = listOf(
    AdminMenuItem(Screen.Dashboard.route, "Painel Geral", Icons.Default.Dashboard, "Visão executiva da operação"),
    AdminMenuItem(Screen.Trips.route, "Gestão de Viagens", Icons.Default.Route, "Agendamento e despacho"),
    AdminMenuItem(Screen.LiveMap.route, "Mapa em Tempo Real", Icons.Default.Map, "Localização 24h e vias completas"),
    AdminMenuItem(Screen.Companies.route, "Empresas", Icons.Default.Business, "Cadastro de clientes e faturamento"),
    AdminMenuItem(Screen.Passengers.route, "Passageiros", Icons.Default.People, "Cadastro de passageiros corporativos"),
    AdminMenuItem(Screen.Drivers.route, "Motoristas", Icons.Default.DirectionsCar, "Gestão de frota e comissões"),
    AdminMenuItem(Screen.Users.route, "Usuários", Icons.Default.Badge, "Controle de acessos do sistema"),
    AdminMenuItem(Screen.Financial.route, "Financeiro", Icons.Default.AccountBalanceWallet, "Faturamento e conciliação"),
    AdminMenuItem(Screen.PaymentMethods.route, "Formas de Pagamento", Icons.Default.Payment, "Prazos e modalidades oficiais"),
    AdminMenuItem(Screen.FareBands.route, "Faixas de Preço", Icons.Default.PriceChange, "Tabela de km e multiplicadores"),
    AdminMenuItem(Screen.Routes.route, "Rotas & Tabela", Icons.Default.AltRoute, "Rotas fixas e orçamentos")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTopBar(
    title: String,
    currentScreen: String = "",
    subtitle: String? = null,
    onNavigate: ((String) -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
    onRefresh: (() -> Unit)? = null,
    showBackButton: Boolean = false,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color.White
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = Color(0xFFE2E8F0)
                    )
                }
            }
        },
        navigationIcon = {
            Box {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showBackButton && onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar",
                                tint = Color.White
                            )
                        }
                    }
                    if (onNavigate != null) {
                        IconButton(
                            onClick = { menuExpanded = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu do Painel Administrativo",
                                tint = EmeraldAccent
                            )
                        }
                    }
                }

                if (onNavigate != null) {
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier
                            .width(280.dp)
                            .background(Color.White)
                    ) {
                        Surface(
                            color = NavyPrimary,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = EmeraldAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Navegação do Painel",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "ExecutivoGo Gestão",
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = SlateBorder)

                        ADMIN_PANEL_MENU.forEach { item ->
                            val isCurrent = item.route == currentScreen
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = item.icon,
                                                contentDescription = null,
                                                tint = if (isCurrent) EmeraldAccent else NavyPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = item.title,
                                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                                                    fontSize = 13.sp,
                                                    color = if (isCurrent) EmeraldAccent else SlateTextPrimary
                                                )
                                                Text(
                                                    text = item.subtitle,
                                                    fontSize = 10.sp,
                                                    color = SlateTextSecondary
                                                )
                                            }
                                        }
                                        if (isCurrent) {
                                            Surface(
                                                color = EmeraldContainer,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "Atual",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF065F46),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                },
                                onClick = {
                                    menuExpanded = false
                                    if (!isCurrent) {
                                        onNavigate(item.route)
                                    }
                                }
                            )
                        }

                        if (onLogout != null) {
                            HorizontalDivider(color = SlateBorder)
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                            contentDescription = null,
                                            tint = RedDanger,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Sair do Painel",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = RedDanger
                                        )
                                    }
                                },
                                onClick = {
                                    menuExpanded = false
                                    onLogout()
                                }
                            )
                        }
                    }
                }
            }
        },
        actions = {
            if (onRefresh != null) {
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Atualizar Dados",
                        tint = Color.White
                    )
                }
            }
            actions()
            if (onLogout != null && !showBackButton) {
                IconButton(onClick = onLogout) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Sair",
                        tint = Color.White
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = NavyPrimary,
            titleContentColor = Color.White
        )
    )
}
