package com.petapp.android.core.storage

import android.content.Context
import android.content.SharedPreferences

object PetPreferences {
    private const val PREFS_NAME = "petapp_pet_prefs"
    private const val KEY_SELECTED_PET_ID = "selected_pet_id"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var selectedPetId: String?
        get() = prefs.getString(KEY_SELECTED_PET_ID, null)
        set(value) {
            prefs.edit().putString(KEY_SELECTED_PET_ID, value).apply()
        }
}
