package com.petapp.android.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    @SerialName("full_name") val fullName: String,
    val email: String,
    @SerialName("phone_number") val phoneNumber: String? = null,
    val country: String? = null,
    val city: String? = null,
    @SerialName("notify_vaccine_reminders") val notifyVaccineReminders: Boolean = true,
    @SerialName("notify_deworming_reminders") val notifyDewormingReminders: Boolean = true,
    @SerialName("notify_appointment_reminders") val notifyAppointmentReminders: Boolean = true,
    @SerialName("notify_pet_shared") val notifyPetShared: Boolean = true,
    @SerialName("notify_document_uploaded") val notifyDocumentUploaded: Boolean = true,
    @SerialName("notify_via_push") val notifyViaPush: Boolean = true,
    @SerialName("notify_via_email") val notifyViaEmail: Boolean = true,
)

@Serializable
data class UpdateUserRequest(
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("phone_number") val phoneNumber: String? = null,
    @SerialName("notify_vaccine_reminders") val notifyVaccineReminders: Boolean? = null,
    @SerialName("notify_deworming_reminders") val notifyDewormingReminders: Boolean? = null,
    @SerialName("notify_appointment_reminders") val notifyAppointmentReminders: Boolean? = null,
    @SerialName("notify_pet_shared") val notifyPetShared: Boolean? = null,
    @SerialName("notify_document_uploaded") val notifyDocumentUploaded: Boolean? = null,
    @SerialName("notify_via_push") val notifyViaPush: Boolean? = null,
    @SerialName("notify_via_email") val notifyViaEmail: Boolean? = null,
)

@Serializable
data class AuthResponse(
    val token: String,
    val user: User,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class RegisterRequest(
    @SerialName("full_name") val fullName: String,
    val email: String,
    val password: String,
)
