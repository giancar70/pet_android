package com.petapp.android.features.vaccines

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petapp.android.core.model.Pet
import com.petapp.android.core.model.VaccineDose
import com.petapp.android.features.incidents.spanishDate
import com.petapp.android.features.main.GreetingHeader
import java.time.LocalDate

private val BrandGreen = Color(0xFF406E5F)
private val SubtitleGray = Color(0xFF666666)
private val CardBorder = Color(0xFFEFEFF4)

@Composable
fun VacunaDetailScreen(
    selectedPet: Pet?,
    userFullName: String?,
    doseId: String,
    onBack: () -> Unit,
    viewModel: VaccinesViewModel = viewModel(),
) {
    val detailState by viewModel.detailState.collectAsState()

    LaunchedEffect(selectedPet?.id, doseId) {
        selectedPet?.id?.let { viewModel.fetchVacunaDetail(it, doseId) }
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
            Text(text = "Detalle de vacuna", color = BrandGreen, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(20.dp))

            when (val state = detailState) {
                is VaccineDoseDetailUiState.Loading -> LoadingBox()
                is VaccineDoseDetailUiState.Error -> Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                )
                is VaccineDoseDetailUiState.Loaded -> VacunaDetailCard(state.dose)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun VacunaDetailCard(dose: VaccineDose) {
    DetailCard {
        DetailRow("Vacuna", dose.vaccine)
        HorizontalDivider(color = CardBorder)
        DetailRow("Fecha de aplicación", formatIsoDate(dose.appliedOn))
        dose.lotNumber?.takeIf { it.isNotBlank() }?.let {
            HorizontalDivider(color = CardBorder)
            DetailRow("N° de lote", it)
        }
        dose.notes?.takeIf { it.isNotBlank() }?.let {
            HorizontalDivider(color = CardBorder)
            DetailRow("Observaciones", it)
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

private fun formatIsoDate(iso: String): String = try {
    spanishDate(LocalDate.parse(iso))
} catch (e: Exception) {
    iso
}
