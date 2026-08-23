package com.petapp.android.features.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petapp.android.core.model.Pet
import com.petapp.android.ui.theme.PetProjectTheme

private val BrandGreen = Color(0xFF406E5F)
private val SubtitleGray = Color(0xFF666666)
private val ContentBackground = Color(0xFFE3FBF1)
private val CardBorder = Color(0xFFEFEFF4)
private val IconCircleBg = Color(0xFFD9FEF2)
private val LogoutRed = Color(0xFFC0392B)

@Composable
fun MasTab(
    pets: List<Pet>,
    selectedPet: Pet?,
    userFullName: String?,
    onSwitchPetClick: () -> Unit,
    onGestionarPetsClick: () -> Unit,
    onMiCuentaClick: () -> Unit,
    onAjustesClick: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(text = "Cerrar sesión") },
            text = { Text(text = "¿Estás seguro de que deseas cerrar sesión?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text(text = "Confirmar", color = LogoutRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(text = "Cancelar", color = BrandGreen)
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ContentBackground)
            .verticalScroll(rememberScrollState()),
    ) {
        Surface(color = Color.White) {
            GreetingHeader(
                selectedPet = selectedPet,
                userFullName = userFullName,
                hasPets = pets.isNotEmpty(),
                onSwitchPetClick = onSwitchPetClick,
            )
        }
        Column(modifier = Modifier.padding(24.dp)) {
            Text(text = "Más", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            MasRow(
                icon = Icons.Filled.Pets,
                title = "Gestionar mascotas",
                subtitle = "Añade, edita o comparte tus mascota",
                onClick = onGestionarPetsClick,
            )
            Spacer(modifier = Modifier.height(12.dp))
            MasRow(
                icon = Icons.Filled.Person,
                title = "Mi cuenta",
                subtitle = "Perfil, correo y notificaciones",
                onClick = onMiCuentaClick,
            )
            Spacer(modifier = Modifier.height(12.dp))
            MasRow(
                icon = Icons.Filled.Settings,
                title = "Ajustes",
                subtitle = "Privacidad, permisos y apariencia",
                onClick = onAjustesClick,
            )
            Spacer(modifier = Modifier.height(12.dp))
            MasRow(
                icon = Icons.AutoMirrored.Filled.Logout,
                title = "Cerrar sesión",
                subtitle = "Salir de tu cuenta",
                titleColor = LogoutRed,
                iconTint = LogoutRed,
                onClick = { showLogoutDialog = true },
            )
        }
    }
}

@Composable
private fun MasRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    titleColor: Color = Color.Black,
    iconTint: Color = BrandGreen,
    onClick: (() -> Unit)? = null,
) {
    val rowModifier = if (onClick != null) {
        Modifier.fillMaxWidth().clickable(onClick = onClick)
    } else {
        Modifier.fillMaxWidth()
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, CardBorder),
        modifier = rowModifier,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconCircle(icon = icon, tint = iconTint)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = titleColor)
                Text(text = subtitle, color = SubtitleGray, fontSize = 11.sp)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = SubtitleGray)
        }
    }
}

@Composable
private fun IconCircle(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(IconCircleBg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun MasTabPreview() {
    val samplePet = Pet(
        id = "1",
        name = "Firulais",
        species = "dog",
        breed = "Labrador",
        sex = "M"
    )
    PetProjectTheme {
        MasTab(
            pets = listOf(samplePet),
            selectedPet = samplePet,
            userFullName = "Juan Pérez",
            onSwitchPetClick = {},
            onGestionarPetsClick = {},
            onMiCuentaClick = {},
            onAjustesClick = {},
            onLogout = {}
        )
    }
}
