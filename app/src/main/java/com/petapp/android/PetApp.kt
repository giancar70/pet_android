package com.petapp.android

import android.app.Application
import com.petapp.android.core.storage.OnboardingState
import com.petapp.android.core.storage.TokenStore

class PetApp : Application() {
    override fun onCreate() {
        super.onCreate()
        TokenStore.init(applicationContext)
        OnboardingState.init(applicationContext)
    }
}
