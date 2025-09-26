package io.engst.cleo

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

val DEFAULT_SYSTEM_PROMPT =
   """
    You are a helpful assistant. Be brief by default.
    Always speak as Assistant ("I" = Assistant, "you" = User).
    If the message contains first-person ("I"), assume it is the User speaking.
    Mirror the user's language and tone.
    """
      .trimIndent()

class MainActivity : ComponentActivity() {

   private var llmInference: LlmInference? = null

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      enableEdgeToEdge()

      if (externalMediaDirs.firstOrNull()?.exists() == false) {
         externalMediaDirs.firstOrNull()?.mkdirs()
      }
      val modelFile = File(externalMediaDirs.firstOrNull(), "gemma3-1b-it-int4.task")

      var initError: String? = null
      if (modelFile.exists() && modelFile.length() > 0L) {
         try {
            val options =
               LlmInference.LlmInferenceOptions.builder().setModelPath(modelFile.absolutePath)
                  .build()
            llmInference = LlmInference.createFromOptions(this, options)
         } catch (t: Throwable) {
            initError = "LLM init failed: ${t.message}"
         }
      } else {
         initError = "Model not found at ${modelFile.absolutePath}"
      }

      setContent {
         val context = LocalContext.current
         val colorScheme =
            if (isSystemInDarkTheme()) dynamicLightColorScheme(context)
            else dynamicLightColorScheme(context)
         MaterialTheme(colorScheme = colorScheme) {
            ChatScreen(
               ready = llmInference != null,
               status = initError ?: "CLEO",
               onGenerate = { fullPrompt -> generateOffMain(fullPrompt) },
            )
         }
      }
   }

   override fun onDestroy() {
      llmInference?.close()
      llmInference = null
      super.onDestroy()
   }

   /** Off-main generation. Accepts the prebuilt, context-augmented prompt. */
   private suspend fun generateOffMain(fullPrompt: String): String =
      withContext(Dispatchers.IO) {
         val engine = llmInference ?: return@withContext "LLM not initialized."
         try {
            Log.i("bexx", "prompt: $fullPrompt")
            engine.generateResponse(fullPrompt).also { Log.i("bexx", "response: $it") }
         } catch (t: Throwable) {
            "Generation failed: ${t.message}"
         }
      }
}

private enum class Role {
   User,
   Assistant,
}

private data class ChatMessage(
   val role: Role,
   val text: String,
   val timestamp: Long = System.currentTimeMillis(),
)

