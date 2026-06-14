package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class MessageBlock {
    data class Text(val content: String) : MessageBlock()
    data class Code(val language: String, val code: String) : MessageBlock()
}

object ChatParser {
    /**
     * Parse text containing block fences into sequential code/text nodes
     */
    fun parseMessage(text: String): List<MessageBlock> {
        val blocks = mutableListOf<MessageBlock>()
        var currentIndex = 0
        val regex = Regex("```([a-zA-Z0-9#+xX-]*)\\n([\\s\\S]*?)\\n```")

        val matches = regex.findAll(text)
        for (match in matches) {
            val startIndex = match.range.first
            val endIndex = match.range.last + 1

            if (startIndex > currentIndex) {
                val plainText = text.substring(currentIndex, startIndex)
                if (plainText.isNotEmpty()) {
                    blocks.add(MessageBlock.Text(plainText))
                }
            }

            val language = match.groupValues[1].trim()
            val code = match.groupValues[2]
            blocks.add(MessageBlock.Code(language.ifEmpty { "code" }, code))
            currentIndex = endIndex
        }

        if (currentIndex < text.length) {
            val remainingText = text.substring(currentIndex)
            if (remainingText.isNotEmpty()) {
                blocks.add(MessageBlock.Text(remainingText))
            }
        }

        if (blocks.isEmpty()) {
            blocks.add(MessageBlock.Text(text))
        }

        return blocks
    }
}

/**
 * Custom-styled terminal-shell layout displaying formatted, high-contrast highlighted code files.
 */
@Composable
fun CodeBlockCard(
    language: String,
    code: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isCopied by remember { mutableStateOf(false) }

    val formattedCode = remember(code, language) {
        buildAnnotatedString {
            val lines = code.split("\n")
            lines.forEachIndexed { index, line ->
                // Basic Highlighting parser for Python, C++, Java, Kotlin
                var i = 0
                while (i < line.length) {
                    when {
                        // 1. Comments highlighting (Grey)
                        line.startsWith("#", i) || line.startsWith("//", i) -> {
                            withStyle(style = SpanStyle(color = Color(0xFF6A737D), fontFamily = FontFamily.Monospace)) {
                                append(line.substring(i))
                            }
                            break
                        }
                        // 2. String literal highlights (Soft Green)
                        line[i] == '"' || line[i] == '\'' -> {
                            val quoteChar = line[i]
                            val quoteEnd = line.indexOf(quoteChar, i + 1)
                            if (quoteEnd != -1) {
                                withStyle(style = SpanStyle(color = Color(0xFF9ECE6A), fontFamily = FontFamily.Monospace)) {
                                    append(line.substring(i, quoteEnd + 1))
                                }
                                i = quoteEnd + 1
                            } else {
                                withStyle(style = SpanStyle(color = Color(0xFF9ECE6A), fontFamily = FontFamily.Monospace)) {
                                    append(line.substring(i))
                                }
                                break
                            }
                        }
                        // 3. Numbers (Pink-orange)
                        line[i].isDigit() -> {
                            var endDigit = i
                            while (endDigit < line.length && (line[endDigit].isDigit() || line[endDigit] == '.')) {
                                endDigit++
                            }
                            withStyle(style = SpanStyle(color = Color(0xFFFF9E64), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)) {
                                append(line.substring(i, endDigit))
                            }
                            i = endDigit
                        }
                        // 4. Word boundary check for coding keywords
                        else -> {
                            var j = i
                            while (j < line.length && (line[j].isLetterOrDigit() || line[j] == '_')) {
                                j++
                            }
                            if (j > i) {
                                val word = line.substring(i, j)
                                // Core high level keywords for C++, Python, Java, Kotlin
                                val isKeyword = word in listOf(
                                    "def", "class", "import", "from", "return", "if", "else", "elif", "for", "while",
                                    "in", "print", "public", "private", "protected", "static", "void", "void", "int", "double", "float",
                                    "char", "boolean", "bool", "fun", "val", "var", "override", "package", "include", "using", "namespace",
                                    "cout", "cin", "endl", "std", "struct", "class", "new", "this", "try", "catch", "throw", "null", "true", "false"
                                )
                                val isType = word in listOf("String", "Int", "Double", "Float", "Boolean", "List", "Map", "ArrayList", "vector")

                                val style = when {
                                    isKeyword -> SpanStyle(color = Color(0xFFBB9AF3), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    isType -> SpanStyle(color = Color(0xFF7AA2F7), fontFamily = FontFamily.Monospace)
                                    else -> SpanStyle(color = Color(0xFFA9B1D6), fontFamily = FontFamily.Monospace)
                                }

                                withStyle(style = style) {
                                    append(word)
                                }
                                i = j
                            } else {
                                // Default operator or whitespace symbol
                                withStyle(style = SpanStyle(color = Color(0xFFA9B1D6), fontFamily = FontFamily.Monospace)) {
                                    append(line[i].toString())
                                }
                                i++
                            }
                        }
                    }
                }
                if (index < lines.size - 1) {
                    append("\n")
                }
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(1.dp, CosmicBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1423)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF161E35))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Small circular console dot accents
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(8.dp).background(Color(0xFFFF5F56), RoundedCornerShape(50)))
                        Box(Modifier.size(8.dp).background(Color(0xFFFFBD2E), RoundedCornerShape(50)))
                        Box(Modifier.size(8.dp).background(Color(0xFF27C93F), RoundedCornerShape(50)))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = language.uppercase(),
                        color = CosmicTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("ARI Generated Code", code)
                        clipboard.setPrimaryClip(clip)
                        isCopied = true
                        coroutineScope.launch {
                            delay(2000)
                            isCopied = false
                        }
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isCopied) Icons.Filled.Done else Icons.Outlined.ContentCopy,
                        contentDescription = "Copy code",
                        tint = if (isCopied) CosmicCyan else CosmicTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Scrollable terminal console
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .horizontalScroll(rememberScrollState())
            ) {
                // Line counts
                Column(modifier = Modifier.padding(end = 12.dp)) {
                    val linesCount = code.split("\n").size
                    (1..linesCount).forEach { num ->
                        Text(
                            text = num.toString().padStart(2),
                            color = CosmicTextTertiary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.height(18.dp)
                        )
                    }
                }

                // Color Highlighted Text Block
                Text(
                    text = formattedCode,
                    style = TextStyle(
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
