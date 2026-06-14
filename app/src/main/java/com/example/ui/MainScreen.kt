package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.database.Conversation
import com.example.data.database.Message
import com.example.ui.components.ChatParser
import com.example.ui.components.CodeBlockCard
import com.example.ui.components.MessageBlock
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: ChatViewModel = viewModel(factory = ChatViewModelFactory(LocalContext.current.applicationContext as android.app.Application))
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Observe state variables
    val conversationList by viewModel.conversations.collectAsState()
    val activeConvId by viewModel.activeConversationId.collectAsState()
    val messagesList by viewModel.activeMessages.collectAsState()

    val currentConversation = conversationList.find { it.id == activeConvId }

    // Synchronize local active model with current conversation configuration
    LaunchedEffect(currentConversation) {
        currentConversation?.let {
            viewModel.selectedModel = it.modelType
        }
    }

    // Modern contracts for Camera photo click and Gallery visual picker
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let { viewModel.attachBitmap(it) }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val bitmap = android.graphics.BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        viewModel.attachBitmap(bitmap)
                    } else {
                        Toast.makeText(context, "Unsupported photo, please pick a JPEG/PNG asset.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error reading image file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch()
        } else {
            Toast.makeText(context, "Camera permission needed to click photos for ARI.", Toast.LENGTH_LONG).show()
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleVoiceListening(context)
        } else {
            Toast.makeText(context, "Audio permission needed for voice typing.", Toast.LENGTH_LONG).show()
        }
    }

    // Modal Drawer Wrapper to hold historical conversation sessions
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(310.dp),
                drawerContainerColor = CosmicDarkBg,
                drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Drawer Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(CosmicCyan, CosmicBlue, CosmicPurple)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "A",
                                color = CosmicDarkBg,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "ARI ARCHIVES",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = CosmicTextPrimary,
                            letterSpacing = 1.sp
                        )
                    }

                    Divider(color = CosmicBorder, modifier = Modifier.padding(bottom = 12.dp))

                    // New Conversation Hub
                    Button(
                        onClick = {
                            viewModel.startNewConversation()
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CosmicBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_chat_button")
                            .height(48.dp)
                    ) {
                        Icon(Icons.Filled.Add, "New Chat")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create New Session", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "PREVIOUS LOGS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CosmicTextTertiary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    // History Loop List
                    Box(modifier = Modifier.weight(1f)) {
                        if (conversationList.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Outlined.ChatBubbleOutline,
                                    "No chats",
                                    tint = CosmicTextTertiary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "No chat logs found",
                                    color = CosmicTextSecondary,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(conversationList) { conv ->
                                    val isSelected = conv.id == activeConvId
                                    val modelAccentColor = when (conv.modelType.lowercase()) {
                                        "grok" -> CosmicGrokOrange
                                        "claude" -> CosmicPurple
                                        "chatgpt" -> CosmicBlue
                                        else -> CosmicCyan
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) CosmicBorder else Color.Transparent)
                                            .border(
                                                1.dp,
                                                if (isSelected) CosmicCyan else Color.Transparent,
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                viewModel.selectConversation(conv.id)
                                                coroutineScope.launch { drawerState.close() }
                                            }
                                            .padding(horizontal = 10.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Status Bullet badge with custom color tag representation
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(modelAccentColor)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = conv.title,
                                                color = if (isSelected) CosmicTextPrimary else CosmicTextSecondary,
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                viewModel.deleteCurrentConversation()
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                "Delete Log",
                                                tint = CosmicTextTertiary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom info label inside Drawer
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "ARI v3.5 Stable Brain • Gemini API Core",
                        fontSize = 11.sp,
                        color = CosmicTextTertiary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    ) {
        // Active accent color based on chosen Model Type
        val activeAccentColorByModel = when (viewModel.selectedModel.lowercase()) {
            "grok" -> CosmicGrokOrange
            "claude" -> CosmicPurple
            "chatgpt" -> CosmicBlue
            else -> CosmicCyan
        }

        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "ARI",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    color = CosmicTextPrimary
                                )

                                // Pulsing Active Status Orb
                                val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
                                val scaleAnim by infiniteTransition.animateFloat(
                                    initialValue = 0.85f,
                                    targetValue = 1.15f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1200, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "orb"
                                )
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .scale(scaleAnim)
                                        .clip(CircleShape)
                                        .background(activeAccentColorByModel)
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                                Icon(Icons.Filled.Menu, "Menu Drawer", tint = CosmicTextPrimary)
                            }
                        },
                        actions = {
                            if (activeConvId != null) {
                                IconButton(onClick = { viewModel.deleteCurrentConversation() }) {
                                    Icon(Icons.Filled.DeleteOutline, "Clear conversation", tint = CosmicTextSecondary)
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = CosmicDarkBg
                        )
                    )

                    // Horizontally scrollable quick-switcher capsule bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CosmicDarkBg)
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Brain engine:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CosmicTextTertiary,
                            modifier = Modifier.padding(end = 4.dp)
                        )

                        val listModelOptions = listOf(
                            Triple("gemini", "Gemini", CosmicCyan),
                            Triple("chatgpt", "ChatGPT", CosmicBlue),
                            Triple("claude", "Claude", CosmicPurple),
                            Triple("grok", "Grok (Beta)", CosmicGrokOrange)
                        )

                        listModelOptions.forEach { (modelKey, name, color) ->
                            val isActive = viewModel.selectedModel == modelKey
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isActive) color.copy(alpha = 0.22f) else Color(0xFF13192B))
                                    .border(
                                        1.dp,
                                        if (isActive) color else CosmicBorder,
                                        RoundedCornerShape(50)
                                    )
                                    .clickable {
                                        viewModel.selectedModel = modelKey
                                        // Auto-renew if active conversation was empty, or start new
                                        if (messagesList.isEmpty()) {
                                            viewModel.startNewConversation(modelKey)
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Text(
                                        text = name,
                                        color = if (isActive) CosmicTextPrimary else CosmicTextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            },
            containerColor = CosmicDarkBg
        ) { paddingValues ->
            val keyboardController = LocalSoftwareKeyboardController.current
            val lazyListState = rememberLazyListState()

            // Automatically scroll to bottom on new messages
            LaunchedEffect(messagesList.size) {
                if (messagesList.isNotEmpty()) {
                    lazyListState.animateScrollToItem(messagesList.size - 1)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Chat panel area
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        if (messagesList.isEmpty()) {
                            // Onboarding dashboard
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.radialGradient(
                                                listOf(
                                                    activeAccentColorByModel,
                                                    activeAccentColorByModel.copy(alpha = 0.2f),
                                                    Color.Transparent
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.AutoAwesome,
                                        contentDescription = "ARI",
                                        tint = activeAccentColorByModel,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                val modelTitleText = when (viewModel.selectedModel.lowercase()) {
                                    "grok" -> "I am ARI, spiced with Grok intelligence"
                                    "claude" -> "I am ARI, configured with Claude logic"
                                    "chatgpt" -> "I am ARI, featuring ChatGPT logic"
                                    else -> "I am ARI, your advanced Omni assistant"
                                }

                                Text(
                                    text = modelTitleText,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = CosmicTextPrimary,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Ask me anything. I write multi-language software (Python, C++, Java), compose realistic artwork, and answer reasoning prompts with high level intelligence.",
                                    fontSize = 12.sp,
                                    color = CosmicTextSecondary,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )

                                Spacer(modifier = Modifier.height(28.dp))

                                // Suggestion grids
                                Text(
                                    "TAP A SUGGESTION TO START",
                                    fontSize = 10.sp,
                                    letterSpacing = 1.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CosmicTextTertiary
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                val ideas = listOf(
                                    "🐍 Write Python code for Quicksort",
                                    "🎨 Generate image: futuristic neon forest",
                                    "🖥️ Explain Java memory model simply",
                                    "⚙️ Solve the C++ pointer reference puzzle"
                                )

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ideas.forEach { idea ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(CosmicCardBg)
                                                .border(1.dp, CosmicBorder, RoundedCornerShape(12.dp))
                                                .clickable {
                                                    viewModel.currentInputText = idea
                                                    // Auto-trigger image mode if they tap the generation suggest
                                                    if (idea.startsWith("🎨") || idea.contains("image")) {
                                                        viewModel.isImageGenerationMode = true
                                                    }
                                                }
                                                .padding(12.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    Icons.Outlined.Lightbulb,
                                                    null,
                                                    tint = activeAccentColorByModel,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = idea,
                                                    color = CosmicTextPrimary,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Messages stream
                            LazyColumn(
                                state = lazyListState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(messagesList) { msg ->
                                    val isUser = msg.sender == "user"
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                                    ) {
                                        if (!isUser) {
                                            // Avatar marker for ARI
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        Brush.linearGradient(
                                                            listOf(activeAccentColorByModel, CosmicBlue)
                                                        )
                                                    )
                                                    .padding(2.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Filled.AutoAwesome,
                                                    null,
                                                    tint = CosmicDarkBg,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }

                                        // Chat bubble Card
                                        Column(
                                            modifier = Modifier.weight(1f, fill = false),
                                            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                                        ) {
                                            Card(
                                                shape = RoundedCornerShape(
                                                    topStart = 16.dp,
                                                    topEnd = 16.dp,
                                                    bottomStart = if (isUser) 16.dp else 4.dp,
                                                    bottomEnd = if (isUser) 4.dp else 16.dp
                                                ),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isUser) Color(0xFF1E2E54) else CosmicCardBg
                                                ),
                                                modifier = Modifier.border(
                                                    1.dp,
                                                    if (isUser) Color(0xFF2E437C) else CosmicBorder,
                                                    RoundedCornerShape(
                                                        topStart = 16.dp,
                                                        topEnd = 16.dp,
                                                        bottomStart = if (isUser) 16.dp else 4.dp,
                                                        bottomEnd = if (isUser) 4.dp else 16.dp
                                                    )
                                                )
                                            ) {
                                                Column(modifier = Modifier.padding(14.dp)) {
                                                    // Image Renderer
                                                    if (msg.isImage && msg.imageData != null) {
                                                        val bitmap = remember(msg.imageData) {
                                                            try {
                                                                val decodedString = android.util.Base64.decode(msg.imageData, android.util.Base64.DEFAULT)
                                                                android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                                                            } catch (e: Exception) {
                                                                null
                                                            }
                                                        }
                                                        if (bitmap != null) {
                                                            Image(
                                                                bitmap = bitmap.asImageBitmap(),
                                                                contentDescription = "ARI Visual Asset",
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .heightIn(max = 260.dp)
                                                                    .clip(RoundedCornerShape(8.dp))
                                                                    .padding(bottom = 8.dp)
                                                            )
                                                        }
                                                    }

                                                    // Text Core with smart parser segmentation (text vs. code blocks)
                                                    val blocks = remember(msg.text) { ChatParser.parseMessage(msg.text) }
                                                    blocks.forEach { block ->
                                                        when (block) {
                                                            is MessageBlock.Text -> {
                                                                SelectionContainer {
                                                                    Text(
                                                                        text = block.content.trim(),
                                                                        color = CosmicTextPrimary,
                                                                        fontSize = 14.sp,
                                                                        lineHeight = 20.sp
                                                                    )
                                                                }
                                                            }
                                                            is MessageBlock.Code -> {
                                                                CodeBlockCard(language = block.language, code = block.code)
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            // Options for assistant messages (TTS voice playback control!)
                                            if (!isUser) {
                                                val isSpeakingThis = viewModel.speakingMessageId == msg.id
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                                                ) {
                                                    IconButton(
                                                        onClick = { viewModel.toggleSpeakMessage(msg) },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = if (isSpeakingThis) Icons.Default.VolumeUp else Icons.Outlined.VolumeUp,
                                                            contentDescription = "Read AloudVoice",
                                                            tint = if (isSpeakingThis) activeAccentColorByModel else CosmicTextTertiary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }

                                                    if (isSpeakingThis) {
                                                        Text(
                                                            "Speaking clean voice playback...",
                                                            fontSize = 10.sp,
                                                            color = activeAccentColorByModel,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(start = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        if (isUser) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF2E437C)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.Person,
                                                    null,
                                                    tint = CosmicTextPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Loading/Typing pulse
                    if (viewModel.isGenerating) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = activeAccentColorByModel,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "ARI is compiling answers...",
                                fontSize = 12.sp,
                                color = CosmicTextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Active Attachments preview card
                    if (viewModel.attachedImageBitmap != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CosmicDarkBg)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box {
                                Image(
                                    bitmap = viewModel.attachedImageBitmap!!.asImageBitmap(),
                                    contentDescription = "Attached photo preview",
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(2.dp, CosmicBorder, RoundedCornerShape(8.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .align(Alignment.TopEnd)
                                        .offset(x = 6.dp, y = (-6).dp)
                                        .clip(CircleShape)
                                        .background(Color.Red)
                                        .clickable { viewModel.clearDraftInput() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        "Remove Image",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "JPEG photo attached. Ready to upload in multi-modal prompt.",
                                fontSize = 11.sp,
                                color = CosmicTextSecondary
                            )
                        }
                    }

                    // Bottom glassmorphic input core
                    Surface(
                        color = Color(0xFF0F1322),
                        tonalElevation = 6.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CosmicBorder, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Camera trigger
                            IconButton(
                                onClick = {
                                    val cameraPermission = Manifest.permission.CAMERA
                                    if (ContextCompat.checkSelfPermission(context, cameraPermission) == PackageManager.PERMISSION_GRANTED) {
                                        cameraLauncher.launch()
                                    } else {
                                        cameraPermissionLauncher.launch(cameraPermission)
                                    }
                                },
                                colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF192036))
                            ) {
                                Icon(Icons.Default.PhotoCamera, "Snap Image", tint = CosmicTextPrimary)
                            }

                            // Album trigger
                            IconButton(
                                onClick = {
                                    galleryLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                },
                                colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF192036))
                            ) {
                                Icon(Icons.Default.PhotoLibrary, "Gallery Album", tint = CosmicTextPrimary)
                            }

                            // Sparkle toggle for Art Generation versus textual prompt mode
                            IconButton(
                                onClick = {
                                    viewModel.isImageGenerationMode = !viewModel.isImageGenerationMode
                                    if (viewModel.isImageGenerationMode) {
                                        Toast.makeText(context, "Art mode active. ARI will generate images!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Text/Reasoning mode active.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = if (viewModel.isImageGenerationMode) CosmicPurple.copy(alpha = 0.25f) else Color(0xFF192036)
                                ),
                                modifier = Modifier.border(
                                    1.dp,
                                    if (viewModel.isImageGenerationMode) CosmicPurple else Color.Transparent,
                                    CircleShape
                                )
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    "Art Mode Toggle",
                                    tint = if (viewModel.isImageGenerationMode) CosmicPurple else CosmicTextPrimary
                                )
                            }

                            // TextField core input dialog box
                            val glowColorForInput = if (viewModel.isImageGenerationMode) CosmicPurple else activeAccentColorByModel
                            TextField(
                                value = viewModel.currentInputText,
                                onValueChange = { viewModel.currentInputText = it },
                                placeholder = {
                                    Text(
                                        text = if (viewModel.isImageGenerationMode) "Describe art to generate..." else "Ask ARI anything...",
                                        fontSize = 13.sp,
                                        color = CosmicTextSecondary
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, glowColorForInput.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                                    .clip(RoundedCornerShape(24.dp))
                                    .testTag("prompt_input_field"),
                                maxLines = 4,
                                textStyle = TextStyle(fontSize = 13.sp, color = CosmicTextPrimary),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(onSend = {
                                    viewModel.submitPrompt()
                                    keyboardController?.hide()
                                }),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF13192B),
                                    unfocusedContainerColor = Color(0xFF13192B),
                                    disabledContainerColor = Color(0xFF13192B),
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )

                            // Microphone dictation toggle trigger
                            IconButton(
                                onClick = {
                                    val audioPermission = Manifest.permission.RECORD_AUDIO
                                    if (ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED) {
                                        viewModel.toggleVoiceListening(context)
                                    } else {
                                        audioPermissionLauncher.launch(audioPermission)
                                    }
                                },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = if (viewModel.isListening) CosmicCyan.copy(alpha = 0.22f) else Color(0xFF192036)
                                ),
                                modifier = Modifier.border(
                                    1.dp,
                                    if (viewModel.isListening) CosmicCyan else Color.Transparent,
                                    CircleShape
                                )
                            ) {
                                Icon(
                                    imageVector = if (viewModel.isListening) Icons.Filled.Mic else Icons.Outlined.Mic,
                                    contentDescription = "Voice Dictate Option",
                                    tint = if (viewModel.isListening) CosmicCyan else CosmicTextPrimary
                                )
                            }

                            // Fire arrow submission trigger button
                            IconButton(
                                onClick = {
                                    viewModel.submitPrompt()
                                    keyboardController?.hide()
                                },
                                enabled = viewModel.currentInputText.trim().isNotEmpty() || viewModel.attachedImageBase64 != null,
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = glowColorForInput,
                                    disabledContainerColor = Color(0xFF1E2841)
                                ),
                                modifier = Modifier.testTag("submit_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = "Submit Request",
                                    tint = if (viewModel.currentInputText.trim().isNotEmpty() || viewModel.attachedImageBase64 != null) CosmicDarkBg else CosmicTextTertiary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
