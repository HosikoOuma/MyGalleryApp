ций# Рекомендации для дальнейшей разработки

## 1. Текущие проблемы и зоны совершенствования

### 🔴 Критические

#### 1.1 Обработка ошибок
**Проблема**: Недостаточная обработка исключений во многих местах
```kotlin
// Текущее состояние - часто нет try/catch
context.contentResolver.query(...)?.use { cursor ->
    // Если query вернет null - может быть NPE
}
```

**Рекомендация**:
```kotlin
try {
    context.contentResolver.query(...)?.use { cursor ->
        // Обработка
    }
} catch (SecurityException e) {
    // Обработать недостаток разрешений
} catch (Exception e) {
    Log.e(TAG, "Failed to query media", e)
    showErrorDialog()
}
```

#### 1.2 Утечки памяти
**Проблема**: ShakeDetector, ImageLoader, SensorManager могут вызвать утечки
```kotlin
// MainActivity
sensorManager?.unregisterListener(shakeDetector)
```

**Рекомендация**:
```kotlin
class MainActivity : AppCompatActivity() {
    private var sensorManager: SensorManager? = null
    private var shakeDetector: ShakeDetector? = null
    
    override fun onDestroy() {
        super.onDestroy()
        sensorManager?.unregisterListener(shakeDetector)
        shakeDetector = null
        sensorManager = null
    }
}
```

#### 1.3 Производительность MediaStore query
**Проблема**: `loadAllMedia()` выполняет большой запрос синхронно в UI потоке
**Решение**: Использовать Paging 3 или Room database

### 🟡 Высокий приоритет

#### 2.1 Добавить Room Database
```kotlin
// Добавить зависимость
implementation("androidx.room:room-runtime:2.6.0")
kapt("androidx.room:room-compiler:2.6.0")

// Создать сущности для кэширования
@Entity
data class CachedMediaItem(
    @PrimaryKey
    val uri: String,
    val name: String,
    val isVideo: Boolean,
    // ...
)

@Database(entities = [CachedMediaItem::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mediaItemDao(): MediaItemDao
}
```

#### 2.2 Улучшить загрузку медиа с Paging 3
```kotlin
class MediaPagingSource(
    private val context: Context,
    private val sortType: SortType
) : PagingSource<Int, MediaItem>() {
    
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaItem> {
        val page = params.key ?: 0
        val pageSize = params.loadSize
        val offset = page * pageSize
        
        return try {
            val items = loadMediaWithOffset(context, offset, pageSize, sortType)
            LoadResult.Page(
                data = items,
                prevKey = if (page == 0) null else page - 1,
                nextKey = if (items.isEmpty()) null else page + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}

// В UI
val mediaPagingFlow = Pager(
    config = PagingConfig(pageSize = 50, enablePlaceholders = false),
    pagingSourceFactory = { MediaPagingSource(context, sortType) }
).flow.cachedIn(viewModelScope)
```

#### 2.3 Переименовать и очистить файлы с комментариями "GEMINI НЕ ТРОГАЙ"
**Текущее состояние**:
```kotlin
//GEMINI НЕ ТРОГАЙ ЭТУ АНОТАЦИЮ
@file:kotlin.OptIn(ExperimentalMaterial3Api::class)
```

**Рекомендация**: Документировать причину OptIn аннотаций
```kotlin
/**
 * ExperimentalMaterial3Api требуется для использования
 * TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
 */
@file:OptIn(ExperimentalMaterial3Api::class)
```

#### 2.4 Обработка SAF (Storage Access Framework)
**Проблема**: При работе с внешним хранилищем нужно использовать SAF вместо прямого доступа

```kotlin
// Добавить в MainActivity
private val directoryPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocumentTree()
) { treeUri ->
    treeUri?.let { uri ->
        contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        // Работать с uri через DocumentFile
    }
}
```

### 🟢 Средний приоритет

