package com.petapp.android.features.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petapp.android.R
import com.petapp.android.core.model.Pet
import com.petapp.android.ui.theme.PetProjectTheme

private val BrandGreen = Color(0xFF406E5F)
private val SubtitleGray = Color(0xFF666666)
private val ContentBackground = Color(0xFFE3FBF1)
private val CardBorder = Color(0xFFEFEFF4)
private val IllustrationBg = Color(0xFFD9FEF2)
private val InfoIconBg = Color(0xFFD9FEF2)

@Composable
fun InicioTab(
    pets: List<Pet>,
    selectedPet: Pet?,
    userFullName: String?,
    onSwitchPetClick: () -> Unit,
    onAddPetClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasPets = pets.isNotEmpty()
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(if (hasPets) Color.White else ContentBackground)
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
                Spacer(modifier = Modifier.height(8.dp))
                PetActivityContent(petId = selectedPet?.id)
                Spacer(modifier = Modifier.height(96.dp))
            } else {
                NoPetContent(onAddPetClick = onAddPetClick)
            }
        }
        if (hasPets) {
            FloatingActionButton(
                onClick = onMoreClick,
                containerColor = BrandGreen,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
            }
        }
    }
}

@Composable
private fun NoPetContent(onAddPetClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .size(180.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(IllustrationBg),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.main_dog),
                contentDescription = null,
                contentScale = ContentScale.FillHeight,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Aún no tienes mascotas registradas",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = "Agrega tu mascota para comenzar a guardar su historial de salud, vacunas, desparasitaciones y documentos importantes.",
            color = SubtitleGray,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(15.dp))
        Button(
            onClick = onAddPetClick,
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Añadir mascota", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = "¿Por qué agregar tu mascota?", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BrandGreen)
                Spacer(modifier = Modifier.height(16.dp))
                InfoRow(
                    icon = Icons.Filled.Shield,
                    title = "Todo su historial en un solo lugar",
                    description = "Accede a su información de salud cuando la necesites.",
                )
                Spacer(modifier = Modifier.height(16.dp))
                InfoRow(
                    icon = Icons.Filled.Notifications,
                    title = "Recordatorios personalizados",
                    description = "Recibe alertas de vacunas, desparasitaciones y controles.",
                )
                Spacer(modifier = Modifier.height(16.dp))
                InfoRow(
                    icon = Icons.Filled.Groups,
                    title = "Comparte con cuidadores y veterinarios",
                    description = "Invita a otros para que también puedan acceder a su información.",
                )
            }
        }
        Spacer(modifier = Modifier.height(96.dp))
    }
}

@Composable
private fun InfoRow(icon: ImageVector, title: String, description: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(InfoIconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(text = description, color = SubtitleGray, fontSize = 12.sp)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun InicioTabNoPetsPreview() {
    PetProjectTheme {
        InicioTab(
            pets = emptyList(),
            selectedPet = null,
            userFullName = "Juan Pérez",
            onSwitchPetClick = {},
            onAddPetClick = {},
            onMoreClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InicioTabWithPetsPreview() {
    val samplePet = Pet(
        id = "1",
        name = "Firulais",
        species = "dog",
        breed = "Labrador",
        sex = "M"
    )
    PetProjectTheme {
        InicioTab(
            pets = listOf(samplePet),
            selectedPet = samplePet,
            userFullName = "Juan Pérez",
            onSwitchPetClick = {},
            onAddPetClick = {},
            onMoreClick = {}
        )
    }
}
