package com.petapp.android.features.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BrandGreen = Color(0xFF406E5F)

private data class MoreOption(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreOptionsSheet(
    onDismiss: () -> Unit,
    sheetState: SheetState,
    onAddPet: () -> Unit,
    onRegistrarIncidencia: () -> Unit,
    onAnadirVacuna: () -> Unit,
    onAnadirDesparasitacion: () -> Unit,
    onAnadirRecordatorio: () -> Unit,
    onCompartirMascota: () -> Unit,
    onRegistrarConsulta: () -> Unit,
    onSubirArchivo: () -> Unit,
) {
    val options = listOf(
        MoreOption(Icons.Filled.Upload, "Subir Archivo", onSubirArchivo),
        MoreOption(Icons.Filled.DocumentScanner, "Capturar\nDocumento", onDismiss),
        MoreOption(Icons.Filled.CameraAlt, "Registrar\nIncidencia", onRegistrarIncidencia),
        MoreOption(Icons.Filled.Vaccines, "Añadir\nVacuna", onAnadirVacuna),
        MoreOption(Icons.Filled.Medication, "Añadir\nDesparasitación", onAnadirDesparasitacion),
        MoreOption(Icons.Filled.MedicalServices, "Añadir\nConsulta", onRegistrarConsulta),
        MoreOption(Icons.Filled.Share, "Compartir\nmascota", onCompartirMascota),
        MoreOption(Icons.Filled.Add, "Añadir\nmascota", onAddPet),
        MoreOption(Icons.Filled.Description, "Añadir\nRecordatorio", onAnadirRecordatorio),
    )

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
        ) {
            items(options) { option ->
                MoreOptionItem(option)
            }
        }
    }
}

@Composable
private fun MoreOptionItem(option: MoreOption) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = option.onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(BrandGreen),
            contentAlignment = Alignment.Center,
        ) {
            Icon(option.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = option.label,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
    }
}
