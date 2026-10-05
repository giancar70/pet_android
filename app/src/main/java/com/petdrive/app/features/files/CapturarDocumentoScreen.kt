package com.petdrive.app.features.files

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.petdrive.app.core.model.CartillaAnalysis
import com.petdrive.app.core.model.Document
import com.petdrive.app.core.model.DocumentTypeOption
import com.petdrive.app.core.model.Pet
import com.petdrive.app.core.util.decodePage
import com.petdrive.app.core.util.mergePagesToJpeg
import com.petdrive.app.core.util.rotate90
import com.petdrive.app.features.deworming.DewormingViewModel
import com.petdrive.app.features.main.GreetingHeader
import com.petdrive.app.features.vaccines.VaccinesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val BrandGreen = Color(0xFF406E5F)
private val SubtitleGray = Color(0xFF666666)
private val CardBorder = Color(0xFFEFEFF4)
private val ScanBlue = Color(0xFF3B82F6)
internal const val MAX_PAGES = 20

private sealed interface ScanStep {
    data object TypeSelection : ScanStep
    data object Scanning : ScanStep
    data object Preview : ScanStep
    data object Uploading : ScanStep
    data object Analyzing : ScanStep
    data object Review : ScanStep
    data class Saved(val documentTypeLabel: String, val registeredVaccines: Int, val registeredDewormings: Int) : ScanStep
}

private sealed interface ScanAction {
    data object Initial : ScanAction
    data object Append : ScanAction
    data class Replace(val index: Int) : ScanAction
}

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

