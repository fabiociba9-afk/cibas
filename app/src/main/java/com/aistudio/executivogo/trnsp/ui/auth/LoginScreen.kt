package com.aistudio.executivogo.trnsp.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.executivogo.trnsp.data.AppUser
import com.aistudio.executivogo.trnsp.data.UserRole
import com.aistudio.executivogo.trnsp.ui.MainViewModel
import com.aistudio.executivogo.trnsp.ui.UiState
import com.aistudio.executivogo.trnsp.ui.executiveTextFieldColors
import com.aistudio.executivogo.trnsp.ui.theme.*

@Composable
fun LoginScreen(
    viewModel: MainViewModel,
    onNavigateToAdmin: () -> Unit = {},
    onNavigateToDriver: () -> Unit = {},
    onNavigateToCompany: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val uiState by viewModel.uiState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    fun handleLogin() {
        if (email.isBlank() || password.isBlank()) {
            errorMessage = "Por favor, preencha o e-mail e a senha."
            return
        }
        errorMessage = null
        viewModel.login(
            email = email.trim(),
            pass = password,
            onSuccess = { user ->
                when {
                    user?.role?.equals(UserRole.DRIVER, ignoreCase = true) == true -> onNavigateToDriver()
                    user?.role?.equals(UserRole.COMPANY, ignoreCase = true) == true -> onNavigateToCompany()
                    else -> onNavigateToAdmin()
                }
            },
            onError = { msg ->
                errorMessage = msg
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(NavyDark, NavyPrimary, SlateLight)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Brand Icon
            Image(
                painter = painterResource(id = com.aistudio.executivogo.trnsp.R.drawable.img_executivo_icon_transparent),
                contentDescription = "ExecutivoGo Logo",
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .shadow(12.dp, RoundedCornerShape(22.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "ExecutivoGo",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 1.sp
            )

            Text(
                text = "Transporte Corporativo & Gestão de Frotas",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = EmeraldLight,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Login Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCard)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Acesso ao Sistema",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                    Text(
                        text = "Entre com suas credenciais corporativas.",
                        fontSize = 12.sp,
                        color = SlateTextSecondary
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            errorMessage = null
                        },
                        label = { Text("E-mail Corporativo") },
                        placeholder = { Text("E-mail corporativo", color = SlateTextSecondary.copy(alpha = 0.6f)) },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = NavySecondary)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        colors = executiveTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = null
                        },
                        label = { Text("Senha") },
                        placeholder = { Text("Senha", color = SlateTextSecondary.copy(alpha = 0.6f)) },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = NavySecondary)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = NavySecondary
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        colors = executiveTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFEE2E2), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = RedDanger, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage ?: "",
                                color = RedDanger,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    Button(
                        onClick = { handleLogin() },
                        enabled = uiState !is UiState.Loading,
                        colors = executiveButtonPrimaryColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (uiState is UiState.Loading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Autenticando no Firebase...", fontSize = 14.sp, color = Color.White)
                        } else {
                            Icon(Icons.Default.Login, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Entrar", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Direct Demo Access Buttons for convenience
                    Text(
                        text = "Acesso Rápido de Demonstração:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SlateTextSecondary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                email = "admin@executivogo.com"
                                password = "admin"
                                errorMessage = null
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NavyPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Admin Demo", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                        }

                        OutlinedButton(
                            onClick = {
                                email = "motorista@executivogo.com"
                                password = "driver"
                                errorMessage = null
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldDark),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Motorista Demo", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldDark)
                        }
                    }
                }
            }
        }
    }
}
