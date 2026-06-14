package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Base64
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.Conversation
import com.example.data.database.Message
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.Locale

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = ChatRepository(db.chatDao())
    private val sharedPrefs = application.getSharedPreferences("ari_prefs", Context.MODE_PRIVATE)

    // --- State Observables ---
    val conversations: StateFlow<List<Conversation>> = repository.allConversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeConversationId = MutableStateFlow<Long?>(null)
    val activeConversationId: StateFlow<Long?> = _activeConversationId.asStateFlow()

    private val _activeMessages = MutableStateFlow<List<Message>>(emptyList())
    val activeMessages: StateFlow<List<Message>> = _activeMessages.asStateFlow()

    var isGenerating by mutableStateOf(false)
        private set

    var uiError by mutableStateOf<String?>(null)

    // Draft Input States
    var currentInputText by mutableStateOf("")
    var attachedImageBase64 by mutableStateOf<String?>(null)
    var attachedImageBitmap by mutableStateOf<Bitmap?>(null)
    
    // selectedModel is persisted via standard backing state representing web-like localStorage
    private var _selectedModel by mutableStateOf(sharedPrefs.getString("selected_model", "gemini") ?: "gemini")
    var selectedModel: String
        get() = _selectedModel
        set(value) {
            _selectedModel = value
            sharedPrefs.edit().putString("selected_model", value).apply()
        }

    // isImageGenerationMode is persisted via standard backing state representing web-like localStorage
    private var _isImageGenerationMode by mutableStateOf(sharedPrefs.getBoolean("is_image_mode", false))
    var isImageGenerationMode: Boolean
        get() = _isImageGenerationMode
        set(value) {
            _isImageGenerationMode = value
            sharedPrefs.edit().putBoolean("is_image_mode", value).apply()
        }

    // --- Voice Processing (TTS & STT) ---
    private var textToSpeech: TextToSpeech? = null
    var speakingMessageId by mutableStateOf<Long?>(null)
        private set

    var isListening by mutableStateOf(false)
        private set

    private var speechRecognizer: SpeechRecognizer? = null

    init {
        // Automatically restore saved conversation or fallback to the latest conversation if available
        viewModelScope.launch {
            val savedActiveId = sharedPrefs.getLong("active_conversation_id", -1L)
            if (savedActiveId != -1L) {
                selectConversation(savedActiveId)
            }
            conversations.collect { list ->
                if (list.isNotEmpty()) {
                    val currentActiveId = _activeConversationId.value
                    val activeExists = list.any { it.id == currentActiveId }
                    if (!activeExists) {
                        selectConversation(list.first().id)
                    }
                } else {
                    _activeConversationId.value = null
                    _activeMessages.value = emptyList()
                }
            }
        }

        // Initialize Text To Speech
        textToSpeech = TextToSpeech(application) { status ->
            if (status != TextToSpeech.SUCCESS) {
                Log.e("ChatViewModel", "Failed to initialize Text To Speech")
            } else {
                textToSpeech?.language = Locale.US
            }
        }
    }

    // --- Database Actions ---
    fun selectConversation(id: Long) {
        _activeConversationId.value = id
        sharedPrefs.edit().putLong("active_conversation_id", id).apply()
        // Collect messages for this specific conversation reactivity
        viewModelScope.launch {
            val messagesFlow = repository.getMessagesForConversation(id)
            messagesFlow.collect { list ->
                _activeMessages.value = list
            }
        }
    }

    fun startNewConversation(model: String = selectedModel) {
        viewModelScope.launch {
            // Generate basic title
            val personaName = model.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            val newTitle = "Chat with $personaName"
            val newId = repository.createConversation(newTitle, model)
            selectConversation(newId)
            clearDraftInput()
        }
    }

    fun deleteCurrentConversation() {
        val currentId = _activeConversationId.value ?: return
        viewModelScope.launch {
            repository.deleteConversation(currentId)
            _activeConversationId.value = null
            sharedPrefs.edit().remove("active_conversation_id").apply()
            _activeMessages.value = emptyList()
            stopSpeaking()
        }
    }

    fun clearDraftInput() {
        currentInputText = ""
        attachedImageBase64 = null
        attachedImageBitmap = null
        uiError = null
    }

    // --- Process Photo Attachment Inputs ---
    fun attachBitmap(bitmap: Bitmap) {
        attachedImageBitmap = bitmap
        // Compress and encode base64
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
        val byteArray = outputStream.toByteArray()
        attachedImageBase64 = Base64.encodeToString(byteArray, Base64.NO_WRAP)
        uiError = null
    }

    // --- Core AI Actions ---
    fun submitPrompt() {
        val promptText = currentInputText.trim()
        val hasText = promptText.isNotEmpty()
        val hasImage = attachedImageBase64 != null

        if (!hasText && !hasImage) return

        viewModelScope.launch {
            var activeId = _activeConversationId.value
            if (activeId == null) {
                // Instantly boost into a new conversation
                val personaName = selectedModel.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                val title = if (isImageGenerationMode) "Art: $promptText" else promptText.take(24)
                activeId = repository.createConversation(title, selectedModel)
                _activeConversationId.value = activeId
                selectConversation(activeId)
            }

            // Save user message
            val userMsg = Message(
                conversationId = activeId,
                sender = "user",
                text = promptText,
                isImage = hasImage,
                imageData = attachedImageBase64
            )
            repository.insertMessage(userMsg)

            // Cache temporary input attributes
            val promptToSubmit = promptText
            val imgBase64 = attachedImageBase64
            val currentMode = isImageGenerationMode

            // Clear inputs right away for beautiful transition animations
            clearDraftInput()
            isGenerating = true
            uiError = null

            try {
                if (currentMode) {
                    // --- Case A: Image Generation via gemini-2.5-flash-image ---
                    val finalPrompt = "Create a high quality digital illustration or creative artwork based on this prompt: $promptToSubmit"
                    val generatedBase64 = repository.generateImage(finalPrompt)
                    
                    val aiMsg = Message(
                        conversationId = activeId,
                        sender = "assistant",
                        text = "Here is the visual masterpiece generated based on your prompt: \"$promptToSubmit\"",
                        isImage = true,
                        imageData = generatedBase64
                    )
                    repository.insertMessage(aiMsg)
                } else {
                    // --- Case B: Conversational / Coding Request via memory backend ---
                    // Fetch all messages to maintain context
                    val messageHistory = _activeMessages.value
                    
                    val responseModelType = selectedModel
                    val answerText = repository.sendChatMessage(
                        conversationId = activeId,
                        modelType = responseModelType,
                        userPrompt = promptToSubmit,
                        attachedImageBase64 = imgBase64,
                        imageMimeType = if (imgBase64 != null) "image/jpeg" else null,
                        previousMessages = messageHistory
                    )

                    val aiMsg = Message(
                        conversationId = activeId,
                        sender = "assistant",
                        text = answerText,
                        isImage = false
                    )
                    repository.insertMessage(aiMsg)
                }
            } catch (e: Exception) {
                uiError = e.localizedMessage ?: "Unknown compilation failure or network disruption."
                val errorMsg = Message(
                    conversationId = activeId,
                    sender = "assistant",
                    text = "ARI System Alert:\n${uiError}\n\nPlease ensure your workspace features a valid GEMINI_API_KEY inside the Secrets Panel."
                )
                repository.insertMessage(errorMsg)
            } finally {
                isGenerating = false
            }
        }
    }

    // --- Voice Input (Speech-To-Text / STT) ---
    fun toggleVoiceListening(context: Context) {
        if (isListening) {
            stopListening()
        } else {
            startListening(context)
        }
    }

    private fun startListening(context: Context) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            uiError = "Speech recognition is not available or disabled on this device."
            return
        }

        stopSpeaking() // Stop readouts while dictating

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    isListening = false
                }

                override fun onError(error: Int) {
                    isListening = false
                    val errorDescription = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client-side error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissions neglected"
                        SpeechRecognizer.ERROR_NETWORK -> "Network failure"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        SpeechRecognizer.ERROR_NO_MATCH -> "Spoken query not recognized"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Dictation engine busy"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Dictation timed out"
                        else -> "Speech recognition failed ($error)"
                    }
                    Log.e("STT", "Speech recognizer error: $errorDescription")
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val textSpoken = matches[0]
                        currentInputText = if (currentInputText.isEmpty()) textSpoken else "$currentInputText $textSpoken"
                    }
                    isListening = false
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }

        speechRecognizer?.startListening(speechIntent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        isListening = false
    }

    // --- Voice Output (Text-To-Speech / TTS) ---
    fun toggleSpeakMessage(message: Message) {
        if (speakingMessageId == message.id) {
            stopSpeaking()
        } else {
            speakText(message.id, message.text)
        }
    }

    private fun speakText(messageId: Long, text: String) {
        textToSpeech?.let { tts ->
            tts.stop()
            speakingMessageId = messageId
            
            // Strip markdown block labels if any for pristine listening experience
            val cleanSpeechText = text
                .replace(Regex("```[a-zA-Z]*"), "") // Strip code fences
                .replace(Regex("[*#_`~]"), "") // Strip simple markdown signs
            
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, messageId.toString())
            }
            
            tts.speak(cleanSpeechText, TextToSpeech.QUEUE_FLUSH, params, messageId.toString())
            
            // Keep status alive
            tts.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    if (utteranceId == messageId.toString()) {
                        speakingMessageId = null
                    }
                }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    speakingMessageId = null
                }
            })
        }
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
        speakingMessageId = null
    }

    override fun onCleared() {
        super.onCleared()
        textToSpeech?.shutdown()
        speechRecognizer?.destroy()
    }
}

class ChatViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            return ChatViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
