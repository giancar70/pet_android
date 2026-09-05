package com.petapp.android.features.files

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Videocam
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.petapp.android.core.model.Document
import com.petapp.android.core.model.Pet
import com.petapp.android.core.util.relativeDateLabel
import com.petapp.android.features.main.GreetingHeader

private val BrandGreen = Color(0xFF406E5F)
private val SubtitleGray = Color(0xFF666666)
private val CardBorder = Color(0xFFEFEFF4)
private val PreviewBg = Color(0xFFF5F5F5)
private val IncidentRed = Color(0xFFC0392B)

@Composable
fun DocumentDetailScreen(
    selectedPet: Pet?,
    userFullName: String?,
    documentId: String,
    onBack: () -> Unit,
    viewModel: FilesViewModel = viewModel(),
) {
    val detailState by viewModel.detailState.collectAsState()
    val deleteState by viewModel.deleteState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(selectedPet?.id, documentId) {
        selectedPet?.id?.let { viewModel.fetchDocumentDetail(it, documentId) }
    }

    // FilesViewModel is Activity-scoped, so a prior deletion's Success can still be
    // sitting in deleteState when this screen re-enters for a different document;
    // ignore the first firing regardless of what it holds, and only act on a later,
    // genuine Success from this screen's own delete (same race-avoidance pattern as
    // VacunaDetailScreen).
    LaunchedEffect(Unit) { viewModel.resetDeleteState() }
    var consumedInitialDeleteState by remember { mutableStateOf(false) }
    LaunchedEffect(deleteState) {
        if (!consumedInitialDeleteState) {
            consumedInitialDeleteState = true
            return@LaunchedEffect
        }
        if (deleteState is DeleteDocumentUiState.Success) onBack()
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "Detalle de documento", color = BrandGreen, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(20.dp))

            when (val state = detailState) {
                is DocumentDetailUiState.Loading -> LoadingBox()
                is DocumentDetailUiState.Error -> Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                )
                is DocumentDetailUiState.Loaded -> DocumentDetailContent(state.document)
            }

            if (deleteState is DeleteDocumentUiState.Error) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = (deleteState as DeleteDocumentUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            val isDeleting = deleteState is DeleteDocumentUiState.Loading
            OutlinedButton(
                onClick = { showDeleteDialog = true },
                enabled = !isDeleting,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = IncidentRed),
                border = BorderStroke(1.dp, IncidentRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(color = IncidentRed, strokeWidth = 2.dp, modifier = Modifier.height(20.dp))
                } else {
                    Icon(Icons.Filled.Delete, contentDescription = null, tint = IncidentRed, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Eliminar documento", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar documento") },
            text = { Text("¿Estás seguro de que deseas eliminar este documento? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        selectedPet?.id?.let { viewModel.deleteDocument(it, documentId) }
                    },
                ) { Text("Eliminar", color = IncidentRed) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar", color = BrandGreen) }
            },
        )
    }
}

@Composable
private fun DocumentDetailContent(document: Document) {
    val context = LocalContext.current
    val fileUrl = document.file
    val isImage = document.mimeType?.startsWith("image/") == true

    val linkedIncidencia = document.eventTitle

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(PreviewBg),
        contentAlignment = Alignment.Center,
    ) {
        if (isImage && fileUrl != null) {
            AsyncImage(
                model = fileUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                iconForMimeType(document.mimeType),
                contentDescription = null,
                tint = BrandGreen,
                modifier = Modifier.size(64.dp),
            )
        }
        if (linkedIncidencia != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(IncidentRed),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.ReportProblem, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
    Spacer(modifier = Modifier.height(20.dp))

    Text(
        text = linkedIncidencia ?: (document.title?.takeIf { it.isNotBlank() } ?: (fileUrl?.substringAfterLast('/') ?: "Documento")),
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        textAlign = TextAlign.Center,
    )
    if (linkedIncidencia != null) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "Documento de incidencia", color = IncidentRed, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
    Spacer(modifier = Modifier.height(20.dp))

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            DetailRow("Tipo de archivo", fileTypeLabel(document.mimeType))
            HorizontalDivider(color = CardBorder)
            DetailRow("Fecha", formatDate(document.documentDate ?: document.createdAt))
        }
    }

    val ocrText = document.ocrText?.trim()
    if (document.ocrStatus == "done" && !ocrText.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(20.dp))
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Texto detectado", color = SubtitleGray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = ocrText, fontSize = 13.sp, color = Color.Black)
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))
    Button(
        onClick = {
            if (fileUrl != null) {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fileUrl))) }
            }
        },
        enabled = fileUrl != null,
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "Abrir archivo", fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
    ) {
        Text(text = label, color = SubtitleGray, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun LoadingBox() {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = BrandGreen)
    }
}

private fun iconForMimeType(mimeType: String?): ImageVector = when {
    mimeType == "application/pdf" -> Icons.Filled.PictureAsPdf
    mimeType?.startsWith("image/") == true -> Icons.Filled.Image
    mimeType?.startsWith("video/") == true -> Icons.Filled.Videocam
    else -> Icons.AutoMirrored.Filled.InsertDriveFile
}

private fun fileTypeLabel(mimeType: String?): String = when {
    mimeType == null -> "—"
    mimeType == "application/pdf" -> "PDF"
    mimeType.startsWith("image/") -> "Imagen (${mimeType.substringAfter('/').uppercase()})"
    mimeType.startsWith("video/") -> "Video (${mimeType.substringAfter('/').uppercase()})"
    else -> mimeType
}

private fun formatDate(iso: String): String = relativeDateLabel(iso)
