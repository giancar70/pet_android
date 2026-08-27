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

class VaccinesViewModel : ViewModel() {
    private val _createState = MutableStateFlow<CreateVaccineDoseUiState>(CreateVaccineDoseUiState.Idle)
    val createState: StateFlow<CreateVaccineDoseUiState> = _createState.asStateFlow()

    private val _listState = MutableStateFlow<VaccinesListUiState>(VaccinesListUiState.Loading)
    val listState: StateFlow<VaccinesListUiState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow<VaccineDoseDetailUiState>(VaccineDoseDetailUiState.Loading)
    val detailState: StateFlow<VaccineDoseDetailUiState> = _detailState.asStateFlow()

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

    fun fetchVacunas(petId: String) {
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
        lotNumber: String?,
        notes: String?,
    ) {
        _createState.value = CreateVaccineDoseUiState.Loading
        viewModelScope.launch {
            try {
                val request = CreateVaccineDoseRequest(
                    vaccineName = vaccineName,
                    appliedOn = appliedOnIso,
                    lotNumber = lotNumber?.takeIf { it.isNotBlank() },
                    notes = notes?.takeIf { it.isNotBlank() },
                )
                val dose: VaccineDose = ApiClient.post(ApiEndpoints.petVaccineDoses(petId), request)
                _createState.value = CreateVaccineDoseUiState.Success(dose)
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
}
