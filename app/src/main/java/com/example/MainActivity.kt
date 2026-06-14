package com.example

import android.Manifest
import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.database.Conversation
import com.example.data.database.Message
import com.example.ui.ChatViewModel
import com.example.ui.ChatViewModelFactory
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import java.io.InputStream
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen()
            }
        }
    }
}

// --- Custom Drawables / Icons ---
val IconCamera = ImageVector.Builder(
    name = "CameraCustom",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        fill = null,
        stroke = Brush.linearGradient(listOf(Color.White, Color.White)),
        strokeLineWidth = 2f,
        strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round
    ) {
        moveTo(12f, 15f)
        curveTo(13.66f, 15f, 15f, 13.66f, 15f, 12f)
        curveTo(15f, 10.34f, 13.66f, 9f, 12f, 9f)
        curveTo(10.34f, 9f, 9f, 10.34f, 9f, 12f)
        curveTo(9f, 13.66f, 10.34f, 15f, 12f, 15f)
        close()
        moveTo(9f, 2f)
        lineTo(15f, 2f)
        lineTo(17f, 5f)
        lineTo(21f, 5f)
        curveTo(22.1f, 5f, 23f, 5.9f, 23f, 7f)
        lineTo(23f, 19f)
        curveTo(23f, 20.1f, 22.1f, 21f, 21f, 21f)
        lineTo(3f, 21f)
        curveTo(1.9f, 21f, 1f, 20.1f, 1f, 19f)
        lineTo(1f, 7f)
        curveTo(1f, 5.9f, 1.9f, 5f, 3f, 5f)
        lineTo(7f, 5f)
        close()
    }
}.build()

val IconPhoto = ImageVector.Builder(
    name = "PhotoCustom",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        fill = null,
        stroke = Brush.linearGradient(listOf(Color.White, Color.White)),
        strokeLineWidth = 2f,
        strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round
    ) {
        moveTo(19f, 3f)
        lineTo(5f, 3f)
        curveTo(3.9f, 3f, 3f, 3.9f, 3f, 5f)
        lineTo(3f, 19f)
        curveTo(3f, 20.1f, 3.9f, 21f, 5f, 21f)
        lineTo(19f, 21f)
        curveTo(20.1f, 21f, 21f, 20.1f, 21f, 19f)
        lineTo(21f, 5f)
        curveTo(21f, 3.9f, 20.1f, 3f, 19f, 3f)
        close()
        moveTo(8.5f, 11f)
        curveTo(9.88f, 11f, 11f, 9.88f, 11f, 8.5f)
        curveTo(11f, 7.12f, 9.88f, 6f, 8.5f, 6f)
        curveTo(7.12f, 6f, 6f, 7.12f, 6f, 8.5f)
        curveTo(6f, 9.88f, 7.12f, 11f, 8.5f, 11f)
        close()
        moveTo(5f, 18f)
        lineTo(9f, 14f)
        lineTo(12f, 17f)
        lineTo(17f, 12f)
        lineTo(20f, 15f)
        lineTo(20f, 19f)
        lineTo(5f, 19f)
        close()
    }
}.build()

