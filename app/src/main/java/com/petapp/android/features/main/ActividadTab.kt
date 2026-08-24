package com.petapp.android.features.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petapp.android.core.model.Pet
import com.petapp.android.features.consultations.ConsultasListUiState
import com.petapp.android.features.consultations.ConsultationsViewModel
import com.petapp.android.features.deworming.DewormingListUiState
import com.petapp.android.features.deworming.DewormingViewModel
import com.petapp.android.features.files.DocumentsListUiState
import com.petapp.android.features.files.FilesViewModel
import com.petapp.android.features.incidents.IncidenciasListUiState
import com.petapp.android.features.incidents.IncidentsViewModel
import com.petapp.android.features.vaccines.VaccinesListUiState
import com.petapp.android.features.vaccines.VaccinesViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

private val BrandGreen = Color(0xFF406E5F)
private val SubtitleGray = Color(0xFF666666)
private val CardBorder = Color(0xFFEFEFF4)
private val ChipUnselectedBg = Color(0xFFF0F0F0)
private val IncidentRed = Color(0xFFC0392B)

@Composable
fun ActividadTab(
    pets: List<Pet>,
    selectedPet: Pet?,
    userFullName: String?,
    onSwitchPetClick: () -> Unit,
    onMoreClick: () -> Unit = {},
    initialFilter: ActivityCategory? = null,
    onFilterConsumed: () -> Unit = {},
    onItemClick: (ActivityCategory, String) -> Unit = { _, _ -> },
    vaccinesViewModel: VaccinesViewModel = viewModel(),
    dewormingViewModel: DewormingViewModel = viewModel(),
    consultationsViewModel: ConsultationsViewModel = viewModel(),
    incidentsViewModel: IncidentsViewModel = viewModel(),
    filesViewModel: FilesViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    val hasPets = pets.isNotEmpty()
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Surface(color = Color.White) {
                GreetingHeader(
                    selectedPet = selectedPet,
                    userFullName = userFullName,
                    hasPets = hasPets,
                    onSwitchPetClick = onSwitchPetClick,
                )
            }
            if (hasPets) {
                ActivityFeed(
                    petId = selectedPet?.id,
                    onMoreClick = onMoreClick,
                    initialFilter = initialFilter,
                    onFilterConsumed = onFilterConsumed,
                    onItemClick = onItemClick,
                    vaccinesViewModel = vaccinesViewModel,
                    dewormingViewModel = dewormingViewModel,
                    consultationsViewModel = consultationsViewModel,
                    incidentsViewModel = incidentsViewModel,
                    filesViewModel = filesViewModel,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, start = 24.dp, end = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Agrega una mascota para ver su actividad.",
                        color = SubtitleGray,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
        if (hasPets) {
            FloatingActionButton(
                onClick = onMoreClick,
                containerColor = BrandGreen,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
            }
        }
    }
}

private enum class ActivityFilter(val label: String, val category: ActivityCategory?) {
    TODOS("Todos", null),
    VACUNAS("Vacunas", ActivityCategory.VACCINE),
    DESPARASITACION("Desparasitación", ActivityCategory.DEWORMING),
    CONSULTAS("Consultas", ActivityCategory.CONSULTA),
    INCIDENCIAS("Incidencias", ActivityCategory.INCIDENCIA),
    DOCUMENTOS("Documentos", ActivityCategory.DOCUMENT),
}

enum class ActivityCategory { VACCINE, DEWORMING, CONSULTA, INCIDENCIA, DOCUMENT }

private data class ActivityItem(
    val id: String,
    val category: ActivityCategory,
    val instant: Instant,
    val icon: ImageVector,
    val iconTint: Color,
    val title: String,
    val subtitle: String,
)

@Composable
private fun ActivityFeed(
    petId: String?,
    onMoreClick: () -> Unit,
    initialFilter: ActivityCategory?,
    onFilterConsumed: () -> Unit,
    onItemClick: (ActivityCategory, String) -> Unit,
    vaccinesViewModel: VaccinesViewModel,
    dewormingViewModel: DewormingViewModel,
    consultationsViewModel: ConsultationsViewModel,
    incidentsViewModel: IncidentsViewModel,
    filesViewModel: FilesViewModel,
) {
    // initialFilter is a one-shot navigation argument (e.g. "Ver todo" from a specific
    // Inicio section): applied once to selectedFilter below, then immediately reported
    // back as consumed so a later plain tap on the Actividad tab doesn't re-apply it.
    LaunchedEffect(initialFilter) {
        if (initialFilter != null) onFilterConsumed()
    }

    LaunchedEffect(petId) {
        if (petId != null) {
            vaccinesViewModel.fetchVacunas(petId)
            dewormingViewModel.fetchDesparasitaciones(petId)
            consultationsViewModel.fetchConsultas(petId)
            incidentsViewModel.fetchIncidencias(petId)
            filesViewModel.fetchDocuments(petId)
        }
    }

    val vacunasState by vaccinesViewModel.listState.collectAsState()
    val dewormingState by dewormingViewModel.listState.collectAsState()
    val consultasState by consultationsViewModel.listState.collectAsState()
    val incidenciasState by incidentsViewModel.listState.collectAsState()
    val documentsState by filesViewModel.listState.collectAsState()

    val isLoading = vacunasState is VaccinesListUiState.Loading ||
        dewormingState is DewormingListUiState.Loading ||
        consultasState is ConsultasListUiState.Loading ||
        incidenciasState is IncidenciasListUiState.Loading ||
        documentsState is DocumentsListUiState.Loading

    val items = remember(vacunasState, dewormingState, consultasState, incidenciasState, documentsState) {
        buildList {
            (vacunasState as? VaccinesListUiState.Loaded)?.doses?.forEach { dose ->
                val instant = isoDateToInstant(dose.appliedOn)
                if (instant != null) {
                    add(
                        ActivityItem(
                            id = dose.id,
                            category = ActivityCategory.VACCINE,
                            instant = instant,
                            icon = Icons.Filled.Vaccines,
                            iconTint = BrandGreen,
                            title = "Vacuna - ${dose.vaccine}",
                            subtitle = "Aplicada el ${spanishShortDate(instant)}",
                        ),
                    )
                }
            }
            (dewormingState as? DewormingListUiState.Loaded)?.applications?.forEach { application ->
                val instant = isoDateToInstant(application.appliedOn)
                if (instant != null) {
                    val title = application.productName?.takeIf { it.isNotBlank() } ?: "Desparasitación"
                    val subtitle = application.nextDueOn?.let { "Próxima dosis recomendada - ${spanishShortDate(isoDateToInstant(it) ?: instant)}" }
                        ?: "Aplicada el ${spanishShortDate(instant)}"
                    add(
                        ActivityItem(
                            id = application.id,
                            category = ActivityCategory.DEWORMING,
                            instant = instant,
                            icon = Icons.Filled.Medication,
                            iconTint = BrandGreen,
                            title = title,
                            subtitle = subtitle,
                        ),
                    )
                }
            }
            (consultasState as? ConsultasListUiState.Loaded)?.consultations?.forEach { consultation ->
                val instant = isoDateToInstant(consultation.consultDate)
                if (instant != null) {
                    add(
                        ActivityItem(
                            id = consultation.id,
                            category = ActivityCategory.CONSULTA,
                            instant = instant,
                            icon = Icons.Filled.MedicalServices,
                            iconTint = BrandGreen,
                            title = consultation.reason,
                            subtitle = consultation.diagnosis?.takeIf { it.isNotBlank() }
                                ?: (consultation.clinicName?.takeIf { it.isNotBlank() } ?: "Consulta veterinaria"),
                        ),
                    )
                }
            }
            (incidenciasState as? IncidenciasListUiState.Loaded)?.events?.forEach { event ->
                val instant = runCatching { Instant.parse(event.eventDate) }.getOrNull()
                if (instant != null) {
                    add(
                        ActivityItem(
                            id = event.id,
                            category = ActivityCategory.INCIDENCIA,
                            instant = instant,
                            icon = Icons.Filled.ReportProblem,
                            iconTint = IncidentRed,
                            title = event.title,
                            subtitle = event.description?.takeIf { it.isNotBlank() } ?: "Incidencia registrada",
                        ),
                    )
                }
            }
            (documentsState as? DocumentsListUiState.Loaded)?.documents?.forEach { document ->
                val instant = document.documentDate?.let { isoDateToInstant(it) }
                    ?: runCatching { Instant.parse(document.createdAt) }.getOrNull()
                if (instant != null) {
                    // A document uploaded as evidence for an incidencia shows that
                    // incidencia's title and icon instead of the generic document ones.
                    val linkedIncidencia = document.eventTitle
                    add(
                        ActivityItem(
                            id = document.id,
                            category = ActivityCategory.DOCUMENT,
                            instant = instant,
                            icon = if (linkedIncidencia != null) Icons.Filled.ReportProblem else Icons.Filled.Description,
                            iconTint = if (linkedIncidencia != null) IncidentRed else BrandGreen,
                            title = linkedIncidencia ?: (document.title?.takeIf { it.isNotBlank() } ?: "Documento"),
                            subtitle = "Subido el ${spanishShortDate(instant)}",
                        ),
                    )
                }
            }
            sortByDescending { it.instant }
        }
    }

    // Captured once per mount (no key): ActivityFeed fully unmounts whenever the user
    // leaves the Actividad tab (MainScaffold's `when` only composes the active tab), so
    // a fresh mount is guaranteed on every "Ver todo" navigation. Keying this on
    // initialFilter would re-seed it when the one-shot consumption below clears
    // initialFilter back to null moments later, undoing the filter before it's visible.
    var selectedFilter by remember {
        mutableStateOf(ActivityFilter.entries.firstOrNull { it.category == initialFilter } ?: ActivityFilter.TODOS)
    }
    val filteredItems = items.filter { selectedFilter.category == null || it.category == selectedFilter.category }

    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Actividad", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = null,
                modifier = Modifier.padding(12.dp),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ActivityFilter.entries.forEach { filter ->
                FilterChip(
                    label = filter.label,
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading && items.isEmpty() -> LoadingRow()
            filteredItems.isEmpty() -> Text(
                text = "No hay actividad registrada.",
                color = SubtitleGray,
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                filteredItems.forEach { item ->
                    ActivityRow(item, onClick = { onItemClick(item.category, item.id) })
                }
            }
        }
        Spacer(modifier = Modifier.height(88.dp))
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) BrandGreen else ChipUnselectedBg,
            contentColor = if (selected) Color.White else SubtitleGray,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(text = label, fontSize = 13.sp, maxLines = 1)
    }
}

