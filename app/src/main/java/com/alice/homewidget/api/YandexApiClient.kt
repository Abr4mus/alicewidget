package com.alice.homewidget.api

import com.alice.homewidget.model.UserInfoResponse
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class YandexApiClient {

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val BASE_URL = "https://api.iot.yandex.net/v1.0"
        const val ENDPOINT_USER_INFO = "$BASE_URL/user/info"
    }

    suspend fun fetchUserInfo(token: String): Result<UserInfoResponse> = withContext(Dispatchers.IO) {
        if (token.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("OAuth-токен Яндекса не указан"))
        }

        val request = Request.Builder()
            .url(ENDPOINT_USER_INFO)
            .addHeader("Authorization", "Bearer ${token.trim()}")
            .addHeader("Accept", "application/json")
            .get()
            .build()

        try {
            val response = client.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = when (response.code) {
                    401 -> "Неверный или просроченный OAuth токен (401 Unauthorized)"
                    403 -> "Доступ запрещен: убедитесь, что приложению выданы права iot:view (403 Forbidden)"
                    404 -> "Учетная запись умного дома не найдена (404 Not Found)"
                    429 -> "Слишком много запросов к серверу Яндекса (429 Rate Limit)"
                    else -> "Ошибка сервера Яндекса: HTTP ${response.code}"
                }
                return@withContext Result.failure(IOException(errorMsg))
            }

            val parsed = gson.fromJson(bodyString, UserInfoResponse::class.java)
            if (parsed == null) {
                return@withContext Result.failure(IOException("Не удалось разобрать ответ от сервера"))
            }

            Result.success(parsed)
        } catch (e: Exception) {
            Result.failure(IOException("Сетевая ошибка при связи с сервером Яндекса: ${e.localizedMessage ?: e.message}", e))
        }
    }
}
