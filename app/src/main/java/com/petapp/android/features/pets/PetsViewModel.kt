package com.petapp.android.features.pets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petapp.android.core.model.Pet
import com.petapp.android.core.model.PetSpecies
import com.petapp.android.core.model.UpdatePetRequest
import com.petapp.android.core.network.ApiClient
import com.petapp.android.core.network.ApiEndpoints
import com.petapp.android.core.network.ApiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PetsUiState {
    data object Loading : PetsUiState
    data class Loaded(val pets: List<Pet>) : PetsUiState
    data class Error(val message: String) : PetsUiState
}

sealed interface CreatePetUiState {
    data object Idle : CreatePetUiState
    data object Loading : CreatePetUiState
    data class Success(val pet: Pet) : CreatePetUiState
    data class Error(val message: String) : CreatePetUiState
}

sealed interface UpdatePetUiState {
    data object Idle : UpdatePetUiState
    data object Loading : UpdatePetUiState
    data class Success(val pet: Pet) : UpdatePetUiState
    data class Error(val message: String) : UpdatePetUiState
}

class PetsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<PetsUiState>(PetsUiState.Loading)
    val uiState: StateFlow<PetsUiState> = _uiState.asStateFlow()

    private val _createState = MutableStateFlow<CreatePetUiState>(CreatePetUiState.Idle)
    val createState: StateFlow<CreatePetUiState> = _createState.asStateFlow()

    private val _selectedPetId = MutableStateFlow<String?>(null)
    val selectedPetId: StateFlow<String?> = _selectedPetId.asStateFlow()

    private val _updateState = MutableStateFlow<UpdatePetUiState>(UpdatePetUiState.Idle)
    val updateState: StateFlow<UpdatePetUiState> = _updateState.asStateFlow()

    init {
        fetchPets()
    }

    fun fetchPets(selectPetId: String? = null) {
        // Don't clobber an already-Loaded list with Loading for a background refresh —
        // consumers (e.g. MainScaffold) treat non-Loaded state as "no pets", which would
        // otherwise flash the empty state on every refetch.
        if (_uiState.value !is PetsUiState.Loaded) {
            _uiState.value = PetsUiState.Loading
        }
        viewModelScope.launch {
            try {
                val pets: List<Pet> = ApiClient.get(ApiEndpoints.PETS)
                _uiState.value = PetsUiState.Loaded(pets)
                val stillExists = pets.any { it.id == _selectedPetId.value }
                _selectedPetId.value = when {
                    selectPetId != null -> selectPetId
                    stillExists -> _selectedPetId.value
                    else -> pets.firstOrNull()?.id
                }
            } catch (e: ApiError.ServerError) {
                _uiState.value = PetsUiState.Error(e.errorMessage)
            } catch (e: ApiError) {
                _uiState.value = PetsUiState.Error(e.message ?: "No se pudo cargar tus mascotas.")
            }
        }
    }

    fun selectPet(petId: String) {
        _selectedPetId.value = petId
    }

    fun createPet(name: String, species: PetSpecies, birthDate: String?, imageBytes: ByteArray?) {
        _createState.value = CreatePetUiState.Loading
        viewModelScope.launch {
            try {
                val fields = buildMap {
                    put("name", name)
                    put("species", species.apiValue)
                    put("sex", "unknown")
                    if (!birthDate.isNullOrEmpty()) put("birth_date", birthDate)
                }
                val pet: Pet = ApiClient.postMultipart(ApiEndpoints.PETS, fields, imageBytes)
                _createState.value = CreatePetUiState.Success(pet)
                // Optimistically add the pet so consumers (e.g. the InicioTab FAB) see it
                // immediately instead of waiting on the fetchPets() round-trip below.
                val current = (_uiState.value as? PetsUiState.Loaded)?.pets.orEmpty()
                _uiState.value = PetsUiState.Loaded(current + pet)
                _selectedPetId.value = pet.id
                fetchPets(selectPetId = pet.id)
            } catch (e: ApiError.ServerError) {
                _createState.value = CreatePetUiState.Error(e.errorMessage)
            } catch (e: ApiError) {
                _createState.value = CreatePetUiState.Error(e.message ?: "Algo salió mal.")
            }
        }
    }

    fun resetCreateState() {
        _createState.value = CreatePetUiState.Idle
    }

    fun updatePet(petId: String, request: UpdatePetRequest) {
        _updateState.value = UpdatePetUiState.Loading
        viewModelScope.launch {
            try {
                val pet: Pet = ApiClient.patch(ApiEndpoints.petDetail(petId), request)
                _updateState.value = UpdatePetUiState.Success(pet)
                fetchPets(selectPetId = _selectedPetId.value)
            } catch (e: ApiError.ServerError) {
                _updateState.value = UpdatePetUiState.Error(e.errorMessage)
            } catch (e: ApiError) {
                _updateState.value = UpdatePetUiState.Error(e.message ?: "Algo salió mal.")
            }
        }
    }

    fun resetUpdateState() {
        _updateState.value = UpdatePetUiState.Idle
    }
}
