package com.petapp.android.features.reminders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PeopleAlt
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petapp.android.core.model.Pet
import com.petapp.android.core.model.ReminderCategory
import com.petapp.android.core.model.ReminderFrequency
import com.petapp.android.features.incidents.SuccessCheckmark
import com.petapp.android.features.incidents.TimePickerDialogSheet
import com.petapp.android.features.incidents.spanishDate
import com.petapp.android.features.main.GreetingHeader
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

private val BrandGreen = Color(0xFF406E5F)
private val SubtitleGray = Color(0xFF666666)
private val CardBorder = Color(0xFFEFEFF4)
private val IconCircleBg = Color(0xFFD9FEF2)

private sealed interface RecordatorioStep {
    data object Form : RecordatorioStep
    data object Success : RecordatorioStep
}

private fun iconFor(category: ReminderCategory): ImageVector = when (category) {
    ReminderCategory.VACCINE -> Icons.Filled.Vaccines
    ReminderCategory.DEWORMING -> Icons.Filled.Medication
    ReminderCategory.ANTIPARASITIC -> Icons.Filled.Shield
    ReminderCategory.MEDICATION -> Icons.Filled.LocalPharmacy
    ReminderCategory.CONSULTATION -> Icons.Filled.MedicalServices
    ReminderCategory.SURGERY -> Icons.Filled.ContentCut
    ReminderCategory.TEST -> Icons.Filled.Biotech
    ReminderCategory.DIET -> Icons.Filled.Restaurant
    ReminderCategory.WEIGHT -> Icons.Filled.MonitorWeight
    ReminderCategory.OTHER -> Icons.Filled.MoreHoriz
}