/** Build a single prompt string that includes system prompt + conversation history. */
private fun buildChatPrompt(systemPrompt: String, history: List<ChatMessage>): String {
   return buildString {
      appendLine(systemPrompt)
      history.forEach { msg ->
         val role = if (msg.role == Role.User) "USER" else "ASSISTANT"
         appendLine("$role: ${msg.text}")
      }
      appendLine("ASSISTANT:")
   }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatScreen(ready: Boolean, status: String, onGenerate: suspend (String) -> String) {
   var input by remember { mutableStateOf("") }
   var isRunning by remember { mutableStateOf(false) }
   val messages = remember { mutableStateListOf<ChatMessage>() }
   val scope = rememberCoroutineScope()

   var systemPrompt by remember { mutableStateOf(DEFAULT_SYSTEM_PROMPT) }
   var tempSystemPrompt by rememberSaveable(systemPrompt) { mutableStateOf(systemPrompt) }
   var showSystemEditor by remember { mutableStateOf(false) }

   if (showSystemEditor) {
      AlertDialog(
         onDismissRequest = { showSystemEditor = false },
         title = { Text("System Prompt") },
         text = {
            OutlinedTextField(
               value = tempSystemPrompt,
               onValueChange = { tempSystemPrompt = it },
               singleLine = false,
               minLines = 6,
               modifier = Modifier.fillMaxWidth(),
            )
         },
         confirmButton = {
            TextButton(
               onClick = {
                  systemPrompt = tempSystemPrompt
                  showSystemEditor = false
               }
            ) {
               Text("Save")
            }
         },
         dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
               TextButton(onClick = { tempSystemPrompt = DEFAULT_SYSTEM_PROMPT }) { Text("Reset") }
               TextButton(onClick = { showSystemEditor = false }) { Text("Cancel") }
            }
         },
      )
   }

   Scaffold(
      topBar = {
         CenterAlignedTopAppBar(
            title = { Text(status) },
            navigationIcon = {
               IconButton(
                  onClick = {
                     tempSystemPrompt = systemPrompt
                     showSystemEditor = true
                  }
               ) {
                  Icon(Icons.Default.EditNote, contentDescription = "Edit System Prompt")
               }
            },
            actions = {
               IconButton(
                  enabled = messages.isNotEmpty() && !isRunning,
                  onClick = { messages.clear() },
               ) {
                  Icon(Icons.Default.ClearAll, contentDescription = "Delete conversation")
               }
            },
         )
      },
      bottomBar = {
         Row(
            Modifier
               .fillMaxWidth()
               .safeDrawingPadding()
               .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
         ) {
            // Shared send action used by both button and Shift+Enter
            fun sendMessage() {
               val prompt = input.trim()
               if (prompt.isNotEmpty()) {
                  messages += ChatMessage(Role.User, prompt)
                  input = ""
                  isRunning = true
                  scope.launch {
                     val reply = onGenerate(buildChatPrompt(systemPrompt, messages))
                     messages += ChatMessage(Role.Assistant, reply)
                     isRunning = false
                  }
               }
            }

            OutlinedTextField(
               value = input,
               onValueChange = { input = it },
               modifier =
                  Modifier
                     .weight(1f)
                     .onPreviewKeyEvent { event ->
                        if (
                           event.type == KeyEventType.KeyDown &&
                           event.key == Key.Enter &&
                           event.isShiftPressed
                        ) {
                           if (ready && input.isNotBlank() && !isRunning) {
                              sendMessage()
                           }
                           true // consume to avoid newline
                        } else {
                           false
                        }
                     },
               placeholder = { Text("Ask anything...") },
               singleLine = false,
               enabled = ready && !isRunning,
            )
            IconButton(
               enabled = ready && input.isNotBlank() && !isRunning,
               onClick = { sendMessage() },
            ) {
               Icon(Icons.AutoMirrored.Filled.Send, null)
            }
         }
      },
   ) { inner ->
      val listState = rememberLazyListState()

      LaunchedEffect(messages.size) {
         if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
         }
      }
      LazyColumn(
         modifier =
            Modifier
               .padding(inner)
               .fillMaxSize()
               .padding(horizontal = 16.dp, vertical = 8.dp),
         verticalArrangement = Arrangement.spacedBy(12.dp),
         state = listState,
      ) {
         items(messages, key = { it.timestamp }) { msg ->
            Box(Modifier.fillMaxWidth()) {
               when (msg.role) {
                  Role.User ->
                     UserBubble(
                        msg.text,
                        modifier = Modifier
                           .align(Alignment.CenterStart)
                           .fillMaxWidth(0.9f),
                     )

                  Role.Assistant ->
                     AssistantBubble(
                        msg.text,
                        modifier = Modifier
                           .align(Alignment.CenterEnd)
                           .fillMaxWidth(0.9f),
                     )
               }
            }
         }
         if (!ready || isRunning) {
            item(key = "progress") {
               Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                  CircularProgressIndicator()
               }
            }
         }
      }
   }
}

@Composable
private fun UserBubble(text: String, modifier: Modifier = Modifier) {
   Surface(
      color = MaterialTheme.colorScheme.primaryContainer,
      shape = MaterialTheme.shapes.medium,
      tonalElevation = 2.dp,
      modifier = modifier,
   ) {
      Column(Modifier.padding(12.dp)) {
         Text("You", style = MaterialTheme.typography.labelMedium)
         Spacer(Modifier.height(4.dp))
         Text(text)
      }
   }
}

@Composable
private fun AssistantBubble(text: String, modifier: Modifier = Modifier) {
   Surface(
      color = MaterialTheme.colorScheme.surfaceVariant,
      shape = MaterialTheme.shapes.medium,
      tonalElevation = 1.dp,
      modifier = modifier,
   ) {
      Column(Modifier.padding(12.dp)) {
         Text("Assistant", style = MaterialTheme.typography.labelMedium)
         Spacer(Modifier.height(4.dp))
         Text(text)
      }
   }
}
