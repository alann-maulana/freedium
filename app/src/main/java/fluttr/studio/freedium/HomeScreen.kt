package fluttr.studio.freedium

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Newspaper
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class ReaderMode { MEDIUM, NEWS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onMediumUrlSubmitted: (String) -> Unit,
    onNewsUrlSubmitted: (String) -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf(ReaderMode.MEDIUM) }
    val scrollState = rememberScrollState()

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F172A), // Slate 900
            Color(0xFF1E1B4B)  // Indigo 950
        )
    )

    val cardGradient = Brush.linearGradient(
        colors = listOf(
            Color(0x1F818CF8),
            Color(0x0C312E81)
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
            // App Logo
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
                    imageVector = if (selectedMode == ReaderMode.MEDIUM)
                        Icons.AutoMirrored.Filled.MenuBook else Icons.Default.Newspaper,
                    contentDescription = "Reader",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Freedium Reader",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (selectedMode == ReaderMode.MEDIUM)
                    "Read Medium articles beautifully and restriction-free."
                else
                    "Read any news article — no ads, no popups, just words.",
                color = Color(0xFF94A3B8),
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ── Mode selector ─────────────────────────────────────────────────
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                SegmentedButton(
                    selected = selectedMode == ReaderMode.MEDIUM,
                    onClick = {
                        selectedMode = ReaderMode.MEDIUM
                        textInput = ""
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = Color(0xFF4F46E5),
                        activeContentColor = Color.White,
                        activeBorderColor = Color(0xFF6366F1),
                        inactiveContainerColor = Color(0x1A818CF8),
                        inactiveContentColor = Color(0xFF94A3B8),
                        inactiveBorderColor = Color(0x334D46E5)
                    ),
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                ) {
                    Text("Medium Bypass", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                SegmentedButton(
                    selected = selectedMode == ReaderMode.NEWS,
                    onClick = {
                        selectedMode = ReaderMode.NEWS
                        textInput = ""
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = Color(0xFF4F46E5),
                        activeContentColor = Color.White,
                        activeBorderColor = Color(0xFF6366F1),
                        inactiveContainerColor = Color(0x1A818CF8),
                        inactiveContentColor = Color(0xFF94A3B8),
                        inactiveBorderColor = Color(0x33818CF8)
                    ),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Newspaper,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                ) {
                    Text("News Reader", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── URL input card ────────────────────────────────────────────────
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (selectedMode == ReaderMode.MEDIUM)
                                "Paste Medium Article Link"
                            else
                                "Paste Any News Article Link",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = {
                                Text(
                                    text = if (selectedMode == ReaderMode.MEDIUM)
                                        "https://medium.com/..."
                                    else
                                        "https://bbc.com/news/...",
                                    color = Color(0xFF64748B)
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Go
                            ),
                            keyboardActions = KeyboardActions(
                                onGo = {
                                    if (textInput.isNotBlank()) {
                                        if (selectedMode == ReaderMode.MEDIUM) {
                                            onMediumUrlSubmitted(textInput.trim())
                                        } else {
                                            onNewsUrlSubmitted(textInput.trim())
                                        }
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
                                    if (selectedMode == ReaderMode.MEDIUM) {
                                        onMediumUrlSubmitted(textInput.trim())
                                    } else {
                                        onNewsUrlSubmitted(textInput.trim())
                                    }
                                }
                            },
                            enabled = textInput.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6366F1),
                                contentColor = Color.White,
                                disabledContainerColor = Color(0x4D6366F1),
                                disabledContentColor = Color(0x80FFFFFF)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text(
                                text = if (selectedMode == ReaderMode.MEDIUM)
                                    "Open Article" else "Read Clean",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Instructions card ─────────────────────────────────────────────
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
                    if (selectedMode == ReaderMode.MEDIUM) {
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
                    } else {
                        Text(
                            text = "🗞️ How News Reader Works:",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "1. Copy a link from any news site (BBC, CNN, Reuters, etc.).\n" +
                                    "2. Paste it above and tap \"Read Clean\".\n" +
                                    "3. We extract just the article — title, author, and body.\n" +
                                    "4. Or share any link from your browser — we'll auto-detect it!",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Version badge ─────────────────────────────────────────────────
            VersionBadge()
        }
    }
}

@Composable
private fun VersionBadge() {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val alpha by pulse.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0x1A818CF8), Color(0x1A4F46E5))
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0x4D818CF8), Color(0x4D4F46E5))
                ),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Pulsing dot
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF818CF8).copy(alpha = alpha))
            )
            Text(
                text = "v${BuildConfig.VERSION_NAME}",
                color = Color(0xFF818CF8),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "·",
                color = Color(0xFF475569),
                fontSize = 12.sp
            )
            Text(
                text = "Build ${BuildConfig.VERSION_CODE}",
                color = Color(0xFF475569),
                fontSize = 12.sp,
                letterSpacing = 0.3.sp
            )
        }
    }
}
