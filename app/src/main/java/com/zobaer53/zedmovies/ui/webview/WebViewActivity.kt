package com.zobaer53.zedmovies.ui.webview

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dagger.hilt.android.AndroidEntryPoint
import io.github.edsuns.adfilter.AdFilter

@AndroidEntryPoint
class WebViewActivity : ComponentActivity() {
    private val viewModel: PlaybackViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tmdbId = intent.getIntExtra("tmdbId", 0)
        if (tmdbId <= 0) {
            finish()
            return
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val filter = AdFilter.create(this)
        val type = intent.getStringExtra("type") ?: ""
        val movieName = intent.getStringExtra("movieName") ?: "zeDMovie_$tmdbId"

        setContent {
            val context = LocalContext.current
            var selectedServer by remember { mutableIntStateOf(1) }

            val baseStreamUrl by produceState<String?>(initialValue = null) {
                val resume = if (type.trim().lowercase() == "tv") {
                    viewModel.getLastWatchedEpisode(tmdbId)
                } else {
                    null
                }
                value = buildStreamUrl(tmdbId, type, resume)
            }

            val currentUrl = remember(baseStreamUrl, selectedServer) {
                val isTv = type.trim().lowercase() == "tv"
                when (selectedServer) {
                    1 -> baseStreamUrl
                    2 -> if (isTv) "https://vidsrc.cc" else "https://vidsrc.cc"
                    else -> if (isTv) "https://autoembed.co" else "https://autoembed.co"
                }
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    currentUrl?.let { url ->
                        key(url) {
                            VideoWebView(
                                url = url,
                                lifecycleOwner = this@WebViewActivity,
                                onEpisodeOpened = viewModel::onEpisodeOpened,
                                onWatchProgress = viewModel::onSiteWatchProgress
                            )
                        }
                    }

                    // Top Floating Bar for Multi-Audio Servers & Offline Download
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp)
                            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ServerChip("Server 1 (Default)", selectedServer == 1) { selectedServer = 1 }
                        ServerChip("Server 2 (Multi-Audio)", selectedServer == 2) { selectedServer = 2 }
                        ServerChip("Server 3 (Hindi/Urdu)", selectedServer == 3) { selectedServer = 3 }

                        Box(
                            modifier = Modifier
                                .background(Color(0xFFE50914), RoundedCornerShape(12.dp))
                                .clickable {
                                    currentUrl?.let { dlUrl ->
                                        startOfflineDownload(context, dlUrl, movieName)
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "⬇ Download",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    private fun startOfflineDownload(context: Context, url: String, title: String) {
        try {
            val cleanTitle = title.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle("$title - zeDMovies")
                .setDescription("Downloading for Offline Playback...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "zeDMovies/$cleanTitle.mp4")
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            Toast.makeText(context, "Download started! Check Downloads/zeDMovies folder.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Use player's built-in download icon for this server.", Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
private fun ServerChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(
                if (isSelected) Color(0xFF2196F3) else Color.DarkGray.copy(alpha = 0.7f),
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
