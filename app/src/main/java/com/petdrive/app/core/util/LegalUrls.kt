package com.petdrive.app.core.util

import android.content.Context
import android.content.Intent
import android.net.Uri

object LegalUrls {
    const val TERMS = "https://petdrive.es/condiciones-de-uso/"
    const val PRIVACY_POLICY = "https://petdrive.es/politica-de-privacidad/"
}

fun openUrl(context: Context, url: String) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}
