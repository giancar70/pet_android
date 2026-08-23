package com.petapp.android.features.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petapp.android.core.model.Pet

private val BrandGreen = Color(0xFF406E5F)
private val SubtitleGray = Color(0xFF666666)
private val AddPetCircleBg = Color(0xFFD9FEF2)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetSwitcherSheet(
    pets: List<Pet>,
    selectedPetId: String?,
    onSelectPet: (Pet) -> Unit,
    onAddPet: () -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        LazyColumn(modifier = Modifier.padding(bottom = 24.dp)) {
            items(pets, key = { it.id }) { pet ->
                PetRow(
                    pet = pet,
                    selected = pet.id == selectedPetId,
                    onClick = { onSelectPet(pet) },
                )
            }
            item {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onAddPet)
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(AddPetCircleBg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = BrandGreen)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(text = "Añadir mascota", color = BrandGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(text = "Registrar una nueva mascota", color = SubtitleGray, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PetRow(pet: Pet, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PetAvatar(pet = pet, size = 48.dp, showStatusDot = false)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = pet.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            val subtitle = pet.breed?.takeIf { it.isNotBlank() } ?: speciesLabelFor(pet.species)
            Text(text = subtitle, color = SubtitleGray, fontSize = 15.sp)
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(BrandGreen),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}

private fun speciesLabelFor(species: String): String = when (species) {
    "dog" -> "Perro"
    "cat" -> "Gato"
    else -> "Otro"
}
