package com.petapp.android.features.consultations

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.petapp.android.core.model.Consultation
import com.petapp.android.core.model.Pet
import com.petapp.android.core.util.relativeDateLabel
import com.petapp.android.features.main.GreetingHeader

private val BrandGreen = Color(0xFF406E5F)
private val SubtitleGray = Color(0xFF666666)
private val TextDark = Color(0xFF333333)
private val CardBorder = Color(0xFFEFEFF4)
private val DeleteRed = Color(0xFFC0392B)

@Composable
fun ConsultaDetailScreen(
    selectedPet: Pet?,
    userFullName: String?,
    consultationId: String,
    onBack: () -> Unit,
    viewModel: ConsultationsViewModel = viewModel(),
) {
    val detailState by viewModel.detailState.collectAsState()
    val deleteState by viewModel.deleteState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(selectedPet?.id, consultationId) {
        selectedPet?.id?.let { viewModel.fetchConsultaDetail(it, consultationId) }
    }

    // ConsultationsViewModel is Activity-scoped, so a prior deletion's Success can still
    // be sitting in deleteState when this screen re-enters for a different consulta;
    // ignore the first firing regardless of what it holds, and only act on a later,
    // genuine Success from this screen's own delete.
    LaunchedEffect(Unit) { viewModel.resetDeleteState() }
    var consumedInitialDeleteState by remember { mutableStateOf(false) }
    LaunchedEffect(deleteState) {
        if (!consumedInitialDeleteState) {
            consumedInitialDeleteState = true
            return@LaunchedEffect
        }
        if (deleteState is DeleteConsultaUiState.Success) onBack()
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
            Text(text = "Detalle de consulta", color = BrandGreen, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(20.dp))

            when (val state = detailState) {
                is ConsultaDetailUiState.Loading -> LoadingBox()
                is ConsultaDetailUiState.Error -> ErrorBox(
                    message = state.message,
                    onRetry = {
                        selectedPet?.id?.let { viewModel.fetchConsultaDetail(it, consultationId) }
                    }
                )
                is ConsultaDetailUiState.Loaded -> ConsultaDetailCard(state.consultation)
            }

            if (deleteState is DeleteConsultaUiState.Error) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = (deleteState as DeleteConsultaUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            val isDeleting = deleteState is DeleteConsultaUiState.Loading
            OutlinedButton(
                onClick = { showDeleteDialog = true },
                enabled = !isDeleting,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DeleteRed),
                border = BorderStroke(1.dp, DeleteRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(color = DeleteRed, strokeWidth = 2.dp, modifier = Modifier.height(20.dp))
                } else {
                    Icon(Icons.Filled.Delete, contentDescription = null, tint = DeleteRed, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Eliminar consulta", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar consulta") },
            text = { Text("¿Estás seguro de que deseas eliminar este registro de consulta? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        selectedPet?.id?.let { viewModel.deleteConsulta(it, consultationId) }
                    },
                ) { Text("Eliminar", color = DeleteRed) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar", color = BrandGreen) }
            },
        )
    }
}

@Composable
private fun ConsultaDetailCard(consultation: Consultation) {
    DetailCard {
        DetailRow("Motivo de consulta", consultation.reason)
        HorizontalDivider(color = CardBorder)
        DetailRow("Fecha de consulta", formatIsoDate(consultation.consultDate))
        consultation.symptoms?.takeIf { it.isNotBlank() }?.let {
            HorizontalDivider(color = CardBorder)
            DetailRow("Síntomas", it)
        }
        consultation.physicalExamFindings?.takeIf { it.isNotBlank() }?.let {
            HorizontalDivider(color = CardBorder)
            DetailRow("Hallazgos del examen físico", it)
        }
        consultation.diagnosis?.takeIf { it.isNotBlank() }?.let {
            HorizontalDivider(color = CardBorder)
            DetailRow("Diagnóstico", it)
        }
        consultation.treatment?.takeIf { it.isNotBlank() }?.let {
            HorizontalDivider(color = CardBorder)
            DetailRow("Tratamiento/Recomendaciones", it)
        }
        consultation.clinicName?.takeIf { it.isNotBlank() }?.let {
            HorizontalDivider(color = CardBorder)
            DetailRow("Veterinario / Clínica", it)
        }
    }
}

@Composable
private fun DetailCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp), content = content)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 14.dp)) {
        Text(text = label, color = SubtitleGray, fontSize = 12.sp)
        Text(text = value, color = TextDark, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
private fun ErrorBox(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = message, color = MaterialTheme.colorScheme.error, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text("Reintentar")
        }
    }
}

@Composable
private fun LoadingBox() {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = BrandGreen)
    }
}

private fun formatIsoDate(iso: String): String = relativeDateLabel(iso)
