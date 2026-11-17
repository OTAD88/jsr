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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.FriendEntity
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
import com.example.ui.theme.TerracottaAccent
import com.example.ui.viewmodel.FriendRequestsTab
import com.example.ui.viewmodel.JesrViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Friend Requests Screen (Spec Section 9):
 * - Fast swipe gestures via HorizontalPager + tapped segmented tabs between Incoming & Requests I Sent
 * - Incoming tab: Sender avatar, name, JISR ID, mutual friends count (when privacy permits),
 *   timestamp, Accept & Reject actions
 * - Requests I Sent tab: Recipient info, status, timestamp, Cancel action for pending requests
 */
@Composable
fun FriendRequestsScreen(
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
    val coroutineScope = rememberCoroutineScope()

    val isMeshActive by viewModel.isMeshActive.collectAsStateWithLifecycle()
    val initialTab by viewModel.requestsInitialTab.collectAsStateWithLifecycle()
    val pendingRequests by viewModel.pendingRequests.collectAsStateWithLifecycle()
    val sentRequests by viewModel.sentRequests.collectAsStateWithLifecycle()
    val globalMutualAllowed by viewModel.globalMutualFriendsAllowed.collectAsStateWithLifecycle()

    val pagerState = rememberPagerState(
        initialPage = if (initialTab == FriendRequestsTab.INCOMING) 0 else 1,
        pageCount = { 2 }
    )
    LaunchedEffect(initialTab) {
        val targetPage = if (initialTab == FriendRequestsTab.INCOMING) 0 else 1
        if (pagerState.currentPage != targetPage) {
            pagerState.scrollToPage(targetPage)
        }
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
                        title = strings.requestsTitle,
                        subtitle = strings.requestsSubtitle,
                        onBack = { viewModel.navigateBack() },
                        backTestTag = "requests_back_btn",
                        trailingContent = {
                            EditorialInteractiveIconButton(
                                icon = Icons.Default.Refresh,
                                contentDescription = strings.refreshRequestsCd,
                                containerColor = palette.pillSurface,
                                contentColor = palette.iconColor,
                                onClick = { viewModel.refreshFriendRequests() },
                                testTag = "requests_refresh_btn"
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Incoming / Requests I Sent Segmented Tabs synced with fast swipe HorizontalPager
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(50))
                            .background(palette.pillSurface)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TabPill(
                            title = strings.incomingTabCount(pendingRequests.size),
                            selected = pagerState.currentPage == 0,
                            modifier = Modifier.weight(1f),
                            testTag = "tab_incoming_requests",
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(0)
                                }
                            }
                        )
                        TabPill(
                            title = strings.sentTabCount(sentRequests.size),
                            selected = pagerState.currentPage == 1,
                            modifier = Modifier.weight(1f),
                            testTag = "tab_sent_requests",
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(1)
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("requests_horizontal_pager")
                    ) { page ->
                        val isIncomingPage = page == 0
                        val itemsList = if (isIncomingPage) pendingRequests else sentRequests
                        if (itemsList.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isIncomingPage) strings.noIncomingRequests else strings.noSentRequests,
                                    fontSize = 13.sp,
                                    color = palette.mutedText,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                itemsIndexed(itemsList, key = { _, entry -> entry.jesrId }) { idx, entry ->
                                    val showMutualCount = entry.peerMutualFriendsConsentEnabled &&
                                        (entry.mutualFriendsPermissionOverride ?: globalMutualAllowed)
                                    if (isIncomingPage) {
                                        IncomingRequestRow(
                                            entry = entry,
                                            showMutualCount = showMutualCount,
                                            entranceIndex = idx.coerceAtMost(8),
                                            slideOffsetX = slideOffsetX,
                                            isReducedMotion = isReducedMotion,
                                            onAccept = { viewModel.acceptFriendRequest(entry.jesrId) },
                                            onReject = { viewModel.rejectFriendRequest(entry.jesrId) }
                                        )
                                    } else {
                                        SentRequestRow(
                                            entry = entry,
                                            entranceIndex = idx.coerceAtMost(8),
                                            slideOffsetX = slideOffsetX,
                                            isReducedMotion = isReducedMotion,
                                            onCancel = { viewModel.cancelSentRequest(entry.jesrId) }
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
private fun TabPill(
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    testTag: String,
    onClick: () -> Unit
) {
    val palette = LocalJesrPalette.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) palette.inkPrimary else palette.pillSurface)
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) palette.elevatedSurface else palette.inkPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun IncomingRequestRow(
    entry: FriendEntity,
    showMutualCount: Boolean,
    entranceIndex: Int,
    slideOffsetX: Dp,
    isReducedMotion: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val palette = LocalJesrPalette.current
    val strings = LocalJesrStrings.current
    val locale = if (strings.isEnglish) Locale.ENGLISH else Locale("ar")
    val timeText = remember(entry.addedAt, strings.isEnglish) {
        SimpleDateFormat("yyyy/MM/dd • hh:mm a", locale).format(Date(entry.addedAt))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .staggeredIconEntrance(
                index = entranceIndex,
                stepDelayMs = 55,
                startOffsetX = slideOffsetX,
                startOffsetY = 0.dp,
                startScale = 0.94f,
                isReducedMotion = isReducedMotion
            )
            .clip(RoundedCornerShape(22.dp))
            .background(palette.elevatedSurface)
            .border(0.7.dp, palette.border, RoundedCornerShape(22.dp))
            .testTag("incoming_request_${entry.jesrId}")
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            JesrAvatar(
                displayName = entry.displayName,
                avatarPath = entry.avatarUri,
                colorIndex = entry.colorIndex,
                size = 46.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.displayName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.inkPrimary
                )
                Text(
                    text = entry.jesrId,
                    fontSize = 11.5.sp,
                    color = palette.mutedText
                )
                if (showMutualCount) {
                    Text(
                        text = strings.mutualFriendsCount(entry.mutualFriendsCount),
                        fontSize = 11.sp,
                        color = palette.accent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = timeText,
                    fontSize = 10.5.sp,
                    color = palette.mutedText
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = PureWhitePill),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("accept_request_${entry.jesrId}")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(5.dp))
                Text(strings.accept, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onReject,
                colors = ButtonDefaults.buttonColors(containerColor = palette.pillSurface, contentColor = TerracottaAccent),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("reject_request_${entry.jesrId}")
            ) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(5.dp))
                Text(strings.reject, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SentRequestRow(
    entry: FriendEntity,
    entranceIndex: Int,
    slideOffsetX: Dp,
    isReducedMotion: Boolean,
    onCancel: () -> Unit
) {
    val palette = LocalJesrPalette.current
    val strings = LocalJesrStrings.current
    val locale = if (strings.isEnglish) Locale.ENGLISH else Locale("ar")
    val timeText = remember(entry.addedAt, strings.isEnglish) {
        SimpleDateFormat("yyyy/MM/dd • hh:mm a", locale).format(Date(entry.addedAt))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .staggeredIconEntrance(
                index = entranceIndex,
                stepDelayMs = 55,
                startOffsetX = slideOffsetX,
                startOffsetY = 0.dp,
                startScale = 0.94f,
                isReducedMotion = isReducedMotion
            )
            .clip(RoundedCornerShape(22.dp))
            .background(palette.elevatedSurface)
            .border(0.7.dp, palette.border, RoundedCornerShape(22.dp))
            .testTag("sent_request_${entry.jesrId}")
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        JesrAvatar(
            displayName = entry.displayName,
            avatarPath = entry.avatarUri,
            colorIndex = entry.colorIndex,
            size = 44.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.displayName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = palette.inkPrimary
            )
            Text(
                text = entry.jesrId,
                fontSize = 11.5.sp,
                color = palette.mutedText
            )
            Text(
                text = strings.statusAwaitingReply(timeText),
                fontSize = 10.5.sp,
                color = palette.accent,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = onCancel,
            colors = ButtonDefaults.buttonColors(containerColor = palette.pillSurface, contentColor = TerracottaAccent),
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .height(48.dp)
                .testTag("cancel_sent_request_${entry.jesrId}")
        ) {
            Text(strings.cancel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
