# Видео Слайд-шоу Превью - Реализация

## 🎬 Обзор функции

Добавлена полнофункциональная система **видео превью слайд-шоу**, которая показывает анимированный цикл из трех кадров видеофайла:
- **START** (1% от начала) - первый важный кадр
- **MIDDLE** (50% видео) - средняя часть  
- **END** (99% от конца) - последний кадр

Это позволяет пользователю быстро понять содержимое видеофайла без необходимости открывать его.

---

## 📁 Созданные файлы

### 1. **VideoFrameExtractor.kt** (`ui/utils/`)
Утилита для извлечения и кэширования видео кадров.

**Основные методы:**
```kotlin
// Извлечение трех кадров из видео
suspend fun extractFrames(context: Context, videoUri: Uri): Map<FrameType, Bitmap?>?

// Извлечение с кэшированием
suspend fun getFramesWithCache(context: Context, videoUri: Uri): Map<FrameType, Bitmap?>?

// Очистка старого кэша (старше 7 дней)
suspend fun cleanOldCache(context: Context, maxAgeMs: Long = 7 * 24 * 60 * 60 * 1000)

// Полная очистка кэша
suspend fun clearCache(context: Context)
```

**Особенности:**
- ✅ Использует `MediaMetadataRetriever` для безопасного извлечения кадров
- ✅ Кэширует кадры в `context.cacheDir` для второстепенной загрузки
- ✅ Автоматическая очистка старых кадров (7+ дней)
- ✅ Обработка ошибок и исключений

### 2. **VideoPreviewSlideshow.kt** (`ui/components/`)
Compose компонент для отображения видео превью с анимацией.

**Компонент:**
```kotlin
@Composable
fun VideoPreviewWithSlideshow(
    modifier: Modifier = Modifier,
    item: MediaItem,
    imageLoader: ImageLoader,
    intervalMs: Long = 800L,
    contentScale: ContentScale = ContentScale.Crop
)
```

**Особенности:**
- ✅ LaunchedEffect автоматически загружает кадры при изменении URI
- ✅ Бесконечная анимация циклического воспроизведения
- ✅ Поддержка настраиваемого интервала между кадрами
- ✅ Fallback на статичное превью если кадры не загружены
- ✅ Play icon поверх видео для идентификации

---

## 🔧 Модифицированные файлы

### 1. **SettingsRepository.kt**
Добавлены методы для сохранения/загрузки настроек:

```kotlin
// Включение/отключение слайд-шоу
fun setVideoPreviewSlideshow(context: Context, enabled: Boolean)
fun isVideoPreviewSlideshowEnabled(context: Context): Boolean

// Интервал между кадрами (в миллисекундах)
fun setVideoSlideshowInterval(context: Context, intervalMs: Long)
fun getVideoSlideshowInterval(context: Context): Long  // По умолчанию 800ms
```

### 2. **MyAppState.kt**
Добавлены состояния:

```kotlin
var isVideoPreviewSlideshowEnabled by mutableStateOf(...)
var videoSlideshowIntervalMs by mutableStateOf(...)
```

### 3. **MediaGrid.kt**
Добавлена поддержка видео слайд-шоу:

```kotlin
@Composable
fun MediaGrid(
    // ... existing parameters ...
    isVideoPreviewSlideshowEnabled: Boolean = false,
    videoSlideshowIntervalMs: Long = 800L
)
```

Логика:
```kotlin
if (item.isVideo && isVideoPreviewSlideshowEnabled) {
    VideoPreviewWithSlideshow(...)
} else {
    AsyncImage(...)  // Обычное превью
}
```

### 4. **SettingsScreen.kt & SettingsScreenImpl.kt**
Добавлены UI элементы управления:

```kotlin
// Включение/отключение слайд-шоу
SettingsSwitch(
    title = stringResource(id = R.string.video_preview_slideshow_label),
    isChecked = state.isVideoPreviewSlideshowEnabled,
    onCheckedChange = actions.onVideoPreviewSlideshowChange
)

// Настройка интервала (видимо только когда включено)
AnimatedVisibility(visible = state.isVideoPreviewSlideshowEnabled) {
    SettingsRow(title = { Text(stringResource(id = R.string.video_slideshow_interval_label)) }) {
        OutlinedTextField(
            value = (state.videoSlideshowIntervalMs / 100).toString(),
            // Пользователь вводит в сотнях миллисекунд (800ms = 8)
        )
    }
}
```

### 5. **SettingsState.kt**
Добавлены поля в data class:

```kotlin
val isVideoPreviewSlideshowEnabled: Boolean
val videoSlideshowIntervalMs: Long
```

И callbacks:
```kotlin
val onVideoPreviewSlideshowChange: (Boolean) -> Unit
val onVideoSlideshowIntervalChange: (Long) -> Unit
```

### 6. **Все вызовы MediaGrid**
Обновлены все экраны чтобы передавать новые параметры:
- `AllMediaScreenImpl.kt`
- `FolderContentScreen.kt`
- `MediaByTagScreenImpl.kt`
- `FavoritesScreen.kt` / `FavoritesScreenImpl.kt`
- `TrashScreen.kt`
- `SecretStorageScreenImpl.kt`
- `ViewHistoryScreen.kt`

