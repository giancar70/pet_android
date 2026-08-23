package com.petapp.android.features.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petapp.android.core.model.Consultation
import com.petapp.android.core.model.DewormingApplication
import com.petapp.android.core.model.PetEvent
import com.petapp.android.core.model.Reminder
import com.petapp.android.core.model.ReminderFrequency
import com.petapp.android.core.model.VaccineDose
import com.petapp.android.features.consultations.ConsultasListUiState
import com.petapp.android.features.consultations.ConsultationsViewModel
import com.petapp.android.features.deworming.DewormingListUiState
import com.petapp.android.features.deworming.DewormingViewModel
import com.petapp.android.features.incidents.IncidenciasListUiState
import com.petapp.android.features.incidents.IncidentsViewModel
import com.petapp.android.features.incidents.spanishDate
import com.petapp.android.features.reminders.RecordatoriosListUiState
import com.petapp.android.features.reminders.RemindersViewModel
import com.petapp.android.features.vaccines.VaccinesListUiState
import com.petapp.android.features.vaccines.VaccinesViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private val BrandGreen = Color(0xFF406E5F)
private val SubtitleGray = Color(0xFF666666)
private val CardBorder = Color(0xFFEFEFF4)
private val PlaceholderIconBg = Color(0xFFE3E3E3)
private val ButtonBackground = Color(0xFFF3F3F3)
private val RecordIconBg = Color(0xFFD9FEF2)

