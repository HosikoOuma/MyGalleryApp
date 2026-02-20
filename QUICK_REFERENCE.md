# Быстрый справочник (Quick Reference)

## 📍 Структура папок и файлов

```
app/src/main/java/com/example/nkdsify/
├── MainActivity.kt                          # Точка входа приложения
├── MyApp.kt                                 # Главная Composable функция
├── MyAppState.kt                            # Глобальное состояние (синглтон)
├── ExternalMediaActivity.kt                 # Обработка внешних медиа (Share/Open with)
├── ContextUtils.kt                          # Утилиты для контекста (локализация)
├── NotificationUtils.kt                     # Уведомления приложения
│
├── data/
│   ├── DataModels.kt                        # Все модели данных (Screen, MediaItem, etc)
│   └── MediaLoader.kt                       # Загрузка медиа из MediaStore
│
└── ui/
    ├── MyAppNavigation.kt                   # Роутер между экранами
    ├── MyAppTopBar.kt                       # Верхняя панель с поиском
    ├── MyAppBottomBar.kt                    # Нижняя панель (навигация экранов)
    ├── MyAppFAB.kt                          # Плавающая кнопка (shuffle/camera)
    ├── AppBars.kt                           # Конфигурация панелей
    ├── BottomBarAnimation.kt                # Анимация нижней панели
    ├── MyAppBackHandler.kt                  # Обработка back button
    │
    ├── components/
    │   ├── MediaViewer.kt                   # ГЛАВНЫЙ: просмотр фото/видео с контролами
    │   ├── MediaGrid.kt                     # Сетка медиа с селектором
    │   ├── VideoPlayerPage.kt               # Плеер видео (ExoPlayer)
    │   ├── ZoomableImage.kt                 # Компонент для pinch & double-tap zoom
    │   ├── AlbumDetailsDialog.kt            # Диалог информации о папке
    │   ├── BackupAndRestoreDialog.kt        # Резервное копирование тегов/избранных
    │   ├── FolderSelectionDialog.kt         # Выбор папки для операций
    │   ├── HiddenFoldersDialog.kt           # Управление скрытыми папками
    │   ├── TagEditDialog.kt                 # Редактирование тегов
    │   ├── UpdateDialog.kt                  # Диалог обновления приложения
    │   └── utils/
    │       ├── rememberCoilImageLoader()    # Инициализация Coil loader
    │
    ├── dialogs/
    │   ├── TagDialogs.kt                    # Диалоги добавления/импорта тегов
    │   ├── FolderDialogs.kt                 # Создание/переименование папок
    │   ├── DeletionDialogs.kt               # Подтверждение удаления
    │   ├── RestorationDialogs.kt            # Восстановление файлов
    │   ├── InfoDialogs.kt                   # Детали медиа, обрезка
    │   ├── OthersDialogs.kt                 # Прочие диалоги
    │   ├── PermissionPermanentlyDeniedDialog.kt
    │   └── SpecialLanguageDialog.kt
    │
    ├── impl/                                # Реализация экранов
    │   ├── FoldersScreen.kt                 # Экран папок
    │   ├── FolderContentScreen.kt           # Содержимое папки
    │   ├── FavoritesScreenImpl.kt            # Избранные папки
    │   ├── AllMediaScreenImpl.kt             # Все медиа на устройстве
    │   ├── TrashScreenImpl.kt                # Корзина
    │   ├── SecretStorageScreenImpl.kt        # Секретное зашифрованное хранилище
    │   ├── TagManagementScreenImpl.kt        # Управление тегами
    │   ├── MediaByTagScreenImpl.kt           # Медиа по конкретному тегу
    │   ├── ViewHistoryScreenImpl.kt          # История просмотра
    │   └── SettingsScreenImpl.kt             # Настройки (большой файл)
    │
    ├── screens/                             # Компоненты экранов (без логики)
    │   ├── SettingsState.kt                 # State и Actions для SettingsScreen
    │   ├── SettingsComponents.kt            # Отдельные компоненты настроек
    │   ├── SettingsScreen.kt                # Сам экран настроек
    │   ├── FoldersGrid.kt                   # Сетка папок (UI только)
    │   ├── AboutScreen.kt                   # О приложении
    │   ├── HelpScreen.kt                    # Справка
    │   ├── WelcomeScreen.kt                 # Приветственный экран (first launch)
    │   └── HiddenFoldersScreen.kt           # Скрытые папки
    │
    ├── editor/                              # Редакторы фото/видео
    │   ├── EditActivity.kt                  # Activity для редактирования фото
    │   ├── PhotoEditorScreen.kt             # UI редактора (рисование)
    │   ├── PhotoEditorViewModel.kt          # ViewModel редактора
    │   ├── EditorModels.kt                  # Модели для редактора (DrawPath, etc)
    │   ├── video/
    │   │   └── VideoEditActivity.kt         # Activity для редактирования видео
    │   │
    │   └── (может быть больше компонентов)
    │
    ├── theme/
    │   ├── Theme.kt                         # Определение Material You темы
    │   ├── Color.kt                         # Цвета (light, dark, amoled)
    │   ├── Typography.kt                    # Типографика
    │   └── Shape.kt                         # Shapes
    │
    └── utils/                               # ОЧЕНЬ ВАЖНОЕ - все утилиты
        ├── SettingsRepository.kt            # 💾 SharedPreferences для настроек
        ├── FavoritesRepository.kt           # 💾 Управление избранными папками
        ├── TagsRepository.kt                # 💾 Управление тегами (JSON в SP)
        ├── TrashRepository.kt               # 💾 Корзина (отложенное удаление)
        ├── SecretRepository.kt              # 🔐 Зашифрованное хранилище
        ├── ViewHistoryRepository.kt         # 📜 История просмотра
        │
        ├── CryptoUtils.kt                   # 🔐 AES шифрование (Android KeyStore)
        ├── BiometricUtils.kt                # 👤 Биометрическая аутентификация
        ├── FileUtils.kt                     # 📁 Операции с файлами (copy, move)
        ├── Utils.kt                         # 🛠️ Общие утилиты (форматирование, диалоги)
        │
        ├── GithubUpdateChecker.kt           # 🔄 Проверка обновлений через GitHub API
        ├── ShakeDetector.kt                 # 📳 Обнаружение встряхивания
        ├── VibrationUtils.kt                # 📳 Вибрация устройства
        ├── performVibration()               # Функция вибрации
        │
        ├── MediaSanitizer.kt                # Санитайзер медиа (фильтрация)
        ├── EncryptedImageDecoder.kt         # 🔐 Декодирование зашифрованных изображений
        ├── EnumExtensions.kt                # Extension функции для enum
        ├── MigrationUtils.kt                # 🔀 Миграция данных между версиями
        ├── QueryParser.kt                   # Парсер поисковых запросов (теги, текст)
        │
        ├── AesStreamingDataSource.kt        # Streaming для зашифрованных файлов
        ├── ExternalMediaErrorDialog()       # Диалог ошибок при работе с внешними файлами
        └── getMediaDetails()                # Получение информации о медиа
```

