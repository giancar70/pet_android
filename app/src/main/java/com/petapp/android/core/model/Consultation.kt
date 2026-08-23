package com.petapp.android.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateConsultationRequest(
    val reason: String,
    @SerialName("consult_date") val consultDate: String,
    val symptoms: String? = null,
    @SerialName("physical_exam_findings") val physicalExamFindings: String? = null,
    val diagnosis: String? = null,
    val treatment: String? = null,
    @SerialName("clinic_name") val clinicName: String? = null,
)

@Serializable
data class Consultation(
    val id: String,
    val reason: String,
    @SerialName("consult_date") val consultDate: String,
    val symptoms: String? = null,
    @SerialName("physical_exam_findings") val physicalExamFindings: String? = null,
    val diagnosis: String? = null,
    val treatment: String? = null,
    @SerialName("clinic_name") val clinicName: String? = null,
    @SerialName("created_at") val createdAt: String,
)
