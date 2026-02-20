# Анализ проекта MyGalleryApp (Nekolery)

## Описание проекта
**Николери** (Nekolery) - полнофункциональное приложение галереи для Android, созданное на Kotlin с использованием Jetpack Compose и Material 3.

**Особенность**: Проект был создан с помощью ИИ (Google Gemini, OpenAI ChatGPT) как эксперимент.

**Версия**: 2.0.21  
**Минимальный SDK**: 30  
**Целевой SDK**: 36 (Android 16)  
**Язык**: Kotlin 2.0.21

---

## 🏗️ Архитектура проекта

### Многоуровневая структура

```
mygalleryapp/
├── app/                          # Основное приложение
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/nkdsify/
│   │   │   │   ├── MainActivity.kt              # Точка входа
│   │   │   │   ├── MyApp.kt                     # Главная Composable
│   │   │   │   ├── MyAppState.kt                # Глобальное состояние
│   │   │   │   ├── ExternalMediaActivity.kt     # Просмотр внешних файлов
│   │   │   │   ├── ContextUtils.kt              # Утилиты контекста
│   │   │   │   ├── NotificationUtils.kt         # Уведомления
│   │   │   │   ├── data/
│   │   │   │   │   ├── DataModels.kt            # Модели данных
│   │   │   │   │   └── MediaLoader.kt           # Загрузка медиа
│   │   │   │   └── ui/
│   │   │   │       ├── MyAppNavigation.kt       # Навигация между экранами
│   │   │   │       ├── MyAppTopBar.kt           # Верхняя панель
│   │   │   │       ├── MyAppBottomBar.kt        # Нижняя панель
│   │   │   │       ├── MyAppFAB.kt              # Плавающая кнопка
│   │   │   │       ├── AppBars.kt               # Конфигурация панелей
│   │   │   │       ├── components/              # Переиспользуемые компоненты
│   │   │   │       ├── screens/                 # UI экранов
│   │   │   │       ├── dialogs/                 # Диалоги
│   │   │   │       ├── impl/                    # Реализация экранов
│   │   │   │       ├── editor/                  # Редактор фото/видео
│   │   │   │       ├── theme/                   # Тема оформления
│   │   │   │       └── utils/                   # Утилиты UI
│   │   │   └── res/                             # Ресурсы
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── baselineprofile/              # Baseline Profile для оптимизации
├── core-common/                  # Общая библиотека
└── gradle/libs.versions.toml     # Версии зависимостей
```

---

## 📊 Модель данных

### Основные классы

#### **Sealed Classes (Навигация)**
```kotlin
sealed class Screen {
    object Folders
    data class FolderContent(val folder: MediaFolder, val scrollToItemUri: Uri?)
    data class Favorites(val openAlbumName: String?)
    object Settings
    object TagManagement
    object Trash
    object AllMedia
    data class MediaByTag(val tag: String)
    object SecretStorage
    object ViewHistory
    object About
    object Help
    object HiddenFolders
}
```

#### **Data Classes**

1. **MediaItem** - Элемент медиа (файл)
```kotlin
data class MediaItem(
    val uri: Uri,
    val name: String,
    val absolutePath: String,
    val isVideo: Boolean = false,
    val size: Long,
    val dateAdded: Long,
    val dateModified: Long
)
```

2. **MediaFolder** - Папка с медиа
```kotlin
data class MediaFolder(
    val id: Long,
    val name: String,
    val items: ImmutableList<MediaItem>,
    val coverUri: Uri? = null,
    val totalSize: Long,
    val dateRange: Pair<Long, Long>,
    val itemCount: Int
)
```

3. **MediaViewerState** - Состояние просмотра медиа
```kotlin
data class MediaViewerState(
    val items: ImmutableList<MediaItem>,
    val startIndex: Int,
    val isExternal: Boolean = false
)
```

4. **MediaDetails** - Детали медиа файла
```kotlin
data class MediaDetails(
    val name: String,
    val size: Long,
    val dateAdded: Long,
    val dateModified: Long,
    val path: String,
    val resolution: String,
    val isVideo: Boolean,
    val exif: ExifInterface? = null,
    val duration: Long = 0L
)
```

#### **Enums**

