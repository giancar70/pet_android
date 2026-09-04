package com.petapp.android.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Document(
    val id: String,
    val event: String? = null,
    @SerialName("event_title") val eventTitle: String? = null,
    @SerialName("document_type") val documentType: String? = null,
    val title: String? = null,
    val file: String? = null,
    @SerialName("mime_type") val mimeType: String? = null,
    @SerialName("ocr_status") val ocrStatus: String? = null,
    @SerialName("ocr_text") val ocrText: String? = null,
    @SerialName("document_date") val documentDate: String? = null,
    @SerialName("created_at") val createdAt: String,
)

enum class DocumentTypeOption(val apiValue: String, val label: String) {
    CLINIC("clinic", "Clínica"),
    INVOICE("invoice", "Factura"),
    LABWORK("labwork", "Laboratorio"),
    PRESCRIPTION("prescription", "Receta"),
    OTHER("other", "Otros"),
}
