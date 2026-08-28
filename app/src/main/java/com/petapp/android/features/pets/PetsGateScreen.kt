package com.petapp.android.features.pets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun PetsGateScreen(
    onHasPets: () -> Unit,
    onNoPets: () -> Unit,
    viewModel: PetsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    // PetsViewModel is Activity-scoped and outlives a single login session (no
    // Navigation-Compose to give this screen its own fresh instance), so its list
    // from a previous account would otherwise still be sitting in uiState. Force a
    // fresh fetch every time this gate is entered rather than trusting init{}.
    LaunchedEffect(Unit) {
        viewModel.fetchPets()
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is PetsUiState.Loaded -> if (state.pets.isEmpty()) onNoPets() else onHasPets()
            is PetsUiState.Error -> {
                // If we get an error here, it's likely a 401 (token expired/cleared).
                // We shouldn't proceed to Main; staying here is fine as the top-level
                // app state should eventually react to the auth failure or the user
                // will be sent back to Login by the auth gate in MainActivity.
                // For now, we just don't navigate to Main.
            }
            PetsUiState.Loading -> Unit
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
