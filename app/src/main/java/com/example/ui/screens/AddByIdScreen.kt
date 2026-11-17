package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.data.model.FriendStatus
import com.example.data.repository.AccountLookupResult
import com.example.ui.components.EditorialFloatingCard
import com.example.ui.components.EditorialSubScreenHeader
import com.example.ui.components.EditorialSunlightBackdrop
import com.example.ui.components.JesrAvatar
import com.example.ui.components.SolidBlackPillButton
import com.example.ui.components.rememberIsReducedMotionEnabled
import com.example.ui.components.staggeredIconEntrance
import com.example.ui.theme.LocalJesrPalette
import com.example.ui.theme.LocalJesrStrings
import com.example.ui.theme.TerracottaAccent
import com.example.ui.theme.TerracottaContainer
import com.example.ui.viewmodel.JesrViewModel

/**
 * Add by User ID Screen (Spec Section 2A):
 * 1. Open screen with ID input field
 * 2. Allow entering another person's unique JISR ID
 * 3. Display matching account info if ID exists
 * 4. Display a Send Friend Request button
 * 5. Show an appropriate message if ID does not exist
 */
@Composable
fun AddByIdScreen(
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
    val lookupResult by viewModel.accountLookupResult.collectAsStateWithLifecycle()

    var queryId by remember { mutableStateOf("") }
    var requestSentForId by remember { mutableStateOf<String?>(null) }

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
                Column {
                    EditorialSubScreenHeader(
                        title = strings.addByIdTitle,
                        subtitle = strings.addByIdSubtitle,
                        onBack = { viewModel.navigateBack() },
                        backTestTag = "add_by_id_back_btn"
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    OutlinedTextField(
                        value = queryId,
                        onValueChange = {
                            queryId = it.uppercase()
                            requestSentForId = null
                            if (it.isBlank()) viewModel.clearAccountLookup()
                        },
                        label = { Text(strings.addByIdInputLabel, fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .staggeredIconEntrance(
                                index = 0,
                                stepDelayMs = 60,
                                startOffsetX = slideOffsetX,
                                startOffsetY = 0.dp,
                                startScale = 0.94f,
                                isReducedMotion = isReducedMotion
                            )
                            .testTag("add_by_id_input"),
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

                    Spacer(modifier = Modifier.height(12.dp))

                    SolidBlackPillButton(
                        text = strings.lookupAccountBtn,
                        icon = Icons.Default.PersonSearch,
                        enabled = queryId.isNotBlank(),
                        containerColor = if (palette.isDark) palette.accent else palette.inkPrimary,
                        onClick = { viewModel.lookupAccountById(queryId) },
                        testTag = "add_by_id_lookup_btn",
                        modifier = Modifier.staggeredIconEntrance(
                            index = 1,
                            stepDelayMs = 60,
                            startOffsetX = slideOffsetX,
                            startOffsetY = 0.dp,
                            startScale = 0.94f,
                            isReducedMotion = isReducedMotion
                        )
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    when (val result = lookupResult) {
                        AccountLookupResult.Idle -> Unit

                        is AccountLookupResult.Found -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .staggeredIconEntrance(
                                        index = 0,
                                        stepDelayMs = 40,
                                        startOffsetX = slideOffsetX,
                                        startOffsetY = 0.dp,
                                        startScale = 0.94f,
                                        isReducedMotion = isReducedMotion
                                    )
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(palette.elevatedSurface)
                                    .border(0.8.dp, palette.border, RoundedCornerShape(22.dp))
                                    .padding(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    JesrAvatar(
                                        displayName = result.displayName,
                                        size = 52.dp,
                                        isOnline = if (result.isNearby) true else null
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = result.displayName,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.inkPrimary
                                        )
                                        Text(
                                            text = result.jesrId,
                                            fontSize = 12.sp,
                                            color = palette.mutedText
                                        )
                                        if (result.isNearby) {
                                            Text(
                                                text = strings.availableLocally,
                                                fontSize = 11.sp,
                                                color = palette.accent,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                val alreadySent = requestSentForId == result.jesrId ||
                                    result.status == FriendStatus.REQUEST_SENT
                                val alreadyFriend = result.status == FriendStatus.FRIEND

                                when {
                                    alreadyFriend -> {
                                        SolidBlackPillButton(
                                            text = strings.alreadyFriends,
                                            icon = Icons.Default.Check,
                                            enabled = false,
                                            onClick = {},
                                            testTag = "add_by_id_already_friend"
                                        )
                                    }
                                    alreadySent -> {
                                        SolidBlackPillButton(
                                            text = strings.friendRequestSentCheck,
                                            icon = Icons.Default.Check,
                                            enabled = false,
                                            onClick = {},
                                            testTag = "add_by_id_request_sent"
                                        )
                                    }
                                    else -> {
                                        SolidBlackPillButton(
                                            text = strings.sendFriendRequestBtn,
                                            icon = Icons.Default.PersonAdd,
                                            containerColor = palette.accent,
                                            onClick = {
                                                viewModel.sendFriendRequest(result.jesrId, result.displayName)
                                                requestSentForId = result.jesrId
                                            },
                                            testTag = "add_by_id_send_request_btn"
                                        )
                                    }
                                }
                            }
                        }

                        is AccountLookupResult.NotFound -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .staggeredIconEntrance(
                                        index = 0,
                                        stepDelayMs = 40,
                                        startOffsetX = slideOffsetX,
                                        startOffsetY = 0.dp,
                                        startScale = 0.94f,
                                        isReducedMotion = isReducedMotion
                                    )
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(TerracottaContainer)
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SearchOff,
                                    contentDescription = null,
                                    tint = TerracottaAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = strings.accountNotFound(result.queriedId),
                                    fontSize = 12.5.sp,
                                    color = TerracottaAccent,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
