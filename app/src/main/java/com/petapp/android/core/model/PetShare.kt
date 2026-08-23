package com.petapp.android.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreatePetShareRequest(
    val email: String,
    val role: String,
)

@Serializable
data class PetShare(
    val id: String,
    val role: String,
    @SerialName("can_view") val canView: Boolean,
    @SerialName("can_edit") val canEdit: Boolean,
    @SerialName("can_upload_documents") val canUploadDocuments: Boolean,
    @SerialName("user_email") val userEmail: String,
    @SerialName("user_full_name") val userFullName: String,
    @SerialName("created_at") val createdAt: String,
)

enum class PetShareRole(val apiValue: String, val label: String) {
    FAMILY("family", "Colaborador"),
    CAREGIVER("caregiver", "Solo lectura"),
    VETERINARY("veterinary", "Veterinario"),
}
