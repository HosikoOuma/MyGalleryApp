package com.example.nkdsify.ui.editor

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nkdsify.R
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditorScreen(
    viewModel: PhotoEditorViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val bitmap by viewModel.currentBitmap.collectAsState()

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                title = { 
                    Text(
                        stringResource(R.string.editor_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = stringResource(R.string.back_content_description),
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (viewModel.activeTab == EditorTab.DRAW && !viewModel.isCropMode) {
                        IconButton(onClick = { viewModel.undo() }) {
                            Icon(Icons.AutoMirrored.Filled.Undo, null, tint = Color.White)
                        }
                        IconButton(onClick = { viewModel.redo() }) {
                            Icon(Icons.AutoMirrored.Filled.Redo, null, tint = Color.White)
                        }
                    }
                    FilledTonalButton(
                        onClick = {
                            viewModel.saveResult(context) { if (it) onBack() }
                        },
                        modifier = Modifier.padding(end = 8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White
                        )
                    ) {
                        Text(stringResource(R.string.save_action), style = MaterialTheme.typography.labelLarge, color = Color.White)
                    }
                }
            )
        },
        bottomBar = {
            EditorBottomDesign(viewModel)
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            bitmap?.let { btm ->
                PhotoCanvas(btm, viewModel)
            } ?: CircularProgressIndicator(color = Color.White)

            if (viewModel.isSaving) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(0.6f)), contentAlignment = Alignment.Center) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
                    ) {
                        Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(Modifier.height(16.dp))
                            Text(stringResource(R.string.editor_processing), style = MaterialTheme.typography.labelLarge, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditorBottomDesign(viewModel: PhotoEditorViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A1A).copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                AnimatedContent(
                    targetState = viewModel.activeTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "TabContent"
                ) { tab ->
                    when (tab) {
                        EditorTab.DRAW -> DrawPanel(viewModel)
                        EditorTab.TRANSFORM -> TransformPanel(viewModel)
                        EditorTab.FILTERS -> FiltersPanel(viewModel)
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                    color = Color.White.copy(alpha = 0.1f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    EditorTabItem(
                        icon = Icons.Default.Brush,
                        label = stringResource(R.string.tab_draw),
                        isSelected = viewModel.activeTab == EditorTab.DRAW,
                        onClick = { viewModel.onTabChanged(EditorTab.DRAW) }
                    )
                    EditorTabItem(
                        icon = Icons.Default.Transform,
                        label = stringResource(R.string.tab_transform),
                        isSelected = viewModel.activeTab == EditorTab.TRANSFORM,
                        onClick = { viewModel.onTabChanged(EditorTab.TRANSFORM) }
                    )
                    EditorTabItem(
                        icon = Icons.Default.FilterBAndW,
                        label = stringResource(R.string.tab_filters),
                        isSelected = viewModel.activeTab == EditorTab.FILTERS,
                        onClick = { viewModel.onTabChanged(EditorTab.FILTERS) }
                    )
                }
            }
        }
    }
}

@Composable
fun EditorTabItem(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current
            ) { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) Color.White else Color.Gray
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 10.sp
            ),
            color = if (isSelected) Color.White else Color.Gray
        )
    }
}

