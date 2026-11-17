package com.example.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.composed
import kotlinx.coroutines.delay
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.FriendEntity
import com.example.ui.theme.LocalJesrPalette
import com.example.ui.theme.LocalJesrStrings
import com.example.ui.theme.PureWhitePill
import com.example.ui.theme.StoneBackdropDeep
import com.example.ui.theme.SwatchStoneDark
import com.example.ui.theme.SwatchStoneLight
import com.example.ui.theme.SwatchStoneMid
import com.example.ui.theme.TerracottaAccent
import com.example.ui.theme.TerracottaContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun JesrAvatar(
    displayName: String,
    avatarPath: String? = null,
    colorIndex: Int = 0,
    size: Dp = 46.dp,
    isOnline: Boolean? = null,
    modifier: Modifier = Modifier
) {
    val palette = LocalJesrPalette.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, avatarPath) {
        value = if (!avatarPath.isNullOrBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    BitmapFactory.decodeFile(avatarPath)?.asImageBitmap()
                } catch (_: Exception) {
                    null
                }
            }
        } else null
    }

    val bgTint = when {
        palette.isDark -> palette.pillSurface
        colorIndex % 2 == 0 -> SwatchStoneLight
        else -> SwatchStoneMid
    }

    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(bgTint)
                .border(1.dp, palette.border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!,
                    contentDescription = displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                val fallbackInitial = if (LocalJesrStrings.current.isEnglish) "J" else "ج"
                Text(
                    text = displayName.trim().take(1).uppercase().ifEmpty { fallbackInitial },
                    fontSize = (size.value * 0.36f).sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.inkPrimary
                )
            }
        }

        if (palette.cinematicTheme != null && palette.cinematicVariant != null) {
            CinematicAvatarFrameOverlay(
                frameStyle = palette.cinematicTheme.frameStyle,
                variant = palette.cinematicVariant,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (isOnline == true) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size((size * 0.28f).coerceAtLeast(11.dp))
                    .clip(CircleShape)
                    .background(palette.elevatedSurface)
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(palette.accent)
                )
            }
        }
    }
}

/**
 * Preserved Top-Left Two Horizontal Lines menu icon with tactile spring animation.
 */
@Composable
fun TwoLinesMenuButton(
    isReducedMotion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalJesrPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && !isReducedMotion) 0.87f else 1.0f,
        animationSpec = spring(dampingRatio = 0.50f, stiffness = 520f),
        label = "menu_btn_scale"
    )
    val pressBg by animateColorAsState(
        targetValue = if (isPressed) palette.pillSurface.copy(alpha = 0.9f) else Color.Transparent,
        animationSpec = tween(110),
        label = "menu_btn_bg"
    )
    val topLineWidth by animateDpAsState(
        targetValue = if (isPressed && !isReducedMotion) 26.dp else 24.dp,
        animationSpec = spring(dampingRatio = 0.60f, stiffness = 500f),
        label = "menu_top_line_width"
    )
    val bottomLineWidth by animateDpAsState(
        targetValue = if (isPressed && !isReducedMotion) 24.dp else 17.dp,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f),
        label = "menu_bottom_line_width"
    )

    Box(
        modifier = modifier
            .size(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(pressBg)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = palette.inkPrimary),
                onClick = onClick
            )
            .testTag("home_two_lines_menu"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .width(topLineWidth)
                    .height(2.5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(palette.iconColor)
            )
            Box(
                modifier = Modifier
                    .width(bottomLineWidth)
                    .height(2.5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(palette.iconColor)
            )
        }
    }
}

/**
 * Morphing Bottom-Left Floating (+) / (×) Button:
 * - Collapsed: square with soft rounded corners (18.dp) and upright (+) icon.
 * - Expanded: spins container (0° -> 90°), morphs shape into a Circle (29.dp),
 *   and spins (+) into (×) (+45° relative / 135° net). Reverses smoothly on close.
 */
