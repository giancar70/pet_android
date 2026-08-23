package com.petapp.android.features.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petapp.android.core.model.Pet
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val BrandGreen = Color(0xFF406E5F)
private val SubtitleGray = Color(0xFF666666)
private val SunYellow = Color(0xFFF5A623)

@Composable
fun GreetingHeader(
    selectedPet: Pet?,
    userFullName: String?,
    hasPets: Boolean,
    onSwitchPetClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = Locale("es", "ES")
    val today = remember { LocalDate.now() }
    val dayName = today.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
        .replaceFirstChar { it.uppercase(locale) }
    val monthName = today.month.getDisplayName(TextStyle.FULL, locale)
        .replaceFirstChar { it.uppercase(locale) }
    val dateLabel = "$dayName ${today.dayOfMonth} $monthName"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.WbSunny,
                    contentDescription = null,
                    tint = SunYellow,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = dateLabel, color = SubtitleGray, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            val greetingName = if (hasPets && selectedPet != null) selectedPet.name else userFullName.orEmpty()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = if (hasPets) {
                    Modifier.clickable(onClick = onSwitchPetClick)
                } else {
                    Modifier
                },
            ) {
                Text(text = "Hola, $greetingName", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                if (hasPets) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = BrandGreen)
                }
            }
            if (hasPets && selectedPet != null) {
                val subtitle = selectedPet.breed?.takeIf { it.isNotBlank() } ?: speciesLabel(selectedPet.species)
                Text(text = subtitle, color = SubtitleGray, fontSize = 15.sp)
            }
        }
        PetAvatar(pet = if (hasPets) selectedPet else null, showStatusDot = hasPets)
    }
}

private fun speciesLabel(species: String): String = when (species) {
    "dog" -> "Perro"
    "cat" -> "Gato"
    else -> "Otro"
}
