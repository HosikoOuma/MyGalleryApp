//GEMINI НЕ ТРОГАЙ ЭТУ АНОТАЦИЮ
@file:kotlin.OptIn(ExperimentalMaterial3Api::class)
package com.example.nkdsify

import android.content.Context
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.app.DownloadManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.nkdsify.ui.dialogs.PermissionPermanentlyDeniedDialog
import com.example.nkdsify.ui.screens.WelcomeScreen
import com.example.nkdsify.ui.theme.NkdsifyAppTheme
import com.example.nkdsify.ui.utils.*
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.delay

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
        val splashScreen = installSplashScreen()
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
            val selectedFontFamily by remember { mutableStateOf(SettingsRepository.getFontFamily(context)) }
            var showPermanentlyDeniedDialog by remember { mutableStateOf(false) }

            val myAppState = rememberMyAppState()
            
            var isAppReady by remember { mutableStateOf(false) }
            
            LaunchedEffect(Unit) {
                delay(1000) 
                isAppReady = true
            }

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
                if (myAppState.hasPermissions) {
                    if (isFirstLaunch) {
                        SettingsRepository.setFirstLaunchDone(context)
                        isFirstLaunch = false
                    }
                } else {
                     if (!isFirstLaunch) {
                        val permanentlyDenied = myAppState.permissionsToRequest.any {
                            !shouldShowRequestPermissionRationale(it)
                        }
                        if (permanentlyDenied) {
                            showPermanentlyDeniedDialog = true
                        }
                    }
                }
                 if (isFirstLaunch) {
                    SettingsRepository.setFirstLaunchDone(context)
                    isFirstLaunch = false
                }
            }
            
            NkdsifyAppTheme(theme = selectedTheme, appFontFamily = selectedFontFamily) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (myAppState.hasPermissions) {
                            MyApp(
                                myAppState = myAppState,
                                initialUri = initialUri,
                                screenWidth = screenWidth,
                                screenHeight = screenHeight,
                            )
                        } else {
                            WelcomeScreen(
                                onGrantPermissionClick = {
                                    permissionLauncher.launch(myAppState.permissionsToRequest)
                                },
                                theme = selectedTheme
                            )
                        }

                        if (showPermanentlyDeniedDialog) {
                            PermissionPermanentlyDeniedDialog(
                                onDismiss = { showPermanentlyDeniedDialog = false },
                                onConfirm = {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    val uri = Uri.fromParts("package", context.packageName, null)
                                    intent.data = uri
                                    context.startActivity(intent)
                                    showPermanentlyDeniedDialog = false
                                }
                            )
                        }

                        // Убираем enter-анимацию, чтобы текст появлялся мгновенно после черного экрана
                        AnimatedVisibility(
                            visible = !isAppReady,
                            enter = EnterTransition.None,
                            exit = fadeOut(animationSpec = tween(600))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(id = R.string.app_name),
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
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

//Хосико любит тебя)
