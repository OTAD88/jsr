package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.EditorialFloatingCard
import com.example.ui.components.EditorialSubScreenHeader
import com.example.ui.components.EditorialSunlightBackdrop
import com.example.ui.components.JesrAvatar
import com.example.ui.components.rememberIsReducedMotionEnabled
import com.example.ui.components.staggeredIconEntrance
import com.example.ui.theme.LocalJesrPalette
import com.example.ui.theme.LocalJesrStrings
import com.example.ui.theme.PureWhitePill
import com.example.ui.viewmodel.JesrViewModel
import com.example.ui.viewmodel.Screen

/**
 * Global Privacy Settings Screen (Spec Section 7):
 * - Accessible from Side Navigation Menu
 * - Top: Two global privacy controls (disabled by default):
 *   1. Allow all friends to see the sending distance of my messages.
 *   2. Allow all friends to see my mutual friends.
 * - Below: Friends list where every friend has two separate controls overriding global in BOTH directions.
 * - Also provides entry to Friend Request Privacy Settings (Section 10).
 */
@Composable
fun GlobalPrivacyScreen(
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
    val globalDistance by viewModel.globalDistanceAllowed.collectAsStateWithLifecycle()
    val globalMutual by viewModel.globalMutualFriendsAllowed.collectAsStateWithLifecycle()
    val friends by viewModel.friends.collectAsStateWithLifecycle()

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
        ) {
            EditorialFloatingCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                backgroundColor = palette.cardSurface,
                contentPadding = 18.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    EditorialSubScreenHeader(
                        title = strings.globalPrivacyTitle,
                        subtitle = strings.globalPrivacySubtitle,
                        onBack = { viewModel.navigateBack() },
                        backTestTag = "global_privacy_back_btn"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            // Top Global Control 1: Message distance
                            GlobalSwitchTile(
                                title = strings.globalDistanceTitle,
                                subtitle = strings.globalDistanceSubtitle,
                                checked = globalDistance,
                                testTag = "global_distance_switch",
                                modifier = Modifier.staggeredIconEntrance(
                                    index = 0,
                                    stepDelayMs = 55,
                                    startOffsetX = slideOffsetX,
                                    startOffsetY = 0.dp,
                                    startScale = 0.94f,
                                    isReducedMotion = isReducedMotion
                                ),
                                onCheckedChange = { viewModel.setGlobalDistanceAllowed(it) }
                            )
                        }

                        item {
                            // Top Global Control 2: Mutual friends
                            GlobalSwitchTile(
                                title = strings.globalMutualTitle,
                                subtitle = strings.globalMutualSubtitle,
                                checked = globalMutual,
                                testTag = "global_mutual_switch",
                                modifier = Modifier.staggeredIconEntrance(
                                    index = 1,
                                    stepDelayMs = 55,
                                    startOffsetX = slideOffsetX,
                                    startOffsetY = 0.dp,
                                    startScale = 0.94f,
                                    isReducedMotion = isReducedMotion
                                ),
                                onCheckedChange = { viewModel.setGlobalMutualFriendsAllowed(it) }
                            )
                        }

                        item {
                            // Entry to Section 10: Friend Request Privacy Settings
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .staggeredIconEntrance(
                                        index = 2,
                                        stepDelayMs = 55,
                                        startOffsetX = slideOffsetX,
                                        startOffsetY = 0.dp,
                                        startScale = 0.94f,
                                        isReducedMotion = isReducedMotion
                                    )
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(palette.cardSurfaceWarm)
                                    .border(0.7.dp, palette.border, RoundedCornerShape(20.dp))
                                    .clickable { viewModel.navigateTo(Screen.REQUEST_PRIVACY) }
                                    .testTag("open_request_privacy_btn")
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = palette.iconColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = strings.reqPrivacyTitle,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.inkPrimary
                                        )
                                        Text(
                                            text = strings.reqPrivacySubtitle,
                                            fontSize = 11.sp,
                                            color = palette.mutedText
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = palette.iconColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        item {
                            HorizontalDivider(color = palette.border)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = strings.friendExceptionsHeader,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.inkPrimary
                            )
                        }

                        if (friends.isEmpty()) {
                            item {
                                Text(
                                    text = strings.noFriendsForExceptions,
                                    fontSize = 12.5.sp,
                                    color = palette.mutedText
                                )
                            }
                        } else {
                            itemsIndexed(friends, key = { _, f -> f.jesrId }) { idx, friend ->
                                val distEffective = friend.distancePermissionOverride ?: globalDistance
                                val mutualEffective = friend.mutualFriendsPermissionOverride ?: globalMutual

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .staggeredIconEntrance(
                                            index = (idx + 3).coerceAtMost(8),
                                            stepDelayMs = 55,
                                            startOffsetX = slideOffsetX,
                                            startOffsetY = 0.dp,
                                            startScale = 0.94f,
                                            isReducedMotion = isReducedMotion
                                        )
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(palette.elevatedSurface)
                                        .border(0.7.dp, palette.border, RoundedCornerShape(20.dp))
                                        .padding(14.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        JesrAvatar(
                                            displayName = friend.displayName,
                                            avatarPath = friend.avatarUri,
                                            colorIndex = friend.colorIndex,
                                            size = 38.dp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = friend.displayName,
                                                fontSize = 14.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = palette.inkPrimary
                                            )
                                            Text(
                                                text = friend.jesrId,
                                                fontSize = 11.sp,
                                                color = palette.mutedText
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Control 1: Message distance visibility
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = strings.seeMsgDistanceLabel,
                                            fontSize = 12.5.sp,
                                            color = palette.inkSecondary
                                        )
                                        Switch(
                                            checked = distEffective,
                                            onCheckedChange = { enabled ->
                                                viewModel.setFriendDistanceOverride(friend.jesrId, enabled)
                                            },
                                            modifier = Modifier.testTag("gp_dist_${friend.jesrId}"),
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = PureWhitePill,
                                                checkedTrackColor = palette.accent,
                                                uncheckedThumbColor = palette.inkPrimary,
                                                uncheckedTrackColor = palette.pillSurface
                                            )
                                        )
                                    }

                                    // Control 2: Mutual friends visibility
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = strings.seeMutualFriendsLabel,
                                            fontSize = 12.5.sp,
                                            color = palette.inkSecondary
                                        )
                                        Switch(
                                            checked = mutualEffective,
                                            onCheckedChange = { enabled ->
                                                viewModel.setFriendMutualFriendsOverride(friend.jesrId, enabled)
                                            },
                                            modifier = Modifier.testTag("gp_mutual_${friend.jesrId}"),
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
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlobalSwitchTile(
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit
) {
    val palette = LocalJesrPalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(palette.elevatedSurface)
            .border(0.7.dp, palette.border, RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 10.dp)
        ) {
            Text(
                text = title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = palette.inkPrimary
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = palette.mutedText,
                lineHeight = 16.sp
            )
        }
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