```kotlin
enum class SortType {
    ALPHABET, DATE_MODIFIED, DATE_ADDED, SIZE
}

enum class Theme {
    SYSTEM, LIGHT, DARK, AMOLED
}

enum class ZoomType {
    DOUBLE_TAP, PINCH
}

enum class Language(val code: String) {
    SYSTEM("system"), ENGLISH("en"), RUSSIAN("ru"), SPECIAL("xx")
}

enum class BlurType {
    BLUR, PLACEHOLDER
}

enum class ViewerControlsPosition {
    TOP, BOTTOM
}

enum class AppFontFamily {
    SYSTEM, JETBRAINS_MONO, GOOGLE_SANS
}

enum class MediaTypeFilter {
    ALL, PHOTOS, VIDEOS
}
```

---

## 🌍 Глобальное состояние приложения (MyAppState)

Синглтон-подобный класс, управляющий всеми состояниями приложения:

### Состояния для настроек
- `isBlurEnabled` - размытие включено
- `selectedTheme` - выбранная тема
- `selectedLanguage` - язык
- `isVibrationEnabled` - вибрация
- `isMuteVideoByDefault` - звук видео по умолчанию
- `autoDeleteTrashEnabled` / `autoDeleteTrashDays` - автоудаление из корзины
- `selectedFontFamily` - шрифт приложения

### Состояния для UI
- `currentScreen` - текущий экран
- `isSearchActive` / `searchQuery` - поиск
- `selectedItems` - выбранные элементы
- `showTagDialog`, `showDetailsDialog` и т.д. - видимость диалогов

### Состояния для данных
- `allFolders` - все папки
- `allMedia` - все медиа на устройстве
- `favoriteItems` - избранные
- `trashedItems` - в корзине
- `secretItems` - в секретном хранилище
- `viewHistory` - история просмотра

### Grid States (для прокрутки)
- `foldersGridState`
- `folderContentGridState`
- `favoritesGridState`
- `trashGridState`
- `allMediaGridState`
- `secretGridState`
- и т.д.

---

## 🔌 Основные компоненты

### MainActivity
- Установка splash screen
- Инициализация sensor manager для shake detector
- Обработка разрешений
- Интеграция с внешними медиа

### MyApp (Главная Composable)
- Инициализация grid states
- Управление корутинами
- Загрузка избранных и тегов
- Pull-to-refresh
- Кэширование данных

### MyAppNavigation
- Switch между экранами с анимацией
- Фильтрация элементов
- Сортировка
- Поиск с поддержкой тегов

---

## 💾 Repositories (Управление данными)

### 1. **SettingsRepository** 
Хранит все настройки в SharedPreferences
```
Ключи: blur_enabled, theme, language, font_family, zoom_type и т.д.
```

### 2. **FavoritesRepository**
Управляет избранными папками
```kotlin
fun saveFavorites(context: Context, favorites: Set<String>)
fun getFavorites(context: Context): Set<String>
```

### 3. **TagsRepository**
Система тегирования медиа
```kotlin
fun getTags(context: Context): Map<String, Set<String>>
fun saveTags(context: Context, tags: Map<String, Set<String>>)
fun getTagsForItem(context: Context, path: String): Set<String>
fun setTagsForItem(context: Context, path: String, tags: Set<String>)
```

### 4. **SecretRepository**
Зашифрованное хранилище медиа
- Использует AES шифрование (CBC mode, PKCS7)
- Хранит файлы в `.secret` папке
- Создает зашифрованные миниатюры

### 5. **TrashRepository**
Управление корзиной с отложенным удалением

### 6. **ViewHistoryRepository**
История просмотра медиа

---

## 🔐 Безопасность и Криптография

### CryptoUtils
- **Алгоритм**: AES-256 в режиме CBC с PKCS7 паддингом
- **Управление ключами**: Android KeyStore
- **IV**: Генерируется автоматически, записывается в начало потока
- **Использование**: Шифрование секретного хранилища

```kotlin
fun encrypt(inputStream: InputStream, outputStream: OutputStream)
fun decrypt(inputStream: InputStream, outputStream: OutputStream)
```

### BiometricUtils
- Поддержка биометрической аутентификации (отпечатки, Face ID)

---

## 📱 Экраны (Screens)