@Composable
fun FloatingPlusActionButton(
    isReducedMotion: Boolean,
    isExpanded: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalJesrPalette.current
    val strings = LocalJesrStrings.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && !isReducedMotion) 0.88f else 1.0f,
        animationSpec = spring(dampingRatio = 0.50f, stiffness = 520f),
        label = "fab_scale"
    )
    val pressOffset by animateDpAsState(
        targetValue = if (isPressed && !isReducedMotion) 3.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 520f),
        label = "fab_offset"
    )
    val cornerRadius by animateDpAsState(
        targetValue = if (isExpanded) 29.dp else 18.dp,
        animationSpec = if (isReducedMotion) {
            tween(90)
        } else {
            spring(dampingRatio = 0.64f, stiffness = 360f)
        },
        label = "fab_corner_radius"
    )
    val containerRotation by animateFloatAsState(
        targetValue = if (isExpanded && !isReducedMotion) 90f else 0f,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 340f),
        label = "fab_container_rotation"
    )
    val iconRotation by animateFloatAsState(
        targetValue = when {
            isExpanded -> 45f
            isPressed && !isReducedMotion -> 18f
            else -> 0f
        },
        animationSpec = if (isReducedMotion) {
            tween(90)
        } else {
            spring(dampingRatio = 0.54f, stiffness = 360f)
        },
        label = "fab_icon_rotation"
    )
    val iconScale by animateFloatAsState(
        targetValue = when {
            isPressed && !isReducedMotion -> 1.12f
            isExpanded -> 1.06f
            else -> 1.0f
        },
        animationSpec = spring(dampingRatio = 0.52f, stiffness = 500f),
        label = "fab_icon_scale"
    )

    val hasCustomIcon = palette.iconColor != palette.inkPrimary
    val baseContainerBg = when {
        hasCustomIcon && palette.iconColor.luminance() < 0.35f -> palette.elevatedSurface
        hasCustomIcon -> Color(0xFF131312)
        palette.isDark -> palette.accent
        else -> palette.inkPrimary
    }
    val expandedContainerBg = when {
        hasCustomIcon && palette.iconColor.luminance() < 0.35f -> palette.cardSurface
        hasCustomIcon -> Color(0xFF1E1E1C)
        else -> TerracottaAccent
    }
    val containerBg by animateColorAsState(
        targetValue = if (isExpanded) expandedContainerBg else baseContainerBg,
        animationSpec = tween(220),
        label = "fab_bg_color"
    )
    val contentTint = if (hasCustomIcon) palette.iconColor else PureWhitePill
    val morphShape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .size(58.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = containerRotation
                translationX = pressOffset.toPx() * 0.35f
                translationY = pressOffset.toPx()
            }
            .drawBehind {
                val shadowFactor = if (isPressed) 0.45f else 1.0f
                val rPx = cornerRadius.toPx()
                drawRoundRect(
                    color = StoneBackdropDeep.copy(alpha = 0.28f * shadowFactor),
                    topLeft = Offset(3.dp.toPx() * shadowFactor, 8.dp.toPx() * shadowFactor),
                    size = size,
                    cornerRadius = CornerRadius(rPx, rPx)
                )
            }
            .shadow(
                elevation = if (isPressed) 6.dp else 16.dp,
                shape = morphShape,
                ambientColor = StoneBackdropDeep.copy(alpha = 0.55f),
                spotColor = StoneBackdropDeep.copy(alpha = 0.65f)
            )
            .clip(morphShape)
            .background(containerBg)
            .border(
                width = if (isExpanded) 1.dp else 0.7.dp,
                color = if (isExpanded) PureWhitePill.copy(alpha = 0.32f) else palette.border.copy(alpha = 0.35f),
                shape = morphShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = contentTint),
                onClick = onClick
            )
            .testTag("home_add_friend_fab"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = if (isExpanded) strings.close else strings.addFriendsCd,
            tint = contentTint,
            modifier = Modifier
                .size(26.dp)
                .graphicsLayer {
                    rotationZ = iconRotation
                    scaleX = iconScale
                    scaleY = iconScale
                }
        )
    }
}

/**
 * Consistent sub-screen header placing Back strictly at Top-Left per spec.
 */
