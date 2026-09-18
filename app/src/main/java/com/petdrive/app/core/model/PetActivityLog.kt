package com.petdrive.app.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PetActivityLog(
    val id: String,
    val action: String,
    @SerialName("resource_type") val resourceType: String,
    val description: String,
    @SerialName("user_full_name") val userFullName: String? = null,
    @SerialName("created_at") val createdAt: String,
)
