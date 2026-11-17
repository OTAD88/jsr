package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.LocalJesrPalette
import com.example.ui.theme.LocalJesrStrings
import com.example.ui.theme.PureWhitePill
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

/**
 * Interactive profile photo cropper launched right after Photo Picker selection.
 * Supports pinch-to-zoom, pan drag, 90-degree rotation, zoom slider, circular avatar mask,
 * and exports a crisp 512x512 square Bitmap without requiring any storage or camera permissions.
 */
@Composable
fun ImageCropperDialog(
    imageUri: Uri,
    onConfirmCrop: (Bitmap) -> Unit,
    onDismiss: () -> Unit,
    aspectRatio: Float = 1f,
    isCircularMask: Boolean = true,
    titleOverride: String? = null,
    subtitleOverride: String? = null
) {
    val context = LocalContext.current
    val palette = LocalJesrPalette.current
    val strings = LocalJesrStrings.current
    val isReducedMotion = rememberIsReducedMotionEnabled()

    var sourceBitmap by remember(imageUri) { mutableStateOf<Bitmap?>(null) }
    var loadFailed by remember(imageUri) { mutableStateOf(false) }

    var zoom by remember(imageUri) { mutableFloatStateOf(1f) }
    var panOffset by remember(imageUri) { mutableStateOf(Offset.Zero) }
    var rotationDegrees by remember(imageUri) { mutableIntStateOf(0) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(imageUri) {
        loadFailed = false
        sourceBitmap = withContext(Dispatchers.IO) {
            decodeSampledBitmapFromUri(context, imageUri, maxDimension = 1080)
        }
        if (sourceBitmap == null) {
            loadFailed = true
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .staggeredIconEntrance(
                    index = 0,
                    stepDelayMs = 40,
                    startOffsetY = 24.dp,
                    startScale = 0.92f,
                    isReducedMotion = isReducedMotion
                )
        ) {
            EditorialFloatingCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("image_cropper_dialog"),
                backgroundColor = palette.cardSurface,
                contentPadding = 18.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = titleOverride ?: strings.cropTitle,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = palette.inkPrimary
                            )
                            Text(
                                text = subtitleOverride ?: strings.cropSubtitle,
                                fontSize = 11.5.sp,
                                color = palette.mutedText
                            )
                        }
                        EditorialInteractiveIconButton(
                            icon = Icons.Default.Close,
                            contentDescription = strings.close,
                            containerColor = palette.pillSurface,
                            contentColor = palette.iconColor,
                            buttonSize = 42.dp,
                            iconSize = 18.dp,
                            onClick = onDismiss,
                            testTag = "crop_close_btn"
                        )
                    }

                    // Cropper Canvas (1:1 for avatar or 16:9 for profile cover banner)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(aspectRatio)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF141414))
                            .border(1.dp, palette.border, RoundedCornerShape(20.dp))
                            .onSizeChanged { viewportSize = it }
                            .pointerInput(sourceBitmap, rotationDegrees) {
                                detectTransformGestures { _, pan, gestureZoom, _ ->
                                    val nextZoom = (zoom * gestureZoom).coerceIn(1f, 4.5f)
                                    zoom = nextZoom
                                    val maxShiftX = (viewportSize.width * nextZoom) * 0.48f
                                    val maxShiftY = (viewportSize.height * nextZoom) * 0.48f
                                    panOffset = Offset(
                                        x = (panOffset.x + pan.x).coerceIn(-maxShiftX, maxShiftX),
                                        y = (panOffset.y + pan.y).coerceIn(-maxShiftY, maxShiftY)
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val bmp = sourceBitmap
                        when {
                            loadFailed -> {
                                Text(
                                    text = strings.cropLoadError,
                                    color = PureWhitePill,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                            bmp == null -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = palette.accent)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = strings.cropLoading,
                                        color = PureWhitePill,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            else -> {
                                val imageBitmap = remember(bmp) { bmp.asImageBitmap() }
                                val isQuarterTurn = (rotationDegrees % 180) != 0
                                val rotatedW = if (isQuarterTurn) bmp.height.toFloat() else bmp.width.toFloat()
                                val rotatedH = if (isQuarterTurn) bmp.width.toFloat() else bmp.height.toFloat()

                                androidx.compose.foundation.Image(
                                    bitmap = imageBitmap,
                                    contentDescription = strings.cropTitle,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            val fitScale = if (size.width > 0f && size.height > 0f && rotatedW > 0f && rotatedH > 0f) {
                                                max(size.width / rotatedW, size.height / rotatedH)
                                            } else 1f
                                            val totalScale = fitScale * zoom
                                            val naturalToViewW = if (bmp.width > 0) size.width / bmp.width else 1f
                                            val naturalToViewH = if (bmp.height > 0) size.height / bmp.height else 1f
                                            val baseScaleX = (bmp.width * totalScale) / size.width.coerceAtLeast(1f)
                                            val baseScaleY = (bmp.height * totalScale) / size.height.coerceAtLeast(1f)
                                            // Match Android Canvas centered-crop transform accurately
                                            val uniformScale = max(
                                                (size.width / rotatedW.coerceAtLeast(1f)),
                                                (size.height / rotatedH.coerceAtLeast(1f))
                                            ) * zoom
                                            scaleX = if (bmp.width > 0 && size.width > 0f) {
                                                (bmp.width * uniformScale) / size.width
                                            } else zoom
                                            scaleY = if (bmp.height > 0 && size.height > 0f) {
                                                (bmp.height * uniformScale) / size.height
                                            } else zoom
                                            rotationZ = rotationDegrees.toFloat()
                                            translationX = panOffset.x
                                            translationY = panOffset.y
                                        }
                                )

                                // Circular avatar spotlight or rectangular cover guide
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    if (isCircularMask) {
                                        val diameter = size.minDimension * 0.88f
                                        val radius = diameter / 2f
                                        val cx = size.width / 2f
                                        val cy = size.height / 2f

                                        val maskPath = Path().apply {
                                            fillType = PathFillType.EvenOdd
                                            addRect(Rect(0f, 0f, size.width, size.height))
                                            addOval(
                                                Rect(
                                                    left = cx - radius,
                                                    top = cy - radius,
                                                    right = cx + radius,
                                                    bottom = cy + radius
                                                )
                                            )
                                        }
                                        drawPath(
                                            path = maskPath,
                                            color = Color.Black.copy(alpha = 0.56f)
                                        )
                                        drawCircle(
                                            color = Color.White.copy(alpha = 0.85f),
                                            radius = radius,
                                            center = Offset(cx, cy),
                                            style = Stroke(width = 2.dp.toPx())
                                        )
                                    } else {
                                        val padX = size.width * 0.04f
                                        val padY = size.height * 0.06f
                                        val rect = Rect(padX, padY, size.width - padX, size.height - padY)
                                        val maskPath = Path().apply {
                                            fillType = PathFillType.EvenOdd
                                            addRect(Rect(0f, 0f, size.width, size.height))
                                            addRect(rect)
                                        }
                                        drawPath(maskPath, color = Color.Black.copy(alpha = 0.45f))
                                        drawRect(
                                            color = Color.White.copy(alpha = 0.88f),
                                            topLeft = Offset(rect.left, rect.top),
                                            size = androidx.compose.ui.geometry.Size(rect.width, rect.height),
                                            style = Stroke(width = 2.dp.toPx())
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Zoom slider + 90-degree rotate button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = strings.cropZoom,
                            tint = palette.iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Slider(
                            value = zoom,
                            onValueChange = { zoom = it },
                            valueRange = 1f..4.5f,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("crop_zoom_slider"),
                            colors = SliderDefaults.colors(
                                thumbColor = palette.accent,
                                activeTrackColor = palette.accent
                            )
                        )
                        OutlinedButton(
                            onClick = {
                                rotationDegrees = (rotationDegrees + 90) % 360
                                panOffset = Offset.Zero
                            },
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(40.dp)
                                .testTag("crop_rotate_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.RotateRight,
                                contentDescription = strings.cropRotate,
                                tint = palette.iconColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = strings.cropRotate,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.inkPrimary
                            )
                        }
                    }

                    // Confirm Crop / Cancel Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val bmp = sourceBitmap
                                if (bmp != null) {
                                    val vw = viewportSize.width.coerceAtLeast(1).toFloat()
                                    val vh = viewportSize.height.coerceAtLeast(1).toFloat()
                                    val cropped = if (isCircularMask) {
                                        cropBitmapSquareRegion(
                                            source = bmp,
                                            zoom = zoom,
                                            panOffset = panOffset,
                                            rotationDegrees = rotationDegrees,
                                            viewportSidePx = vw,
                                            outputSizePx = 512
                                        )
                                    } else {
                                        cropBitmapRectRegion(
                                            source = bmp,
                                            zoom = zoom,
                                            panOffset = panOffset,
                                            rotationDegrees = rotationDegrees,
                                            viewportWidthPx = vw,
                                            viewportHeightPx = vh,
                                            outputWidthPx = 960,
                                            outputHeightPx = 540
                                        )
                                    }
                                    onConfirmCrop(cropped)
                                }
                            },
                            enabled = sourceBitmap != null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.inkPrimary,
                                contentColor = palette.elevatedSurface
                            ),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("crop_confirm_btn")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.cropConfirm, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.pillSurface,
                                contentColor = palette.inkPrimary
                            ),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("crop_cancel_btn")
                        ) {
                            Text(strings.cancel, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

internal fun cropBitmapSquareRegion(
    source: Bitmap,
    zoom: Float,
    panOffset: Offset,
    rotationDegrees: Int,
    viewportSidePx: Float,
    outputSizePx: Int = 512
): Bitmap {
    val safeOutput = outputSizePx.coerceIn(128, 1024)
    val result = Bitmap.createBitmap(safeOutput, safeOutput, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(result)
    canvas.drawColor(android.graphics.Color.BLACK)

    val safeViewport = viewportSidePx.coerceAtLeast(1f)
    val outToViewRatio = safeOutput / safeViewport

    val isQuarterTurn = (rotationDegrees % 180) != 0
    val rotatedW = if (isQuarterTurn) source.height.toFloat() else source.width.toFloat()
    val rotatedH = if (isQuarterTurn) source.width.toFloat() else source.height.toFloat()

    // Circle diameter occupies 88% of viewport side; crop snugly to that circular bounding box
    val circleViewportRatio = 0.88f
    val fitScaleInView = max(safeViewport / rotatedW.coerceAtLeast(1f), safeViewport / rotatedH.coerceAtLeast(1f))
    val totalScaleOut = (fitScaleInView * zoom * outToViewRatio) / circleViewportRatio

    val matrix = Matrix().apply {
        postTranslate(-source.width / 2f, -source.height / 2f)
        postRotate(rotationDegrees.toFloat())
        postScale(totalScaleOut, totalScaleOut)
        postTranslate(
            safeOutput / 2f + (panOffset.x * outToViewRatio / circleViewportRatio),
            safeOutput / 2f + (panOffset.y * outToViewRatio / circleViewportRatio)
        )
    }

    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    canvas.drawBitmap(source, matrix, paint)
    return result
}

internal fun cropBitmapRectRegion(
    source: Bitmap,
    zoom: Float,
    panOffset: Offset,
    rotationDegrees: Int,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
    outputWidthPx: Int = 960,
    outputHeightPx: Int = 540
): Bitmap {
    val result = Bitmap.createBitmap(outputWidthPx, outputHeightPx, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(result)
    canvas.drawColor(android.graphics.Color.BLACK)

    val safeVw = viewportWidthPx.coerceAtLeast(1f)
    val safeVh = viewportHeightPx.coerceAtLeast(1f)
    val outToViewX = outputWidthPx / safeVw
    val outToViewY = outputHeightPx / safeVh
    val outScale = max(outToViewX, outToViewY)

    val isQuarterTurn = (rotationDegrees % 180) != 0
    val rotatedW = if (isQuarterTurn) source.height.toFloat() else source.width.toFloat()
    val rotatedH = if (isQuarterTurn) source.width.toFloat() else source.height.toFloat()

    val fitScaleInView = max(safeVw / rotatedW.coerceAtLeast(1f), safeVh / rotatedH.coerceAtLeast(1f))
    val totalScaleOut = fitScaleInView * zoom * outScale

    val matrix = Matrix().apply {
        postTranslate(-source.width / 2f, -source.height / 2f)
        postRotate(rotationDegrees.toFloat())
        postScale(totalScaleOut, totalScaleOut)
        postTranslate(
            outputWidthPx / 2f + panOffset.x * outToViewX,
            outputHeightPx / 2f + panOffset.y * outToViewY
        )
    }

    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    canvas.drawBitmap(source, matrix, paint)
    return result
}

private fun decodeSampledBitmapFromUri(
    context: Context,
    uri: Uri,
    maxDimension: Int
): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val w = info.size.width
                val h = info.size.height
                val largest = max(w, h)
                if (largest > maxDimension) {
                    val ratio = maxDimension.toFloat() / largest.toFloat()
                    decoder.setTargetSize(
                        (w * ratio).toInt().coerceAtLeast(1),
                        (h * ratio).toInt().coerceAtLeast(1)
                    )
                }
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            var sampleSize = 1
            val largest = max(bounds.outWidth, bounds.outHeight)
            while (largest / sampleSize > maxDimension * 2) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            }
        }
    } catch (_: Exception) {
        null
    }
}
