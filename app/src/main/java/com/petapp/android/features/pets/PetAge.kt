package com.petapp.android.features.pets

import java.time.LocalDate
import java.time.Period

fun formatPetAge(birthDateIso: String?): String? {
    if (birthDateIso.isNullOrBlank()) return null
    return try {
        val birthDate = LocalDate.parse(birthDateIso)
        val today = LocalDate.now()
        if (birthDate.isAfter(today)) return null
        val period = Period.between(birthDate, today)
        "${period.years}año ${period.months}meses"
    } catch (e: Exception) {
        null
    }
}
