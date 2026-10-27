package com.nexusstock.bot

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * Nexus con IA: envía la conversación a Groq junto con las [InventoryTools] y ejecuta
 * las llamadas a herramientas que pida el modelo hasta obtener una respuesta final.
 */
class GroqNexus(
    private val api: ChatCompletionApi,
    private val tools: InventoryTools,
    private val model: String
) : NexusEngine {

    /** Historial de turnos usuario/asistente (sin los mensajes intermedios de herramientas). */
    private val history = ArrayDeque<JSONObject>()

    /** Indica si el último [reply] llegó a modificar o consultar el inventario antes de fallar. */
    var executedTools = 0
        private set

    override suspend fun reply(input: String): String {
        executedTools = 0
        val messages = JSONArray().put(message("system", systemPrompt()))
        history.forEach { messages.put(it) }
        messages.put(message("user", input))

        repeat(MAX_TOOL_ROUNDS) {
            val request = JSONObject()
                .put("model", model)
                .put("messages", messages)
                .put("tools", tools.definitions)
                .put("tool_choice", "auto")
                .put("temperature", 0.2)
            val choice = api.complete(request).getJSONArray("choices").getJSONObject(0).getJSONObject("message")
            val toolCalls = choice.optJSONArray("tool_calls")

            if (toolCalls == null || toolCalls.length() == 0) {
                val answer = choice.text("content").trim().ifEmpty { "Hecho." }
                remember(input, answer)
                return answer
            }

            // Reenviamos solo los campos estándar del mensaje del asistente.
            messages.put(
                JSONObject()
                    .put("role", "assistant")
                    .put("content", choice.text("content"))
                    .put("tool_calls", toolCalls)
            )
            for (i in 0 until toolCalls.length()) {
                val call = toolCalls.getJSONObject(i)
                val function = call.getJSONObject("function")
                val result = tools.execute(function.getString("name"), function.optString("arguments"))
                executedTools++
                messages.put(
                    JSONObject()
                        .put("role", "tool")
                        .put("tool_call_id", call.getString("id"))
                        .put("content", result)
                )
            }
        }
        return "He realizado varias operaciones pero no he podido terminar. Revisa el inventario."
    }

    private fun remember(user: String, assistant: String) {
        history.addLast(message("user", user))
        history.addLast(message("assistant", assistant))
        while (history.size > MAX_HISTORY) history.removeFirst()
    }

    /** El contenido puede venir como null JSON; optString lo convertiría en "null". */
    private fun JSONObject.text(key: String) = if (isNull(key)) "" else optString(key)

    private fun message(role: String, content: String) = JSONObject().put("role", role).put("content", content)

    private fun systemPrompt() = """
        Eres Nexus, el asistente de inventario de la app NexusStock. Hoy es ${LocalDate.now()}.
        - Responde siempre en español, de forma breve y clara.
        - Para cualquier dato del inventario usa las herramientas; nunca inventes cantidades ni productos.
        - Puedes encadenar varias herramientas para cumplir una petición.
        - Si un nombre es ambiguo o falta información imprescindible, pregunta antes de modificar nada.
        - Elimina productos solo si el usuario lo pide de forma explícita.
        - Resalta los nombres de producto con **negrita**. No uses tablas ni encabezados.
    """.trimIndent()

    private companion object {
        const val MAX_TOOL_ROUNDS = 6
        const val MAX_HISTORY = 20
    }
}
