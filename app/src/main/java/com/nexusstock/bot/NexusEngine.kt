package com.nexusstock.bot

/** Cualquier motor capaz de responder al usuario (IA en la nube o reglas locales). */
interface NexusEngine {
    suspend fun reply(input: String): String
}
