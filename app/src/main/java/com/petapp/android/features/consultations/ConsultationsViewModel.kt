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

sealed interface ConsultaDetailUiState {
    data object Loading : ConsultaDetailUiState
    data class Loaded(val consultation: Consultation) : ConsultaDetailUiState
    data class Error(val message: String) : ConsultaDetailUiState
}

sealed interface DeleteConsultaUiState {
    data object Idle : DeleteConsultaUiState
    data object Loading : DeleteConsultaUiState
    data object Success : DeleteConsultaUiState
    data class Error(val message: String) : DeleteConsultaUiState
}

class ConsultationsViewModel : ViewModel() {
    private val _createState = MutableStateFlow<CreateConsultaUiState>(CreateConsultaUiState.Idle)
    val createState: StateFlow<CreateConsultaUiState> = _createState.asStateFlow()

    private val _listState = MutableStateFlow<ConsultasListUiState>(ConsultasListUiState.Loading)
    val listState: StateFlow<ConsultasListUiState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow<ConsultaDetailUiState>(ConsultaDetailUiState.Loading)
    val detailState: StateFlow<ConsultaDetailUiState> = _detailState.asStateFlow()

    private val _deleteState = MutableStateFlow<DeleteConsultaUiState>(DeleteConsultaUiState.Idle)
    val deleteState: StateFlow<DeleteConsultaUiState> = _deleteState.asStateFlow()

    // Tracks which pet's list is currently held in _listState so fetchConsultas can
    // skip a redundant network call when nothing has changed -- see that function.
    private var lastFetchedPetId: String? = null

    fun fetchConsultaDetail(petId: String, consultationId: String) {
        _detailState.value = ConsultaDetailUiState.Loading
        viewModelScope.launch {
            try {
                val consultation: Consultation = ApiClient.get(ApiEndpoints.petConsultationDetail(petId, consultationId))
                _detailState.value = ConsultaDetailUiState.Loaded(consultation)
            } catch (e: ApiError.ServerError) {
                _detailState.value = ConsultaDetailUiState.Error(e.errorMessage)
            } catch (e: ApiError) {
                _detailState.value = ConsultaDetailUiState.Error(e.message ?: "No se pudo cargar la consulta.")
            }
        }
    }

    // Blocks not affected by an unrelated navigation shouldn't refetch: if this pet's
    // list is already loaded, re-entering the screen is a no-op instead of a fresh
    // network round-trip. createConsulta/deleteConsulta keep this list in sync locally
    // on success, so a real change is reflected without invalidating the cache.
    fun fetchConsultas(petId: String, forceRefresh: Boolean = false) {
        if (!forceRefresh && petId == lastFetchedPetId && _listState.value is ConsultasListUiState.Loaded) {
            return
        }
        lastFetchedPetId = petId
        if (_listState.value !is ConsultasListUiState.Loaded) {
            _listState.value = ConsultasListUiState.Loading
        }
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
                // Updates just this block locally instead of refetching the whole list.
                (_listState.value as? ConsultasListUiState.Loaded)?.let {
                    _listState.value = ConsultasListUiState.Loaded(listOf(consultation) + it.consultations)
                }
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

    // Soft-delete: DELETE flips the row's `deleted` flag on the backend rather than
    // removing it, scoped to this one consulta -- other records/pets are untouched.
    fun deleteConsulta(petId: String, consultationId: String) {
        _deleteState.value = DeleteConsultaUiState.Loading
        viewModelScope.launch {
            try {
                ApiClient.delete(ApiEndpoints.petConsultationDetail(petId, consultationId))
                _deleteState.value = DeleteConsultaUiState.Success
                // Updates just this block locally instead of refetching the whole list.
                (_listState.value as? ConsultasListUiState.Loaded)?.let {
                    _listState.value = ConsultasListUiState.Loaded(it.consultations.filterNot { c -> c.id == consultationId })
                }
            } catch (e: ApiError.ServerError) {
                _deleteState.value = DeleteConsultaUiState.Error(e.errorMessage)
            } catch (e: ApiError) {
                _deleteState.value = DeleteConsultaUiState.Error(e.message ?: "No se pudo eliminar la consulta.")
            }
        }
    }

    fun resetDeleteState() {
        _deleteState.value = DeleteConsultaUiState.Idle
    }
}
