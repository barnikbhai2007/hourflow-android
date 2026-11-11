package dev.hourflow.app.ai

import dev.hourflow.app.data.AiProvider
import dev.hourflow.app.data.AppSettings
import dev.hourflow.app.data.ChatMessage
import dev.hourflow.app.data.ChatRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object AiClient {

    suspend fun chat(
        settings: AppSettings,
        systemPrompt: String,
        history: List<ChatMessage>
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val key = settings.apiKey.trim()
            require(key.isNotBlank()) { "Add an API key in Settings first." }
            val messages = history.filterNot { it.failed || it.text.isBlank() }.takeLast(18)
            require(messages.isNotEmpty()) { "Nothing to send." }

            when (settings.provider) {
                AiProvider.GEMINI -> gemini(settings, key, systemPrompt, messages)
                AiProvider.OPENAI -> openAiCompatible(settings, key, systemPrompt, messages)
            }
        }
    }

    private fun gemini(
        settings: AppSettings,
        key: String,
        systemPrompt: String,
        messages: List<ChatMessage>
    ): String {
        val model = settings.model.trim()
            .removePrefix("models/")
            .ifBlank { AiProvider.GEMINI.defaultModel }
        val base = settings.baseUrl.trim().ifBlank { AiProvider.GEMINI.defaultBaseUrl }.trimEnd('/')
        val url = "$base/models/$model:generateContent?key=${URLEncoder.encode(key, "UTF-8")}"

        val contents = JSONArray()
        messages.forEach { message ->
            contents.put(
                JSONObject()
                    .put(
                        "role",
                        if (message.role == ChatRole.USER) "user" else "model"
                    )
                    .put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", message.text))
                    )
            )
        }

        val body = JSONObject()
            .put(
                "systemInstruction",
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
            )
            .put("contents", contents)
            .put(
                "generationConfig",
                JSONObject()
                    .put("temperature", 0.65)
                    .put("maxOutputTokens", 1400)
            )

        val raw = postJson(url, body, emptyMap())
        val root = JSONObject(raw)
        val candidates = root.optJSONArray("candidates")
            ?: throw IllegalStateException(apiMessage(root) ?: "The model returned no candidates.")
        if (candidates.length() == 0) {
            throw IllegalStateException(apiMessage(root) ?: "The model returned an empty response.")
        }
        val candidate = candidates.getJSONObject(0)
        val parts = candidate.optJSONObject("content")?.optJSONArray("parts")
            ?: throw IllegalStateException(
                apiMessage(root) ?: "No text in the response (finish reason: ${candidate.optString("finishReason")})."
            )
        val text = buildString {
            for (index in 0 until parts.length()) {
                append(parts.optJSONObject(index)?.optString("text").orEmpty())
            }
        }.trim()
        if (text.isBlank()) {
            throw IllegalStateException(
                apiMessage(root) ?: "The model returned an empty reply (finish reason: ${candidate.optString("finishReason")})."
            )
        }
        return text
    }

    private fun openAiCompatible(
        settings: AppSettings,
        key: String,
        systemPrompt: String,
        messages: List<ChatMessage>
    ): String {
        val model = settings.model.trim().ifBlank { AiProvider.OPENAI.defaultModel }
        val base = settings.baseUrl.trim().ifBlank { AiProvider.OPENAI.defaultBaseUrl }.trimEnd('/')
        val url = "$base/chat/completions"

        val messageArray = JSONArray()
        messageArray.put(JSONObject().put("role", "system").put("content", systemPrompt))
        messages.forEach { message ->
            messageArray.put(
                JSONObject()
                    .put("role", if (message.role == ChatRole.USER) "user" else "assistant")
                    .put("content", message.text)
            )
        }

        val body = JSONObject()
            .put("model", model)
            .put("messages", messageArray)
            .put("temperature", 0.65)
            .put("max_tokens", 1400)

        val raw = postJson(
            url = url,
            body = body,
            headers = mapOf("Authorization" to "Bearer $key")
        )
        val root = JSONObject(raw)
        val choices = root.optJSONArray("choices")
            ?: throw IllegalStateException(apiMessage(root) ?: "The provider returned no choices.")
        if (choices.length() == 0) {
            throw IllegalStateException(apiMessage(root) ?: "The provider returned an empty response.")
        }
        val content = choices.getJSONObject(0)
            .optJSONObject("message")
            ?.optString("content")
            ?.trim()
            .orEmpty()
        if (content.isBlank()) {
            throw IllegalStateException(apiMessage(root) ?: "The model returned an empty reply.")
        }
        return content
    }

    private fun postJson(
        url: String,
        body: JSONObject,
        headers: Map<String, String>
    ): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 90_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            headers.forEach { (name, value) -> setRequestProperty(name, value) }
        }

        try {
            connection.outputStream.use { stream ->
                stream.write(body.toString().toByteArray(Charsets.UTF_8))
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                throw IllegalStateException(readableError(code, text))
            }
            return text
        } finally {
            connection.disconnect()
        }
    }

    private fun readableError(code: Int, raw: String): String {
        val parsed = runCatching {
            val json = JSONObject(raw)
            json.optJSONObject("error")?.optString("message")
                ?: json.optJSONArray("error")?.optJSONObject(0)?.optString("message")
        }.getOrNull()

        val detail = parsed?.takeIf { it.isNotBlank() }
            ?: raw.trim().take(260).ifBlank { "No error details." }
        val hint = when (code) {
            401, 403 -> " Check the API key and model access."
            404 -> " Check the model name and base URL."
            429 -> " Rate limit or quota reached. Try again in a minute."
            else -> ""
        }
        return "HTTP $code · $detail$hint"
    }

    private fun apiMessage(root: JSONObject): String? =
        root.optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }
}