#### 3.1 Добавить Unit тесты
```kotlin
// ExampleUnitTest.kt уже существует
class MediaLoaderTest {
    @Test
    fun testLoadAllMediaEmpty() {
        val result = loadAllMedia(
            context = mockContext,
            sortType = SortType.DATE_MODIFIED,
            sortAscending = false,
            hiddenFolderIds = emptySet()
        )
        Assert.assertTrue(result.isEmpty())
    }
    
    @Test
    fun testLoadAllMediaWithHiddenFolders() {
        val result = loadAllMedia(
            context = mockContext,
            sortType = SortType.DATE_MODIFIED,
            sortAscending = false,
            hiddenFolderIds = setOf("1", "2")
        )
        Assert.assertFalse(result.any { it.id.toString() in setOf("1", "2") })
    }
}
```

#### 3.2 Добавить Instrumentation тесты
```kotlin
class MediaViewerTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun testMediaViewerNavigation() {
        composeTestRule.setContent {
            MediaViewer(
                myAppState = mockMyAppState,
                items = testMediaItems,
                startIndex = 0
            )
        }
        
        composeTestRule.onNodeWithTag("media_pager").performSwipeLeft()
        // Assert что произошло переключение
    }
}
```

#### 3.3 Добавить логирование
```kotlin
object AppLogger {
    private const val TAG = "MyGalleryApp"
    
    fun d(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message)
        }
    }
    
    fun e(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
    }
}

// Использование
AppLogger.d("Loading media from: $path")
AppLogger.e("Failed to encrypt file", exception)
```

#### 3.4 Улучшить обработку Thumb (миниатюр)
```kotlin
// Добавить кэширование миниатюр в Room
@Entity
data class MediaThumbnail(
    @PrimaryKey
    val mediaUri: String,
    val thumbnailPath: String,
    val generatedAt: Long = System.currentTimeMillis()
)

// Периодически очищать старые миниатюры
fun cleanOldThumbnails(database: AppDatabase, maxAgeMs: Long = 7 * 24 * 60 * 60 * 1000) {
    val cutoffTime = System.currentTimeMillis() - maxAgeMs
    database.thumbnailDao().deleteOlderThan(cutoffTime)
}
```

#### 3.5 Добавить Kotlin Flow для реактивности
```kotlin
// Вместо mutableStateOf
class MyAppState {
    private val _allMediaFlow = MutableStateFlow<ImmutableList<MediaItem>>(persistentListOf())
    val allMediaFlow: StateFlow<ImmutableList<MediaItem>> = _allMediaFlow.asStateFlow()
    
    private val _searchQueryFlow = MutableStateFlow("")
    val searchQueryFlow: StateFlow<String> = _searchQueryFlow.asStateFlow()
    
    // Комбинировать потоки
    val filteredMediaFlow: Flow<ImmutableList<MediaItem>> = combine(
        _allMediaFlow,
        _searchQueryFlow
    ) { allMedia, query ->
        if (query.isBlank()) allMedia
        else allMedia.filter { it.name.contains(query, ignoreCase = true) }.toImmutableList()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = persistentListOf()
    )
}
```

---

## 2. Новые функции для добавления

### 🆕 Приоритет 1: Базовые улучшения

#### 2.1.1 Поддержка GIF анимаций
```kotlin
// Уже есть зависимость coil-gif
// Просто нужно использовать ImageRequest.Builder
val imageLoader = ImageLoader(context) {
    components {
        add(ImageDecoderDecoder.Factory()) // Для GIF
    }
}
```

#### 2.1.2 Микро-фоновая синхронизация
```kotlin
// Добавить WorkManager
implementation("androidx.work:work-runtime-ktx:2.8.1")

class BackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            TagsRepository.backupTags(context)
            FavoritesRepository.backupFavorites(context)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

// Запланировать резервное копирование раз в день
val backupWork = PeriodicWorkRequestBuilder<BackupWorker>(
    1, TimeUnit.DAYS
).build()

WorkManager.getInstance(context).enqueueUniquePeriodicWork(
    "backup",
    ExistingPeriodicWorkPolicy.KEEP,
    backupWork
)
```

#### 2.1.3 Улучшенный обработчик ошибок
```kotlin
class ErrorHandler {
    fun handleMediaLoadError(
        context: Context,
        error: Exception,
        callback: (retry: Boolean) -> Unit
    ) {
        val message = when (error) {
            is SecurityException -> context.getString(R.string.permission_denied)
            is IOException -> context.getString(R.string.file_not_found)
            else -> context.getString(R.string.unknown_error)
        }
        
        // Показать Snackbar с кнопкой Retry
    }
}
```

