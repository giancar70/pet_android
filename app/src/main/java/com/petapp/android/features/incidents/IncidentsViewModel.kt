package com.petapp.android.features.incidents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petapp.android.core.model.CreatePetEventRequest
import com.petapp.android.core.model.Document
import com.petapp.android.core.model.PetEvent
import com.petapp.android.core.network.ApiClient
import com.petapp.android.core.network.ApiEndpoints
import com.petapp.android.core.network.ApiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EvidenceFile(val bytes: ByteArray, val fileName: String, val mimeType: String)

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

    fun createIncidencia(
        petId: String,
        title: String,
        description: String?,
        eventDateIso: String,
        photo: EvidenceFile? = null,
        document: EvidenceFile? = null,
    ) {
        _createState.value = CreateEventUiState.Loading
        viewModelScope.launch {
            try {
                val request = CreatePetEventRequest(
                    title = title,
                    description = description?.takeIf { it.isNotBlank() },
                    eventDate = eventDateIso,
                )
                val event: PetEvent = ApiClient.post(ApiEndpoints.petEvents(petId), request)
                listOfNotNull(photo, document).forEach { evidence -> uploadEvidence(petId, event.id, evidence) }
                _createState.value = CreateEventUiState.Success(event)
            } catch (e: ApiError.ServerError) {
                _createState.value = CreateEventUiState.Error(e.errorMessage)
            } catch (e: ApiError) {
                _createState.value = CreateEventUiState.Error(e.message ?: "No se pudo guardar la incidencia.")
            }
        }
    }

    // The incidencia itself is already saved by the time this runs, so a failed evidence
    // upload is swallowed rather than surfaced as an error for the whole save.
    private suspend fun uploadEvidence(petId: String, eventId: String, evidence: EvidenceFile) {
        try {
            ApiClient.postMultipartFile<Document>(
                path = ApiEndpoints.petDocuments(petId),
                fields = mapOf("event" to eventId),
                fileBytes = evidence.bytes,
                fileName = evidence.fileName,
                mimeType = evidence.mimeType,
            )
        } catch (e: ApiError) {
            // ignore: evidence is a best-effort attachment
        }
    }

    fun resetCreateState() {
        _createState.value = CreateEventUiState.Idle
    }
}
