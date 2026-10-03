package com.petdrive.app.features.files

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petdrive.app.core.model.CartillaAnalysis
import com.petdrive.app.core.model.CreateDewormingApplicationRequest
import com.petdrive.app.core.model.CreateVaccineDoseRequest
import com.petdrive.app.core.model.DocumentTypeOption
import com.petdrive.app.core.model.Pet
import com.petdrive.app.core.model.UpdatePetRequest
import com.petdrive.app.features.deworming.DewormingViewModel
import com.petdrive.app.features.deworming.RegisterDewormingsUiState
import com.petdrive.app.features.deworming.RegistrarDesparasitacionScreen
import com.petdrive.app.features.main.GreetingHeader
import com.petdrive.app.features.pets.PetDetailScreen
import com.petdrive.app.features.pets.PetsViewModel
import com.petdrive.app.features.pets.UpdatePetUiState
import com.petdrive.app.features.vaccines.RegisterVaccinesUiState
import com.petdrive.app.features.vaccines.RegistrarVacunaScreen
import com.petdrive.app.features.vaccines.VaccinesViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val BrandGreen = Color(0xFF406E5F)
private val SubtitleGray = Color(0xFF666666)
private val CardBorder = Color(0xFFEFEFF4)
private val IncompleteRed = Color(0xFFC0392B)

private sealed interface ReviewStep {
    data object Summary : ReviewStep
    data object EditPet : ReviewStep
    data class EditVaccine(val index: Int) : ReviewStep
    data class EditDeworming(val index: Int) : ReviewStep
    data object Saving : ReviewStep
}

// The 7 fields the AI is expected to read off a Cartilla/Pasaporte -- see feature.md and
// the "X de 7 datos encontrados" header on this screen.
private const val TRACKED_PET_FIELDS = 7

private fun detectedPetFieldCount(analysis: CartillaAnalysis): Int = listOf(
    analysis.petInfo.name,
    analysis.petInfo.species,
    analysis.petInfo.breed,
    analysis.petInfo.sex,
    analysis.petInfo.birthDate,
    analysis.petInfo.weightKg?.toString(),
    analysis.petInfo.microchip,
).count { !it.isNullOrBlank() }

