package fluttr.studio.freedium

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    /**
     * URL currently being classified by Tier 2 HTML fingerprint.
     * Non-null only during the async detection window; drives the loading screen.
     */
    private val detectingUrl = mutableStateOf<String?>(null)

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
                    detectingUrl = detectingUrl.value,
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
     * - **Tier 1** (sync): known Medium hostnames → WebViewScreen immediately, no loading shown.
     * - **Tier 2** (async, ~1 s): HTML head fingerprint → shows [UrlDetectionScreen] while
     *   waiting, then routes to WebViewScreen or ArticleReaderScreen.
     */
    private fun routeUrl(url: String) {
        // Tier 1 — instant sync path, no loading indicator needed
        if (UrlDetector.detectInstant(url) == UrlType.Medium) {
            sharedUrl.value = url
            return
        }

        // Tier 2 — async HTML fingerprint; show loading screen for the duration
        detectingUrl.value = url
        lifecycleScope.launch {
            when (UrlDetector.detect(url)) {
                UrlType.Medium -> sharedUrl.value = url
                UrlType.News   -> newsReaderUrl.value = url
            }
            detectingUrl.value = null   // dismiss loading screen
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
    detectingUrl: String?,
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
        // ── News Reader (highest priority) ────────────────────────────────────
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

        // ── WebView — Medium (with Freedium proxy) or browser fallback ────────
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

        // ── Tier 2 detection loading screen ───────────────────────────────────
        AnimatedVisibility(
            visible = detectingUrl != null
                    && sharedUrl == null
                    && newsReaderUrl == null
                    && fallbackBrowserUrl == null,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200))
        ) {
            UrlDetectionScreen(url = detectingUrl ?: "")
        }

        // ── Home — shown when nothing else is active ──────────────────────────
        AnimatedVisibility(
            visible = sharedUrl == null
                    && newsReaderUrl == null
                    && fallbackBrowserUrl == null
                    && detectingUrl == null,
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

/**
 * Full-screen loading indicator shown while [UrlDetector] runs its Tier 2
 * HTML head fingerprint check on a shared URL.
 */
@Composable
private fun UrlDetectionScreen(url: String) {
    // Gentle pulsing alpha for the subtitle line
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B))
                )
            )
            .statusBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 40.dp)
        ) {
            CircularProgressIndicator(
                color = Color(0xFF6366F1),
                strokeWidth = 3.dp,
                modifier = Modifier.size(56.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Identifying article type…",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Checking if this is a Medium article\nor a news site",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                lineHeight = 19.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.alpha(pulseAlpha)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Show the URL being checked, truncated cleanly
            Text(
                text = url,
                color = Color(0xFF475569),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}