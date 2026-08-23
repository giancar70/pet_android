package com.petapp.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
                when (screen) {
                    AppScreen.Splash -> SplashScreen(
                        onTimeout = {
                            screen = if (TokenStore.token != null) AppScreen.CheckingPets else AppScreen.Onboarding
                        },
                    )
                    AppScreen.Onboarding -> OnboardingScreen(
                        onGetStarted = { screen = AppScreen.Login },
                    )
                    AppScreen.Login -> LoginScreen(
                        onLoginSuccess = { screen = AppScreen.CheckingPets },
                        onNavigateToRegister = { screen = AppScreen.Register },
                    )
                    AppScreen.Register -> RegisterScreen(
                        onRegisterSuccess = { screen = AppScreen.CheckingPets },
                        onNavigateToLogin = { screen = AppScreen.Login },
                    )
                    AppScreen.CheckingPets -> PetsGateScreen(
                        onHasPets = { screen = AppScreen.Main },
                        onNoPets = { screen = AppScreen.RegisterPet },
                    )
                    AppScreen.RegisterPet -> RegisterPetScreen(
                        onDone = { screen = AppScreen.Main },
                        onSkip = { screen = AppScreen.Main },
                    )
                    AppScreen.Main -> MainScaffold(
                        onLoggedOut = { screen = AppScreen.Onboarding },
                    )
                }
            }
        }
    }
}
