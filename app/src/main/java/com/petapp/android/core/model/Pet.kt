package com.petapp.android.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Pet(
    val id: String,
    val name: String,
    val image: String? = null,
    val species: String = "other",
    val breed: String? = null,
    val sex: String = "unknown",
    @SerialName("birth_date") val birthDate: String? = null,
    @SerialName("weight_kg") val weightKg: String? = null,
    val color: String? = null,
    val microchip: String? = null,
    val notes: String? = null,
    // "owner", or the share's role ("family"/"caregiver"/"veterinary") for a pet
    // shared with the current user. canEdit/canUploadDocuments mirror the
    // backend's PetUserAccess flags -- both default true so pets fetched before
    // this field existed (shouldn't happen post-rollout, but keeps old cached
    // data safe) don't accidentally lock the owner out of their own pet.
    val role: String? = null,
    @SerialName("can_edit") val canEdit: Boolean = true,
    @SerialName("can_upload_documents") val canUploadDocuments: Boolean = true,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class UpdatePetRequest(
    val name: String,
    val species: String,
    val breed: String? = null,
    val sex: String,
    @SerialName("birth_date") val birthDate: String? = null,
    @SerialName("weight_kg") val weightKg: String? = null,
    val color: String? = null,
    val microchip: String? = null,
    val notes: String? = null,
)

@Serializable
data class Breed(
    val id: String,
    val species: String,
    val name: String,
)

enum class PetSpecies(val apiValue: String, val label: String) {
    DOG("dog", "Perro"),
    CAT("cat", "Gato"),
    OTHER("other", "otro"),
}

enum class PetSex(val apiValue: String, val label: String) {
    MALE("M", "Macho"),
    FEMALE("F", "Hembra"),
    UNKNOWN("unknown", "Desconocido"),
}

fun petSpeciesLabel(species: String): String =
    PetSpecies.entries.find { it.apiValue == species }?.label?.replaceFirstChar(Char::uppercase) ?: "Otro"

fun petSexLabel(sex: String): String =
    PetSex.entries.find { it.apiValue == sex }?.label ?: PetSex.UNKNOWN.label
