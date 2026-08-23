package com.petapp.android.features.deworming

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petapp.android.core.model.AdministrationRoute
import com.petapp.android.core.model.DewormingPresentation
import com.petapp.android.core.model.DewormingType
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

private sealed interface DesparasitacionStep {
    data object Form : DesparasitacionStep
    data object Success : DesparasitacionStep
}

@Composable
fun RegistrarDesparasitacionScreen(
    selectedPet: Pet?,
    userFullName: String?,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    onViewActivity: () -> Unit,
    viewModel: DewormingViewModel = viewModel(),
) {
    var step by remember { mutableStateOf<DesparasitacionStep>(DesparasitacionStep.Form) }

    when (step) {
        is DesparasitacionStep.Form -> DesparasitacionFormContent(
            selectedPet = selectedPet,
            userFullName = userFullName,
            onBack = onBack,
            onSaved = { step = DesparasitacionStep.Success },
            viewModel = viewModel,
        )
        is DesparasitacionStep.Success -> DesparasitacionSuccessContent(
            selectedPet = selectedPet,
            userFullName = userFullName,
            onViewActivity = onViewActivity,
            onFinish = onFinish,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DesparasitacionFormContent(
    selectedPet: Pet?,
    userFullName: String?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: DewormingViewModel,
) {
    val createState by viewModel.createState.collectAsState()

    var tipo by remember { mutableStateOf(DewormingType.INTERNAL) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var producto by remember { mutableStateOf("") }
    var presentacion by remember { mutableStateOf(DewormingPresentation.TABLET) }
    var via by remember { mutableStateOf(AdministrationRoute.ORAL) }
    var peso by remember { mutableStateOf("") }
    var observaciones by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    // DewormingViewModel is Activity-scoped (no Navigation-Compose back stack), so a
    // prior success would otherwise still be sitting in createState and bounce this
    // screen straight to the success step via the LaunchedEffect below.
    LaunchedEffect(Unit) {
        viewModel.resetCreateState()
    }
    LaunchedEffect(createState) {
        if (createState is CreateDewormingUiState.Success) {
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
            Text(text = "Registrar Desparasitación", color = BrandGreen, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(20.dp))

            FormCard {
                Text(text = "Tipo*", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DewormingType.entries.forEach { option ->
                        TypeChip(
                            label = option.label,
                            selected = tipo == option,
                            onClick = { tipo = option },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

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
                Text(text = "Producto", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(text = "  (Recomendado)", color = SubtitleGray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = producto,
                    onValueChange = { producto = it },
                    placeholder = { Text("Ej: Bravecto") },
                    singleLine = true,
                    trailingIcon = { Icon(Icons.Filled.ChevronRight, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = CardBorder,
                        focusedBorderColor = BrandGreen,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DropdownSelector(
                        label = "Presentación",
                        selected = presentacion,
                        options = DewormingPresentation.entries,
                        optionLabel = { it.label },
                        onSelected = { presentacion = it },
                        modifier = Modifier.weight(1f),
                    )
                    DropdownSelector(
                        label = "Vía",
                        selected = via,
                        options = AdministrationRoute.entries,
                        optionLabel = { it.label },
                        onSelected = { via = it },
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = peso,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() || c == '.' }) peso = it },
                        label = { Text("Peso") },
                        placeholder = { Text("Kg") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = CardBorder,
                            focusedBorderColor = BrandGreen,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            FormCard {
                Text(text = "Observaciones", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(text = "  (opcional)", color = SubtitleGray, fontSize = 12.sp)
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
            val apiErrorMessage = (createState as? CreateDewormingUiState.Error)?.message
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
            val isLoading = createState is CreateDewormingUiState.Loading
            Button(
                onClick = {
                    val petId = selectedPet?.id
                    if (petId == null) {
                        validationError = "Agrega una mascota primero."
                    } else {
                        validationError = null
                        viewModel.createDesparasitacion(
                            petId = petId,
                            dewormingType = tipo.apiValue,
                            appliedOnIso = date.toString(),
                            administrationRoute = via.apiValue,
                            presentation = presentacion.apiValue,
                            productName = producto,
                            weightKg = peso,
                            notes = observaciones,
                        )
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
                    Text(text = "Guardar desparasitación", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
private fun TypeChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    if (selected) {
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
            modifier = modifier,
        ) {
            Text(text = label, fontSize = 13.sp, maxLines = 1)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = SubtitleGray),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
            modifier = modifier,
        ) {
            Text(text = label, fontSize = 13.sp, maxLines = 1)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> DropdownSelector(
    label: String,
    selected: T,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = optionLabel(selected),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label, fontSize = 12.sp) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = CardBorder,
                focusedBorderColor = BrandGreen,
            ),
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
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
private fun DesparasitacionSuccessContent(
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
            Text(text = "¡Desparasitación registrada!", fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Hemos guardado correctamente la desparasitación aplicada.",
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
