package com.petapp.android.features.account

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petapp.android.core.model.User
import com.petapp.android.features.main.UpdateUserUiState
import com.petapp.android.features.main.UserUiState
import com.petapp.android.features.main.UserViewModel
import com.petapp.android.ui.theme.PetProjectTheme

private val BrandGreen = Color(0xFF406E5F)
private val SubtitleGray = Color(0xFF666666)
private val ContentBackground = Color(0xFFE3FBF1)
private val CardBorder = Color(0xFFEFEFF4)
private val AvatarBg = Color(0xFFD9FEF2)

private enum class AccountField { NOMBRE, TELEFONO }

@Composable
fun MiCuentaScreen(
    onBack: () -> Unit,
    viewModel: UserViewModel = viewModel(),
) {
    val userState by viewModel.uiState.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    val user = (userState as? UserUiState.Loaded)?.user

    MiCuentaContent(
        user = user,
        updateState = updateState,
        onBack = onBack,
        onUpdateProfile = { fullName, phoneNumber, notifyVaccineReminders, notifyDewormingReminders, notifyAppointmentReminders, notifyPetShared, notifyDocumentUploaded, notifyViaPush, notifyViaEmail ->
            viewModel.updateProfile(
                fullName = fullName,
                phoneNumber = phoneNumber,
                notifyVaccineReminders = notifyVaccineReminders,
                notifyDewormingReminders = notifyDewormingReminders,
                notifyAppointmentReminders = notifyAppointmentReminders,
                notifyPetShared = notifyPetShared,
                notifyDocumentUploaded = notifyDocumentUploaded,
                notifyViaPush = notifyViaPush,
                notifyViaEmail = notifyViaEmail
            )
        },
        resetUpdateState = { viewModel.resetUpdateState() }
    )
}

