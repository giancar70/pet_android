package com.petdrive.app.core.model

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
    @SerialName("ai_extraction_status") val aiExtractionStatus: String? = null,
    @SerialName("ai_json_result") val aiJsonResult: List<DetectedVaccine>? = null,
    @SerialName("document_date") val documentDate: String? = null,
    @SerialName("created_at") val createdAt: String,
)

// POST documents/analyze-cartilla/ -- pet info + vaccines + deworming found on a
// not-yet-saved Cartilla/Pasaporte scan (apps/files/document_analysis.py
// analyze_cartilla). Nothing is persisted; the client shows this for review.
@Serializable
data class CartillaAnalysis(
    @SerialName("pet_info") val petInfo: PetInfoAnalysis = PetInfoAnalysis(),
    val vaccines: List<DetectedVaccine> = emptyList(),
    val dewormings: List<DewormingAnalysis> = emptyList(),
    val analyzed: Boolean = false,
)

@Serializable
data class PetInfoAnalysis(
    val name: String? = null,
    val species: String? = null,
    val breed: String? = null,
    val sex: String? = null,
    @SerialName("birth_date") val birthDate: String? = null,
    @SerialName("weight_kg") val weightKg: Double? = null,
    val microchip: String? = null,
)

@Serializable
data class DewormingAnalysis(
    @SerialName("deworming_type") val dewormingType: String? = null,
    @SerialName("product_name") val productName: String? = null,
    @SerialName("applied_on") val appliedOn: String? = null,
    @SerialName("next_due_on") val nextDueOn: String? = null,
    @SerialName("duration_months") val durationMonths: Int? = null,
    @SerialName("lot_number") val lotNumber: String? = null,
)

// POST documents/analyze/ -- classification + date + vaccines found on a not-yet-saved
// scan (apps/files/document_analysis.py). analyzed=false means the AI was unavailable or
// its answer unusable, so the values are just defaults for the user to correct.
@Serializable
data class DocumentAnalysis(
    @SerialName("document_type") val documentType: String = "other",
    @SerialName("document_date") val documentDate: String? = null,
    val vaccines: List<DetectedVaccine> = emptyList(),
    val analyzed: Boolean = false,
)

// Shape returned in Document.aiJsonResult when document_type is vaccine_card -- see
// apps/files/vaccine_extraction.py's VACCINE_EXTRACTION_PROMPT for the exact contract.
@Serializable
data class DetectedVaccine(
    @SerialName("vaccine_name") val vaccineName: String? = null,
    @SerialName("applied_on") val appliedOn: String? = null,
    @SerialName("next_due_on") val nextDueOn: String? = null,
    @SerialName("lot_number") val lotNumber: String? = null,
)

enum class DocumentTypeOption(val apiValue: String, val label: String) {
    CLINIC("clinic", "Clínica"),
    INVOICE("invoice", "Factura"),
    LABWORK("labwork", "Laboratorio"),
    PRESCRIPTION("prescription", "Receta"),
    VACCINE_CARD("vaccine_card", "Cartilla de vacunas (IA)"),
    OTHER("other", "Otros"),
}
