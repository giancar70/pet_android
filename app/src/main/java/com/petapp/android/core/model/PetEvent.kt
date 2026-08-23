package com.petapp.android.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreatePetEventRequest(
    val title: String,
    val description: String? = null,
    @SerialName("event_date") val eventDate: String,
    @SerialName("event_type") val eventType: String? = null,
    @SerialName("issuer_name") val issuerName: String? = null,
    @SerialName("issuer_type") val issuerType: String? = null,
)

@Serializable
data class PetEvent(
    val id: String,
    @SerialName("event_type") val eventType: String,
    @SerialName("event_date") val eventDate: String,
    val title: String,
    val description: String? = null,
    @SerialName("issuer_name") val issuerName: String? = null,
    @SerialName("issuer_type") val issuerType: String? = null,
    @SerialName("created_at") val createdAt: String,
)
