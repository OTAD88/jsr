package com.example.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.EditorialFloatingCard
import com.example.ui.components.EditorialInteractiveIconButton
import com.example.ui.components.EditorialSunlightBackdrop
import com.example.ui.components.ImageCropperDialog
import com.example.ui.components.JesrAvatar
import com.example.ui.components.rememberIsReducedMotionEnabled
import com.example.ui.components.staggeredIconEntrance
import com.example.ui.theme.InkBlack
import com.example.ui.theme.LocalJesrPalette
import com.example.ui.theme.LocalJesrStrings
import com.example.ui.theme.PureWhitePill
import com.example.ui.viewmodel.JesrViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

/**
 * User Profile Screen (Spec Section 4 + Account Cover Customization):
 * - Top: Customizable Cover Banner + Back button + Profile picture (gallery picker + cropper)
 * - Below picture: Display name
 * - Below name: Unique JISR ID + Copy button copying ONLY the raw ID
 */
@Composable
fun ProfileScreen(
    viewModel: JesrViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val palette = LocalJesrPalette.current
    val strings = LocalJesrStrings.current
    val appLayoutDirection = LocalLayoutDirection.current
    val clipboard = LocalClipboardManager.current
    val isReducedMotion = rememberIsReducedMotionEnabled()
    val isMeshActive by viewModel.isMeshActive.collectAsStateWithLifecycle()
    val userDisplayName by viewModel.userDisplayName.collectAsStateWithLifecycle()
    val userAvatarPath by viewModel.userAvatarPath.collectAsStateWithLifecycle()
    val userCoverPath by viewModel.userCoverPath.collectAsStateWithLifecycle()

    var isEditingName by remember { mutableStateOf(false) }
    var draftName by remember(userDisplayName) { mutableStateOf(userDisplayName) }
    var copiedIdOnly by remember { mutableStateOf(false) }
    var pendingCropUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCoverCropUri by remember { mutableStateOf<Uri?>(null) }
    var showConfirmCoverDialog by remember { mutableStateOf(false) }

    val coverBitmap by produceState<ImageBitmap?>(initialValue = null, userCoverPath) {
        value = if (!userCoverPath.isNullOrBlank()) {
            withContext(Dispatchers.IO) {
                runCatching {
                    val file = File(userCoverPath!!)
                    if (file.exists()) BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() else null
                }.getOrNull()
            }
        } else {
            null
        }
    }

    LaunchedEffect(copiedIdOnly) {
        if (copiedIdOnly) {
            delay(1800)
            copiedIdOnly = false
        }
    }

    // Zero-permission Android Photo Picker -> opens interactive ImageCropperDialog before saving
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pendingCropUri = uri
        }
    }

    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pendingCoverCropUri = uri
        }
    }

    if (pendingCropUri != null) {
        ImageCropperDialog(
            imageUri = pendingCropUri!!,
            onConfirmCrop = { croppedBitmap ->
                viewModel.updateUserAvatarBitmap(croppedBitmap)
                pendingCropUri = null
            },
            onDismiss = { pendingCropUri = null }
        )
    }

    if (pendingCoverCropUri != null) {
        ImageCropperDialog(
            imageUri = pendingCoverCropUri!!,
            aspectRatio = 16f / 9f,
            isCircularMask = false,
            titleOverride = strings.cropCoverTitle,
            subtitleOverride = strings.cropCoverSubtitle,
            onConfirmCrop = { croppedBitmap ->
                viewModel.updateUserCoverBitmap(croppedBitmap)
                pendingCoverCropUri = null
            },
            onDismiss = { pendingCoverCropUri = null }
        )
    }

    if (showConfirmCoverDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmCoverDialog = false },
            containerColor = palette.elevatedSurface,
            titleContentColor = palette.inkPrimary,
            textContentColor = palette.inkSecondary,
            modifier = Modifier.testTag("profile_cover_confirm_dialog"),
            title = {
                Text(
                    text = strings.confirmCoverDialogTitle,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp
                )
            },
            text = {
                Text(
                    text = strings.confirmCoverDialogBody,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmCoverDialog = false
                        coverPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.testTag("profile_cover_confirm_proceed_btn")
                ) {
                    Text(
                        text = strings.confirmCoverProceedBtn,
                        fontWeight = FontWeight.Bold,
                        color = if (palette.accent.luminance() > 0.55f) InkBlack else PureWhitePill
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmCoverDialog = false },
                    modifier = Modifier.testTag("profile_cover_confirm_cancel_btn")
                ) {
                    Text(
                        text = strings.cancel,
                        color = palette.mutedText,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        EditorialSunlightBackdrop(
            modifier = Modifier.fillMaxSize(),
            isMeshActive = isMeshActive
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 10.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState())
        ) {
            EditorialFloatingCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = palette.cardSurface,
                contentPadding = 20.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Cover Banner Header + Top-Left Back Button + Avatar Overlap
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // 16:9 Account Cover Image Container
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(168.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            palette.accent.copy(alpha = 0.28f),
                                            palette.cardSurfaceWarm,
                                            palette.pillSurface
                                        )
                                    )
                                )
                                .border(0.9.dp, palette.border, RoundedCornerShape(22.dp))
                                .clickable {
                                    showConfirmCoverDialog = true
                                }
                                .testTag("profile_cover_banner")
                        ) {
                            if (coverBitmap != null) {
                                Image(
                                    bitmap = coverBitmap!!,
                                    contentDescription = strings.coverPhotoTitle,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Black.copy(alpha = 0.24f),
                                                    Color.Transparent,
                                                    Color.Black.copy(alpha = 0.36f)
                                                )
                                            )
                                        )
                                )
                            } else {
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .padding(horizontal = 18.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = strings.coverPhotoTitle,
                                        tint = palette.inkPrimary.copy(alpha = 0.72f),
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = strings.addCoverHint,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = palette.inkSecondary
                                    )
                                }
                            }

                            // LTR Row at top for Back button + Cover Actions
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    EditorialInteractiveIconButton(
                                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = strings.back,
                                        containerColor = palette.cardSurface.copy(alpha = 0.88f),
                                        contentColor = palette.iconColor,
                                        onClick = { viewModel.navigateBack() },
                                        testTag = "profile_back_btn",
                                        modifier = Modifier.staggeredIconEntrance(
                                            index = 0,
                                            stepDelayMs = 65,
                                            startOffsetX = (-18).dp,
                                            startOffsetY = 0.dp,
                                            isReducedMotion = isReducedMotion
                                        )
                                    )

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (coverBitmap != null) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .background(Color.Black.copy(alpha = 0.56f))
                                                    .clickable { viewModel.clearUserCover() }
                                                    .testTag("profile_clear_cover_btn")
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = strings.removeCoverBtn,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .background(palette.cardSurface.copy(alpha = 0.90f))
                                                .border(0.7.dp, palette.border, RoundedCornerShape(50))
                                                .clickable {
                                                    showConfirmCoverDialog = true
                                                }
                                                .testTag("profile_change_cover_btn")
                                                .padding(horizontal = 12.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AddPhotoAlternate,
                                                contentDescription = null,
                                                tint = palette.iconColor,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Text(
                                                text = strings.changeCoverBtn,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = palette.inkPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Avatar overlapping smoothly below cover
                    val avatarInteraction = remember { MutableInteractionSource() }
                    val isAvatarPressed by avatarInteraction.collectIsPressedAsState()
                    val avatarScale by animateFloatAsState(
                        targetValue = if (isAvatarPressed) 0.94f else 1f,
                        animationSpec = spring(dampingRatio = 0.56f, stiffness = 500f),
                        label = "profile_avatar_scale"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-28).dp)
                            .padding(horizontal = 10.dp)
                            .staggeredIconEntrance(
                                index = 1,
                                stepDelayMs = 65,
                                startOffsetX = 18.dp,
                                startOffsetY = 0.dp,
                                isReducedMotion = isReducedMotion
                            ),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .graphicsLayer {
                                    scaleX = avatarScale
                                    scaleY = avatarScale
                                }
                                .clip(CircleShape)
                                .background(palette.cardSurface)
                                .padding(3.dp)
                                .clip(CircleShape)
                                .semantics {
                                    contentDescription = strings.changeProfilePicCd
                                }
                                .clickable(
                                    interactionSource = avatarInteraction,
                                    indication = ripple(bounded = true, color = palette.accent),
                                    role = Role.Button,
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                )
                                .testTag("profile_change_avatar_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            JesrAvatar(
                                displayName = userDisplayName,
                                avatarPath = userAvatarPath,
                                size = 84.dp
                            )
                        }

                        Column(modifier = Modifier.padding(bottom = 6.dp)) {
                            Text(
                                text = strings.tapPhotoToChangeHint,
                                fontSize = 11.sp,
                                color = palette.mutedText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Below picture: Display name
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .staggeredIconEntrance(
                                index = 2,
                                stepDelayMs = 65,
                                startOffsetX = if (appLayoutDirection == LayoutDirection.Rtl) (-24).dp else 24.dp,
                                startOffsetY = 0.dp,
                                startScale = 0.94f,
                                isReducedMotion = isReducedMotion
                            )
                    ) {
                        Text(
                            text = strings.displayNameLabel,
                            fontSize = 12.sp,
                            color = palette.mutedText,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        if (isEditingName) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = draftName,
                                    onValueChange = { draftName = it },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("profile_name_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = palette.inkPrimary,
                                        unfocusedTextColor = palette.inkPrimary,
                                        focusedContainerColor = palette.elevatedSurface,
                                        unfocusedContainerColor = palette.elevatedSurface,
                                        focusedBorderColor = palette.inkPrimary,
                                        unfocusedBorderColor = palette.border
                                    ),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                Button(
                                    onClick = {
                                        if (draftName.isNotBlank()) {
                                            viewModel.updateUserDisplayName(draftName)
                                            isEditingName = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = strings.save, tint = PureWhitePill)
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(palette.cardSurfaceWarm)
                                    .clickable { isEditingName = true }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = userDisplayName,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = palette.inkPrimary
                                )
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = strings.editNameCd,
                                    tint = palette.mutedText,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Below name: Unique JISR ID card + separate icon-only Copy button beside it
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .staggeredIconEntrance(
                                index = 3,
                                stepDelayMs = 65,
                                startOffsetX = if (appLayoutDirection == LayoutDirection.Rtl) (-24).dp else 24.dp,
                                startOffsetY = 0.dp,
                                startScale = 0.94f,
                                isReducedMotion = isReducedMotion
                            )
                    ) {
                        Text(
                            text = strings.uniqueJesrIdLabel,
                            fontSize = 12.sp,
                            color = palette.mutedText,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val copyContainerColor = if (copiedIdOnly) palette.accent else palette.inkPrimary
                        val copyIconTint = if (copyContainerColor.luminance() > 0.55f) InkBlack else PureWhitePill

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(palette.elevatedSurface)
                                    .border(0.8.dp, palette.border, RoundedCornerShape(20.dp))
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = viewModel.myJesrId,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.inkPrimary,
                                    modifier = Modifier.testTag("profile_jesr_id_text")
                                )
                            }

                            EditorialInteractiveIconButton(
                                icon = if (copiedIdOnly) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = strings.copyIdOnlyCd,
                                containerColor = copyContainerColor,
                                contentColor = copyIconTint,
                                buttonSize = 50.dp,
                                iconSize = 20.dp,
                                onClick = {
                                    // Section 4 rule: Copy ID ONLY — no invitations, spaces, or extra text
                                    clipboard.setText(AnnotatedString(viewModel.myJesrId))
                                    copiedIdOnly = true
                                },
                                testTag = "profile_copy_id_btn"
                            )
                        }
                    }
                }
            }
        }
    }
}
