package com.petapp.android.features.pets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petapp.android.core.model.Pet
import com.petapp.android.core.model.PetSex
import com.petapp.android.core.model.PetSpecies
import com.petapp.android.core.model.UpdatePetRequest
import com.petapp.android.core.model.petSexLabel
import com.petapp.android.core.model.petSpeciesLabel
import com.petapp.android.features.main.GreetingHeader
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val BrandGreen = Color(0xFF406E5F)
private val SubtitleGray = Color(0xFF666666)
private val CardBorder = Color(0xFFEFEFF4)
private val PendingBg = Color(0xFFFBE3D6)
private val PendingText = Color(0xFFB4552B)

private enum class DetailField { NAME, SPECIES, BREED, SEX, WEIGHT, COLOR, MICROCHIP, NOTES }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetDetailScreen(
    pet: Pet,
    onBack: () -> Unit,
    viewModel: PetsViewModel = viewModel(),
) {
    val updateState by viewModel.updateState.collectAsState()

    var name by remember(pet.id) { mutableStateOf(pet.name) }
    var species by remember(pet.id) { mutableStateOf(pet.species) }
    var breed by remember(pet.id) { mutableStateOf(pet.breed.orEmpty()) }
    var sex by remember(pet.id) { mutableStateOf(pet.sex) }
    var birthDateIso by remember(pet.id) { mutableStateOf(pet.birthDate) }
    var weightKg by remember(pet.id) { mutableStateOf(pet.weightKg.orEmpty()) }
    var color by remember(pet.id) { mutableStateOf(pet.color.orEmpty()) }
    var microchip by remember(pet.id) { mutableStateOf(pet.microchip.orEmpty()) }
    var notes by remember(pet.id) { mutableStateOf(pet.notes.orEmpty()) }

    var activeDialog by remember { mutableStateOf<DetailField?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.resetUpdateState() }
    LaunchedEffect(updateState) {
        if (updateState is UpdatePetUiState.Success) onBack()
    }

    val displayPet = pet.copy(name = name, species = species, breed = breed.ifBlank { null }, birthDate = birthDateIso)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.padding(start = 12.dp, top = 12.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
        }

        GreetingHeader(
            selectedPet = displayPet,
            userFullName = null,
            hasPets = true,
            onSwitchPetClick = onBack,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        ) {
            DetailCard {
                DetailRow(label = "Nombre", value = name) { activeDialog = DetailField.NAME }
                HorizontalDivider(color = CardBorder)
                DetailRow(label = "Especie", value = petSpeciesLabel(species)) { activeDialog = DetailField.SPECIES }
                HorizontalDivider(color = CardBorder)
                DetailRow(label = "Raza", value = breed.ifBlank { "Agregar" }) { activeDialog = DetailField.BREED }
            }

            Spacer(modifier = Modifier.height(16.dp))

            DetailCard {
                DetailRow(label = "Sexo", value = petSexLabel(sex)) { activeDialog = DetailField.SEX }
                HorizontalDivider(color = CardBorder)
                DetailRow(
                    label = "Fecha de Nacimiento",
                    value = birthDateIso?.let(::formatDisplayDate) ?: "Agregar",
                ) { showDatePicker = true }
                HorizontalDivider(color = CardBorder)
                DetailRow(
                    label = "Peso (Kg)",
                    value = weightKg.ifBlank { "Agregar" }.let { if (it == "Agregar") it else "$it Kg" },
                ) { activeDialog = DetailField.WEIGHT }
            }

            Spacer(modifier = Modifier.height(16.dp))

            DetailCard {
                DetailRow(label = "Color", value = color.ifBlank { "Agregar" }) { activeDialog = DetailField.COLOR }
                HorizontalDivider(color = CardBorder)
                MicrochipRow(microchip = microchip) { activeDialog = DetailField.MICROCHIP }
            }

            Spacer(modifier = Modifier.height(16.dp))

            DetailCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { activeDialog = DetailField.NOTES }
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.NoteAlt, contentDescription = null, tint = BrandGreen)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "Notas", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = SubtitleGray)
                }
                Text(
                    text = notes.ifBlank { "Información adicional, información relevante, temperamento, etc." },
                    color = SubtitleGray,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Filled.Shield, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Tu información y la de $name están protegidas. Solo las personas autorizadas pueden acceder.",
                    color = SubtitleGray,
                    fontSize = 11.sp,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (updateState is UpdatePetUiState.Error) {
                Text(
                    text = (updateState as UpdatePetUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            val isSaving = updateState is UpdatePetUiState.Loading
            Button(
                onClick = {
                    viewModel.updatePet(
                        pet.id,
                        UpdatePetRequest(
                            name = name,
                            species = species,
                            breed = breed.ifBlank { null },
                            sex = sex,
                            birthDate = birthDateIso,
                            weightKg = weightKg.ifBlank { null },
                            color = color.ifBlank { null },
                            microchip = microchip.ifBlank { null },
                            notes = notes.ifBlank { null },
                        ),
                    )
                },
                enabled = !isSaving,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text(text = if (isSaving) "Guardando…" else "Guardar", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    when (activeDialog) {
        DetailField.NAME -> EditTextDialog(
            title = "Nombre",
            initialValue = name,
            onConfirm = { name = it },
            onDismiss = { activeDialog = null },
        )
        DetailField.SPECIES -> SelectionDialog(
            title = "Especie",
            options = PetSpecies.entries.map { it.apiValue to it.label.replaceFirstChar(Char::uppercase) },
            selected = species,
            onSelect = { species = it },
            onDismiss = { activeDialog = null },
        )
        DetailField.BREED -> EditTextDialog(
            title = "Raza",
            initialValue = breed,
            onConfirm = { breed = it },
            onDismiss = { activeDialog = null },
        )
        DetailField.SEX -> SelectionDialog(
            title = "Sexo",
            options = PetSex.entries.map { it.apiValue to it.label },
            selected = sex,
            onSelect = { sex = it },
            onDismiss = { activeDialog = null },
        )
        DetailField.WEIGHT -> EditTextDialog(
            title = "Peso (Kg)",
            initialValue = weightKg,
            keyboardType = KeyboardType.Decimal,
            onConfirm = { weightKg = it },
            onDismiss = { activeDialog = null },
        )
        DetailField.COLOR -> EditTextDialog(
            title = "Color",
            initialValue = color,
            onConfirm = { color = it },
            onDismiss = { activeDialog = null },
        )
        DetailField.MICROCHIP -> EditTextDialog(
            title = "Microchip",
            initialValue = microchip,
            onConfirm = { microchip = it },
            onDismiss = { activeDialog = null },
        )
        DetailField.NOTES -> EditTextDialog(
            title = "Notas",
            initialValue = notes,
            singleLine = false,
            onConfirm = { notes = it },
            onDismiss = { activeDialog = null },
        )
        null -> Unit
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        birthDateIso = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            .format(DateTimeFormatter.ISO_LOCAL_DATE)
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
}

@Composable
private fun DetailCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp), content = content)
    }
}

@Composable
private fun DetailRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, color = SubtitleGray, fontSize = 14.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = value, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = SubtitleGray, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun MicrochipRow(microchip: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "Microchip", color = SubtitleGray, fontSize = 15.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (microchip.isBlank()) {
                Surface(shape = RoundedCornerShape(12.dp), color = PendingBg) {
                    Text(
                        text = "Microchip pendiente",
                        color = PendingText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            } else {
                Text(text = microchip, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = SubtitleGray, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun EditTextDialog(
    title: String,
    initialValue: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = singleLine,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text); onDismiss() }) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

@Composable
private fun SelectionDialog(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(value); onDismiss() }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = value == selected, onClick = { onSelect(value); onDismiss() })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        },
    )
}

private fun formatDisplayDate(iso: String): String = try {
    java.time.LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
} catch (e: Exception) {
    iso
}
