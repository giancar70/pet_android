package com.petapp.android.features.pets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petapp.android.core.model.Pet
import com.petapp.android.core.model.petSpeciesLabel
import com.petapp.android.features.main.PetAvatar

private val BrandGreen = Color(0xFF406E5F)
private val SubtitleGray = Color(0xFF666666)
private val ContentBackground = Color(0xFFE3FBF1)
private val CardBorder = Color(0xFFEFEFF4)

@Composable
fun GestionarPetScreen(
    pets: List<Pet>,
    selectedPetId: String?,
    onBack: () -> Unit,
    onAddPet: () -> Unit,
    onOpenPetDetail: (Pet) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ContentBackground)
            .verticalScroll(rememberScrollState()),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.padding(start = 12.dp, top = 12.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Gestionar mascotas",
                color = BrandGreen,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Administra tus mascotas y comparte su información de forma segura.",
                color = SubtitleGray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onAddPet,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Registrar mascotas", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))

            pets.forEach { pet ->
                PetManageRow(
                    pet = pet,
                    isSelected = pet.id == selectedPetId,
                    onClick = { onOpenPetDetail(pet) },
                )
                Spacer(modifier = Modifier.height(14.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PetManageRow(pet: Pet, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PetAvatar(pet = pet, size = 56.dp, showStatusDot = isSelected)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = pet.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                val subtitle = pet.breed?.takeIf { it.isNotBlank() } ?: petSpeciesLabel(pet.species)
                Text(text = subtitle, color = SubtitleGray, fontSize = 14.sp)
                val age = formatPetAge(pet.birthDate)
                if (age != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Pets,
                            contentDescription = null,
                            tint = BrandGreen,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = age, color = SubtitleGray, fontSize = 13.sp)
                    }
                }
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = SubtitleGray)
        }
    }
}
