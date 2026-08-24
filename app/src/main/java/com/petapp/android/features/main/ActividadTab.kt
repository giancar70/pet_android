package com.petapp.android.features.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petapp.android.core.model.Pet

private val SubtitleGray = Color(0xFF666666)

@Composable
fun ActividadTab(
    pets: List<Pet>,
    selectedPet: Pet?,
    userFullName: String?,
    onSwitchPetClick: () -> Unit,
    onAnadirVacuna: () -> Unit = {},
    onAnadirDesparasitacion: () -> Unit = {},
    onRegistrarConsulta: () -> Unit = {},
    onRegistrarIncidencia: () -> Unit = {},
    onAnadirRecordatorio: () -> Unit = {},
    onCapturarDocumento: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val hasPets = pets.isNotEmpty()
    Column(
        modifier = modifier
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
            PetActivityContent(
                petId = selectedPet?.id,
                onAnadirVacuna = onAnadirVacuna,
                onAnadirDesparasitacion = onAnadirDesparasitacion,
                onRegistrarConsulta = onRegistrarConsulta,
                onRegistrarIncidencia = onRegistrarIncidencia,
                onAnadirRecordatorio = onAnadirRecordatorio,
                onCapturarDocumento = onCapturarDocumento,
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
}
