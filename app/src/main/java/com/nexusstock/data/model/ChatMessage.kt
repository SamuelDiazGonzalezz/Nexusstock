package com.nexusstock.data.model

enum class Sender { USER, NEXUS }

data class ChatMessage(
    val id: Long = System.nanoTime(),
    val text: String,
    val sender: Sender,
    val timestamp: Long = System.currentTimeMillis()
)
