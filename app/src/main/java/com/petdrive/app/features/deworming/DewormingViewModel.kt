package com.petdrive.app.features.deworming

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petdrive.app.core.model.CreateDewormingApplicationRequest
import com.petdrive.app.core.model.RegisterDewormingsFromDocumentRequest
import com.petdrive.app.core.model.DewormingApplication
import com.petdrive.app.core.model.UpdateDewormingApplicationRequest
import com.petdrive.app.core.network.ApiClient
import com.petdrive.app.core.network.ApiEndpoints
import com.petdrive.app.core.network.ApiError
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

sealed interface RegisterDewormingsUiState {
    data object Idle : RegisterDewormingsUiState
    data object Loading : RegisterDewormingsUiState
    data class Success(val applications: List<DewormingApplication>) : RegisterDewormingsUiState
    data class Error(val message: String) : RegisterDewormingsUiState
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

sealed interface UpdateDewormingUiState {
    data object Idle : UpdateDewormingUiState
    data object Loading : UpdateDewormingUiState
    data class Success(val application: DewormingApplication) : UpdateDewormingUiState
    data class Error(val message: String) : UpdateDewormingUiState
}

class DewormingViewModel : ViewModel() {
    private val _createState = MutableStateFlow<CreateDewormingUiState>(CreateDewormingUiState.Idle)
    val createState: StateFlow<CreateDewormingUiState> = _createState.asStateFlow()

    private val _listState = MutableStateFlow<DewormingListUiState>(DewormingListUiState.Loading)
    val listState: StateFlow<DewormingListUiState> = _listState.asStateFlow()

    private val _registerFromDocumentState = MutableStateFlow<RegisterDewormingsUiState>(RegisterDewormingsUiState.Idle)
    val registerFromDocumentState: StateFlow<RegisterDewormingsUiState> = _registerFromDocumentState.asStateFlow()

    private val _detailState = MutableStateFlow<DewormingDetailUiState>(DewormingDetailUiState.Loading)
    val detailState: StateFlow<DewormingDetailUiState> = _detailState.asStateFlow()

    private val _deleteState = MutableStateFlow<DeleteDewormingUiState>(DeleteDewormingUiState.Idle)
    val deleteState: StateFlow<DeleteDewormingUiState> = _deleteState.asStateFlow()

    private val _updateState = MutableStateFlow<UpdateDewormingUiState>(UpdateDewormingUiState.Idle)
    val updateState: StateFlow<UpdateDewormingUiState> = _updateState.asStateFlow()

    // Tracks which pet's list is currently held in _listState so fetchDesparasitaciones
    // can skip a redundant network call when nothing has changed -- see that function.
    private var lastFetchedPetId: String? = null

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

    // Blocks not affected by an unrelated navigation shouldn't refetch: if this pet's
    // list is already loaded, re-entering the screen is a no-op instead of a fresh
    // network round-trip. createDesparasitacion/deleteDesparasitacion keep this list in
    // sync locally on success, so a real change is reflected without invalidating the cache.
    fun fetchDesparasitaciones(petId: String, forceRefresh: Boolean = false) {
        if (!forceRefresh && petId == lastFetchedPetId && _listState.value is DewormingListUiState.Loaded) {
            return
        }
        lastFetchedPetId = petId
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
                // Updates just this block locally instead of refetching the whole list.
                (_listState.value as? DewormingListUiState.Loaded)?.let {
                    _listState.value = DewormingListUiState.Loaded(listOf(application) + it.applications)
                }
            } catch (e: ApiError.ServerError) {
                _createState.value = CreateDewormingUiState.Error(e.errorMessage)
            } catch (e: ApiError) {
                _createState.value = CreateDewormingUiState.Error(e.message ?: "No se pudo guardar la desparasitación.")
            }
        }
    }

    // Confirms the (possibly user-edited) list of deworming applications detected on a
    // Cartilla/Pasaporte scan, creating one DewormingApplication per entry in a single
    // request -- see DewormingApplicationBulkCreateFromDocumentView (apps/pet/views.py).
    fun registerFromDocument(petId: String, documentId: String, applications: List<CreateDewormingApplicationRequest>) {
        _registerFromDocumentState.value = RegisterDewormingsUiState.Loading
        viewModelScope.launch {
            try {
                val created: List<DewormingApplication> = ApiClient.post(
                    ApiEndpoints.petDocumentRegisterDewormings(petId, documentId),
                    RegisterDewormingsFromDocumentRequest(dewormings = applications),
                )
                _registerFromDocumentState.value = RegisterDewormingsUiState.Success(created)
                (_listState.value as? DewormingListUiState.Loaded)?.let {
                    _listState.value = DewormingListUiState.Loaded(created + it.applications)
                }
            } catch (e: ApiError.ServerError) {
                _registerFromDocumentState.value = RegisterDewormingsUiState.Error(e.errorMessage)
            } catch (e: ApiError) {
                _registerFromDocumentState.value = RegisterDewormingsUiState.Error(e.message ?: "No se pudieron registrar las desparasitaciones.")
            }
        }
    }

    fun resetRegisterFromDocumentState() {
        _registerFromDocumentState.value = RegisterDewormingsUiState.Idle
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
                // Updates just this block locally instead of refetching the whole list.
                (_listState.value as? DewormingListUiState.Loaded)?.let {
                    _listState.value = DewormingListUiState.Loaded(it.applications.filterNot { app -> app.id == applicationId })
                }
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

    // Edits the renewal date on an existing application in place
    // (DesparasitacionDetailScreen's "Guardar") -- unlike "Renovar", which creates a
    // brand new application and leaves this one untouched (see
    // DewormingApplicationSerializer.create(), which no longer auto-replaces).
    fun updateNextDueOn(petId: String, applicationId: String, nextDueOnIso: String) {
        _updateState.value = UpdateDewormingUiState.Loading
        viewModelScope.launch {
            try {
                val application: DewormingApplication = ApiClient.patch(
                    ApiEndpoints.petDewormingApplicationDetail(petId, applicationId),
                    UpdateDewormingApplicationRequest(nextDueOn = nextDueOnIso),
                )
                _updateState.value = UpdateDewormingUiState.Success(application)
                if (_detailState.value is DewormingDetailUiState.Loaded) {
                    _detailState.value = DewormingDetailUiState.Loaded(application)
                }
                (_listState.value as? DewormingListUiState.Loaded)?.let {
                    _listState.value = DewormingListUiState.Loaded(it.applications.map { a -> if (a.id == application.id) application else a })
                }
            } catch (e: ApiError.ServerError) {
                _updateState.value = UpdateDewormingUiState.Error(e.errorMessage)
            } catch (e: ApiError) {
                _updateState.value = UpdateDewormingUiState.Error(e.message ?: "No se pudo actualizar la desparasitación.")
            }
        }
    }

    fun resetUpdateState() {
        _updateState.value = UpdateDewormingUiState.Idle
    }
}
