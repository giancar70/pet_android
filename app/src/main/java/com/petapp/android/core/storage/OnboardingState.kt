package com.petapp.android.core.storage

import android.content.Context
import android.content.SharedPreferences

/**
 * Tracks whether the user has ever gotten past the Onboarding screen, independent of
 * whether they currently have an auth token -- used by `MainActivity` to distinguish
 * "first launch ever" (show Onboarding) from "logged out but has seen it before" (skip
 * straight to Login), mirroring iOS's equivalent `UserDefaults`-backed flag.
 */
object OnboardingState {
    private const val PREFS_NAME = "petapp_onboarding_prefs"
    private const val KEY_HAS_COMPLETED = "has_completed_onboarding"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    val hasCompleted: Boolean
        get() = prefs.getBoolean(KEY_HAS_COMPLETED, false)

    fun markCompleted() {
        prefs.edit().putBoolean(KEY_HAS_COMPLETED, true).apply()
    }
}
