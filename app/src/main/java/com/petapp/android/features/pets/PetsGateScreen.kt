package com.petapp.android.features.pets

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petapp.android.R
import com.petapp.android.core.model.User
import com.petapp.android.core.network.ApiClient
import com.petapp.android.core.network.ApiEndpoints
import com.petapp.android.core.notifications.PushTokenManager
import kotlinx.coroutines.launch

private val BrandGreen = Color(0xFF406E5F)

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
    //
    // Looks up the user's last-selected pet first (synced across devices/logins by
    // PetsViewModel.selectPet()) and passes it as a hint, so returning to the app --
    // whether after a logout/login or on a different phone -- resumes on the same
    // pet instead of always defaulting to the first one in the list. Best-effort: a
    // failure here just falls back to fetchPets()'s existing default selection.
    LaunchedEffect(Unit) {
        // Fire-and-forget alongside the pets fetch below rather than awaited -- this
        // registers (or refreshes) this device's push token for recordatorio
        // notifications every time the app reaches this gate (cold start, post-login,
        // post-register), which is the same point Android's own FCM token can change.
        launch { PushTokenManager.registerCurrentToken() }
        val lastSelectedPet = runCatching { ApiClient.get<User>(ApiEndpoints.USER) }.getOrNull()?.lastSelectedPet
        viewModel.fetchPets(selectPetId = lastSelectedPet)
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.splash_logo),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.width(180.dp),
            )
            Spacer(modifier = Modifier.height(32.dp))
            CircularProgressIndicator(color = BrandGreen)
        }
    }
}
