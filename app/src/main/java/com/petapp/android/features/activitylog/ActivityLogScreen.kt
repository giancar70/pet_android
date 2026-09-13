package com.petapp.android.features.activitylog

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petapp.android.core.model.PetActivityLog
import com.petapp.android.core.util.relativeDateTimeLabel

private val BrandGreen = Color(0xFF406E5F)
private val SubtitleGray = Color(0xFF666666)
private val ContentBackground = Color.White
private val CardBorder = Color(0xFFEFEFF4)
private val IconBg = Color(0xFFD9FEF2)

@Composable
fun ActivityLogScreen(
    petId: String,
    onBack: () -> Unit,
    viewModel: ActivityLogViewModel = viewModel(),
) {
    val listState by viewModel.listState.collectAsState()

    LaunchedEffect(petId) { viewModel.fetchActivityLog(petId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ContentBackground)
            .verticalScroll(rememberScrollState()),
    ) {
        BackHandler(onBack = onBack)
        IconButton(onClick = onBack, modifier = Modifier.padding(start = 12.dp, top = 12.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "Historial de actividad", color = BrandGreen, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Quién agregó, modificó o eliminó información de esta mascota.",
                color = SubtitleGray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(20.dp))

            when (val state = listState) {
                is ActivityLogUiState.Loading -> Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BrandGreen)
                }
                is ActivityLogUiState.Error -> Text(text = state.message, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                is ActivityLogUiState.Loaded -> {
                    if (state.entries.isEmpty()) {
                        Text(
                            text = "Todavía no hay actividad registrada.",
                            color = SubtitleGray,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 24.dp),
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                            state.entries.forEach { entry -> ActivityLogRow(entry) }
                        }
                    }
                }
            }
        }
    }
}

private fun iconFor(resourceType: String): ImageVector = when (resourceType) {
    "vaccine_doses" -> Icons.Filled.Vaccines
    "deworming_applications" -> Icons.Filled.Medication
    "consultations" -> Icons.Filled.MedicalServices
    "events" -> Icons.Filled.ReportProblem
    "documents" -> Icons.Filled.Description
    "reminders" -> Icons.Filled.CalendarMonth
    else -> Icons.Filled.Pets
}

@Composable
private fun ActivityLogRow(entry: PetActivityLog) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).background(IconBg, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(iconFor(entry.resourceType), contentDescription = null, tint = BrandGreen, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = entry.description, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(
                    text = "${entry.userFullName ?: "Usuario eliminado"} · ${relativeDateTimeLabel(entry.createdAt)}",
                    color = SubtitleGray,
                    fontSize = 12.sp,
                )
            }
        }
    }
}
