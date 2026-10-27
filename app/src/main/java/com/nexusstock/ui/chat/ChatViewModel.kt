package com.nexusstock.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexusstock.bot.NexusBot
import com.nexusstock.data.model.ChatMessage
import com.nexusstock.data.model.Sender
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<ChatMessage> = listOf(
        ChatMessage(text = "Hola, soy **Nexus**. Puedo añadir, retirar, consultar o eliminar productos. Escribe \"ayuda\" para ver ejemplos.", sender = Sender.NEXUS)
    ),
    val input: String = "",
    val isTyping: Boolean = false,
    val isAiEnabled: Boolean = false
)

class ChatViewModel(private val bot: NexusBot) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState(isAiEnabled = bot.isAiEnabled))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun onInputChange(value: String) = _uiState.update { it.copy(input = value) }

    fun send(text: String = _uiState.value.input) {
        val message = text.trim()
        if (message.isEmpty() || _uiState.value.isTyping) return
        _uiState.update {
            it.copy(messages = it.messages + ChatMessage(text = message, sender = Sender.USER), input = "", isTyping = true)
        }
        viewModelScope.launch {
            if (!bot.isAiEnabled) delay(350) // pequeña pausa para que la respuesta se sienta natural
            val answer = runCatching { bot.reply(message) }
                .getOrElse { "Ha ocurrido un error: ${it.message}" }
            _uiState.update {
                it.copy(messages = it.messages + ChatMessage(text = answer, sender = Sender.NEXUS), isTyping = false)
            }
        }
    }
}
