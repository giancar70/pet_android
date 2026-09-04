package com.petapp.android.features.files

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petapp.android.core.model.Document
import com.petapp.android.core.network.ApiClient
import com.petapp.android.core.network.ApiEndpoints
import com.petapp.android.core.network.ApiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface UploadDocumentUiState {
    data object Idle : UploadDocumentUiState
    data object Loading : UploadDocumentUiState
    data class Success(val document: Document) : UploadDocumentUiState
    data class Error(val message: String) : UploadDocumentUiState
}

sealed interface DocumentsListUiState {
    data object Loading : DocumentsListUiState
    data class Loaded(val documents: List<Document>) : DocumentsListUiState
    data class Error(val message: String) : DocumentsListUiState
}

sealed interface DocumentDetailUiState {
    data object Loading : DocumentDetailUiState
    data class Loaded(val document: Document) : DocumentDetailUiState
    data class Error(val message: String) : DocumentDetailUiState
}

sealed interface DeleteDocumentUiState {
    data object Idle : DeleteDocumentUiState
    data object Loading : DeleteDocumentUiState
    data object Success : DeleteDocumentUiState
    data class Error(val message: String) : DeleteDocumentUiState
}

class FilesViewModel : ViewModel() {
    private val _uploadState = MutableStateFlow<UploadDocumentUiState>(UploadDocumentUiState.Idle)
    val uploadState: StateFlow<UploadDocumentUiState> = _uploadState.asStateFlow()

    private val _listState = MutableStateFlow<DocumentsListUiState>(DocumentsListUiState.Loading)
    val listState: StateFlow<DocumentsListUiState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow<DocumentDetailUiState>(DocumentDetailUiState.Loading)
    val detailState: StateFlow<DocumentDetailUiState> = _detailState.asStateFlow()

    private val _deleteState = MutableStateFlow<DeleteDocumentUiState>(DeleteDocumentUiState.Idle)
    val deleteState: StateFlow<DeleteDocumentUiState> = _deleteState.asStateFlow()

    // Tracks which pet's list is currently held in _listState so fetchDocuments can
    // skip a redundant network call when nothing has changed -- see that function.
    private var lastFetchedPetId: String? = null

    // Blocks not affected by an unrelated navigation shouldn't refetch: if this pet's
    // list is already loaded, re-entering the screen is a no-op instead of a fresh
    // network round-trip. uploadDocument keeps this list in sync locally on success, so
    // a real change is reflected without invalidating the cache.
    fun fetchDocuments(petId: String, forceRefresh: Boolean = false) {
        if (!forceRefresh && petId == lastFetchedPetId && _listState.value is DocumentsListUiState.Loaded) {
            return
        }
        lastFetchedPetId = petId
        if (_listState.value !is DocumentsListUiState.Loaded) {
            _listState.value = DocumentsListUiState.Loading
        }
        viewModelScope.launch {
            try {
                val documents: List<Document> = ApiClient.get(ApiEndpoints.petDocuments(petId))
                _listState.value = DocumentsListUiState.Loaded(documents)
            } catch (e: ApiError.ServerError) {
                _listState.value = DocumentsListUiState.Error(e.errorMessage)
            } catch (e: ApiError) {
                _listState.value = DocumentsListUiState.Error(e.message ?: "No se pudieron cargar los documentos.")
            }
        }
    }

    fun fetchDocumentDetail(petId: String, documentId: String) {
        _detailState.value = DocumentDetailUiState.Loading
        viewModelScope.launch {
            try {
                val document: Document = ApiClient.get(ApiEndpoints.petDocumentDetail(petId, documentId))
                _detailState.value = DocumentDetailUiState.Loaded(document)
            } catch (e: ApiError.ServerError) {
                _detailState.value = DocumentDetailUiState.Error(e.errorMessage)
            } catch (e: ApiError) {
                _detailState.value = DocumentDetailUiState.Error(e.message ?: "No se pudo cargar el documento.")
            }
        }
    }

    fun uploadDocument(petId: String, fileBytes: ByteArray, fileName: String, mimeType: String, documentType: String) {
        _uploadState.value = UploadDocumentUiState.Loading
        viewModelScope.launch {
            try {
                val document: Document = ApiClient.postMultipartFile(
                    path = ApiEndpoints.petDocuments(petId),
                    fields = mapOf("title" to fileName, "document_type" to documentType),
                    fileBytes = fileBytes,
                    fileName = fileName,
                    mimeType = mimeType,
                )
                _uploadState.value = UploadDocumentUiState.Success(document)
                // Updates just this block locally instead of refetching the whole list.
                (_listState.value as? DocumentsListUiState.Loaded)?.let {
                    _listState.value = DocumentsListUiState.Loaded(listOf(document) + it.documents)
                }
            } catch (e: ApiError.ServerError) {
                _uploadState.value = UploadDocumentUiState.Error(e.errorMessage)
            } catch (e: ApiError) {
                _uploadState.value = UploadDocumentUiState.Error(e.message ?: "No se pudo subir el archivo.")
            }
        }
    }

    fun resetUploadState() {
        _uploadState.value = UploadDocumentUiState.Idle
    }

    // Soft-delete: DELETE flips the row's `deleted` flag on the backend rather than
    // removing it, scoped to this one document -- other documents/pets are untouched.
    fun deleteDocument(petId: String, documentId: String) {
        _deleteState.value = DeleteDocumentUiState.Loading
        viewModelScope.launch {
            try {
                ApiClient.delete(ApiEndpoints.petDocumentDetail(petId, documentId))
                _deleteState.value = DeleteDocumentUiState.Success
                // Updates just this block locally instead of refetching the whole list.
                (_listState.value as? DocumentsListUiState.Loaded)?.let {
                    _listState.value = DocumentsListUiState.Loaded(it.documents.filterNot { doc -> doc.id == documentId })
                }
            } catch (e: ApiError.ServerError) {
                _deleteState.value = DeleteDocumentUiState.Error(e.errorMessage)
            } catch (e: ApiError) {
                _deleteState.value = DeleteDocumentUiState.Error(e.message ?: "No se pudo eliminar el documento.")
            }
        }
    }

    fun resetDeleteState() {
        _deleteState.value = DeleteDocumentUiState.Idle
    }
}
