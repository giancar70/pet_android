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

class DewormingViewModel : ViewModel() {
    private val _createState = MutableStateFlow<CreateDewormingUiState>(CreateDewormingUiState.Idle)
    val createState: StateFlow<CreateDewormingUiState> = _createState.asStateFlow()

    private val _listState = MutableStateFlow<DewormingListUiState>(DewormingListUiState.Loading)
    val listState: StateFlow<DewormingListUiState> = _listState.asStateFlow()

    fun fetchDesparasitaciones(petId: String) {
        _listState.value = DewormingListUiState.Loading
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
        administrationRoute: String?,
        presentation: String?,
        productName: String?,
        weightKg: String?,
        notes: String?,
    ) {
        _createState.value = CreateDewormingUiState.Loading
        viewModelScope.launch {
            try {
                val request = CreateDewormingApplicationRequest(
                    dewormingType = dewormingType,
                    appliedOn = appliedOnIso,
                    administrationRoute = administrationRoute,
                    presentation = presentation,
                    productName = productName?.takeIf { it.isNotBlank() },
                    weightKgAtApplication = weightKg?.takeIf { it.isNotBlank() },
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
}
