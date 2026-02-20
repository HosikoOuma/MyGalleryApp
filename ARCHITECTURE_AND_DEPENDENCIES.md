# Диаграмма зависимостей и связей (MyGalleryApp)

## 1. Граф зависимостей компонентов

```
┌─────────────────────────────────────────────────────────────┐
│                      MainActivity                            │
│  (Точка входа, инициализация, обработка разрешений)         │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ↓
        ┌──────────────────────────────┐
        │      MyApp Composable        │
        │  (Главный UI контейнер)      │
        └──────────────────────────────┘
                       │
         ┌─────────────┼─────────────┐
         ↓             ↓             ↓
    ┌────────┐  ┌──────────┐  ┌──────────────┐
    │MyAppFAB│  │AppBars   │  │MyAppBackHand │
    └────────┘  │(Top/Bot) │  └──────────────┘
                └──────────┘
         ↓
    MyAppNavigation
    (Роутер между экранами)
         │
    ┌────┼────┬────┬──────┬───────┬────────┬──────────┐
    ↓    ↓    ↓    ↓      ↓       ↓        ↓          ↓
Folders Fav All Trash Secret TagMgmt ViewHist Settings
Screen  Screen Media Screen Storage Screen  Screen  Screen
    │
    └─→ FoldersGrid ─→ MediaGrid ─→ MediaViewer
                        (Compose)    (Горизонтальное
                                      пролистывание)

MyAppState (Глобальное состояние)
    │
    ├─→ allFolders: ImmutableList<MediaFolder>
    ├─→ allMedia: ImmutableList<MediaItem>
    ├─→ favoriteItems: ImmutableList<MediaItem>
    ├─→ trashedItems: ImmutableList<MediaItem>
    ├─→ secretItems: ImmutableList<MediaItem>
    ├─→ selectedItems: MutableList<Uri> (для выделения)
    ├─→ tags: Map<String, Set<String>>
    ├─→ viewHistory: ImmutableList<MediaItem>
    │
    ├─→ [Settings]
    │   ├─→ selectedTheme
    │   ├─→ selectedLanguage
    │   ├─→ selectedFontFamily
    │   ├─→ isBlurEnabled
    │   ├─→ selectedBlurType
    │   └─→ ... и еще 30+ флагов
    │
    ├─→ [UI States]
    │   ├─→ currentScreen
    │   ├─→ isSearchActive / searchQuery
    │   ├─→ showTagDialog
    │   ├─→ showDetailsDialog
    │   └─→ ... и еще диалоги
    │
    └─→ [Grid States]
        ├─→ foldersGridState
        ├─→ folderContentGridState
        ├─→ favoritesGridState
        └─→ ... и т.д.
```

## 2. Архитектурные слои

### Уровень UI (Presentation Layer)
```
┌─────────────────────────────────────────┐
│              Screens (UI)                │
├──────────────────────────────────────────┤
│ • FoldersScreen                         │
│ • FolderContentScreen                   │
│ • FavoritesScreen                       │
│ • AllMediaScreen                        │
│ • TrashScreen                           │
│ • SecretStorageScreen                   │
│ • TagManagementScreen                   │
│ • SettingsScreen                        │
│ • ViewHistoryScreen                     │
└─────────────────────────────────────────┘
           ↓ (использует)
┌─────────────────────────────────────────┐
│         Components & Composables         │
├──────────────────────────────────────────┤
│ • MediaViewer (HorizontalPager)          │
│ • MediaGrid (LazyVerticalGrid)           │
│ • VideoPlayerPage (Media3)               │
│ • ZoomableImage (Pinch & DoubleTap)     │
│ • Dialogs (TagDialog, DeleteDialog, ...) │
└─────────────────────────────────────────┘
           ↓ (управляется)
┌─────────────────────────────────────────┐
│        MyAppState & Navigation           │
├──────────────────────────────────────────┤
│ • MyAppState (синглтон состояния)        │
│ • MyAppNavigation (роутер)               │
│ • Screen sealed class (типы экранов)     │
└─────────────────────────────────────────┘
```

