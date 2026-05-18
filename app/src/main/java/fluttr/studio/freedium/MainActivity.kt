package fluttr.studio.freedium

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import fluttr.studio.freedium.ui.theme.FreediumTheme

class MainActivity : ComponentActivity() {

    private val sharedUrl = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle incoming intent if app was launched via share sheet
        handleIntent(intent)

        setContent {
            FreediumTheme {
                MainScreen(
                    sharedUrl = sharedUrl.value,
                    onUrlCleared = { sharedUrl.value = null },
                    onUrlSubmitted = { url -> sharedUrl.value = url }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            val extracted = extractUrl(sharedText)
            if (extracted != null) {
                sharedUrl.value = extracted
            }
        }
    }

    private fun extractUrl(text: String?): String? {
        if (text == null) return null
        val urlRegex = "(https?://\\S+)".toRegex()
        val match = urlRegex.find(text)
        return match?.value
    }
}

@Composable
fun MainScreen(
    sharedUrl: String?,
    onUrlCleared: () -> Unit,
    onUrlSubmitted: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        AnimatedVisibility(
            visible = sharedUrl != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (sharedUrl != null) {
                // Prepend base Freedium mirror URL
                val targetUrl = "https://freedium-mirror.cfd/$sharedUrl"
                WebViewScreen(
                    url = targetUrl,
                    onClose = onUrlCleared
                )
            }
        }

        AnimatedVisibility(
            visible = sharedUrl == null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            HomeScreen(onUrlSubmitted = onUrlSubmitted)
        }
    }
}