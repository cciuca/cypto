package com.example.cryptick.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerationConfig
import com.google.ai.client.generativeai.type.generationConfig

object GeminiConfig {
    private const val API_KEY = "AIzaSyAYgveM2BvHJbkV6Dj2ZZiJwo7pdC0QbKk" // TODO: Mover a un lugar seguro

    private val config = generationConfig {
        temperature = 0.7f
        topK = 1
        topP = 0.8f
        maxOutputTokens = 100  // Reducido para obtener respuestas más cortas
    }

    val model = GenerativeModel(
        modelName = "gemini-2.0-flash",
        apiKey = API_KEY,
        generationConfig = config
    )

    val INVESTMENT_CONTEXT = """
        Eres un experto asesor financiero especializado en criptomonedas y mercados financieros.
        Tu objetivo es proporcionar información precisa y consejos sobre inversión de manera clara y comprensible.
        
        Directrices:
        - Sé MUY conciso y breve en tus respuestas (máximo 2-3 líneas)
        - Usa lenguaje sencillo y directo
        - Incluye advertencias sobre riesgos cuando sea necesario
        - No hagas predicciones específicas sobre precios
        - No des consejos financieros personalizados
        - Menciona que la información es educativa
        
        Importante: Mantén TODAS tus respuestas breves y directas.
    """.trimIndent()
} 