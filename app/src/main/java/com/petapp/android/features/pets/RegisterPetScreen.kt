package com.petapp.android.features.pets

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petapp.android.core.model.PetSpecies
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val BrandGreen = Color(0xFF406E5F)
private val SubtitleGray = Color(0xFF666666)
private val PhotoCircleBg = Color(0xFFD9FEF2)
private val ChipUnselectedText = Color(0xFFA3A3A3)
private val ProgressTrack = Color(0xFFE5E5E5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterPetScreen(
    onDone: () -> Unit,
    onSkip: () -> Unit,
    viewModel: PetsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val createState by viewModel.createState.collectAsState()

    var name by remember { mutableStateOf("") }
    var selectedSpecies by remember { mutableStateOf<PetSpecies?>(PetSpecies.DOG) }
    var birthDateIso by remember { mutableStateOf<String?>(null) }
    var birthDateDisplay by remember { mutableStateOf("") }
    var imageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var imagePreview by remember { mutableStateOf<ImageBitmap?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    // PetsViewModel is shared (Activity-scoped, no Navigation-Compose back stack), so a
    // prior successful creation would otherwise still be sitting in createState and bounce
    // this screen straight back out via the LaunchedEffect below.
    LaunchedEffect(Unit) {
        viewModel.resetCreateState()
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null) {
                imageBytes = bytes
                imagePreview = BitmapFactory.decodeByteArray(bytes, 0, bytes.size).asImageBitmap()
            }
        }
    }

    LaunchedEffect(createState) {
        if (createState is CreatePetUiState.Success) onDone()
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis <= System.currentTimeMillis()
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        birthDateIso = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                        birthDateDisplay = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
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
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onSkip) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
            LinearProgressIndicator(
                progress = { 0.66f },
                color = BrandGreen,
                trackColor = ProgressTrack,
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Saltar",
                color = SubtitleGray,
                fontSize = 16.sp,
                modifier = Modifier.clickable(onClick = onSkip),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Cuéntanos sobre tu mascota",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Solo tomará unos segundos",
            fontSize = 13.sp,
            color = SubtitleGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape)
                    .background(PhotoCircleBg)
                    .dashedBorder(color = BrandGreen)
                    .clickable { imagePicker.launch("image/*") },
                contentAlignment = Alignment.Center,
            ) {
                if (imagePreview != null) {
                    Image(
                        bitmap = imagePreview!!,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.PhotoCamera,
                            contentDescription = null,
                            tint = BrandGreen,
                            modifier = Modifier.size(32.dp),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Añadir foto", color = BrandGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(text = "(opcional)", color = SubtitleGray, fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(text = "Tipo de mascota", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SpeciesChip(
                label = PetSpecies.DOG.label,
                selected = selectedSpecies == PetSpecies.DOG,
                onClick = { selectedSpecies = PetSpecies.DOG },
            )
            SpeciesChip(
                label = PetSpecies.CAT.label,
                selected = selectedSpecies == PetSpecies.CAT,
                onClick = { selectedSpecies = PetSpecies.CAT },
            )
            SpeciesChip(
                label = PetSpecies.OTHER.label,
                selected = selectedSpecies == PetSpecies.OTHER,
                onClick = { selectedSpecies = PetSpecies.OTHER },
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "Nombre de mascota", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = { Text("Luna", color = ChipUnselectedText) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = BrandGreen,
                focusedBorderColor = BrandGreen,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(text = "Fecha de nacimiento", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(modifier = Modifier.height(8.dp))
        Box {
            OutlinedTextField(
                value = birthDateDisplay,
                onValueChange = {},
                placeholder = { Text("DD/MM/AAAA", color = ChipUnselectedText) },
                readOnly = true,
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = BrandGreen,
                    focusedBorderColor = BrandGreen,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            // OutlinedTextField consumes taps itself even when readOnly, so an
            // invisible box on top is needed to intercept the tap and open the dialog.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { showDatePicker = true },
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        if (validationError != null) {
            Text(
                text = validationError!!,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        if (createState is CreatePetUiState.Error) {
            Text(
                text = (createState as CreatePetUiState.Error).message,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        val isLoading = createState is CreatePetUiState.Loading
        Button(
            onClick = {
                val species = selectedSpecies
                validationError = when {
                    name.isBlank() -> "Ingresa el nombre de tu mascota."
                    species == null -> "Selecciona el tipo de mascota."
                    birthDateIso != null && LocalDate.parse(birthDateIso).isAfter(LocalDate.now()) ->
                        "La fecha de nacimiento no puede ser una fecha futura."
                    else -> null
                }
                if (validationError == null && species != null) {
                    viewModel.createPet(name, species, birthDateIso, imageBytes)
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
                Text(text = "Crear mascota", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SpeciesChip(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
        ) {
            Text(text = label, fontWeight = FontWeight.Bold)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ChipUnselectedText),
        ) {
            Text(text = label)
        }
    }
}

private fun Modifier.dashedBorder(color: Color, strokeWidth: Dp = 1.5.dp): Modifier =
    this.drawWithContent {
        drawContent()
        drawCircle(
            color = color,
            style = Stroke(
                width = strokeWidth.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f),
            ),
        )
    }

@Preview(showBackground = true)
@Composable
private fun RegisterPetScreenPreview() {
    RegisterPetScreen(onDone = {}, onSkip = {})
}
