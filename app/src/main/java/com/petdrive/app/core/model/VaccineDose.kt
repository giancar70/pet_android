package com.petdrive.app.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateVaccineDoseRequest(
    @SerialName("vaccine_name") val vaccineName: String,
    @SerialName("applied_on") val appliedOn: String,
    @SerialName("next_due_on") val nextDueOn: String? = null,
    @SerialName("lot_number") val lotNumber: String? = null,
    val notes: String? = null,
)

// Same per-vaccine shape as CreateVaccineDoseRequest, reused as-is for
// VaccineDoseBulkCreateFromDocumentView's request body (apps/pet/views.py).
@Serializable
data class RegisterVaccinesFromDocumentRequest(val vaccines: List<CreateVaccineDoseRequest>)

// PATCH vaccine-doses/<id>/ -- full edit of an existing dose in place (VacunaDetailScreen's
// "Editar", reusing RegistrarVacunaScreen pre-filled), as opposed to "Registrar nueva
// aplicación" which creates a brand new dose via CreateVaccineDoseRequest and leaves this
// one untouched. Every field is optional here (unlike on create) since this is also used
// for the narrower edits a PATCH allows.
@Serializable
data class UpdateVaccineDoseRequest(
    @SerialName("vaccine_name") val vaccineName: String? = null,
    @SerialName("applied_on") val appliedOn: String? = null,
    @SerialName("next_due_on") val nextDueOn: String? = null,
    @SerialName("lot_number") val lotNumber: String? = null,
    val notes: String? = null,
)

@Serializable
data class VaccineDose(
    val id: String,
    val vaccine: String,
    @SerialName("applied_on") val appliedOn: String,
    @SerialName("next_due_on") val nextDueOn: String? = null,
    @SerialName("lot_number") val lotNumber: String? = null,
    val notes: String? = null,
    val status: String = "active",
    @SerialName("created_at") val createdAt: String,
)
