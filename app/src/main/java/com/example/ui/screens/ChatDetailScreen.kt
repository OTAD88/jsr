package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.MessageEntity
import com.example.data.model.DeliveryStatus
import com.example.ui.components.BlockFriendDialog
import com.example.ui.components.EditorialInteractiveIconButton
import com.example.ui.components.EditorialSunlightBackdrop
import com.example.ui.components.FriendProfileModal
import com.example.ui.components.JesrAvatar
import com.example.ui.components.MessageDeliveryBadge
import com.example.ui.components.RemoveFriendMutualDialog
import com.example.ui.components.ResumeSecondConfirmationDialog
import com.example.ui.components.rememberIsReducedMotionEnabled
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberLight
import com.example.ui.theme.InkBlack
import com.example.ui.theme.LocalJesrPalette
import com.example.ui.theme.LocalJesrStrings
import com.example.ui.theme.PureWhitePill
import com.example.ui.theme.SageOliveContainer
import com.example.ui.theme.StoneBackdropShadow
import com.example.ui.theme.TerracottaAccent
import com.example.ui.theme.TerracottaContainer
import com.example.ui.viewmodel.JesrViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Conversation Screen (Spec Sections 5, 11, 12):
 * - Top-left: Back button returning to Friends list
 * - Top area: Friend's avatar + name
 * - Beside friend info: Conversation settings menu (5 items)
 * - Center: Message history (Empty, Connection Lost non-destructive, Message Failed + Retry, Connection Restored,
 *   and Section 11 Blocked / Unblocked Awaiting Mutual Resumption locks)
 * - Bottom: Attachment entry (PickVisualMedia), Message input field, Send button
 */