1. **FoldersScreen** - Сетка папок с медиа
2. **FolderContentScreen** - Содержимое папки
3. **FavoritesScreen** - Избранные папки
4. **AllMediaScreen** - Все медиа на устройстве
5. **TrashScreen** - Корзина
6. **SecretStorageScreen** - Секретное хранилище (зашифрованное)
7. **TagManagementScreen** - Управление тегами
8. **ViewHistoryScreen** - История просмотра
9. **SettingsScreen** - Настройки приложения
10. **HiddenFoldersScreen** - Скрытые папки
11. **AboutScreen** - О приложении
12. **HelpScreen** - Помощь

---

## 🎨 UI Компоненты

### MediaViewer
Основной компонент для просмотра медиа
- Поддержка свайпа между фото
- Сжатие по пальцам (Pinch to Zoom)
- Двойной тап для зума
- Поддержка видео с ExoPlayer
- Контролы в верхней/нижней части
- Размытие (Blur/Placeholder)
- История просмотра

### MediaGrid
Сетка для отображения медиа
- Lazy grid для оптимизации
- Селектор элементов
- Поддержка blur типов

### VideoPlayerPage
Плеер видео на базе ExoPlayer
- Media3 экосистема
- Цикличное воспроизведение
- Приглушение звука

### ZoomableImage
Компонент для увеличиваемых изображений

---

## 🔄 Загрузка медиа (MediaLoader)

### loadAllMedia()
```kotlin
fun loadAllMedia(
    context: Context,
    sortType: SortType,
    sortAscending: Boolean,
    hiddenFolderIds: Set<String>,
    selectedDate: Long? = null
): ImmutableList<MediaItem>
```
- Запрос к MediaStore
- Фильтрация по дате
- Сортировка (по дате, имени, размеру)
- Исключение скрытых папок

### loadMediaFolders()
Загрузка всех папок с их содержимым

### loadFavoriteMediaItems()
Загрузка элементов из избранных папок

### loadTrashedMediaItems()
Загрузка удаленных элементов

---

## 🎯 Особенности приложения

### 1. **Система тегирования**
- Создание произвольных тегов
- Присвоение тегов элементам и папкам
- Фильтрация по тегам в поиске
- Резервное копирование/восстановление тегов

### 2. **Размытие (Blur)**
- **Глобальное размытие** - все папки/медиа
- **Размытие в папках** - папка целиком размыта
- **Размытие в корзине** - удаленные файлы размыты
- **Размытие конкретных элементов** - отдельные файлы
- **Два типа**: BLUR (фон) или PLACEHOLDER

### 3. **Секретное хранилище**
- Зашифрованное AES хранилище
- Скрытая папка `.secret`
- Криптографически защищенные миниатюры
- Требует биометрической аутентификации

### 4. **Shake to Blur**
- Акселерометр для обнаружения встряхивания
- Быстрое включение/отключение размытия

### 5. **FAB (Floating Action Button)**
- Перемешивание медиа
- Запуск камеры
- Пользовательские действия

### 6. **История просмотра**
- Отслеживание просмотренных файлов
- Фильтрация по дате
- Полная очистка

### 7. **Резервное копирование**
- Экспорт тегов и избранных
- Импорт из JSON
- GSON для сериализации

### 8. **Проверка обновлений**
- GitHub API для получения последнего релиза
- Загрузка APK
- Уведомления обновлений

### 9. **Поддержка язык**
- Русский (ru)
- Английский (en)
- Системный (system)
- "Специальный" язык (easter egg)

### 10. **Редактирование фото**
- Встроенный фото-редактор
- Рисование на фото
- Обрезка изображений (CropImageActivity)
- Экспорт отредактированных файлов

### 11. **Автоудаление корзины**
- Параметр дней до автоудаления
- Фоновая очистка

---

## 🔗 Зависимости и библиотеки

### Compose & Material
- Jetpack Compose BOM 2024.06.00
- Material 3
- Compose Animation
- Reorderable lists

### Media & Image
- Coil 2.7.0 (загрузка и кэширование изображений)
- Media3 1.3.1 (видеоплеер ExoPlayer)
- ExifInterface 1.3.7 (EXIF данные)
- android-image-cropper 4.5.0 (обрезка фото)