### Уровень бизнес-логики (Domain Layer)
```
┌──────────────────────────────────┐
│   Repositories (Бизнес-логика)   │
├──────────────────────────────────┤
│ • SettingsRepository             │
│ • FavoritesRepository            │
│ • TagsRepository                 │
│ • SecretRepository               │
│ • TrashRepository                │
│ • ViewHistoryRepository          │
└──────────────────────────────────┘
           ↓
┌──────────────────────────────────┐
│    Utilities (вспомогательные)   │
├──────────────────────────────────┤
│ • CryptoUtils                    │
│ • FileUtils                      │
│ • MediaSanitizer                 │
│ • GithubUpdateChecker            │
│ • ShakeDetector                  │
│ • BiometricUtils                 │
└──────────────────────────────────┘
```

### Уровень данных (Data Layer)
```
┌──────────────────────────────────┐
│      Источники данных (Data)     │
├──────────────────────────────────┤
│ • MediaStore (система Android)   │
│ • SharedPreferences (локально)   │
│ • FileSystem (файлы)             │
│ • Android KeyStore (шифрование)  │
│ • GitHub API (обновления)        │
└──────────────────────────────────┘
           ↓ (загружается через)
┌──────────────────────────────────┐
│      Loaders & Parsers           │
├──────────────────────────────────┤
│ • MediaLoader                    │
│ • EncryptedImageDecoder          │
│ • MediaSanitizer                 │
│ • QueryParser                    │
└──────────────────────────────────┘
```

## 3. Граф зависимостей по функциям

### Просмотр медиа
```
MediaViewer
    ├─→ MediaViewerState (какое медиа показывать)
    ├─→ HorizontalPager (для свайпа)
    ├─→ ZoomableImage или VideoPlayerPage
    │   ├─→ Coil ImageLoader (загрузка изображений)
    │   ├─→ ExoPlayer (видео)
    │   └─→ CryptoUtils (расшифровка для secret storage)
    └─→ Controls
        ├─→ TagEditDialog
        ├─→ MediaDetailsDialog
        ├─→ DeleteDialog
        └─→ ShareButton
```

### Система тегирования
```
TagsRepository
    ├─→ getTags() → Map<String, Set<String>>
    ├─→ setTagsForItem()
    ├─→ addNewTag()
    ├─→ removeTagFromAllItems()
    └─→ renameTag()

MyAppNavigation
    ├─→ parseQueryString() (парсит поиск с тегами)
    └─→ Фильтрует элементы по тегам

UI: TagManagementScreen, TagDialog, TagEditDialog
```

### Секретное хранилище
```
SecretRepository
    ├─→ moveToSecret()
    │   ├─→ CryptoUtils.encrypt()
    │   └─→ createThumbnail()
    ├─→ restoreFromSecret()
    │   ├─→ CryptoUtils.decrypt()
    │   └─→ restoreFile()
    └─→ getSecretItems()

SecretStorageScreen
    ├─→ BiometricUtils (аутентификация)
    └─→ MediaViewer с isSecretMode=true
        └─→ EncryptedImageDecoder (расшифровка на лету)
```

### Система поиска
```
MyAppNavigation (searchQuery)
    ├─→ QueryParser.parseQueryString()
    │   ├─→ Извлекает текстовые терми́ны
    │   ├─→ Извлекает группы тегов (+tag1, +tag2)
    │   └─→ Извлекает исключенные теги (-tag3)
    │
    └─→ Фильтрует:
        ├─→ visibleFolders
        ├─→ filteredFavoriteItems
        ├─→ filteredAllMedia
        └─→ filteredTrashItems

UI: SearchBar в MyAppTopBar
```

### Система размытия
```
MyAppState.blurredUris: Set<String>
    ├─→ SettingsRepository.setBlurredUris()
    │
    ├─→ MediaGrid
    │   ├─→ Если isBlurEnabled = true
    │   └─→ Применяет BlurType (BLUR или PLACEHOLDER)
    │
    ├─→ ShakeDetector
    │   └─→ При встряхивании переключает все blur флаги
    │
    └─→ toggleBlurForUris(uris: List<Uri>)
        └─→ Скрывает/раскрывает отдельные файлы
```

