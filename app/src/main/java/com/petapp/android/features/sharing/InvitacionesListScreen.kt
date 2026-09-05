package com.petapp.android.features.sharing

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petapp.android.core.model.PetShare
import com.petapp.android.core.model.PetShareRole

private val BrandGreen = Color(0xFF406E5F)
private val SubtitleGray = Color(0xFF666666)
private val ContentBackground = Color(0xFFE3FBF1)
private val CardBorder = Color(0xFFEFEFF4)
private val PendingText = Color(0xFFB4552B)
private val DeleteRed = Color(0xFFC0392B)

@Composable
fun InvitacionesListScreen(
    onBack: () -> Unit,
    viewModel: SharingViewModel = viewModel(),
) {
    val listState by viewModel.allSharesState.collectAsState()
    val deleteState by viewModel.deleteState.collectAsState()
    var petFilter by remember { mutableStateOf<String?>(null) }
    var activeShare by remember { mutableStateOf<PetShare?>(null) }
    var showRoleDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.fetchAllShares()
        viewModel.resetDeleteState()
    }
    LaunchedEffect(deleteState) {
        if (deleteState is DeleteShareUiState.Success) {
            viewModel.resetDeleteState()
            activeShare = null
        }
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
            Text(text = "Invitaciones", color = BrandGreen, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Gestiona quién tiene acceso a tus mascotas.",
                color = SubtitleGray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(20.dp))

            when (val state = listState) {
                is SharesListUiState.Loading -> Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BrandGreen)
                }
                is SharesListUiState.Error -> Text(text = state.message, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                is SharesListUiState.Loaded -> {
                    val pets = state.shares.map { it.pet to it.petName }.distinct()
                    if (pets.size > 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(label = "Todos", selected = petFilter == null, onClick = { petFilter = null })
                            pets.forEach { (petId, petName) ->
                                FilterChip(label = petName, selected = petFilter == petId, onClick = { petFilter = petId })
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    val visible = state.shares.filter { petFilter == null || it.pet == petFilter }
                    if (visible.isEmpty()) {
                        Text(
                            text = "Aún no has compartido ninguna mascota.",
                            color = SubtitleGray,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 24.dp),
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                        ) {
                            visible.forEach { share ->
                                InvitacionRow(
                                    share = share,
                                    onClick = {
                                        activeShare = share
                                        showRoleDialog = false
                                        showDeleteDialog = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val current = activeShare
    if (current != null && !showRoleDialog && !showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { activeShare = null },
            title = { Text(current.userFullName.ifBlank { current.userEmail }) },
            text = {
                Column {
                    Text(text = "${current.petName} · ${roleLabel(current.role)}", color = SubtitleGray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showRoleDialog = true }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Cambiar permiso", fontSize = 15.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showDeleteDialog = true }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, tint = DeleteRed, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Eliminar invitación", color = DeleteRed, fontSize = 15.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { activeShare = null }) { Text("Cerrar", color = BrandGreen) }
            },
        )
    }

    if (current != null && showRoleDialog) {
        AlertDialog(
            onDismissRequest = { showRoleDialog = false },
            title = { Text("Cambiar permiso") },
            text = {
                Column {
                    PetShareRole.entries.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showRoleDialog = false
                                    activeShare = null
                                    viewModel.updateShareRole(current.id, option.apiValue)
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (current.role == option.apiValue) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                            } else {
                                Spacer(modifier = Modifier.width(26.dp))
                            }
                            Text(option.label, fontSize = 15.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRoleDialog = false; activeShare = null }) { Text("Cancelar", color = BrandGreen) }
            },
        )
    }

    if (current != null && showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar invitación") },
            text = { Text("¿Estás seguro de que deseas quitarle el acceso a ${current.userFullName.ifBlank { current.userEmail }}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteShare(current.id)
                    },
                ) { Text("Eliminar", color = DeleteRed) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false; activeShare = null }) { Text("Cancelar", color = BrandGreen) }
            },
        )
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) BrandGreen else Color.White,
        border = BorderStroke(1.dp, if (selected) BrandGreen else CardBorder),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else Color.Black,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun InvitacionRow(share: PetShare, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).background(Color(0xFFD9FEF2), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                val source = share.userFullName.ifBlank { share.userEmail }
                Text(
                    text = source.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    color = BrandGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = share.userFullName.ifBlank { share.userEmail }, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(text = share.petName, color = SubtitleGray, fontSize = 12.sp)
                if (share.status == "pending") {
                    Text(text = "Pendiente", color = PendingText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
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