@Composable
fun MiCuentaContent(
    user: User?,
    updateState: UpdateUserUiState,
    onBack: () -> Unit,
    onUpdateProfile: (
        fullName: String?,
        phoneNumber: String?,
        notifyVaccineReminders: Boolean?,
        notifyDewormingReminders: Boolean?,
        notifyAppointmentReminders: Boolean?,
        notifyPetShared: Boolean?,
        notifyDocumentUploaded: Boolean?,
        notifyViaPush: Boolean?,
        notifyViaEmail: Boolean?
    ) -> Unit,
    resetUpdateState: () -> Unit,
) {
    var activeDialog by remember { mutableStateOf<AccountField?>(null) }

    LaunchedEffect(Unit) {
        resetUpdateState()
    }
    LaunchedEffect(updateState) {
        if (updateState is UpdateUserUiState.Success) activeDialog = null
    }

    var vacunas by remember(user) { mutableStateOf(user?.notifyVaccineReminders ?: true) }
    var desparasitacion by remember(user) { mutableStateOf(user?.notifyDewormingReminders ?: true) }
    var citas by remember(user) { mutableStateOf(user?.notifyAppointmentReminders ?: true) }
    var compartida by remember(user) { mutableStateOf(user?.notifyPetShared ?: true) }
    var documentoSubido by remember(user) { mutableStateOf(user?.notifyDocumentUploaded ?: true) }
    var avisoPush by remember(user) { mutableStateOf(user?.notifyViaPush ?: true) }
    var avisoEmail by remember(user) { mutableStateOf(user?.notifyViaEmail ?: true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
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
            Text(text = "Mi cuenta", color = BrandGreen, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Administra tu información personal y tus preferencias de notificación",
                color = SubtitleGray,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(AvatarBg, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = initialsFor(user?.fullName), color = BrandGreen, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(text = user?.fullName ?: "—", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(text = user?.email ?: "", color = SubtitleGray, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            SectionTitle("Perfil")
            Spacer(modifier = Modifier.height(10.dp))
            AccountCard {
                AccountRow(
                    icon = Icons.Filled.Person,
                    title = "Nombre completo",
                    value = user?.fullName ?: "—",
                    onClick = { activeDialog = AccountField.NOMBRE },
                )
                HorizontalDivider(color = CardBorder)
                AccountRow(
                    icon = Icons.Filled.Email,
                    title = "Email",
                    value = user?.email ?: "—",
                    onClick = null,
                )
                HorizontalDivider(color = CardBorder)
                AccountRow(
                    icon = Icons.Filled.Phone,
                    title = "Teléfono",
                    value = user?.phoneNumber?.takeIf { it.isNotBlank() } ?: "Agregar",
                    onClick = { activeDialog = AccountField.TELEFONO },
                )
            }
            if (updateState is UpdateUserUiState.Error) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = updateState.message,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ContentBackground)
                .padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            SectionTitle("Preferencias de notificación")
            Text(text = "Elige sobre qué temas quieres recibir avisos.", color = SubtitleGray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(10.dp))
            AccountCard {
                ToggleRow(Icons.Filled.Vaccines, "Recordatorios de vacunas", "Te avisamos antes de que venza una vacuna.", vacunas) {
                    vacunas = it
                    onUpdateProfile(null, null, it, null, null, null, null, null, null)
                }
                HorizontalDivider(color = CardBorder)
                ToggleRow(Icons.Filled.Medication, "Recordatorios de desparasitación", "Te avisamos cuando toque desparasitar.", desparasitacion) {
                    desparasitacion = it
                    onUpdateProfile(null, null, null, it, null, null, null, null, null)
                }
                HorizontalDivider(color = CardBorder)
                ToggleRow(Icons.Filled.MedicalServices, "Recordatorios de citas", "Te avisamos de tus próximas citas.", citas) {
                    citas = it
                    onUpdateProfile(null, null, null, null, it, null, null, null, null)
                }
                HorizontalDivider(color = CardBorder)
                ToggleRow(Icons.Filled.Groups, "Notificaciones al compartir una mascota", "Te avisamos cuando te dan acceso.", compartida) {
                    compartida = it
                    onUpdateProfile(null, null, null, null, null, it, null, null, null)
                }
                HorizontalDivider(color = CardBorder)
                ToggleRow(Icons.Filled.NotificationsActive, "Notificaciones de documentos", "Te avisamos cuando hay nuevos documentos.", documentoSubido) {
                    documentoSubido = it
                    onUpdateProfile(null, null, null, null, null, null, it, null, null)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            SectionTitle("Métodos de aviso")
            Text(text = "Elige cómo deseas recibir las notificaciones.", color = SubtitleGray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(10.dp))
            AccountCard {
                ToggleRow(Icons.Filled.Sms, "Notificaciones push", "Avisos dentro de la aplicación.", avisoPush) {
                    avisoPush = it
                    onUpdateProfile(null, null, null, null, null, null, null, it, null)
                }
                HorizontalDivider(color = CardBorder)
                ToggleRow(Icons.Filled.Email, "Notificaciones por correo", "Avisos enviados a tu email.", avisoEmail) {
                    avisoEmail = it
                    onUpdateProfile(null, null, null, null, null, null, null, null, it)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    when (activeDialog) {
        AccountField.NOMBRE -> AccountEditDialog(
            title = "Nombre completo",
            initialValue = user?.fullName.orEmpty(),
            isLoading = updateState is UpdateUserUiState.Loading,
            onConfirm = { onUpdateProfile(it, null, null, null, null, null, null, null, null) },
            onDismiss = { activeDialog = null },
        )
        AccountField.TELEFONO -> AccountEditDialog(
            title = "Teléfono",
            initialValue = user?.phoneNumber.orEmpty(),
            keyboardType = KeyboardType.Phone,
            isLoading = updateState is UpdateUserUiState.Loading,
            onConfirm = { onUpdateProfile(null, it, null, null, null, null, null, null, null) },
            onDismiss = { activeDialog = null },
        )
        null -> Unit
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = BrandGreen,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Start
    )
}

@Composable
private fun AccountCard(content: @Composable ColumnScope.() -> Unit) {
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
private fun AccountRow(icon: ImageVector, title: String, value: String, onClick: (() -> Unit)?) {
    Row(
        modifier = (if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(text = value, color = SubtitleGray, fontSize = 13.sp)
        }
        if (onClick != null) {
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = SubtitleGray)
        }
    }
}

@Composable
private fun ToggleRow(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(text = subtitle, color = SubtitleGray, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = BrandGreen),
        )
    }
}

@Composable
private fun AccountEditDialog(
    title: String,
    initialValue: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isLoading: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = !isLoading) {
                Text(if (isLoading) "Guardando…" else "Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

private fun initialsFor(name: String?): String {
    if (name.isNullOrBlank()) return "?"
    val parts = name.trim().split(Regex("\\s+"))
    return parts.take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
}

@Preview(showBackground = true)
@Composable
private fun MiCuentaContentPreview() {
    val sampleUser = User(
        id = "1",
        fullName = "Juan Pérez",
        email = "juan.perez@example.com",
        phoneNumber = "+34 600 000 000"
    )
    PetProjectTheme {
        MiCuentaContent(
            user = sampleUser,
            updateState = UpdateUserUiState.Idle,
            onBack = {},
            onUpdateProfile = { _, _, _, _, _, _, _, _, _ -> },
            resetUpdateState = {}
        )
    }
}
