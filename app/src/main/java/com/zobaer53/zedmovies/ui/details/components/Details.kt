package com.zobaer53.zedmovies.ui.details.components

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Environment
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.startapp.sdk.adsbase.StartAppAd
import com.zobaer53.zedmovies.R
import com.zobaer53.zedmovies.data.model.Credits
import com.zobaer53.zedmovies.data.model.ReleaseDate
import com.zobaer53.zedmovies.ui.designsystem.component.ZedMoviesCardNetworkImage
import com.zobaer53.zedmovies.ui.designsystem.component.ZedMoviesNetworkImage
import com.zobaer53.zedmovies.ui.designsystem.component.zedMoviesImagePlaceholder
import com.zobaer53.zedmovies.ui.designsystem.component.zedMoviesOverlay
import com.zobaer53.zedmovies.ui.designsystem.theme.zedMoviesTheme
import kotlinx.coroutines.delay

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Suppress("LongParameterList")
@Composable
internal fun DetailsItem(
    id: Int,
    type: String,
    title: String,
    overview: String,
    posterPath: String?,
    releaseDate: ReleaseDate,
    runtime: String,
    genres: List<String>,
    voteAverage: Double,
    credits: Credits,
    trailerKey: String?,
    isWishlisted: Boolean,
    onBackButtonClick: () -> Unit,
    onWishlistButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    playLabel: String? = null,
    playCaption: String? = null,
    windowInsets: WindowInsets = WindowInsets.safeDrawing.only(
        WindowInsetsSides.Horizontal + WindowInsetsSides.Top
    ),
    isPlaceholder: Boolean = false
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var isPlaying by remember { mutableStateOf(false) }
    var isFullScreen by remember { mutableStateOf(false) }
    var selectedServer by remember { mutableIntStateOf(1) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            StartAppAd.showAd(context)
            while (isPlaying) {
                delay(30 * 60 * 1000L)
                StartAppAd.showAd(context)
            }
        }
    }

    val isTv = type.trim().lowercase() == "tv"
    val streamUrl = remember(id, isTv, selectedServer) {
        when (selectedServer) {
            1 -> if (isTv) "https://vidsrc.xyz" else "https://vidsrc.xyz"
            2 -> if (isTv) "https://embed.su" else "https://embed.su"
            3 -> if (isTv) "https://vidlink.pro" else "https://vidlink.pro"
            else -> if (isTv) "https://2embed.cc" else "https://2embed.cc"
        }
    }

    if (isPlaying && isFullScreen) {
        DisposableEffect(Unit) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            onDispose {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            MoviePlayerWebView(
                url = streamUrl,
                onWebViewCreated = { webViewRef = it },
                modifier = Modifier.fillMaxSize()
            )
            PlayerControlBar(
                webView = webViewRef,
                isFullScreen = true,
                onToggleFullScreen = { isFullScreen = false },
                modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp)
            )
        }
        return
    }
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BackdropHeight)
        ) {
            if (isPlaceholder) {
                Box(
                    modifier = Modifier
                        .background(color = zedMoviesTheme.colors.primaryVariant)
                        .fillMaxSize()
                )
            } else {
                ZedMoviesNetworkImage(
                    modifier = Modifier.fillMaxSize(),
                    model = posterPath,
                    contentDescription = title
                )
            }
            zedMoviesOverlay(color = zedMoviesTheme.colors.primary, alpha = BackdropAlpha)
        }

        Scaffold(
            topBar = {
                if (isPlaceholder) {
                    TopAppBarPlaceholder(
                        isWishlisted = isWishlisted,
                        onBackButtonClick = onBackButtonClick,
                        onWishlistButtonClick = onWishlistButtonClick
                    )
                } else {
                    TopAppBar(
                        title = title,
                        isWishlisted = isWishlisted,
                        onBackButtonClick = onBackButtonClick,
                        onWishlistButtonClick = onWishlistButtonClick,
                        windowInsets = windowInsets
                    )
                }
            },
            containerColor = Color.Transparent,
            contentWindowInsets = windowInsets
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(zedMoviesTheme.spacing.extraMedium)
            ) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(zedMoviesTheme.spacing.small)
                    ) {
                        if (isPlaceholder) {
                            zedMoviesImagePlaceholder(
                                modifier = Modifier
                                    .size(width = PosterWidth, height = PosterHeight)
                                    .clip(zedMoviesTheme.shapes.smallMedium)
                            )
                        } else if (isPlaying) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black)
                            ) {
                                key(streamUrl) {
                                    MoviePlayerWebView(
                                        url = streamUrl,
                                        onWebViewCreated = { webViewRef = it },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(230.dp)
                                    )
                                }
                                PlayerControlBar(
                                    webView = webViewRef,
                                    isFullScreen = false,
                                    onToggleFullScreen = { isFullScreen = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF141414))
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                ServerButton("Server 1 (HD)", selectedServer == 1) { selectedServer = 1 }
                                ServerButton("Server 2 (Multi)", selectedServer == 2) { selectedServer = 2 }
                                ServerButton("Server 3 (Hindi)", selectedServer == 3) { selectedServer = 3 }
                                ServerButton("Server 4", selectedServer == 4) { selectedServer = 4 }
                            }
                        } else {
                            ZedMoviesCardNetworkImage(
                                modifier = Modifier.size(
                                    width = PosterWidth,
                                    height = PosterHeight
                                ),
                                model = posterPath,
                                contentDescription = title,
                                shape = zedMoviesTheme.shapes.smallMedium
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFE50914))
                                        .clickable { isPlaying = true }
                                        .padding(horizontal = 20.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = "▶ Watch Full Movie",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF2B2B2B))
                                        .clickable {
                                            StartAppAd.showAd(context)
                                            startOfflineDownload(context, streamUrl, title)
                                        }
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = "⬇ Download",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }

                        Column(
                            modifier = Modifier.padding(horizontal = zedMoviesTheme.spacing.largest),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (isPlaceholder) {
                                IconAndTextPlaceholder(iconResourceId = R.drawable.ic_calendar)
                                IconAndTextPlaceholder(iconResourceId = R.drawable.ic_clock)
                                IconAndTextPlaceholder(iconResourceId = R.drawable.ic_film)
                            } else {
                                IconAndText(
                                    iconResourceId = R.drawable.ic_calendar,
                                    text = releaseDate.year.ifEmpty { stringResource(id = R.string.no_release_date) }
                                )
                                IconAndText(iconResourceId = R.drawable.ic_clock, text = runtime)
                                IconAndText(
                                    iconResourceId = R.drawable.ic_film,
                                    text = genres.joinToString(separator = GenreSeparator)
                                        .ifEmpty { stringResource(id = R.string.no_genre) }
                                )
                            }
                        }
                    }
                }
                item {
                    if (isPlaceholder) OverviewPlaceholder() else Overview(overview = overview)
                }
                item {
                    if (isPlaceholder) CastAndCrewPlaceholder() else CastAndCrew(credits = credits)
                }
                item {
                    Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.safeDrawing))
                }
            }
        }
    }
}
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun MoviePlayerWebView(
    url: String,
    onWebViewCreated: (WebView) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(android.graphics.Color.BLACK)
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    allowContentAccess = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    userAgentString = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                }
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val targetHost = request?.url?.host ?: return false
                        val isAllowed = targetHost.contains("vidsrc") ||
                            targetHost.contains("embed") ||
                            targetHost.contains("vidlink") ||
                            targetHost.contains("2embed") ||
                            targetHost.contains("cloudflare") ||
                            targetHost.contains("cloudfront")
                        return !isAllowed
                    }
                }
                webChromeClient = WebChromeClient()
                onWebViewCreated(this)
                loadUrl(url)
            }
        }
    )
}

