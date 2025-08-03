package com.example.locationtest.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object TelegramService {
    
    suspend fun sendMessage(
        botToken: String,
        chatId: String,
        message: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Validate bot token format
            if (!isValidBotToken(botToken)) {
                return@withContext Result.failure(Exception("Invalid bot token format. Expected format: 123456789:ABC..."))
            }
            
            // Construct the URL
            val urlString = "https://api.telegram.org/bot$botToken/sendMessage"
            val url = URL(urlString)
            
            // Create connection
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            
            // Create JSON payload
            val jsonPayload = JSONObject().apply {
                put("chat_id", chatId)
                put("text", message)
                put("parse_mode", "HTML")
            }
            
            // Send request
            val outputWriter = OutputStreamWriter(connection.outputStream)
            outputWriter.write(jsonPayload.toString())
            outputWriter.flush()
            outputWriter.close()
            
            // Read response
            val responseCode = connection.responseCode
            val inputStream = if (responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            
            val reader = BufferedReader(InputStreamReader(inputStream))
            val response = reader.readText()
            reader.close()
            
            // Parse response
            val responseJson = JSONObject(response)
            
            if (responseJson.getBoolean("ok")) {
                Result.success("Message sent successfully")
            } else {
                val errorDescription = responseJson.optString("description", "Unknown error")
                Result.failure(Exception("Telegram API Error: $errorDescription"))
            }
            
        } catch (e: Exception) {
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }
    
    private fun isValidBotToken(token: String): Boolean {
        // Telegram bot token format: number:alphanumeric (e.g., 123456789:ABCdefGHIjklMNOpqrsTUVwxyz)
        val tokenPattern = "^[0-9]+:[A-Za-z0-9_-]+$".toRegex()
        return token.matches(tokenPattern)
    }
}
