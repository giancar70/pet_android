package com.petdrive.app.features.pets

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petdrive.app.R
import com.petdrive.app.core.model.User
import com.petdrive.app.core.network.ApiClient
import com.petdrive.app.core.network.ApiEndpoints
import com.petdrive.app.core.notifications.PushTokenManager
import com.petdrive.app.core.storage.TokenStore
import kotlinx.coroutines.launch

private val BrandGreen = Color(0xFF406E5F)

@Composable
fun PetsGateScreen(
    onHasPets: () -> Unit,
    onNoPets: () -> Unit,
    onUnauthorized: () -> Unit,
    viewModel: PetsViewModel = viewModel(),
) {
    // PetsViewModel is Activity-scoped (no Navigation-Compose to give this screen its own
    // fresh instance), so uiState can still be holding a terminal value -- e.g.
    // Unauthorized -- left over from an earlier, unrelated visit to this gate (a stale
    // token from a previous session, a transient error, ...). Reset it synchronously here,
    // during composition and before any effect below runs, so LaunchedEffect(uiState)'s
    // very first collection can't act on that leftover value instead of a fresh fetch --
    // otherwise a stale Unauthorized gets acted on (clearing a token this gate's own fresh
    // login just issued, and bouncing straight back to Login) before fetchPets() below ever
    // gets a chance to run.
    remember { viewModel.resetToLoading() }
    val uiState by viewModel.uiState.collectAsState()
    // Remembered so "Reintentar" can retry with the same hint rather than losing it --
    // the /auth/user/ lookup that produces it only needs to run once per gate entry.
    var selectPetIdHint by remember { mutableStateOf<String?>(null) }

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
        selectPetIdHint = runCatching { ApiClient.get<User>(ApiEndpoints.USER) }.getOrNull()?.lastSelectedPet
        viewModel.fetchPets(selectPetId = selectPetIdHint)
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is PetsUiState.Loaded -> if (state.pets.isEmpty()) onNoPets() else onHasPets()
            is PetsUiState.Error -> Unit // shown inline below, with a retry -- see the Box content.
            is PetsUiState.Unauthorized -> {
                TokenStore.token = null
                onUnauthorized()
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
            when (val state = uiState) {
                is PetsUiState.Error -> {
                    // A transient/server error here (e.g. a network blip) used to leave
                    // the user stuck on a bare spinner forever, with no indication
                    // anything had gone wrong and no way out short of force-quitting the
                    // app. Now offers an actual retry, plus a way back to Login if
                    // retrying doesn't help.
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { viewModel.fetchPets(selectPetId = selectPetIdHint) },
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                    ) {
                        Text(text = "Reintentar", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = {
                        TokenStore.token = null
                        onUnauthorized()
                    }) {
                        Text(text = "Cerrar sesión", color = BrandGreen)
                    }
                }
                else -> CircularProgressIndicator(color = BrandGreen)
            }
        }
    }
}
