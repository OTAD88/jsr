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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Sensors
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppLanguage
import com.example.data.model.AppLockMethod
import com.example.ui.components.AppLockSetupDialog
import com.example.ui.components.EditorialFloatingCard
import com.example.ui.components.EditorialSubScreenHeader
import com.example.ui.components.EditorialSunlightBackdrop
import com.example.ui.components.rememberIsReducedMotionEnabled
import com.example.ui.components.staggeredIconEntrance
import com.example.ui.theme.InkBlack
import com.example.ui.theme.LocalJesrPalette
import com.example.ui.theme.LocalJesrStrings
import com.example.ui.theme.PureWhitePill
import com.example.ui.viewmodel.JesrViewModel
import com.example.ui.viewmodel.Screen

/**
 * General Application Settings Screen:
 * Includes App Language, Appearance entry, App Lock (Biometric/PIN/Password/Pattern), and Mesh & Discovery toggles.
 */
@Composable
fun SettingsScreen(
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

    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val isMeshActive by viewModel.isMeshActive.collectAsStateWithLifecycle()
    val isNearbyVisible by viewModel.isNearbyVisibilityEnabled.collectAsStateWithLifecycle()
    val appLockMethod by viewModel.appLockMethod.collectAsStateWithLifecycle()
    var showAppLockDialog by remember { mutableStateOf(false) }

    if (showAppLockDialog) {
        AppLockSetupDialog(
            currentMethod = appLockMethod,
            onSelectNone = { viewModel.disableAppLock() },
            onSetBiometric = { viewModel.setAppLockBiometric() },
            onSetPin = { pin -> viewModel.setAppLockPin(pin) },
            onSetPassword = { pass -> viewModel.setAppLockPassword(pass) },
            onSetPattern = { dots -> viewModel.setAppLockPattern(dots) },
            onDismiss = { showAppLockDialog = false }
        )
    }

    val activeLockLabel = when (appLockMethod) {
        AppLockMethod.NONE -> strings.lockMethodNone
        AppLockMethod.BIOMETRIC -> strings.lockMethodBiometric
        AppLockMethod.PIN -> strings.lockMethodPin
        AppLockMethod.PASSWORD -> strings.lockMethodPassword
        AppLockMethod.PATTERN -> strings.lockMethodPattern
    }

    Box(modifier = modifier.fillMaxSize()) {
        EditorialSunlightBackdrop(
            modifier = Modifier.fillMaxSize(),
            isMeshActive = isMeshActive
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                EditorialFloatingCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = palette.cardSurface,
                    contentPadding = 20.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        EditorialSubScreenHeader(
                            title = strings.settingsTitle,
                            subtitle = strings.settingsSubtitle,
                            onBack = { viewModel.navigateBack() },
                            backTestTag = "settings_back_btn"
                        )

                        // App Language Selector (Arabic / English)
                        Column(
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
                                .clip(RoundedCornerShape(20.dp))
                                .background(palette.elevatedSurface)
                                .border(0.7.dp, palette.border, RoundedCornerShape(20.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = palette.iconColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = strings.languageSectionTitle,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.inkPrimary
                                    )
                                    Text(
                                        text = strings.languageSectionSubtitle,
                                        fontSize = 11.sp,
                                        color = palette.mutedText
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                LanguageOptionPill(
                                    label = strings.arabicLabel,
                                    selected = appLanguage == AppLanguage.AR,
                                    testTag = "settings_lang_ar",
                                    modifier = Modifier.weight(1f),
                                    onClick = { viewModel.setAppLanguage(AppLanguage.AR) }
                                )
                                LanguageOptionPill(
                                    label = strings.englishLabel,
                                    selected = appLanguage == AppLanguage.EN,
                                    testTag = "settings_lang_en",
                                    modifier = Modifier.weight(1f),
                                    onClick = { viewModel.setAppLanguage(AppLanguage.EN) }
                                )
                            }
                        }

                        // Appearance Navigation Entry (moved inside Settings)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .staggeredIconEntrance(
                                    index = 1,
                                    stepDelayMs = 60,
                                    startOffsetX = slideOffsetX,
                                    startOffsetY = 0.dp,
                                    startScale = 0.94f,
                                    isReducedMotion = isReducedMotion
                                )
                                .clip(RoundedCornerShape(20.dp))
                                .background(palette.cardSurfaceWarm)
                                .border(0.7.dp, palette.border, RoundedCornerShape(20.dp))
                                .clickable { viewModel.navigateTo(Screen.APPEARANCE) }
                                .testTag("settings_appearance_btn")
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = strings.appearanceSettingsEntryTitle,
                                    tint = palette.iconColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.padding(end = 8.dp)) {
                                    Text(
                                        text = strings.appearanceSettingsEntryTitle,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.inkPrimary
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = strings.appearanceSettingsEntrySubtitle,
                                        fontSize = 11.sp,
                                        color = palette.mutedText,
                                        lineHeight = 16.sp
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

                        // App Lock Security Card (Biometric, PIN, Password, Pattern)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .staggeredIconEntrance(
                                    index = 2,
                                    stepDelayMs = 60,
                                    startOffsetX = slideOffsetX,
                                    startOffsetY = 0.dp,
                                    startScale = 0.94f,
                                    isReducedMotion = isReducedMotion
                                )
                                .clip(RoundedCornerShape(20.dp))
                                .background(palette.cardSurfaceWarm)
                                .border(
                                    width = if (appLockMethod != AppLockMethod.NONE) 1.1.dp else 0.7.dp,
                                    color = if (appLockMethod != AppLockMethod.NONE) palette.accent else palette.border,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { showAppLockDialog = true }
                                .testTag("settings_app_lock_btn")
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = strings.appLockTitle,
                                        tint = if (appLockMethod != AppLockMethod.NONE) palette.accent else palette.iconColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.padding(end = 8.dp)) {
                                        Text(
                                            text = strings.appLockTitle,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.inkPrimary
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = strings.appLockSubtitle,
                                            fontSize = 11.sp,
                                            color = palette.mutedText,
                                            lineHeight = 16.sp
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

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            if (appLockMethod != AppLockMethod.NONE) {
                                                palette.accent.copy(alpha = 0.16f)
                                            } else {
                                                palette.pillSurface
                                            }
                                        )
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = activeLockLabel,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (appLockMethod != AppLockMethod.NONE) palette.accent else palette.mutedText
                                    )
                                }

                                if (appLockMethod != AppLockMethod.NONE) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(palette.inkPrimary)
                                            .clickable { viewModel.lockAppNow() }
                                            .testTag("settings_lock_now_btn")
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = strings.lockNowBtn,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.elevatedSurface
                                        )
                                    }
                                }
                            }
                        }

                        // Local Mesh Radio Switch
                        SettingsToggleCard(
                            title = strings.meshSwitchTitle,
                            subtitle = strings.meshSwitchSubtitle,
                            icon = Icons.Default.Sensors,
                            checked = isMeshActive,
                            testTag = "settings_mesh_switch",
                            modifier = Modifier.staggeredIconEntrance(
                                index = 3,
                                stepDelayMs = 60,
                                startOffsetX = slideOffsetX,
                                startOffsetY = 0.dp,
                                startScale = 0.94f,
                                isReducedMotion = isReducedMotion
                            ),
                            onCheckedChange = { viewModel.toggleMesh() }
                        )

                        // Explicit Opt-in Nearby Visibility Switch
                        SettingsToggleCard(
                            title = strings.nearbyVisTitle,
                            subtitle = strings.nearbyVisSubtitle,
                            icon = Icons.Default.Radar,
                            checked = isNearbyVisible,
                            testTag = "settings_nearby_visible_switch",
                            modifier = Modifier.staggeredIconEntrance(
                                index = 4,
                                stepDelayMs = 60,
                                startOffsetX = slideOffsetX,
                                startOffsetY = 0.dp,
                                startScale = 0.94f,
                                isReducedMotion = isReducedMotion
                            ),
                            onCheckedChange = { viewModel.setNearbyVisibilityEnabled(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageOptionPill(
    label: String,
    selected: Boolean,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = LocalJesrPalette.current
    val bgColor = if (selected) palette.accent else palette.pillSurface
    val textColor = if (selected) {
        if (bgColor.luminance() > 0.55f) InkBlack else PureWhitePill
    } else {
        palette.inkPrimary
    }
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(
                width = if (selected) 1.2.dp else 0.7.dp,
                color = if (selected) palette.accent else palette.border,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SettingsToggleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
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
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(imageVector = icon, contentDescription = null, tint = palette.iconColor, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.padding(end = 8.dp)) {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = palette.inkPrimary)
                Spacer(modifier = Modifier.height(3.dp))
                Text(text = subtitle, fontSize = 11.sp, color = palette.mutedText, lineHeight = 16.sp)
            }
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
