package com.example.data.repository

import com.example.BuildConfig
import com.example.data.api.*
import com.example.data.database.ChatDao
import com.example.data.database.Conversation
import com.example.data.database.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import android.util.Log

class ChatRepository(private val chatDao: ChatDao) {

    val allConversations: Flow<List<Conversation>> = chatDao.getAllConversations()

    fun getMessagesForConversation(conversationId: Long): Flow<List<Message>> {
        return chatDao.getMessagesForConversation(conversationId)
    }

    suspend fun createConversation(title: String, modelType: String): Long = withContext(Dispatchers.IO) {
        val conv = Conversation(title = title, modelType = modelType)
        chatDao.insertConversation(conv)
    }

    suspend fun insertMessage(message: Message): Long = withContext(Dispatchers.IO) {
        chatDao.insertMessage(message)
    }

    suspend fun deleteConversation(conversationId: Long) = withContext(Dispatchers.IO) {
        chatDao.deleteConversation(conversationId)
    }

    /**
     * Send chat prompt to Gemini API with conversational memory and personality mapping!
     */
    suspend fun sendChatMessage(
        conversationId: Long,
        modelType: String,
        userPrompt: String,
        attachedImageBase64: String?,
        imageMimeType: String?,
        previousMessages: List<Message> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "YOUR_GEMINI_API_KEY") {
            return@withContext "API Error: Please set your GEMINI_API_KEY in the AI Studio Secrets Panel to interact with ARI."
        }

        // Configure system instruction personality
        val personalityPrompt = when (modelType.lowercase()) {
            "grok" -> "You are ARI, configured with the rebellious, humorous, and witty Grok personality. Answer with sarcasm, playful comments, complete transparency, and intellectual depth. Keep answers highly engaging and extremely smart!"
            "claude" -> "You are ARI, configured with the Claude personality. You are deep, cautious, highly detailed, brilliant at multi-language software development, and explain algorithms or syntax perfectly with beautiful reasoning and structural explanations."
            "chatgpt" -> "You are ARI, configured with the ChatGPT personality. You are extremely friendly, clear, highly structured, structured inside formatting with markdown, and highly verbose. Give summaries, lists, bullet points, and helpful formatting."
            else -> "You are ARI, an all-purpose advanced AI Assistant with a direct Gemini core. Be helpful, direct, prompt, accurate, and provide pristine explanations with syntax-highlighted code format when coding is asked."
        }

        // Keep system instruction as a Content object
        val systemInstruction = Content(
            parts = listOf(Part(text = personalityPrompt))
        )

        // Compile discussion memory
        val contentsList = mutableListOf<Content>()
        
        // Add past chat rounds (limit to last 10 for latency, maintaining conversational flow)
        val historyToInclude = previousMessages.takeLast(10)
        for (msg in historyToInclude) {
            val role = if (msg.sender == "user") "user" else "model"
            if (msg.isImage && msg.imageData != null) {
                // If past message had image, we can include it in content history if we want
                contentsList.add(
                    Content(
                        parts = listOf(
                            Part(inlineData = InlineData(mimeType = "image/jpeg", data = msg.imageData)),
                            Part(text = msg.text)
                        )
                    )
                )
            } else {
                contentsList.add(
                    Content(parts = listOf(Part(text = msg.text)))
                )
            }
        }

        // Add contemporary user input
        val currentParts = mutableListOf<Part>()
        if (attachedImageBase64 != null && imageMimeType != null) {
            currentParts.add(Part(inlineData = InlineData(mimeType = imageMimeType, data = attachedImageBase64)))
        }
        currentParts.add(Part(text = userPrompt))
        contentsList.add(Content(parts = currentParts))

        // We use gemini-3.5-flash as our generalist/reasoning/multimodal standard engine
        val request = GenerateContentRequest(
            contents = contentsList,
            systemInstruction = systemInstruction,
            generationConfig = GenerationConfig(temperature = 0.7)
        )

        try {
            val response = RetrofitClient.service.generateContent("gemini-3.5-flash", apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "I apologize, but I could not formulate a response. No content was returned."
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error generation content", e)
            "Error querying ARI: ${e.localizedMessage ?: "Unknown connection failure."}"
        }
    }

    /**
     * Generates an image using gemini-2.5-flash-image given a textual design prompt.
     * Returns base64 representation of the generated JPEG image if successful.
     */
    suspend fun generateImage(prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "YOUR_GEMINI_API_KEY") {
            throw Exception("API Key is missing. Configure GEMINI_API_KEY in the Secrets panel.")
        }

        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)))
            ),
            generationConfig = GenerationConfig(
                imageConfig = ImageConfig(aspectRatio = "1:1", imageSize = "1K"),
                responseModalities = listOf("TEXT", "IMAGE")
            )
        )

        try {
            val response = RetrofitClient.service.generateContent("gemini-2.5-flash-image", apiKey, request)
            // Look for image data in parts
            val parts = response.candidates?.firstOrNull()?.content?.parts
            val imagePart = parts?.firstOrNull { it.inlineData != null }
            val base64Data = imagePart?.inlineData?.data
            
            if (base64Data != null) {
                base64Data
            } else {
                // If it output text explanation of why it couldn't generate or similar message
                val textResponse = parts?.firstOrNull { it.text != null }?.text
                throw Exception(textResponse ?: "Image generation could not complete. No image parts received.")
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error generating image", e)
            throw e
        }
    }
}
