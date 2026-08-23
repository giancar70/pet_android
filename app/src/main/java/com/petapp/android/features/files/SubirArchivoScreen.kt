package com.petapp.android.features.files

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petapp.android.core.model.Pet
import com.petapp.android.features.main.GreetingHeader
import java.text.DecimalFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val BrandGreen = Color(0xFF406E5F)
private val SubtitleGray = Color(0xFF666666)
private val IllustrationBg = Color(0xFFD9FEF2)
private val CardBorder = Color(0xFFEFEFF4)
private val ChipBg = Color(0xFFD9FEF2)

private data class SelectedFile(
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
    val lastModifiedMillis: Long?,
)

@Composable
fun SubirArchivoScreen(
    selectedPet: Pet?,
    userFullName: String?,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    var selectedFile by remember { mutableStateOf<SelectedFile?>(null) }
    val file = selectedFile

    if (file == null) {
        SubirArchivoStep(
            selectedPet = selectedPet,
            userFullName = userFullName,
            onBack = onBack,
            onFilePicked = { selectedFile = it },
        )
    } else {
        ArchivoSeleccionadoStep(
            selectedPet = selectedPet,
            userFullName = userFullName,
            file = file,
            onBack = { selectedFile = null },
            onContinue = onDone,
        )
    }
}

@Composable
private fun SubirArchivoStep(
    selectedPet: Pet?,
    userFullName: String?,
    onBack: () -> Unit,
    onFilePicked: (SelectedFile) -> Unit,
) {
    val context = LocalContext.current

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri -> if (uri != null) onFilePicked(readFileMeta(context, uri)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState()),
    ) {
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
            Spacer(modifier = Modifier.height(8.dp))
            UploadIllustration()
            Spacer(modifier = Modifier.height(20.dp))
            Text(text = "Subir archivo", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            val petName = selectedPet?.name ?: "tu mascota"
            Text(
                text = "Sube documentos, análisis, estudios, imágenes o videos relacionados con la salud de $petName.",
                color = SubtitleGray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(20.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .dashedBorder(BrandGreen)
                    .clickable { filePicker.launch("*/*") }
                    .padding(vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Toca para seleccionar", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(text = "o arrastra tu archivo aquí", color = SubtitleGray, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(text = "Formatos admitidos", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                FormatChip(Icons.Filled.Description, "PDF", Modifier.weight(1f))
                FormatChip(Icons.Filled.Image, "Imágenes", Modifier.weight(1f))
                FormatChip(Icons.Filled.Videocam, "Videos", Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun UploadIllustration() {
    Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
        Icon(
            Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = Color(0xFFF5A623),
            modifier = Modifier.size(16.dp).align(Alignment.TopStart),
        )
        Icon(
            Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = Color(0xFF3B82F6),
            modifier = Modifier.size(18.dp).align(Alignment.TopEnd),
        )
        Icon(
            Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = Color(0xFF3B82F6),
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
                .size(96.dp)
                .clip(CircleShape)
                .background(IllustrationBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Folder, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(44.dp))
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .align(Alignment.BottomEnd)
                .clip(CircleShape)
                .background(BrandGreen),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun FormatChip(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ChipBg,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = label, color = BrandGreen, fontWeight = FontWeight.Medium, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ArchivoSeleccionadoStep(
    selectedPet: Pet?,
    userFullName: String?,
    file: SelectedFile,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState()),
    ) {
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
            Text(text = "Archivo seleccionado", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(20.dp))

            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF5F5F5)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(iconForMimeType(file.mimeType), contentDescription = null, tint = fileIconTint(file.mimeType), modifier = Modifier.size(40.dp))
                }
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = BrandGreen,
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color.White, CircleShape),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = file.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center)

            Spacer(modifier = Modifier.height(24.dp))
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    InfoRow(Icons.AutoMirrored.Filled.InsertDriveFile, "Tipo de archivo", fileTypeLabel(file.mimeType))
                    HorizontalDivider(color = CardBorder)
                    InfoRow(Icons.Filled.CalendarToday, "Fecha de modificación", formatLastModified(file.lastModifiedMillis))
                    HorizontalDivider(color = CardBorder)
                    InfoRow(Icons.Filled.SdStorage, "Tamaño", formatFileSize(file.sizeBytes))
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = onContinue,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(text = "Continuar", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(text = value, color = SubtitleGray, fontSize = 13.sp)
        }
    }
}

private fun Modifier.dashedBorder(color: Color, strokeWidth: androidx.compose.ui.unit.Dp = 1.5.dp): Modifier =
    this.drawBehind {
        drawRoundRect(
            color = color,
            style = Stroke(
                width = strokeWidth.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f),
            ),
            cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx()),
        )
    }

private fun readFileMeta(context: Context, uri: Uri): SelectedFile {
    var name = "archivo"
    var size = 0L
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (cursor.moveToFirst()) {
            if (nameIdx >= 0) cursor.getString(nameIdx)?.let { name = it }
            if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) size = cursor.getLong(sizeIdx)
        }
    }
    val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
    val lastModified = try {
        context.contentResolver.query(
            uri,
            arrayOf(DocumentsContract.Document.COLUMN_LAST_MODIFIED),
            null,
            null,
            null,
        )?.use { cursor ->
            val idx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
            if (idx >= 0 && cursor.moveToFirst() && !cursor.isNull(idx)) cursor.getLong(idx) else null
        }
    } catch (e: Exception) {
        null
    }
    return SelectedFile(uri, name, mimeType, size, lastModified)
}

private fun iconForMimeType(mimeType: String): ImageVector = when {
    mimeType == "application/pdf" -> Icons.Filled.PictureAsPdf
    mimeType.startsWith("image/") -> Icons.Filled.Image
    mimeType.startsWith("video/") -> Icons.Filled.Videocam
    else -> Icons.AutoMirrored.Filled.InsertDriveFile
}

private fun fileIconTint(mimeType: String): Color = when {
    mimeType == "application/pdf" -> Color(0xFFE53935)
    mimeType.startsWith("image/") -> Color(0xFF3B82F6)
    mimeType.startsWith("video/") -> Color(0xFF8E24AA)
    else -> BrandGreen
}

private fun fileTypeLabel(mimeType: String): String = when {
    mimeType == "application/pdf" -> "PDF"
    mimeType.startsWith("image/") -> "Imagen (${mimeType.substringAfter('/').uppercase()})"
    mimeType.startsWith("video/") -> "Video (${mimeType.substringAfter('/').uppercase()})"
    else -> mimeType
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "—"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val df = DecimalFormat("0.0")
    return when {
        mb >= 1.0 -> "${df.format(mb)} MB"
        kb >= 1.0 -> "${df.format(kb)} KB"
        else -> "$bytes B"
    }
}

private fun formatLastModified(millis: Long?): String {
    if (millis == null) return "—"
    val dateTime = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
    val months = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
    val datePart = "${dateTime.dayOfMonth} ${months[dateTime.monthValue - 1]} ${dateTime.year}"
    val timePart = dateTime.format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()))
    return "$datePart - $timePart"
}
