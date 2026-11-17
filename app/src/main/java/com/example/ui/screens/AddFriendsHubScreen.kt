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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Outbox
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.EditorialFloatingCard
import com.example.ui.components.EditorialSubScreenHeader
import com.example.ui.components.EditorialSunlightBackdrop
import com.example.ui.components.rememberIsReducedMotionEnabled
import com.example.ui.components.staggeredIconEntrance
import com.example.ui.theme.LocalJesrPalette
import com.example.ui.theme.LocalJesrStrings
import com.example.ui.theme.PureWhitePill
import com.example.ui.viewmodel.FriendRequestsTab
import com.example.ui.viewmodel.JesrViewModel
import com.example.ui.viewmodel.Screen

/**
 * Add Friends (+) Hub (Spec Section 2):
 * Opens when the bottom-left (+) button is pressed and presents:
 * A. Add by User ID
 * B. Nearby People
 * C. Friend Requests (Incoming)
 * D. Sent Requests (Sent)
 */
@Composable
fun AddFriendsHubScreen(
    viewModel: JesrViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val palette = LocalJesrPalette.current
    val strings = LocalJesrStrings.current
    val isMeshActive by viewModel.isMeshActive.collectAsStateWithLifecycle()
    val pendingRequests by viewModel.pendingRequests.collectAsStateWithLifecycle()
    val sentRequests by viewModel.sentRequests.collectAsStateWithLifecycle()
    val neighbors by viewModel.neighbors.collectAsStateWithLifecycle()
    val nearbyCount = neighbors.values.count { it.nearbyVisible }
    val isReducedMotion = rememberIsReducedMotionEnabled()

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
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    EditorialSubScreenHeader(
                        title = strings.addFriendsTitle,
                        subtitle = strings.addFriendsSubtitle,
                        onBack = { viewModel.navigateBack() },
                        backTestTag = "add_hub_back_btn"
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    HubOptionCard(
                        title = strings.hubAddByIdTitle,
                        subtitle = strings.hubAddByIdSubtitle,
                        icon = Icons.Default.Badge,
                        testTag = "hub_add_by_id",
                        entranceIndex = 0,
                        isReducedMotion = isReducedMotion,
                        onClick = {
                            viewModel.clearAccountLookup()
                            viewModel.navigateTo(Screen.ADD_BY_ID)
                        }
                    )

                    HubOptionCard(
                        title = strings.hubNearbyTitle,
                        subtitle = strings.hubNearbySubtitle,
                        icon = Icons.Default.Radar,
                        badgeCount = nearbyCount,
                        testTag = "hub_nearby_people",
                        entranceIndex = 1,
                        isReducedMotion = isReducedMotion,
                        onClick = { viewModel.navigateTo(Screen.NEARBY_PEOPLE) }
                    )

                    HubOptionCard(
                        title = strings.hubRequestsTitle,
                        subtitle = strings.hubRequestsSubtitle,
                        icon = Icons.Default.Inbox,
                        badgeCount = pendingRequests.size,
                        testTag = "hub_friend_requests",
                        entranceIndex = 2,
                        isReducedMotion = isReducedMotion,
                        onClick = { viewModel.navigateToFriendRequests(FriendRequestsTab.INCOMING) }
                    )

                    HubOptionCard(
                        title = strings.hubSentTitle,
                        subtitle = strings.hubSentSubtitle,
                        icon = Icons.Default.Outbox,
                        badgeCount = sentRequests.size,
                        testTag = "hub_sent_requests",
                        entranceIndex = 3,
                        isReducedMotion = isReducedMotion,
                        onClick = { viewModel.navigateToFriendRequests(FriendRequestsTab.SENT) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HubOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeCount: Int = 0,
    testTag: String,
    entranceIndex: Int = 0,
    isReducedMotion: Boolean = false,
    onClick: () -> Unit
) {
    val palette = LocalJesrPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .staggeredIconEntrance(
                index = entranceIndex,
                stepDelayMs = 70,
                startOffsetX = 22.dp,
                startOffsetY = 12.dp,
                startScale = 0.88f,
                isReducedMotion = isReducedMotion
            )
            .clip(RoundedCornerShape(22.dp))
            .background(palette.elevatedSurface)
            .border(0.7.dp, palette.border, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .staggeredIconEntrance(
                    index = entranceIndex,
                    stepDelayMs = 70,
                    initialDelayMs = 45,
                    startOffsetX = 0.dp,
                    startOffsetY = 8.dp,
                    startScale = 0.65f,
                    isReducedMotion = isReducedMotion
                )
                .clip(CircleShape)
                .background(palette.pillSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = palette.iconColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = palette.inkPrimary
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                fontSize = 11.5.sp,
                color = palette.mutedText,
                lineHeight = 16.sp
            )
        }

        if (badgeCount > 0) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(palette.accent)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "$badgeCount",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureWhitePill
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = palette.iconColor,
            modifier = Modifier.size(18.dp)
        )
    }
}
