package com.petapp.android.features.activitylog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petapp.android.core.model.PetActivityLog
import com.petapp.android.core.network.ApiClient
import com.petapp.android.core.network.ApiEndpoints
import com.petapp.android.core.network.ApiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ActivityLogUiState {
    data object Loading : ActivityLogUiState
    data class Loaded(val entries: List<PetActivityLog>) : ActivityLogUiState
    data class Error(val message: String) : ActivityLogUiState
}

class ActivityLogViewModel : ViewModel() {
    private val _listState = MutableStateFlow<ActivityLogUiState>(ActivityLogUiState.Loading)
    val listState: StateFlow<ActivityLogUiState> = _listState.asStateFlow()

    fun fetchActivityLog(petId: String) {
        _listState.value = ActivityLogUiState.Loading
        viewModelScope.launch {
            try {
                val entries: List<PetActivityLog> = ApiClient.get(ApiEndpoints.petActivityLog(petId))
                _listState.value = ActivityLogUiState.Loaded(entries)
            } catch (e: ApiError.ServerError) {
                _listState.value = ActivityLogUiState.Error(e.errorMessage)
            } catch (e: ApiError) {
                _listState.value = ActivityLogUiState.Error(e.message ?: "No se pudo cargar el historial de actividad.")
            }
        }
    }
}
