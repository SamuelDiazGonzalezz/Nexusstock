package com.nexusstock.bot

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Abstracción del endpoint de chat para poder sustituirlo en los tests. */
fun interface ChatCompletionApi {
    suspend fun complete(request: JSONObject): JSONObject
}

/** Cliente mínimo de la API de Groq (compatible con OpenAI) sin dependencias externas. */
class GroqClient(private val apiKey: String) : ChatCompletionApi {

    override suspend fun complete(request: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 60_000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "application/json")
        }
        try {
            connection.outputStream.use { it.write(request.toString().toByteArray()) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IOException("Groq respondió $code: ${body.take(300)}")
            JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val ENDPOINT = "https://api.groq.com/openai/v1/chat/completions"
    }
}