### 🆕 Приоритет 2: Продвинутые функции

#### 2.2.1 Распознавание лиц (Face Detection)
```kotlin
// Добавить зависимость
implementation("com.google.android.gms:play-services-mlkit-face-detection:17.0.0")

suspend fun detectFaceInImage(bitmap: Bitmap): Boolean = withContext(Dispatchers.Default) {
    val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .build()
    )
    
    val image = InputImage.fromBitmap(bitmap, 0)
    val faces = Tasks.await(detector.process(image))
    
    faces.isNotEmpty()
}

// Использование для автоматического размытия
if (detectFaceInImage(bitmap)) {
    myAppState.toggleBlurForUris(listOf(uri))
}
```

#### 2.2.2 Поддержка HEIC/HEIF форматов
```kotlin
// Уже есть в Coil, но нужна дополнительная библиотека
implementation("io.coil-kt:coil-video:2.7.0") // расширение для видео

// Для HEIC нужен CustomDecoder
class HeicDecoder : Decoder {
    override suspend fun decode(): DecodeResult? {
        // Использовать BitmapFactory с HEIC поддержкой
    }
}
```

#### 2.2.3 Облако синхронизация (Google Drive/OneDrive)
```kotlin
// Интеграция с Google Drive API
implementation("com.google.api-client:google-api-client-android:1.35.2")

class CloudSyncRepository(private val context: Context) {
    private val googleDriveService = getDriveService()
    
    suspend fun uploadMediaToCloud(fileUri: Uri) = withContext(Dispatchers.IO) {
        val file = File()
        file.name = getFileName(context, fileUri)
        file.mimeType = getMimeType(context, fileUri)
        
        val content = FileContent(file.mimeType, File(getPath(context, fileUri)))
        googleDriveService.files().create(file, content).execute()
    }
}
```

#### 2.2.4 Продвинутое редактирование видео
```kotlin
// Уже есть VideoEditActivity, но можно добавить больше функций
implementation("androidx.media3:media3-transformer:1.3.1")

class VideoProcessor {
    fun addWatermark(inputUri: Uri, watermarkUri: Uri): Uri {
        // Использовать Media3 Transformer
    }
    
    fun applyFilter(inputUri: Uri, filter: VideoFilter): Uri {
        // Применить фильтр (например, изменение насыщенности)
    }
    
    fun extractFrames(inputUri: Uri, frameRate: Int): List<Bitmap> {
        // Извлечь кадры из видео
    }
}
```

### 🆕 Приоритет 3: Интеграции

#### 2.3.1 Telegram/Discord интеграция
```kotlin
// Поделиться прямо в Telegram/Discord
class ShareManager(private val context: Context) {
    fun shareToTelegram(mediaUri: Uri) {
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, mediaUri)
            type = context.contentResolver.getType(mediaUri)
            `package` = "org.telegram.messenger" // или com.discord
        }
        context.startActivity(intent)
    }
}
```

#### 2.3.2 Google Photos интеграция
```kotlin
// Синхронизация с Google Photos
implementation("com.google.android.gms:play-services-photos-media-compat:1.0.0")

class GooglePhotosSync(private val context: Context) {
    suspend fun uploadToPhotos(mediaUri: Uri) {
        // Использовать Google Photos API
    }
}
```

#### 2.3.3 Analytics (Firebase/Mixpanel)
```kotlin
// Добавить Firebase
implementation("com.google.firebase:firebase-analytics:21.3.0")

object Analytics {
    fun logViewMedia(mediaType: String) {
        FirebaseAnalytics.getInstance(context).logEvent(
            "view_media",
            bundleOf("media_type" to mediaType)
        )
    }
    
    fun logSearchQuery(query: String, resultCount: Int) {
        FirebaseAnalytics.getInstance(context).logEvent(
            "search",
            bundleOf(
                "query" to query,
                "result_count" to resultCount
            )
        )
    }
}
```

---

## 3. Рефакторинг и оптимизация

### 3.1 Извлечение SettingsScreen компонентов
**Текущее состояние**: SettingsScreen.kt содержит много кода (вероятно 500+ строк)

