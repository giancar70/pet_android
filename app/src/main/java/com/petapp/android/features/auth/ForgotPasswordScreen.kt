package com.petapp.android.features.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

private val ScreenGradientTop = Color(0xFFFFFFFF)
private val ScreenGradientBottom = Color(0xFFD7FFF4)
private val BrandGreen = Color(0xFF406E5F)
private val SubtitleGray = Color(0xFF666666)
private val CardBorder = Color(0xFFEFEFF4)
private val ButtonDisabledBg = Color(0xFFD9D9D9)
private val ButtonDisabledText = Color(0xFF8A8A8A)

/// Same pattern the rest of the app's auth forms use (CU04 §2/§3): errors only
/// *appear* on blur, and re-validate on every keystroke once shown so they can clear
/// the moment the user corrects the field.
private val forgotEmailPattern = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

private fun forgotEmailError(text: String): String? = when {
    text.isBlank() -> "Introduce tu correo electrónico."
    !forgotEmailPattern.matches(text) -> "Introduce un correo electrónico válido."
    else -> null
}

@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    onCodeSent: (email: String) -> Unit,
    viewModel: ForgotPasswordViewModel = viewModel(),
) {
    val requestState by viewModel.requestState.collectAsState()
    var email by remember { mutableStateOf("") }
    var emailErr by remember { mutableStateOf<String?>(null) }
    var generalError by remember { mutableStateOf<String?>(null) }

    val isFormValid = forgotEmailError(email) == null
    val isLoading = requestState is RequestCodeUiState.Loading

    BackHandler(onBack = onBack)

    // ForgotPasswordViewModel resolves to the same Activity-scoped singleton on every
    // viewModel() call (no Navigation-Compose back stack to scope it to -- same as
    // PetsViewModel etc. elsewhere in the app), so a previous visit's Success/Error can
    // still be sitting in requestState when this screen re-enters (e.g. Login ->
    // Forgot -> back -> Forgot again). Resetting it races the effect below
    // (collectAsState's initial value may already have latched onto the stale state),
    // so consumedInitialState instead always ignores the first firing regardless of
    // what it sees, and only acts on a later, genuine result from this screen's own
    // request.
    LaunchedEffect(Unit) {
        viewModel.resetRequestState()
    }
    var consumedInitialState by remember { mutableStateOf(false) }
    LaunchedEffect(requestState) {
        if (!consumedInitialState) {
            consumedInitialState = true
            return@LaunchedEffect
        }
        when (val state = requestState) {
            is RequestCodeUiState.Success -> onCodeSent(email)
            is RequestCodeUiState.Error -> generalError = state.message
            else -> {}
        }
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
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "¿Olvidaste tu contraseña?",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Ingresa tu correo electrónico y te enviaremos\nun código para restablecerla",
                fontSize = 12.sp,
                color = SubtitleGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                shape = RoundedCornerShape(15.dp),
                color = Color.White,
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    AuthTextField(
                        label = "Correo electrónico",
                        value = email,
                        onValueChange = {
                            email = it
                            if (emailErr != null) emailErr = forgotEmailError(it)
                        },
                        leadingIcon = Icons.Filled.Email,
                        placeholder = "Ingresa tu correo",
                        keyboardType = KeyboardType.Email,
                        errorMessage = emailErr,
                        onFocusLost = { emailErr = forgotEmailError(email) },
                    )
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
                    viewModel.requestCode(email)
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
                    Text(text = "Enviando código…", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text(text = "Enviar código", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ForgotPasswordScreenPreview() {
    ForgotPasswordScreen(onBack = {}, onCodeSent = {})
}
