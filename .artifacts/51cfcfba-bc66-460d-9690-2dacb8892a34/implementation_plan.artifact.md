# Implementation Plan - Fix Logout Routing to Register Pet

The user reported that logging out takes them to the "Crear Pet" (RegisterPet) screen instead of the "Login" screen. This is likely caused by stale state in Activity-scoped ViewModels (especially `AuthViewModel`), which triggers immediate navigation when the Login screen is re-composed after logout.

## User Review Required

> [!IMPORTANT]
> This change will reset the authentication and pets state upon logout. This ensures that a fresh login or logout doesn't accidentally trigger navigation based on the previous user's session data.

## Proposed Changes

### Auth Feature

#### [MODIFY] [AuthViewModel.kt](file:///Users/giancarlo/Documents/Projects/PET_PROJECT/android/PetProjectAPP/app/src/main/java/com/petapp/android/features/auth/AuthViewModel.kt)
- Add a `reset()` function to set `uiState` back to `Idle`.

#### [MODIFY] [LoginScreen.kt](file:///Users/giancarlo/Documents/Projects/PET_PROJECT/android/PetProjectAPP/app/src/main/java/com/petapp/android/features/auth/LoginScreen.kt)
- Call `viewModel.reset()` in a `LaunchedEffect(Unit)` to ensure the screen starts in a clean state.

#### [MODIFY] [RegisterScreen.kt](file:///Users/giancarlo/Documents/Projects/PET_PROJECT/android/PetProjectAPP/app/src/main/java/com/petapp/android/features/auth/RegisterScreen.kt)
- Call `viewModel.reset()` in a `LaunchedEffect(Unit)` to ensure the screen starts in a clean state.

### Pets Feature

#### [MODIFY] [PetsViewModel.kt](file:///Users/giancarlo/Documents/Projects/PET_PROJECT/android/PetProjectAPP/app/src/main/java/com/petapp/android/features/pets/PetsViewModel.kt)
- Add a `clearState()` function to reset `uiState` and `selectedPetId`.

### Main Feature

#### [MODIFY] [UserViewModel.kt](file:///Users/giancarlo/Documents/Projects/PET_PROJECT/android/PetProjectAPP/app/src/main/java/com/petapp/android/features/main/UserViewModel.kt)
- Add a `reset()` function to reset `uiState`, `updateState`, and `deleteAccountState`.

#### [MODIFY] [MainScaffold.kt](file:///Users/giancarlo/Documents/Projects/PET_PROJECT/android/PetProjectAPP/app/src/main/java/com/petapp/android/features/main/MainScaffold.kt)
- In the `onLogout` callback, call `petsViewModel.clearState()` and `userViewModel.reset()` before (or during) the logout process to ensure all shared state is cleared.

## Verification Plan

### Automated Tests
- Run `analyze_file` on all modified files to ensure no syntax errors.

### Manual Verification
- Log in to the app.
- Go to the "Más" tab and log out.
- Verify the app goes directly to the Login screen.
- Verify that it doesn't flash the "Crear mascota" screen or automatically navigate to it.