// paso5: three tappable modules (pet info, vaccines, deworming) built from what the AI
// found on the Cartilla/Pasaporte scan. Tapping one opens the REAL form/editor in draft
// mode to verify/correct it -- nothing is persisted until "Guardar registros", which
// always updates the currently selected pet (never creates a new one) and replaces all of
// its fields except the photo.
@Composable
fun RevisarInformacionDetectadaScreen(
    selectedPet: Pet?,
    userFullName: String?,
    analysis: CartillaAnalysis,
    onBack: () -> Unit,
    onSaved: (vaccineCount: Int, dewormingCount: Int) -> Unit,
    mergedImageBytes: suspend () -> ByteArray,
    fileName: String,
    filesViewModel: FilesViewModel,
    vaccinesViewModel: VaccinesViewModel,
    dewormingViewModel: DewormingViewModel,
    petsViewModel: PetsViewModel = viewModel(),
) {
    val pet = selectedPet
    if (pet == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val scope = rememberCoroutineScope()
    var reviewStep by remember { mutableStateOf<ReviewStep>(ReviewStep.Summary) }
    var saveError by remember { mutableStateOf<String?>(null) }

    var petDraft by remember(analysis, pet.id) {
        mutableStateOf(
            PetInfoDraft(
                name = analysis.petInfo.name ?: pet.name,
                species = analysis.petInfo.species ?: pet.species,
                breed = pet.breed,
                sex = analysis.petInfo.sex ?: pet.sex,
                birthDate = analysis.petInfo.birthDate ?: pet.birthDate,
                weightKg = analysis.petInfo.weightKg?.toString() ?: pet.weightKg,
                color = pet.color,
                microchip = analysis.petInfo.microchip ?: pet.microchip,
                notes = pet.notes,
            ),
        )
    }
    var vaccineDrafts by remember(analysis) {
        mutableStateOf(
            analysis.vaccines.map {
                VaccineDraft(
                    vaccineName = it.vaccineName ?: "",
                    appliedOn = it.appliedOn ?: "",
                    nextDueOn = it.nextDueOn,
                    lotNumber = it.lotNumber,
                )
            },
        )
    }
    var dewormingDrafts by remember(analysis) {
        mutableStateOf(
            analysis.dewormings.map {
                DewormingDraft(
                    dewormingType = it.dewormingType ?: "internal",
                    productName = it.productName,
                    appliedOn = it.appliedOn ?: "",
                    nextDueOn = it.nextDueOn,
                    durationMonths = it.durationMonths,
                )
            },
        )
    }

    fun startSave() {
        reviewStep = ReviewStep.Saving
        saveError = null
        scope.launch {
            try {
                val petChanged = petDraft.name != pet.name ||
                    petDraft.species != pet.species ||
                    petDraft.breed != pet.breed ||
                    petDraft.sex != pet.sex ||
                    petDraft.birthDate != pet.birthDate ||
                    petDraft.weightKg != pet.weightKg ||
                    petDraft.color != pet.color ||
                    petDraft.microchip != pet.microchip ||
                    petDraft.notes != pet.notes
                if (petChanged) {
                    petsViewModel.updatePet(
                        pet.id,
                        UpdatePetRequest(
                            name = petDraft.name,
                            species = petDraft.species,
                            breed = petDraft.breed,
                            sex = petDraft.sex,
                            birthDate = petDraft.birthDate,
                            weightKg = petDraft.weightKg,
                            color = petDraft.color,
                            microchip = petDraft.microchip,
                            notes = petDraft.notes,
                        ),
                    )
                    val result = petsViewModel.updateState.first { it !is UpdatePetUiState.Loading }
                    if (result is UpdatePetUiState.Error) {
                        saveError = result.message
                        reviewStep = ReviewStep.Summary
                        return@launch
                    }
                }

                val bytes = mergedImageBytes()
                filesViewModel.uploadDocument(pet.id, bytes, fileName, "image/jpeg", DocumentTypeOption.VACCINE_CARD.apiValue)
                val uploadResult = filesViewModel.uploadState.first { it !is UploadDocumentUiState.Loading }
                val documentId = when (uploadResult) {
                    is UploadDocumentUiState.Success -> uploadResult.document.id
                    is UploadDocumentUiState.Error -> {
                        saveError = uploadResult.message
                        reviewStep = ReviewStep.Summary
                        return@launch
                    }
                    else -> {
                        saveError = "No se pudo subir el documento."
                        reviewStep = ReviewStep.Summary
                        return@launch
                    }
                }

                if (vaccineDrafts.isNotEmpty()) {
                    vaccinesViewModel.registerFromDocument(
                        pet.id,
                        documentId,
                        vaccineDrafts.map {
                            CreateVaccineDoseRequest(
                                vaccineName = it.vaccineName,
                                appliedOn = it.appliedOn,
                                nextDueOn = it.nextDueOn,
                                lotNumber = it.lotNumber,
                                notes = it.notes,
                            )
                        },
                    )
                    val vaccineResult = vaccinesViewModel.registerFromDocumentState.first { it !is RegisterVaccinesUiState.Loading }
                    if (vaccineResult is RegisterVaccinesUiState.Error) {
                        saveError = vaccineResult.message
                        reviewStep = ReviewStep.Summary
                        return@launch
                    }
                }

                if (dewormingDrafts.isNotEmpty()) {
                    dewormingViewModel.registerFromDocument(
                        pet.id,
                        documentId,
                        dewormingDrafts.map {
                            CreateDewormingApplicationRequest(
                                dewormingType = it.dewormingType,
                                appliedOn = it.appliedOn,
                                nextDueOn = it.nextDueOn,
                                durationMonths = it.durationMonths,
                                productName = it.productName,
                                notes = it.notes,
                            )
                        },
                    )
                    val dewormingResult = dewormingViewModel.registerFromDocumentState.first { it !is RegisterDewormingsUiState.Loading }
                    if (dewormingResult is RegisterDewormingsUiState.Error) {
                        saveError = dewormingResult.message
                        reviewStep = ReviewStep.Summary
                        return@launch
                    }
                }

                onSaved(vaccineDrafts.size, dewormingDrafts.size)
            } catch (e: Exception) {
                saveError = e.message ?: "No se pudo guardar la información."
                reviewStep = ReviewStep.Summary
            }
        }
    }

    when (val current = reviewStep) {
        ReviewStep.Summary -> ReviewSummaryStep(
            selectedPet = pet,
            userFullName = userFullName,
            detectedCount = remember(analysis) { detectedPetFieldCount(analysis) },
            vaccineDrafts = vaccineDrafts,
            dewormingDrafts = dewormingDrafts,
            saveError = saveError,
            onBack = onBack,
            onEditPet = { reviewStep = ReviewStep.EditPet },
            onEditVaccine = { reviewStep = ReviewStep.EditVaccine(it) },
            onEditDeworming = { reviewStep = ReviewStep.EditDeworming(it) },
            onSaveClick = ::startSave,
        )
        ReviewStep.EditPet -> PetDetailScreen(
            pet = pet,
            onBack = { reviewStep = ReviewStep.Summary },
            draft = petDraft,
            onDraftSaved = { petDraft = it },
        )
        is ReviewStep.EditVaccine -> RegistrarVacunaScreen(
            selectedPet = pet,
            userFullName = userFullName,
            draft = vaccineDrafts[current.index],
            onDraftSaved = { updated -> vaccineDrafts = vaccineDrafts.toMutableList().also { it[current.index] = updated } },
            onRemoveDraft = { vaccineDrafts = vaccineDrafts.toMutableList().also { it.removeAt(current.index) } },
            onBack = { reviewStep = ReviewStep.Summary },
        )
        is ReviewStep.EditDeworming -> RegistrarDesparasitacionScreen(
            selectedPet = pet,
            userFullName = userFullName,
            draft = dewormingDrafts[current.index],
            onDraftSaved = { updated -> dewormingDrafts = dewormingDrafts.toMutableList().also { it[current.index] = updated } },
            onRemoveDraft = { dewormingDrafts = dewormingDrafts.toMutableList().also { it.removeAt(current.index) } },
            onBack = { reviewStep = ReviewStep.Summary },
        )
        ReviewStep.Saving -> GuardandoRegistrosStep(selectedPet = pet, userFullName = userFullName)
    }
}

@Composable
private fun ReviewSummaryStep(
    selectedPet: Pet,
    userFullName: String?,
    detectedCount: Int,
    vaccineDrafts: List<VaccineDraft>,
    dewormingDrafts: List<DewormingDraft>,
    saveError: String?,
    onBack: () -> Unit,
    onEditPet: () -> Unit,
    onEditVaccine: (Int) -> Unit,
    onEditDeworming: (Int) -> Unit,
    onSaveClick: () -> Unit,
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
            hasPets = true,
            onSwitchPetClick = {},
        )

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(text = "Revisa la información detectada", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Verifica o corrige cada dato antes de guardarlo en el perfil de ${selectedPet.name}.",
                color = SubtitleGray,
                fontSize = 13.sp,
            )
            Spacer(modifier = Modifier.height(20.dp))

            ReviewModuleCard(
                icon = Icons.Filled.Pets,
                title = "Datos de la mascota",
                subtitle = "$detectedCount de $TRACKED_PET_FIELDS datos encontrados",
                onClick = onEditPet,
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Vacunas detectadas", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(8.dp))
            if (vaccineDrafts.isEmpty()) {
                EmptyDetectionNote(text = "No se detectaron vacunas en este documento.")
            } else {
                vaccineDrafts.forEachIndexed { index, draft ->
                    Spacer(modifier = Modifier.height(if (index == 0) 0.dp else 10.dp))
                    ReviewItemCard(
                        icon = Icons.Filled.Vaccines,
                        title = draft.vaccineName.ifBlank { "Vacuna sin nombre" },
                        subtitle = draft.appliedOn.ifBlank { "Sin fecha" },
                        incomplete = draft.vaccineName.isBlank() || draft.appliedOn.isBlank(),
                        onClick = { onEditVaccine(index) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Desparasitaciones detectadas", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(8.dp))
            if (dewormingDrafts.isEmpty()) {
                EmptyDetectionNote(text = "No se detectaron desparasitaciones en este documento.")
            } else {
                dewormingDrafts.forEachIndexed { index, draft ->
                    Spacer(modifier = Modifier.height(if (index == 0) 0.dp else 10.dp))
                    ReviewItemCard(
                        icon = Icons.Filled.BugReport,
                        title = draft.productName?.takeIf { it.isNotBlank() } ?: "Desparasitación sin producto",
                        subtitle = draft.appliedOn.ifBlank { "Sin fecha" },
                        incomplete = draft.appliedOn.isBlank(),
                        onClick = { onEditDeworming(index) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            if (saveError != null) {
                Text(
                    text = saveError,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            Text(
                text = "El documento escaneado se guardará en la historia de ${selectedPet.name} junto con estos registros.",
                color = SubtitleGray,
                fontSize = 12.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onSaveClick,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
            ) {
                Text(text = "Guardar registros", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 6.dp))
            }
        }
    }
}

@Composable
private fun ReviewModuleCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, CardBorder),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = BrandGreen)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(text = subtitle, color = SubtitleGray, fontSize = 13.sp)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = SubtitleGray)
        }
    }
}

@Composable
private fun ReviewItemCard(icon: ImageVector, title: String, subtitle: String, incomplete: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, CardBorder),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = BrandGreen)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(text = subtitle, color = SubtitleGray, fontSize = 12.sp)
            }
            if (incomplete) {
                Surface(shape = RoundedCornerShape(50), color = IncompleteRed.copy(alpha = 0.12f)) {
                    Text(
                        text = "Incompleta",
                        color = IncompleteRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = SubtitleGray)
        }
    }
}

@Composable
private fun EmptyDetectionNote(text: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF7F7F7),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            color = SubtitleGray,
            fontSize = 13.sp,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun GuardandoRegistrosStep(selectedPet: Pet?, userFullName: String?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        GreetingHeader(
            selectedPet = selectedPet,
            userFullName = userFullName,
            hasPets = selectedPet != null,
            onSwitchPetClick = {},
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(color = BrandGreen)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Guardando registros...", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}
