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
            is PetsUiState.Error -> onHasPets()
            PetsUiState.Loading -> Unit
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
