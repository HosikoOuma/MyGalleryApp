package com.example.nkdsify

import android.content.Context
import com.example.nkdsify.ui.utils.GithubUpdateChecker
import com.example.nkdsify.ui.utils.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Сравнивает два версионных имени, возвращает -1 если v1<v2, 0 если равны, 1 если v1>v2
 */
fun compareVersionNames(v1: String, v2: String): Int {
    val parts1 = v1.removePrefix("v").split('.').map { it.toIntOrNull() ?: 0 }
    val parts2 = v2.removePrefix("v").split('.').map { it.toIntOrNull() ?: 0 }
    val size = maxOf(parts1.size, parts2.size)
    for (i in 0 until size) {
        val p1 = parts1.getOrElse(i) { 0 }
        val p2 = parts2.getOrElse(i) { 0 }
        if (p1 < p2) return -1
        if (p1 > p2) return 1
    }
    return 0
}

/**
 * Проверяет наличие обновления релиза на GitHub. Функция сама запускает корутину в переданном [coroutineScope].
 *
 * Параметры:
 * - context: Context — для проверки настроек (isCheckForUpdatesOnStartup).
 * - coroutineScope: CoroutineScope — scope для запуска фоновой работы.
 * - currentVersion: String — текущая версия приложения (например "1.2.3").
 * - isTriggeredByUser: Boolean — если true, показываем feedback при отсутствии обновлений.
 * - onNewVersion: (tagName) -> Unit — колбек, вызываемый в Main (на главном потоке), если найдена новая версия.
 * - onNoUpdate: (() -> Unit)? — необязательный колбек (на главном потоке), вызывается при ручной проверке и отсутствии обновлений.
 */
fun checkForUpdates(
    context: Context,
    coroutineScope: CoroutineScope,
    currentVersion: String,
    isTriggeredByUser: Boolean,
    onNewVersion: (String) -> Unit,
    onNoUpdate: (() -> Unit)? = null
) {
    coroutineScope.launch(Dispatchers.IO) {
        if (!isTriggeredByUser && !SettingsRepository.isCheckForUpdatesOnStartupEnabled(context)) {
            return@launch
        }

        val release = GithubUpdateChecker.getLatestRelease("HosikoOuma", "MyGalleryApp")
        release?.let {
            if (compareVersionNames(it.tag_name, currentVersion) > 0) {
                withContext(Dispatchers.Main) {
                    onNewVersion(it.tag_name)
                }
            } else {
                if (isTriggeredByUser) {
                    withContext(Dispatchers.Main) {
                        onNoUpdate?.invoke()
                    }
                }
            }
        }
    }
}
