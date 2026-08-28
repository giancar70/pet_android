package com.petapp.android.core.network

import com.petapp.android.core.storage.TokenStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class ApiError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class ServerError(val code: Int, val errorMessage: String) : ApiError(errorMessage)
    data object Unauthorized : ApiError("Session expired. Please log in again.")
    class NetworkError(cause: Throwable) : ApiError("Network error. Check your connection.", cause)
    class DecodingError(cause: Throwable) : ApiError("Unexpected server response.", cause)
}

@Serializable
@PublishedApi
internal data class ErrorBody(val error: String)

object ApiClient {
    @PublishedApi
    internal val json = Json { ignoreUnknownKeys = true }

    @PublishedApi
    internal val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    @PublishedApi
    internal val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend inline fun <reified T> get(path: String): T =
        execute(newRequest(path).get().build())

    suspend inline fun <reified TRequest, reified TResponse> post(path: String, body: TRequest): TResponse {
        val requestBody = json.encodeToString(body).toRequestBody(jsonMediaType)
        return execute(newRequest(path).post(requestBody).build())
    }

    suspend inline fun <reified TRequest, reified TResponse> patch(path: String, body: TRequest): TResponse {
        val requestBody = json.encodeToString(body).toRequestBody(jsonMediaType)
        return execute(newRequest(path).patch(requestBody).build())
    }

    suspend inline fun <reified T> postMultipart(
        path: String,
        fields: Map<String, String>,
        imageBytes: ByteArray?,
        imageMimeType: String = "image/jpeg",
        imageFieldName: String = "image",
    ): T {
        val bodyBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
        for ((key, value) in fields) {
            bodyBuilder.addFormDataPart(key, value)
        }
        if (imageBytes != null) {
            bodyBuilder.addFormDataPart(
                imageFieldName,
                "pet.jpg",
                imageBytes.toRequestBody(imageMimeType.toMediaType()),
            )
        }
        return execute(newRequest(path).post(bodyBuilder.build()).build())
    }

    suspend inline fun <reified T> postMultipartFile(
        path: String,
        fields: Map<String, String>,
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String,
        fileFieldName: String = "file",
    ): T {
        val bodyBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
        for ((key, value) in fields) {
            bodyBuilder.addFormDataPart(key, value)
        }
        bodyBuilder.addFormDataPart(
            fileFieldName,
            fileName,
            fileBytes.toRequestBody(mimeType.toMediaType()),
        )
        return execute(newRequest(path).post(bodyBuilder.build()).build())
    }

    suspend fun postEmpty(path: String, tokenOverride: String? = null) {
        val empty = ByteArray(0).toRequestBody(null)
        val (code, bodyStr) = withContext(Dispatchers.IO) { runRequest(newRequest(path, tokenOverride).post(empty).build()) }
        if (code !in 200..299) throw serverErrorFor(code, bodyStr)
    }

    suspend fun delete(path: String) {
        val (code, bodyStr) = withContext(Dispatchers.IO) { runRequest(newRequest(path).delete().build()) }
        if (code !in 200..299) throw serverErrorFor(code, bodyStr)
    }

    @PublishedApi
    internal fun newRequest(path: String, tokenOverride: String? = null): Request.Builder {
        val builder = Request.Builder().url(ApiEndpoints.BASE_URL + path)
        val token = tokenOverride ?: TokenStore.token
        token?.let { builder.addHeader("Authorization", "Token $it") }
        return builder
    }

    @PublishedApi
    internal suspend inline fun <reified T> execute(request: Request): T {
        val (code, bodyStr) = withContext(Dispatchers.IO) { runRequest(request) }
        if (code !in 200..299) throw serverErrorFor(code, bodyStr)
        return try {
            json.decodeFromString(bodyStr)
        } catch (e: Exception) {
            throw ApiError.DecodingError(e)
        }
    }

    @PublishedApi
    internal fun serverErrorFor(code: Int, bodyStr: String): ApiError {
        if (code == 401) return ApiError.Unauthorized
        val message = try {
            json.decodeFromString<ErrorBody>(bodyStr).error
        } catch (e: Exception) {
            "Something went wrong."
        }
        return ApiError.ServerError(code, message)
    }

    @PublishedApi
    internal fun runRequest(request: Request): Pair<Int, String> = try {
        client.newCall(request).execute().use { response ->
            response.code to (response.body?.string().orEmpty())
        }
    } catch (e: IOException) {
        throw ApiError.NetworkError(e)
    }
}
