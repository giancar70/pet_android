package com.petapp.android.features.account

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petapp.android.features.main.DeleteAccountUiState
import com.petapp.android.features.main.UserViewModel

private val BrandGreen = Color(0xFF406E5F)
private val SubtitleGray = Color(0xFF666666)
private val ContentBackground = Color(0xFFE3FBF1)
private val CardBorder = Color(0xFFEFEFF4)
private val DeleteRed = Color(0xFFB3261E)

@Composable
fun PrivacidadScreen(
    onBack: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenTerms: () -> Unit,
    onAccountDeleted: () -> Unit,
    viewModel: UserViewModel = viewModel(),
) {
    val deleteState by viewModel.deleteAccountState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    // UserViewModel is Activity-scoped (no Navigation-Compose back stack), so a prior
    // delete attempt's state would otherwise still be sitting here on re-entry.
    LaunchedEffect(Unit) {
        viewModel.resetDeleteAccountState()
    }
    LaunchedEffect(deleteState) {
        if (deleteState is DeleteAccountUiState.Success) onAccountDeleted()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ContentBackground)
            .verticalScroll(rememberScrollState()),
    ) {
        BackHandler(onBack = onBack)
        IconButton(onClick = onBack, modifier = Modifier.padding(start = 12.dp, top = 12.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "Privacidad y datos", color = BrandGreen, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Gestiona tu información personal y el uso de tus datos en PetDrive",
                color = SubtitleGray,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(28.dp))
            PrivacyRow(
                icon = Icons.Filled.Description,
                title = "Política de privacidad",
                onClick = onOpenPrivacyPolicy,
            )
            Spacer(modifier = Modifier.height(12.dp))
            PrivacyRow(
                icon = Icons.Filled.Description,
                title = "Términos y condiciones",
                onClick = onOpenTerms,
            )
            Spacer(modifier = Modifier.height(12.dp))
            PrivacyRow(
                icon = Icons.Filled.Delete,
                title = "Eliminar cuenta",
                titleColor = DeleteRed,
                iconTint = DeleteRed,
                onClick = { showDeleteDialog = true },
            )

            Spacer(modifier = Modifier.height(48.dp))
            Icon(Icons.Filled.Shield, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "Tu información está protegida", color = BrandGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "PetDrive cumple con las normas de \n protección de datos y privacidad.",
                color = SubtitleGray,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showDeleteDialog) {
        DeleteAccountDialog(
            isLoading = deleteState is DeleteAccountUiState.Loading,
            errorMessage = (deleteState as? DeleteAccountUiState.Error)?.message,
            onDismiss = { showDeleteDialog = false },
            onConfirm = { viewModel.deleteAccount() },
        )
    }
}

@Composable
private fun PrivacyRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    titleColor: Color = Color.Black,
    iconTint: Color = BrandGreen,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = titleColor, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = titleColor)
        }
    }
}

@Composable
private fun DeleteAccountDialog(
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color.White) {
            Box {
                IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd)) {
                    Icon(Icons.Filled.Close, contentDescription = null)
                }
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, tint = DeleteRed, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(text = "Eliminar cuenta", color = DeleteRed, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Se eliminará tu cuenta, todas tus mascotas, documentos y accesos compartidos. Esta acción no se puede deshacer.",
                        color = SubtitleGray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                    )
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Row {
                        OutlinedButton(
                            onClick = onDismiss,
                            enabled = !isLoading,
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = DeleteRed),
                            border = BorderStroke(1.dp, DeleteRed),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(text = "Cancelar", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = onConfirm,
                            enabled = !isLoading,
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DeleteRed),
                            modifier = Modifier.weight(1f),
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.height(18.dp))
                            } else {
                                Text(text = "Solicitar", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
