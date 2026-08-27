package com.petapp.android

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
import com.petapp.android.core.storage.OnboardingState
import com.petapp.android.core.storage.TokenStore
import com.petapp.android.features.auth.LoginScreen
import com.petapp.android.features.auth.RegisterScreen
import com.petapp.android.features.main.MainScaffold
import com.petapp.android.features.onboarding.OnboardingScreen
import com.petapp.android.features.pets.PetsGateScreen
import com.petapp.android.features.pets.RegisterPetScreen
import com.petapp.android.features.splash.SplashScreen
import com.petapp.android.ui.theme.PetProjectTheme

sealed interface AppScreen {
    data object Splash : AppScreen
    data object Onboarding : AppScreen
    data object Login : AppScreen
    data object Register : AppScreen
    data object CheckingPets : AppScreen
    data object RegisterPet : AppScreen
    data object Main : AppScreen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PetProjectTheme {
                var screen by remember { mutableStateOf<AppScreen>(AppScreen.Splash) }
                // Every transition crossfades rather than hard-cutting, matching the same
                // "smooth transition" request that governs the splash hold itself.
                Crossfade(targetState = screen, animationSpec = tween(300), label = "AppScreen") { current ->
                    when (current) {
                        AppScreen.Splash -> SplashScreen(
                            onTimeout = {
                                // Mirrors the app's 3 possible post-splash states: (A) first
                                // launch ever -> Onboarding; (B) onboarding already seen but
                                // logged out -> straight to Login, skipping Onboarding; (C) a
                                // saved token exists -> the existing pets-check -> Main flow.
                                screen = when {
                                    TokenStore.token != null -> AppScreen.CheckingPets
                                    OnboardingState.hasCompleted -> AppScreen.Login
                                    else -> AppScreen.Onboarding
                                }
                            },
                        )
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
                            )
                        }
                        AppScreen.Register -> {
                            BackHandler { screen = AppScreen.Login }
                            RegisterScreen(
                                onRegisterSuccess = { screen = AppScreen.CheckingPets },
                                onNavigateToLogin = { screen = AppScreen.Login },
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
