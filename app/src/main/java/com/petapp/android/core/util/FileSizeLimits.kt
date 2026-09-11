package com.petapp.android.core.util

/// Mirrors the backend's PetSerializer.MAX_IMAGE_SIZE_MB / DocumentSerializer.
/// MAX_FILE_SIZE_MB -- checked client-side too so an oversized file is rejected
/// instantly instead of after a wasted upload round-trip. The backend remains the
/// authoritative check either way.
object FileSizeLimits {
    const val MAX_IMAGE_MB = 10
    const val MAX_DOCUMENT_MB = 20

    private const val BYTES_PER_MB = 1024 * 1024
    const val MAX_IMAGE_BYTES = MAX_IMAGE_MB * BYTES_PER_MB
    const val MAX_DOCUMENT_BYTES = MAX_DOCUMENT_MB * BYTES_PER_MB

    fun imageTooLargeMessage() = "La foto no puede superar los $MAX_IMAGE_MB MB."
    fun documentTooLargeMessage() = "El documento no puede superar los $MAX_DOCUMENT_MB MB."
}
