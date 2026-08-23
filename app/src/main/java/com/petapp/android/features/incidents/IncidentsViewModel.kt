package com.petapp.android.features.incidents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petapp.android.core.model.CreatePetEventRequest
import com.petapp.android.core.model.PetEvent
import com.petapp.android.core.network.ApiClient
import com.petapp.android.core.network.ApiEndpoints
import com.petapp.android.core.network.ApiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CreateEventUiState {
    data object Idle : CreateEventUiState
    data object Loading : CreateEventUiState
    data class Success(val event: PetEvent) : CreateEventUiState
    data class Error(val message: String) : CreateEventUiState
}

sealed interface IncidenciasListUiState {
    data object Loading : IncidenciasListUiState
    data class Loaded(val events: List<PetEvent>) : IncidenciasListUiState
    data class Error(val message: String) : IncidenciasListUiState
}

class IncidentsViewModel : ViewModel() {
    private val _createState = MutableStateFlow<CreateEventUiState>(CreateEventUiState.Idle)
    val createState: StateFlow<CreateEventUiState> = _createState.asStateFlow()

    private val _listState = MutableStateFlow<IncidenciasListUiState>(IncidenciasListUiState.Loading)
    val listState: StateFlow<IncidenciasListUiState> = _listState.asStateFlow()

    fun fetchIncidencias(petId: String) {
        _listState.value = IncidenciasListUiState.Loading
        viewModelScope.launch {
            try {
                val events: List<PetEvent> = ApiClient.get(ApiEndpoints.petEvents(petId))
                _listState.value = IncidenciasListUiState.Loaded(events)
            } catch (e: ApiError.ServerError) {
                _listState.value = IncidenciasListUiState.Error(e.errorMessage)
            } catch (e: ApiError) {
                _listState.value = IncidenciasListUiState.Error(e.message ?: "No se pudieron cargar las incidencias.")
            }
        }
    }

    fun createIncidencia(petId: String, title: String, description: String?, eventDateIso: String) {
        _createState.value = CreateEventUiState.Loading
        viewModelScope.launch {
            try {
                val request = CreatePetEventRequest(
                    title = title,
                    description = description?.takeIf { it.isNotBlank() },
                    eventDate = eventDateIso,
                )
                val event: PetEvent = ApiClient.post(ApiEndpoints.petEvents(petId), request)
                _createState.value = CreateEventUiState.Success(event)
            } catch (e: ApiError.ServerError) {
                _createState.value = CreateEventUiState.Error(e.errorMessage)
            } catch (e: ApiError) {
                _createState.value = CreateEventUiState.Error(e.message ?: "No se pudo guardar la incidencia.")
            }
        }
    }

    fun resetCreateState() {
        _createState.value = CreateEventUiState.Idle
    }
}