---

## 🔑 Ключевые классы и их назначение

| Класс | Назначение | Использование |
|-------|-----------|-------|
| **MyAppState** | Глобальное состояние приложения | `rememberMyAppState()` |
| **MediaItem** | Элемент медиа (файл) | Везде где работаем с файлами |
| **MediaFolder** | Папка с медиа | Список всех папок |
| **Screen** | Типы экранов (sealed class) | Навигация между экранами |
| **MyAppNavigation** | Роутер | Переключение экранов |
| **MediaViewer** | Просмотр с контролами | Просмотр фото/видео |
| **SettingsRepository** | SharedPreferences утилиты | Все настройки |
| **TagsRepository** | Система тегов | Теги для медиа |
| **SecretRepository** | Зашифрованное хранилище | Secret Storage |
| **CryptoUtils** | AES криптография | Шифрование/расшифровка |

---

## 🚀 Как работать с проектом

### Добавить новый экран
1. Создать файл в `ui/impl/` (например `NewScreen.kt`)
2. Добавить `object NewScreen : Screen()` в `DataModels.kt`
3. Добавить обработчик в `MyAppNavigation.kt`
4. Добавить в bottom bar если нужна навигация

### Добавить новую настройку
1. Добавить в `SettingsRepository` (getXXX / setXXX)
2. Добавить в `MyAppState` (var XXX by mutableStateOf(...))
3. Добавить UI компонент в `SettingsScreen.kt`
4. Обновить `SettingsState.kt` если нужно

