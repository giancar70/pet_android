package com.petapp.android.features.consultations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petapp.android.core.model.Consultation
import com.petapp.android.core.model.CreateConsultationRequest
import com.petapp.android.core.network.ApiClient
import com.petapp.android.core.network.ApiEndpoints
import com.petapp.android.core.network.ApiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CreateConsultaUiState {
    data object Idle : CreateConsultaUiState
    data object Loading : CreateConsultaUiState
    data class Success(val consultation: Consultation) : CreateConsultaUiState
    data class Error(val message: String) : CreateConsultaUiState
}

sealed interface ConsultasListUiState {
    data object Loading : ConsultasListUiState
    data class Loaded(val consultations: List<Consultation>) : ConsultasListUiState
    data class Error(val message: String) : ConsultasListUiState
}

class ConsultationsViewModel : ViewModel() {
    private val _createState = MutableStateFlow<CreateConsultaUiState>(CreateConsultaUiState.Idle)
    val createState: StateFlow<CreateConsultaUiState> = _createState.asStateFlow()

    private val _listState = MutableStateFlow<ConsultasListUiState>(ConsultasListUiState.Loading)
    val listState: StateFlow<ConsultasListUiState> = _listState.asStateFlow()

    fun fetchConsultas(petId: String) {
        _listState.value = ConsultasListUiState.Loading
        viewModelScope.launch {
            try {
                val consultations: List<Consultation> = ApiClient.get(ApiEndpoints.petConsultations(petId))
                _listState.value = ConsultasListUiState.Loaded(consultations)
            } catch (e: ApiError.ServerError) {
                _listState.value = ConsultasListUiState.Error(e.errorMessage)
            } catch (e: ApiError) {
                _listState.value = ConsultasListUiState.Error(e.message ?: "No se pudieron cargar las consultas.")
            }
        }
    }

    fun createConsulta(
        petId: String,
        motivo: String,
        consultDateIso: String,
        sintomas: String?,
        hallazgos: String?,
        diagnostico: String?,
        tratamiento: String?,
        clinicaVeterinario: String?,
    ) {
        _createState.value = CreateConsultaUiState.Loading
        viewModelScope.launch {
            try {
                val request = CreateConsultationRequest(
                    reason = motivo,
                    consultDate = consultDateIso,
                    symptoms = sintomas?.takeIf { it.isNotBlank() },
                    physicalExamFindings = hallazgos?.takeIf { it.isNotBlank() },
                    diagnosis = diagnostico?.takeIf { it.isNotBlank() },
                    treatment = tratamiento?.takeIf { it.isNotBlank() },
                    clinicName = clinicaVeterinario?.takeIf { it.isNotBlank() },
                )
                val consultation: Consultation = ApiClient.post(ApiEndpoints.petConsultations(petId), request)
                _createState.value = CreateConsultaUiState.Success(consultation)
            } catch (e: ApiError.ServerError) {
                _createState.value = CreateConsultaUiState.Error(e.errorMessage)
            } catch (e: ApiError) {
                _createState.value = CreateConsultaUiState.Error(e.message ?: "No se pudo guardar la consulta.")
            }
        }
    }

    fun resetCreateState() {
        _createState.value = CreateConsultaUiState.Idle
    }
}
