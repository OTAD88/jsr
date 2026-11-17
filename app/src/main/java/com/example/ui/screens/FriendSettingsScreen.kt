package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.BlockFriendDialog
import com.example.ui.components.EditorialFloatingCard
import com.example.ui.components.EditorialSubScreenHeader
import com.example.ui.components.EditorialSunlightBackdrop
import com.example.ui.components.JesrAvatar
import com.example.ui.components.RemoveFriendMutualDialog
import com.example.ui.components.rememberIsReducedMotionEnabled
import com.example.ui.components.staggeredIconEntrance
import com.example.ui.theme.LocalJesrPalette
import com.example.ui.theme.LocalJesrStrings
import com.example.ui.theme.PureWhitePill
import com.example.ui.theme.TerracottaAccent
import com.example.ui.theme.TerracottaContainer
import com.example.ui.viewmodel.JesrViewModel

/**
 * Individual Friend Settings Screen (Spec Section 6):
 * - Accessible from Friend contextual menu OR Conversation settings
 * - Back returns directly to caller (Friends list OR exact Conversation)
 * - Two completely independent privacy sections:
 *   A. Message Distance Privacy: "Allow this friend to see the distance from which I sent my messages."
 *   B. Mutual Friends Privacy: "Allow this friend to see my mutual friends."
 * - Friend Management Actions: Mute Friend, Block Friend (with confirmation), Remove Friend (with confirmation)
 */