### Добавить новую функцию к медиа
1. Добавить в `MediaItem` если нужны новые свойства
2. Реализовать логику в утилитах
3. Добавить UI в `MediaViewer` контролы
4. Обновить `MyAppState` если нужно сохранять состояние

---

## 📊 Основные потоки данных

### При загрузке приложения
```
MainActivity.onCreate()
  ↓
setContent { MyApp() }
  ↓
rememberMyAppState() → загрузка настроек из SP
  ↓
LaunchedEffect → MigrationUtils.runMigration()
  ↓
MediaLoader.loadAllMedia() → myAppState.allFolders
  ↓
MyAppNavigation рендерит текущий экран
```

### При просмотре медиа
```
User клики на элемент в MediaGrid
  ↓
MediaViewer компонент показывается
  ↓
HorizontalPager загружает изображения через Coil
  ↓
ViewHistoryRepository.addToHistory(item)
  ↓
User закрывает → viewerState = null
```

### При поиске
```
User вводит текст в SearchBar
  ↓
myAppState.searchQuery = text
  ↓
MyAppNavigation пересчитывает visibleFolders/filteredItems
  ↓
UI обновляется с отфильтрованными результатами
```

---

## 🎨 Color scheme и Material You

```kotlin
// Темы определены в theme/Color.kt
// Используются светлые, темные и AMOLED варианты

// Использование в Composable
val backgroundColor = MaterialTheme.colorScheme.background
val primaryColor = MaterialTheme.colorScheme.primary
val errorColor = MaterialTheme.colorScheme.error
```

---

## 💾 SharedPreferences структура

```
PREFS_NAME = "app_settings"
├── blur_enabled: Boolean
├── blur_type: String (BLUR/PLACEHOLDER)
├── theme: String (SYSTEM/LIGHT/DARK/AMOLED)
├── language: String (en/ru/system/xx)
├── font_family: String (SYSTEM/JETBRAINS_MONO/GOOGLE_SANS)
├── zoom_type: String (DOUBLE_TAP/PINCH)
├── vibration_enabled: Boolean
├── show_file_count: Boolean
├── shake_to_blur: Boolean
├── loop_video: Boolean
├── swipe_to_dismiss: Boolean
├── use_large_fab: Boolean
├── auto_delete_trash_enabled: Boolean
├── auto_delete_trash_days: Int
├── keep_controls_visible: Boolean
├── blurred_uris: StringSet
└── viewer_controls_position: String (TOP/BOTTOM)

"MyGalleryAppPrefs"
└── favorites: StringSet (пути папок)

"media_tags"
├── tags_map: JSON (String → Map)
└── all_tags_list: JSON (List)
```

---

## 🔐 Криптография

```kotlin
// Все шифрование в CryptoUtils
val TRANSFORMATION = "AES/CBC/PKCS7"
val KEY_ALIAS = "secret_storage_key"
val ANDROID_KEYSTORE = "AndroidKeyStore"

// Использование
CryptoUtils.encrypt(inputStream, outputStream)
CryptoUtils.decrypt(inputStream, outputStream)

// IV автоматически генерируется и записывается в поток
```

---

## 📱 Разрешения