### Networking & Data
- Retrofit 2.9.0
- GSON 2.10.1
- Paging 3.3.6

### Security
- Biometric 1.1.0
- Android KeyStore для криптографии

### Utilities
- Kotlin Collections Immutable
- Calvin Reorderable 2.4.1

---

## 📄 AndroidManifest.xml

### Требуемые разрешения
```xml
READ_MEDIA_IMAGES, READ_MEDIA_VIDEO
READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE
MANAGE_EXTERNAL_STORAGE
VIBRATE, INTERNET
SET_WALLPAPER, CAMERA
POST_NOTIFICATIONS
```

### Активности
1. **MainActivity** - Главная активность
2. **ExternalMediaActivity** - Просмотр внешних файлов (intent filter для VIEW/EDIT)
3. **EditActivity** - Редактор фото
4. **VideoEditActivity** - Редактор видео
5. **CropImageActivity** - Обрезка изображений

---

## 🚀 Процесс запуска

1. **Splash Screen** установлен в `onCreate`
2. **ShakeDetector** инициализируется
3. **Permissions** запрашиваются у пользователя
4. **MigrationUtils** выполняет миграции данных
5. **Settings** загружаются из SharedPreferences
6. **Медиа** загружается в фоновом потоке (корутины)
7. **MyApp** Composable рендерится

---

## 📊 Стек технологий

| Слой | Технология |
|------|-----------|
| **UI Framework** | Jetpack Compose + Material 3 |
| **Язык** | Kotlin 2.0.21 |
| **Состояние** | Compose State (mutableStateOf) |
| **Навигация** | Custom Screen sealed class |
| **Параллелизм** | Kotlin Coroutines |
| **Изображения** | Coil ImageLoader |
| **Видео** | Media3 (ExoPlayer) |
| **Хранилище** | SharedPreferences + FileSystem + Android KeyStore |
| **Сеть** | Retrofit + GSON |
| **Безопасность** | AES Encryption (Android KeyStore) |

---

## 🎓 Ключевые паттерны и практики

1. **State Hoisting** - Глобальное состояние в MyAppState
2. **Immutable Collections** - Использование ImmutableList
3. **Sealed Classes** - Типобезопасная навигация
4. **Repository Pattern** - Отделение источников данных
5. **Lazy Loading** - LazyGrid/LazyList для оптимизации
6. **Coroutine Scopes** - Управление асинхронными операциями
7. **Composition Locals** - LocalContext, LocalImageLoader и т.д.
8. **Defensive Copying** - Использование копирования состояния

---

## 🔍 Потоки данных (Data Flow)

```
MediaStore 
    ↓
MediaLoader.loadAllMedia() / loadMediaFolders() ...
    ↓
MyAppState (allFolders, allMedia, etc.)
    ↓
MyAppNavigation (фильтрация, сортировка, поиск)
    ↓
UI Screens (FoldersScreen, FolderContentScreen и т.д.)
    ↓
MediaViewer (просмотр с контролами)
```

---

## 📝 Замечания по коду

1. **GEMINI комментарии** - В коде есть комментарии типа "GEMINI НЕ ТРОГАЙ ЭТУ АНОТАЦИЮ" (следы работы с ИИ)
2. **TODO** - Есть места, требующие завершения/оптимизации
3. **Многоязычность** - Весь текст в `strings.xml`
4. **Dark Mode** - Полная поддержка AMOLED темы
5. **Accessibility** - Используются правильные паттерны Compose

---

## ✨ Возможные улучшения

1. Добавить database (Room) для локального кэширования
2. Оптимизировать загрузку больших количеств файлов
3. Добавить поддержку RAW и других форматов
4. Реализовать batch operations для лучшей производительности
5. Добавить unit тесты
6. Улучшить обработку ошибок
7. Добавить analytics

---

## 🎯 Заключение

Это полнофункциональное, хорошо структурированное приложение галереи, демонстрирующее:
- Продвинутый уровень работы с Jetpack Compose
- Правильную архитектуру многоэкранного приложения
- Сложную работу с медиа, включая видео и криптографию
- Полный цикл функционала (от загрузки до обработки и сохранения)

Проект готов к дальнейшей разработке и может служить отличной базой для добавления новых функций.

