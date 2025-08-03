package com.example.locationtest.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface TelegramApi {
    @POST("bot{token}/sendMessage")
    suspend fun sendMessage(
        @Path("token", encoded = true) token: String,
        @Body message: TelegramMessage
    ): Response<TelegramResponse>
}

data class TelegramMessage(
    val chat_id: String,
    val text: String,
    val parse_mode: String = "HTML"
)

data class TelegramResponse(
    val ok: Boolean,
    val result: TelegramMessageResult?
)

data class TelegramMessageResult(
    val message_id: Int,
    val date: Long
)