@Composable
fun EditorialSubScreenHeader(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit,
    backTestTag: String = "subscreen_back_btn",
    trailingContent: (@Composable () -> Unit)? = null
) {
    val palette = LocalJesrPalette.current
    val strings = LocalJesrStrings.current
    val appLayoutDirection = LocalLayoutDirection.current
    val isReducedMotion = rememberIsReducedMotionEnabled()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            EditorialInteractiveIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = strings.back,
                containerColor = palette.pillSurface,
                contentColor = palette.iconColor,
                onClick = onBack,
                testTag = backTestTag,
                modifier = Modifier.staggeredIconEntrance(
                    index = 0,
                    stepDelayMs = 55,
                    startOffsetX = (-18).dp,
                    startOffsetY = (-6).dp,
                    startScale = 0.72f,
                    isReducedMotion = isReducedMotion
                )
            )

            Spacer(modifier = Modifier.width(10.dp))

            CompositionLocalProvider(LocalLayoutDirection provides appLayoutDirection) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .staggeredIconEntrance(
                            index = 1,
                            stepDelayMs = 55,
                            startOffsetX = 16.dp,
                            startOffsetY = 0.dp,
                            startScale = 0.94f,
                            isReducedMotion = isReducedMotion
                        ),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.inkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            fontSize = 11.sp,
                            color = palette.mutedText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (trailingContent != null) {
                Spacer(modifier = Modifier.width(8.dp))
                CompositionLocalProvider(LocalLayoutDirection provides appLayoutDirection) {
                    Box(
                        modifier = Modifier.staggeredIconEntrance(
                            index = 2,
                            stepDelayMs = 55,
                            startOffsetX = 18.dp,
                            startOffsetY = (-6).dp,
                            startScale = 0.72f,
                            isReducedMotion = isReducedMotion
                        )
                    ) {
                        trailingContent()
                    }
                }
            }
        }
    }
}

