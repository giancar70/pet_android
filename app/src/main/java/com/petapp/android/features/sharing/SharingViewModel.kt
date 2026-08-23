package com.petapp.android.features.sharing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petapp.android.core.model.CreatePetShareRequest
import com.petapp.android.core.model.PetShare
import com.petapp.android.core.network.ApiClient
import com.petapp.android.core.network.ApiEndpoints
import com.petapp.android.core.network.ApiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SharePetUiState {
    data object Idle : SharePetUiState
    data object Loading : SharePetUiState
    data class Success(val share: PetShare) : SharePetUiState
    data class Error(val message: String) : SharePetUiState
}

sealed interface SharesListUiState {
    data object Loading : SharesListUiState
    data class Loaded(val shares: List<PetShare>) : SharesListUiState
    data class Error(val message: String) : SharesListUiState
}

class SharingViewModel : ViewModel() {
    private val _shareState = MutableStateFlow<SharePetUiState>(SharePetUiState.Idle)
    val shareState: StateFlow<SharePetUiState> = _shareState.asStateFlow()

    private val _listState = MutableStateFlow<SharesListUiState>(SharesListUiState.Loading)
    val listState: StateFlow<SharesListUiState> = _listState.asStateFlow()

    fun sharePet(petId: String, email: String, role: String) {
        _shareState.value = SharePetUiState.Loading
        viewModelScope.launch {
            try {
                val request = CreatePetShareRequest(email = email, role = role)
                val share: PetShare = ApiClient.post(ApiEndpoints.petShare(petId), request)
                _shareState.value = SharePetUiState.Success(share)
                fetchShares(petId)
            } catch (e: ApiError.ServerError) {
                _shareState.value = SharePetUiState.Error(e.errorMessage)
            } catch (e: ApiError) {
                _shareState.value = SharePetUiState.Error(e.message ?: "No se pudo enviar la invitación.")
            }
        }
    }

    fun fetchShares(petId: String) {
        _listState.value = SharesListUiState.Loading
        viewModelScope.launch {
            try {
                val shares: List<PetShare> = ApiClient.get(ApiEndpoints.petShare(petId))
                _listState.value = SharesListUiState.Loaded(shares)
            } catch (e: ApiError.ServerError) {
                _listState.value = SharesListUiState.Error(e.errorMessage)
            } catch (e: ApiError) {
                _listState.value = SharesListUiState.Error(e.message ?: "No se pudieron cargar las personas con acceso.")
            }
        }
    }

    fun resetShareState() {
        _shareState.value = SharePetUiState.Idle
    }
}
