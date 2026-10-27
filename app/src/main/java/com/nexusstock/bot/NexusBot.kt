package com.nexusstock.bot

/**
 * Punto de entrada de Nexus para la UI.
 * Usa la IA de Groq si hay API key y, si falla la conexión, responde con las reglas locales.
 */
class NexusBot(
    private val ai: GroqNexus?,
    private val local: RuleBasedNexus
) : NexusEngine {

    val isAiEnabled: Boolean get() = ai != null

    override suspend fun reply(input: String): String {
        if (ai == null) return local.reply(input)
        return try {
            ai.reply(input)
        } catch (e: Exception) {
            // Si la IA ya tocó el inventario no repetimos la orden, para no aplicarla dos veces.
            if (ai.executedTools > 0) {
                "Se perdió la conexión a mitad de la operación. Revisa el inventario antes de repetirla."
            } else {
                local.reply(input) + "\n\n(Modo sin conexión: ${e.message ?: "error de red"})"
            }
        }
    }
}
