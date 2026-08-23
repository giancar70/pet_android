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
