package com.petapp.android.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateVaccineDoseRequest(
    @SerialName("vaccine_name") val vaccineName: String,
    @SerialName("applied_on") val appliedOn: String,
    @SerialName("lot_number") val lotNumber: String? = null,
    @SerialName("clinic_name") val clinicName: String? = null,
    val notes: String? = null,
)

@Serializable
data class VaccineDose(
    val id: String,
    val vaccine: String,
    @SerialName("applied_on") val appliedOn: String,
    @SerialName("lot_number") val lotNumber: String? = null,
    @SerialName("clinic_name") val clinicName: String? = null,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String,
)