@Composable
fun AnadirRecordatorioScreen(
    selectedPet: Pet?,
    userFullName: String?,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    onViewActivity: () -> Unit,
    viewModel: RemindersViewModel = viewModel(),
) {
    var step by remember { mutableStateOf<RecordatorioStep>(RecordatorioStep.Form) }

    when (step) {
        is RecordatorioStep.Form -> RecordatorioFormContent(
            selectedPet = selectedPet,
            userFullName = userFullName,
            onBack = onBack,
            onSaved = { step = RecordatorioStep.Success },
            viewModel = viewModel,
        )
        is RecordatorioStep.Success -> RecordatorioSuccessContent(
            selectedPet = selectedPet,
            userFullName = userFullName,
            onViewActivity = onViewActivity,
            onFinish = onFinish,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordatorioFormContent(
    selectedPet: Pet?,
    userFullName: String?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: RemindersViewModel,
) {
    val createState by viewModel.createState.collectAsState()

    var category by remember { mutableStateOf(ReminderCategory.VACCINE) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var time by remember { mutableStateOf(LocalTime.now()) }
    var frequency by remember { mutableStateOf(ReminderFrequency.NONE) }
    var customDays by remember { mutableStateOf("") }
    var nota by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showFrequencyDialog by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    // RemindersViewModel is Activity-scoped (no Navigation-Compose back stack), so a
    // prior success would otherwise still be sitting in createState and bounce this
    // screen straight to the success step via the LaunchedEffect below.
    LaunchedEffect(Unit) {
        viewModel.resetCreateState()
    }
    LaunchedEffect(createState) {
        if (createState is CreateReminderUiState.Success) {
            onSaved()
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
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

    if (showTimePicker) {
        TimePickerDialogSheet(
            initialTime = time,
            onDismissRequest = { showTimePicker = false },
            onConfirm = {
                time = it
                showTimePicker = false
            },
        )
    }

    if (showFrequencyDialog) {
        FrequencyDialog(
            selected = frequency,
            customDays = customDays,
            onCustomDaysChange = { customDays = it },
            onSelect = { frequency = it },
            onDismiss = { showFrequencyDialog = false },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState()),
    ) {
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
            Text(text = "Añadir recordatorio", fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Tipo de recordatorio", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(190.dp),
            ) {
                items(ReminderCategory.entries) { option ->
                    CategoryTile(
                        icon = iconFor(option),
                        label = option.label,
                        selected = category == option,
                        onClick = { category = option },
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            RecordatorioRow(
                icon = Icons.Filled.Event,
                title = "Fecha y hora",
                subtitle = "${spanishDate(date)} - %02d:%02d".format(time.hour, time.minute),
                onClick = { showDatePicker = true },
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row {
                Box(modifier = Modifier.weight(1f)) {
                    RecordatorioRow(
                        icon = Icons.Filled.Repeat,
                        title = "Frecuencia",
                        subtitle = frequencySubtitle(frequency, customDays),
                        onClick = { showFrequencyDialog = true },
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            RecordatorioRow(
                icon = Icons.Filled.EditNote,
                title = "Nota adicional",
                subtitle = if (nota.isBlank()) "Opcional — se usa como título del recordatorio" else nota,
                onClick = null,
            ) {
                OutlinedTextField(
                    value = nota,
                    onValueChange = { if (it.length <= 180) nota = it },
                    placeholder = { Text("Ej: Vacuna múltiple (quíntuple)") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = CardBorder,
                        focusedBorderColor = BrandGreen,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            RecordatorioRow(icon = Icons.Filled.NotificationsActive, title = "Aviso previo", subtitle = "Próximamente disponible", onClick = null)
            Spacer(modifier = Modifier.height(10.dp))
            RecordatorioRow(icon = Icons.Filled.PeopleAlt, title = "Añadir personas", subtitle = "Comparte este recordatorio · Próximamente", onClick = null)
            Spacer(modifier = Modifier.height(10.dp))
            RecordatorioRow(icon = Icons.Filled.LocationOn, title = "Añadir ubicación", subtitle = "Opcional · Próximamente", onClick = null)

            Spacer(modifier = Modifier.height(24.dp))
            val apiErrorMessage = (createState as? CreateReminderUiState.Error)?.message
            if (validationError != null || apiErrorMessage != null) {
                Text(
                    text = validationError ?: apiErrorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            val isLoading = createState is CreateReminderUiState.Loading
            Button(
                onClick = {
                    val petId = selectedPet?.id
                    val days = customDays.toIntOrNull()
                    when {
                        petId == null -> validationError = "Agrega una mascota primero."
                        frequency == ReminderFrequency.CUSTOM_DAYS && (days == null || days <= 0) ->
                            validationError = "Ingresa cada cuántos días se repite."
                        else -> {
                            validationError = null
                            val title = nota.takeIf { it.isNotBlank() } ?: "Recordatorio de ${category.label}"
                            viewModel.createRecordatorio(
                                petId = petId,
                                category = category.apiValue,
                                title = title,
                                dueDateIso = date.toString(),
                                frequency = frequency.apiValue,
                                customDays = if (frequency == ReminderFrequency.CUSTOM_DAYS) days else null,
                                notifyPush = true,
                            )
                        }
                    }
                },
                enabled = !isLoading,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.height(20.dp))
                } else {
                    Text(text = "Guardar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private fun frequencySubtitle(frequency: ReminderFrequency, customDays: String): String =
    if (frequency == ReminderFrequency.CUSTOM_DAYS) {
        val days = customDays.toIntOrNull()
        if (days != null) "Cada $days días" else frequency.label
    } else {
        frequency.label
    }

@Composable
private fun CategoryTile(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) BrandGreen else BrandGreen.copy(alpha = 0.55f))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

@Composable
private fun RecordatorioRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
    expandedContent: (@Composable () -> Unit)? = null,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = if (onClick != null) Modifier.fillMaxWidth().clickable(onClick = onClick) else Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(IconCircleBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = subtitle, color = SubtitleGray, fontSize = 12.sp)
                }
                if (onClick != null) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = SubtitleGray)
                }
            }
            if (expandedContent != null) {
                expandedContent()
            }
        }
    }
}

@Composable
private fun FrequencyDialog(
    selected: ReminderFrequency,
    customDays: String,
    onCustomDaysChange: (String) -> Unit,
    onSelect: (ReminderFrequency) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = "Frecuencia", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))
                ReminderFrequency.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(if (selected == option) BrandGreen else CardBorder),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected == option) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = option.label, fontSize = 15.sp)
                    }
                }
                if (selected == ReminderFrequency.CUSTOM_DAYS) {
                    OutlinedTextField(
                        value = customDays,
                        onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) onCustomDaysChange(it) },
                        placeholder = { Text("Ej: 15") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = CardBorder,
                            focusedBorderColor = BrandGreen,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Listo") }
                }
            }
        }
    }
}

@Composable
private fun RecordatorioSuccessContent(
    selectedPet: Pet?,
    userFullName: String?,
    onViewActivity: () -> Unit,
    onFinish: () -> Unit,
) {
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
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SuccessCheckmark()
            Spacer(modifier = Modifier.height(20.dp))
            Text(text = "¡Recordatorio guardado!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Te avisaremos cuando se acerque la fecha.",
                color = SubtitleGray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Button(
                onClick = onViewActivity,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text(text = "Ver en Actividad", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onFinish,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandGreen),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text(text = "Finalizar", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
