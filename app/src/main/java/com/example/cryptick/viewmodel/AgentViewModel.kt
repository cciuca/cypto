package com.example.cryptick.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cryptick.ai.GeminiConfig
import com.example.cryptick.model.Message
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AgentViewModel : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private var isFirstMessage = true

    init {
        // Añadir el mensaje inicial del sistema
        addMessage(Message(
            text = "¡Hola! Soy tu asistente virtual de inversión. ¿En qué puedo ayudarte?",
            isFromUser = false
        ))
    }

    fun sendMessage(text: String) {
        // Si es el mensaje inicial del usuario, no lo procesamos
        if (isFirstMessage && text == messages.value.first().text) {
            isFirstMessage = false
            return
        }

        val userMessage = Message(text = text, isFromUser = true)
        addMessage(userMessage)

        val thinkingMessage = Message(text = "Pensando...", isFromUser = false)
        addMessage(thinkingMessage)

        viewModelScope.launch {
            try {
                // Construir el prompt completo con el contexto
                val fullPrompt = """
                    ${GeminiConfig.INVESTMENT_CONTEXT}
                    
                    Usuario: $text
                """.trimIndent()

                val response = GeminiConfig.model.generateContent(
                    content {
                        text(fullPrompt)
                    }
                )
                
                removeLastMessage() // Eliminar mensaje de "pensando"
                
                response.text?.let { responseText ->
                    val agentMessage = Message(text = responseText, isFromUser = false)
                    addMessage(agentMessage)
                }
            } catch (e: Exception) {
                Log.e("AgentViewModel", "Error al generar respuesta: ${e.message}", e)
                removeLastMessage() // Eliminar mensaje de "pensando"
                val errorMessage = Message(
                    text = "Error: ${e.message}\nPor favor, inténtalo de nuevo.",
                    isFromUser = false
                )
                addMessage(errorMessage)
            }
        }
    }

    private fun addMessage(message: Message) {
        _messages.value = _messages.value + message
    }

    private fun removeLastMessage() {
        _messages.value = _messages.value.dropLast(1)
    }
} 