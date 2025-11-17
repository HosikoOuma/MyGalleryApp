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
import androidx.compose.runtime.mutableIntStateOf
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
    // Внутри класса MainActivity, но вне onCreate
    override fun attachBaseContext(newBase: Context) {
        // Здесь мы подменяем контекст на тот, в котором уже есть нужный язык
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
            val initialUri = if (intent?.action == Intent.ACTION_VIEW || intent?.action == Intent.ACTION_EDIT || intent?.action == "com.android.camera.action.REVIEW") intent.data else null
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels
            var isShakeToBlurEnabled by remember { mutableStateOf(SettingsRepository.isShakeToBlurEnabled(this@MainActivity)) }
            var isBlurEnabled by remember { mutableStateOf(SettingsRepository.isBlurEnabled(this@MainActivity)) }
            var isVibrationEnabled by remember { mutableStateOf(SettingsRepository.isVibrationEnabled(this@MainActivity)) }
            var isBlurInFolderEnabled by remember { mutableStateOf(SettingsRepository.isBlurInFolderEnabled(this@MainActivity)) }
            var isTrashBlurEnabled by remember { mutableStateOf(SettingsRepository.isTrashBlurEnabled(context = this@MainActivity)) }
            var isBlurAllMediaEnabled by remember { mutableStateOf(SettingsRepository.isBlurAllMediaEnabled(context = this@MainActivity)) }
            var isViewerOpen by remember { mutableStateOf(false) }
            var isLoopVideoEnabled by remember { mutableStateOf(SettingsRepository.isLoopVideoEnabled(this@MainActivity)) }
            var isSwipeToDismissEnabled by remember { mutableStateOf(SettingsRepository.isSwipeToDismissEnabled(this@MainActivity)) }
            var useLargeFab by remember { mutableStateOf(SettingsRepository.isUseLargeFab(this@MainActivity)) }
            var autoDeleteTrashEnabled by remember { mutableStateOf(SettingsRepository.isAutoDeleteTrashEnabled(this@MainActivity)) }
            var autoDeleteTrashDays by remember { mutableIntStateOf(SettingsRepository.getAutoDeleteTrashDays(this@MainActivity)) }
            val context = LocalContext.current
            var isFirstLaunch by remember { mutableStateOf(SettingsRepository.isFirstLaunch(context)) }
            var selectedTheme by remember { mutableStateOf(SettingsRepository.getTheme(context)) }
            val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
            } else {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            var hasPermissions by remember { mutableStateOf(permissionsToRequest.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) }

            val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                hasPermissions = permissions.values.all { it }
                if (isFirstLaunch) {
                    SettingsRepository.setFirstLaunchDone(context)
                    isFirstLaunch = false
                }
            }

            // This is the corrected logic block
            if (isFirstLaunch && !hasPermissions) {
                WelcomeScreen(
                    onGrantPermissionClick = { permissionLauncher.launch(permissionsToRequest)  },
                    theme = selectedTheme
                )           } else {
                if (isFirstLaunch) {
                    // This handles the case where permissions were already granted on the first launch.
                    // We must still mark it as "not first launch" for the next run.
                    SettingsRepository.setFirstLaunchDone(context)
                    isFirstLaunch = false
                }

                if (hasPermissions) {
                    MyApp(
                        initialUri = initialUri,
                        screenWidth = screenWidth,
                        screenHeight = screenHeight,
                        isShakeToBlurEnabled = isShakeToBlurEnabled, onShakeToBlurEnabledChange = { isShakeToBlurEnabled = it },
                        isBlurEnabled = isBlurEnabled, onBlurEnabledChange = { isBlurEnabled = it },
                        isVibrationEnabled = isVibrationEnabled, onVibrationEnabledChange = { isVibrationEnabled = it },
                        isBlurInFolderEnabled = isBlurInFolderEnabled, onBlurInFolderEnabledChange = { isBlurInFolderEnabled = it },
                        onViewerOpenChange = { isViewerOpen = it },
                        isLoopVideoEnabled = isLoopVideoEnabled, onLoopVideoEnabledChange = {isLoopVideoEnabled = it},
                        isSwipeToDismissEnabled = isSwipeToDismissEnabled, onSwipeToDismissEnabledChange = {isSwipeToDismissEnabled = it},
                        useLargeFab = useLargeFab, onUseLargeFabChange = { useLargeFab = it }, isBlurAllMediaEnabled = isBlurAllMediaEnabled, isTrashBlurEnabled = isTrashBlurEnabled,
                        onBlurAllMediaEnabledChange = { isBlurAllMediaEnabled = it },
                        onTrashBlurEnabledChange = { isTrashBlurEnabled = it },
                        autoDeleteTrashEnabled = autoDeleteTrashEnabled, onAutoDeleteTrashEnabledChange = { autoDeleteTrashEnabled = it },
                        autoDeleteTrashDays = autoDeleteTrashDays, onAutoDeleteTrashDaysChange = { autoDeleteTrashDays = it }
                    )
                } else {
                    // This handles the case where permissions are revoked by the user on a subsequent launch.
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(id = R.string.permission_required_message))
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { permissionLauncher.launch(permissionsToRequest) }) {
                                Text(stringResource(id = R.string.grant_permission_button))
                            }
                        }
                    }
                }
            }

            shakeDetector?.setOnShakeListener {
                if (isShakeToBlurEnabled && !isViewerOpen) {
                    if (isVibrationEnabled) {
                        performVibration(this@MainActivity)
                    }
                    val newBlurState = !isBlurEnabled
                    isBlurEnabled = newBlurState
                    isBlurInFolderEnabled = newBlurState
                    isTrashBlurEnabled = newBlurState
                    isBlurAllMediaEnabled = newBlurState
                    SettingsRepository.setBlurEnabled(this@MainActivity, newBlurState)
                    SettingsRepository.setBlurInFolderEnabled(this@MainActivity, newBlurState)
                    SettingsRepository.setTrashBlurEnabled(this@MainActivity, newBlurState)
                    SettingsRepository.setBlurAllMediaEnabled(this@MainActivity, newBlurState)
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


