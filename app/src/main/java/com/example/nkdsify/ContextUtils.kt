package com.example.nkdsify // Убедись, что пакет правильный

import android.content.Context
import android.content.ContextWrapper
import androidx.appcompat.app.AppCompatDelegate
import com.example.nkdsify.ui.utils.SettingsRepository
import java.util.Locale

object ContextUtils {

    fun updateLocale(context: Context): ContextWrapper {
        // 1. Получаем сохраненный код языка
        val languageCode = SettingsRepository.getLanguage(context).code
        val localeToSwitchTo = if (languageCode == "system") {
            // Если "system", используем локаль по умолчанию из системы
            AppCompatDelegate.getApplicationLocales()[0] ?: Locale.getDefault()
        } else {
            // Иначе, создаем локаль из нашего кода ("ru", "en")
            Locale(languageCode)
        }

        // 2. Создаем новую конфигурацию с нашей локалью
        val configuration = context.resources.configuration
        configuration.setLocale(localeToSwitchTo)

        // 3. Создаем новый контекст с этой конфигурацией
        val updatedContext = context.createConfigurationContext(configuration)
        return ContextWrapper(updatedContext)
    }
}


/**
 * <string name="screen_title_folders">Папо4ки</string>
 *     <string name="screen_title_favorites">ФАВы</string>
 *     <string name="screen_title_settings">Хуйстрой</string>
 *     <string name="screen_title_manage_tags">Тег менеджмент</string>
 *     <string name="screen_title_trash">Трэщ</string>
 *     <string name="screen_title_all_media">Всё порно</string>
 *     <string name="album_name_all_favorites">Все ФАВы</string>
 *     <string name="dialog_ok">КО</string>
 */
