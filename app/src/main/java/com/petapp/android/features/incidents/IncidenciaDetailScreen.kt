package com.petapp.android.features.incidents

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.petapp.android.core.model.Document
import com.petapp.android.core.model.Pet
import com.petapp.android.core.model.PetEvent
import com.petapp.android.core.util.relativeDateTimeLabel
import com.petapp.android.features.main.GreetingHeader

private val BrandGreen = Color(0xFF406E5F)
private val SubtitleGray = Color(0xFF666666)
private val CardBorder = Color(0xFFEFEFF4)
private val EvidenceIconBg = Color(0xFFD9FEF2)
private val DeleteRed = Color(0xFFC0392B)

@Composable
fun IncidenciaDetailScreen(
    selectedPet: Pet?,
    userFullName: String?,
    eventId: String,
    onBack: () -> Unit,
    viewModel: IncidentsViewModel = viewModel(),
) {
    val detailState by viewModel.detailState.collectAsState()
    val documentsState by viewModel.documentsState.collectAsState()
    val deleteState by viewModel.deleteState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(selectedPet?.id, eventId) {
        selectedPet?.id?.let {
            viewModel.fetchIncidenciaDetail(it, eventId)
            viewModel.fetchIncidenciaDocuments(it, eventId)
        }
    }

    // IncidentsViewModel is Activity-scoped, so a prior deletion's Success can still be
    // sitting in deleteState when this screen re-enters for a different incidencia;
    // ignore the first firing regardless of what it holds, and only act on a later,
    // genuine Success from this screen's own delete.
    LaunchedEffect(Unit) { viewModel.resetDeleteState() }
    var consumedInitialDeleteState by remember { mutableStateOf(false) }
    LaunchedEffect(deleteState) {
        if (!consumedInitialDeleteState) {
            consumedInitialDeleteState = true
            return@LaunchedEffect
        }
        if (deleteState is DeleteIncidenciaUiState.Success) onBack()
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
            Text(text = "Detalle de incidencia", color = BrandGreen, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(20.dp))

            when (val state = detailState) {
                is IncidenciaDetailUiState.Loading -> LoadingBox()
                is IncidenciaDetailUiState.Error -> Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                )
                is IncidenciaDetailUiState.Loaded -> IncidenciaDetailCard(state.event)
            }

            val loadedDocuments = (documentsState as? IncidenciaDocumentsUiState.Loaded)?.documents.orEmpty()
            if (loadedDocuments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(text = "Evidencia", color = BrandGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    loadedDocuments.forEach { document -> EvidenceRow(document) }
                }
            }

            if (deleteState is DeleteIncidenciaUiState.Error) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = (deleteState as DeleteIncidenciaUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            val isDeleting = deleteState is DeleteIncidenciaUiState.Loading
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
                    Text(text = "Eliminar incidencia", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar incidencia") },
            text = { Text("¿Estás seguro de que deseas eliminar este registro de incidencia? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        selectedPet?.id?.let { viewModel.deleteIncidencia(it, eventId) }
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
private fun IncidenciaDetailCard(event: PetEvent) {
    DetailCard {
        DetailRow("Título", event.title)
        HorizontalDivider(color = CardBorder)
        DetailRow("Fecha y hora", formatIsoDateTime(event.eventDate))
        event.description?.takeIf { it.isNotBlank() }?.let {
            HorizontalDivider(color = CardBorder)
            DetailRow("Descripción", it)
        }
        event.issuerName?.takeIf { it.isNotBlank() }?.let {
            HorizontalDivider(color = CardBorder)
            DetailRow("Registrado por", it)
        }
    }
}

@Composable
private fun EvidenceRow(document: Document) {
    val context = LocalContext.current
    val fileUrl = document.file
    val isImage = document.mimeType?.startsWith("image/") == true
    val fileName = fileUrl?.substringAfterLast('/') ?: (document.title ?: "Archivo")

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, CardBorder),
        onClick = {
            if (fileUrl != null) {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fileUrl))) }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isImage && fileUrl != null) {
                AsyncImage(
                    model = fileUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp)),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(EvidenceIconBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Description, contentDescription = null, tint = BrandGreen)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = fileName,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = SubtitleGray, modifier = Modifier.size(18.dp))
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
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
private fun LoadingBox() {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = BrandGreen)
    }
}

private fun formatIsoDateTime(iso: String): String = relativeDateTimeLabel(iso)