@Composable
private fun ActivityRow(item: ActivityItem, onClick: (ActivityItem) -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, CardBorder),
        onClick = { onClick(item) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(item.icon, contentDescription = null, tint = item.iconTint, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = relativeLabel(item.instant), color = SubtitleGray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = item.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = item.subtitle, color = SubtitleGray, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun LoadingRow() {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 12.dp)) {
        CircularProgressIndicator(color = BrandGreen, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = "Cargando…", color = SubtitleGray, fontSize = 13.sp)
    }
}

private fun isoDateToInstant(iso: String): Instant? =
    runCatching { LocalDate.parse(iso).atStartOfDay(ZoneId.systemDefault()).toInstant() }.getOrNull()

private fun spanishShortDate(instant: Instant): String {
    val date = instant.atZone(ZoneId.systemDefault()).toLocalDate()
    val months = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
    return "${date.dayOfMonth} ${months[date.monthValue - 1]} ${date.year}"
}

private fun relativeLabel(instant: Instant): String {
    val date = instant.atZone(ZoneId.systemDefault()).toLocalDate()
    val today = LocalDate.now()
    val days = ChronoUnit.DAYS.between(date, today)
    return when {
        days <= 0 -> "Hoy"
        days == 1L -> "Hace 1 día"
        days < 30 -> "Hace $days días"
        days < 365 -> {
            val months = days / 30
            if (months <= 1) "Hace 1 mes" else "Hace $months meses"
        }
        else -> {
            val years = days / 365
            if (years <= 1) "Hace 1 año" else "Hace $years años"
        }
    }
}
