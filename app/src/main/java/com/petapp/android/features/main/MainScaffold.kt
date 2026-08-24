package com.petapp.android.features.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petapp.android.R
import com.petapp.android.core.model.Pet
import com.petapp.android.features.account.MiCuentaScreen
import com.petapp.android.features.account.PdfViewerScreen
import com.petapp.android.features.account.PrivacidadScreen
import com.petapp.android.features.consultations.RegistrarConsultaScreen
import com.petapp.android.features.deworming.RegistrarDesparasitacionScreen
import com.petapp.android.features.files.SubirArchivoScreen
import com.petapp.android.features.incidents.RegistrarIncidenciaScreen
import com.petapp.android.features.pets.GestionarPetScreen
import com.petapp.android.features.pets.PetDetailScreen
import com.petapp.android.features.pets.PetsUiState
import com.petapp.android.features.pets.PetsViewModel
import com.petapp.android.features.pets.RegisterPetScreen
import com.petapp.android.features.reminders.AnadirRecordatorioScreen
import com.petapp.android.features.sharing.CompartirMascotaScreen
import com.petapp.android.features.vaccines.RegistrarVacunaScreen
import kotlinx.coroutines.launch

private val BrandGreen = Color(0xFF406E5F)

private enum class MainTab {
    INICIO,
    ACTIVIDAD,
    MAS,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(onLoggedOut: () -> Unit) {
    val petsViewModel: PetsViewModel = viewModel()
    val userViewModel: UserViewModel = viewModel()

    // Both ViewModels are Activity-scoped and outlive a single login session, so a
    // fresh fetch is needed on every entry into Main rather than relying on init{}
    // (which only ran once, possibly for a previous account).
    LaunchedEffect(Unit) {
        petsViewModel.fetchPets()
        userViewModel.loadUser()
    }

    val petsState by petsViewModel.uiState.collectAsState()
    val selectedPetId by petsViewModel.selectedPetId.collectAsState()
    val userState by userViewModel.uiState.collectAsState()

    val pets = (petsState as? PetsUiState.Loaded)?.pets.orEmpty()
    val selectedPet = pets.firstOrNull { it.id == selectedPetId }
    val userFullName = (userState as? UserUiState.Loaded)?.user?.fullName

    var currentTab by remember { mutableStateOf(MainTab.INICIO) }
    var showAddPet by remember { mutableStateOf(false) }
    var showPetSwitcher by remember { mutableStateOf(false) }
    var showMoreOptions by remember { mutableStateOf(false) }
    var showGestionarPets by remember { mutableStateOf(false) }
    var showRegistrarIncidencia by remember { mutableStateOf(false) }
    var showAnadirVacuna by remember { mutableStateOf(false) }
    var showAnadirDesparasitacion by remember { mutableStateOf(false) }
    var showRegistrarConsulta by remember { mutableStateOf(false) }
    var showSubirArchivo by remember { mutableStateOf(false) }
    var showAnadirRecordatorio by remember { mutableStateOf(false) }
    var showCompartirMascota by remember { mutableStateOf(false) }
    var showMiCuenta by remember { mutableStateOf(false) }
    var showAjustes by remember { mutableStateOf(false) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    var showTerms by remember { mutableStateOf(false) }
    var petDetail by remember { mutableStateOf<Pet?>(null) }

    val currentPetDetail = petDetail
    when {
        currentPetDetail != null -> {
            PetDetailScreen(
                pet = currentPetDetail,
                onBack = { petDetail = null },
                viewModel = petsViewModel,
            )
            return
        }
        showAddPet -> {
            RegisterPetScreen(
                onDone = { showAddPet = false },
                onSkip = { showAddPet = false },
                viewModel = petsViewModel,
            )
            return
        }
        showRegistrarIncidencia -> {
            RegistrarIncidenciaScreen(
                selectedPet = selectedPet,
                userFullName = userFullName,
                onBack = { showRegistrarIncidencia = false },
                onFinish = { showRegistrarIncidencia = false },
                onViewActivity = {
                    showRegistrarIncidencia = false
                    currentTab = MainTab.ACTIVIDAD
                },
            )
            return
        }
        showAnadirVacuna -> {
            RegistrarVacunaScreen(
                selectedPet = selectedPet,
                userFullName = userFullName,
                onBack = { showAnadirVacuna = false },
                onFinish = { showAnadirVacuna = false },
                onViewActivity = {
                    showAnadirVacuna = false
                    currentTab = MainTab.ACTIVIDAD
                },
            )
            return
        }
        showAnadirDesparasitacion -> {
            RegistrarDesparasitacionScreen(
                selectedPet = selectedPet,
                userFullName = userFullName,
                onBack = { showAnadirDesparasitacion = false },
                onFinish = { showAnadirDesparasitacion = false },
                onViewActivity = {
                    showAnadirDesparasitacion = false
                    currentTab = MainTab.ACTIVIDAD
                },
            )
            return
        }
        showRegistrarConsulta -> {
            RegistrarConsultaScreen(
                selectedPet = selectedPet,
                userFullName = userFullName,
                onBack = { showRegistrarConsulta = false },
                onFinish = { showRegistrarConsulta = false },
                onViewActivity = {
                    showRegistrarConsulta = false
                    currentTab = MainTab.ACTIVIDAD
                },
            )
            return
        }
        showSubirArchivo -> {
            SubirArchivoScreen(
                selectedPet = selectedPet,
                userFullName = userFullName,
                onBack = { showSubirArchivo = false },
                onDone = { showSubirArchivo = false },
            )
            return
        }
        showAnadirRecordatorio -> {
            AnadirRecordatorioScreen(
                selectedPet = selectedPet,
                userFullName = userFullName,
                onBack = { showAnadirRecordatorio = false },
                onFinish = { showAnadirRecordatorio = false },
                onViewActivity = {
                    showAnadirRecordatorio = false
                    currentTab = MainTab.ACTIVIDAD
                },
            )
            return
        }
        showCompartirMascota -> {
            CompartirMascotaScreen(
                selectedPet = selectedPet,
                userFullName = userFullName,
                onBack = { showCompartirMascota = false },
                onFinish = { showCompartirMascota = false },
            )
            return
        }
        showPrivacyPolicy -> {
            PdfViewerScreen(
                title = "Política de privacidad",
                rawResId = R.raw.privacy_policy,
                onBack = { showPrivacyPolicy = false },
            )
            return
        }
        showTerms -> {
            PdfViewerScreen(
                title = "Términos y condiciones",
                rawResId = R.raw.terms_and_conditions,
                onBack = { showTerms = false },
            )
            return
        }
        showAjustes -> {
            PrivacidadScreen(
                onBack = { showAjustes = false },
                onOpenPrivacyPolicy = { showPrivacyPolicy = true },
                onOpenTerms = { showTerms = true },
                onAccountDeleted = onLoggedOut,
                viewModel = userViewModel,
            )
            return
        }
        showMiCuenta -> {
            MiCuentaScreen(
                onBack = { showMiCuenta = false },
                viewModel = userViewModel,
            )
            return
        }
        showGestionarPets -> {
            GestionarPetScreen(
                pets = pets,
                selectedPetId = selectedPetId,
                onBack = { showGestionarPets = false },
                onAddPet = { showAddPet = true },
                onOpenPetDetail = { petDetail = it },
            )
            return
        }
    }

    if (showPetSwitcher) {
        val sheetState = rememberModalBottomSheetState()
        val scope = rememberCoroutineScope()
        fun dismiss() {
            scope.launch { sheetState.hide() }.invokeOnCompletion { showPetSwitcher = false }
        }
        PetSwitcherSheet(
            pets = pets,
            selectedPetId = selectedPetId,
            onSelectPet = { pet ->
                petsViewModel.selectPet(pet.id)
                dismiss()
            },
            onAddPet = {
                dismiss()
                showAddPet = true
            },
            onDismiss = { dismiss() },
            sheetState = sheetState,
        )
    }

    if (showMoreOptions) {
        val sheetState = rememberModalBottomSheetState()
        val scope = rememberCoroutineScope()
        fun dismiss() {
            scope.launch { sheetState.hide() }.invokeOnCompletion { showMoreOptions = false }
        }
        MoreOptionsSheet(
            onDismiss = { dismiss() },
            sheetState = sheetState,
            onAddPet = {
                dismiss()
                showAddPet = true
            },
            onRegistrarIncidencia = {
                dismiss()
                showRegistrarIncidencia = true
            },
            onAnadirVacuna = {
                dismiss()
                showAnadirVacuna = true
            },
            onAnadirDesparasitacion = {
                dismiss()
                showAnadirDesparasitacion = true
            },
            onAnadirRecordatorio = {
                dismiss()
                showAnadirRecordatorio = true
            },
            onCompartirMascota = {
                dismiss()
                showCompartirMascota = true
            },
            onRegistrarConsulta = {
                dismiss()
                showRegistrarConsulta = true
            },
            onSubirArchivo = {
                dismiss()
                showSubirArchivo = true
            },
        )
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    selected = currentTab == MainTab.INICIO,
                    onClick = { currentTab = MainTab.INICIO },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text("Inicio") },
                    colors = mainNavColors(),
                )
                NavigationBarItem(
                    selected = currentTab == MainTab.ACTIVIDAD,
                    onClick = { currentTab = MainTab.ACTIVIDAD },
                    icon = { Icon(Icons.Filled.Pets, contentDescription = null) },
                    label = { Text("Actividad") },
                    colors = mainNavColors(),
                )
                NavigationBarItem(
                    selected = currentTab == MainTab.MAS,
                    onClick = { currentTab = MainTab.MAS },
                    icon = { Icon(Icons.Filled.MoreHoriz, contentDescription = null) },
                    label = { Text("Más") },
                    colors = mainNavColors(),
                )
            }
        },
    ) { paddingValues ->
        val contentModifier = Modifier.padding(paddingValues)
        when (currentTab) {
            MainTab.INICIO -> InicioTab(
                pets = pets,
                selectedPet = selectedPet,
                userFullName = userFullName,
                onSwitchPetClick = { showPetSwitcher = true },
                onAddPetClick = { showAddPet = true },
                onMoreClick = { showMoreOptions = true },
                onAnadirVacuna = { showAnadirVacuna = true },
                onAnadirDesparasitacion = { showAnadirDesparasitacion = true },
                onRegistrarConsulta = { showRegistrarConsulta = true },
                onRegistrarIncidencia = { showRegistrarIncidencia = true },
                onAnadirRecordatorio = { showAnadirRecordatorio = true },
                onCapturarDocumento = { showSubirArchivo = true },
                modifier = contentModifier,
            )
            MainTab.ACTIVIDAD -> ActividadTab(
                pets = pets,
                selectedPet = selectedPet,
                userFullName = userFullName,
                onSwitchPetClick = { showPetSwitcher = true },
                onMoreClick = { showMoreOptions = true },
                modifier = contentModifier,
            )
            MainTab.MAS -> MasTab(
                pets = pets,
                selectedPet = selectedPet,
                userFullName = userFullName,
                onSwitchPetClick = { showPetSwitcher = true },
                onGestionarPetsClick = { showGestionarPets = true },
                onMiCuentaClick = { showMiCuenta = true },
                onAjustesClick = { showAjustes = true },
                onLogout = { userViewModel.logout(onLoggedOut) },
                modifier = contentModifier,
            )
        }
    }
}

@Composable
private fun mainNavColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = BrandGreen,
    selectedTextColor = BrandGreen,
    indicatorColor = Color(0xFFD9FEF2),
)
