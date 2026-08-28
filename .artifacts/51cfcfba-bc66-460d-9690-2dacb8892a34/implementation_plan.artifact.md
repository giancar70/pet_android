# Implementation Plan - Fix Logout Navigation

The goal is to fix an issue where confirming the logout dialog does not navigate the user back to the Login screen. We will make the logout process immediate by clearing the token and triggering the navigation callback before the network call, ensuring the UI transition is not delayed or blocked.

## User Review Required

> [!NOTE]
> The logout transition will now be immediate. The server-side logout call will still be attempted in the background, but it won't block the user from returning to the Login screen.

## Proposed Changes

### Main Feature

#### [MODIFY] [UserViewModel.kt](file:///Users/giancarlo/Documents/Projects/PET_PROJECT/android/PetProjectAPP/app/src/main/java/com/petapp/android/features/main/UserViewModel.kt)
- Update the `logout` function to clear the `TokenStore.token` and call `onComplete()` immediately.
- Move the `ApiClient.postEmpty(ApiEndpoints.LOGOUT)` call to the background within the `viewModelScope`.

#### [MODIFY] [MainScaffold.kt](file:///Users/giancarlo/Documents/Projects/PET_PROJECT/android/PetProjectAPP/app/src/main/java/com/petapp/android/features/main/MainScaffold.kt)
- Simplify the `onLogout` lambda passed to `MasTab` to ensure `onLoggedOut()` is called as soon as the session is cleared.

## Verification Plan

### Automated Tests
- Run `analyze_file` on `UserViewModel.kt` and `MainScaffold.kt` to ensure no syntax errors.

### Manual Verification
- Log in to the app.
- Go to the "Más" tab.
- Tap on "Cerrar sesión".
- Tap "Confirmar" in the dialog.
- Verify that the app immediately navigates to the Login screen.
