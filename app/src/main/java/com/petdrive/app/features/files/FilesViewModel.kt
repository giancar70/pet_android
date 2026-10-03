package com.petdrive.app.features.files

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petdrive.app.core.model.Document
import com.petdrive.app.core.model.CartillaAnalysis
import com.petdrive.app.core.model.DocumentAnalysis
import com.petdrive.app.core.network.ApiClient
import com.petdrive.app.core.network.ApiEndpoints
import com.petdrive.app.core.network.ApiError
import com.petdrive.app.core.util.AllowedFileTypes
import com.petdrive.app.core.util.FileSizeLimits
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

sealed interface AnalyzeDocumentUiState {
    data object Idle : AnalyzeDocumentUiState
    data object Loading : AnalyzeDocumentUiState
    data class Success(val analysis: DocumentAnalysis) : AnalyzeDocumentUiState
    data class Error(val message: String) : AnalyzeDocumentUiState
}

sealed interface AnalyzeCartillaUiState {
    data object Idle : AnalyzeCartillaUiState
    data object Loading : AnalyzeCartillaUiState
    data class Success(val analysis: CartillaAnalysis) : AnalyzeCartillaUiState
    data class Error(val message: String) : AnalyzeCartillaUiState
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

    private val _analyzeState = MutableStateFlow<AnalyzeDocumentUiState>(AnalyzeDocumentUiState.Idle)
    val analyzeState: StateFlow<AnalyzeDocumentUiState> = _analyzeState.asStateFlow()

    private val _analyzeCartillaState = MutableStateFlow<AnalyzeCartillaUiState>(AnalyzeCartillaUiState.Idle)
    val analyzeCartillaState: StateFlow<AnalyzeCartillaUiState> = _analyzeCartillaState.asStateFlow()

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

    // Classifies a scan and finds its date/vaccines without saving anything -- see
    // DocumentAnalyzeView (apps/files/views.py). The caller then uploads for real with
    // uploadDocument(analyzed = true).
    fun analyzeDocument(petId: String, fileBytes: ByteArray, fileName: String, mimeType: String) {
        _analyzeState.value = AnalyzeDocumentUiState.Loading
        viewModelScope.launch {
            try {
                val analysis: DocumentAnalysis = ApiClient.postMultipartFile(
                    path = ApiEndpoints.petDocumentsAnalyze(petId),
                    fields = emptyMap(),
                    fileBytes = fileBytes,
                    fileName = fileName,
                    mimeType = mimeType,
                )
                _analyzeState.value = AnalyzeDocumentUiState.Success(analysis)
            } catch (e: ApiError.ServerError) {
                _analyzeState.value = AnalyzeDocumentUiState.Error(e.errorMessage)
            } catch (e: ApiError) {
                _analyzeState.value = AnalyzeDocumentUiState.Error(e.message ?: "No se pudo analizar el documento.")
            }
        }
    }

    fun resetAnalyzeState() {
        _analyzeState.value = AnalyzeDocumentUiState.Idle
    }

    // Classifies a Cartilla/Pasaporte scan (pet info + vaccines + deworming) without
    // saving anything -- see DocumentAnalyzeCartillaView (apps/files/views.py).
    fun analyzeCartilla(petId: String, fileBytes: ByteArray, fileName: String, mimeType: String) {
        _analyzeCartillaState.value = AnalyzeCartillaUiState.Loading
        viewModelScope.launch {
            try {
                val analysis: CartillaAnalysis = ApiClient.postMultipartFile(
                    path = ApiEndpoints.petDocumentsAnalyzeCartilla(petId),
                    fields = emptyMap(),
                    fileBytes = fileBytes,
                    fileName = fileName,
                    mimeType = mimeType,
                )
                _analyzeCartillaState.value = AnalyzeCartillaUiState.Success(analysis)
            } catch (e: ApiError.ServerError) {
                _analyzeCartillaState.value = AnalyzeCartillaUiState.Error(e.errorMessage)
            } catch (e: ApiError) {
                _analyzeCartillaState.value = AnalyzeCartillaUiState.Error(e.message ?: "No se pudo analizar el documento.")
            }
        }
    }

    fun resetAnalyzeCartillaState() {
        _analyzeCartillaState.value = AnalyzeCartillaUiState.Idle
    }

    fun uploadDocument(
        petId: String,
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String,
        documentType: String,
        documentDate: String? = null,
        analyzed: Boolean = false,
    ) {
        if (mimeType !in AllowedFileTypes.DOCUMENT_TYPES) {
            _uploadState.value = UploadDocumentUiState.Error(AllowedFileTypes.documentTypeMessage())
            return
        }
        if (fileBytes.size > FileSizeLimits.MAX_DOCUMENT_BYTES) {
            _uploadState.value = UploadDocumentUiState.Error(FileSizeLimits.documentTooLargeMessage())
            return
        }
        _uploadState.value = UploadDocumentUiState.Loading
        viewModelScope.launch {
            try {
                val document: Document = ApiClient.postMultipartFile(
                    path = ApiEndpoints.petDocuments(petId),
                    fields = buildMap {
                        put("title", fileName)
                        put("document_type", documentType)
                        if (documentDate != null) put("document_date", documentDate)
                        if (analyzed) put("analyzed", "true")
                    },
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
