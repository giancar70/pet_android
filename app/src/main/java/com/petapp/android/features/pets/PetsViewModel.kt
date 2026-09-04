package com.petapp.android.features.pets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petapp.android.core.model.Breed
import com.petapp.android.core.model.Pet
import com.petapp.android.core.model.PetSpecies
import com.petapp.android.core.model.UpdatePetRequest
import com.petapp.android.core.network.ApiClient
import com.petapp.android.core.network.ApiEndpoints
import com.petapp.android.core.network.ApiError
import com.petapp.android.core.storage.PetPreferences
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

sealed interface DeletePetUiState {
    data object Idle : DeletePetUiState
    data object Loading : DeletePetUiState
    data object Success : DeletePetUiState
    data class Error(val message: String) : DeletePetUiState
}

sealed interface BreedsUiState {
    data object Idle : BreedsUiState
    data object Loading : BreedsUiState
    data class Loaded(val breeds: List<Breed>) : BreedsUiState
    data class Error(val message: String) : BreedsUiState
}

class PetsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<PetsUiState>(PetsUiState.Loading)
    val uiState: StateFlow<PetsUiState> = _uiState.asStateFlow()

    private val _createState = MutableStateFlow<CreatePetUiState>(CreatePetUiState.Idle)
    val createState: StateFlow<CreatePetUiState> = _createState.asStateFlow()

    private val _selectedPetId = MutableStateFlow<String?>(PetPreferences.selectedPetId)
    val selectedPetId: StateFlow<String?> = _selectedPetId.asStateFlow()

    private val _updateState = MutableStateFlow<UpdatePetUiState>(UpdatePetUiState.Idle)
    val updateState: StateFlow<UpdatePetUiState> = _updateState.asStateFlow()

    private val _deleteState = MutableStateFlow<DeletePetUiState>(DeletePetUiState.Idle)
    val deleteState: StateFlow<DeletePetUiState> = _deleteState.asStateFlow()

    private val _breedsState = MutableStateFlow<BreedsUiState>(BreedsUiState.Idle)
    val breedsState: StateFlow<BreedsUiState> = _breedsState.asStateFlow()

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
                val newSelectedId = when {
                    selectPetId != null -> selectPetId
                    stillExists -> _selectedPetId.value
                    else -> pets.firstOrNull()?.id
                }
                _selectedPetId.value = newSelectedId
                PetPreferences.selectedPetId = newSelectedId
            } catch (e: ApiError.ServerError) {
                _uiState.value = PetsUiState.Error(e.errorMessage)
            } catch (e: ApiError) {
                _uiState.value = PetsUiState.Error(e.message ?: "No se pudo cargar tus mascotas.")
            }
        }
    }

    fun selectPet(petId: String) {
        _selectedPetId.value = petId
        PetPreferences.selectedPetId = petId
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
                PetPreferences.selectedPetId = pet.id
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
                // Patch the already-loaded list in place with the PATCH response instead
                // of waiting on a separate fetchPets() round-trip -- screens reading
                // uiState (GreetingHeader, GestionarPetScreen, ...) reflect the edit the
                // instant Guardar succeeds rather than a moment later.
                val current = (_uiState.value as? PetsUiState.Loaded)?.pets
                if (current != null) {
                    _uiState.value = PetsUiState.Loaded(current.map { if (it.id == pet.id) pet else it })
                }
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

    // Soft-delete on the backend (Pet.delete() flips a `deleted` flag rather than
    // removing the row), scoped to this one pet by id -- other pets and their own
    // records are untouched.
    fun deletePet(petId: String) {
        _deleteState.value = DeletePetUiState.Loading
        viewModelScope.launch {
            try {
                ApiClient.delete(ApiEndpoints.petDetail(petId))
                _deleteState.value = DeletePetUiState.Success
                val remaining = (_uiState.value as? PetsUiState.Loaded)?.pets.orEmpty().filter { it.id != petId }
                _uiState.value = PetsUiState.Loaded(remaining)
                if (_selectedPetId.value == petId) {
                    val newSelectedId = remaining.firstOrNull()?.id
                    _selectedPetId.value = newSelectedId
                    PetPreferences.selectedPetId = newSelectedId
                }
            } catch (e: ApiError.ServerError) {
                _deleteState.value = DeletePetUiState.Error(e.errorMessage)
            } catch (e: ApiError) {
                _deleteState.value = DeletePetUiState.Error(e.message ?: "No se pudo eliminar la mascota.")
            }
        }
    }

    fun resetDeleteState() {
        _deleteState.value = DeletePetUiState.Idle
    }

    // Backs the "Raza" picker on the pet-edit screen -- a server-side catalog (seeded
    // from lista_raza.csv) filtered by the pet's current species, rather than free text.
    fun fetchBreeds(species: String) {
        _breedsState.value = BreedsUiState.Loading
        viewModelScope.launch {
            try {
                val breeds: List<Breed> = ApiClient.get(ApiEndpoints.breeds(species))
                _breedsState.value = BreedsUiState.Loaded(breeds)
            } catch (e: ApiError.ServerError) {
                _breedsState.value = BreedsUiState.Error(e.errorMessage)
            } catch (e: ApiError) {
                _breedsState.value = BreedsUiState.Error(e.message ?: "No se pudieron cargar las razas.")
            }
        }
    }

    fun resetBreedsState() {
        _breedsState.value = BreedsUiState.Idle
    }

    fun clearState() {
        _uiState.value = PetsUiState.Loading
        _selectedPetId.value = null
        PetPreferences.selectedPetId = null
        _createState.value = CreatePetUiState.Idle
        _updateState.value = UpdatePetUiState.Idle
        _deleteState.value = DeletePetUiState.Idle
        _breedsState.value = BreedsUiState.Idle
    }
}
