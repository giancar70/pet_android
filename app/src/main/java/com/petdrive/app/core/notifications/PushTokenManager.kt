package com.petdrive.app.core.notifications

import com.google.android.gms.tasks.Tasks
import com.google.firebase.messaging.FirebaseMessaging
import com.petdrive.app.core.network.ApiClient
import com.petdrive.app.core.network.ApiEndpoints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

// platform has no default: ApiClient's Json is built with the kotlinx.serialization
// default encodeDefaults = false, which silently omits any field left at its default
// value -- a default here would drop `platform` from the request body entirely, and
// the backend's DeviceTokenSerializer requires it, so every registration would 400.
@Serializable
private data class DeviceTokenRequest(val token: String, val platform: String)

/// Registers/unregisters this device's FCM token for recordatorio push notifications
/// (see the backend's apps/notifications/push.py). Every call here is best-effort and
/// swallows its own errors -- a failed registration just means this device doesn't get
/// pushes until the next successful one, never something surfaced to the user.
object PushTokenManager {
    suspend fun registerCurrentToken() {
        val token = fetchToken() ?: return
        registerToken(token)
    }

    suspend fun registerToken(token: String) {
        runCatching { ApiClient.postForStatus(ApiEndpoints.DEVICE_TOKEN, DeviceTokenRequest(token = token, platform = "android")) }
    }

    // tokenOverride mirrors UserViewModel.logout()'s pattern for the LOGOUT call: the
    // auth token is already cleared from TokenStore by the time this runs, so it has
    // to be passed in explicitly to still authenticate the unregister request.
    suspend fun unregisterCurrentToken(tokenOverride: String?) {
        val token = fetchToken() ?: return
        runCatching { ApiClient.delete(ApiEndpoints.deviceTokenDelete(token), tokenOverride) }
    }

    private suspend fun fetchToken(): String? = withContext(Dispatchers.IO) {
        runCatching { Tasks.await(FirebaseMessaging.getInstance().token) }.getOrNull()
    }
}