val IconSparkles = ImageVector.Builder(
    name = "SparklesCustom",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        fill = null,
        stroke = Brush.linearGradient(listOf(Color.White, Color.White)),
        strokeLineWidth = 2f,
        strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round
    ) {
        moveTo(9f, 4f)
        lineTo(11.5f, 9.5f)
        lineTo(17f, 12f)
        lineTo(11.5f, 14.5f)
        lineTo(9f, 20f)
        lineTo(6.5f, 14.5f)
        lineTo(1f, 12f)
        lineTo(6.5f, 9.5f)
        close()
        moveTo(19f, 3f)
        lineTo(20f, 5.5f)
        lineTo(22.5f, 6.5f)
        lineTo(20f, 7.5f)
        lineTo(19f, 10f)
        lineTo(18f, 7.5f)
        lineTo(15.5f, 6.5f)
        lineTo(18f, 5.5f)
        close()
    }
}.build()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val viewModel: ChatViewModel = viewModel(factory = ChatViewModelFactory(app))
    
    val conversations by viewModel.conversations.collectAsState()
    val activeConversationId by viewModel.activeConversationId.collectAsState()
    val activeMessages by viewModel.activeMessages.collectAsState()
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // Permission Handlers
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleVoiceListening(context)
        } else {
            Toast.makeText(context, "Microphone access is mandatory for voice commands.", Toast.LENGTH_SHORT).show()
        }
    }
    
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Camera permission granted! Ready to snap.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Camera permission denied.", Toast.LENGTH_SHORT).show()
        }
    }

    // Media Intent Launchers
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    viewModel.attachBitmap(bitmap)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error uploading photo: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            viewModel.attachBitmap(it)
        } ?: Toast.makeText(context, "No snapshot captured.", Toast.LENGTH_SHORT).show()
    }

    // Immersive Interactive "Talk with AI" vocal dialog
    var isVocalInteractiveModeActive by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(310.dp),
                drawerContainerColor = MaterialTheme.colorScheme.background,
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
            ) {
                SidebarContent(
                    conversations = conversations,
                    activeId = activeConversationId,
                    onSelect = { id ->
                        viewModel.selectConversation(id)
                        scope.launch { drawerState.close() }
                    },
                    onNewChat = { model ->
                        viewModel.startNewConversation(model)
                        scope.launch { drawerState.close() }
                    },
                    onDelete = { id ->
                        viewModel.deleteCurrentConversation()
                    },
                    selectedModel = viewModel.selectedModel,
                    onSelectModel = { viewModel.selectedModel = it }
                )
            }
        }
    ) {
        val uiAccentColor = when (viewModel.selectedModel.lowercase()) {
            "grok" -> Color(0xFFFF6D00) // Vibrant Wit Orange
            "claude" -> Color(0xFFE0D4C3) // Light ivory/clay standard model tone
            "chatgpt" -> Color(0xFF10A37F) // Smart green standard
            else -> MaterialTheme.colorScheme.primary // Cosmic Cyan glow
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Navigation Sidebar",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Pulsing core orb
                            val infiniteTransition = rememberInfiniteTransition(label = "orb")
                            val orbAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.4f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1500, easing = EaseInOutSine),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "orbAlpha"
                            )
                            
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(uiAccentColor.copy(alpha = orbAlpha))
                                    .border(1.5.dp, uiAccentColor, CircleShape)
                            )

                            Text(
                                text = "ARI",
                                style = TextStyle(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp,
                                    letterSpacing = 2.sp,
                                    brush = Brush.linearGradient(
                                        colors = listOf(uiAccentColor, MaterialTheme.colorScheme.primaryContainer)
                                    )
                                )
                            )
                            
                            Spacer(modifier = Modifier.width(4.dp))
                            
                            // Badge indicating active model type
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(uiAccentColor.copy(alpha = 0.15f))
                                    .border(0.5.dp, uiAccentColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = viewModel.selectedModel.uppercase(),
                                    style = TextStyle(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = uiAccentColor
                                    )
                                )
                            }
                        }
                    },
                    actions = {
                        // Quick Action button: Immersive vocal "Talk to AI" option
                        IconButton(
                            onClick = {
                                isVocalInteractiveModeActive = true
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            },
                            modifier = Modifier
                                .testTag("talk_with_ai_button")
                                .clip(CircleShape)
                                .background(uiAccentColor.copy(alpha = 0.15f))
                                .border(1.dp, uiAccentColor.copy(alpha = 0.3f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Talk to ARI",
                                tint = uiAccentColor
                            )
                        }

                        IconButton(
                            onClick = { viewModel.startNewConversation() },
                            modifier = Modifier.testTag("new_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Start New Convo",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        
                        if (activeConversationId != null) {
                            IconButton(
                                onClick = { viewModel.deleteCurrentConversation() },
                                modifier = Modifier.testTag("delete_chat_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Active Chat",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        actionIconContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            bottomBar = {
                InputBarArea(
                    inputText = viewModel.currentInputText,
                    onTextChanged = { viewModel.currentInputText = it },
                    attachedBitmap = viewModel.attachedImageBitmap,
                    onRemoveBitmap = {
                        viewModel.attachedImageBase64 = null
                        viewModel.attachedImageBitmap = null
                    },
                    isGenerating = viewModel.isGenerating,
                    isListening = viewModel.isListening,
                    isImageGenerationMode = viewModel.isImageGenerationMode,
                    onToggleGenerationMode = { viewModel.isImageGenerationMode = it },
                    onAttachGallery = { galleryLauncher.launch("image/*") },
                    onAttachCamera = {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        cameraLauncher.launch(null)
                    },
                    onTriggerMic = {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    onSubmit = { viewModel.submitPrompt() },
                    accentColor = uiAccentColor
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (activeMessages.isEmpty()) {
                    // Empty Conversation State
                    ChatIntroDisplay(
                        modelName = viewModel.selectedModel,
                        onQuickPrompt = { prompt ->
                            viewModel.currentInputText = prompt
                        },
                        isImageMode = viewModel.isImageGenerationMode,
                        onChangeMode = { viewModel.isImageGenerationMode = it },
                        accentColor = uiAccentColor
                    )
                } else {
                    // Chat Message Feed
                    val listState = rememberLazyListState()
                    LaunchedEffect(activeMessages.size, viewModel.isGenerating) {
                        if (activeMessages.isNotEmpty()) {
                            listState.animateScrollToItem(activeMessages.size - 1)
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(activeMessages) { msg ->
                            MessageBubble(
                                message = msg,
                                speakerId = viewModel.speakingMessageId,
                                onSpeakToggle = { viewModel.toggleSpeakMessage(msg) },
                                activeAccentColor = uiAccentColor
                            )
                        }
                        if (viewModel.isGenerating) {
                            item {
                                typingIndicator(accentColor = uiAccentColor)
                            }
                        }
                    }
                }

                // Global Toast UI Alert for validation errors
                viewModel.uiError?.let { err ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .border(1.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = err,
                            style = TextStyle(color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 13.sp)
                        )
                    }
                }
            }
        }

        // Voice Interactive "Talk with AI" overlay dialog when activated
        if (isVocalInteractiveModeActive) {
            Dialog(onDismissRequest = {
                viewModel.stopListening()
                viewModel.stopSpeaking()
                isVocalInteractiveModeActive = false
            }) {
                InteractiveVocalOverlay(
                    viewModel = viewModel,
                    accentColor = uiAccentColor,
                    onDismiss = {
                        viewModel.stopListening()
                        viewModel.stopSpeaking()
                        isVocalInteractiveModeActive = false
                    }
                )
            }
        }
    }
}

// --- Dynamic Sidebar Drawer content ---
@Composable
fun SidebarContent(
    conversations: List<Conversation>,
    activeId: Long?,
    onSelect: (Long) -> Unit,
    onNewChat: (String) -> Unit,
    onDelete: (Long) -> Unit,
    selectedModel: String,
    onSelectModel: (String) -> Unit
) {
    val modelColors = mapOf(
        "gemini" to Color(0xFF00E5FF),
        "chatgpt" to Color(0xFF10A37F),
        "claude" to Color(0xFFE0D4C3),
        "grok" to Color(0xFFFF6D00)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        // App header inside sidebar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = IconSparkles,
                contentDescription = null,
                tint = modelColors[selectedModel] ?: MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )
            Text(
                text = "ARI ENGINE HUB",
                style = TextStyle(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Persona selectors to quickly configure conversational flavor
        Text(
            text = "AI Personality Core",
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Responsive grid of models
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val personalities = listOf(
                "gemini" to "ARI Standard Gemini Core",
                "chatgpt" to "Structured Conversationalist",
                "claude" to "Expert Developer Brain",
                "grok" to "Sarcastic Intellectual"
            )

            personalities.forEach { (model, description) ->
                val primaryColor = modelColors[model] ?: Color.Cyan
                val isSelected = selectedModel == model
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) primaryColor.copy(alpha = 0.15f) else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) primaryColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            onSelectModel(model)
                            onNewChat(model)
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(primaryColor)
                    )
                    Column {
                        Text(
                            text = model.replaceFirstChar { it.uppercase() },
                            style = TextStyle(
                                color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        )
                        Text(
                            text = description,
                            style = TextStyle(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Conversations History",
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            
            IconButton(
                onClick = { onNewChat(selectedModel) },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Conversation",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Scrollable Chat History List
        if (conversations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No history. Initiate a prompt to begin first chat log.",
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(conversations) { item ->
                    val isActive = item.id == activeId
                    val itemAccent = modelColors[item.modelType.lowercase()] ?: MaterialTheme.colorScheme.primary
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isActive) itemAccent.copy(alpha = 0.1f) else Color.Transparent)
                            .border(
                                width = 0.5.dp,
                                color = if (isActive) itemAccent else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelect(item.id) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(itemAccent)
                            )
                            Text(
                                text = item.title,
                                style = TextStyle(
                                    fontSize = 13.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isActive) itemAccent else MaterialTheme.colorScheme.onBackground
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        
                        IconButton(
                            onClick = { onDelete(item.id) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove Conversation",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- Empty Chat State Greeting display with Quick Action prompts ---
@Composable
fun ChatIntroDisplay(
    modelName: String,
    onQuickPrompt: (String) -> Unit,
    isImageMode: Boolean,
    onChangeMode: (Boolean) -> Unit,
    accentColor: Color
) {
    val introductionText = when (modelName.lowercase()) {
        "grok" -> "I am ARI, running with Grok's witty intelligence. Let me share some sarcastic wisdom, review lines of complex python code, or visualize wacky art pieces."
        "claude" -> "I am ARI, configured with Claude's precise reasoning logic. Ready to tackle algorithm structures in c++, java, or elaborate on complex theoretical scenarios."
        "chatgpt" -> "I am ARI, running with ChatGPT's ultra-helpful system prompt. Let's create long summaries, build code step-by-step, or generate creative visual files!"
        else -> "I am ARI, an advanced all-purpose AI Assistant with direct Gemini core. Ask me anything: multi-language coding, logical reasoning, document translation, visual illustrations, and beyond."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Core Logo icon with floating anim
        val infiniteTransition = rememberInfiniteTransition(label = "logo float")
        val floatOffsetY by infiniteTransition.animateFloat(
            initialValue = -5f,
            targetValue = 5f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = EaseInOutQuad),
                repeatMode = RepeatMode.Reverse
            ),
            label = "offsetY"
        )

        Box(
            modifier = Modifier
                .offset(y = floatOffsetY.dp)
                .size(96.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.radialGradient(colors = listOf(accentColor.copy(alpha = 0.3f), Color.Transparent)))
                .border(2.dp, accentColor, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = com.example.R.drawable.ari_logo_1781427693250),
                contentDescription = "ARI AI Assistant",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Welcome to ARI",
            style = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = introductionText,
            style = TextStyle(
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            ),
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Toggle generation mode
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { onChangeMode(false) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!isImageMode) accentColor else Color.Transparent,
                    contentColor = if (!isImageMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text("Chat/Coding Mode", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold))
            }
            
            Button(
                onClick = { onChangeMode(true) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isImageMode) accentColor else Color.Transparent,
                    contentColor = if (isImageMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text("AI Painter Mode", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold))
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Quick Suggestbox Options
        Text(
            text = "Suggested starting points:",
            style = TextStyle(
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(10.dp))

        val prompts = if (isImageMode) {
            listOf(
                "A futuristic neon cybernetic cities, glowing synthwave skies, highly detailed illustration",
                "Cute cosmic hamster floating inside space, wearing tiny bubble helmet on, digital art 3d",
                "A vintage steampunk explorer mechanical pocket watch on dark rustic velvet, watercolor style"
            )
        } else {
            listOf(
                "Write a fully optimized quicksort algorithm in Python with syntax descriptions",
                "Design a simple responsive Java Class for banking balances, with custom transactions logic",
                "Solve: If a clock strikes 12, how many times will its hands overlay during a 24-hour day?"
            )
        }

        prompts.forEach { prompt ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { onQuickPrompt(prompt) }
                    .padding(14.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isImageMode) IconPhoto else IconSparkles,
                        contentDescription = null,
                        tint = accentColor.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = prompt,
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 16.sp
                        )
                    )
                }
            }
        }
    }
}

// --- Typing / Generation Loader indicator ---
@Composable
fun typingIndicator(accentColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "ARI is thinking",
                    style = TextStyle(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val transition = rememberInfiniteTransition(label = "dots")
                    for (i in 0..2) {
                        val delay = i * 200
                        val alpha by transition.animateFloat(
                            initialValue = 0.2f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(600, delayMillis = delay, easing = EaseInOutSine),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "dotAlpha$i"
                        )
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = alpha))
                        )
                    }
                }
            }
        }
    }
}

// --- Individual Message bubble rendering (with formatting representation) ---
@Composable
fun MessageBubble(
    message: Message,
    speakerId: Long?,
    onSpeakToggle: () -> Unit,
    activeAccentColor: Color
) {
    val isUser = message.sender == "user"
    val bubbleBg = if (isUser) {
        activeAccentColor.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val bubbleBorder = if (isUser) {
        activeAccentColor.copy(alpha = 0.6f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    }

    val alignment = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (isUser) activeAccentColor else Color.LightGray)
            )
            Text(
                text = if (isUser) "You" else "ARI Assistant",
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(bubbleBg)
                .border(
                    width = 1.dp,
                    color = bubbleBorder,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .widthIn(max = 320.dp)
                .padding(14.dp)
        ) {
            Column {
                if (message.isImage && message.imageData != null) {
                    val decodedBytes = Base64.decode(message.imageData, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    
                    var showLightbox by remember { mutableStateOf(false) }

                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Visual attachment",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                .clickable { showLightbox = true },
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (showLightbox && bitmap != null) {
                        Dialog(onDismissRequest = { showLightbox = false }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.Black)
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Image Preview",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                                IconButton(
                                    onClick = { showLightbox = false },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                                }
                            }
                        }
                    }
                }

                FormattedMarkdownText(text = message.text, activeAccentColor = activeAccentColor)
            }
        }

        if (!isUser) {
            Row(
                modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isSpeaking = speakerId == message.id

                IconButton(
                    onClick = onSpeakToggle,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isSpeaking) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = "Text to Speech Readout",
                        tint = if (isSpeaking) activeAccentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                val clipboardManager = LocalClipboardManager.current
                val context = LocalContext.current
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(message.text))
                        Toast.makeText(context, "Copied content to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy message",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// --- Custom Code Highlighter parser to render high-level code structures (python, c++, java) recursively ---
@Composable
fun FormattedMarkdownText(
    text: String,
    activeAccentColor: Color = MaterialTheme.colorScheme.primary
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val blocks = text.split("```")

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        blocks.forEachIndexed { index, part ->
            if (index % 2 == 1) {
                val lines = part.trim().split("\n")
                val rawLanguage = lines.firstOrNull()?.trim() ?: ""
                val codeContent = if (lines.size > 1) {
                    lines.drop(1).joinToString("\n")
                } else {
                    part
                }

                val normalizedLang = when (rawLanguage.lowercase()) {
                    "py", "python" -> "Python"
                    "cpp", "c++" -> "C++"
                    "java" -> "Java"
                    "kotlin", "kt" -> "Kotlin"
                    "js", "javascript" -> "JavaScript"
                    else -> rawLanguage.replaceFirstChar { it.uppercase() }.ifEmpty { "Source Code" }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F121D))
                        .border(1.dp, Color(0xFF1E2841), RoundedCornerShape(8.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF181C2B))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = normalizedLang,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E5FF)
                            )
                        )
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(codeContent))
                                Toast.makeText(context, "Copied code block!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy code",
                                tint = Color.LightGray,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    Text(
                        text = codeContent,
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color(0xFFE2E8F0),
                            lineHeight = 16.sp
                        ),
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(12.dp)
                    )
                }
            } else {
                if (part.isNotEmpty()) {
                    MarkdownTextSegment(
                        part = part,
                        accentColor = activeAccentColor,
                        onBackground = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}

@Composable
fun MarkdownTextSegment(part: String, accentColor: Color, onBackground: Color) {
    val lines = part.split("\n")
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        lines.forEach { rawLine ->
            val trimLine = rawLine.trim()
            when {
                trimLine.isEmpty() -> {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                trimLine.startsWith("# ") -> {
                    val headerText = trimLine.substring(2).trim()
                    Text(
                        text = remember(headerText) { parseInlineMarkdown(headerText, accentColor) },
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = accentColor,
                            lineHeight = 24.sp
                        ),
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                
                trimLine.startsWith("## ") -> {
                    val headerText = trimLine.substring(3).trim()
                    Text(
                        text = remember(headerText) { parseInlineMarkdown(headerText, accentColor) },
                        style = TextStyle(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            lineHeight = 22.sp
                        ),
                        modifier = Modifier.padding(top = 6.dp, bottom = 3.dp)
                    )
                }
                
                trimLine.startsWith("### ") -> {
                    val headerText = trimLine.substring(4).trim()
                    Text(
                        text = remember(headerText) { parseInlineMarkdown(headerText, accentColor) },
                        style = TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = accentColor,
                            lineHeight = 19.sp
                        ),
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }

                trimLine.startsWith("#### ") -> {
                    val headerText = trimLine.substring(5).trim()
                    Text(
                        text = remember(headerText) { parseInlineMarkdown(headerText, accentColor) },
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            lineHeight = 18.sp
                        ),
                        modifier = Modifier.padding(top = 2.dp, bottom = 1.dp)
                    )
                }
                
                (trimLine.startsWith("* ") && !trimLine.startsWith("**")) || trimLine.startsWith("- ") || trimLine.startsWith("+ ") -> {
                    val bulletText = trimLine.substring(2).trim()
                    Row(
                        modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "• ",
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            ),
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(
                            text = remember(bulletText) { parseInlineMarkdown(bulletText, accentColor) },
                            style = TextStyle(
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                                lineHeight = 20.sp
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                trimLine.matches(Regex("^\\d+\\.\\s+.*")) -> {
                    val dotIdx = trimLine.indexOf('.')
                    val listNumber = trimLine.substring(0, dotIdx + 1)
                    val listText = trimLine.substring(dotIdx + 1).trim()
                    Row(
                        modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "$listNumber ",
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            ),
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(
                            text = remember(listText) { parseInlineMarkdown(listText, accentColor) },
                            style = TextStyle(
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                                lineHeight = 20.sp
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                else -> {
                    Text(
                        text = remember(rawLine) { parseInlineMarkdown(rawLine, accentColor) },
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                            lineHeight = 20.sp
                        )
                    )
                }
            }
        }
    }
}

// Robust parsing for **bold**, *italic*, and `inline code` text with character escaping support
fun findUnescapedChar(text: String, char: Char, startIndex: Int): Int {
    var index = text.indexOf(char, startIndex)
    while (index != -1) {
        var backslashCount = 0
        var checkIndex = index - 1
        while (checkIndex >= 0 && text[checkIndex] == '\\') {
            backslashCount++
            checkIndex--
        }
        if (backslashCount % 2 == 0) {
            return index
        }
        index = text.indexOf(char, index + 1)
    }
    return -1
}

fun findUnescapedString(text: String, token: String, startIndex: Int): Int {
    var index = text.indexOf(token, startIndex)
    while (index != -1) {
        var backslashCount = 0
        var checkIndex = index - 1
        while (checkIndex >= 0 && text[checkIndex] == '\\') {
            backslashCount++
            checkIndex--
        }
        if (backslashCount % 2 == 0) {
            return index
        }
        index = text.indexOf(token, index + 1)
    }
    return -1
}

fun unescapeProcessedText(text: String): String {
    val sb = java.lang.StringBuilder()
    var i = 0
    val len = text.length
    while (i < len) {
        val c = text[i]
        if (c == '\\' && i + 1 < len) {
            val next = text[i + 1]
            // Unescape common markdown-escaped and standard JSON/LaTeX-escaped characters:
            if (next == '\\' || next == '`' || next == '*' || next == '_' || 
                next == '{' || next == '}' || next == '[' || next == ']' || 
                next == '(' || next == ')' || next == '#' || next == '+' || 
                next == '-' || next == '.' || next == '!' || next == '$' || 
                next == '/' || next == '|') {
                sb.append(next)
                i += 2
            } else {
                sb.append(c)
                i++
            }
        } else {
            sb.append(c)
            i++
        }
    }
    return sb.toString()
}

fun parseInlineMarkdown(part: String, accentColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val length = part.length
        
        while (cursor < length) {
            val inlineCodeIndex = findUnescapedChar(part, '`', cursor)
            val boldIndex1 = findUnescapedString(part, "**", cursor)
            val boldIndex2 = findUnescapedString(part, "__", cursor)
            val italicIndex1 = findUnescapedChar(part, '*', cursor)
            val italicIndex2 = findUnescapedChar(part, '_', cursor)
            
            var closestType = -1 // 1: code, 2: bold, 3: italic
            var closestIndex = Int.MAX_VALUE
            var tokenLength = 0
            
            if (inlineCodeIndex != -1 && inlineCodeIndex < closestIndex) {
                closestType = 1
                closestIndex = inlineCodeIndex
                tokenLength = 1
            }
            if (boldIndex1 != -1 && boldIndex1 < closestIndex) {
                closestType = 2
                closestIndex = boldIndex1
                tokenLength = 2
            }
            if (boldIndex2 != -1 && boldIndex2 < closestIndex) {
                closestType = 2
                closestIndex = boldIndex2
                tokenLength = 2
            }
            if (italicIndex1 != -1 && italicIndex1 < closestIndex && (italicIndex1 + 1 >= length || part[italicIndex1 + 1] != '*')) {
                closestType = 3
                closestIndex = italicIndex1
                tokenLength = 1
            }
            if (italicIndex2 != -1 && italicIndex2 < closestIndex && (italicIndex2 + 1 >= length || part[italicIndex2 + 1] != '_')) {
                closestType = 3
                closestIndex = italicIndex2
                tokenLength = 1
            }
            
            if (closestIndex == Int.MAX_VALUE) {
                append(unescapeProcessedText(part.substring(cursor)))
                break
            }
            
            append(unescapeProcessedText(part.substring(cursor, closestIndex)))
            cursor = closestIndex + tokenLength
            
            when (closestType) {
                1 -> {
                    val closingIndex = findUnescapedChar(part, '`', cursor)
                    if (closingIndex != -1) {
                        val codeText = part.substring(cursor, closingIndex)
                        withStyle(
                            style = SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                color = accentColor,
                                background = accentColor.copy(alpha = 0.1f),
                                fontSize = 13.sp
                            )
                        ) {
                            append(codeText)
                        }
                        cursor = closingIndex + 1
                    } else {
                        append("`")
                    }
                }
                2 -> {
                    val closingToken = if (part[closestIndex] == '*') "**" else "__"
                    val closingIndex = findUnescapedString(part, closingToken, cursor)
                    if (closingIndex != -1) {
                        val boldText = part.substring(cursor, closingIndex)
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color.White)) {
                            append(unescapeProcessedText(boldText))
                        }
                        cursor = closingIndex + 2
                    } else {
                        append(closingToken)
                    }
                }
                3 -> {
                    val closingToken = part[closestIndex].toString()
                    val closingIndex = findUnescapedChar(part, part[closestIndex], cursor)
                    if (closingIndex != -1) {
                        val italicText = part.substring(cursor, closingIndex)
                        withStyle(style = SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) {
                            append(unescapeProcessedText(italicText))
                        }
                        cursor = closingIndex + 1
                    } else {
                        append(closingToken)
                    }
                }
            }
        }
    }
}

// --- Action Input Hub at Bottom ---
@Composable
fun InputBarArea(
    inputText: String,
    onTextChanged: (String) -> Unit,
    attachedBitmap: Bitmap?,
    onRemoveBitmap: () -> Unit,
    isGenerating: Boolean,
    isListening: Boolean,
    isImageGenerationMode: Boolean,
    onToggleGenerationMode: (Boolean) -> Unit,
    onAttachGallery: () -> Unit,
    onAttachCamera: () -> Unit,
    onTriggerMic: () -> Unit,
    onSubmit: () -> Unit,
    accentColor: Color
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        if (attachedBitmap != null) {
            Box(
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.5.dp, accentColor, RoundedCornerShape(12.dp))
            ) {
                Image(
                    bitmap = attachedBitmap.asImageBitmap(),
                    contentDescription = "Attachment Image to Prompt",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                IconButton(
                    onClick = onRemoveBitmap,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(20.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove attached picture",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onAttachGallery,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = IconPhoto,
                        contentDescription = "Attach image from library",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onAttachCamera,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = IconCamera,
                        contentDescription = "Snap camera picture",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = onTextChanged,
                placeholder = {
                    Text(
                        text = if (isImageGenerationMode) "Describe the painting prompt..." else "Message ARI / paste code...",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(max = 120.dp)
                    .testTag("prompt_text_field"),
                shape = RoundedCornerShape(20.dp),
                textStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    onSubmit()
                    keyboardController?.hide()
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                trailingIcon = {
                    IconButton(
                        onClick = { onToggleGenerationMode(!isImageGenerationMode) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = IconSparkles,
                            contentDescription = "Activate AI Art Core",
                            tint = if (isImageGenerationMode) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            )

            val micBg = if (isListening) Color.Red.copy(alpha = 0.15f) else Color.Transparent
            val micBorder = if (isListening) Color.Red else Color.Transparent
            IconButton(
                onClick = onTriggerMic,
                modifier = Modifier
                    .testTag("dictate_button")
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(micBg)
                    .border(1.dp, micBorder, CircleShape)
            ) {
                if (isListening) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 0.8f,
                        targetValue = 1.2f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, easing = EaseInOutSine),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale"
                    )
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color.Red)
                            .offset(y = (scale * 2).dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Dictate Speech text",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            IconButton(
                onClick = {
                    onSubmit()
                    keyboardController?.hide()
                },
                enabled = !isGenerating && (inputText.trim().isNotEmpty() || attachedBitmap != null),
                modifier = Modifier
                    .testTag("send_button")
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        if (inputText.trim().isNotEmpty() || attachedBitmap != null) {
                            accentColor
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Submit prompted request",
                    tint = if (inputText.trim().isNotEmpty() || attachedBitmap != null) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    },
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// --- Interactive Full Screen Voice vocal overlay "Talk with AI" mode ---
@Composable
fun InteractiveVocalOverlay(
    viewModel: ChatViewModel,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val systemSpeaking = viewModel.speakingMessageId != null
    val userListening = viewModel.isListening

    LaunchedEffect(systemSpeaking) {
        if (!systemSpeaking && !userListening) {
            viewModel.toggleVoiceListening(context)
        }
    }

    val activeMessages by viewModel.activeMessages.collectAsState()
    LaunchedEffect(activeMessages.size) {
        val lastMsg = activeMessages.lastOrNull()
        if (lastMsg != null && lastMsg.sender == "assistant" && !viewModel.isGenerating) {
            viewModel.toggleSpeakMessage(lastMsg)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE6090C15))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxHeight()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .testTag("close_voice_overlay")
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Leave Voice Mode",
                        tint = Color.White
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (userListening) "ARI is listening..." else if (viewModel.isGenerating) "ARI is reasoning..." else "Speaking to you...",
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.height(80.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val count = 8
                    val infiniteTransition = rememberInfiniteTransition(label = "audioWave")
                    
                    for (i in 0 until count) {
                        val duration = 400 + (i * 80)
                        val animHeight by infiniteTransition.animateFloat(
                            initialValue = 10f,
                            targetValue = if (userListening || systemSpeaking) 75f else 15f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(duration, easing = EaseInOutSine),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "waveHeight$i"
                        )

                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .height(animHeight.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            accentColor,
                                            accentColor.copy(alpha = 0.4f)
                                        )
                                    )
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    val feedbackText = if (viewModel.currentInputText.isNotEmpty()) {
                        viewModel.currentInputText
                    } else if (activeMessages.isNotEmpty()) {
                        activeMessages.last().text
                    } else {
                        "Start talking! Press the central circular trigger to speak aloud your question."
                    }

                    Text(
                        text = feedbackText,
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = Color(0xFFE2E8F0),
                            lineHeight = 20.sp,
                            textAlign = TextAlign.Center
                        ),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                val actionBtnBg = if (userListening) Color.Red else accentColor
                IconButton(
                    onClick = {
                        if (userListening) {
                            viewModel.stopListening()
                            viewModel.submitPrompt()
                        } else {
                            viewModel.stopSpeaking()
                            viewModel.toggleVoiceListening(context)
                        }
                    },
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(actionBtnBg)
                        .border(4.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (userListening) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "Trigger Speech Voice",
                        tint = Color.Black,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = if (userListening) "Tap to send query" else "Tap to dictate input",
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}
