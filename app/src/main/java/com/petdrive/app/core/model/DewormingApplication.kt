package com.petdrive.app.core.model

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

// Same per-application shape as CreateDewormingApplicationRequest, reused as-is for
// DewormingApplicationBulkCreateFromDocumentView's request body (apps/pet/views.py).
@Serializable
data class RegisterDewormingsFromDocumentRequest(val dewormings: List<CreateDewormingApplicationRequest>)

// PATCH deworming-applications/<id>/ -- full edit of an existing application in place
// (DesparasitacionDetailScreen's "Editar", reusing RegistrarDesparasitacionScreen
// pre-filled), as opposed to "Registrar nueva aplicación" which creates a brand new
// application via CreateDewormingApplicationRequest and leaves this one untouched. Every
// field is optional here (unlike on create).
@Serializable
data class UpdateDewormingApplicationRequest(
    @SerialName("deworming_type") val dewormingType: String? = null,
    @SerialName("applied_on") val appliedOn: String? = null,
    @SerialName("next_due_on") val nextDueOn: String? = null,
    @SerialName("duration_months") val durationMonths: Int? = null,
    @SerialName("product_name") val productName: String? = null,
    val notes: String? = null,
)

@Serializable
data class DewormingApplication(
    val id: String,
    // Was write-only server-side until this session's full-edit work, so older cached
    // responses in this app's local state may predate it -- nullable/defaulted rather
    // than assumed always present.
    @SerialName("deworming_type") val dewormingType: String? = null,
    @SerialName("applied_on") val appliedOn: String,
    @SerialName("next_due_on") val nextDueOn: String? = null,
    @SerialName("duration_months") val durationMonths: Int? = null,
    @SerialName("product_name") val productName: String? = null,
    val notes: String? = null,
    val status: String = "active",
    @SerialName("created_at") val createdAt: String,
)

enum class DewormingType(val apiValue: String, val label: String) {
    INTERNAL("internal", "Interna"),
    EXTERNAL("external", "Externa"),
    MIXED("mixed", "Mixta"),
}