@Composable
fun PetActivityContent(
    petId: String?,
    modifier: Modifier = Modifier,
    vaccinesViewModel: VaccinesViewModel = viewModel(),
    dewormingViewModel: DewormingViewModel = viewModel(),
    consultationsViewModel: ConsultationsViewModel = viewModel(),
    incidentsViewModel: IncidentsViewModel = viewModel(),
    remindersViewModel: RemindersViewModel = viewModel(),
) {
    LaunchedEffect(petId) {
        if (petId != null) {
            vaccinesViewModel.fetchVacunas(petId)
            dewormingViewModel.fetchDesparasitaciones(petId)
            consultationsViewModel.fetchConsultas(petId)
            incidentsViewModel.fetchIncidencias(petId)
            remindersViewModel.fetchRecordatorios(petId)
        }
    }

    val vacunasState by vaccinesViewModel.listState.collectAsState()
    val dewormingState by dewormingViewModel.listState.collectAsState()
    val consultasState by consultationsViewModel.listState.collectAsState()
    val incidenciasState by incidentsViewModel.listState.collectAsState()
    val recordatoriosState by remindersViewModel.listState.collectAsState()

    Column(modifier = modifier.padding(horizontal = 24.dp)) {
        Text(text = "Actividad", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(20.dp))

        SectionHeader(icon = Icons.Filled.Vaccines, title = "Vacunas")
        Spacer(modifier = Modifier.height(10.dp))
        VaccinesSection(vacunasState)

        Spacer(modifier = Modifier.height(28.dp))

        SectionHeader(icon = Icons.Filled.Medication, title = "Desparasitación")
        Spacer(modifier = Modifier.height(10.dp))
        DewormingSection(dewormingState)

        Spacer(modifier = Modifier.height(28.dp))

        SectionHeader(icon = Icons.Filled.MedicalServices, title = "Consultas")
        Spacer(modifier = Modifier.height(10.dp))
        ConsultasSection(consultasState)

        Spacer(modifier = Modifier.height(28.dp))

        SectionHeader(icon = Icons.Filled.ReportProblem, title = "Incidencias")
        Spacer(modifier = Modifier.height(10.dp))
        IncidenciasSection(incidenciasState)

        Spacer(modifier = Modifier.height(28.dp))

        SectionHeader(icon = Icons.Filled.CalendarMonth, title = "Recordatorios")
        Spacer(modifier = Modifier.height(10.dp))
        RecordatoriosSection(recordatoriosState)

        Spacer(modifier = Modifier.height(28.dp))

        SectionHeader(icon = Icons.Filled.Description, title = "Documentos")
        Spacer(modifier = Modifier.height(10.dp))
        EmptyStateCard(
            icon = Icons.Filled.Description,
            title = "Aún no hay documentos",
            description = "Guarda análisis, estudios, cartillas o cualquier documento importante.",
            primaryLabel = "Capturar documento",
            primarySublabel = "Escanear, subir PDF o foto",
            secondaryLabel = null,
            secondarySublabel = null,
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun VaccinesSection(state: VaccinesListUiState) {
    when (state) {
        is VaccinesListUiState.Loading -> LoadingRow()
        is VaccinesListUiState.Error -> ErrorRow(state.message)
        is VaccinesListUiState.Loaded -> {
            if (state.doses.isEmpty()) {
                EmptyStateCard(
                    icon = Icons.Filled.Vaccines,
                    title = "No hay vacunas registradas",
                    description = "Sube una cartilla o registra la última vacuna aplicada.",
                    primaryLabel = "Capturar documento",
                    primarySublabel = "Escanear, subir PDF o foto",
                    secondaryLabel = "Añadir manualmente",
                    secondarySublabel = "Registrar vacuna",
                )
            } else {
                RecordList {
                    state.doses.forEach { dose -> VaccineRow(dose) }
                }
            }
        }
    }
}

@Composable
private fun VaccineRow(dose: VaccineDose) {
    RecordRow(icon = Icons.Filled.Vaccines, title = dose.vaccine, subtitle = formatIsoDate(dose.appliedOn))
}

@Composable
private fun DewormingSection(state: DewormingListUiState) {
    when (state) {
        is DewormingListUiState.Loading -> LoadingRow()
        is DewormingListUiState.Error -> ErrorRow(state.message)
        is DewormingListUiState.Loaded -> {
            if (state.applications.isEmpty()) {
                EmptyStateCard(
                    icon = Icons.Filled.Medication,
                    title = "Sin registros de desparasitación",
                    description = "Añade una desparasitación o sube una foto del producto.",
                    primaryLabel = null,
                    primarySublabel = null,
                    secondaryLabel = "Añadir manualmente",
                    secondarySublabel = "Registrar desparasitación",

                )
            } else {
                RecordList {
                    state.applications.forEach { application -> DewormingRow(application) }
                }
            }
        }
    }
}

@Composable
private fun DewormingRow(application: DewormingApplication) {
    val title = application.productName?.takeIf { it.isNotBlank() } ?: "Desparasitación"
    val subtitle = formatIsoDate(application.appliedOn)
    RecordRow(icon = Icons.Filled.Medication, title = title, subtitle = subtitle)
}

@Composable
private fun ConsultasSection(state: ConsultasListUiState) {
    when (state) {
        is ConsultasListUiState.Loading -> LoadingRow()
        is ConsultasListUiState.Error -> ErrorRow(state.message)
        is ConsultasListUiState.Loaded -> {
            if (state.consultations.isEmpty()) {
                EmptyStateCard(
                    icon = Icons.Filled.MedicalServices,
                    title = "No hay consultas registradas",
                    description = "Registra el motivo, diagnóstico y tratamiento de la última visita al veterinario.",
                    primaryLabel = null,
                    primarySublabel = null,
                    secondaryLabel = "Añadir manualmente",
                    secondarySublabel = "Registrar consulta",
                )
            } else {
                RecordList {
                    state.consultations.forEach { consultation -> ConsultaRow(consultation) }
                }
            }
        }
    }
}

@Composable
private fun ConsultaRow(consultation: Consultation) {
    val subtitle = buildString {
        append(formatIsoDate(consultation.consultDate))
        consultation.diagnosis?.takeIf { it.isNotBlank() }?.let { append(" · $it") }
    }
    RecordRow(icon = Icons.Filled.MedicalServices, title = consultation.reason, subtitle = subtitle)
}

@Composable
private fun IncidenciasSection(state: IncidenciasListUiState) {
    when (state) {
        is IncidenciasListUiState.Loading -> LoadingRow()
        is IncidenciasListUiState.Error -> ErrorRow(state.message)
        is IncidenciasListUiState.Loaded -> {
            if (state.events.isEmpty()) {
                EmptyStateCard(
                    icon = Icons.Filled.ReportProblem,
                    title = "No hay incidencias registradas",
                    description = "Registra algo que le haya pasado a tu mascota para llevar un mejor historial.",
                    primaryLabel = null,
                    primarySublabel = null,
                    secondaryLabel = "Añadir manualmente",
                    secondarySublabel =  "Registrar incidencia",
                )
            } else {
                RecordList {
                    state.events.forEach { event -> IncidenciaRow(event) }
                }
            }
        }
    }
}

@Composable
private fun IncidenciaRow(event: PetEvent) {
    RecordRow(icon = Icons.Filled.ReportProblem, title = event.title, subtitle = formatIsoDateTime(event.eventDate))
}

@Composable
private fun RecordatoriosSection(state: RecordatoriosListUiState) {
    when (state) {
        is RecordatoriosListUiState.Loading -> LoadingRow()
        is RecordatoriosListUiState.Error -> ErrorRow(state.message)
        is RecordatoriosListUiState.Loaded -> {
            if (state.reminders.isEmpty()) {
                EmptyStateCard(
                    icon = Icons.Filled.CalendarMonth,
                    title = "No hay recordatorios",
                    description = "Añade un recordatorio para no olvidar vacunas, desparasitaciones o controles.",
                    primaryLabel = null,
                    primarySublabel = null,
                    secondaryLabel = "Añadir manualmente",
                    secondarySublabel = "Añadir recordatorio",
                )
            } else {
                RecordList {
                    state.reminders.forEach { reminder -> RecordatorioRow(reminder) }
                }
            }
        }
    }
}

@Composable
private fun RecordatorioRow(reminder: Reminder) {
    val frequencyLabel = ReminderFrequency.entries.find { it.apiValue == reminder.frequency }?.label ?: reminder.frequency
    val subtitle = "${formatIsoDate(reminder.dueDate)} · $frequencyLabel"
    RecordRow(icon = Icons.Filled.CalendarMonth, title = reminder.title, subtitle = subtitle)
}

@Composable
private fun RecordList(content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        content()
    }
}

@Composable
private fun RecordRow(icon: ImageVector, title: String, subtitle: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(RecordIconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(text = subtitle, color = SubtitleGray, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun LoadingRow() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(color = BrandGreen, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = "Cargando…", color = SubtitleGray, fontSize = 13.sp)
    }
}

@Composable
private fun ErrorRow(message: String) {
    Text(text = message, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
}

private fun formatIsoDate(iso: String): String = try {
    spanishDate(LocalDate.parse(iso))
} catch (e: Exception) {
    iso
}

private fun formatIsoDateTime(iso: String): String = try {
    val localDate = Instant.parse(iso).atZone(ZoneId.systemDefault()).toLocalDate()
    spanishDate(localDate)
} catch (e: Exception) {
    iso
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(BrandGreen),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    description: String,
    primaryLabel: String?,
    primarySublabel: String?,
    secondaryLabel: String?,
    secondarySublabel: String?,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(PlaceholderIconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = description, color = SubtitleGray, fontSize = 12.sp, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (primaryLabel != null && primarySublabel != null) {
                    ActionChip(
                        icon = Icons.Filled.AddAPhoto,
                        label = primaryLabel,
                        sublabel = primarySublabel,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (secondaryLabel != null && secondarySublabel != null) {
                    ActionChip(
                        icon = Icons.Filled.Edit,
                        label = secondaryLabel,
                        sublabel = secondarySublabel,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionChip(icon: ImageVector, label: String, sublabel: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ButtonBackground,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Icon(icon, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = label, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Text(text = sublabel, color = SubtitleGray, fontSize = 10.sp)
        }
    }
}
