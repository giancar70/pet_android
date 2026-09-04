package com.petapp.android.features.deworming

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petapp.android.core.model.CreateDewormingApplicationRequest
import com.petapp.android.core.model.DewormingApplication
import com.petapp.android.core.network.ApiClient
import com.petapp.android.core.network.ApiEndpoints
import com.petapp.android.core.network.ApiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CreateDewormingUiState {
    data object Idle : CreateDewormingUiState
    data object Loading : CreateDewormingUiState
    data class Success(val application: DewormingApplication) : CreateDewormingUiState
    data class Error(val message: String) : CreateDewormingUiState
}

sealed interface DewormingListUiState {
    data object Loading : DewormingListUiState
    data class Loaded(val applications: List<DewormingApplication>) : DewormingListUiState
    data class Error(val message: String) : DewormingListUiState
}

sealed interface DewormingDetailUiState {
    data object Loading : DewormingDetailUiState
    data class Loaded(val application: DewormingApplication) : DewormingDetailUiState
    data class Error(val message: String) : DewormingDetailUiState
}

sealed interface DeleteDewormingUiState {
    data object Idle : DeleteDewormingUiState
    data object Loading : DeleteDewormingUiState
    data object Success : DeleteDewormingUiState
    data class Error(val message: String) : DeleteDewormingUiState
}

class DewormingViewModel : ViewModel() {
    private val _createState = MutableStateFlow<CreateDewormingUiState>(CreateDewormingUiState.Idle)
    val createState: StateFlow<CreateDewormingUiState> = _createState.asStateFlow()

    private val _listState = MutableStateFlow<DewormingListUiState>(DewormingListUiState.Loading)
    val listState: StateFlow<DewormingListUiState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow<DewormingDetailUiState>(DewormingDetailUiState.Loading)
    val detailState: StateFlow<DewormingDetailUiState> = _detailState.asStateFlow()

    private val _deleteState = MutableStateFlow<DeleteDewormingUiState>(DeleteDewormingUiState.Idle)
    val deleteState: StateFlow<DeleteDewormingUiState> = _deleteState.asStateFlow()

    fun fetchDesparasitacionDetail(petId: String, applicationId: String) {
        _detailState.value = DewormingDetailUiState.Loading
        viewModelScope.launch {
            try {
                val application: DewormingApplication =
                    ApiClient.get(ApiEndpoints.petDewormingApplicationDetail(petId, applicationId))
                _detailState.value = DewormingDetailUiState.Loaded(application)
            } catch (e: ApiError.ServerError) {
                _detailState.value = DewormingDetailUiState.Error(e.errorMessage)
            } catch (e: ApiError) {
                _detailState.value = DewormingDetailUiState.Error(e.message ?: "No se pudo cargar la desparasitación.")
            }
        }
    }

    fun fetchDesparasitaciones(petId: String) {
        if (_listState.value !is DewormingListUiState.Loaded) {
            _listState.value = DewormingListUiState.Loading
        }
        viewModelScope.launch {
            try {
                val applications: List<DewormingApplication> =
                    ApiClient.get(ApiEndpoints.petDewormingApplications(petId))
                _listState.value = DewormingListUiState.Loaded(applications)
            } catch (e: ApiError.ServerError) {
                _listState.value = DewormingListUiState.Error(e.errorMessage)
            } catch (e: ApiError) {
                _listState.value = DewormingListUiState.Error(e.message ?: "No se pudieron cargar las desparasitaciones.")
            }
        }
    }

    fun createDesparasitacion(
        petId: String,
        dewormingType: String,
        appliedOnIso: String,
        nextDueOnIso: String?,
        durationMonths: Int?,
        productName: String?,
        notes: String?,
    ) {
        _createState.value = CreateDewormingUiState.Loading
        viewModelScope.launch {
            try {
                val request = CreateDewormingApplicationRequest(
                    dewormingType = dewormingType,
                    appliedOn = appliedOnIso,
                    nextDueOn = nextDueOnIso,
                    durationMonths = durationMonths,
                    productName = productName?.takeIf { it.isNotBlank() },
                    notes = notes?.takeIf { it.isNotBlank() },
                )
                val application: DewormingApplication =
                    ApiClient.post(ApiEndpoints.petDewormingApplications(petId), request)
                _createState.value = CreateDewormingUiState.Success(application)
            } catch (e: ApiError.ServerError) {
                _createState.value = CreateDewormingUiState.Error(e.errorMessage)
            } catch (e: ApiError) {
                _createState.value = CreateDewormingUiState.Error(e.message ?: "No se pudo guardar la desparasitación.")
            }
        }
    }

    fun resetCreateState() {
        _createState.value = CreateDewormingUiState.Idle
    }

    // Soft-delete: DELETE flips the row's `deleted` flag on the backend rather than
    // removing it, scoped to this one application -- other records/pets are untouched.
    fun deleteDesparasitacion(petId: String, applicationId: String) {
        _deleteState.value = DeleteDewormingUiState.Loading
        viewModelScope.launch {
            try {
                ApiClient.delete(ApiEndpoints.petDewormingApplicationDetail(petId, applicationId))
                _deleteState.value = DeleteDewormingUiState.Success
            } catch (e: ApiError.ServerError) {
                _deleteState.value = DeleteDewormingUiState.Error(e.errorMessage)
            } catch (e: ApiError) {
                _deleteState.value = DeleteDewormingUiState.Error(e.message ?: "No se pudo eliminar la desparasitación.")
            }
        }
    }

    fun resetDeleteState() {
        _deleteState.value = DeleteDewormingUiState.Idle
    }
}
