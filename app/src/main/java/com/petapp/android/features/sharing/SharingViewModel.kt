package com.petapp.android.features.sharing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petapp.android.core.model.CreatePetShareRequest
import com.petapp.android.core.model.PetShare
import com.petapp.android.core.model.UpdatePetShareRequest
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

sealed interface UpdateShareUiState {
    data object Idle : UpdateShareUiState
    data object Loading : UpdateShareUiState
    data object Success : UpdateShareUiState
    data class Error(val message: String) : UpdateShareUiState
}

sealed interface DeleteShareUiState {
    data object Idle : DeleteShareUiState
    data object Loading : DeleteShareUiState
    data object Success : DeleteShareUiState
    data class Error(val message: String) : DeleteShareUiState
}

class SharingViewModel : ViewModel() {
    private val _shareState = MutableStateFlow<SharePetUiState>(SharePetUiState.Idle)
    val shareState: StateFlow<SharePetUiState> = _shareState.asStateFlow()

    private val _listState = MutableStateFlow<SharesListUiState>(SharesListUiState.Loading)
    val listState: StateFlow<SharesListUiState> = _listState.asStateFlow()

    // Separate from `listState` -- the cross-pet "Invitaciones" screen (Más) and the
    // per-pet "Personas con acceso" section (Compartir Mascota) are different lists
    // shown on different screens, so sharing one StateFlow would let one clobber the
    // other's data on re-entry.
    private val _allSharesState = MutableStateFlow<SharesListUiState>(SharesListUiState.Loading)
    val allSharesState: StateFlow<SharesListUiState> = _allSharesState.asStateFlow()

    private val _updateState = MutableStateFlow<UpdateShareUiState>(UpdateShareUiState.Idle)
    val updateState: StateFlow<UpdateShareUiState> = _updateState.asStateFlow()

    private val _deleteState = MutableStateFlow<DeleteShareUiState>(DeleteShareUiState.Idle)
    val deleteState: StateFlow<DeleteShareUiState> = _deleteState.asStateFlow()

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

    fun fetchAllShares() {
        _allSharesState.value = SharesListUiState.Loading
        viewModelScope.launch {
            try {
                val shares: List<PetShare> = ApiClient.get(ApiEndpoints.PET_SHARES_ALL)
                _allSharesState.value = SharesListUiState.Loaded(shares)
            } catch (e: ApiError.ServerError) {
                _allSharesState.value = SharesListUiState.Error(e.errorMessage)
            } catch (e: ApiError) {
                _allSharesState.value = SharesListUiState.Error(e.message ?: "No se pudieron cargar las invitaciones.")
            }
        }
    }

    fun updateShareRole(shareId: String, role: String) {
        _updateState.value = UpdateShareUiState.Loading
        viewModelScope.launch {
            try {
                val updated: PetShare = ApiClient.patch(ApiEndpoints.petShareDetail(shareId), UpdatePetShareRequest(role))
                _updateState.value = UpdateShareUiState.Success
                // Updates just this block locally instead of refetching the whole list.
                (_allSharesState.value as? SharesListUiState.Loaded)?.let {
                    _allSharesState.value = SharesListUiState.Loaded(it.shares.map { s -> if (s.id == updated.id) updated else s })
                }
            } catch (e: ApiError.ServerError) {
                _updateState.value = UpdateShareUiState.Error(e.errorMessage)
            } catch (e: ApiError) {
                _updateState.value = UpdateShareUiState.Error(e.message ?: "No se pudo actualizar el permiso.")
            }
        }
    }

    // Soft-delete: DELETE flips the row's `deleted` flag on the backend rather than
    // removing it, scoped to this one share/invitation -- other rows are untouched.
    fun deleteShare(shareId: String) {
        _deleteState.value = DeleteShareUiState.Loading
        viewModelScope.launch {
            try {
                ApiClient.delete(ApiEndpoints.petShareDetail(shareId))
                _deleteState.value = DeleteShareUiState.Success
                (_allSharesState.value as? SharesListUiState.Loaded)?.let {
                    _allSharesState.value = SharesListUiState.Loaded(it.shares.filterNot { s -> s.id == shareId })
                }
            } catch (e: ApiError.ServerError) {
                _deleteState.value = DeleteShareUiState.Error(e.errorMessage)
            } catch (e: ApiError) {
                _deleteState.value = DeleteShareUiState.Error(e.message ?: "No se pudo eliminar la invitación.")
            }
        }
    }

    fun resetShareState() {
        _shareState.value = SharePetUiState.Idle
    }

    fun resetUpdateState() {
        _updateState.value = UpdateShareUiState.Idle
    }

    fun resetDeleteState() {
        _deleteState.value = DeleteShareUiState.Idle
    }
}