@Composable
private fun PlayerControlBar(
    webView: WebView?,
    isFullScreen: Boolean,
    onToggleFullScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ControlChip("↺ 10s") {
            webView?.evaluateJavascript(
                "document.querySelectorAll('video').forEach(v => v.currentTime = Math.max(0, v.currentTime - 10));",
                null
            )
        }
        ControlChip("⏯ Play/Pause") {
            webView?.evaluateJavascript(
                "document.querySelectorAll('video').forEach(v => v.paused ? v.play() : v.pause());",
                null
            )
        }
        ControlChip("10s ↻") {
            webView?.evaluateJavascript(
                "document.querySelectorAll('video').forEach(v => v.currentTime += 10);",
                null
            )
        }
        ControlChip(if (isFullScreen) "🗗 Exit Full" else "⛶ Fullscreen") {
            onToggleFullScreen()
        }
    }
}

@Composable
private fun ControlChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF2C2C2C))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text = label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ServerButton(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) Color(0xFFE50914) else Color(0xFF262626))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
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

@Composable
internal fun DetailsItemPlaceholder(
    onBackButtonClick: () -> Unit,
    onWishlistButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    DetailsItem(
        modifier = modifier,
        id = 0,
        type = "",
        title = PlaceholderText,
        overview = PlaceholderText,
        posterPath = null,
        releaseDate = ReleaseDate(),
        runtime = PlaceholderText,
        genres = emptyList(),
        voteAverage = 0.0,
        credits = Credits(cast = emptyList(), crew = emptyList()),
        trailerKey = null,
        isWishlisted = false,
        onBackButtonClick = onBackButtonClick,
        onWishlistButtonClick = onWishlistButtonClick,
        isPlaceholder = true
    )
}

private val BackdropHeight = 552.dp
private val PosterWidth = 205.dp
private val PosterHeight = 287.dp
private const val BackdropAlpha = 0.2f
private const val GenreSeparator = ", "
private const val PlaceholderText = ""