**Рекомендация**:
```kotlin
// settings/SettingsComponents.kt
@Composable
fun BlurSettingsSection(myAppState: MyAppState, onBlurTypeChange: (BlurType) -> Unit) {
    // Весь код для настроек размытия
}

@Composable
fun NotificationSettingsSection(myAppState: MyAppState, onVibrationChange: (Boolean) -> Unit) {
    // Весь код для уведомлений
}

@Composable
fun AppearanceSettingsSection(myAppState: MyAppState) {
    // Тема, язык, шрифт
}

// SettingsScreen.kt
@Composable
fun SettingsScreen(/* ... */) {
    LazyColumn {
        item { BlurSettingsSection(...) }
        item { NotificationSettingsSection(...) }
        item { AppearanceSettingsSection(...) }
    }
}
```

### 3.2 Создать ViewModel для состояния экранов
```kotlin
class FoldersScreenViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<FoldersUiState>(FoldersUiState.Loading)
    val uiState: StateFlow<FoldersUiState> = _uiState.asStateFlow()
    
    fun loadFolders(context: Context, sortType: SortType) {
        viewModelScope.launch {
            _uiState.value = FoldersUiState.Loading
            try {
                val folders = loadMediaFolders(context, sortType)
                _uiState.value = FoldersUiState.Success(folders)
            } catch (e: Exception) {
                _uiState.value = FoldersUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed class FoldersUiState {
    object Loading : FoldersUiState()
    data class Success(val folders: ImmutableList<MediaFolder>) : FoldersUiState()
    data class Error(val message: String) : FoldersUiState()
}
```

### 3.3 Оптимизировать MediaViewer
```kotlin
// Добавить lazy loading для контролов
@Composable
fun MediaViewer(/* ... */) {
    var showControls by remember { mutableStateOf(true) }
    var controlsVisibleTime by remember { mutableStateOf(System.currentTimeMillis()) }
    
    // Автоматически скрывать контролы через 5 секунд
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(5000)
            showControls = false
        }
    }
    
    // Показывать контролы по клику
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures {
                    showControls = !showControls
                }
            }
    )
}
```

---

## 4. Производительность и оптимизация

### 4.1 Профилирование
```kotlin
// Использовать Jetpack Benchmark
implementation("androidx.benchmark:benchmark-junit4:1.1.1")

class ImageLoadingBenchmark : BenchmarkTestCase() {
    @Test
    fun measureImageLoading() = benchmarkRule.measureRepeated {
        val imageLoader = rememberCoilImageLoader(context)
        imageLoader.enqueue(ImageRequest.Builder(context).data(imageUri).build())
    }
}
```

### 4.2 Улучшить Coil кэширование
```kotlin
fun rememberCoilImageLoader(context: Context): ImageLoader {
    return remember {
        ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25) // 25% от памяти
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .crossfade(true)
            .respectCacheHeaders(true)
            .build()
    }
}
```

### 4.3 Lazy инициализация экранов
```kotlin
// Вместо загрузки всех данных сразу
@Composable
fun MyAppNavigation(/* ... */) {
    when (val screen = myAppState.currentScreen) {
        is Screen.Folders -> {
            // Загружать только когда нужно
            LaunchedEffect(screen) {
                if (myAppState.allFolders.isEmpty()) {
                    loadFolders()
                }
            }
            FoldersScreen(/* ... */)
        }
        is Screen.AllMedia -> {
            LaunchedEffect(screen) {
                if (myAppState.allMedia.isEmpty()) {
                    loadAllMedia()
                }
            }
            AllMediaScreen(/* ... */)
        }
    }
}
```

---

## 5. Документирование и поддержка

