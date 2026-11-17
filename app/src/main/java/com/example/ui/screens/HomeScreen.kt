package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Outbox
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.ConversationEntity
import com.example.data.entity.FriendEntity
import com.example.ui.components.BlockFriendDialog
import com.example.ui.components.EditorialFloatingCard
import com.example.ui.components.EditorialInteractiveIconButton
import com.example.ui.components.EditorialSunlightBackdrop
import com.example.ui.components.FloatingPlusActionButton
import com.example.ui.components.JesrAvatar
import com.example.ui.components.RemoveFriendMutualDialog
import com.example.ui.components.TwoLinesMenuButton
import com.example.ui.components.rememberIsReducedMotionEnabled
import com.example.ui.components.staggeredIconEntrance
import com.example.ui.theme.LocalJesrPalette
import com.example.ui.theme.LocalJesrStrings
import com.example.ui.theme.PureWhitePill
import com.example.ui.theme.StoneBackdropDeep
import com.example.ui.theme.StoneBackdropShadow
import com.example.ui.theme.TerracottaAccent
import com.example.ui.viewmodel.FriendRequestsTab
import com.example.ui.viewmodel.JesrViewModel
import com.example.ui.viewmodel.Screen

/**
 * Main Screen — Friends List (Spec Section 1):
 * - Top-left: Main menu icon (Two Horizontal Lines) -> opens Side Navigation Menu
 * - Top area: Search icon -> opens friend & conversation search
 * - Top-right: User profile icon -> opens User Profile screen
 * - Main area: Friends list (Avatar, Display Name, Unique JISR ID, Connection Status)
 *   - Tap -> Open private conversation
 *   - Long-press -> Contextual menu (Open conversation, Friend settings, Mute notifications, Block friend, Remove friend)
 * - Bottom-left: Single (+) button -> opens Add Friends hub
 */
