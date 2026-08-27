# Implementation Plan - Fix Consultation Feature (CU12)

This plan addresses several issues in the veterinarian consultation feature, including nomenclature changes, UI contrast improvements, keyboard accessibility, and state management fixes.

## Proposed Changes

### Consultations Feature

#### [MODIFY] [RegistrarConsultaScreen.kt](file:///Users/giancarlo/Documents/Projects/PET_PROJECT/android/PetProjectAPP/app/src/main/java/com/petapp/android/features/consultations/RegistrarConsultaScreen.kt)
- Update nomenclature: Change "Fecha de aplicación*" to "Fecha de consulta*".
- Improve contrast:
    - Update `SubtitleGray` to `#666666`.
    - Add `TextDark` (`#333333`) and `PlaceholderGray` (`#8A8A8A`).
    - Apply these colors to labels, placeholders, and main text.
- Keyboard accessibility: Ensure the `Column` with `verticalScroll` handles the IME correctly.
- Ensure "Guardar consulta" button is always enabled unless the required field ("Motivo") is empty or a save is in progress.

#### [MODIFY] [ConsultaDetailScreen.kt](file:///Users/giancarlo/Documents/Projects/PET_PROJECT/android/PetProjectAPP/app/src/main/java/com/petapp/android/features/consultations/ConsultaDetailScreen.kt)
- Update nomenclature: Change "Fecha" label to "Fecha de consulta".
- Fix infinite loading: Improve the `Error` state to include a "Reintentar" button.
- Improve contrast: Apply `TextDark` and `SubtitleGray` as per standards.

#### [MODIFY] [ConsultationsViewModel.kt](file:///Users/giancarlo/Documents/Projects/PET_PROJECT/android/PetProjectAPP/app/src/main/java/com/petapp/android/features/consultations/ConsultationsViewModel.kt)
- Prevent unnecessary loading states: Update `fetchConsultas` to only set `ConsultasListUiState.Loading` if the current state is not already `Loaded`.

### Other ViewModels (for consistency with CU06)

#### [MODIFY] [VaccinesViewModel.kt](file:///Users/giancarlo/Documents/Projects/PET_PROJECT/android/PetProjectAPP/app/src/main/java/com/petapp/android/features/vaccines/VaccinesViewModel.kt)
- Update `fetchVacunas` to prevent unnecessary loading states.

#### [MODIFY] [DewormingViewModel.kt](file:///Users/giancarlo/Documents/Projects/PET_PROJECT/android/PetProjectAPP/app/src/main/java/com/petapp/android/features/deworming/DewormingViewModel.kt)
- Update `fetchDesparasitaciones` to prevent unnecessary loading states.

#### [MODIFY] [IncidentsViewModel.kt](file:///Users/giancarlo/Documents/Projects/PET_PROJECT/android/PetProjectAPP/app/src/main/java/com/petapp/android/features/incidents/IncidentsViewModel.kt)
- Update `fetchIncidencias` to prevent unnecessary loading states.

#### [MODIFY] [FilesViewModel.kt](file:///Users/giancarlo/Documents/Projects/PET_PROJECT/android/PetProjectAPP/app/src/main/java/com/petapp/android/features/files/FilesViewModel.kt)
- Update `fetchDocuments` to prevent unnecessary loading states.

## Verification Plan

### Automated Tests
- Run `analyze_file` on all modified files.

### Manual Verification
- **Formulario**: Verify "Fecha de consulta" label. Check contrast of labels and placeholders.
- **Teclado**: Verify that the form can be scrolled to "Guardar consulta" when the keyboard is open.
- **Detalle**: Verify that an error in loading the detail shows a "Reintentar" button instead of a loader.
- **Actividad**: Navigate to a consultation detail and back. Verify that the Activity timeline does not flash a loading state.
