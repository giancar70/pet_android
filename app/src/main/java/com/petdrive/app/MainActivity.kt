package com.petdrive.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petdrive.app.core.model.User
import com.petdrive.app.core.network.ApiClient
import com.petdrive.app.core.network.ApiEndpoints
import com.petdrive.app.core.notifications.PushTokenManager
import com.petdrive.app.core.storage.OnboardingState
import com.petdrive.app.core.storage.TokenStore
import com.petdrive.app.features.auth.ForgotPasswordScreen
import com.petdrive.app.features.auth.LoginScreen
import com.petdrive.app.features.auth.RegisterScreen
import com.petdrive.app.features.auth.ResetPasswordScreen
import com.petdrive.app.features.main.MainScaffold
import com.petdrive.app.features.onboarding.OnboardingScreen
import com.petdrive.app.features.pets.PetsGateScreen
import com.petdrive.app.features.pets.PetsViewModel
import com.petdrive.app.features.pets.RegisterPetScreen
import com.petdrive.app.ui.theme.PetProjectTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed interface AppScreen {
    data object Onboarding : AppScreen
    data object Login : AppScreen
    data object Register : AppScreen
    data object ForgotPassword : AppScreen
    data class ResetPassword(val email: String) : AppScreen
    data object CheckingPets : AppScreen
    data object RegisterPet : AppScreen
    data object Main : AppScreen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        // Holds the system splash on screen for a beat, matching the previous branding
        // hold time -- but as a *single* splash instead of the system splash handing off
        // to a second, separately-drawn Compose splash screen right after it.
        var keepSplashOnScreen = true
        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }
        super.onCreate(savedInstanceState)
        setContent {
            PetProjectTheme {
                var screen by remember { mutableStateOf<AppScreen?>(null) }
                val petsViewModel: PetsViewModel = viewModel()
                LaunchedEffect(Unit) {
                    delay(1500)
                    // Mirrors the app's 3 possible post-splash states: (A) first launch
                    // ever -> Onboarding; (B) onboarding already seen but logged out ->
                    // straight to Login, skipping Onboarding; (C) a saved token exists ->
                    // resolve pets right here, still hidden behind the splash, instead of
                    // handing off to CheckingPets' own visible loading screen -- otherwise
                    // cold start reads as two separate views (splash, then a second screen
                    // with its own logo + spinner) rather than one continuous screen.
                    screen = if (TokenStore.token != null) {
                        launch { PushTokenManager.registerCurrentToken() }
                        val lastSelectedPet = runCatching { ApiClient.get<User>(ApiEndpoints.USER) }.getOrNull()?.lastSelectedPet
                        val pets = petsViewModel.fetchPetsAndAwait(selectPetId = lastSelectedPet)
                        when {
                            pets == null -> AppScreen.Login
                            pets.isEmpty() -> AppScreen.RegisterPet
                            else -> AppScreen.Main
                        }
                    } else if (OnboardingState.hasCompleted) {
                        AppScreen.Login
                    } else {
                        AppScreen.Onboarding
                    }
                    keepSplashOnScreen = false
                }
                val resolvedScreen = screen ?: return@PetProjectTheme
                // Every transition crossfades rather than hard-cutting, matching the same
                // "smooth transition" request that governs the splash hold itself.
                Crossfade(targetState = resolvedScreen, animationSpec = tween(300), label = "AppScreen") { current ->
                    when (current) {
                        AppScreen.Onboarding -> OnboardingScreen(
                            onGetStarted = {
                                OnboardingState.markCompleted()
                                screen = AppScreen.Login
                            },
                        )
                        AppScreen.Login -> {
                            LoginScreen(
                                onLoginSuccess = { screen = AppScreen.CheckingPets },
                                onNavigateToRegister = { screen = AppScreen.Register },
                                onNavigateToForgotPassword = { screen = AppScreen.ForgotPassword },
                            )
                        }
                        AppScreen.Register -> {
                            BackHandler { screen = AppScreen.Login }
                            RegisterScreen(
                                onRegisterSuccess = { screen = AppScreen.CheckingPets },
                                onNavigateToLogin = { screen = AppScreen.Login },
                            )
                        }
                        AppScreen.ForgotPassword -> {
                            ForgotPasswordScreen(
                                onBack = { screen = AppScreen.Login },
                                onCodeSent = { email -> screen = AppScreen.ResetPassword(email) },
                            )
                        }
                        is AppScreen.ResetPassword -> {
                            ResetPasswordScreen(
                                email = current.email,
                                onBack = { screen = AppScreen.ForgotPassword },
                                onResetSuccess = { screen = AppScreen.CheckingPets },
                            )
                        }
                        AppScreen.CheckingPets -> PetsGateScreen(
                            onHasPets = { screen = AppScreen.Main },
                            onNoPets = { screen = AppScreen.RegisterPet },
                        )
                        AppScreen.RegisterPet -> RegisterPetScreen(
                            onDone = { screen = AppScreen.Main },
                            onSkip = { screen = AppScreen.Main },
                        )
                        AppScreen.Main -> MainScaffold(
                            // Onboarding is guaranteed already completed by the time any user
                            // reaches Main (the only path here goes through it), so logout
                            // returns straight to Login rather than showing Onboarding again.
                            onLoggedOut = { screen = AppScreen.Login },
                        )
                    }
                }
            }
        }
    }
}
