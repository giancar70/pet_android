package com.petapp.android.features.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petapp.android.core.model.UpdateUserRequest
import com.petapp.android.core.model.User
import com.petapp.android.core.network.ApiClient
import com.petapp.android.core.network.ApiEndpoints
import com.petapp.android.core.network.ApiError
import com.petapp.android.core.storage.TokenStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface UserUiState {
    data object Loading : UserUiState
    data class Loaded(val user: User) : UserUiState
    data class Error(val message: String) : UserUiState
}

sealed interface UpdateUserUiState {
    data object Idle : UpdateUserUiState
    data object Loading : UpdateUserUiState
    data class Success(val user: User) : UpdateUserUiState
    data class Error(val message: String) : UpdateUserUiState
}

sealed interface DeleteAccountUiState {
    data object Idle : DeleteAccountUiState
    data object Loading : DeleteAccountUiState
    data object Success : DeleteAccountUiState
    data class Error(val message: String) : DeleteAccountUiState
}

class UserViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<UserUiState>(UserUiState.Loading)
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    private val _updateState = MutableStateFlow<UpdateUserUiState>(UpdateUserUiState.Idle)
    val updateState: StateFlow<UpdateUserUiState> = _updateState.asStateFlow()

    private val _deleteAccountState = MutableStateFlow<DeleteAccountUiState>(DeleteAccountUiState.Idle)
    val deleteAccountState: StateFlow<DeleteAccountUiState> = _deleteAccountState.asStateFlow()

    init {
        loadUser()
    }

    fun loadUser() {
        viewModelScope.launch {
            try {
                val user: User = ApiClient.get(ApiEndpoints.USER)
                _uiState.value = UserUiState.Loaded(user)
            } catch (e: ApiError.ServerError) {
                _uiState.value = UserUiState.Error(e.errorMessage)
            } catch (e: ApiError) {
                _uiState.value = UserUiState.Error(e.message ?: "No se pudo cargar el usuario.")
            }
        }
    }

    // Clears the token and returns immediately rather than waiting on the network
    // round-trip -- the caller navigates to Login right away. The LOGOUT call itself
    // is best-effort (server-side token invalidation) and fires in the background;
    // its result doesn't affect the local logout, which is already complete once this
    // function returns.
    fun logout() {
        val capturedToken = TokenStore.token
        TokenStore.token = null
        viewModelScope.launch {
            if (capturedToken != null) {
                runCatching { ApiClient.postEmpty(ApiEndpoints.LOGOUT, capturedToken) }
            }
        }
    }

    fun updateProfile(
        fullName: String? = null,
        phoneNumber: String? = null,
        notifyVaccineReminders: Boolean? = null,
        notifyDewormingReminders: Boolean? = null,
        notifyAppointmentReminders: Boolean? = null,
        notifyPetShared: Boolean? = null,
        notifyDocumentUploaded: Boolean? = null,
        notifyViaPush: Boolean? = null,
        notifyViaEmail: Boolean? = null,
    ) {
        _updateState.value = UpdateUserUiState.Loading
        viewModelScope.launch {
            try {
                val request = UpdateUserRequest(
                    fullName = fullName,
                    phoneNumber = phoneNumber,
                    notifyVaccineReminders = notifyVaccineReminders,
                    notifyDewormingReminders = notifyDewormingReminders,
                    notifyAppointmentReminders = notifyAppointmentReminders,
                    notifyPetShared = notifyPetShared,
                    notifyDocumentUploaded = notifyDocumentUploaded,
                    notifyViaPush = notifyViaPush,
                    notifyViaEmail = notifyViaEmail,
                )
                val user: User = ApiClient.patch(ApiEndpoints.USER, request)
                _updateState.value = UpdateUserUiState.Success(user)
                _uiState.value = UserUiState.Loaded(user)
            } catch (e: ApiError.ServerError) {
                _updateState.value = UpdateUserUiState.Error(e.errorMessage)
            } catch (e: ApiError) {
                _updateState.value = UpdateUserUiState.Error(e.message ?: "No se pudo actualizar tu perfil.")
            }
        }
    }

    fun resetUpdateState() {
        _updateState.value = UpdateUserUiState.Idle
    }

    fun deleteAccount() {
        _deleteAccountState.value = DeleteAccountUiState.Loading
        viewModelScope.launch {
            try {
                ApiClient.delete(ApiEndpoints.USER)
                TokenStore.token = null
                _deleteAccountState.value = DeleteAccountUiState.Success
            } catch (e: ApiError.ServerError) {
                _deleteAccountState.value = DeleteAccountUiState.Error(e.errorMessage)
            } catch (e: ApiError) {
                _deleteAccountState.value = DeleteAccountUiState.Error(e.message ?: "No se pudo eliminar la cuenta.")
            }
        }
    }

    fun resetDeleteAccountState() {
        _deleteAccountState.value = DeleteAccountUiState.Idle
    }

    fun reset() {
        _uiState.value = UserUiState.Loading
        _updateState.value = UpdateUserUiState.Idle
        _deleteAccountState.value = DeleteAccountUiState.Idle
    }
}
