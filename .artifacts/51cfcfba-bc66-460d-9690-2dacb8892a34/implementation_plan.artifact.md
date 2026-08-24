# Implementation Plan - Preview for RegistrarDesparasitacionScreen

This plan outlines the steps to add a `@Preview` for `RegistrarDesparasitacionScreen`. To achieve this, the Composable will be refactored to separate the `ViewModel` dependency, allowing for a stateless version that can be easily previewed.

## Proposed Changes

### Deworming Feature

#### [MODIFY] [RegistrarDesparasitacionScreen.kt](file:///Users/giancarlo/Documents/Projects/PET_PROJECT/android/PetProjectAPP/app/src/main/java/com/petapp/android/features/deworming/RegistrarDesparasitacionScreen.kt)
- Add necessary imports for `@Preview` and `PetProjectTheme`.
- Extract `RegistrarDesparasitacionScreen` logic into a stateless `RegistrarDesparasitacionContent` Composable.
- Update the original `RegistrarDesparasitacionScreen` to collect `ViewModel` state and pass it to `RegistrarDesparasitacionContent`.
- Refactor `DesparasitacionFormContent` to be stateless by taking state and callbacks instead of a `ViewModel` instance.
- Add `RegistrarDesparasitacionScreenPreview` at the end of the file using sample data.

## Verification Plan

### Automated Tests
- Run `analyze_current_file` to ensure no syntax errors.
- Run `render_compose_preview` for `RegistrarDesparasitacionScreenPreview` to verify the UI.

### Manual Verification
- Verify that the screen still works as expected in the app (if possible, though the prompt focus is on the preview).