@Composable
fun BlockFriendDialog(
    friendName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalJesrPalette.current
    val strings = LocalJesrStrings.current
    val isReducedMotion = rememberIsReducedMotionEnabled()
    Dialog(onDismissRequest = onDismiss) {
        EditorialFloatingCard(
            modifier = Modifier
                .fillMaxWidth()
                .staggeredIconEntrance(index = 0, startOffsetY = 22.dp, startScale = 0.90f, isReducedMotion = isReducedMotion),
            backgroundColor = palette.cardSurface,
            contentPadding = 22.dp
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(TerracottaContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = null,
                        tint = TerracottaAccent,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = strings.blockDialogTitle(friendName),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.inkPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = strings.blockDialogBody,
                    fontSize = 12.5.sp,
                    color = palette.mutedText,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = TerracottaAccent),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("confirm_block_friend_btn")
                    ) {
                        Text(strings.confirmBlockBtn, color = PureWhitePill, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = palette.pillSurface),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("cancel_block_friend_btn")
                    ) {
                        Text(strings.cancel, color = palette.inkPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

/**
 * Section 12 mandatory confirmation explaining mutual deletion + external copies disclaimer.
 */
@Composable
fun RemoveFriendMutualDialog(
    friendName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalJesrPalette.current
    val strings = LocalJesrStrings.current
    val isReducedMotion = rememberIsReducedMotionEnabled()
    Dialog(onDismissRequest = onDismiss) {
        EditorialFloatingCard(
            modifier = Modifier
                .fillMaxWidth()
                .staggeredIconEntrance(index = 0, startOffsetY = 22.dp, startScale = 0.90f, isReducedMotion = isReducedMotion),
            backgroundColor = palette.cardSurface,
            contentPadding = 22.dp
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(TerracottaContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = TerracottaAccent,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = strings.removeDialogTitle(friendName),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.inkPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = strings.removeDialogBody,
                    fontSize = 12.sp,
                    color = palette.mutedText,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = TerracottaAccent),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("confirm_remove_friend_btn")
                    ) {
                        Text(strings.confirmRemoveBtn, color = PureWhitePill, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = palette.pillSurface),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("cancel_remove_friend_btn")
                    ) {
                        Text(strings.undo, color = palette.inkPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

/**
 * Section 11 second confirmation required before mutual resumption approval is finalized.
 */
@Composable
fun ResumeSecondConfirmationDialog(
    friendName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalJesrPalette.current
    val strings = LocalJesrStrings.current
    val isReducedMotion = rememberIsReducedMotionEnabled()
    Dialog(onDismissRequest = onDismiss) {
        EditorialFloatingCard(
            modifier = Modifier
                .fillMaxWidth()
                .staggeredIconEntrance(index = 0, startOffsetY = 22.dp, startScale = 0.90f, isReducedMotion = isReducedMotion),
            backgroundColor = palette.cardSurface,
            contentPadding = 22.dp
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(palette.accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = strings.resumeSecondTitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.inkPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = strings.resumeSecondBody(friendName),
                    fontSize = 12.sp,
                    color = palette.mutedText,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("confirm_resume_second_btn")
                    ) {
                        Text(strings.yesApproveBtn, color = PureWhitePill, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = palette.pillSurface),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(strings.cancel, color = palette.inkPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteAllIncomingRequestsDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalJesrPalette.current
    val strings = LocalJesrStrings.current
    val isReducedMotion = rememberIsReducedMotionEnabled()
    Dialog(onDismissRequest = onDismiss) {
        EditorialFloatingCard(
            modifier = Modifier
                .fillMaxWidth()
                .staggeredIconEntrance(index = 0, startOffsetY = 22.dp, startScale = 0.90f, isReducedMotion = isReducedMotion),
            backgroundColor = palette.cardSurface,
            contentPadding = 22.dp
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(TerracottaContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = TerracottaAccent,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = strings.deleteAllReqTitle,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.inkPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = strings.deleteAllReqBody(count),
                    fontSize = 12.5.sp,
                    color = palette.mutedText,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = TerracottaAccent),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("confirm_delete_all_requests_btn")
                    ) {
                        Text(strings.deleteAllBtn, color = PureWhitePill, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = palette.pillSurface),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(strings.cancel, color = palette.inkPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun FriendProfileModal(
    friend: FriendEntity,
    isOnline: Boolean,
    showMutualFriends: Boolean,
    onDismiss: () -> Unit
) {
    val palette = LocalJesrPalette.current
    val strings = LocalJesrStrings.current
    val clipboard = LocalClipboardManager.current
    val isReducedMotion = rememberIsReducedMotionEnabled()
    var copiedId by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        EditorialFloatingCard(
            modifier = Modifier
                .fillMaxWidth()
                .staggeredIconEntrance(index = 0, startOffsetY = 22.dp, startScale = 0.90f, isReducedMotion = isReducedMotion),
            backgroundColor = palette.cardSurface,
            contentPadding = 22.dp
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = strings.close,
                            tint = palette.inkPrimary
                        )
                    }
                }
                JesrAvatar(
                    displayName = friend.displayName,
                    avatarPath = friend.avatarUri,
                    colorIndex = friend.colorIndex,
                    size = 76.dp,
                    isOnline = isOnline
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = friend.displayName,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.inkPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(palette.elevatedSurface)
                        .clickable {
                            clipboard.setText(AnnotatedString(friend.jesrId))
                            copiedId = true
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = friend.jesrId,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.inkPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (copiedId) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = strings.copyIdOnlyCd,
                        tint = if (copiedId) palette.accent else palette.mutedText,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (isOnline) strings.statusMeshOnline else strings.statusMeshOffline,
                    fontSize = 12.sp,
                    color = if (isOnline) palette.accent else palette.mutedText,
                    fontWeight = FontWeight.SemiBold
                )
                if (showMutualFriends) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings.mutualFriendsCount(friend.mutualFriendsCount),
                        fontSize = 12.sp,
                        color = palette.inkSecondary
                    )
                }
                if (friend.publicKeyBase64.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = strings.keyFingerprint(friend.publicKeyBase64.take(24)),
                        fontSize = 10.5.sp,
                        color = palette.mutedText
                    )
                }
            }
        }
    }
}

/**
 * Staggered sequential entrance modifier: animates icons and cards one after another
 * with a clean spring slide + scale settle directly into their exact layout position.
 */
fun Modifier.staggeredIconEntrance(
    visible: Boolean = true,
    index: Int,
    stepDelayMs: Int = 65,
    initialDelayMs: Int = 0,
    startOffsetX: Dp = 0.dp,
    startOffsetY: Dp = 18.dp,
    startScale: Float = 0.78f,
    isReducedMotion: Boolean = false
): Modifier = composed {
    val progress = remember { Animatable(if (visible && isReducedMotion) 1f else 0f) }

    LaunchedEffect(visible, isReducedMotion) {
        if (!visible) {
            progress.snapTo(0f)
        } else if (isReducedMotion) {
            progress.snapTo(1f)
        } else {
            progress.snapTo(0f)
            val totalDelay = initialDelayMs + (index * stepDelayMs)
            if (totalDelay > 0) {
                delay(totalDelay.toLong())
            }
            progress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.68f,
                    stiffness = 340f
                )
            )
        }
    }

    val p = progress.value
    this.graphicsLayer {
        alpha = p.coerceIn(0f, 1f)
        translationX = (1f - p) * startOffsetX.toPx()
        translationY = (1f - p) * startOffsetY.toPx()
        val s = startScale + (1f - startScale) * p
        scaleX = s
        scaleY = s
    }
}

