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