### Система разрешений и хранилища
```
MainActivity
    ├─→ permissionsToRequest: Array<String>
    │   ├─→ Android 13+: READ_MEDIA_IMAGES, READ_MEDIA_VIDEO
    │   └─→ Android <13: READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE
    │
    ├─→ manageStorageLauncher
    │   └─→ Запрос MANAGE_EXTERNAL_STORAGE (Android 11+)
    │
    └─→ MediaLoader
        └─→ Использует разрешения для чтения MediaStore

MyApp
    ├─→ MigrationUtils (миграция старых данных)
    └─→ Загрузка медиа в allFolders, allMedia, etc.
```

## 4. Взаимодействие с Android системой

### MediaStore интеграция
```
MediaStore (система Android)
    ├─→ MediaStore.Files.getContentUri()
    ├─→ MediaStore.Images.Media
    ├─→ MediaStore.Video.Media
    │
    └─→ Запросы:
        ├─→ loadAllMedia() - все файлы
        ├─→ loadMediaFolders() - папки
        ├─→ loadFavoriteMediaItems() - избранные
        └─→ loadTrashedMediaItems() - корзина
```

### Content Provider операции
```
context.contentResolver
    ├─→ query() - чтение медиа
    ├─→ openInputStream/openOutputStream - работа с файлами
    ├─→ insert() - добавление файлов
    ├─→ update() - изменение свойств
    └─→ delete() - удаление (в корзину при API 30+)
```

### Экспорт и обмен файлами
```
FileUtils
    ├─→ copyMediaToFolder()
    │   └─→ context.contentResolver.insert/update
    │
    ├─→ moveMediaBetweenFolders()
    │   └─→ Копирует → удаляет оригинал
    │
    └─→ Интеграция с другими приложениями
        ├─→ Intent.ACTION_SEND (Share)
        ├─→ Intent.ACTION_VIEW (Open with)
        └─→ Intent.ACTION_EDIT (Edit)
```

## 5. Граф состояний приложения

```
┌─────────────────────┐
│   App Initialized   │
└──────────┬──────────┘
           │
           ↓
┌─────────────────────────────────────┐
│   Request Permissions & Load Data   │
├─────────────────────────────────────┤
│ • Ask READ_MEDIA_IMAGES             │
│ • Ask READ_EXTERNAL_STORAGE         │
│ • Load MyAppState                   │
│ • Load all MediaStore data          │
└─────────────────────────────────────┘
           │
           ↓
┌─────────────────────┐
│   MainScreen Ready  │
└──────────┬──────────┘
           │
    ┌──────┴──────┬────────┬──────────┐
    ↓             ↓        ↓          ↓
Folders       Favorites  All Media  Settings
 Screen       Screen     Screen     Screen
    │
    └─→ FoldersGrid (click) → FolderContent
           │                       │
           └─→ MediaGrid ←──────┤
                  │
                  ↓
             MediaViewer
                  │
          ┌───────┼───────┐
          ↓       ↓       ↓
        Tag    Share   Delete
      Dialog  Intent   Dialog
```

## 6. Жизненный цикл медиа-файла в приложении

### Путь 1: Просмотр файла
```
1. MediaStore.query() → MediaItem
2. MediaGrid отображает (с blur если нужно)
3. User clicks → MediaViewerState создается
4. MediaViewer загружает через Coil
5. ViewHistoryRepository.addToHistory()
6. User closes → ViewerState = null
```

### Путь 2: Добавление в избранное
```
1. User в MediaViewer нажимает ♥
2. FavoritesRepository.saveFavorites() (папка)
3. MyAppState.favoritesList обновляется
4. FavoritesScreen обновляет UI
```

### Путь 3: Перемещение в корзину
```
1. User нажимает Delete в MediaViewer
2. showConfirmDeleteDialog = true
3. On confirm:
   context.contentResolver.delete(uri) → MediaStore.TRASH_VOLUME
4. TrashRepository.addToTrash()
5. tashedItems обновляются в MyAppState
6. Загружается с задержкой (autoDeleteTrashDays)
```

### Путь 4: Перемещение в Secret Storage
```
1. User нажимает Lock в MediaViewer
2. showConfirmMoveToSecretDialog = true
3. SecretRepository.moveToSecret(uri):
   a. Читает оригинальный файл
   b. CryptoUtils.encrypt() → .secret папка
   c. Создает зашифрованную миниатюру
   d. Удаляет оригинальный файл
4. SecretStorageScreen загружает через EncryptedImageDecoder
```

