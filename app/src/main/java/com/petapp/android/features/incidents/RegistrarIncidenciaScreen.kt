package com.petapp.android.features.incidents

import android.graphics.BitmapFactory
import android.provider.OpenableColumns
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
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PhotoCamera
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
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petapp.android.core.model.Pet
import com.petapp.android.features.main.GreetingHeader
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

private val BrandGreen = Color(0xFF406E5F)
private val SubtitleGray = Color(0xFF666666)
private val InfoBannerBg = Color(0xFFE3FBF1)
private val EvidenceBoxBg = Color(0xFFF6F7F8)
private val CardBorder = Color(0xFFEFEFF4)
private val SuccessCardBorder = Color(0xFF3B82F6)

private data class SavedIncidencia(
    val title: String,
    val dateDisplay: String,
    val timeDisplay: String,
    val imagePreview: ImageBitmap?,
)

private sealed interface IncidenciaStep {
    data object Form : IncidenciaStep
    data class Success(val incidencia: SavedIncidencia) : IncidenciaStep
}

@Composable
fun RegistrarIncidenciaScreen(
    selectedPet: Pet?,
    userFullName: String?,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    onViewActivity: () -> Unit,
    viewModel: IncidentsViewModel = viewModel(),
) {
    var step by remember { mutableStateOf<IncidenciaStep>(IncidenciaStep.Form) }

    when (val current = step) {
        is IncidenciaStep.Form -> IncidenciaFormContent(
            selectedPet = selectedPet,
            userFullName = userFullName,
            onBack = onBack,
            onSaved = { step = IncidenciaStep.Success(it) },
            viewModel = viewModel,
        )
        is IncidenciaStep.Success -> IncidenciaSuccessContent(
            selectedPet = selectedPet,
            userFullName = userFullName,
            incidencia = current.incidencia,
            onViewActivity = onViewActivity,
            onRegisterAnother = { step = IncidenciaStep.Form },
            onFinish = onFinish,
            onDelete = { step = IncidenciaStep.Form },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IncidenciaFormContent(
    selectedPet: Pet?,
    userFullName: String?,
    onBack: () -> Unit,
    onSaved: (SavedIncidencia) -> Unit,
    viewModel: IncidentsViewModel,
) {
    val context = LocalContext.current
    val createState by viewModel.createState.collectAsState()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var time by remember { mutableStateOf(LocalTime.now()) }
    var imageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var imagePreview by remember { mutableStateOf<ImageBitmap?>(null) }
    var documentEvidence by remember { mutableStateOf<EvidenceFile?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    // IncidentsViewModel is Activity-scoped, so a prior success can still be sitting
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
        val state = createState
        if (state is CreateEventUiState.Success) {
            onSaved(
                SavedIncidencia(
                    title = state.event.title,
                    dateDisplay = spanishDate(date),
                    timeDisplay = "%02d:%02d".format(time.hour, time.minute),
                    imagePreview = imagePreview,
                ),
            )
        }
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
    val documentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null) {
                var name = "documento"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx >= 0 && cursor.moveToFirst()) {
                        cursor.getString(nameIdx)?.let { name = it }
                    }
                }
                val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                documentEvidence = EvidenceFile(bytes, name, mimeType)
            }
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
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = InfoBannerBg,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        val petName = selectedPet?.name ?: "tu mascota"
                        Text(
                            text = "Registra algo que le pasó a $petName.",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                        Text(
                            text = "Esto te ayudará a llevar un mejor historial.",
                            color = SubtitleGray,
                            fontSize = 13.sp,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "¿Qué pasó?", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Título*", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 80) title = it },
                placeholder = { Text("Ej: Vómito después de comer") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = CardBorder,
                    focusedBorderColor = BrandGreen,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "${title.length}/80",
                color = SubtitleGray,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Descripción", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { if (it.length <= 500) description = it },
                placeholder = { Text("Describe lo que observaste") },
                minLines = 4,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = CardBorder,
                    focusedBorderColor = BrandGreen,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "${description.length}/500",
                color = SubtitleGray,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Fecha*", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    val isToday = date == LocalDate.now()
                    val dateLabel = if (isToday) "Hoy, ${spanishDate(date)}" else spanishDate(date)
                    DropdownField(
                        text = dateLabel,
                        onClick = { showDatePicker = true },
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Hora*", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    DropdownField(
                        text = "%02d:%02d horas".format(time.hour, time.minute),
                        leadingIcon = Icons.Filled.AccessTime,
                        onClick = { showTimePicker = true },
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(text = "Añadir evidencia", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = "(opcional)", color = SubtitleGray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                EvidenceBox(
                    icon = if (imagePreview != null) Icons.Filled.Check else Icons.Filled.PhotoCamera,
                    label = if (imagePreview != null) "Foto añadida" else "Añadir foto",
                    preview = imagePreview,
                    onClick = { imagePicker.launch("image/*") },
                    modifier = Modifier.weight(1f),
                )
                EvidenceBox(
                    icon = if (documentEvidence != null) Icons.Filled.Check else Icons.Filled.Description,
                    label = if (documentEvidence != null) "Documento añadido" else "Añadir documento",
                    preview = null,
                    onClick = { documentPicker.launch("*/*") },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            val apiErrorMessage = (createState as? CreateEventUiState.Error)?.message
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

            val isLoading = createState is CreateEventUiState.Loading
            Button(
                onClick = {
                    val petId = selectedPet?.id
                    when {
                        title.isBlank() -> validationError = "Ingresa un título para la incidencia."
                        petId == null -> validationError = "Agrega una mascota primero."
                        else -> {
                            validationError = null
                            val eventDateIso = LocalDateTime.of(date, time)
                                .atZone(ZoneId.systemDefault())
                                .toInstant()
                                .toString()
                            val photoEvidence = imageBytes?.let { EvidenceFile(it, "incidencia.jpg", "image/jpeg") }
                            viewModel.createIncidencia(
                                petId = petId,
                                title = title,
                                description = description,
                                eventDateIso = eventDateIso,
                                photo = photoEvidence,
                                document = documentEvidence,
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
                    Text(text = "Guardar incidencia", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DropdownField(
    text: String,
    onClick: () -> Unit,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    Box {
        OutlinedTextField(
            value = text,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null, tint = BrandGreen) } },
            trailingIcon = { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null) },
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
                .clickable(onClick = onClick),
        )
    }
}

@Composable
private fun EvidenceBox(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    preview: ImageBitmap?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(EvidenceBoxBg)
            .clickable(onClick = onClick)
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (preview != null) {
            Image(
                bitmap = preview,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
        } else {
            Icon(icon, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, color = BrandGreen, fontSize = 13.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
    }
}

@Composable
private fun IncidenciaSuccessContent(
    selectedPet: Pet?,
    userFullName: String?,
    incidencia: SavedIncidencia,
    onViewActivity: () -> Unit,
    onRegisterAnother: () -> Unit,
    onFinish: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState()),
    ) {
        GreetingHeader(
            selectedPet = selectedPet,
            userFullName = userFullName,
            hasPets = selectedPet != null,
            onSwitchPetClick = {},
        )

        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SuccessCheckmark()
            Spacer(modifier = Modifier.height(20.dp))
            Text(text = "¡Incidencia guardada!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            val petName = selectedPet?.name ?: "tu mascota"
            Text(
                text = "La incidencia se ha registrado en el historial de $petName.",
                color = SubtitleGray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(24.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(2.dp, SuccessCardBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(text = "Incidencia registrada", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text(text = "Ahora", color = SubtitleGray, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = incidencia.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                text = "${incidencia.dateDisplay} - ${incidencia.timeDisplay}",
                                color = SubtitleGray,
                                fontSize = 13.sp,
                            )
                        }
                        val preview = incidencia.imagePreview
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFE0E0E0)),
                        ) {
                            if (preview != null) {
                                Image(
                                    bitmap = preview,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = null, tint = Color(0xFFE53935))
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
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
                onClick = onRegisterAnother,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandGreen),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text(text = "Registrar otra incidencia", fontWeight = FontWeight.Bold)
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

@Composable
internal fun SuccessCheckmark() {
    Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
        Icon(
            Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = Color(0xFFF5A623),
            modifier = Modifier.size(18.dp).align(Alignment.TopStart),
        )
        Icon(
            Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = Color(0xFF3B82F6),
            modifier = Modifier.size(16.dp).align(Alignment.TopEnd),
        )
        Icon(
            Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = BrandGreen,
            modifier = Modifier.size(14.dp).align(Alignment.BottomStart),
        )
        Icon(
            Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = Color(0xFFF5A623),
            modifier = Modifier.size(16.dp).align(Alignment.BottomEnd),
        )
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(BrandGreen),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimePickerDialogSheet(
    initialTime: LocalTime,
    onDismissRequest: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true,
    )
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color.White) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TimePicker(state = state)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismissRequest) { Text("Cancelar") }
                    TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) { Text("Aceptar") }
                }
            }
        }
    }
}

private val spanishMonths = listOf(
    "ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic",
)

internal fun spanishDate(date: LocalDate): String =
    "${date.dayOfMonth} ${spanishMonths[date.monthValue - 1]} ${date.year}"
