package com.petapp.android.features.sharing

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petapp.android.core.model.Pet
import com.petapp.android.core.model.PetShare
import com.petapp.android.core.model.PetShareRole
import com.petapp.android.features.incidents.SuccessCheckmark
import com.petapp.android.features.main.GreetingHeader

private val BrandGreen = Color(0xFF406E5F)
private val SubtitleGray = Color(0xFF666666)
private val ContentBackground = Color(0xFFE3FBF1)
private val CardBg = Color(0xFFF5F5F5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompartirMascotaScreen(
    selectedPet: Pet?,
    userFullName: String?,
    onBack: () -> Unit,
    onFinish: () -> Unit,
) {
    val viewModel: SharingViewModel = viewModel()
    val shareState by viewModel.shareState.collectAsState()
    val listState by viewModel.listState.collectAsState()

    var email by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(PetShareRole.VETERINARY) }
    var validationError by remember { mutableStateOf<String?>(null) }

    // SharingViewModel is Activity-scoped (no Navigation-Compose back stack), so a
    // prior success would otherwise still be sitting in shareState on re-entry.
    LaunchedEffect(Unit) {
        viewModel.resetShareState()
    }
    LaunchedEffect(selectedPet?.id) {
        selectedPet?.id?.let { viewModel.fetchShares(it) }
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
                .background(ContentBackground)
                .padding(24.dp),
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            PersonasConAccesoSection(listState)

            Spacer(modifier = Modifier.height(24.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = CardBg,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val successShare = (shareState as? SharePetUiState.Success)?.share
                    if (successShare != null) {
                        SuccessCheckmark()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "Invitación enviada", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${successShare.userEmail} ahora tiene acceso como ${roleLabel(successShare.role)}.",
                            color = SubtitleGray,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        OutlinedButton(
                            onClick = {
                                email = ""
                                viewModel.resetShareState()
                            },
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandGreen),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                        ) {
                            Text(text = "Compartir con otra persona", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onFinish,
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                        ) {
                            Text(text = "Finalizar", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(text = "Compartir Mascota", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF333333))
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = { Text("Correo electrónico") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = BrandGreen,
                                unfocusedContainerColor = Color.White,
                                focusedContainerColor = Color.White,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        PetShareRole.entries.forEach { option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { role = option },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = role == option,
                                    onClick = { role = option },
                                    colors = RadioButtonDefaults.colors(selectedColor = BrandGreen),
                                )
                                Text(text = option.label, fontSize = 15.sp, color = Color(0xFF333333))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        val apiErrorMessage = (shareState as? SharePetUiState.Error)?.message
                        if (validationError != null || apiErrorMessage != null) {
                            Text(
                                text = validationError ?: apiErrorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        val isLoading = shareState is SharePetUiState.Loading
                        Button(
                            onClick = {
                                val petId = selectedPet?.id
                                when {
                                    email.isBlank() -> validationError = "Ingresa un correo electrónico."
                                    petId == null -> validationError = "Agrega una mascota primero."
                                    else -> {
                                        validationError = null
                                        viewModel.sharePet(petId, email.trim(), role.apiValue)
                                    }
                                }
                            },
                            enabled = !isLoading,
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.height(20.dp))
                            } else {
                                Text(text = "Enviar Invitación", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

private fun roleLabel(apiValue: String): String =
    PetShareRole.entries.find { it.apiValue == apiValue }?.label ?: apiValue

@Composable
private fun PersonasConAccesoSection(listState: SharesListUiState) {
    when (listState) {
        is SharesListUiState.Loading -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = BrandGreen, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = "Cargando personas con acceso…", color = SubtitleGray, fontSize = 13.sp)
            }
        }
        is SharesListUiState.Error -> {
            Text(text = listState.message, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }
        is SharesListUiState.Loaded -> {
            if (listState.shares.isNotEmpty()) {
                Text(text = "Personas con acceso", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF333333))
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listState.shares.forEach { share -> PersonaConAccesoRow(share) }
                }
            }
        }
    }
}

@Composable
private fun PersonaConAccesoRow(share: PetShare) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFEFEFF4)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFFD9FEF2), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = initialFor(share), color = BrandGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                val name = share.userFullName.ifBlank { share.userEmail }
                Text(text = name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(text = share.userEmail, color = SubtitleGray, fontSize = 12.sp)
            }
            Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFD9FEF2)) {
                Text(
                    text = roleLabel(share.role),
                    color = BrandGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}

private fun initialFor(share: PetShare): String {
    val source = share.userFullName.ifBlank { share.userEmail }
    return source.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
}
