# Fix Logout Navigation and State Persistence

The user is experiencing an issue where logging out does not return to the login view, or it returns and immediately loops back into the main view with an error. This is caused by state persistence in activity-scoped ViewModels and a race condition in the login flow.

## Proposed Changes

### [Component] Authentication & User Session

#### [MODIFY] [UserViewModel.kt](file:///Users/giancarlo/Documents/Projects/PET_PROJECT/android/PetProjectAPP/app/src/main/java/com/petapp/android/features/main/UserViewModel.kt)
- Improve `logout` to be immediate and resilient.
- Clear the local token immediately so the UI can navigate away without waiting for the network.
- Capture the token before clearing it to ensure the server-side logout request is sent with the correct credentials.

#### [MODIFY] [AuthViewModel.kt](file:///Users/giancarlo/Documents/Projects/PET_PROJECT/android/PetProjectAPP/app/src/main/java/com/petapp/android/features/auth/AuthViewModel.kt)
- Ensure `reset()` is thorough and clears any pending `Success` states.

### [Component] Navigation & UI

#### [MODIFY] [MainScaffold.kt](file:///Users/giancarlo/Documents/Projects/PET_PROJECT/android/PetProjectAPP/app/src/main/java/com/petapp/android/features/main/MainScaffold.kt)
- Update the `onLogout` lambda to also reset the `AuthViewModel` state, preventing the automatic re-login loop.

#### [MODIFY] [PetsGateScreen.kt](file:///Users/giancarlo/Documents/Projects/PET_PROJECT/android/PetProjectAPP/app/src/main/java/com/petapp/android/features/pets/PetsGateScreen.kt)
- Fix the error handling logic. If fetching pets fails (e.g., due to a 401 Unauthorized), it should not assume the user has pets and proceed to `Main`. Instead, it should handle the error (which will typically lead back to Login via a session check or manual navigation).

## Verification Plan

### Manual Verification
1. Open the app and go to the "Más" (More) tab.
2. Tap "Cerrar sesión" (Logout) and confirm.
3. Verify that the app immediately navigates to the Login screen.
4. Verify that the app does NOT automatically log you back in.
5. Verify that trying to login again works as expected.
