package com.petdrive.app.features.files

import android.app.Activity
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.petdrive.app.core.model.CartillaAnalysis
import com.petdrive.app.core.util.decodePage
import com.petdrive.app.core.util.mergePagesToJpeg
import com.petdrive.app.core.util.rotate90
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed interface PassportScanStep {
    data object Scanning : PassportScanStep
    data object Preview : PassportScanStep
    data object Analyzing : PassportScanStep
}

private sealed interface PassportScanAction {
    data object Initial : PassportScanAction
    data object Append : PassportScanAction
    data class Replace(val index: Int) : PassportScanAction
}

// Scans a Cartilla/Pasaporte to prefill the pet-creation form ("Crear desde Cartilla /
// Pasaporte" on RegisterPetScreen) -- unlike CapturarDocumentoScreen, there's no pet yet
// to scope the request/document to, so this hits analyzeCartillaForNewPet and hands the
// raw CartillaAnalysis + merged image bytes back to the caller instead of saving
// anything itself; RegisterPetScreen decides what to do with them once the pet exists.
// Reuses ScanningStep/ScanPreviewStep/AnalizandoStep from CapturarDocumentoScreen.kt.
@Composable
fun EscanearPasaporteScreen(
    userFullName: String?,
    onBack: () -> Unit,
    onAnalyzed: (analysis: CartillaAnalysis, imageBytes: ByteArray, fileName: String) -> Unit,
    filesViewModel: FilesViewModel = viewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var step by remember { mutableStateOf<PassportScanStep>(PassportScanStep.Scanning) }
    var pages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var scanAction by remember { mutableStateOf<PassportScanAction>(PassportScanAction.Initial) }
    var scanError by remember { mutableStateOf<String?>(null) }
    var mergedBytes by remember { mutableStateOf<ByteArray?>(null) }
    var analysisRequested by remember { mutableStateOf(false) }
    val fileName = remember { "pasaporte_${System.currentTimeMillis()}.jpg" }

    val analyzeState by filesViewModel.analyzeCartillaState.collectAsState()

    val scannerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scan = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            val uris = scan?.pages?.map { it.imageUri }.orEmpty()
            scope.launch {
                val decoded = withContext(Dispatchers.IO) { uris.mapNotNull { decodePage(context, it) } }
                pages = when (val action = scanAction) {
                    PassportScanAction.Initial -> decoded
                    PassportScanAction.Append -> pages + decoded
                    is PassportScanAction.Replace -> pages.toMutableList().also { list ->
                        if (decoded.isNotEmpty() && action.index in list.indices) {
                            list.removeAt(action.index)
                            list.addAll(action.index, decoded)
                        }
                    }
                }
                if (pages.isEmpty()) onBack() else step = PassportScanStep.Preview
            }
        } else if (pages.isEmpty()) {
            onBack()
        } else {
            step = PassportScanStep.Preview
        }
    }

    fun launchScanner(action: PassportScanAction) {
        val activity = context.findActivity()
        if (activity == null) {
            scanError = "No se pudo abrir el escáner."
            return
        }
        scanAction = action
        scanError = null
        val limit = when (action) {
            PassportScanAction.Initial -> MAX_PAGES
            PassportScanAction.Append -> (MAX_PAGES - pages.size).coerceAtLeast(1)
            is PassportScanAction.Replace -> 1
        }
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(false)
            .setPageLimit(limit)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
        GmsDocumentScanning.getClient(options)
            .getStartScanIntent(activity)
            .addOnSuccessListener { sender -> scannerLauncher.launch(IntentSenderRequest.Builder(sender).build()) }
            .addOnFailureListener { scanError = "No se pudo abrir el escáner de documentos. Comprueba que Google Play Services esté actualizado." }
    }

    LaunchedEffect(Unit) {
        launchScanner(PassportScanAction.Initial)
    }

    // Merges the pages and asks the backend to read pet info + vaccines + deworming off
    // them -- same contract as CapturarDocumentoScreen's Analyzing step, just via the
    // pet-less endpoint.
    LaunchedEffect(step) {
        if (step != PassportScanStep.Analyzing) return@LaunchedEffect
        analysisRequested = false
        val bytes = withContext(Dispatchers.Default) { mergePagesToJpeg(pages) }
        mergedBytes = bytes
        filesViewModel.analyzeCartillaForNewPet(bytes, fileName, "image/jpeg")
        analysisRequested = true
    }
    LaunchedEffect(analyzeState, analysisRequested) {
        val state = analyzeState
        if (step == PassportScanStep.Analyzing && analysisRequested && state is AnalyzeCartillaUiState.Success) {
            mergedBytes?.let { onAnalyzed(state.analysis, it, fileName) }
        }
    }

    when (step) {
        PassportScanStep.Scanning -> ScanningStep(error = scanError, onBack = onBack)
        PassportScanStep.Preview -> ScanPreviewStep(
            selectedPet = null,
            userFullName = userFullName,
            pages = pages,
            onBack = onBack,
            onRotate = { index -> pages = pages.toMutableList().also { it[index] = rotate90(it[index]) } },
            onCrop = { index -> launchScanner(PassportScanAction.Replace(index)) },
            onAddPage = { launchScanner(PassportScanAction.Append) },
            onContinue = {
                filesViewModel.resetAnalyzeCartillaState()
                step = PassportScanStep.Analyzing
            },
        )
        PassportScanStep.Analyzing -> AnalizandoStep(
            selectedPet = null,
            userFullName = userFullName,
            error = (analyzeState as? AnalyzeCartillaUiState.Error)?.message.takeIf { analysisRequested },
            onBack = { step = PassportScanStep.Preview },
        )
    }
}