### 7. **Строки ресурсов**
Добавлены в `strings.xml`:
```xml
<string name="video_preview_slideshow_label">Video Preview Slideshow</string>
<string name="video_slideshow_interval_label">Slideshow Speed</string>
```

И в `strings-ru.xml`:
```xml
<string name="video_preview_slideshow_label">Слайд-шоу видео превью</string>
<string name="video_slideshow_interval_label">Скорость показа</string>
```

---

## ⚙️ Оптимизация и производительность

### 1. **Кэширование кадров**
- Кадры сохраняются в `app/cache/video_frames_cache/`
- Формат: `<video_hash>_<frame_type>.bmp`
- Повторная загрузка одного видео - без переобработки
- Автоматическая очистка старых кадров (>7 дней)

### 2. **Асинхронная загрузка**
```kotlin
LaunchedEffect(item.uri) {
    scope.launch {  // Загружается в фоне, не блокирует UI
        val frames = VideoFrameExtractor.getFramesWithCache(context, item.uri)
    }
}
```

### 3. **Управление памятью**
- `MediaMetadataRetriever.release()` вызывается после использования
- Bitmaaps хранятся временно и переиспользуются в памяти
- Старый кэш автоматически удаляется

### 4. **Ленивое включение**
- Слайд-шоу активируется **только если включено в настройках**
- Для видео без слайд-шоу используется обычное `AsyncImage` превью
- При отключении опции - возврат к обычным превью без экстра нагрузки

---

## 🎯 Использование в приложении

### Для пользователя:
1. Откройте **Настройки** → **Медиа** 
2. Включите **"Video Preview Slideshow"**
3. Выберите скорость показа (800ms по умолчанию - хороший баланс)
4. Теперь видеофайлы в сетке показывают анимированное слайд-шоу из 3 кадров

### Для разработчика:
```kotlin
// Получить кадры для видео (если нужно)
val frames = VideoFrameExtractor.extractFrames(context, videoUri)

// Очистить кэш (например, в фоновом режиме)
VideoFrameExtractor.clearCache(context)

// Очистить только старый кэш
VideoFrameExtractor.cleanOldCache(context)
```

---

## 📊 Структура данных

### VideoFrameType
```kotlin
enum class FrameType {
    START,   // 1% от начала
    MIDDLE,  // 50% видео
    END      // 99% от конца
}
```

### Результат извлечения
```kotlin
Map<FrameType, Bitmap?> = {
    FrameType.START -> Bitmap(...),
    FrameType.MIDDLE -> Bitmap(...),
    FrameType.END -> Bitmap(...)
}
```

---

## 🔧 Параметры настройки

| Параметр | По умолчанию | Описание |
|----------|-------------|---------|
| `isVideoPreviewSlideshowEnabled` | `false` | Включить/отключить слайд-шоу |
| `videoSlideshowIntervalMs` | `800ms` | Интервал между кадрами в миллисекундах |

**Рекомендуемые значения интервала:**
- **600ms** - быстрое переключение (много информации)
- **800ms** - сбалансированное (по умолчанию)
- **1000ms** - медленное (для внимательного просмотра)
- **1200ms** - очень медленное (для деликатных видео)

---

## 🐛 Известные особенности

1. **Первая загрузка видео** - может задержаться на 0.5-1 сек пока извлекаются кадры
2. **Мобильные видео** - некоторые форматы могут не поддерживаться `MediaMetadataRetriever`
3. **Очень длинные видео** - кэш может занять место если много видео

---

## 📈 Предложения для будущих улучшений

1. **Добавить плавный переход** между кадрами (crossfade анимация)
2. **Пользовательские точки кадров** - позволить пользователю выбирать какие кадры показывать
3. **Динамическое качество** - регулировать размер кадров для сохранения памяти
4. **Статистика кэша** - показывать размер кэша в настройках с кнопкой очистки
5. **Настройка кадров** - 3 кадра vs 5 кадров vs другие варианты
6. **Умный выбор кадров** - автоматически выбирать "интересные" кадры на основе анализа содержимого

---

## ✅ Чек-лист реализации

- ✅ VideoFrameExtractor утилита создана
- ✅ VideoPreviewSlideshow компонент создан
- ✅ SettingsRepository методы добавлены
- ✅ MyAppState состояние добавлено
- ✅ MediaGrid обновлен для поддержки слайд-шоу
- ✅ UI в SettingsScreen добавлен
- ✅ SettingsState и Actions обновлены
- ✅ Все экраны обновлены
- ✅ Строки ресурсов добавлены (EN + RU)
- ✅ Кэширование реализовано
- ✅ Код очищен от warnings
- ✅ Обработка ошибок реализована

---

## 🚀 Готово!

Функция полностью интегрирована в приложение и готова к использованию. Видеофайлы в галерее теперь будут показывать анимированное слайд-шоу из трех ключевых кадров, если пользователь включит эту опцию в настройках.

