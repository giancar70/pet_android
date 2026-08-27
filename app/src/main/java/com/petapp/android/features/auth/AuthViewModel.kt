package com.petapp.android.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petapp.android.core.model.AuthResponse
import com.petapp.android.core.model.LoginRequest
import com.petapp.android.core.model.RegisterRequest
import com.petapp.android.core.network.ApiClient
import com.petapp.android.core.network.ApiEndpoints
import com.petapp.android.core.network.ApiError
import com.petapp.android.core.storage.TokenStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data object Success : AuthUiState
    /// Registration failed because the email is already in use -- kept distinct from
    /// `Error` so the screen can route it to the email field's own error text rather
    /// than a generic banner (CU02 §1).
    data object EmailTaken : AuthUiState
    data class Error(val message: String) : AuthUiState
}

class AuthViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /// Field-level validation lives in `LoginScreen` itself, same as `register()` below
    /// -- by the time this is called the "Iniciar sesión" button is only enabled once
    /// the whole form already validates, so this never needs to re-check that
    /// client-side (CU04).
    fun login(email: String, password: String) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                val response: AuthResponse = ApiClient.post(ApiEndpoints.LOGIN, LoginRequest(email, password))
                TokenStore.token = response.token
                _uiState.value = AuthUiState.Success
            } catch (e: ApiError.ServerError) {
                // The backend returns the same "Invalid credentials." message whether
                // the email is unregistered or the password is wrong, so any
                // server-side validation error on login maps to the one CU04-specified
                // string -- never the raw backend text.
                _uiState.value = AuthUiState.Error("El correo electrónico o la contraseña son incorrectos.")
            } catch (e: ApiError.NetworkError) {
                _uiState.value = AuthUiState.Error("No se pudo conectar. Comprueba tu conexión e inténtalo de nuevo.")
            } catch (e: ApiError) {
                _uiState.value = AuthUiState.Error("No se pudo iniciar sesión. Inténtalo de nuevo.")
            }
        }
    }

    /// Field-level validation (blank/format/length/match) lives in `RegisterScreen`
    /// itself, same as the rest of the form's inline errors -- by the time this is
    /// called the "Crear cuenta" button is only enabled once the whole form already
    /// validates, so this never needs to re-check that client-side.
    fun register(fullName: String, email: String, password: String) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                val response: AuthResponse = ApiClient.post(ApiEndpoints.REGISTER, RegisterRequest(fullName, email, password))
                TokenStore.token = response.token
                _uiState.value = AuthUiState.Success
            } catch (e: ApiError.ServerError) {
                _uiState.value = if (e.errorMessage.contains("already registered", ignoreCase = true)) {
                    AuthUiState.EmailTaken
                } else {
                    AuthUiState.Error("No se pudo crear la cuenta. Inténtalo de nuevo.")
                }
            } catch (e: ApiError.NetworkError) {
                _uiState.value = AuthUiState.Error("No se pudo conectar. Comprueba tu conexión e inténtalo de nuevo.")
            } catch (e: ApiError) {
                _uiState.value = AuthUiState.Error("No se pudo crear la cuenta. Inténtalo de nuevo.")
            }
        }
    }

    fun reset() {
        _uiState.value = AuthUiState.Idle
    }

}
