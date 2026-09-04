package com.petapp.android.features.vaccines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petapp.android.core.model.CreateVaccineDoseRequest
import com.petapp.android.core.model.VaccineDose
import com.petapp.android.core.network.ApiClient
import com.petapp.android.core.network.ApiEndpoints
import com.petapp.android.core.network.ApiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CreateVaccineDoseUiState {
    data object Idle : CreateVaccineDoseUiState
    data object Loading : CreateVaccineDoseUiState
    data class Success(val dose: VaccineDose) : CreateVaccineDoseUiState
    data class Error(val message: String) : CreateVaccineDoseUiState
}

sealed interface VaccinesListUiState {
    data object Loading : VaccinesListUiState
    data class Loaded(val doses: List<VaccineDose>) : VaccinesListUiState
    data class Error(val message: String) : VaccinesListUiState
}

sealed interface VaccineDoseDetailUiState {
    data object Loading : VaccineDoseDetailUiState
    data class Loaded(val dose: VaccineDose) : VaccineDoseDetailUiState
    data class Error(val message: String) : VaccineDoseDetailUiState
}

sealed interface DeleteVaccineDoseUiState {
    data object Idle : DeleteVaccineDoseUiState
    data object Loading : DeleteVaccineDoseUiState
    data object Success : DeleteVaccineDoseUiState
    data class Error(val message: String) : DeleteVaccineDoseUiState
}

class VaccinesViewModel : ViewModel() {
    private val _createState = MutableStateFlow<CreateVaccineDoseUiState>(CreateVaccineDoseUiState.Idle)
    val createState: StateFlow<CreateVaccineDoseUiState> = _createState.asStateFlow()

    private val _listState = MutableStateFlow<VaccinesListUiState>(VaccinesListUiState.Loading)
    val listState: StateFlow<VaccinesListUiState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow<VaccineDoseDetailUiState>(VaccineDoseDetailUiState.Loading)
    val detailState: StateFlow<VaccineDoseDetailUiState> = _detailState.asStateFlow()

    private val _deleteState = MutableStateFlow<DeleteVaccineDoseUiState>(DeleteVaccineDoseUiState.Idle)
    val deleteState: StateFlow<DeleteVaccineDoseUiState> = _deleteState.asStateFlow()

    // Tracks which pet's list is currently held in _listState so fetchVacunas can skip
    // a redundant network call when nothing has changed (e.g. re-entering Inicio after
    // navigating to an unrelated screen) -- see fetchVacunas.
    private var lastFetchedPetId: String? = null

    fun fetchVacunaDetail(petId: String, doseId: String) {
        _detailState.value = VaccineDoseDetailUiState.Loading
        viewModelScope.launch {
            try {
                val dose: VaccineDose = ApiClient.get(ApiEndpoints.petVaccineDoseDetail(petId, doseId))
                _detailState.value = VaccineDoseDetailUiState.Loaded(dose)
            } catch (e: ApiError.ServerError) {
                _detailState.value = VaccineDoseDetailUiState.Error(e.errorMessage)
            } catch (e: ApiError) {
                _detailState.value = VaccineDoseDetailUiState.Error(e.message ?: "No se pudo cargar la vacuna.")
            }
        }
    }

    // Blocks not affected by an unrelated navigation shouldn't refetch: if this pet's
    // list is already loaded, re-entering the screen is a no-op instead of a fresh
    // network round-trip. createVacuna/deleteVacuna keep this list in sync locally on
    // success, so a real change is reflected without needing to invalidate the cache.
    fun fetchVacunas(petId: String, forceRefresh: Boolean = false) {
        if (!forceRefresh && petId == lastFetchedPetId && _listState.value is VaccinesListUiState.Loaded) {
            return
        }
        lastFetchedPetId = petId
        if (_listState.value !is VaccinesListUiState.Loaded) {
            _listState.value = VaccinesListUiState.Loading
        }
        viewModelScope.launch {
            try {
                val doses: List<VaccineDose> = ApiClient.get(ApiEndpoints.petVaccineDoses(petId))
                _listState.value = VaccinesListUiState.Loaded(doses)
            } catch (e: ApiError.ServerError) {
                _listState.value = VaccinesListUiState.Error(e.errorMessage)
            } catch (e: ApiError) {
                _listState.value = VaccinesListUiState.Error(e.message ?: "No se pudieron cargar las vacunas.")
            }
        }
    }

    fun createVacuna(
        petId: String,
        vaccineName: String,
        appliedOnIso: String,
        nextDueOnIso: String?,
        lotNumber: String?,
        notes: String?,
    ) {
        _createState.value = CreateVaccineDoseUiState.Loading
        viewModelScope.launch {
            try {
                val request = CreateVaccineDoseRequest(
                    vaccineName = vaccineName,
                    appliedOn = appliedOnIso,
                    nextDueOn = nextDueOnIso,
                    lotNumber = lotNumber?.takeIf { it.isNotBlank() },
                    notes = notes?.takeIf { it.isNotBlank() },
                )
                val dose: VaccineDose = ApiClient.post(ApiEndpoints.petVaccineDoses(petId), request)
                _createState.value = CreateVaccineDoseUiState.Success(dose)
                // Updates just this block locally instead of refetching the whole list.
                (_listState.value as? VaccinesListUiState.Loaded)?.let {
                    _listState.value = VaccinesListUiState.Loaded(listOf(dose) + it.doses)
                }
            } catch (e: ApiError.ServerError) {
                _createState.value = CreateVaccineDoseUiState.Error(e.errorMessage)
            } catch (e: ApiError) {
                _createState.value = CreateVaccineDoseUiState.Error(e.message ?: "No se pudo guardar la vacuna.")
            }
        }
    }

    fun resetCreateState() {
        _createState.value = CreateVaccineDoseUiState.Idle
    }

    // Soft-delete: DELETE flips the row's `deleted` flag on the backend rather than
    // removing it, scoped to this one dose -- other vaccines/pets are untouched.
    fun deleteVacuna(petId: String, doseId: String) {
        _deleteState.value = DeleteVaccineDoseUiState.Loading
        viewModelScope.launch {
            try {
                ApiClient.delete(ApiEndpoints.petVaccineDoseDetail(petId, doseId))
                _deleteState.value = DeleteVaccineDoseUiState.Success
                // Updates just this block locally instead of refetching the whole list.
                (_listState.value as? VaccinesListUiState.Loaded)?.let {
                    _listState.value = VaccinesListUiState.Loaded(it.doses.filterNot { dose -> dose.id == doseId })
                }
            } catch (e: ApiError.ServerError) {
                _deleteState.value = DeleteVaccineDoseUiState.Error(e.errorMessage)
            } catch (e: ApiError) {
                _deleteState.value = DeleteVaccineDoseUiState.Error(e.message ?: "No se pudo eliminar la vacuna.")
            }
        }
    }

    fun resetDeleteState() {
        _deleteState.value = DeleteVaccineDoseUiState.Idle
    }
}
