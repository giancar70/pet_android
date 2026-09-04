package com.petapp.android.features.vaccines

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.material3.SelectableDates
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petapp.android.core.model.Pet
import com.petapp.android.features.incidents.SuccessCheckmark
import com.petapp.android.features.incidents.spanishDate
import com.petapp.android.features.main.GreetingHeader
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val BrandGreen = Color(0xFF406E5F)
private val SubtitleGray = Color(0xFF666666)
private val CardBorder = Color(0xFFEFEFF4)

private data class SavedVacuna(val name: String, val dateDisplay: String)

private sealed interface VacunaStep {
    data object Form : VacunaStep
    data class Success(val vacuna: SavedVacuna) : VacunaStep
}

@Composable
fun RegistrarVacunaScreen(
    selectedPet: Pet?,
    userFullName: String?,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    onViewActivity: () -> Unit,
    viewModel: VaccinesViewModel = viewModel(),
) {
    var step by remember { mutableStateOf<VacunaStep>(VacunaStep.Form) }

    when (val current = step) {
        is VacunaStep.Form -> VacunaFormContent(
            selectedPet = selectedPet,
            userFullName = userFullName,
            onBack = onBack,
            onSaved = { step = VacunaStep.Success(it) },
            viewModel = viewModel,
        )
        is VacunaStep.Success -> VacunaSuccessContent(
            selectedPet = selectedPet,
            userFullName = userFullName,
            onViewActivity = onViewActivity,
            onFinish = onFinish,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VacunaFormContent(
    selectedPet: Pet?,
    userFullName: String?,
    onBack: () -> Unit,
    onSaved: (SavedVacuna) -> Unit,
    viewModel: VaccinesViewModel,
) {
    val createState by viewModel.createState.collectAsState()

    var vacunaAplicada by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var nextDueDate by remember { mutableStateOf<LocalDate?>(null) }
    var lote by remember { mutableStateOf("") }
    var observaciones by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showNextDueDatePicker by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    // Recomputed every recomposition (cheap) so it reacts immediately to either date
    // picker without a separate effect -- próxima vacunación is optional, so there's
    // nothing to validate while it's unset.
    val nextDueError = nextDueDate?.let { nextDue ->
        if (!nextDue.isAfter(date)) {
            "La próxima vacunación debe ser posterior a la fecha de aplicación."
        } else {
            null
        }
    }

    // VaccinesViewModel is Activity-scoped (no Navigation-Compose back stack), so a
    // prior success can still be sitting in createState when this screen re-enters —
    // resetting it here races the LaunchedEffect(createState) below (collectAsState's
    // initial value may already have latched onto the stale Success before the reset's
    // StateFlow emission propagates), which could bounce straight to the success step.
    // Guarding with consumedInitialState sidesteps the race entirely: the first firing
    // of LaunchedEffect(createState) is always ignored regardless of what it sees, and
    // only a later, genuine transition (Loading -> Success from this screen's own save)
    // triggers onSaved.
    LaunchedEffect(Unit) {
        viewModel.resetCreateState()
    }
    var consumedInitialState by remember { mutableStateOf(false) }
    LaunchedEffect(createState) {
        if (!consumedInitialState) {
            consumedInitialState = true
            return@LaunchedEffect
        }
        val state = createState
        if (state is CreateVaccineDoseUiState.Success) {
            onSaved(SavedVacuna(name = state.dose.vaccine, dateDisplay = spanishDate(date)))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= System.currentTimeMillis()
                }
            }
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

    if (showNextDueDatePicker) {
        val nextDueDatePickerState = rememberDatePickerState(
            initialSelectedDateMillis = (nextDueDate ?: date)
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showNextDueDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = nextDueDatePickerState.selectedDateMillis
                    if (millis != null) {
                        nextDueDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showNextDueDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showNextDueDatePicker = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = nextDueDatePickerState)
        }
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

        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "Registrar Vacuna", color = BrandGreen, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(20.dp))

            FormCard {
                Text(text = "Vacuna aplicada*", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = vacunaAplicada,
                    onValueChange = { vacunaAplicada = it },
                    placeholder = { Text("Ej: Rabia, Óctuple, Leishmania...") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = CardBorder,
                        focusedBorderColor = BrandGreen,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Fecha de aplicación*", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = spanishDate(date),
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = CardBorder,
                                focusedBorderColor = BrandGreen,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showDatePicker = true },
                        )
                    }
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

                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Próxima vacunación", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(text = "  (opcional)", color = SubtitleGray, fontSize = 10.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = nextDueDate?.let { spanishDate(it) } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            placeholder = { Text("Selecciona una fecha") },
                            isError = nextDueError != null,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = CardBorder,
                                focusedBorderColor = BrandGreen,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showNextDueDatePicker = true },
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(BrandGreen, RoundedCornerShape(12.dp))
                            .clickable { showNextDueDatePicker = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = Color.White)
                    }
                }
                if (nextDueError != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = nextDueError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            FormCard {
                Text(text = "N° de lote", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(text = "  (opcional)", color = SubtitleGray, fontSize = 10.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = lote,
                    onValueChange = { lote = it },
                    placeholder = { Text("Ej: A123845") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = CardBorder,
                        focusedBorderColor = BrandGreen,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            FormCard {
                Text(text = "Observaciones", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(text = "  (opcional)", color = SubtitleGray, fontSize = 10.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = observaciones,
                    onValueChange = { if (it.length <= 300) observaciones = it },
                    placeholder = { Text("Ej: Estado general, reacciones, notas...") },
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = CardBorder,
                        focusedBorderColor = BrandGreen,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "${observaciones.length}/300",
                    color = SubtitleGray,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            val apiErrorMessage = (createState as? CreateVaccineDoseUiState.Error)?.message
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
            val isLoading = createState is CreateVaccineDoseUiState.Loading
            Button(
                onClick = {
                    val petId = selectedPet?.id
                    when {
                        vacunaAplicada.isBlank() -> validationError = "Ingresa el nombre de la vacuna aplicada."
                        petId == null -> validationError = "Agrega una mascota primero."
                        else -> {
                            validationError = null
                            viewModel.createVacuna(
                                petId = petId,
                                vaccineName = vacunaAplicada,
                                appliedOnIso = date.toString(),
                                nextDueOnIso = nextDueDate?.toString(),
                                lotNumber = lote,
                                notes = observaciones,
                            )
                        }
                    }
                },
                enabled = !isLoading && nextDueError == null,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.height(20.dp))
                } else {
                    Text(text = "Guardar vacuna", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = SubtitleGray, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Tu información está protegida y encriptada", color = SubtitleGray, fontSize = 10.sp)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun FormCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun VacunaSuccessContent(
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
            Text(text = "¡Vacuna registrada!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Hemos guardado correctamente la vacuna aplicada.",
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
