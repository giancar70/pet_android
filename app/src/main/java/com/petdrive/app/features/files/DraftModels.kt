package com.petdrive.app.features.files

// Plain draft values passed into RegistrarVacunaScreen/RegistrarDesparasitacionScreen/
// PetDetailScreen's draft mode and returned back to RevisarInformacionDetectadaScreen
// (paso5) on save -- nothing here ever touches the network directly; only paso5's
// "Guardar registros" does, via the real CreateVaccineDoseRequest/
// CreateDewormingApplicationRequest/UpdatePetRequest.

data class VaccineDraft(
    val vaccineName: String,
    val appliedOn: String,
    val nextDueOn: String? = null,
    val lotNumber: String? = null,
    val notes: String? = null,
)

data class DewormingDraft(
    val dewormingType: String,
    val productName: String? = null,
    val appliedOn: String,
    val nextDueOn: String? = null,
    val durationMonths: Int? = null,
    val notes: String? = null,
)

data class PetInfoDraft(
    val name: String,
    val species: String,
    val breed: String? = null,
    val sex: String,
    val birthDate: String? = null,
    val weightKg: String? = null,
    val color: String? = null,
    val microchip: String? = null,
    val notes: String? = null,
)
