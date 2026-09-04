package com.petapp.android.features.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petapp.android.core.model.CreateReminderRequest
import com.petapp.android.core.model.Reminder
import com.petapp.android.core.network.ApiClient
import com.petapp.android.core.network.ApiEndpoints
import com.petapp.android.core.network.ApiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CreateReminderUiState {
    data object Idle : CreateReminderUiState
    data object Loading : CreateReminderUiState
    data class Success(val reminder: Reminder) : CreateReminderUiState
    data class Error(val message: String) : CreateReminderUiState
}

sealed interface RecordatoriosListUiState {
    data object Loading : RecordatoriosListUiState
    data class Loaded(val reminders: List<Reminder>) : RecordatoriosListUiState
    data class Error(val message: String) : RecordatoriosListUiState
}

sealed interface RecordatorioDetailUiState {
    data object Loading : RecordatorioDetailUiState
    data class Loaded(val reminder: Reminder) : RecordatorioDetailUiState
    data class Error(val message: String) : RecordatorioDetailUiState
}

sealed interface DeleteReminderUiState {
    data object Idle : DeleteReminderUiState
    data object Loading : DeleteReminderUiState
    data object Success : DeleteReminderUiState
    data class Error(val message: String) : DeleteReminderUiState
}

class RemindersViewModel : ViewModel() {
    private val _createState = MutableStateFlow<CreateReminderUiState>(CreateReminderUiState.Idle)
    val createState: StateFlow<CreateReminderUiState> = _createState.asStateFlow()

    private val _listState = MutableStateFlow<RecordatoriosListUiState>(RecordatoriosListUiState.Loading)
    val listState: StateFlow<RecordatoriosListUiState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow<RecordatorioDetailUiState>(RecordatorioDetailUiState.Loading)
    val detailState: StateFlow<RecordatorioDetailUiState> = _detailState.asStateFlow()

    private val _deleteState = MutableStateFlow<DeleteReminderUiState>(DeleteReminderUiState.Idle)
    val deleteState: StateFlow<DeleteReminderUiState> = _deleteState.asStateFlow()

    fun fetchRecordatorioDetail(petId: String, reminderId: String) {
        _detailState.value = RecordatorioDetailUiState.Loading
        viewModelScope.launch {
            try {
                val reminder: Reminder = ApiClient.get(ApiEndpoints.petReminderDetail(petId, reminderId))
                _detailState.value = RecordatorioDetailUiState.Loaded(reminder)
            } catch (e: ApiError.ServerError) {
                _detailState.value = RecordatorioDetailUiState.Error(e.errorMessage)
            } catch (e: ApiError) {
                _detailState.value = RecordatorioDetailUiState.Error(e.message ?: "No se pudo cargar el recordatorio.")
            }
        }
    }

    // Soft-delete: DELETE flips the row's `deleted` flag on the backend rather than
    // removing it, scoped to this one reminder -- other reminders/pets are untouched.
    fun deleteRecordatorio(petId: String, reminderId: String) {
        _deleteState.value = DeleteReminderUiState.Loading
        viewModelScope.launch {
            try {
                ApiClient.delete(ApiEndpoints.petReminderDetail(petId, reminderId))
                _deleteState.value = DeleteReminderUiState.Success
            } catch (e: ApiError.ServerError) {
                _deleteState.value = DeleteReminderUiState.Error(e.errorMessage)
            } catch (e: ApiError) {
                _deleteState.value = DeleteReminderUiState.Error(e.message ?: "No se pudo eliminar el recordatorio.")
            }
        }
    }

    fun resetDeleteState() {
        _deleteState.value = DeleteReminderUiState.Idle
    }

    fun fetchRecordatorios(petId: String) {
        _listState.value = RecordatoriosListUiState.Loading
        viewModelScope.launch {
            try {
                val reminders: List<Reminder> = ApiClient.get(ApiEndpoints.petReminders(petId))
                _listState.value = RecordatoriosListUiState.Loaded(reminders)
            } catch (e: ApiError.ServerError) {
                _listState.value = RecordatoriosListUiState.Error(e.errorMessage)
            } catch (e: ApiError) {
                _listState.value = RecordatoriosListUiState.Error(e.message ?: "No se pudieron cargar los recordatorios.")
            }
        }
    }

    fun createRecordatorio(
        petId: String,
        category: String,
        title: String,
        dueDateIso: String,
        frequency: String,
        customDays: Int?,
        notifyPush: Boolean,
    ) {
        _createState.value = CreateReminderUiState.Loading
        viewModelScope.launch {
            try {
                val request = CreateReminderRequest(
                    category = category,
                    title = title,
                    dueDate = dueDateIso,
                    frequency = frequency,
                    customDays = customDays,
                    notifyPush = notifyPush,
                )
                val reminder: Reminder = ApiClient.post(ApiEndpoints.petReminders(petId), request)
                _createState.value = CreateReminderUiState.Success(reminder)
            } catch (e: ApiError.ServerError) {
                _createState.value = CreateReminderUiState.Error(e.errorMessage)
            } catch (e: ApiError) {
                _createState.value = CreateReminderUiState.Error(e.message ?: "No se pudo guardar el recordatorio.")
            }
        }
    }

    fun resetCreateState() {
        _createState.value = CreateReminderUiState.Idle
    }
}
