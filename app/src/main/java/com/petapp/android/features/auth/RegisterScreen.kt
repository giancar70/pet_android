package com.petapp.android.features.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petapp.android.R
import com.petapp.android.features.account.PdfViewerScreen

private val ScreenGradientTop = Color(0xFFFFFFFF)
private val ScreenGradientBottom = Color(0xFFD7FFF4)
private val BrandGreen = Color(0xFF406E5F)
private val SubtitleGray = Color(0xFF666666)
private val TextDark = Color(0xFF333333)
private val CardBorder = Color(0xFFEFEFF4)
private val FooterPillBackground = Color(0xFFC1E1D7)
private val StepDotInactive = Color(0xFFCFCFCF)
private val ButtonDisabledBg = Color(0xFFD9D9D9)
private val ButtonDisabledText = Color(0xFF8A8A8A)

private val emailPattern = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

private fun nameError(text: String): String? =
    if (text.isBlank()) "Introduce tu nombre." else null

private fun emailError(text: String): String? = when {
    text.isBlank() -> "Introduce tu correo electrónico."
    !emailPattern.matches(text) -> "Introduce un correo electrónico válido."
    else -> null
}

private fun passwordError(text: String): String? = when {
    text.isBlank() -> "Introduce una contraseña."
    text.length < 8 -> "La contraseña debe tener al menos 8 caracteres."
    else -> null
}

private fun confirmError(text: String, password: String): String? = when {
    text.isBlank() -> "Confirma tu contraseña."
    text != password -> "Las contraseñas no coinciden."
    else -> null
}

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var termsAccepted by remember { mutableStateOf(false) }

    // Errors only *appear* on blur (set from onFocusLost below); editing a field with a
    // visible error re-validates on every keystroke so it can clear as soon as the user
    // corrects it, without popping up new errors mid-typing (CU02 §1/§2).
    var nameErr by remember { mutableStateOf<String?>(null) }
    var emailErr by remember { mutableStateOf<String?>(null) }
    var passwordErr by remember { mutableStateOf<String?>(null) }
    var confirmErr by remember { mutableStateOf<String?>(null) }
    var generalError by remember { mutableStateOf<String?>(null) }

    var showTerms by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }

    val isFormValid = nameError(fullName) == null && emailError(email) == null &&
        passwordError(password) == null && confirmError(confirmPassword, password) == null &&
        termsAccepted
    val isLoading = uiState is AuthUiState.Loading

    LaunchedEffect(Unit) {
        viewModel.reset()
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            is AuthUiState.Success -> onRegisterSuccess()
            is AuthUiState.EmailTaken -> emailErr = "Ya existe una cuenta con este correo electrónico."
            is AuthUiState.Error -> generalError = (uiState as AuthUiState.Error).message
            else -> {}
        }
    }

    if (showTerms) {
        PdfViewerScreen(title = "Términos y condiciones", rawResId = R.raw.terms_and_conditions, onBack = { showTerms = false })
        return
    }
    if (showPrivacy) {
        PdfViewerScreen(title = "Política de privacidad", rawResId = R.raw.privacy_policy, onBack = { showPrivacy = false })
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ScreenGradientTop, ScreenGradientBottom))),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
        ) {
            Image(
                painter = painterResource(id = R.drawable.splash_logo),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                StepDot(active = true)
                Spacer(modifier = Modifier.width(8.dp))
                StepDot(active = false)
                Spacer(modifier = Modifier.width(8.dp))
                StepDot(active = false)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Crea tu cuenta",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 15.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Usa tu correo para gestionar \n toda la información de tu mascota.",
                fontSize = 12.sp,
                color = SubtitleGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(15.dp)) {
                    AuthTextField(
                        label = "Nombre completo",
                        value = fullName,
                        onValueChange = {
                            fullName = it
                            if (nameErr != null) nameErr = nameError(it)
                        },
                        leadingIcon = Icons.Filled.Person,
                        placeholder = "Ingresa tu nombre completo",
                        errorMessage = nameErr,
                        onFocusLost = { nameErr = nameError(fullName) },
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    AuthTextField(
                        label = "Correo electrónico",
                        value = email,
                        onValueChange = {
                            email = it
                            if (emailErr != null) emailErr = emailError(it)
                        },
                        leadingIcon = Icons.Filled.Email,
                        placeholder = "Ingresa tu correo",
                        keyboardType = KeyboardType.Email,
                        errorMessage = emailErr,
                        onFocusLost = { emailErr = emailError(email) },
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    AuthTextField(
                        label = "Contraseña",
                        value = password,
                        onValueChange = {
                            password = it
                            if (passwordErr != null) passwordErr = passwordError(it)
                        },
                        leadingIcon = Icons.Filled.Lock,
                        placeholder = "Crea una contraseña",
                        isPassword = true,
                        errorMessage = passwordErr,
                        caption = "Mínimo 8 caracteres.",
                        onFocusLost = { passwordErr = passwordError(password) },
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    AuthTextField(
                        label = "Confirmar contraseña",
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            if (confirmErr != null) confirmErr = confirmError(it, password)
                        },
                        leadingIcon = Icons.Filled.Lock,
                        placeholder = "Repite tu contraseña",
                        isPassword = true,
                        errorMessage = confirmErr,
                        onFocusLost = { confirmErr = confirmError(confirmPassword, password) },
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = termsAccepted,
                            onCheckedChange = { termsAccepted = it },
                            colors = CheckboxDefaults.colors(checkedColor = BrandGreen),
                        )
                        val termsText = buildAnnotatedString {
                            append("Acepto los ")
                            pushStringAnnotation(tag = "terms", annotation = "terms")
                            withStyle(SpanStyle(color = BrandGreen, textDecoration = TextDecoration.Underline)) {
                                append("Términos y Condiciones")
                            }
                            pop()
                            append(" y la ")
                            pushStringAnnotation(tag = "privacy", annotation = "privacy")
                            withStyle(SpanStyle(color = BrandGreen, textDecoration = TextDecoration.Underline)) {
                                append("Política de Privacidad")
                            }
                            pop()
                            append(".")
                        }
                        ClickableText(
                            text = termsText,
                            style = TextStyle(fontSize = 13.sp, color = TextDark),
                            modifier = Modifier.padding(start = 4.dp),
                            onClick = { offset ->
                                termsText.getStringAnnotations("terms", offset, offset).firstOrNull()?.let { showTerms = true }
                                termsText.getStringAnnotations("privacy", offset, offset).firstOrNull()?.let { showPrivacy = true }
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (generalError != null) {
                Text(
                    text = generalError!!,
                    color = AuthFieldErrorRed,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
                onClick = {
                    generalError = null
                    viewModel.register(fullName, email, password)
                },
                enabled = isFormValid && !isLoading,
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandGreen,
                    contentColor = Color.White,
                    disabledContainerColor = ButtonDisabledBg,
                    disabledContentColor = ButtonDisabledText,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.height(18.dp).width(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Creando cuenta…", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text(text = "Crear cuenta", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = FooterPillBackground,
                modifier = Modifier.height(50.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(text = "¿Ya tienes una cuenta? ", color = Color.Black, fontSize = 13.sp)
                    Text(
                        text = "Ingresa",
                        color = BrandGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable(onClick = onNavigateToLogin),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StepDot(active: Boolean) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(if (active) BrandGreen else StepDotInactive),
    )
}

@Preview(showBackground = true)
@Composable
private fun RegisterScreenPreview() {
    RegisterScreen(onRegisterSuccess = {}, onNavigateToLogin = {})
}
