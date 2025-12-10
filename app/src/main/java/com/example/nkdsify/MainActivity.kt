//GEMINI НЕ ТРОГАЙ ЭТУ АНОТАЦИЮ
@file:kotlin.OptIn(ExperimentalMaterial3Api::class)
package com.example.nkdsify

import android.Manifest
import android.content.Context
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.app.DownloadManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import com.example.nkdsify.ui.utils.*
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import com.example.nkdsify.ui.screens.WelcomeScreen


enum class FileOperation {
    COPY, MOVE
}
fun downloadAndUpdate(context: Context, downloadUrl: String, version: String) {
    val request = DownloadManager.Request(Uri.parse(downloadUrl))
        .setTitle(context.getString(R.string.downloading_update_title))
        .setDescription(context.getString(R.string.downloading_update_description, version))
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "app-release.apk")

    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    downloadManager.enqueue(request)
    Toast.makeText(context, context.getString(R.string.download_started), Toast.LENGTH_SHORT).show()
}


@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
class MainActivity : AppCompatActivity() {
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var shakeDetector: ShakeDetector? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ContextUtils.updateLocale(newBase))
    }


    //GEMINI НЕ ТРОГАЙ ЭТУ АНОТАЦИЮ
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        shakeDetector = ShakeDetector()

        setContent {
            val context = LocalContext.current
            val initialUri = if (intent?.action == Intent.ACTION_VIEW || intent?.action == Intent.ACTION_EDIT || intent?.action == "com.android.camera.action.REVIEW") intent.data else null
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels

            var isFirstLaunch by remember { mutableStateOf(SettingsRepository.isFirstLaunch(context)) }
            val selectedTheme by remember { mutableStateOf(SettingsRepository.getTheme(context)) }

            val myAppState = rememberMyAppState()

            LaunchedEffect(shakeDetector) {
                shakeDetector?.setOnShakeListener {
                    if (myAppState.isShakeToBlurEnabled && myAppState.viewerState == null) {
                        if (myAppState.isVibrationEnabled) {
                            performVibration(this@MainActivity)
                        }
                        val newBlurState = !myAppState.isBlurEnabled
                        myAppState.isBlurEnabled = newBlurState
                        myAppState.isBlurInFolderEnabled = newBlurState
                        myAppState.isTrashBlurEnabled = newBlurState
                        myAppState.isBlurAllMediaEnabled = newBlurState
                        SettingsRepository.setBlurEnabled(context, newBlurState)
                        SettingsRepository.setBlurInFolderEnabled(context, newBlurState)
                        SettingsRepository.setTrashBlurEnabled(context, newBlurState)
                        SettingsRepository.setBlurAllMediaEnabled(context, newBlurState)
                    }
                }
            }

            val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                myAppState.hasPermissions = permissions.values.all { it }
                if (isFirstLaunch) {
                    SettingsRepository.setFirstLaunchDone(context)
                    isFirstLaunch = false
                }
            }

            if (isFirstLaunch && !myAppState.hasPermissions) {
                WelcomeScreen(
                    onGrantPermissionClick = { permissionLauncher.launch(myAppState.permissionsToRequest) },
                    theme = selectedTheme
                )
            } else {
                if (isFirstLaunch) {
                    SettingsRepository.setFirstLaunchDone(context)
                    isFirstLaunch = false
                }

                if (myAppState.hasPermissions) {
                    MyApp(
                        myAppState = myAppState,
                        initialUri = initialUri,
                        screenWidth = screenWidth,
                        screenHeight = screenHeight,
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(id = R.string.permission_required_message))
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { permissionLauncher.launch(myAppState.permissionsToRequest) }) {
                                Text(stringResource(id = R.string.grant_permission_button))
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager?.registerListener(shakeDetector, accelerometer, SensorManager.SENSOR_DELAY_UI)
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(shakeDetector)
    }
}