@Composable
fun DrawPanel(viewModel: PhotoEditorViewModel) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { viewModel.isEraser = !viewModel.isEraser },
                modifier = Modifier
                    .background(
                        if (viewModel.isEraser) Color.White.copy(alpha = 0.2f) else Color.Transparent,
                        CircleShape
                    )
            ) {
                Icon(
                    if (viewModel.isEraser) Icons.Default.AutoFixHigh else Icons.Default.Brush,
                    contentDescription = null,
                    tint = Color.White
                )
            }
            
            Slider(
                value = viewModel.strokeWidth,
                onValueChange = { viewModel.strokeWidth = it },
                valueRange = 2f..80f,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.Gray
                )
            )
        }
        
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            val colors = listOf(Color.Red, Color(0xFFFF9800), Color.Yellow, Color.Green, Color(0xFF2196F3), Color.Blue, Color(0xFF9C27B0), Color.White, Color.Black)
            items(colors) { color ->
                val isSelected = viewModel.drawColor == color && !viewModel.isEraser
                val size by animateDpAsState(if (isSelected) 36.dp else 28.dp, label = "colorSize")
                val interactionSource = remember { MutableInteractionSource() }
                
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(size)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color.White else Color.Gray.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                            .clickable(
                                interactionSource = interactionSource,
                                indication = LocalIndication.current
                            ) {
                                viewModel.drawColor = color
                                viewModel.isEraser = false
                            }
                    )
                }
            }
        }
    }
}

@Composable
fun TransformPanel(viewModel: PhotoEditorViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        TransformActionBtn(Icons.Default.RotateLeft, stringResource(R.string.editor_rotate_left)) { viewModel.rotate(-90f) }
        TransformActionBtn(Icons.Default.RotateRight, stringResource(R.string.editor_rotate_right)) { viewModel.rotate(90f) }
        TransformActionBtn(Icons.Default.Flip, stringResource(R.string.editor_flip)) { viewModel.flipHorizontal() }
        TransformActionBtn(Icons.Default.Crop, stringResource(R.string.editor_crop)) { viewModel.toggleCropMode(true) }
    }
}

@Composable
fun TransformActionBtn(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.1f),
        modifier = Modifier.size(56.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, label, tint = Color.White)
        }
    }
}

@Composable
fun FiltersPanel(viewModel: PhotoEditorViewModel) {
    val filterNames = remember {
        mapOf(
            PhotoFilter.NONE to R.string.filter_none,
            PhotoFilter.B_AND_W to R.string.filter_bw,
            PhotoFilter.SEPIA to R.string.filter_sepia,
            PhotoFilter.INVERT to R.string.filter_invert,
            PhotoFilter.VINTAGE to R.string.filter_vintage,
            PhotoFilter.COOL to R.string.filter_cool,
            PhotoFilter.WARM to R.string.filter_warm
        )
    }

    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        items(PhotoFilter.entries) { filter ->
            FilterCard(
                name = stringResource(filterNames[filter] ?: R.string.filter_none),
                isSelected = viewModel.currentFilter == filter,
                onClick = { viewModel.setFilter(filter) }
            )
        }
    }
}

