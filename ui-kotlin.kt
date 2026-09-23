package com.example.mp3player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ThemeMode { LIGHT, DARK, SYSTEM }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var themeMode by remember { mutableStateOf(ThemeMode.SYSTEM) }
            val systemDark = isSystemInDarkTheme()
            val isDarkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> systemDark
            }

            MaterialTheme(
                colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MusicPlayerApp(
                        currentTheme = themeMode,
                        onThemeChange = { themeMode = it }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerApp(currentTheme: ThemeMode, onThemeChange: (ThemeMode) -> Unit) {
    var showThemeDialog by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    
    // Pager for Playlist(0) <- Player(1) -> Lyrics(2)
    val pagerState = rememberPagerState(initialPage = 1) { 3 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("NOW PLAYING", fontSize = 14.sp, fontWeight = FontWeight.Medium) 
                },
                navigationIcon = {
                    IconButton(onClick = { /* Handle collapse */ }) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Minimize")
                    }
                },
                actions = {
                    IconButton(onClick = { showThemeDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Theme Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> PlaylistScreen()
                    1 -> PlayerScreen(onQrClick = { showQrDialog = true })
                    2 -> LyricsScreen()
                }
            }
            
            // Carousel Indicator Dots
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(3) { index ->
                    val color = if (pagerState.currentPage == index) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }
        }

        // Modals
        if (showThemeDialog) {
            ThemeSelectionDialog(
                currentTheme = currentTheme,
                onDismiss = { showThemeDialog = false },
                onThemeSelected = { 
                    onThemeChange(it)
                    showThemeDialog = false
                }
            )
        }

        if (showQrDialog) {
            QrShareDialog(onDismiss = { showQrDialog = false })
        }
    }
}

@Composable
fun PlayerScreen(onQrClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Album Art
        Box(
            modifier = Modifier
                .size(250.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Song 2 (Prototype)", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Artist B", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Progress Bar
        Slider(
            value = 0.3f,
            onValueChange = {},
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("1:12", fontSize = 12.sp)
            Text("3:45", fontSize = 12.sp)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Media Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {}) { Icon(Icons.Default.Refresh, "Shuffle") }
            IconButton(onClick = {}) { Icon(Icons.Default.ArrowBack, "Previous") }
            FloatingActionButton(onClick = {}) { Icon(Icons.Default.PlayArrow, "Play") }
            IconButton(onClick = {}) { Icon(Icons.Default.ArrowForward, "Next") }
            IconButton(onClick = {}) { Icon(Icons.Default.Refresh, "Repeat") }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Action Bar (Like, QR, EQ)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = {}) { Icon(Icons.Default.FavoriteBorder, "Like") }
            IconButton(onClick = onQrClick) { Icon(Icons.Default.Share, "LAN Share") }
            IconButton(onClick = {}) { Icon(Icons.Default.List, "Equalizer") }
        }
        
        Spacer(modifier = Modifier.height(64.dp))
    }
}

@Composable
fun PlaylistScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Current Queue", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
        // Mock List
        repeat(3) { index ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Text("${index + 1}", modifier = Modifier.padding(end = 16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Track Title $index", fontWeight = FontWeight.Medium)
                    Text("Artist Name", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Default.MoreVert, "More Options")
            }
        }
    }
}

@Composable
fun LyricsScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Live Lyrics", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 24.dp))
        Text("Yeah, we are starting up.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(8.dp))
        Text("This is the current playing line.", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(8.dp))
        Text("Swipe left or right to navigate.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(8.dp))
    }
}

@Composable
fun ThemeSelectionDialog(currentTheme: ThemeMode, onDismiss: () -> Unit, onThemeSelected: (ThemeMode) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Theme Settings") },
        text = {
            Column {
                ThemeOptionRow("Light Mode", currentTheme == ThemeMode.LIGHT) { onThemeSelected(ThemeMode.LIGHT) }
                ThemeOptionRow("Dark Mode", currentTheme == ThemeMode.DARK) { onThemeSelected(ThemeMode.DARK) }
                ThemeOptionRow("Device Default", currentTheme == ThemeMode.SYSTEM) { onThemeSelected(ThemeMode.SYSTEM) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
fun ThemeOptionRow(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = isSelected, onClick = onClick)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text)
    }
}

@Composable
fun QrShareDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("LAN Audio Cast") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Scan this QR code from another device on the same Wi-Fi network to sync playback.", fontSize = 14.sp, modifier = Modifier.padding(bottom = 16.dp))
                // Dummy QR Placeholder
                Box(
                    modifier = Modifier.size(150.dp).background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text("QR CODE", color = Color.Black)
                }
                Text("192.168.1.45:8080", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 16.dp))
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}