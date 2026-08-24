package com.petapp.android.core.network

object ApiEndpoints {
    // Android emulator alias for host localhost; change this one constant when the
    // backend's reachable address changes (mirrors APIEndpoint.baseURL in iOS).
    const val BASE_URL = "https://pet-api-production-a168.up.railway.app/api"

    const val REGISTER = "/auth/register/"
    const val LOGIN = "/auth/login/"
    const val LOGOUT = "/auth/logout/"
    const val USER = "/auth/user/"
    const val PETS = "/pets/"

    fun petDetail(id: String) = "/pets/$id/"
    fun petEvents(petId: String) = "/pets/$petId/events/"
    fun petEventDetail(petId: String, eventId: String) = "/pets/$petId/events/$eventId/"
    fun petVaccineDoses(petId: String) = "/pets/$petId/vaccine-doses/"
    fun petVaccineDoseDetail(petId: String, doseId: String) = "/pets/$petId/vaccine-doses/$doseId/"
    fun petDewormingApplications(petId: String) = "/pets/$petId/deworming-applications/"
    fun petDewormingApplicationDetail(petId: String, applicationId: String) =
        "/pets/$petId/deworming-applications/$applicationId/"
    fun petReminders(petId: String) = "/pets/$petId/reminders/"
    fun petShare(petId: String) = "/pets/$petId/share/"
    fun petConsultations(petId: String) = "/pets/$petId/consultations/"
    fun petConsultationDetail(petId: String, consultationId: String) = "/pets/$petId/consultations/$consultationId/"
    fun petDocuments(petId: String) = "/pets/$petId/documents/"
    fun petDocumentDetail(petId: String, documentId: String) = "/pets/$petId/documents/$documentId/"
    fun petEventDocuments(petId: String, eventId: String) = "/pets/$petId/documents/?event=$eventId"
}
