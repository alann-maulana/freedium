package fluttr.studio.freedium

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onUrlSubmitted: (String) -> Unit) {
    var textInput by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    // Vibrant gradients matching modern premium design guidelines
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F172A), // Slate 900
            Color(0xFF1E1B4B)  // Indigo 950
        )
    )

    val cardGradient = Brush.linearGradient(
        colors = listOf(
            Color(0x1F818CF8), // Indigo 400 with 12% opacity
            Color(0x0C312E81)  // Indigo 900 with 5% opacity
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Elegant glowing App Logo/Icon container
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF818CF8), Color(0xFF4F46E5))
                        )
                    )
                    .shadow(16.dp, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = "Reader",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Freedium Mirror Client",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Read Medium articles beautifully and restriction-free.",
                color = Color(0xFF94A3B8), // Slate 400
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Premium Glassmorphism Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x33818CF8), RoundedCornerShape(24.dp))
                    .shadow(12.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cardGradient)
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Paste Article Link",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = { Text("https://medium.com/...", color = Color(0xFF64748B)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Go
                            ),
                            keyboardActions = KeyboardActions(
                                onGo = {
                                    if (textInput.isNotBlank()) {
                                        onUrlSubmitted(textInput.trim())
                                    }
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF818CF8),
                                unfocusedBorderColor = Color(0x4D818CF8),
                                focusedContainerColor = Color(0x1A000000),
                                unfocusedContainerColor = Color(0x0D000000),
                                focusedLabelColor = Color(0xFF818CF8)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (textInput.isNotBlank()) {
                                    onUrlSubmitted(textInput.trim())
                                }
                            },
                            enabled = textInput.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6366F1), // Indigo 500
                                contentColor = Color.White,
                                disabledContainerColor = Color(0x4D6366F1),
                                disabledContentColor = Color(0x80FFFFFF)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text("Open Article", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Instructions card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x1F94A3B8), RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x0DFFFFFF))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "💡 How to Use Share Extension:",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "1. Open any Medium article inside Medium, Chrome, or any other browser.\n" +
                               "2. Tap the Share button.\n" +
                               "3. Select \"Freedium\" from the system share sheet.\n" +
                               "4. The article will open instantly in our reader WebView!",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(url: String, onClose: () -> Unit) {
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var loadProgress by remember { mutableFloatStateOf(0f) }
    var isPageLoading by remember { mutableStateOf(true) }

    // Intercept Back Press to navigate back inside WebView history if possible
    BackHandler(enabled = true) {
        if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            onClose()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Freedium Reader",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { webViewInstance?.reload() }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            builtInZoomControls = true
                            displayZoomControls = false
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isPageLoading = true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isPageLoading = false
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                loadProgress = newProgress / 100f
                            }
                        }
                        loadUrl(url)
                        webViewInstance = this
                    }
                },
                update = {
                    // Update state or trigger updates if url changes dynamically
                },
                modifier = Modifier.fillMaxSize()
            )

            // High quality dynamic loading bar
            if (isPageLoading) {
                LinearProgressIndicator(
                    progress = { loadProgress },
                    color = Color(0xFF6366F1), // Matching Indigo Accent
                    trackColor = Color(0x336366F1),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.TopCenter)
                )
            }
        }
    }
}