//GEMINI НЕ ТРОГАЙ ЭТУ АНОТАЦИЮ
@file:kotlin.OptIn(ExperimentalMaterial3Api::class)
package com.example.nkdsify

import android.Manifest
import android.app.Activity
import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.VideoFrameDecoder
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.example.nkdsify.data.*
import com.example.nkdsify.ui.AppNavigation
import com.example.nkdsify.ui.BottomBar
import com.example.nkdsify.ui.TopBar
import com.example.nkdsify.ui.components.*
import com.example.nkdsify.ui.theme.NkdsifyAppTheme
import com.example.nkdsify.ui.utils.*
import com.example.nkdsify.ui.utils.getMediaDetails
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import android.content.ContentUris
import androidx.annotation.OptIn
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.ui.res.stringResource
import com.example.nkdsify.ui.components.FolderSelectionDialog

enum class FileOperation {
    COPY, MOVE
}

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
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
//        val language = SettingsRepository.getLanguage(this)
//        val locale = if (language == Language.SYSTEM) {
//            LocaleListCompat.getEmptyLocaleList()
//        } else {
//            LocaleListCompat.forLanguageTags(language.code)
//        }
//        AppCompatDelegate.setApplicationLocales(locale)
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        shakeDetector = ShakeDetector()

        setContent {
            val initialUri = if (intent?.action == Intent.ACTION_VIEW) intent.data else null
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


            // MyApp был вынесен в MainContent.kt — смотрите `/app/src/main/java/com/example/nkdsify/MainContent.kt`.

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

            MyApp(initialUri = initialUri, screenWidth = screenWidth, screenHeight = screenHeight,
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
//