```xml
<!-- Обязательные -->
READ_MEDIA_IMAGES (API 33+)
READ_MEDIA_VIDEO (API 33+)
READ_EXTERNAL_STORAGE (API <33)
WRITE_EXTERNAL_STORAGE

<!-- Опциональные -->
MANAGE_EXTERNAL_STORAGE (для полного доступа)
VIBRATE
INTERNET (для обновлений)
SET_WALLPAPER
CAMERA
POST_NOTIFICATIONS
```

---

## 🧪 Тестирование

### Unit тесты
```
app/src/test/java/com/example/nkdsify/ExampleUnitTest.kt
```

### UI тесты
```
app/src/androidTest/java/com/example/nkdsify/ExampleInstrumentedTest.kt
```

---

## 🐛 Частые ошибки и решения

### Проблема: NPE при работе с MediaStore
```kotlin
// ❌ Неправильно
context.contentResolver.query(...)?.use { cursor ->
    cursor.moveToFirst() // Может быть cursor = null
}

// ✅ Правильно
context.contentResolver.query(...)?.use { cursor ->
    if (cursor.moveToFirst()) {
        // работа с cursor
    }
}
```

### Проблема: Утечка памяти от SensorManager
```kotlin
// ✅ Добавить в onDestroy
override fun onDestroy() {
    super.onDestroy()
    sensorManager?.unregisterListener(shakeDetector)
}
```

### Проблема: LazyGrid не скроллит
```kotlin
// ❌ Неправильно
LazyVerticalGrid(...) {
    items(1000) { // Слишком много элементов сразу
    }
}

// ✅ Правильно - использовать Paging 3 для больших списков
val pagingFlow = Pager(PagingConfig(pageSize = 50)) {
    MediaPagingSource()
}.flow

val lazyPagingItems = collectAsLazyPagingItems()
LazyVerticalGrid(...) {
    items(lazyPagingItems.itemCount) { index ->
        lazyPagingItems[index]?.let { item ->
            // Отрисовать item
        }
    }
}
```

---

## 📚 Полезные ссылки

- [Jetpack Compose docs](https://developer.android.com/jetpack/compose/documentation)
- [Material Design 3](https://m3.material.io/)
- [Android MediaStore](https://developer.android.com/reference/android/provider/MediaStore)
- [Coil Image Loading](https://coil-kt.github.io/coil/)
- [Media3/ExoPlayer](https://developer.android.com/guide/topics/media/exoplayer)
- [Android Security & Cryptography](https://developer.android.com/training/articles/keystore)

---

## 🔄 Git workflow рекомендация

```bash
# Создать ветку
git checkout -b feature/new-feature

# Делать commits
git commit -m "feat: description of changes"

# Push
git push origin feature/new-feature

# Pull request на main
```

---

## 💡 Советы для новичков

1. **Перед тем как менять что-то**: прочитайте `PROJECT_ANALYSIS.md`
2. **Если не ясна архитектура**: смотрите `ARCHITECTURE_AND_DEPENDENCIES.md`
3. **Если хотите добавить функцию**: читайте `DEVELOPMENT_ROADMAP.md`
4. **Не меняйте MyAppState напрямую из разных мест**: используйте callbacks
5. **Для новых Composables**: начните с простого, потом рефакторьте
6. **Тестируйте все**: особенно разрешения и работу с файлами

---

## 🎯 Иерархия состояния

```
MyAppState (главное состояние)
  ├─ Settings состояние (из SharedPreferences)
  ├─ UI состояние (диалоги, текущий экран)
  ├─ Data состояние (folders, media, history)
  ├─ Selection состояние (selectedItems)
  └─ Composition-provided (imageLoader, coroutineScope, gridStates)
```

**Правило**: Никогда не создавайте отдельное локальное состояние если это нужно сохранять!

---

## ⚡ Performance tips

1. Используйте `ImmutableList` для больших списков
2. Используйте `LazyVerticalGrid` вместо `Column`
3. Используйте `remember` для дорогих операций
4. Используйте `rememberUpdatedState` для callbacks
5. Используйте `derivedStateOf` для вычисляемых значений
6. Не вызывайте `loadAllMedia()` повторно если данные есть

---

Удачи в разработке! 🚀

