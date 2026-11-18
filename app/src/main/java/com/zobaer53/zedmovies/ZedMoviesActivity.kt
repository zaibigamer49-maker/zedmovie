package com.zobaer53.zedmovies

import android.app.DownloadManager
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.startapp.sdk.ads.banner.Banner
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.StartAppSDK
import com.zobaer53.zedmovies.ui.zedMoviesApp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

@AndroidEntryPoint
class zedMoviesActivity : ComponentActivity() {

    private val STARTAPP_APP_ID = "156899168"
    private val UPDATE_JSON_URL = "https://githubusercontent.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Initialize Start.io Earning Ads with your real ID
        StartAppSDK.init(this, STARTAPP_APP_ID, true)
        StartAppAd.disableSplash()

        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        setContent {
            val context = LocalContext.current
            var updateInfo by remember { mutableStateOf<UpdateData?>(null) }

            LaunchedEffect(Unit) {
                val remote = checkForAppUpdate(UPDATE_JSON_URL)
                if (remote != null && remote.versionCode > BuildConfig.VERSION_CODE) {
                    updateInfo = remote
                }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) {
                    zedMoviesApp()
                }

                // Bottom Earning Banner Ad
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    factory = { ctx -> Banner(ctx) }
                )
            }

            // In-App Auto Update Popup with Your Custom Logo
            updateInfo?.let { info ->
                Dialog(onDismissRequest = { if (!info.forceUpdate) updateInfo = null }) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                painter = painterResource(id = R.mipmap.zed_movies_round),
                                contentDescription = "zeDMovies Logo",
                                modifier = Modifier.size(76.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "zeDMovies",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = info.message.ifEmpty { "New Update Available! Download the latest version for new movies and faster servers." },
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = {
                                    downloadNewApk(context, info.apkUrl, info.versionName)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "Update Now (v${info.versionName})",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun checkForAppUpdate(url: String): UpdateData? = withContext(Dispatchers.IO) {
        try {
            val jsonText = URL(url).readText()
            val obj = JSONObject(jsonText)
            UpdateData(
                versionCode = obj.getInt("versionCode"),
                versionName = obj.optString("versionName", "2.0"),
                message = obj.optString("message", ""),
                apkUrl = obj.getString("apkUrl"),
                forceUpdate = obj.optBoolean("forceUpdate", false)
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun downloadNewApk(context: Context, apkUrl: String, versionName: String) {
        try {
            val request = DownloadManager.Request(Uri.parse(apkUrl))
                .setTitle("zeDMovies v$versionName Update")
                .setDescription("Downloading latest update...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "zeDMovies_v$versionName.apk")
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            Toast.makeText(context, "Update downloading! Check notification bar to install.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Download failed. Check internet connection.", Toast.LENGTH_SHORT).show()
        }
    }
}

data class UpdateData(
    val versionCode: Int,
    val versionName: String,
    val message: String,
    val apkUrl: String,
    val forceUpdate: Boolean
)
