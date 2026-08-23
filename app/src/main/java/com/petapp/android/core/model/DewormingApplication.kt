package com.petapp.android.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateDewormingApplicationRequest(
    @SerialName("deworming_type") val dewormingType: String,
    @SerialName("applied_on") val appliedOn: String,
    @SerialName("administration_route") val administrationRoute: String? = null,
    val presentation: String? = null,
    @SerialName("product_name") val productName: String? = null,
    @SerialName("weight_kg_at_application") val weightKgAtApplication: String? = null,
    val notes: String? = null,
)

@Serializable
data class DewormingApplication(
    val id: String,
    @SerialName("applied_on") val appliedOn: String,
    @SerialName("administration_route") val administrationRoute: String? = null,
    val presentation: String? = null,
    @SerialName("product_name") val productName: String? = null,
    @SerialName("weight_kg_at_application") val weightKgAtApplication: String? = null,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String,
)

enum class DewormingType(val apiValue: String, val label: String) {
    INTERNAL("internal", "Interna"),
    EXTERNAL("external", "Externa"),
    MIXED("mixed", "Mixta"),
}

enum class AdministrationRoute(val apiValue: String, val label: String) {
    ORAL("oral", "Oral"),
    TOPICAL("topical", "Tópica"),
    INJECTABLE("injectable", "Inyectable"),
    COLLAR("collar", "Collar"),
    OTHER("other", "Otro"),
}

enum class DewormingPresentation(val apiValue: String, val label: String) {
    TABLET("tablet", "Comprimido"),
    PIPETTE("pipette", "Pipeta"),
    SPRAY("spray", "Spray"),
    COLLAR("collar", "Collar"),
    SYRUP("syrup", "Jarabe"),
    OTHER("other", "Otro"),
}
