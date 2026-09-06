package com.petapp.android.features.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
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

private fun codeError(text: String): String? = when {
    text.isBlank() -> "Introduce el código de 6 dígitos."
    text.length != 6 || !text.all(Char::isDigit) -> "El código debe tener 6 dígitos."
    else -> null
}

private fun newPasswordError(text: String): String? = when {
    text.isBlank() -> "Introduce una contraseña."
    text.length < 8 -> "La contraseña debe tener al menos 8 caracteres."
    else -> null
}

private fun confirmPasswordError(text: String, password: String): String? = when {
    text.isBlank() -> "Confirma tu contraseña."
    text != password -> "Las contraseñas no coinciden."
    else -> null
}

@Composable
fun ResetPasswordScreen(
    email: String,
    onBack: () -> Unit,
    onResetSuccess: () -> Unit,
    viewModel: ForgotPasswordViewModel = viewModel(),
) {
    val requestState by viewModel.requestState.collectAsState()
    val confirmState by viewModel.confirmState.collectAsState()

    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var codeErr by remember { mutableStateOf<String?>(null) }
    var newPasswordErr by remember { mutableStateOf<String?>(null) }
    var confirmPasswordErr by remember { mutableStateOf<String?>(null) }
    var generalError by remember { mutableStateOf<String?>(null) }
    var resendMessage by remember { mutableStateOf<String?>(null) }

    val isFormValid = codeError(code) == null &&
        newPasswordError(newPassword) == null &&
        confirmPasswordError(confirmPassword, newPassword) == null
    val isSubmitting = confirmState is ConfirmResetUiState.Loading
    val isResending = requestState is RequestCodeUiState.Loading

    BackHandler(onBack = onBack)

    // ForgotPasswordViewModel resolves to the same Activity-scoped singleton on every
    // viewModel() call (no Navigation-Compose back stack to scope it to), so a previous
    // visit's Success/Error can still be sitting in confirmState when this screen
    // re-enters. consumedInitialState always ignores the first firing regardless of
    // what it sees, and only acts on a later, genuine result from this screen's own
    // submit -- same pattern as PetDetailScreen etc.
    LaunchedEffect(Unit) {
        viewModel.resetConfirmState()
    }
    var consumedInitialConfirmState by remember { mutableStateOf(false) }
    LaunchedEffect(confirmState) {
        if (!consumedInitialConfirmState) {
            consumedInitialConfirmState = true
            return@LaunchedEffect
        }
        when (val state = confirmState) {
            is ConfirmResetUiState.Success -> onResetSuccess()
            is ConfirmResetUiState.Error -> generalError = state.message
            else -> {}
        }
    }
    // The request-code state is shared with ForgotPasswordScreen's ViewModel instance
    // (the same Activity-scoped singleton), so "Reenviar código" reuses the exact same
    // requestCode() call the previous screen made -- this LaunchedEffect only reacts to
    // it to show a small confirmation instead of navigating (we're already on the right
    // screen), and only after the first, already-consumed emission from getting here.
    var consumedInitialRequestState by remember { mutableStateOf(false) }
    LaunchedEffect(requestState) {
        if (!consumedInitialRequestState) {
            consumedInitialRequestState = true
            return@LaunchedEffect
        }
        when (val state = requestState) {
            is RequestCodeUiState.Success -> resendMessage = "Te enviamos un nuevo código."
            is RequestCodeUiState.Error -> resendMessage = state.message
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
                text = "Ingresa el código",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Enviamos un código de 6 dígitos a\n$email",
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
                        label = "Código de verificación",
                        value = code,
                        onValueChange = {
                            val digitsOnly = it.filter(Char::isDigit).take(6)
                            code = digitsOnly
                            if (codeErr != null) codeErr = codeError(digitsOnly)
                        },
                        leadingIcon = Icons.Filled.Pin,
                        placeholder = "123456",
                        keyboardType = KeyboardType.NumberPassword,
                        errorMessage = codeErr,
                        onFocusLost = { codeErr = codeError(code) },
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    AuthTextField(
                        label = "Nueva contraseña",
                        value = newPassword,
                        onValueChange = {
                            newPassword = it
                            if (newPasswordErr != null) newPasswordErr = newPasswordError(it)
                            if (confirmPasswordErr != null) confirmPasswordErr = confirmPasswordError(confirmPassword, it)
                        },
                        leadingIcon = Icons.Filled.Lock,
                        placeholder = "Mínimo 8 caracteres",
                        isPassword = true,
                        errorMessage = newPasswordErr,
                        onFocusLost = { newPasswordErr = newPasswordError(newPassword) },
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    AuthTextField(
                        label = "Confirmar contraseña",
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            if (confirmPasswordErr != null) confirmPasswordErr = confirmPasswordError(it, newPassword)
                        },
                        leadingIcon = Icons.Filled.Lock,
                        placeholder = "Repite tu nueva contraseña",
                        isPassword = true,
                        errorMessage = confirmPasswordErr,
                        onFocusLost = { confirmPasswordErr = confirmPasswordError(confirmPassword, newPassword) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "¿No recibiste el código? Reenviar",
                color = BrandGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isResending) {
                        resendMessage = null
                        viewModel.requestCode(email)
                    },
            )
            if (resendMessage != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = resendMessage!!,
                    color = SubtitleGray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
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
                    viewModel.confirmReset(email, code, newPassword)
                },
                enabled = isFormValid && !isSubmitting,
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
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.height(18.dp).width(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Restableciendo…", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text(text = "Restablecer contraseña", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ResetPasswordScreenPreview() {
    ResetPasswordScreen(email = "usuario@ejemplo.com", onBack = {}, onResetSuccess = {})
}
