package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.FriendStatus
import com.example.ui.components.EditorialFloatingCard
import com.example.ui.components.EditorialInteractiveIconButton
import com.example.ui.components.EditorialSubScreenHeader
import com.example.ui.components.EditorialSunlightBackdrop
import com.example.ui.components.JesrAvatar
import com.example.ui.components.rememberIsReducedMotionEnabled
import com.example.ui.components.staggeredIconEntrance
import com.example.ui.theme.LocalJesrPalette
import com.example.ui.theme.LocalJesrStrings
import com.example.ui.theme.PureWhitePill
import com.example.ui.viewmodel.JesrViewModel

/**
 * Nearby People Screen (Spec Section 8):
 * - Top-left: Back button
 * - Top area: Nearby People title + Manual Refresh button
 * - Displays ONLY users who explicitly enabled nearbyVisible
 * - Sorted strictly nearest to farthest (highest RSSI first)
 * - NEVER displays exact locations, exact distances, or search radius options
 */
@Composable
fun NearbyPeopleScreen(
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
    val neighbors by viewModel.neighbors.collectAsStateWithLifecycle()
    val allFriendEntries by viewModel.allFriendEntries.collectAsStateWithLifecycle()

    val entryStatusMap = remember(allFriendEntries) {
        allFriendEntries.associate { it.jesrId to it.status }
    }

    // Opt-in only, sorted nearest-to-farthest, NO default cap, NO radius filtering UI
    val eligibleNearbyUsers = remember(neighbors) {
        neighbors.values
            .filter { it.nearbyVisible }
            .sortedByDescending { it.rssi }
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
                        title = strings.nearbyTitle,
                        subtitle = strings.nearbySubtitle,
                        onBack = { viewModel.navigateBack() },
                        backTestTag = "nearby_back_btn",
                        trailingContent = {
                            EditorialInteractiveIconButton(
                                icon = Icons.Default.Refresh,
                                contentDescription = strings.manualRefreshCd,
                                containerColor = palette.pillSurface,
                                contentColor = palette.iconColor,
                                onClick = { viewModel.refreshNearbyPeople() },
                                testTag = "nearby_refresh_btn"
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (eligibleNearbyUsers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.staggeredIconEntrance(
                                    index = 0,
                                    stepDelayMs = 60,
                                    startOffsetX = slideOffsetX,
                                    startOffsetY = 8.dp,
                                    startScale = 0.94f,
                                    isReducedMotion = isReducedMotion
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(58.dp)
                                        .clip(CircleShape)
                                        .background(palette.pillSurface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Radar,
                                        contentDescription = null,
                                        tint = palette.mutedText,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = strings.noNearbyPeopleHint,
                                    fontSize = 13.sp,
                                    color = palette.mutedText,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(eligibleNearbyUsers, key = { _, peer -> peer.id }) { idx, peer ->
                                val status = entryStatusMap[peer.id]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .staggeredIconEntrance(
                                            index = idx.coerceAtMost(8),
                                            stepDelayMs = 55,
                                            startOffsetX = slideOffsetX,
                                            startOffsetY = 0.dp,
                                            startScale = 0.94f,
                                            isReducedMotion = isReducedMotion
                                        )
                                        .clip(RoundedCornerShape(22.dp))
                                        .background(palette.elevatedSurface)
                                        .border(0.7.dp, palette.border, RoundedCornerShape(22.dp))
                                        .testTag("nearby_user_${peer.id}")
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    JesrAvatar(
                                        displayName = peer.name,
                                        size = 46.dp,
                                        isOnline = true
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    // Strict privacy: Never display exact locations or distances
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = peer.name,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.inkPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = peer.id,
                                            fontSize = 11.5.sp,
                                            color = palette.mutedText,
                                            maxLines = 1
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    when (status) {
                                        FriendStatus.FRIEND -> {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .background(palette.pillSurface)
                                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                            ) {
                                                Text(
                                                    text = strings.friendBadge,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = palette.inkPrimary
                                                )
                                            }
                                        }
                                        FriendStatus.REQUEST_SENT -> {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .background(palette.accent.copy(alpha = 0.16f))
                                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                            ) {
                                                Text(
                                                    text = strings.sentBadge,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = palette.accent
                                                )
                                            }
                                        }
                                        else -> {
                                            Button(
                                                onClick = { viewModel.sendFriendRequest(peer.id, peer.name) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = palette.accent,
                                                    contentColor = PureWhitePill
                                                ),
                                                shape = RoundedCornerShape(50),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                                modifier = Modifier.testTag("nearby_send_request_${peer.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PersonAdd,
                                                    contentDescription = strings.sendFriendRequestBtn,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Text(
                                                    text = strings.sendRequestShort,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold
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
    }
}
