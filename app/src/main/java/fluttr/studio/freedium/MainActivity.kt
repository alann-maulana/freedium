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
import androidx.lifecycle.lifecycleScope
import fluttr.studio.freedium.ui.theme.FreediumTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    /**
     * Medium URL shared via intent or typed in Medium-Bypass mode.
     * Rendered via WebViewScreen with the Freedium proxy prefix applied.
     */
    private val sharedUrl = mutableStateOf<String?>(null)

    /**
     * News URL from intent routing or typed in News Reader mode.
     * Rendered via ArticleReaderScreen (Jsoup parsing, no proxy).
     */
    private val newsReaderUrl = mutableStateOf<String?>(null)

    /**
     * Raw URL opened as a WebView fallback from ArticleReaderScreen.
     * Used *without* any Freedium proxy prefix.
     */
    private val fallbackBrowserUrl = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle URL if app was launched via Android share sheet
        handleIntent(intent)

        setContent {
            FreediumTheme {
                MainScreen(
                    sharedUrl = sharedUrl.value,
                    newsReaderUrl = newsReaderUrl.value,
                    fallbackBrowserUrl = fallbackBrowserUrl.value,
                    onMediumUrlCleared = {
                        sharedUrl.value = null
                        fallbackBrowserUrl.value = null
                    },
                    onNewsUrlCleared = { newsReaderUrl.value = null },
                    onMediumUrlSubmitted = { url -> sharedUrl.value = url },
                    onNewsUrlSubmitted = { url -> newsReaderUrl.value = url },
                    onOpenNewsInBrowser = { url ->
                        // User tapped "Open in Browser" from ArticleReaderScreen
                        newsReaderUrl.value = null
                        fallbackBrowserUrl.value = url
                    }
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
            val url = extractUrl(sharedText) ?: return
            routeUrl(url)
        }
    }

    /**
     * Routes an incoming [url] to the correct destination using [UrlDetector]:
     * - **Tier 1** (sync): known Medium hostnames → WebViewScreen immediately.
     * - **Tier 2** (async, ~1 s): HTML head fingerprint → WebViewScreen or ArticleReaderScreen.
     */
    private fun routeUrl(url: String) {
        // Tier 1 — instant sync path, no coroutine overhead
        if (UrlDetector.detectInstant(url) == UrlType.Medium) {
            sharedUrl.value = url
            return
        }

        // Tier 2 — async HTML fingerprint for unknown / custom domains
        lifecycleScope.launch {
            when (UrlDetector.detect(url)) {
                UrlType.Medium -> sharedUrl.value = url
                UrlType.News   -> newsReaderUrl.value = url
            }
        }
    }

    private fun extractUrl(text: String?): String? {
        if (text == null) return null
        val urlRegex = "(https?://\\S+)".toRegex()
        return urlRegex.find(text)?.value
    }
}

@Composable
fun MainScreen(
    sharedUrl: String?,
    newsReaderUrl: String?,
    fallbackBrowserUrl: String?,
    onMediumUrlCleared: () -> Unit,
    onNewsUrlCleared: () -> Unit,
    onMediumUrlSubmitted: (String) -> Unit,
    onNewsUrlSubmitted: (String) -> Unit,
    onOpenNewsInBrowser: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        // ── News Reader (highest priority — covers WebView if both are set) ──
        AnimatedVisibility(
            visible = newsReaderUrl != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (newsReaderUrl != null) {
                ArticleReaderScreen(
                    url = newsReaderUrl,
                    onClose = onNewsUrlCleared,
                    onOpenInBrowser = onOpenNewsInBrowser
                )
            }
        }

        // ── WebView — Medium (with Freedium proxy) or browser fallback ───────
        AnimatedVisibility(
            visible = (sharedUrl != null || fallbackBrowserUrl != null) && newsReaderUrl == null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val targetUrl = when {
                fallbackBrowserUrl != null -> fallbackBrowserUrl
                sharedUrl != null         -> "https://freedium-mirror.cfd/$sharedUrl"
                else                      -> return@AnimatedVisibility
            }
            WebViewScreen(
                url = targetUrl,
                onClose = onMediumUrlCleared
            )
        }

        // ── Home — shown when nothing else is active ──────────────────────────
        AnimatedVisibility(
            visible = sharedUrl == null && newsReaderUrl == null && fallbackBrowserUrl == null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            HomeScreen(
                onMediumUrlSubmitted = onMediumUrlSubmitted,
                onNewsUrlSubmitted = onNewsUrlSubmitted
            )
        }
    }
}