// paso1 (elegir tipo) -> Escanear -> Previsualizar -> [solo Cartilla/Pasaporte: Analizar ->
// Revisar (paso5, datos de mascota + vacunas + desparasitaciones detectadas) ] -> Guardar ->
// Confirmación. Nothing is saved until Guardar: any other document type just scans, uploads
// with that type, and finishes -- only Cartilla/Pasaporte goes through the AI at all.
@Composable
fun CapturarDocumentoScreen(
    selectedPet: Pet?,
    userFullName: String?,
    onBack: () -> Unit,
    onViewActivity: () -> Unit,
    onViewVaccinesActivity: () -> Unit = onViewActivity,
    filesViewModel: FilesViewModel = viewModel(),
    vaccinesViewModel: VaccinesViewModel = viewModel(),
    dewormingViewModel: DewormingViewModel = viewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val petId = selectedPet?.id

    var step by remember { mutableStateOf<ScanStep>(ScanStep.TypeSelection) }
    var documentType by remember { mutableStateOf(DocumentTypeOption.VACCINE_CARD) }
    var documentTypeLabel by remember { mutableStateOf("") }
    var pages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var scanAction by remember { mutableStateOf<ScanAction>(ScanAction.Initial) }
    var scanError by remember { mutableStateOf<String?>(null) }
    var mergedBytes by remember { mutableStateOf<ByteArray?>(null) }
    var analysis by remember { mutableStateOf(CartillaAnalysis()) }
    var analysisRequested by remember { mutableStateOf(false) }
    val fileName = remember { "escaneo_${System.currentTimeMillis()}.jpg" }

    val analyzeState by filesViewModel.analyzeCartillaState.collectAsState()
    val uploadState by filesViewModel.uploadState.collectAsState()

    val scannerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scan = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            val uris = scan?.pages?.map { it.imageUri }.orEmpty()
            scope.launch {
                val decoded = withContext(Dispatchers.IO) { uris.mapNotNull { decodePage(context, it) } }
                pages = when (val action = scanAction) {
                    ScanAction.Initial -> decoded
                    ScanAction.Append -> pages + decoded
                    is ScanAction.Replace -> pages.toMutableList().also { list ->
                        if (decoded.isNotEmpty() && action.index in list.indices) {
                            list.removeAt(action.index)
                            list.addAll(action.index, decoded)
                        }
                    }
                }
                if (pages.isEmpty()) onBack() else step = ScanStep.Preview
            }
        } else if (pages.isEmpty()) {
            onBack()
        } else {
            step = ScanStep.Preview
        }
    }

    fun launchScanner(action: ScanAction) {
        val activity = context.findActivity()
        if (activity == null) {
            scanError = "No se pudo abrir el escáner."
            return
        }
        scanAction = action
        scanError = null
        val limit = when (action) {
            ScanAction.Initial -> MAX_PAGES
            ScanAction.Append -> (MAX_PAGES - pages.size).coerceAtLeast(1)
            is ScanAction.Replace -> 1
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

    // Analyzing (Cartilla/Pasaporte only): merge the pages and ask the backend to read pet
    // info + vaccines + deworming off them.
    LaunchedEffect(step) {
        if (step != ScanStep.Analyzing || petId == null) return@LaunchedEffect
        analysisRequested = false
        val bytes = withContext(Dispatchers.Default) { mergePagesToJpeg(pages) }
        mergedBytes = bytes
        filesViewModel.analyzeCartilla(petId, bytes, fileName, "image/jpeg")
        analysisRequested = true
    }
    // analysisRequested guards against an Activity-scoped ViewModel still holding a
    // Success from an earlier scan while the pages are being merged.
    LaunchedEffect(analyzeState, analysisRequested) {
        val state = analyzeState
        if (step == ScanStep.Analyzing && analysisRequested && state is AnalyzeCartillaUiState.Success) {
            analysis = state.analysis
            step = ScanStep.Review
        }
    }

    fun startPlainUpload() {
        petId?.let {
            scope.launch {
                val merged = withContext(Dispatchers.Default) { mergePagesToJpeg(pages) }
                filesViewModel.uploadDocument(it, merged, fileName, "image/jpeg", documentType.apiValue)
            }
        }
    }
    LaunchedEffect(uploadState) {
        val state = uploadState
        if (step != ScanStep.Uploading) return@LaunchedEffect
        if (state is UploadDocumentUiState.Success) {
            step = ScanStep.Saved(documentTypeLabel, 0, 0)
        }
    }

    when (val current = step) {
        ScanStep.TypeSelection -> DocumentTypeSelectionStep(
            selectedPet = selectedPet,
            userFullName = userFullName,
            onBack = onBack,
            onTypeSelected = { entry ->
                documentType = entry.option
                documentTypeLabel = entry.label
                step = ScanStep.Scanning
                launchScanner(ScanAction.Initial)
            },
        )
        ScanStep.Scanning -> ScanningStep(error = scanError, onBack = onBack)
        ScanStep.Preview -> ScanPreviewStep(
            selectedPet = selectedPet,
            userFullName = userFullName,
            pages = pages,
            onBack = onBack,
            onRotate = { index -> pages = pages.toMutableList().also { it[index] = rotate90(it[index]) } },
            onCrop = { index -> launchScanner(ScanAction.Replace(index)) },
            onAddPage = { launchScanner(ScanAction.Append) },
            onContinue = {
                if (documentType == DocumentTypeOption.VACCINE_CARD) {
                    filesViewModel.resetAnalyzeCartillaState()
                    step = ScanStep.Analyzing
                } else {
                    filesViewModel.resetUploadState()
                    step = ScanStep.Uploading
                    startPlainUpload()
                }
            },
        )
        ScanStep.Uploading -> UploadingStep(
            selectedPet = selectedPet,
            userFullName = userFullName,
            error = (uploadState as? UploadDocumentUiState.Error)?.message,
            onBack = { step = ScanStep.Preview },
        )
        ScanStep.Analyzing -> AnalizandoStep(
            selectedPet = selectedPet,
            userFullName = userFullName,
            error = (analyzeState as? AnalyzeCartillaUiState.Error)?.message.takeIf { analysisRequested },
            onBack = { step = ScanStep.Preview },
        )
        ScanStep.Review -> RevisarInformacionDetectadaScreen(
            selectedPet = selectedPet,
            userFullName = userFullName,
            analysis = analysis,
            onBack = { step = ScanStep.Preview },
            onSaved = { vaccineCount, dewormingCount ->
                step = ScanStep.Saved(documentTypeLabel, vaccineCount, dewormingCount)
            },
            mergedImageBytes = { mergedBytes ?: withContext(Dispatchers.Default) { mergePagesToJpeg(pages) }.also { mergedBytes = it } },
            fileName = fileName,
            filesViewModel = filesViewModel,
            vaccinesViewModel = vaccinesViewModel,
            dewormingViewModel = dewormingViewModel,
        )
        is ScanStep.Saved -> DocumentoGuardadoStep(
            selectedPet = selectedPet,
            userFullName = userFullName,
            fileName = fileName,
            mimeType = "image/jpeg",
            documentTypeLabel = current.documentTypeLabel,
            registeredVaccines = current.registeredVaccines,
            registeredDewormings = current.registeredDewormings,
            uploadAnotherLabel = "Escanear otro documento",
            onViewActivity = if (current.registeredVaccines > 0 || current.registeredDewormings > 0) onViewVaccinesActivity else onViewActivity,
            onUploadAnother = onBack,
        )
    }
}

private data class ScanTypeOption(val option: DocumentTypeOption, val label: String, val icon: ImageVector, val iconColor: Color)

private val ScanTypeOptions = listOf(
    ScanTypeOption(DocumentTypeOption.VACCINE_CARD, "Cartilla / Pasaporte", Icons.Filled.Person, BrandGreen),
    ScanTypeOption(DocumentTypeOption.LABWORK, "Análisis / Laboratorio", Icons.Filled.Science, BrandGreen),
    ScanTypeOption(DocumentTypeOption.PRESCRIPTION, "Receta", Icons.Filled.Description, BrandGreen),
    ScanTypeOption(DocumentTypeOption.INVOICE, "Factura", Icons.Filled.Receipt, Color(0xFFC0392B)),
    ScanTypeOption(DocumentTypeOption.OTHER, "Otros", Icons.Filled.Description, Color(0xFFC0392B)),
)

// paso1: only "Cartilla / Pasaporte" goes through the AI afterward -- see feature.md.
@Composable
private fun DocumentTypeSelectionStep(
    selectedPet: Pet?,
    userFullName: String?,
    onBack: () -> Unit,
    onTypeSelected: (ScanTypeOption) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState()),
    ) {
        BackHandler(onBack = onBack)
        IconButton(onClick = onBack, modifier = Modifier.padding(start = 12.dp, top = 12.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
        }
        GreetingHeader(
            selectedPet = selectedPet,
            userFullName = userFullName,
            hasPets = selectedPet != null,
            onSwitchPetClick = {},
        )
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(text = "¿Qué desea escanear?", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            ScanTypeOptions.forEach { entry ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                    onClick = { onTypeSelected(entry) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(entry.icon, contentDescription = null, tint = entry.iconColor)
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(text = entry.label, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = entry.iconColor)
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Composable
internal fun ScanningStep(error: String?, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (error != null) {
            Text(text = error, color = MaterialTheme.colorScheme.error, fontSize = 14.sp, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onBack, shape = RoundedCornerShape(28.dp)) { Text("Volver", color = BrandGreen) }
        } else {
            CircularProgressIndicator(color = BrandGreen)
        }
    }
}

@Composable
internal fun ScanPreviewStep(
    selectedPet: Pet?,
    userFullName: String?,
    pages: List<Bitmap>,
    onBack: () -> Unit,
    onRotate: (Int) -> Unit,
    onCrop: (Int) -> Unit,
    onAddPage: () -> Unit,
    onContinue: () -> Unit,
) {
    val pagerState = rememberPagerState { pages.size }
    val currentPage = pagerState.currentPage.coerceIn(0, (pages.size - 1).coerceAtLeast(0))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        BackHandler(onBack = onBack)
        IconButton(onClick = onBack, modifier = Modifier.padding(start = 12.dp, top = 12.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
        }
        GreetingHeader(
            selectedPet = selectedPet,
            userFullName = userFullName,
            hasPets = selectedPet != null,
            onSwitchPetClick = {},
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 40.dp, vertical = 12.dp),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(ScanBlue)
                    .padding(3.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.Black),
            ) { index ->
                Image(
                    bitmap = pages[index].asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = BrandGreen,
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                Text(
                    text = "${currentPage + 1}/${pages.size}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier
                    .weight(2f)
                    .height(96.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .dashedBorder(Color(0xFFBDBDBD)),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ScanToolButton(Icons.Filled.Crop, "Recortar", Color.Black) { onCrop(currentPage) }
                ScanToolButton(Icons.Filled.RotateRight, "Girar", Color.Black) { onRotate(currentPage) }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(96.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .dashedBorder(BrandGreen)
                    .clickable(enabled = pages.size < MAX_PAGES, onClick = onAddPage),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Añadir página", color = BrandGreen, fontWeight = FontWeight.Medium, fontSize = 12.sp)
            }
        }

        Button(
            onClick = onContinue,
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp, vertical = 16.dp)
                .height(52.dp),
        ) {
            Text(text = "Continuar", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ScanToolButton(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontWeight = FontWeight.Medium, fontSize = 12.sp)
    }
}

// paso4: simplified per the new reference screen (just a spinner + "Procesando...", no
// per-step checklist).
@Composable
internal fun AnalizandoStep(
    selectedPet: Pet?,
    userFullName: String?,
    error: String?,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState()),
    ) {
        BackHandler(onBack = onBack)
        IconButton(onClick = onBack, modifier = Modifier.padding(start = 12.dp, top = 12.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
        }
        GreetingHeader(
            selectedPet = selectedPet,
            userFullName = userFullName,
            hasPets = selectedPet != null,
            onSwitchPetClick = {},
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD0E2DC)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Description, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(56.dp))
            }
            Spacer(modifier = Modifier.height(28.dp))
            Text(text = "Estamos analizando tus documento...", fontWeight = FontWeight.Bold, fontSize = 17.sp, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Clasificamos el tipo de documento y extraemos la información automáticamente.",
                color = SubtitleGray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(28.dp))
            if (error == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = BrandGreen, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "Procesando...", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            } else {
                Text(text = error, color = MaterialTheme.colorScheme.error, fontSize = 14.sp, textAlign = TextAlign.Center)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// Plain (non-Cartilla) path: uploads the scan directly with no analysis step. Same shell
// as AnalizandoStep, but for "Subiendo documento..." -- and, unlike Analizando, an error
// here has no automatic retry path other than backing out to Preview and tapping
// Continuar again, so it needs to actually show up instead of leaving the tap looking
// like it did nothing.
@Composable
private fun UploadingStep(
    selectedPet: Pet?,
    userFullName: String?,
    error: String?,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState()),
    ) {
        BackHandler(onBack = onBack)
        IconButton(onClick = onBack, modifier = Modifier.padding(start = 12.dp, top = 12.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
        }
        GreetingHeader(
            selectedPet = selectedPet,
            userFullName = userFullName,
            hasPets = selectedPet != null,
            onSwitchPetClick = {},
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD0E2DC)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Description, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(56.dp))
            }
            Spacer(modifier = Modifier.height(28.dp))
            Text(text = "Subiendo documento...", fontWeight = FontWeight.Bold, fontSize = 17.sp, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(28.dp))
            if (error == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = BrandGreen, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "Procesando...", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            } else {
                Text(text = error, color = MaterialTheme.colorScheme.error, fontSize = 14.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(onClick = onBack, shape = RoundedCornerShape(28.dp)) { Text("Volver", color = BrandGreen) }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
