package com.petapp.android.features.consultations

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.SelectableDates
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

private sealed interface ConsultaStep {
    data object Form : ConsultaStep
    data object Success : ConsultaStep
}

@Composable
fun RegistrarConsultaScreen(
    selectedPet: Pet?,
    userFullName: String?,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    onViewActivity: () -> Unit,
    viewModel: ConsultationsViewModel = viewModel(),
) {
    var step by remember { mutableStateOf<ConsultaStep>(ConsultaStep.Form) }

    when (step) {
        is ConsultaStep.Form -> ConsultaFormContent(
            selectedPet = selectedPet,
            userFullName = userFullName,
            onBack = onBack,
            onSaved = { step = ConsultaStep.Success },
            viewModel = viewModel,
        )
        is ConsultaStep.Success -> ConsultaSuccessContent(
            selectedPet = selectedPet,
            userFullName = userFullName,
            onViewActivity = onViewActivity,
            onFinish = onFinish,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConsultaFormContent(
    selectedPet: Pet?,
    userFullName: String?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: ConsultationsViewModel,
) {
    val createState by viewModel.createState.collectAsState()

    var motivo by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var sintomas by remember { mutableStateOf("") }
    var hallazgos by remember { mutableStateOf("") }
    var diagnostico by remember { mutableStateOf("") }
    var tratamiento by remember { mutableStateOf("") }
    var veterinario by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    // ConsultationsViewModel is Activity-scoped (no Navigation-Compose back stack), so
    // a prior success would otherwise still be sitting in createState and bounce this
    // screen straight to the success step via the LaunchedEffect below.
    // ConsultationsViewModel is Activity-scoped, so a prior success can still be sitting
    // in createState when this screen re-enters. Resetting it races the effect below
    // (collectAsState's initial value may already have latched onto the stale Success),
    // so consumedInitialState instead always ignores the first firing regardless of what
    // it sees, and only acts on a later, genuine Success from this screen's own save.
    LaunchedEffect(Unit) {
        viewModel.resetCreateState()
    }
    var consumedInitialState by remember { mutableStateOf(false) }
    LaunchedEffect(createState) {
        if (!consumedInitialState) {
            consumedInitialState = true
            return@LaunchedEffect
        }
        if (createState is CreateConsultaUiState.Success) {
            onSaved()
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= System.currentTimeMillis()
                }
            },
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
            Text(text = "Registrar consulta", color = BrandGreen, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(20.dp))

            FormCard {
                Text(text = "Motivo de consulta*", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = motivo,
                    onValueChange = { motivo = it },
                    placeholder = { Text("Ej. Tos y estornudos") },
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
            }

            Spacer(modifier = Modifier.height(16.dp))
            FormCard {
                Text(text = "Síntomas", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(text = "  (opcional)", color = SubtitleGray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = sintomas,
                    onValueChange = { if (it.length <= 200) sintomas = it },
                    placeholder = { Text("Ej. Tos seca, estornudos ocasionales.") },
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = CardBorder,
                        focusedBorderColor = BrandGreen,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "${sintomas.length}/200",
                    color = SubtitleGray,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Hallazgos del examen físico", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(text = "  (opcional)", color = SubtitleGray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = hallazgos,
                    onValueChange = { if (it.length <= 300) hallazgos = it },
                    placeholder = { Text("Temperatura normal. Auscultación cardiopulmonar sin ruidos anormales.") },
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = CardBorder,
                        focusedBorderColor = BrandGreen,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "${hallazgos.length}/300",
                    color = SubtitleGray,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            FormCard {
                Text(text = "Diagnóstico", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(text = "  (opcional)", color = SubtitleGray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = diagnostico,
                    onValueChange = { diagnostico = it },
                    placeholder = { Text("Ej. Rinitis leve") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = CardBorder,
                        focusedBorderColor = BrandGreen,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Tratamiento/Recomendaciones", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(text = "  (opcional)", color = SubtitleGray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = tratamiento,
                    onValueChange = { if (it.length <= 200) tratamiento = it },
                    placeholder = { Text("Doxiciclina 10mg/Kg cada 12hrs por 7 días. Control si no mejora.") },
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = CardBorder,
                        focusedBorderColor = BrandGreen,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "${tratamiento.length}/200",
                    color = SubtitleGray,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Veterinario / Clínica", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(text = "  (opcional)", color = SubtitleGray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = veterinario,
                    onValueChange = { veterinario = it },
                    placeholder = { Text("Clínica Vetra") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = CardBorder,
                        focusedBorderColor = BrandGreen,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            val apiErrorMessage = (createState as? CreateConsultaUiState.Error)?.message
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
            val isLoading = createState is CreateConsultaUiState.Loading
            Button(
                onClick = {
                    val petId = selectedPet?.id
                    when {
                        motivo.isBlank() -> validationError = "Ingresa el motivo de la consulta."
                        petId == null -> validationError = "Agrega una mascota primero."
                        else -> {
                            validationError = null
                            viewModel.createConsulta(
                                petId = petId,
                                motivo = motivo,
                                consultDateIso = date.toString(),
                                sintomas = sintomas,
                                hallazgos = hallazgos,
                                diagnostico = diagnostico,
                                tratamiento = tratamiento,
                                clinicaVeterinario = veterinario,
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
                    Text(text = "Guardar consulta", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = SubtitleGray, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Tu información está protegida y encriptada", color = SubtitleGray, fontSize = 12.sp)
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
private fun ConsultaSuccessContent(
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
            Text(text = "¡Consulta registrada!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Hemos guardado correctamente la consulta.",
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
