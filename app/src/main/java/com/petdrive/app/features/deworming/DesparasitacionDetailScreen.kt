package com.petdrive.app.features.deworming

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petdrive.app.core.model.DewormingApplication
import com.petdrive.app.core.model.Pet
import com.petdrive.app.core.util.relativeDateLabel
import com.petdrive.app.features.incidents.spanishDate
import com.petdrive.app.features.main.GreetingHeader
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val BrandGreen = Color(0xFF406E5F)
private val SubtitleGray = Color(0xFF666666)
private val CardBorder = Color(0xFFEFEFF4)
private val DeleteRed = Color(0xFFC0392B)

@Composable
fun DesparasitacionDetailScreen(
    selectedPet: Pet?,
    userFullName: String?,
    applicationId: String,
    onBack: () -> Unit,
    onRenovar: () -> Unit = {},
    viewModel: DewormingViewModel = viewModel(),
) {
    val detailState by viewModel.detailState.collectAsState()
    val deleteState by viewModel.deleteState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(selectedPet?.id, applicationId) {
        selectedPet?.id?.let { viewModel.fetchDesparasitacionDetail(it, applicationId) }
    }

    // DewormingViewModel is Activity-scoped, so a prior deletion's Success can still be
    // sitting in deleteState when this screen re-enters for a different application;
    // ignore the first firing regardless of what it holds, and only act on a later,
    // genuine Success from this screen's own delete.
    LaunchedEffect(Unit) {
        viewModel.resetDeleteState()
        viewModel.resetUpdateState()
    }
    var consumedInitialDeleteState by remember { mutableStateOf(false) }
    LaunchedEffect(deleteState) {
        if (!consumedInitialDeleteState) {
            consumedInitialDeleteState = true
            return@LaunchedEffect
        }
        if (deleteState is DeleteDewormingUiState.Success) onBack()
    }

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
            Text(text = "Detalle de desparasitación", color = BrandGreen, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(20.dp))

            when (val state = detailState) {
                is DewormingDetailUiState.Loading -> LoadingBox()
                is DewormingDetailUiState.Error -> Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                )
                is DewormingDetailUiState.Loaded -> DesparasitacionDetailContent(
                    selectedPet = selectedPet,
                    application = state.application,
                    viewModel = viewModel,
                    onRenovar = onRenovar,
                )
            }

            if (deleteState is DeleteDewormingUiState.Error) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = (deleteState as DeleteDewormingUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                )
            }

            if (selectedPet?.canEdit != false) {
                Spacer(modifier = Modifier.height(20.dp))
                val isDeleting = deleteState is DeleteDewormingUiState.Loading
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    enabled = !isDeleting,
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DeleteRed),
                    border = BorderStroke(1.dp, DeleteRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(color = DeleteRed, strokeWidth = 2.dp, modifier = Modifier.height(20.dp))
                    } else {
                        Icon(Icons.Filled.Delete, contentDescription = null, tint = DeleteRed, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Eliminar desparasitación", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar desparasitación") },
            text = { Text("¿Estás seguro de que deseas eliminar este registro de desparasitación? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        selectedPet?.id?.let { viewModel.deleteDesparasitacion(it, applicationId) }
                    },
                ) { Text("Eliminar", color = DeleteRed) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar", color = BrandGreen) }
            },
        )
    }
}

// Holds the renewal-date edit state, scoped to (and reset whenever) the loaded application
// changes -- "Guardar" PATCHes just that field in place
// (DewormingViewModel.updateNextDueOn); it never touches the rest of the record. "Renovar"
// is the separate, always-available path into "Registrar desparasitación" for logging a
// brand new application instead.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DesparasitacionDetailContent(
    selectedPet: Pet?,
    application: DewormingApplication,
    viewModel: DewormingViewModel,
    onRenovar: () -> Unit,
) {
    val updateState by viewModel.updateState.collectAsState()
    var nextDueDate by remember(application.id) {
        mutableStateOf(application.nextDueOn?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() })
    }
    var showDatePicker by remember(application.id) { mutableStateOf(false) }
    val hasChanges = nextDueDate?.toString() != application.nextDueOn?.takeIf { it.isNotBlank() }
    val canEdit = selectedPet?.canEdit != false
    // Unlike vaccines, "Renovar" here isn't gated to being due soon -- always offered
    // for an active (not yet superseded) record, per explicit request.
    val canRenovar = application.status != "replaced"

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = (nextDueDate ?: LocalDate.now())
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        nextDueDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    DetailCard {
        DetailRow("Producto", application.productName?.takeIf { it.isNotBlank() } ?: "Desparasitación")
        HorizontalDivider(color = CardBorder)
        DetailRow("Fecha de aplicación", formatIsoDate(application.appliedOn))
        application.durationMonths?.let {
            HorizontalDivider(color = CardBorder)
            DetailRow("Duración", if (it == 1) "1 mes" else "$it meses")
        }
        application.notes?.takeIf { it.isNotBlank() }?.let {
            HorizontalDivider(color = CardBorder)
            DetailRow("Observaciones", it)
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    Text(text = "Fecha de renovación", fontSize = 14.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = nextDueDate?.let { spanishDate(it) } ?: "",
                onValueChange = {},
                readOnly = true,
                enabled = canEdit,
                singleLine = true,
                placeholder = { Text("Selecciona una fecha") },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = CardBorder,
                    focusedBorderColor = BrandGreen,
                    disabledBorderColor = CardBorder,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            if (canEdit) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showDatePicker = true },
                )
            }
        }
        if (canEdit) {
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(BrandGreen, RoundedCornerShape(12.dp))
                    .clickable { showDatePicker = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = Color.White)
            }
        }
    }

    if (updateState is UpdateDewormingUiState.Error) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = (updateState as UpdateDewormingUiState.Error).message,
            color = MaterialTheme.colorScheme.error,
            fontSize = 13.sp,
        )
    }

    if (canEdit && hasChanges) {
        Spacer(modifier = Modifier.height(12.dp))
        val isSaving = updateState is UpdateDewormingUiState.Loading
        Button(
            onClick = {
                val petId = selectedPet?.id ?: return@Button
                val date = nextDueDate ?: return@Button
                viewModel.updateNextDueOn(petId, application.id, date.toString())
            },
            enabled = !isSaving && nextDueDate != null,
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            if (isSaving) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.height(20.dp))
            } else {
                Text(text = "Guardar", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (canRenovar && canEdit) {
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onRenovar,
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Icon(Icons.Filled.Autorenew, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Renovar", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DetailCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp), content = content)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 14.dp)) {
        Text(text = label, color = SubtitleGray, fontSize = 12.sp)
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
private fun LoadingBox() {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = BrandGreen)
    }
}

private fun formatIsoDate(iso: String): String = relativeDateLabel(iso)
