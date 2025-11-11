package com.example.nkdsify

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions

/**
 * Небольшой helper, который создаёт все нужные launchers внутри composable scope
 * и предоставляет функции для их запуска.
 */
class AppLaunchers(
    val requestPermissions: (Array<String>) -> Unit,
    val launchManageStorage: (Intent) -> Unit,
    val launchCropImage: (CropImageContractOptions) -> Unit,
    val launchImportFavorites: () -> Unit,
    val launchImportTags: () -> Unit
)

@Composable
fun rememberAppLaunchers(
    onPermissionsResult: (Map<String, Boolean>) -> Unit,
    onManageStorageResult: () -> Unit,
    onCropResult: (com.canhub.cropper.CropImageView.CropResult) -> Unit,
    onImportFavoritesResult: (android.net.Uri?) -> Unit,
    onImportTagsResult: (android.net.Uri?) -> Unit
): AppLaunchers {
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        onPermissionsResult(permissions)
    }

    val manageStorageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // Возвращаем господину MainContent флаг, он проверит Environment.isExternalStorageManager()
        onManageStorageResult()
    }

    val cropImageLauncher = rememberLauncherForActivityResult(contract = CropImageContract()) { result ->
        onCropResult(result)
    }

    val importFavoritesLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
        onImportFavoritesResult(uri)
    }

    val importTagsLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
        onImportTagsResult(uri)
    }

    return remember {
        AppLaunchers(
            requestPermissions = { perms -> permissionLauncher.launch(perms) },
            launchManageStorage = { intent -> manageStorageLauncher.launch(intent) },
            launchCropImage = { options -> cropImageLauncher.launch(options) },
            launchImportFavorites = { importFavoritesLauncher.launch("application/json") },
            launchImportTags = { importTagsLauncher.launch("application/json") }
        )
    }
}