### Путь 5: Редактирование фото
```
1. User в MediaViewer нажимает Edit
2. context.startActivity(EditActivity, uri)
3. PhotoEditorViewModel.loadBitmap(uri)
4. Pользователь рисует/редактирует
5. Сохраняет:
   a. Оригинальный файл обновляется
   b. Или создается новый копия
6. Возврат в галерею, UI обновляется
```

## 7. Поток инициализации при запуске

```
1. MainActivity.onCreate()
   ├─→ installSplashScreen()
   ├─→ sensorManager = getSystemService(SENSOR_SERVICE)
   ├─→ shakeDetector = ShakeDetector()
   │
   └─→ setContent { // Composable block
       ├─→ rememberMyAppState(context)
       │   └─→ MyAppState инициализируется
       │       ├─→ SettingsRepository.getBlurEnabled()
       │       ├─→ SettingsRepository.getTheme()
       │       └─→ и еще 30+ вызовов
       │
       ├─→ isFirstLaunch = SettingsRepository.isFirstLaunch()
       │
       ├─→ LaunchedEffect:
       │   ├─→ delay(1000) // Splash screen
       │   └─→ isAppReady = true
       │
       ├─→ shakeDetector.setOnShakeListener
       │
       ├─→ permissionLauncher.launch(permissions)
       │   └─→ На confirm: myAppState.hasPermissions = true
       │
       └─→ MyApp(myAppState)
           ├─→ MigrationUtils.runMigrationIfNeeded()
           ├─→ myAppState.checkForUpdates(false)
           ├─→ Инициализация grid states
           ├─→ Загрузка favorites
           ├─→ TagsRepository.getAllTags()
           └─→ MyAppNavigation(myAppState)
               └─→ Отрисовка текущего экрана
```

## 8. Корутины и асинхронные операции

```
rememberCoroutineScope()
    ├─→ MediaLoader.loadAllMedia()
    │   └─→ withContext(Dispatchers.IO) → context.contentResolver.query()
    │
    ├─→ GithubUpdateChecker.getLatestRelease()
    │   └─→ withContext(Dispatchers.IO) → Retrofit API call
    │
    ├─→ SecretRepository.moveToSecret()
    │   └─→ withContext(Dispatchers.IO) → CryptoUtils.encrypt()
    │
    ├─→ TagsRepository операции
    │   └─→ withContext(Dispatchers.IO) → SharedPreferences
    │
    └─→ FileUtils операции
        └─→ withContext(Dispatchers.IO) → File I/O
```

## 9. Сохранение состояния (State Persistence)

```
SharedPreferences (PREFS_NAME = "app_settings")
├─→ SettingsRepository:
│   ├─→ blur_enabled, blur_type, blur_in_folder_enabled
│   ├─→ theme, language, font_family
│   ├─→ zoom_type, vibration_enabled, show_file_count
│   ├─→ shake_to_blur, loop_video, swipe_to_dismiss
│   ├─→ use_large_fab, auto_delete_trash_enabled
│   ├─→ check_for_updates_on_startup
│   └─→ blurred_uris (Set<String>)
│
├─→ FavoritesRepository (PREFS_NAME = "MyGalleryAppPrefs"):
│   └─→ favorites: Set<String> (пути к папкам)
│
├─→ TagsRepository (PREFS_NAME = "media_tags"):
│   ├─→ tags_map: JSON (Map<String, Set<String>>)
│   └─→ all_tags_list: JSON (List<String>)
│
└─→ ViewHistoryRepository:
    └─→ view_history: JSON (List<MediaItem>)

FileSystem
├─→ Context.filesDir + ".secret" → SecretRepository
└─→ App cache dir → EncryptedImageDecoder (кэш расшифрованных)
```

---

## Резюме архитектуры

Приложение следует **многослойной архитектуре**:
- **Presentation (UI)**: Composable, Screens, Components
- **Domain (Business Logic)**: Repositories, Utilities, State Management
- **Data (Sources)**: MediaStore, SharedPreferences, FileSystem, APIs

**Основной паттерн управления состоянием**: Centralized State (MyAppState) с использованием Compose State Management.

**Поток данных**: Unidirectional (от Data → Domain → UI с вернутыми callbacks).