@Composable
fun HomeScreen(
    viewModel: JesrViewModel,
    modifier: Modifier = Modifier
) {
    val palette = LocalJesrPalette.current
    val strings = LocalJesrStrings.current
    val appLayoutDirection = LocalLayoutDirection.current
    val isMeshActive by viewModel.isMeshActive.collectAsStateWithLifecycle()
    val friends by viewModel.friends.collectAsStateWithLifecycle()
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val neighbors by viewModel.neighbors.collectAsStateWithLifecycle()
    val pendingRequests by viewModel.pendingRequests.collectAsStateWithLifecycle()
    val sentRequests by viewModel.sentRequests.collectAsStateWithLifecycle()
    val userDisplayName by viewModel.userDisplayName.collectAsStateWithLifecycle()
    val userAvatarPath by viewModel.userAvatarPath.collectAsStateWithLifecycle()
    val isReducedMotion = rememberIsReducedMotionEnabled()

    val nearbyCount = remember(neighbors) { neighbors.values.count { it.nearbyVisible } }
    var isFabMenuOpen by remember { mutableStateOf(false) }
    var isSearchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var pendingBlockFriend by remember { mutableStateOf<FriendEntity?>(null) }
    var pendingRemoveFriend by remember { mutableStateOf<FriendEntity?>(null) }

    BackHandler(enabled = isFabMenuOpen) {
        isFabMenuOpen = false
    }

    val fabShiftX by animateDpAsState(
        targetValue = if (isFabMenuOpen) 103.dp else 0.dp,
        animationSpec = if (isReducedMotion) {
            tween(100)
        } else {
            spring(dampingRatio = 0.66f, stiffness = 340f)
        },
        label = "fab_shift_x"
    )
    val fabShiftY by animateDpAsState(
        targetValue = if (isFabMenuOpen) (-4).dp else 0.dp,
        animationSpec = if (isReducedMotion) {
            tween(100)
        } else {
            spring(dampingRatio = 0.66f, stiffness = 340f)
        },
        label = "fab_shift_y"
    )

    val convMap = remember(conversations) { conversations.associateBy { it.id } }

    val filteredFriends = remember(friends, convMap, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) {
            friends
        } else {
            friends.filter { friend ->
                val lastMsg = convMap[friend.jesrId]?.lastMessageText.orEmpty()
                friend.displayName.contains(q, ignoreCase = true) ||
                    friend.jesrId.contains(q, ignoreCase = true) ||
                    lastMsg.contains(q, ignoreCase = true)
            }
        }
    }

    if (pendingBlockFriend != null) {
        val target = pendingBlockFriend!!
        BlockFriendDialog(
            friendName = target.displayName,
            onConfirm = {
                viewModel.blockFriend(target.jesrId)
                pendingBlockFriend = null
            },
            onDismiss = { pendingBlockFriend = null }
        )
    }

    if (pendingRemoveFriend != null) {
        val target = pendingRemoveFriend!!
        RemoveFriendMutualDialog(
            friendName = target.displayName,
            onConfirm = {
                viewModel.removeFriendMutually(target.jesrId)
                pendingRemoveFriend = null
            },
            onDismiss = { pendingRemoveFriend = null }
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
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            EditorialFloatingCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                backgroundColor = palette.cardSurface,
                contentPadding = 18.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header: Strictly Top-Left Menu icon, Top Search icon, Top-Right User Profile icon
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Top-left: Preserved Two Horizontal Lines menu icon
                            TwoLinesMenuButton(
                                isReducedMotion = isReducedMotion,
                                onClick = { viewModel.openDrawer() },
                                modifier = Modifier.staggeredIconEntrance(
                                    index = 0,
                                    stepDelayMs = 75,
                                    startOffsetX = (-18).dp,
                                    startOffsetY = (-10).dp,
                                    startScale = 0.70f,
                                    isReducedMotion = isReducedMotion
                                )
                            )

                            // Top area: App title + Search icon
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = strings.appName,
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = palette.inkPrimary
                                )
                                EditorialInteractiveIconButton(
                                    icon = if (isSearchVisible) Icons.Default.Close else Icons.Default.Search,
                                    contentDescription = strings.searchFriendsCd,
                                    containerColor = if (isSearchVisible) palette.inkPrimary else palette.pillSurface,
                                    contentColor = if (isSearchVisible) palette.elevatedSurface else palette.iconColor,
                                    buttonSize = 48.dp,
                                    iconSize = 20.dp,
                                    onClick = {
                                        isSearchVisible = !isSearchVisible
                                        if (!isSearchVisible) searchQuery = ""
                                    },
                                    testTag = "home_search_btn",
                                    modifier = Modifier.staggeredIconEntrance(
                                        index = 1,
                                        stepDelayMs = 75,
                                        startOffsetX = 0.dp,
                                        startOffsetY = (-14).dp,
                                        startScale = 0.70f,
                                        isReducedMotion = isReducedMotion
                                    )
                                )
                            }

                            // Top-right: User Profile icon
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .staggeredIconEntrance(
                                        index = 2,
                                        stepDelayMs = 75,
                                        startOffsetX = 18.dp,
                                        startOffsetY = (-10).dp,
                                        startScale = 0.70f,
                                        isReducedMotion = isReducedMotion
                                    )
                                    .clip(CircleShape)
                                    .clickable { viewModel.navigateTo(Screen.PROFILE) }
                                    .testTag("home_profile_btn"),
                                contentAlignment = Alignment.Center
                            ) {
                                JesrAvatar(
                                    displayName = userDisplayName,
                                    avatarPath = userAvatarPath,
                                    size = 42.dp
                                )
                            }
                        }
                    }

                    // Collapsible Search Bar for friends & conversations
                    AnimatedVisibility(
                        visible = isSearchVisible,
                        enter = fadeIn(tween(180)) + expandVertically(tween(200)),
                        exit = fadeOut(tween(150)) + shrinkVertically(tween(180))
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text(
                                        text = strings.searchPlaceholder,
                                        fontSize = 13.sp,
                                        color = palette.mutedText
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = palette.mutedText,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = strings.clearSearchCd,
                                                tint = palette.mutedText,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("home_search_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = palette.inkPrimary,
                                    unfocusedTextColor = palette.inkPrimary,
                                    focusedContainerColor = palette.elevatedSurface,
                                    unfocusedContainerColor = palette.elevatedSurface,
                                    cursorColor = palette.inkPrimary,
                                    focusedBorderColor = palette.inkPrimary,
                                    unfocusedBorderColor = palette.border
                                ),
                                shape = RoundedCornerShape(50)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Friends List
                    if (filteredFriends.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (friends.isEmpty()) {
                                    strings.emptyFriendsHint
                                } else {
                                    strings.noSearchResults
                                },
                                fontSize = 13.5.sp,
                                color = palette.mutedText,
                                textAlign = TextAlign.Center,
                                lineHeight = 22.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(bottom = 86.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(filteredFriends, key = { _, item -> item.jesrId }) { index, friend ->
                                val isOnline = neighbors.containsKey(friend.jesrId)
                                val conv = convMap[friend.jesrId]
                                FriendListRow(
                                    friend = friend,
                                    conversation = conv,
                                    isOnline = isOnline,
                                    isReducedMotion = isReducedMotion,
                                    entranceModifier = Modifier.staggeredIconEntrance(
                                        index = index.coerceAtMost(8),
                                        stepDelayMs = 45,
                                        startOffsetX = if (appLayoutDirection == LayoutDirection.Rtl) (-24).dp else 24.dp,
                                        startOffsetY = 0.dp,
                                        startScale = 0.94f,
                                        isReducedMotion = isReducedMotion
                                    ),
                                    onTap = {
                                        viewModel.openConversation(friend.jesrId, friend.displayName)
                                    },
                                    onOpenSettings = {
                                        viewModel.openFriendSettings(friend.jesrId)
                                    },
                                    onToggleMute = {
                                        viewModel.toggleMuteFriend(friend.jesrId)
                                    },
                                    onBlockOrUnblock = {
                                        if (friend.isBlocked) {
                                            viewModel.unblockFriend(friend.jesrId)
                                        } else {
                                            pendingBlockFriend = friend
                                        }
                                    },
                                    onRemove = {
                                        pendingRemoveFriend = friend
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Scrim behind the semi-square FAB menu
        AnimatedVisibility(
            visible = isFabMenuOpen,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(150))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if (palette.isDark) 0.44f else 0.24f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isFabMenuOpen = false }
                    .testTag("home_fab_menu_scrim")
            )
        }

        // Semi-square action menu blooming out of the (+) button
        AnimatedVisibility(
            visible = isFabMenuOpen,
            enter = if (isReducedMotion) {
                fadeIn(tween(120))
            } else {
                scaleIn(
                    animationSpec = spring(dampingRatio = 0.66f, stiffness = 360f),
                    initialScale = 0.22f,
                    transformOrigin = TransformOrigin(0.18f, 1.0f)
                ) + fadeIn(tween(170))
            },
            exit = if (isReducedMotion) {
                fadeOut(tween(100))
            } else {
                scaleOut(
                    animationSpec = spring(dampingRatio = 0.78f, stiffness = 420f),
                    targetScale = 0.22f,
                    transformOrigin = TransformOrigin(0.18f, 1.0f)
                ) + fadeOut(tween(140))
            },
            modifier = Modifier
                .align(AbsoluteAlignment.BottomLeft)
                .navigationBarsPadding()
                .absolutePadding(left = 24.dp, bottom = 94.dp)
        ) {
            HomeFabSquircleMenu(
                nearbyCount = nearbyCount,
                incomingCount = pendingRequests.size,
                sentCount = sentRequests.size,
                isReducedMotion = isReducedMotion,
                onAddById = {
                    isFabMenuOpen = false
                    viewModel.clearAccountLookup()
                    viewModel.navigateTo(Screen.ADD_BY_ID)
                },
                onNearbyPeople = {
                    isFabMenuOpen = false
                    viewModel.navigateTo(Screen.NEARBY_PEOPLE)
                },
                onIncomingRequests = {
                    isFabMenuOpen = false
                    viewModel.navigateToFriendRequests(FriendRequestsTab.INCOMING)
                },
                onSentRequests = {
                    isFabMenuOpen = false
                    viewModel.navigateToFriendRequests(FriendRequestsTab.SENT)
                }
            )
        }

        // Morphing (+) / (×) button: soft-cornered square when closed, spinning circle when open
        FloatingPlusActionButton(
            isReducedMotion = isReducedMotion,
            isExpanded = isFabMenuOpen,
            onClick = { isFabMenuOpen = !isFabMenuOpen },
            modifier = Modifier
                .align(AbsoluteAlignment.BottomLeft)
                .navigationBarsPadding()
                .absolutePadding(left = 24.dp, bottom = 24.dp)
                .absoluteOffset(x = fabShiftX, y = fabShiftY)
                .staggeredIconEntrance(
                    index = 3,
                    stepDelayMs = 75,
                    startOffsetX = (-20).dp,
                    startOffsetY = 22.dp,
                    startScale = 0.65f,
                    isReducedMotion = isReducedMotion
                )
        )
    }
}

@Composable
private fun HomeFabSquircleMenu(
    nearbyCount: Int,
    incomingCount: Int,
    sentCount: Int,
    isReducedMotion: Boolean,
    onAddById: () -> Unit,
    onNearbyPeople: () -> Unit,
    onIncomingRequests: () -> Unit,
    onSentRequests: () -> Unit
) {
    val palette = LocalJesrPalette.current
    val strings = LocalJesrStrings.current
    val menuShape = RoundedCornerShape(28.dp)

    Column(
        modifier = Modifier
            .width(264.dp)
            .shadow(
                elevation = 20.dp,
                shape = menuShape,
                ambientColor = StoneBackdropDeep.copy(alpha = 0.50f),
                spotColor = StoneBackdropDeep.copy(alpha = 0.60f)
            )
            .clip(menuShape)
            .background(palette.cardSurface)
            .border(1.dp, palette.border, menuShape)
            .testTag("home_fab_squircle_menu")
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = strings.addFriendsTitle,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.ExtraBold,
            color = palette.inkPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SquircleMenuActionTile(
                title = strings.hubAddByIdTitle,
                icon = Icons.Default.Badge,
                isReducedMotion = isReducedMotion,
                testTag = "hub_add_by_id",
                onClick = onAddById,
                modifier = Modifier.weight(1f)
            )
            SquircleMenuActionTile(
                title = strings.hubNearbyTitle,
                icon = Icons.Default.Radar,
                badgeCount = nearbyCount,
                isReducedMotion = isReducedMotion,
                testTag = "hub_nearby_people",
                onClick = onNearbyPeople,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SquircleMenuActionTile(
                title = strings.hubRequestsTitle,
                icon = Icons.Default.Inbox,
                badgeCount = incomingCount,
                isReducedMotion = isReducedMotion,
                testTag = "hub_friend_requests",
                onClick = onIncomingRequests,
                modifier = Modifier.weight(1f)
            )
            SquircleMenuActionTile(
                title = strings.hubSentTitle,
                icon = Icons.Default.Outbox,
                badgeCount = sentCount,
                isReducedMotion = isReducedMotion,
                testTag = "hub_sent_requests",
                onClick = onSentRequests,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SquircleMenuActionTile(
    title: String,
    icon: ImageVector,
    badgeCount: Int = 0,
    isReducedMotion: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalJesrPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !isReducedMotion) 0.94f else 1.0f,
        animationSpec = spring(dampingRatio = 0.56f, stiffness = 500f),
        label = "squircle_tile_scale"
    )
    val tileShape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .heightIn(min = 92.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(tileShape)
            .background(palette.elevatedSurface)
            .border(0.8.dp, palette.border, tileShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = palette.inkPrimary),
                onClick = onClick
            )
            .testTag(testTag)
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        if (badgeCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(palette.accent)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "$badgeCount",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureWhitePill
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(palette.pillSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = palette.iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = title,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = palette.inkPrimary,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FriendListRow(
    friend: FriendEntity,
    conversation: ConversationEntity?,
    isOnline: Boolean,
    isReducedMotion: Boolean,
    entranceModifier: Modifier = Modifier,
    onTap: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleMute: () -> Unit,
    onBlockOrUnblock: () -> Unit,
    onRemove: () -> Unit
) {
    val palette = LocalJesrPalette.current
    val strings = LocalJesrStrings.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var showContextMenu by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed && !isReducedMotion) 0.965f else 1.0f,
        animationSpec = spring(dampingRatio = 0.54f, stiffness = 520f),
        label = "friend_row_scale"
    )
    val pressOffset by animateDpAsState(
        targetValue = if (isPressed && !isReducedMotion) 2.5.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 520f),
        label = "friend_row_offset"
    )
    val rowBg by animateColorAsState(
        targetValue = if (isPressed) palette.cardSurface else palette.elevatedSurface,
        animationSpec = tween(100),
        label = "friend_row_bg"
    )

    val connectionStatusText = when {
        friend.isBlocked -> strings.statusBlocked
        friend.unblockedAwaitingResume -> strings.statusAwaitingResume
        isOnline -> strings.statusOnlineMesh
        else -> strings.statusOffline
    }
    val connectionStatusColor = when {
        friend.isBlocked -> TerracottaAccent
        friend.unblockedAwaitingResume -> palette.mutedText
        isOnline -> palette.accent
        else -> palette.mutedText
    }

    Box(modifier = entranceModifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 68.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = pressOffset.toPx() * 0.35f
                    translationY = pressOffset.toPx()
                }
                .drawBehind {
                    val radius = 22.dp.toPx()
                    val shadowFactor = if (isPressed) 0.45f else 1.0f
                    drawRoundRect(
                        color = StoneBackdropDeep.copy(alpha = 0.18f * shadowFactor),
                        topLeft = Offset(3.dp.toPx() * shadowFactor, 8.dp.toPx() * shadowFactor),
                        size = size,
                        cornerRadius = CornerRadius(radius, radius)
                    )
                    drawRoundRect(
                        color = StoneBackdropShadow.copy(alpha = 0.10f * shadowFactor),
                        topLeft = Offset(6.dp.toPx() * shadowFactor, 13.dp.toPx() * shadowFactor),
                        size = size,
                        cornerRadius = CornerRadius(radius, radius)
                    )
                }
                .shadow(
                    elevation = if (isPressed) 4.dp else 9.dp,
                    shape = RoundedCornerShape(22.dp),
                    ambientColor = StoneBackdropShadow.copy(alpha = 0.28f),
                    spotColor = StoneBackdropShadow.copy(alpha = 0.34f)
                )
                .clip(RoundedCornerShape(22.dp))
                .background(rowBg)
                .border(0.6.dp, palette.border, RoundedCornerShape(22.dp))
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = true, color = palette.inkPrimary),
                    onClick = onTap,
                    onLongClick = { showContextMenu = true }
                )
                .testTag("home_friend_${friend.jesrId}")
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            JesrAvatar(
                displayName = friend.displayName,
                avatarPath = friend.avatarUri,
                colorIndex = friend.colorIndex,
                size = 46.dp,
                isOnline = if (!friend.isBlocked && isOnline) true else null
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = friend.displayName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.inkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (friend.isMuted) {
                        Icon(
                            imageVector = Icons.Default.NotificationsOff,
                            contentDescription = strings.mutedCd,
                            tint = palette.mutedText,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = friend.jesrId,
                    fontSize = 11.sp,
                    color = palette.mutedText,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = connectionStatusText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = connectionStatusColor,
                    maxLines = 1
                )
            }

            if ((conversation?.unreadCount ?: 0) > 0 && !friend.isBlocked && !friend.unblockedAwaitingResume) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(palette.accent)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${conversation!!.unreadCount}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhitePill
                    )
                }
            }
        }

        // Section 1 Long-press Contextual Menu (exact 5 items)
        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false },
            modifier = Modifier
                .background(palette.cardSurface)
                .testTag("friend_context_menu_${friend.jesrId}")
        ) {
            DropdownMenuItem(
                text = { Text(strings.openConversation, color = palette.inkPrimary, fontWeight = FontWeight.SemiBold) },
                leadingIcon = {
                    Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = palette.inkPrimary)
                },
                onClick = {
                    showContextMenu = false
                    onTap()
                }
            )
            DropdownMenuItem(
                text = { Text(strings.friendSettingsMenu, color = palette.inkPrimary, fontWeight = FontWeight.SemiBold) },
                leadingIcon = {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = palette.inkPrimary)
                },
                onClick = {
                    showContextMenu = false
                    onOpenSettings()
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        if (friend.isMuted) strings.unmuteNotifications else strings.muteNotifications,
                        color = palette.inkPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                leadingIcon = {
                    Icon(
                        if (friend.isMuted) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                        contentDescription = null,
                        tint = palette.inkPrimary
                    )
                },
                onClick = {
                    showContextMenu = false
                    onToggleMute()
                }
            )
            HorizontalDivider(color = palette.border)
            DropdownMenuItem(
                text = {
                    Text(
                        if (friend.isBlocked) strings.unblockFriend else strings.blockFriend,
                        color = TerracottaAccent,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Block, contentDescription = null, tint = TerracottaAccent)
                },
                onClick = {
                    showContextMenu = false
                    onBlockOrUnblock()
                }
            )
            DropdownMenuItem(
                text = { Text(strings.removeFriend, color = TerracottaAccent, fontWeight = FontWeight.Bold) },
                leadingIcon = {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = TerracottaAccent)
                },
                onClick = {
                    showContextMenu = false
                    onRemove()
                }
            )
        }
    }
}