@Composable
fun FilterCard(name: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.1f),
        contentColor = if (isSelected) Color.Black else Color.White,
        modifier = Modifier.height(40.dp)
    ) {
        Box(Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            Text(name, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun PhotoCanvas(
    bitmap: Bitmap,
    viewModel: PhotoEditorViewModel
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var activeHandle by remember { mutableStateOf(CropHandle.NONE) }
    
    // Счетчик для принудительной перерисовки Canvas
    var drawTrigger by remember { mutableStateOf(0) }

    val imageBitmap = bitmap.asImageBitmap()

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { canvasSize = it.size }
            .pointerInput(viewModel.activeTab, viewModel.isCropMode) {
                if (viewModel.isCropMode) {
                    val scale = min(size.width / bitmap.width.toFloat(), size.height / bitmap.height.toFloat())
                    val drawWidth = bitmap.width.toFloat() * scale
                    val drawHeight = bitmap.height.toFloat() * scale
                    val offsetX = (size.width - drawWidth) / 2f
                    val offsetY = (size.height - drawHeight) / 2f

                    detectDragGestures(
                        onDragStart = { offset ->
                            val r = viewModel.cropRect
                            val rl = offsetX + r.left * drawWidth
                            val rt = offsetY + r.top * drawHeight
                            val rr = offsetX + r.right * drawWidth
                            val rb = offsetY + r.bottom * drawHeight
                            val hit = 40.dp.toPx()
                            fun ds(x1: Float, y1: Float, x2: Float, y2: Float) = (x1-x2)*(x1-x2) + (y1-y2)*(y1-y2)

                            activeHandle = when {
                                ds(offset.x, offset.y, rl, rt) < hit * hit -> CropHandle.TOP_LEFT
                                ds(offset.x, offset.y, rr, rt) < hit * hit -> CropHandle.TOP_RIGHT
                                ds(offset.x, offset.y, rl, rb) < hit * hit -> CropHandle.BOTTOM_LEFT
                                ds(offset.x, offset.y, rr, rb) < hit * hit -> CropHandle.BOTTOM_RIGHT
                                offset.x in rl..rr && offset.y in rt..rb -> CropHandle.CENTER
                                else -> CropHandle.NONE
                            }
                        },
                        onDrag = { change, drag ->
                            if (activeHandle == CropHandle.NONE) return@detectDragGestures
                            change.consume()
                            val dx = drag.x / drawWidth
                            val dy = drag.y / drawHeight
                            val r = viewModel.cropRect
                            
                            viewModel.cropRect = when(activeHandle) {
                                CropHandle.TOP_LEFT -> Rect((r.left + dx).coerceIn(0f, r.right-0.1f), (r.top+dy).coerceIn(0f, r.bottom-0.1f), r.right, r.bottom)
                                CropHandle.TOP_RIGHT -> Rect(r.left, (r.top+dy).coerceIn(0f, r.bottom-0.1f), (r.right+dx).coerceIn(r.left+0.1f, 1f), r.bottom)
                                CropHandle.BOTTOM_LEFT -> Rect((r.left+dx).coerceIn(0f, r.right-0.1f), r.top, r.right, (r.bottom+dy).coerceIn(r.top+0.1f, 1f))
                                CropHandle.BOTTOM_RIGHT -> Rect(r.left, r.top, (r.right+dx).coerceIn(r.left+0.1f, 1f), (r.bottom+dy).coerceIn(r.top+0.1f, 1f))
                                CropHandle.CENTER -> {
                                    val nl = (r.left + dx).coerceIn(0f, 1f - r.width)
                                    val nt = (r.top + dy).coerceIn(0f, 1f - r.height)
                                    Rect(nl, nt, nl + r.width, nt + r.height)
                                }
                                else -> r
                            }
                        },
                        onDragEnd = { activeHandle = CropHandle.NONE }
                    )
                } else if (viewModel.activeTab == EditorTab.DRAW) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val btmOffset = mapOffsetToBitmap(offset, canvasSize, bitmap.width, bitmap.height)
                            currentPath = Path().apply { moveTo(btmOffset.x, btmOffset.y) }
                        },
                        onDrag = { change, _ ->
                            val btmOffset = mapOffsetToBitmap(change.position, canvasSize, bitmap.width, bitmap.height)
                            currentPath?.lineTo(btmOffset.x, btmOffset.y)
                            drawTrigger++ // ФОРСИРУЕМ ПЕРЕРИСОВКУ
                        },
                        onDragEnd = {
                            currentPath?.let {
                                viewModel.addPath(DrawPath(it, viewModel.drawColor, viewModel.strokeWidth, viewModel.isEraser))
                            }
                            currentPath = null
                        }
                    )
                }
            }
    ) {
        // Доступ к drawTrigger заставляет Canvas перерисовываться при каждом движении
        val _trigger = drawTrigger 
        
        val scale = min(size.width / bitmap.width.toFloat(), size.height / bitmap.height.toFloat())
        val drawWidth = bitmap.width.toFloat() * scale
        val drawHeight = bitmap.height.toFloat() * scale
        val offsetX = (size.width - drawWidth) / 2f
        val offsetY = (size.height - drawHeight) / 2f

        drawImage(
            image = imageBitmap,
            dstOffset = IntOffset(offsetX.toInt(), offsetY.toInt()),
            dstSize = IntSize(drawWidth.toInt(), drawHeight.toInt())
        )

        if (viewModel.isCropMode) {
            val r = viewModel.cropRect
            val rl = offsetX + r.left * drawWidth
            val rt = offsetY + r.top * drawHeight
            val rr = offsetX + r.right * drawWidth
            val rb = offsetY + r.bottom * drawHeight
            
            drawRect(color = Color.Black.copy(0.7f), topLeft = Offset(offsetX, offsetY), size = Size(drawWidth, drawHeight))
            clipRect(rl, rt, rr, rb, ClipOp.Difference) {}
            drawRect(color = Color.White, topLeft = Offset(rl, rt), size = Size(rr-rl, rb-rt), style = Stroke(2.dp.toPx()))
        } else {
            clipRect(offsetX, offsetY, offsetX + drawWidth, offsetY + drawHeight) {
                // РИСУЕМ СОХРАНЕННЫЕ ПУТИ
                drawIntoCanvas { canvas ->
                    canvas.saveLayer(Rect(offsetX, offsetY, offsetX + drawWidth, offsetY + drawHeight), Paint())
                    viewModel.paths.forEach { drawPath ->
                        val androidPath = drawPath.path.asAndroidPath()
                        val matrix = android.graphics.Matrix()
                        matrix.postScale(scale, scale)
                        matrix.postTranslate(offsetX, offsetY)
                        val transformedPath = android.graphics.Path()
                        androidPath.transform(matrix, transformedPath)
                        drawPath(
                            path = transformedPath.asComposePath(),
                            color = if (drawPath.isEraser) Color.Transparent else drawPath.color,
                            style = Stroke(width = drawPath.strokeWidth * scale, cap = StrokeCap.Round, join = StrokeJoin.Round),
                            blendMode = if (drawPath.isEraser) BlendMode.Clear else DrawScope.DefaultBlendMode
                        )
                    }
                    canvas.restore()
                }
            }
            
            // РИСУЕМ ТЕКУЩИЙ ПУТЬ (МГНОВЕННО)
            currentPath?.let { path ->
                val androidPath = path.asAndroidPath()
                val matrix = android.graphics.Matrix()
                matrix.postScale(scale, scale)
                matrix.postTranslate(offsetX, offsetY)
                val transformedPath = android.graphics.Path()
                androidPath.transform(matrix, transformedPath)
                drawPath(
                    path = transformedPath.asComposePath(),
                    color = if (viewModel.isEraser) Color.Transparent else viewModel.drawColor,
                    style = Stroke(width = viewModel.strokeWidth * scale, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    blendMode = if (viewModel.isEraser) BlendMode.Clear else DrawScope.DefaultBlendMode
                )
            }
        }
    }
    
    if (viewModel.isCropMode) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.BottomCenter) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = { viewModel.isCropMode = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) { Text(stringResource(R.string.dialog_cancel), color = Color.White) }
                Button(
                    onClick = { viewModel.applyCrop() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) { Text(stringResource(R.string.dialog_ok), color = Color.Black) }
            }
        }
    }
}

private fun mapOffsetToBitmap(offset: Offset, canvasSize: IntSize, bitmapWidth: Int, bitmapHeight: Int): Offset {
    val scale = min(canvasSize.width.toFloat() / bitmapWidth.toFloat(), canvasSize.height.toFloat() / bitmapHeight.toFloat())
    val offsetX = (canvasSize.width.toFloat() - bitmapWidth.toFloat() * scale) / 2f
    val offsetY = (canvasSize.height.toFloat() - bitmapHeight.toFloat() * scale) / 2f
    return Offset((offset.x - offsetX) / scale, (offset.y - offsetY) / scale).let {
        Offset(it.x.coerceIn(0f, bitmapWidth.toFloat()), it.y.coerceIn(0f, bitmapHeight.toFloat()))
    }
}
