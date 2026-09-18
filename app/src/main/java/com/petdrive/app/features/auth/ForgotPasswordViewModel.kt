package com.petdrive.app.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petdrive.app.core.model.AuthResponse
import com.petdrive.app.core.model.PasswordResetConfirmRequest
import com.petdrive.app.core.model.PasswordResetRequestRequest
import com.petdrive.app.core.network.ApiClient
import com.petdrive.app.core.network.ApiEndpoints
import com.petdrive.app.core.network.ApiError
import com.petdrive.app.core.storage.TokenStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface RequestCodeUiState {
    data object Idle : RequestCodeUiState
    data object Loading : RequestCodeUiState
    data object Success : RequestCodeUiState
    data class Error(val message: String) : RequestCodeUiState
}

sealed interface ConfirmResetUiState {
    data object Idle : ConfirmResetUiState
    data object Loading : ConfirmResetUiState
    data object Success : ConfirmResetUiState
    data class Error(val message: String) : ConfirmResetUiState
}

class ForgotPasswordViewModel : ViewModel() {
    private val _requestState = MutableStateFlow<RequestCodeUiState>(RequestCodeUiState.Idle)
    val requestState: StateFlow<RequestCodeUiState> = _requestState.asStateFlow()

    private val _confirmState = MutableStateFlow<ConfirmResetUiState>(ConfirmResetUiState.Idle)
    val confirmState: StateFlow<ConfirmResetUiState> = _confirmState.asStateFlow()

    fun requestCode(email: String) {
        _requestState.value = RequestCodeUiState.Loading
        viewModelScope.launch {
            try {
                ApiClient.postForStatus(ApiEndpoints.PASSWORD_RESET_REQUEST, PasswordResetRequestRequest(email))
                _requestState.value = RequestCodeUiState.Success
            } catch (e: ApiError.ServerError) {
                _requestState.value = RequestCodeUiState.Error(e.errorMessage)
            } catch (e: ApiError.NetworkError) {
                _requestState.value = RequestCodeUiState.Error("No se pudo conectar. Comprueba tu conexión e inténtalo de nuevo.")
            } catch (e: ApiError) {
                _requestState.value = RequestCodeUiState.Error("No se pudo enviar el código. Inténtalo de nuevo.")
            }
        }
    }

    fun resetRequestState() {
        _requestState.value = RequestCodeUiState.Idle
    }

    // On success the backend returns a fresh auth token (same shape as login/register),
    // so a successful reset logs the user straight in rather than bouncing them back to
    // the login screen to type the password they just set.
    fun confirmReset(email: String, code: String, newPassword: String) {
        _confirmState.value = ConfirmResetUiState.Loading
        viewModelScope.launch {
            try {
                val response: AuthResponse = ApiClient.post(
                    ApiEndpoints.PASSWORD_RESET_CONFIRM,
                    PasswordResetConfirmRequest(email, code, newPassword),
                )
                TokenStore.token = response.token
                _confirmState.value = ConfirmResetUiState.Success
            } catch (e: ApiError.ServerError) {
                _confirmState.value = ConfirmResetUiState.Error(e.errorMessage)
            } catch (e: ApiError.NetworkError) {
                _confirmState.value = ConfirmResetUiState.Error("No se pudo conectar. Comprueba tu conexión e inténtalo de nuevo.")
            } catch (e: ApiError) {
                _confirmState.value = ConfirmResetUiState.Error("No se pudo restablecer la contraseña. Inténtalo de nuevo.")
            }
        }
    }

    fun resetConfirmState() {
        _confirmState.value = ConfirmResetUiState.Idle
    }
}
