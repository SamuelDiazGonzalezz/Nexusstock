package com.nexusstock.bot

import com.nexusstock.data.model.Product
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class GroqNexusTest {
    private val repo = FakeRepository()
    private val tools = InventoryTools(repo)

    /** API falsa que devuelve las respuestas en orden y guarda las peticiones recibidas. */
    private class ScriptedApi(vararg responses: () -> JSONObject) : ChatCompletionApi {
        private val queue = ArrayDeque(responses.toList())
        val requests = mutableListOf<JSONObject>()
        override suspend fun complete(request: JSONObject): JSONObject {
            requests += JSONObject(request.toString())
            return queue.removeFirst().invoke()
        }
    }

    private fun toolCall(id: String, name: String, args: String) = JSONObject().put(
        "choices", JSONArray().put(
            JSONObject().put(
                "message", JSONObject()
                    .put("role", "assistant")
                    .put("content", JSONObject.NULL)
                    .put(
                        "tool_calls", JSONArray().put(
                            JSONObject().put("id", id).put("type", "function")
                                .put("function", JSONObject().put("name", name).put("arguments", args))
                        )
                    )
            )
        )
    )

    private fun answer(text: String) = JSONObject().put(
        "choices", JSONArray().put(JSONObject().put("message", JSONObject().put("role", "assistant").put("content", text)))
    )

    @Test
    fun `ejecuta la herramienta y devuelve la respuesta final`() = runBlocking {
        val api = ScriptedApi(
            { toolCall("call_1", "ajustar_stock", """{"nombre":"Tornillos","cantidad":10}""") },
            { answer("Añadidas 10 uds. de **Tornillos**.") }
        )
        val reply = GroqNexus(api, tools, "openai/gpt-oss-120b").reply("mete 10 tornillos")

        assertEquals("Añadidas 10 uds. de **Tornillos**.", reply)
        assertEquals(10, repo.findByName("Tornillos")?.quantity)
        val toolMessage = api.requests[1].getJSONArray("messages").let { it.getJSONObject(it.length() - 1) }
        assertEquals("tool", toolMessage.getString("role"))
        assertEquals("call_1", toolMessage.getString("tool_call_id"))
        assertTrue(api.requests[0].getJSONArray("tools").length() > 0)
    }

    @Test
    fun `nombre ambiguo devuelve error al modelo sin modificar nada`() = runBlocking {
        repo.save(Product(name = "Tornillos M4", quantity = 5))
        repo.save(Product(name = "Tornillos M6", quantity = 5))
        val result = JSONObject(tools.execute("eliminar_producto", """{"nombre":"Tornillos"}"""))

        assertFalse(result.getBoolean("ok"))
        assertEquals(2, repo.getAll().size)
    }

    @Test
    fun `sin conexion usa las reglas locales`() = runBlocking {
        val failing = GroqNexus(ScriptedApi({ throw IOException("sin red") }), tools, "m")
        val reply = NexusBot(failing, RuleBasedNexus(repo)).reply("añade 4 tuercas")

        assertEquals(4, repo.findByName("tuercas")?.quantity)
        assertTrue(reply.contains("Modo sin conexión"))
    }

    @Test
    fun `si falla tras ejecutar herramientas no repite la orden`() = runBlocking {
        val api = ScriptedApi(
            { toolCall("call_1", "ajustar_stock", """{"nombre":"Tuercas","cantidad":4}""") },
            { throw IOException("sin red") }
        )
        NexusBot(GroqNexus(api, tools, "m"), RuleBasedNexus(repo)).reply("añade 4 tuercas")

        assertEquals(4, repo.findByName("Tuercas")?.quantity)
    }
}