@Composable
fun ChatDetailScreen(
    viewModel: JesrViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val palette = LocalJesrPalette.current
    val strings = LocalJesrStrings.current
    val appLayoutDirection = LocalLayoutDirection.current
    val conversationId by viewModel.activeConversationId.collectAsStateWithLifecycle()
    val conversationName by viewModel.activeConversationName.collectAsStateWithLifecycle()
    val messages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val neighbors by viewModel.neighbors.collectAsStateWithLifecycle()
    val friends by viewModel.friends.collectAsStateWithLifecycle()
    val isMeshActive by viewModel.isMeshActive.collectAsStateWithLifecycle()
    val globalMutualAllowed by viewModel.globalMutualFriendsAllowed.collectAsStateWithLifecycle()
    val isReducedMotion = rememberIsReducedMotionEnabled()

    val activeFriend = remember(friends, conversationId) {
        conversationId?.let { id -> friends.find { it.jesrId == id } }
    }
    val displayTitle = activeFriend?.displayName ?: conversationName
    val isOnline = conversationId != null && neighbors.containsKey(conversationId)
    val isBlocked = activeFriend?.isBlocked == true
    val isAwaitingResume = activeFriend?.unblockedAwaitingResume == true
    val isHistoryLocked = isBlocked || isAwaitingResume

    var messageText by remember { mutableStateOf("") }
    var isEmergencySos by remember { mutableStateOf(false) }
    var selectedAttachmentUri by remember { mutableStateOf<String?>(null) }
    var selectedAttachmentName by remember { mutableStateOf<String?>(null) }

    var showSettingsMenu by remember { mutableStateOf(false) }
    var showFriendProfileModal by remember { mutableStateOf(false) }
    var showBlockConfirmDialog by remember { mutableStateOf(false) }
    var showRemoveConfirmDialog by remember { mutableStateOf(false) }
    var showResumeSecondConfirmDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    val attachmentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedAttachmentUri = uri.toString()
            selectedAttachmentName = uri.lastPathSegment?.substringAfterLast('/') ?: strings.attachedImageDefaultName
        }
    }

    LaunchedEffect(messages.size, isHistoryLocked) {
        if (!isHistoryLocked && messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (showFriendProfileModal && activeFriend != null) {
        val showMutual = activeFriend.peerMutualFriendsConsentEnabled &&
            (activeFriend.mutualFriendsPermissionOverride ?: globalMutualAllowed)
        FriendProfileModal(
            friend = activeFriend,
            isOnline = isOnline,
            showMutualFriends = showMutual,
            onDismiss = { showFriendProfileModal = false }
        )
    }

    if (showBlockConfirmDialog && activeFriend != null) {
        BlockFriendDialog(
            friendName = activeFriend.displayName,
            onConfirm = {
                viewModel.blockFriend(activeFriend.jesrId)
                showBlockConfirmDialog = false
            },
            onDismiss = { showBlockConfirmDialog = false }
        )
    }

    if (showRemoveConfirmDialog && activeFriend != null) {
        RemoveFriendMutualDialog(
            friendName = activeFriend.displayName,
            onConfirm = {
                showRemoveConfirmDialog = false
                viewModel.removeFriendMutually(activeFriend.jesrId)
            },
            onDismiss = { showRemoveConfirmDialog = false }
        )
    }

    if (showResumeSecondConfirmDialog && activeFriend != null) {
        ResumeSecondConfirmationDialog(
            friendName = activeFriend.displayName,
            onConfirm = {
                showResumeSecondConfirmDialog = false
                viewModel.approveResumeCommunication(activeFriend.jesrId)
            },
            onDismiss = { showResumeSecondConfirmDialog = false }
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
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .then(
                    if (palette.cardSurface.alpha >= 0.95f) {
                        Modifier.shadow(
                            elevation = 18.dp,
                            shape = RoundedCornerShape(28.dp),
                            ambientColor = StoneBackdropShadow.copy(alpha = 0.45f),
                            spotColor = StoneBackdropShadow.copy(alpha = 0.5f)
                        )
                    } else {
                        Modifier
                    }
                )
                .clip(RoundedCornerShape(28.dp))
                .background(palette.cardSurface)
                .border(1.dp, palette.highlight, RoundedCornerShape(28.dp))
                .imePadding()
        ) {
            // Top Bar: Top-left Back, Friend avatar & name, Conversation settings beside info
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.cardSurface)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EditorialInteractiveIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.backToFriendsCd,
                        containerColor = palette.pillSurface,
                        contentColor = palette.iconColor,
                        onClick = { viewModel.navigateBack() },
                        testTag = "chat_back_button"
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    // Friend info + Settings button in app's active content flow (RTL or LTR)
                    CompositionLocalProvider(LocalLayoutDirection provides appLayoutDirection) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            JesrAvatar(
                                displayName = displayTitle,
                                avatarPath = activeFriend?.avatarUri,
                                colorIndex = activeFriend?.colorIndex ?: 0,
                                size = 42.dp,
                                isOnline = if (!isHistoryLocked && isOnline) true else null
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = displayTitle,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.inkPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = strings.e2eeCd,
                                        tint = palette.accent,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                Text(
                                    text = when {
                                        isBlocked -> strings.chatLockedBlocked
                                        isAwaitingResume -> strings.chatAwaitingResume
                                        isOnline -> strings.chatOnlineRange
                                        else -> strings.chatDirectUnavailable
                                    },
                                    fontSize = 11.sp,
                                    color = if (isOnline && !isHistoryLocked) palette.accent else palette.mutedText,
                                    maxLines = 1
                                )
                            }

                            if (activeFriend != null) {
                                Box {
                                    EditorialInteractiveIconButton(
                                        icon = Icons.Default.MoreVert,
                                        contentDescription = strings.chatSettingsCd,
                                        containerColor = palette.pillSurface,
                                        contentColor = palette.iconColor,
                                        buttonSize = 48.dp,
                                        iconSize = 20.dp,
                                        onClick = { showSettingsMenu = true },
                                        testTag = "chat_settings_menu_btn"
                                    )

                                    DropdownMenu(
                                        expanded = showSettingsMenu,
                                        onDismissRequest = { showSettingsMenu = false },
                                        modifier = Modifier
                                            .background(palette.cardSurface)
                                            .testTag("chat_settings_dropdown")
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(strings.viewFriendProfile, color = palette.inkPrimary, fontWeight = FontWeight.SemiBold) },
                                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = palette.iconColor) },
                                            onClick = {
                                                showSettingsMenu = false
                                                showFriendProfileModal = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(strings.friendPrivacySettings, color = palette.inkPrimary, fontWeight = FontWeight.SemiBold) },
                                            leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, tint = palette.iconColor) },
                                            onClick = {
                                                showSettingsMenu = false
                                                viewModel.openFriendSettings(activeFriend.jesrId)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    if (activeFriend.isMuted) strings.unmuteNotifications else strings.muteNotifications,
                                                    color = palette.inkPrimary,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    if (activeFriend.isMuted) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                                    contentDescription = null,
                                                    tint = palette.iconColor
                                                )
                                            },
                                            onClick = {
                                                showSettingsMenu = false
                                                viewModel.toggleMuteFriend(activeFriend.jesrId)
                                            }
                                        )
                                        HorizontalDivider(color = palette.border)
                                        if (!activeFriend.isBlocked) {
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = strings.blockFriend,
                                                        color = TerracottaAccent,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                },
                                                leadingIcon = { Icon(Icons.Default.Block, contentDescription = null, tint = TerracottaAccent) },
                                                onClick = {
                                                    showSettingsMenu = false
                                                    showBlockConfirmDialog = true
                                                }
                                            )
                                        }
                                        DropdownMenuItem(
                                            text = { Text(strings.removeFriend, color = TerracottaAccent, fontWeight = FontWeight.Bold) },
                                            leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = TerracottaAccent) },
                                            onClick = {
                                                showSettingsMenu = false
                                                showRemoveConfirmDialog = true
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = palette.border)

            // Non-destructive Connection Lost status banner
            if (!isOnline && !isHistoryLocked && conversationId != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.cardSurfaceWarm)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = palette.mutedText,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = strings.directLostBanner,
                        fontSize = 11.sp,
                        color = palette.mutedText,
                        lineHeight = 16.sp
                    )
                }
            }

            // Center Area: Locked (Blocked / Unblocked Awaiting Mutual Resume) OR Message History
            when {
                isBlocked && activeFriend != null -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(palette.elevatedSurface)
                                .border(0.8.dp, palette.border, RoundedCornerShape(24.dp))
                                .padding(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                tint = TerracottaAccent,
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = strings.friendBlockedTitle(activeFriend.displayName),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.inkPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = strings.friendBlockedBody,
                                fontSize = 12.sp,
                                color = palette.mutedText,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.unblockFriend(activeFriend.jesrId) },
                                colors = ButtonDefaults.buttonColors(containerColor = palette.pillSurface, contentColor = palette.inkPrimary),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("chat_unblock_btn")
                            ) {
                                Text(strings.unblockBtn, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }

                isAwaitingResume && activeFriend != null -> {
                    // Section 11: Unblocked but locked until mutual approval;
                    // No prominent reject button; small unobtrusive approval control -> 2nd confirmation
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(palette.elevatedSurface)
                                .border(0.8.dp, palette.border, RoundedCornerShape(24.dp))
                                .padding(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = palette.mutedText,
                                modifier = Modifier.size(30.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = strings.resumeInviteTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.inkPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (activeFriend.localResumeApproved) {
                                    strings.resumeApprovedWaitingBody(activeFriend.displayName)
                                } else {
                                    strings.resumeHiddenUntilMutualBody
                                },
                                fontSize = 12.sp,
                                color = palette.mutedText,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                            if (!activeFriend.localResumeApproved) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = strings.approveResumeBtn,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = palette.accent,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(palette.accent.copy(alpha = 0.12f))
                                        .clickable { showResumeSecondConfirmDialog = true }
                                        .testTag("chat_resume_approve_small_btn")
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (messages.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = strings.emptyChatHint,
                                        fontSize = 13.sp,
                                        color = palette.mutedText,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        } else {
                            itemsIndexed(messages, key = { _, msg -> msg.id }) { _, msg ->
                                AnimatedVisibility(
                                    visible = true,
                                    enter = if (isReducedMotion) {
                                        fadeIn()
                                    } else {
                                        fadeIn(animationSpec = tween(200)) + slideInVertically(
                                            initialOffsetY = { 20 },
                                            animationSpec = tween(200)
                                        )
                                    }
                                ) {
                                    MessageBubbleItem(
                                        msg = msg,
                                        onRetry = { viewModel.retryMessage(msg.id) }
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Input Area
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(palette.cardSurface)
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .navigationBarsPadding()
                    ) {
                        if (selectedAttachmentName != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(palette.cardSurfaceWarm)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = strings.attachmentPrefix(selectedAttachmentName.orEmpty()),
                                    fontSize = 12.sp,
                                    color = palette.inkPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        selectedAttachmentUri = null
                                        selectedAttachmentName = null
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = strings.removeAttachmentCd, tint = palette.mutedText, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        if (isEmergencySos) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(TerracottaContainer)
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Campaign,
                                        contentDescription = null,
                                        tint = TerracottaAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = strings.sosActiveBanner,
                                        fontSize = 12.sp,
                                        color = TerracottaAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = strings.cancel,
                                    fontSize = 12.sp,
                                    color = palette.inkPrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { isEmergencySos = false }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Attachment entry near message input
                            IconButton(
                                onClick = {
                                    attachmentPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(palette.pillSurface)
                                    .testTag("chat_attach_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = strings.attachPhotoCd,
                                    tint = palette.iconColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            IconButton(
                                onClick = { isEmergencySos = !isEmergencySos },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(if (isEmergencySos) TerracottaAccent else palette.pillSurface)
                                    .testTag("chat_sos_toggle")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = strings.sosCd,
                                    tint = if (isEmergencySos) PureWhitePill else palette.iconColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            OutlinedTextField(
                                value = messageText,
                                onValueChange = { messageText = it },
                                placeholder = {
                                    Text(
                                        text = if (isEmergencySos) strings.emergencyPlaceholder else strings.messagePlaceholder,
                                        color = palette.mutedText,
                                        fontSize = 13.sp
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chat_message_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = palette.inkPrimary,
                                    unfocusedTextColor = palette.inkPrimary,
                                    focusedContainerColor = palette.elevatedSurface,
                                    unfocusedContainerColor = palette.elevatedSurface,
                                    cursorColor = palette.inkPrimary,
                                    focusedBorderColor = if (isEmergencySos) TerracottaAccent else palette.inkPrimary,
                                    unfocusedBorderColor = palette.border
                                ),
                                shape = RoundedCornerShape(50),
                                maxLines = 4
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            val canSend = messageText.isNotBlank() || selectedAttachmentName != null
                            val sendInteraction = remember { MutableInteractionSource() }
                            val sendPressed by sendInteraction.collectIsPressedAsState()
                            val sendScale by animateFloatAsState(
                                targetValue = if (sendPressed && canSend && !isReducedMotion) 0.86f else 1.0f,
                                animationSpec = spring(dampingRatio = 0.48f, stiffness = 520f),
                                label = "send_scale"
                            )

                            IconButton(
                                onClick = {
                                    if (canSend) {
                                        viewModel.sendMessage(
                                            text = messageText,
                                            isSos = isEmergencySos,
                                            attachmentUri = selectedAttachmentUri,
                                            attachmentName = selectedAttachmentName
                                        )
                                        messageText = ""
                                        isEmergencySos = false
                                        selectedAttachmentUri = null
                                        selectedAttachmentName = null
                                    }
                                },
                                enabled = canSend,
                                interactionSource = sendInteraction,
                                modifier = Modifier
                                    .size(48.dp)
                                    .scale(sendScale)
                                    .clip(CircleShape)
                                    .background(if (canSend) palette.inkPrimary else palette.pillSurface)
                                    .testTag("chat_send_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = strings.sendCd,
                                    tint = if (canSend) palette.elevatedSurface else palette.mutedText,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubbleItem(
    msg: MessageEntity,
    onRetry: () -> Unit = {}
) {
    val palette = LocalJesrPalette.current
    val strings = LocalJesrStrings.current
    val isMe = !msg.isIncoming
    val alignment = if (isMe) Alignment.End else Alignment.Start

    val locale = if (strings.isEnglish) Locale.ENGLISH else Locale("ar")
    val timeFormatted = remember(msg.timestamp, strings.isEnglish) {
        SimpleDateFormat("h:mm a", locale).format(Date(msg.timestamp))
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        val cinSpec = palette.cinematicTheme
        val cinVariant = palette.cinematicVariant
        val bubbleShape = when (cinSpec?.bubbleStyle) {
            com.example.ui.theme.CinematicBubbleStyle.CYBER_CHAMFER -> {
                if (isMe) RoundedCornerShape(topStart = 18.dp, topEnd = 2.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
                else RoundedCornerShape(topStart = 2.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
            }
            com.example.ui.theme.CinematicBubbleStyle.ROYAL_SILK -> {
                if (isMe) RoundedCornerShape(topStart = 24.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 20.dp)
                else RoundedCornerShape(topStart = 8.dp, topEnd = 24.dp, bottomStart = 20.dp, bottomEnd = 24.dp)
            }
            else -> {
                if (isMe) RoundedCornerShape(topStart = 20.dp, topEnd = 6.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
                else RoundedCornerShape(topStart = 6.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
            }
        }

        val bubbleBackgroundBrush = when {
            msg.isSos -> androidx.compose.ui.graphics.Brush.linearGradient(listOf(TerracottaAccent, TerracottaAccent))
            cinVariant != null && isMe -> androidx.compose.ui.graphics.Brush.linearGradient(
                listOf(cinVariant.bubbleOutStart, cinVariant.bubbleOutEnd)
            )
            cinVariant != null -> androidx.compose.ui.graphics.Brush.linearGradient(
                listOf(cinVariant.bubbleInBg, cinVariant.bubbleInBg)
            )
            isMe -> {
                val c = if (palette.isDark) palette.accent else palette.inkPrimary
                androidx.compose.ui.graphics.Brush.linearGradient(listOf(c, c))
            }
            else -> androidx.compose.ui.graphics.Brush.linearGradient(listOf(palette.elevatedSurface, palette.elevatedSurface))
        }

        val isDarkBubble = msg.isSos || isMe
        val textColor = when {
            msg.isSos -> PureWhitePill
            cinVariant != null && isMe -> {
                val avgLum = (cinVariant.bubbleOutStart.luminance() + cinVariant.bubbleOutEnd.luminance()) / 2f
                if (avgLum > 0.56f) InkBlack else PureWhitePill
            }
            isDarkBubble -> PureWhitePill
            else -> palette.inkPrimary
        }
        val subTextColor = if (textColor == InkBlack) InkBlack.copy(alpha = 0.65f) else if (isDarkBubble) Color(0xFFEAE2D6) else palette.mutedText
        val frameStrokeBrush = when {
            cinVariant != null && isMe -> androidx.compose.ui.graphics.Brush.linearGradient(
                listOf(cinVariant.lightColor.copy(alpha = 0.85f), cinVariant.bubbleBorder)
            )
            cinVariant != null -> androidx.compose.ui.graphics.Brush.linearGradient(
                listOf(cinVariant.bubbleBorder, cinVariant.frameSecondary.copy(alpha = 0.5f))
            )
            else -> androidx.compose.ui.graphics.Brush.linearGradient(
                listOf(if (isDarkBubble) Color.Transparent else palette.border, if (isDarkBubble) Color.Transparent else palette.border)
            )
        }
        val borderWidth = if (cinVariant != null) 1.4.dp else 0.6.dp

        Box(
            modifier = Modifier
                .widthIn(min = 84.dp, max = 290.dp)
                .shadow(
                    elevation = if (isMe) 4.dp else 8.dp,
                    shape = bubbleShape,
                    ambientColor = StoneBackdropShadow.copy(alpha = 0.22f)
                )
                .clip(bubbleShape)
                .background(bubbleBackgroundBrush)
                .border(borderWidth, frameStrokeBrush, bubbleShape)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column {
                Text(
                    text = msg.text,
                    fontSize = 14.sp,
                    color = textColor,
                    lineHeight = 20.sp
                )

                if (!msg.isBroadcast && msg.isDistanceConsented && msg.estimatedDistanceMeters != null) {
                    Spacer(modifier = Modifier.height(5.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (isDarkBubble) AmberContainer.copy(alpha = 0.28f)
                                else SageOliveContainer
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NearMe,
                            contentDescription = null,
                            tint = if (isDarkBubble) AmberLight else palette.accent,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = strings.sentFromDistance(msg.estimatedDistanceMeters),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDarkBubble) AmberLight else palette.accent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = timeFormatted,
                        fontSize = 10.sp,
                        color = subTextColor
                    )

                    if (isMe) {
                        MessageDeliveryBadge(
                            status = msg.status,
                            hopCount = msg.hopCount,
                            isOnDarkBubble = isDarkBubble
                        )
                    }
                }

                // Explicit Retry action for FAILED messages (Section 5)
                if (isMe && msg.status == DeliveryStatus.FAILED) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(TerracottaContainer)
                            .clickable(onClick = onRetry)
                            .testTag("retry_message_${msg.id}")
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = strings.retry,
                            tint = AmberLight,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = strings.retry,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberLight
                        )
                    }
                }
            }
        }
    }
}
