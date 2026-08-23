package com.petapp.android.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateReminderRequest(
    val category: String,
    val title: String,
    @SerialName("due_date") val dueDate: String,
    val frequency: String = "none",
    @SerialName("custom_days") val customDays: Int? = null,
    @SerialName("notify_push") val notifyPush: Boolean = true,
    @SerialName("notify_email") val notifyEmail: Boolean = false,
)

@Serializable
data class Reminder(
    val id: String,
    val category: String,
    val title: String,
    @SerialName("due_date") val dueDate: String,
    val frequency: String,
    @SerialName("custom_days") val customDays: Int? = null,
    @SerialName("notify_push") val notifyPush: Boolean = true,
    @SerialName("notify_email") val notifyEmail: Boolean = false,
    val status: String,
    @SerialName("created_at") val createdAt: String,
)

enum class ReminderCategory(val apiValue: String, val label: String) {
    VACCINE("vaccine", "Vacuna"),
    DEWORMING("deworming", "Desparasitación"),
    ANTIPARASITIC("antiparasitic", "Antiparasitario"),
    MEDICATION("medication", "Medicación"),
    CONSULTATION("consultation", "Consulta"),
    SURGERY("surgery", "Cirugía"),
    TEST("test", "Analítica"),
    DIET("diet", "Dieta/Alimentación"),
    WEIGHT("weight", "Peso/Control"),
    OTHER("other", "Otro"),
}

enum class ReminderFrequency(val apiValue: String, val label: String) {
    NONE("none", "No se repite"),
    WEEKLY("weekly", "Semanal"),
    MONTHLY("monthly", "Mensual"),
    YEARLY("yearly", "Anual"),
    CUSTOM_DAYS("custom_days", "Cada N días"),
}
