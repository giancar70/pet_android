package com.petapp.android.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateDewormingApplicationRequest(
    @SerialName("deworming_type") val dewormingType: String,
    @SerialName("applied_on") val appliedOn: String,
    @SerialName("next_due_on") val nextDueOn: String? = null,
    @SerialName("duration_months") val durationMonths: Int? = null,
    @SerialName("product_name") val productName: String? = null,
    val notes: String? = null,
)

@Serializable
data class DewormingApplication(
    val id: String,
    @SerialName("applied_on") val appliedOn: String,
    @SerialName("next_due_on") val nextDueOn: String? = null,
    @SerialName("duration_months") val durationMonths: Int? = null,
    @SerialName("product_name") val productName: String? = null,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String,
)

enum class DewormingType(val apiValue: String, val label: String) {
    INTERNAL("internal", "Interna"),
    EXTERNAL("external", "Externa"),
    MIXED("mixed", "Mixta"),
}
