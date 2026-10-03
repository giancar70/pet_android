package com.petdrive.app.features.files

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petdrive.app.core.model.CreateVaccineDoseRequest
import com.petdrive.app.core.model.DetectedVaccine
import com.petdrive.app.core.model.Document
import com.petdrive.app.core.model.Pet
import com.petdrive.app.features.main.GreetingHeader
import com.petdrive.app.features.vaccines.RegisterVaccinesUiState
import com.petdrive.app.features.vaccines.VaccinesViewModel

private val ReviewGreen = Color(0xFF406E5F)
private val ReviewGray = Color(0xFF666666)
private val ReviewBorder = Color(0xFFEFEFF4)

private data class VaccineEntryForm(
    val key: Int,
    val vaccineName: String,
    val appliedOn: String,
    val nextDueOn: String,
    val lotNumber: String,
    val selected: Boolean = true,
)

// One editable card per vaccine the AI found on the document (name + dates as plain text --
// already YYYY-MM-DD from the AI, and the simplest thing to correct a misread on), each with
// a checkbox to choose whether it gets registered and a way to drop it entirely. Shared by
// the scan flow (nothing saved yet -- onSave receives the ticked entries) and by
// "Subir archivo" (RevisarVacunasDetectadasStep below).
@Composable
internal fun VaccineReviewContent(
    selectedPet: Pet?,
    userFullName: String?,
    detected: List<DetectedVaccine>,
    saveLabel: (selectedCount: Int) -> String,
    allowSaveWithNone: Boolean,
    isSaving: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onSave: (List<CreateVaccineDoseRequest>) -> Unit,
) {
    var entries by remember {
        mutableStateOf(
            detected.mapIndexed { index, vaccine ->
                VaccineEntryForm(
                    key = index,
                    vaccineName = vaccine.vaccineName ?: "",
                    appliedOn = vaccine.appliedOn ?: "",
                    nextDueOn = vaccine.nextDueOn ?: "",
                    lotNumber = vaccine.lotNumber ?: "",
                )
            },
        )
    }
    var validationError by remember { mutableStateOf<String?>(null) }
    val selectedCount = entries.count { it.selected }

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
            Text(text = "Vacunas detectadas", color = ReviewGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Revisa, corrige y elige cuáles registrar.",
                color = ReviewGray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(20.dp))

            entries.forEachIndexed { index, entry ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, ReviewBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = entry.selected,
                                onCheckedChange = { checked -> entries = entries.map { if (it.key == entry.key) it.copy(selected = checked) else it } },
                                colors = CheckboxDefaults.colors(checkedColor = ReviewGreen),
                            )
                            Text(text = "Vacuna ${index + 1}", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = { entries = entries.filterNot { it.key == entry.key } }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Quitar", tint = Color(0xFFC0392B))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = entry.vaccineName,
                            onValueChange = { value -> entries = entries.map { if (it.key == entry.key) it.copy(vaccineName = value) else it } },
                            label = { Text("Nombre de la vacuna*") },
                            enabled = entry.selected,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = entry.appliedOn,
                            onValueChange = { value -> entries = entries.map { if (it.key == entry.key) it.copy(appliedOn = value) else it } },
                            label = { Text("Fecha de aplicación* (AAAA-MM-DD)") },
                            enabled = entry.selected,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = entry.nextDueOn,
                            onValueChange = { value -> entries = entries.map { if (it.key == entry.key) it.copy(nextDueOn = value) else it } },
                            label = { Text("Próxima dosis (opcional, AAAA-MM-DD)") },
                            enabled = entry.selected,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            if (entries.isEmpty()) {
                Text(
                    text = "No queda ninguna vacuna por registrar.",
                    color = ReviewGray,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }

            val shownError = validationError ?: errorMessage
            if (shownError != null) {
                Text(
                    text = shownError,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
                onClick = {
                    val chosen = entries.filter { it.selected }
                    when {
                        chosen.isEmpty() && !allowSaveWithNone -> validationError = "Elige al menos una vacuna para registrar."
                        chosen.any { it.vaccineName.isBlank() || it.appliedOn.isBlank() } ->
                            validationError = "Completa el nombre y la fecha de aplicación de cada vacuna elegida."
                        else -> {
                            validationError = null
                            onSave(
                                chosen.map {
                                    CreateVaccineDoseRequest(
                                        vaccineName = it.vaccineName.trim(),
                                        appliedOn = it.appliedOn.trim(),
                                        nextDueOn = it.nextDueOn.trim().takeIf { due -> due.isNotBlank() },
                                        lotNumber = it.lotNumber.trim().takeIf { lot -> lot.isNotBlank() },
                                    )
                                },
                            )
                        }
                    }
                },
                enabled = !isSaving,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ReviewGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.height(20.dp))
                } else {
                    Text(text = saveLabel(selectedCount), fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// "Subir archivo" path: the document is already uploaded (typed "Cartilla de vacunas"),
// so confirming registers the ticked vaccines against it right here.
@Composable
internal fun RevisarVacunasDetectadasStep(
    selectedPet: Pet?,
    userFullName: String?,
    document: Document,
    onBack: () -> Unit,
    onRegistered: () -> Unit,
    viewModel: VaccinesViewModel,
) {
    val registerState by viewModel.registerFromDocumentState.collectAsState()
    LaunchedEffect(Unit) { viewModel.resetRegisterFromDocumentState() }
    LaunchedEffect(registerState) {
        if (registerState is RegisterVaccinesUiState.Success) onRegistered()
    }
    VaccineReviewContent(
        selectedPet = selectedPet,
        userFullName = userFullName,
        detected = document.aiJsonResult.orEmpty(),
        saveLabel = { count -> "Registrar $count vacuna${if (count == 1) "" else "s"}" },
        allowSaveWithNone = false,
        isSaving = registerState is RegisterVaccinesUiState.Loading,
        errorMessage = (registerState as? RegisterVaccinesUiState.Error)?.message,
        onBack = onBack,
        onSave = { vaccines ->
            selectedPet?.id?.let { viewModel.registerFromDocument(it, document.id, vaccines) }
        },
    )
}
