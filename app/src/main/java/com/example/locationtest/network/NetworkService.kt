package com.example.locationtest.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkService {
    private const val TELEGRAM_BASE_URL = "https://api.telegram.org/"
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(TELEGRAM_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val telegramApi: TelegramApi = retrofit.create(TelegramApi::class.java)
    
    suspend fun sendTelegramMessage(
        botToken: String,
        chatId: String,
        message: String
    ): Result<String> {
        return try {
            // Validate bot token format
            if (!isValidBotToken(botToken)) {
                return Result.failure(Exception("Invalid bot token format. Expected format: 123456789:ABC..."))
            }
            
            val telegramMessage = TelegramMessage(
                chat_id = chatId,
                text = message
            )
            
            val response = telegramApi.sendMessage(botToken, telegramMessage)
            
            if (response.isSuccessful && response.body()?.ok == true) {
                Result.success("Message sent successfully")
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Result.failure(Exception("Failed to send message: $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun isValidBotToken(token: String): Boolean {
        // Telegram bot token format: number:alphanumeric (e.g., 123456789:ABCdefGHIjklMNOpqrsTUVwxyz)
        val tokenPattern = "^[0-9]+:[A-Za-z0-9_-]+$".toRegex()
        return token.matches(tokenPattern)
    }
}