@Composable
fun FriendSettingsScreen(
    viewModel: JesrViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val palette = LocalJesrPalette.current
    val strings = LocalJesrStrings.current
    val appLayoutDirection = LocalLayoutDirection.current
    val isReducedMotion = rememberIsReducedMotionEnabled()
    val slideOffsetX = if (appLayoutDirection == LayoutDirection.Rtl) (-24).dp else 24.dp
    val isMeshActive by viewModel.isMeshActive.collectAsStateWithLifecycle()
    val selectedId by viewModel.selectedFriendSettingsId.collectAsStateWithLifecycle()
    val friends by viewModel.friends.collectAsStateWithLifecycle()
    val globalDistance by viewModel.globalDistanceAllowed.collectAsStateWithLifecycle()
    val globalMutual by viewModel.globalMutualFriendsAllowed.collectAsStateWithLifecycle()

    val friend = remember(friends, selectedId) {
        selectedId?.let { id -> friends.find { it.jesrId == id } }
    }

    var showBlockDialog by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf(false) }

    if (showBlockDialog && friend != null) {
        BlockFriendDialog(
            friendName = friend.displayName,
            onConfirm = {
                viewModel.blockFriend(friend.jesrId)
                showBlockDialog = false
            },
            onDismiss = { showBlockDialog = false }
        )
    }

    if (showRemoveDialog && friend != null) {
        RemoveFriendMutualDialog(
            friendName = friend.displayName,
            onConfirm = {
                showRemoveDialog = false
                viewModel.removeFriendMutually(friend.jesrId)
            },
            onDismiss = { showRemoveDialog = false }
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
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    EditorialSubScreenHeader(
                        title = strings.friendSettingsTitle,
                        subtitle = friend?.displayName ?: "",
                        onBack = { viewModel.navigateBack() },
                        backTestTag = "friend_settings_back_btn"
                    )

                    if (friend == null) {
                        Text(
                            text = strings.friendNotFound,
                            fontSize = 13.sp,
                            color = palette.mutedText
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .staggeredIconEntrance(
                                    index = 0,
                                    stepDelayMs = 55,
                                    startOffsetX = slideOffsetX,
                                    startOffsetY = 0.dp,
                                    startScale = 0.94f,
                                    isReducedMotion = isReducedMotion
                                )
                                .clip(RoundedCornerShape(20.dp))
                                .background(palette.elevatedSurface)
                                .border(0.7.dp, palette.border, RoundedCornerShape(20.dp))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            JesrAvatar(
                                displayName = friend.displayName,
                                avatarPath = friend.avatarUri,
                                colorIndex = friend.colorIndex,
                                size = 50.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = friend.displayName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.inkPrimary
                                )
                                Text(
                                    text = friend.jesrId,
                                    fontSize = 12.sp,
                                    color = palette.mutedText
                                )
                            }
                        }

                        // A. Message Distance Privacy (Independent)
                        val distanceEffective = friend.distancePermissionOverride ?: globalDistance
                        PrivacyControlCard(
                            sectionTitle = strings.msgDistancePrivacyTitle,
                            controlLabel = strings.msgDistancePrivacyLabel,
                            checked = distanceEffective,
                            testTag = "friend_distance_switch",
                            modifier = Modifier.staggeredIconEntrance(
                                index = 1,
                                stepDelayMs = 55,
                                startOffsetX = slideOffsetX,
                                startOffsetY = 0.dp,
                                startScale = 0.94f,
                                isReducedMotion = isReducedMotion
                            ),
                            onCheckedChange = { enabled ->
                                viewModel.setFriendDistanceOverride(friend.jesrId, enabled)
                            }
                        )

                        // B. Mutual Friends Privacy (Independent)
                        val mutualEffective = friend.mutualFriendsPermissionOverride ?: globalMutual
                        PrivacyControlCard(
                            sectionTitle = strings.mutualFriendsPrivacyTitle,
                            controlLabel = strings.mutualFriendsPrivacyLabel,
                            checked = mutualEffective,
                            testTag = "friend_mutual_switch",
                            modifier = Modifier.staggeredIconEntrance(
                                index = 2,
                                stepDelayMs = 55,
                                startOffsetX = slideOffsetX,
                                startOffsetY = 0.dp,
                                startScale = 0.94f,
                                isReducedMotion = isReducedMotion
                            ),
                            onCheckedChange = { enabled ->
                                viewModel.setFriendMutualFriendsOverride(friend.jesrId, enabled)
                            }
                        )

                        HorizontalDivider(color = palette.border)

                        Text(
                            text = strings.manageFriendTitle,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.inkPrimary
                        )

                        FriendActionRow(
                            title = if (friend.isMuted) strings.unmuteNotifications else strings.muteFriend,
                            icon = if (friend.isMuted) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                            tint = palette.inkPrimary,
                            iconTint = palette.iconColor,
                            container = palette.elevatedSurface,
                            testTag = "friend_settings_mute_btn",
                            modifier = Modifier.staggeredIconEntrance(
                                index = 3,
                                stepDelayMs = 55,
                                startOffsetX = slideOffsetX,
                                startOffsetY = 0.dp,
                                startScale = 0.94f,
                                isReducedMotion = isReducedMotion
                            ),
                            onClick = { viewModel.toggleMuteFriend(friend.jesrId) }
                        )

                        FriendActionRow(
                            title = if (friend.isBlocked) strings.unblockFriend else strings.blockFriend,
                            icon = Icons.Default.Block,
                            tint = TerracottaAccent,
                            iconTint = TerracottaAccent,
                            container = TerracottaContainer,
                            testTag = "friend_settings_block_btn",
                            modifier = Modifier.staggeredIconEntrance(
                                index = 4,
                                stepDelayMs = 55,
                                startOffsetX = slideOffsetX,
                                startOffsetY = 0.dp,
                                startScale = 0.94f,
                                isReducedMotion = isReducedMotion
                            ),
                            onClick = {
                                if (friend.isBlocked) {
                                    viewModel.unblockFriend(friend.jesrId)
                                } else {
                                    showBlockDialog = true
                                }
                            }
                        )

                        FriendActionRow(
                            title = strings.removeFriend,
                            icon = Icons.Default.DeleteOutline,
                            tint = TerracottaAccent,
                            iconTint = TerracottaAccent,
                            container = TerracottaContainer,
                            testTag = "friend_settings_remove_btn",
                            modifier = Modifier.staggeredIconEntrance(
                                index = 5,
                                stepDelayMs = 55,
                                startOffsetX = slideOffsetX,
                                startOffsetY = 0.dp,
                                startScale = 0.94f,
                                isReducedMotion = isReducedMotion
                            ),
                            onClick = { showRemoveDialog = true }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacyControlCard(
    sectionTitle: String,
    controlLabel: String,
    checked: Boolean,
    testTag: String,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit
) {
    val palette = LocalJesrPalette.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(palette.elevatedSurface)
            .border(0.7.dp, palette.border, RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Text(
            text = sectionTitle,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = palette.inkPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = controlLabel,
                fontSize = 12.sp,
                color = palette.mutedText,
                lineHeight = 18.sp,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp)
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.testTag(testTag),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PureWhitePill,
                    checkedTrackColor = palette.accent,
                    uncheckedThumbColor = palette.inkPrimary,
                    uncheckedTrackColor = palette.pillSurface
                )
            )
        }
    }
}

@Composable
private fun FriendActionRow(
    title: String,
    icon: ImageVector,
    tint: Color,
    iconTint: Color,
    container: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = LocalJesrPalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(container)
            .border(0.6.dp, palette.border, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = tint)
    }
}