### 5.1 Добавить KDoc документацию
```kotlin
/**
 * Загружает все медиа-файлы с устройства с применением фильтров.
 *
 * @param context контекст приложения
 * @param sortType тип сортировки (см. [SortType])
 * @param sortAscending если true - сортировка по возрастанию
 * @param hiddenFolderIds множество ID скрытых папок для исключения
 * @param selectedDate если указана - фильтровать по дате (опционально)
 * @return неизменяемый список загруженных медиа-файлов
 * @throws SecurityException если нет разрешений на чтение
 * @throws IOException если ошибка при чтении файловой системы
 *
 * @sample
 * ```
 * val allMedia = loadAllMedia(
 *     context = context,
 *     sortType = SortType.DATE_MODIFIED,
 *     sortAscending = false,
 *     hiddenFolderIds = emptySet()
 * )
 * ```
 */
fun loadAllMedia(
    context: Context,
    sortType: SortType,
    sortAscending: Boolean,
    hiddenFolderIds: Set<String>,
    selectedDate: Long? = null
): ImmutableList<MediaItem>
```

### 5.2 Создать CONTRIBUTING.md
```markdown
# Гайд для разработчиков

## Установка
1. Клонировать репозиторий
2. Открыть в Android Studio Giraffe+
3. Синхронизировать Gradle
4. Запустить на устройстве с SDK 30+

## Код-стайл
- Использовать Kotlin style guide
- Max line length: 120
- Используйте noninline lambdas
- Документируйте public API

## Тестирование
- Писать unit тесты для logic
- Писать UI тесты для Composables
- Запускать все тесты перед PR

## Git commits
- Использовать conventional commits
- Примеры:
  - `feat: add face detection for auto-blur`
  - `fix: correct memory leak in ShakeDetector`
  - `docs: update architecture documentation`
```

---

## 6. Примеры для начинающих разработчиков

### 6.1 Как добавить новую настройку
```kotlin
// 1. Добавить в enum (если это перечисление)
enum class SomeOption {
    OPTION_A, OPTION_B
}

// 2. Добавить сохранение в SettingsRepository
fun setSomeOption(context: Context, option: SomeOption) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit { putString("some_option", option.name) }
}

fun getSomeOption(context: Context): SomeOption {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val name = prefs.getString("some_option", SomeOption.OPTION_A.name) ?: SomeOption.OPTION_A.name
    return SomeOption.valueOf(name)
}

// 3. Добавить в MyAppState
var someOption by mutableStateOf(SettingsRepository.getSomeOption(context))

// 4. Добавить в UI (SettingsScreen)
@Composable
fun SomeOptionSetting(myAppState: MyAppState, onOptionChange: (SomeOption) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Some Option")
        Spacer(modifier = Modifier.weight(1f))
        
        DropdownMenu(
            expanded = dropdownExpanded,
            onDismissRequest = { dropdownExpanded = false }
        ) {
            SomeOption.values().forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.name) },
                    onClick = {
                        onOptionChange(option)
                        dropdownExpanded = false
                    }
                )
            }
        }
    }
}

// 5. Вызвать в SettingsScreen
SomeOptionSetting(
    myAppState = myAppState,
    onOptionChange = { option ->
        myAppState.someOption = option
        SettingsRepository.setSomeOption(context, option)
    }
)
```

### 6.2 Как добавить новый экран
```kotlin
// 1. Добавить в Screen sealed class
sealed class Screen {
    // ...
    object NewScreen : Screen()
}

// 2. Создать файл NewScreenImpl.kt
@Composable
fun NewScreen(
    myAppState: MyAppState,
    gridState: LazyGridState
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        state = gridState,
        modifier = Modifier.fillMaxSize()
    ) {
        items(count = 10) { index ->
            Text("Item $index")
        }
    }
}

// 3. Добавить в MyAppNavigation
when (val screen = myAppState.currentScreen) {
    // ...
    Screen.NewScreen -> NewScreen(myAppState, gridState)
}

// 4. Добавить в bottom bar для навигации
NavigationBarItem(
    icon = { Icon(Icons.Default.NewIcon, contentDescription = "New Screen") },
    label = { Text("New") },
    selected = currentScreen == Screen.NewScreen,
    onClick = { myAppState.currentScreen = Screen.NewScreen }
)
```

---

## Заключение

Это 30% примерно от всех возможных улучшений. Главное направление развития:
1. **Стабильность** - улучшить обработку ошибок
2. **Производительность** - добавить кэширование и пагинацию
3. **Функциональность** - добавить новые форматы и интеграции
4. **Качество кода** - документирование и тесты

Проект хорошо структурирован и готов к масштабированию! 🚀

