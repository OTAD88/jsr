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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.DeleteAllIncomingRequestsDialog
import com.example.ui.components.EditorialFloatingCard
import com.example.ui.components.EditorialSubScreenHeader
import com.example.ui.components.EditorialSunlightBackdrop
import com.example.ui.components.rememberIsReducedMotionEnabled
import com.example.ui.components.staggeredIconEntrance
import com.example.ui.theme.LocalJesrPalette
import com.example.ui.theme.LocalJesrStrings
import com.example.ui.theme.PureWhitePill
import com.example.ui.theme.TerracottaAccent
import com.example.ui.theme.TerracottaContainer
import com.example.ui.viewmodel.JesrViewModel

/**
 * Friend Request Privacy Settings (Spec Section 10):
 * - Separate Mute Requests system: Global + Individual + Exceptions in both directions
 *   (suppresses notifications/indicators without blocking receipt)
 * - Separate Block Requests system: Global + Individual + Exceptions in both directions
 *   (silently drops incoming requests without notifying sender; disabling restores future receipt)
 * - Delete All Incoming Requests action with confirmation
 */
@Composable
fun RequestPrivacyScreen(
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
    val globalMute by viewModel.globalMuteRequests.collectAsStateWithLifecycle()
    val globalBlock by viewModel.globalBlockRequests.collectAsStateWithLifecycle()
    val rules by viewModel.requestPrivacyRules.collectAsStateWithLifecycle()
    val allEntries by viewModel.allFriendEntries.collectAsStateWithLifecycle()
    val pendingRequests by viewModel.pendingRequests.collectAsStateWithLifecycle()

    var customUserId by remember { mutableStateOf("") }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    // Combine known users & explicit ID rules
    val knownUserIds = remember(allEntries, rules) {
        (allEntries.map { it.jesrId } + rules.keys).distinct()
    }
    val nameById = remember(allEntries) {
        allEntries.associate { it.jesrId to it.displayName }
    }

    if (showDeleteAllDialog) {
        DeleteAllIncomingRequestsDialog(
            count = pendingRequests.size,
            onConfirm = {
                viewModel.deleteAllIncomingRequests()
                showDeleteAllDialog = false
            },
            onDismiss = { showDeleteAllDialog = false }
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
                        title = strings.reqPrivacyScreenTitle,
                        subtitle = strings.reqPrivacyScreenSubtitle,
                        onBack = { viewModel.navigateBack() },
                        backTestTag = "request_privacy_back_btn"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // A. Separate Mute Requests Section
                        item {
                            Column(
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
                                    .padding(14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsOff,
                                        contentDescription = null,
                                        tint = palette.iconColor,
                                        modifier = Modifier.size(19.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = strings.muteReqSystemTitle,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.inkPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = strings.muteReqSystemSubtitle,
                                        fontSize = 11.5.sp,
                                        color = palette.mutedText,
                                        lineHeight = 17.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Switch(
                                        checked = globalMute,
                                        onCheckedChange = { viewModel.setGlobalMuteRequests(it) },
                                        modifier = Modifier.testTag("req_global_mute_switch"),
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

                        // B. Separate Block Requests Section
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .staggeredIconEntrance(
                                        index = 1,
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
                                    Icon(
                                        imageVector = Icons.Default.Block,
                                        contentDescription = null,
                                        tint = TerracottaAccent,
                                        modifier = Modifier.size(19.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = strings.blockReqSystemTitle,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.inkPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = strings.blockReqSystemSubtitle,
                                        fontSize = 11.5.sp,
                                        color = palette.mutedText,
                                        lineHeight = 17.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Switch(
                                        checked = globalBlock,
                                        onCheckedChange = { viewModel.setGlobalBlockRequests(it) },
                                        modifier = Modifier.testTag("req_global_block_switch"),
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = PureWhitePill,
                                            checkedTrackColor = TerracottaAccent,
                                            uncheckedThumbColor = palette.inkPrimary,
                                            uncheckedTrackColor = palette.pillSurface
                                        )
                                    )
                                }
                            }
                        }

                        // Add exception by JISR ID
                        item {
                            Column(
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
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = strings.addIdExceptionTitle,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.inkPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = customUserId,
                                        onValueChange = { customUserId = it.uppercase() },
                                        placeholder = { Text("JESR-XXXX-YYYY", fontSize = 12.sp) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = palette.inkPrimary,
                                            unfocusedTextColor = palette.inkPrimary,
                                            focusedContainerColor = palette.elevatedSurface,
                                            unfocusedContainerColor = palette.elevatedSurface
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    Button(
                                        onClick = {
                                            val clean = customUserId.trim().uppercase()
                                            if (clean.isNotEmpty()) {
                                                // Create a rule toggling opposite of global mute as initial exception
                                                viewModel.setUserRequestMuteRule(clean, !globalMute)
                                                customUserId = ""
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = palette.inkPrimary, contentColor = PureWhitePill),
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier.height(48.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = strings.addExceptionCd, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }

                        if (knownUserIds.isNotEmpty()) {
                            item {
                                Text(
                                    text = strings.individualRulesHeader,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.inkPrimary
                                )
                            }

                            itemsIndexed(knownUserIds, key = { _, id -> id }) { idx, userId ->
                                val rule = rules[userId]
                                val effectiveMute = rule?.muteOverride ?: globalMute
                                val effectiveBlock = rule?.blockOverride ?: globalBlock
                                val displayName = nameById[userId] ?: userId

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
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(palette.elevatedSurface)
                                        .border(0.7.dp, palette.border, RoundedCornerShape(18.dp))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = displayName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.inkPrimary
                                    )
                                    if (displayName != userId) {
                                        Text(text = userId, fontSize = 11.sp, color = palette.mutedText)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(strings.muteUserRequests, fontSize = 12.5.sp, color = palette.inkSecondary)
                                        Switch(
                                            checked = effectiveMute,
                                            onCheckedChange = { viewModel.setUserRequestMuteRule(userId, it) },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = PureWhitePill,
                                                checkedTrackColor = palette.accent,
                                                uncheckedThumbColor = palette.inkPrimary,
                                                uncheckedTrackColor = palette.pillSurface
                                            )
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(strings.blockUserRequestsSilently, fontSize = 12.5.sp, color = TerracottaAccent)
                                        Switch(
                                            checked = effectiveBlock,
                                            onCheckedChange = { viewModel.setUserRequestBlockRule(userId, it) },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = PureWhitePill,
                                                checkedTrackColor = TerracottaAccent,
                                                uncheckedThumbColor = palette.inkPrimary,
                                                uncheckedTrackColor = palette.pillSurface
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            HorizontalDivider(color = palette.border)
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = { showDeleteAllDialog = true },
                                enabled = pendingRequests.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TerracottaContainer,
                                    contentColor = TerracottaAccent,
                                    disabledContainerColor = palette.pillSurface,
                                    disabledContentColor = palette.mutedText
                                ),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("req_delete_all_incoming_btn")
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = strings.deleteAllIncomingBtn(pendingRequests.size),
                                    fontSize = 13.5.sp,
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
