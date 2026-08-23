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
    data class Error(val message: String) : AuthUiState
}

class AuthViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Completa todos los campos.")
            return
        }
        runAuthCall { ApiClient.post(ApiEndpoints.LOGIN, LoginRequest(email, password)) }
    }

    fun register(fullName: String, email: String, password: String, confirmPassword: String) {
        if (fullName.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            _uiState.value = AuthUiState.Error("Completa todos los campos.")
            return
        }
        if (password != confirmPassword) {
            _uiState.value = AuthUiState.Error("Las contraseñas no coinciden.")
            return
        }
        runAuthCall { ApiClient.post(ApiEndpoints.REGISTER, RegisterRequest(fullName, email, password)) }
    }

    private fun runAuthCall(call: suspend () -> AuthResponse) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                val response = call()
                TokenStore.token = response.token
                _uiState.value = AuthUiState.Success
            } catch (e: ApiError.ServerError) {
                _uiState.value = AuthUiState.Error(e.errorMessage)
            } catch (e: ApiError) {
                _uiState.value = AuthUiState.Error(e.message ?: "Algo salió mal.")
            }
        }
    }
}
