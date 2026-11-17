package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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
import com.example.data.repository.JesrRepository
import com.example.data.repository.StoreThemeItem
import com.example.ui.components.CinematicSceneCanvas
import com.example.ui.components.EditorialFloatingCard
import com.example.ui.components.EditorialInteractiveIconButton
import com.example.ui.components.EditorialSubScreenHeader
import com.example.ui.components.EditorialSunlightBackdrop
import com.example.ui.components.GoogleLogoIcon
import com.example.ui.components.rememberIsReducedMotionEnabled
import com.example.ui.components.staggeredIconEntrance
import com.example.ui.theme.CinematicThemeSpec
import com.example.ui.theme.CinematicThemesCatalog
import com.example.ui.theme.InkBlack
import com.example.ui.theme.LocalJesrPalette
import com.example.ui.theme.LocalJesrStrings
import com.example.ui.theme.PureWhitePill
import com.example.ui.theme.SageOliveAccent
import com.example.ui.theme.localizedCreator
import com.example.ui.theme.localizedDescription
import com.example.ui.theme.localizedTitle
import com.example.ui.viewmodel.JesrViewModel
import com.example.ui.viewmodel.Screen
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.delay

private enum class StoreFilter {
    ALL,
    FREE
}

@Composable
private fun StoreFilter.localizedLabel(): String {
    val strings = LocalJesrStrings.current
    return when (this) {
        StoreFilter.ALL -> strings.filterAll
        StoreFilter.FREE -> strings.filterFree
    }
}

@Composable
fun ThemeStoreScreen(
    viewModel: JesrViewModel,
    currentUser: FirebaseUser?,
    isAuthLoading: Boolean,
    onGoogleSignInClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val palette = LocalJesrPalette.current
    val strings = LocalJesrStrings.current
    val layoutDirection = LocalLayoutDirection.current
    val slideOffsetX = if (layoutDirection == LayoutDirection.Rtl) 16.dp else (-16).dp
    val isMeshActive by viewModel.isMeshActive.collectAsStateWithLifecycle()
    val storeThemes by viewModel.storeThemes.collectAsStateWithLifecycle()
    val acquiredThemeIds by viewModel.acquiredThemeIds.collectAsStateWithLifecycle()
    val storeStatusMessage by viewModel.storeStatusMessage.collectAsStateWithLifecycle()
    val canUndoCustom by viewModel.canUndoCustomAppearance.collectAsStateWithLifecycle()
    val isReducedMotion = rememberIsReducedMotionEnabled()

    var selectedFilter by remember { mutableStateOf(StoreFilter.ALL) }

    LaunchedEffect(storeStatusMessage) {
        if (storeStatusMessage != null) {
            delay(3200)
            viewModel.clearStoreStatusMessage()
        }
    }

    // Store strictly shows themes that have NOT been acquired yet (and never the protected Base UI which is permanently in Downloaded Themes)
    val availableStoreThemes = remember(storeThemes, acquiredThemeIds) {
        storeThemes.filter { it.id != JesrRepository.BASE_THEME_ID && it.id !in acquiredThemeIds }
    }

    val filteredThemes = remember(availableStoreThemes, selectedFilter) {
        when (selectedFilter) {
            StoreFilter.ALL -> availableStoreThemes
            StoreFilter.FREE -> availableStoreThemes.filter { it.priceUsd == 0 }
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
                        title = strings.themeStoreTitle,
                        subtitle = strings.themeStoreSubtitle,
                        onBack = { viewModel.navigateBack() },
                        backTestTag = "theme_store_back_btn",
                        trailingContent = if (canUndoCustom) {
                            {
                                EditorialInteractiveIconButton(
                                    icon = Icons.AutoMirrored.Filled.Undo,
                                    contentDescription = strings.undoApplyCd,
                                    containerColor = palette.pillSurface,
                                    contentColor = palette.iconColor,
                                    onClick = { viewModel.undoCustomAppearance() },
                                    testTag = "store_undo_btn"
                                )
                            }
                        } else null
                    )

                    // Optional Cloud Sync banner when signed out
                    if (currentUser == null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(palette.cardSurfaceWarm)
                                .border(0.8.dp, palette.border, RoundedCornerShape(18.dp))
                                .clickable(enabled = !isAuthLoading, onClick = onGoogleSignInClick)
                                .testTag("store_google_signin_btn")
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (isAuthLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = palette.accent,
                                    strokeWidth = 2.4.dp
                                )
                            } else {
                                GoogleLogoIcon(modifier = Modifier.size(30.dp))
                            }
                            Text(
                                text = strings.storeGoogleSyncBanner,
                                fontSize = 11.5.sp,
                                color = palette.inkSecondary,
                                lineHeight = 16.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    AnimatedVisibility(visible = !storeStatusMessage.isNullOrBlank()) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(palette.accent.copy(alpha = 0.16f))
                                    .border(0.8.dp, palette.accent.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 14.dp, vertical = 9.dp)
                            ) {
                                Text(
                                    text = storeStatusMessage.orEmpty(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = palette.inkPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Filter Chips Row (Only All & Free)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StoreFilter.entries.forEachIndexed { idx, filter ->
                            val selected = selectedFilter == filter
                            val selectedBg = palette.accent
                            val selectedTextColor = if (selectedBg.luminance() > 0.55f) InkBlack else PureWhitePill
                            Box(
                                modifier = Modifier
                                    .staggeredIconEntrance(
                                        index = idx,
                                        stepDelayMs = 55,
                                        startOffsetX = slideOffsetX,
                                        startOffsetY = 8.dp,
                                        startScale = 0.84f,
                                        isReducedMotion = isReducedMotion
                                    )
                                    .clip(RoundedCornerShape(50))
                                    .background(if (selected) selectedBg else palette.pillSurface)
                                    .border(
                                        width = 0.8.dp,
                                        color = if (selected) palette.accent else palette.border,
                                        shape = RoundedCornerShape(50)
                                    )
                                    .clickable { selectedFilter = filter }
                                    .testTag("store_filter_${filter.name.lowercase()}")
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = filter.localizedLabel(),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) selectedTextColor else palette.inkPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (filteredThemes.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(horizontal = 20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    tint = palette.mutedText,
                                    modifier = Modifier.size(34.dp)
                                )
                                Text(
                                    text = if (availableStoreThemes.isEmpty()) {
                                        strings.storeAllAcquiredMsg
                                    } else {
                                        strings.storeEmptyCategory
                                    },
                                    fontSize = 13.sp,
                                    color = palette.mutedText,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp
                                )
                                OutlinedButton(
                                    onClick = { viewModel.navigateTo(Screen.APPEARANCE) },
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.testTag("store_go_to_downloaded_btn")
                                ) {
                                    Text(
                                        text = strings.myThemesHeader,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.inkPrimary
                                    )
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .testTag("store_themes_list"),
                            contentPadding = PaddingValues(bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(filteredThemes, key = { _, item -> item.id }) { idx, item ->
                                val cinematicSpec = remember(item.id) { CinematicThemesCatalog.findById(item.id) }
                                StoreThemeCard(
                                    item = item,
                                    cinematicSpec = cinematicSpec,
                                    entranceIndex = idx.coerceAtMost(6),
                                    isReducedMotion = isReducedMotion,
                                    onGetTheme = {
                                        viewModel.acquireFromStoreAndMoveToAppearance(
                                            item = item,
                                            isDarkForCinematic = palette.isDark
                                        )
                                    }
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
private fun StoreThemeCard(
    item: StoreThemeItem,
    cinematicSpec: CinematicThemeSpec?,
    entranceIndex: Int = 0,
    isReducedMotion: Boolean = false,
    onGetTheme: () -> Unit
) {
    val palette = LocalJesrPalette.current
    val strings = LocalJesrStrings.current
    val layoutDirection = LocalLayoutDirection.current
    val slideOffsetX = if (layoutDirection == LayoutDirection.Rtl) 16.dp else (-16).dp
    val localizedDesc = item.localizedDescription(strings.isEnglish)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .staggeredIconEntrance(
                index = entranceIndex,
                stepDelayMs = 60,
                startOffsetX = slideOffsetX,
                startOffsetY = 16.dp,
                startScale = 0.90f,
                isReducedMotion = isReducedMotion
            )
            .clip(RoundedCornerShape(22.dp))
            .background(palette.elevatedSurface)
            .border(
                width = 0.8.dp,
                color = palette.border,
                shape = RoundedCornerShape(22.dp)
            )
            .testTag("store_theme_card_${item.id}")
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (cinematicSpec != null) {
                MiniCinematicPreviewThumbnail(
                    spec = cinematicSpec,
                    isReducedMotion = isReducedMotion
                )
            } else {
                MiniThemePreviewThumbnail(
                    backgroundHex = item.backgroundHex,
                    iconHex = item.iconHex,
                    lightHex = item.lightHex,
                    lightEnabled = item.lightEnabled
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.localizedTitle(strings.isEnglish),
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.inkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    PriceOrProtectedBadge(isProtectedBase = false)
                }

                Text(
                    text = strings.designerPrefix(item.localizedCreator(strings.isEnglish)),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.accent
                )

                if (cinematicSpec != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = palette.accent,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = strings.cinematicBadge,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.inkSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (localizedDesc.isNotBlank()) {
                    Text(
                        text = localizedDesc,
                        fontSize = 11.5.sp,
                        color = palette.mutedText,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        val btnBg = cinematicSpec?.darkVariant?.bgMid ?: palette.inkPrimary
        val btnContent = if (btnBg.luminance() > 0.55f) InkBlack else PureWhitePill
        Button(
            onClick = onGetTheme,
            colors = ButtonDefaults.buttonColors(
                containerColor = btnBg,
                contentColor = btnContent
            ),
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag("store_get_theme_${item.id}")
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${strings.getThemeBtn} • ${strings.freeLabel}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PriceOrProtectedBadge(
    isProtectedBase: Boolean
) {
    val strings = LocalJesrStrings.current
    val bgColor = if (isProtectedBase) SageOliveAccent.copy(alpha = 0.22f) else SageOliveAccent.copy(alpha = 0.16f)
    val textColor = SageOliveAccent
    val label = if (isProtectedBase) strings.badgeBaseProtected else strings.freeLabel

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (isProtectedBase) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = strings.unDeletableCd,
                tint = textColor,
                modifier = Modifier.size(11.dp)
            )
        }
        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.ExtraBold,
            color = textColor
        )
    }
}

@Composable
internal fun MiniCinematicPreviewThumbnail(
    spec: CinematicThemeSpec,
    isReducedMotion: Boolean = false
) {
    Box(
        modifier = Modifier
            .size(width = 78.dp, height = 78.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(1.1.dp, spec.darkVariant.framePrimary.copy(alpha = 0.72f), RoundedCornerShape(18.dp))
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left half: Dark animated scene
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                CinematicSceneCanvas(
                    spec = spec,
                    isDark = true,
                    isReducedMotion = isReducedMotion,
                    modifier = Modifier.fillMaxSize()
                )
            }
            // Right half: Light animated scene
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                CinematicSceneCanvas(
                    spec = spec,
                    isDark = false,
                    isReducedMotion = isReducedMotion,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Miniature Framed Emblem Center Badge showing avatar frame colors
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(28.dp)
                .clip(CircleShape)
                .background(spec.darkVariant.cardSurface.copy(alpha = 0.78f))
                .border(
                    width = 2.dp,
                    brush = Brush.sweepGradient(
                        listOf(
                            spec.darkVariant.framePrimary,
                            spec.lightVariant.frameSecondary,
                            spec.darkVariant.framePrimary
                        )
                    ),
                    shape = CircleShape
                )
        )
    }
}

@Composable
internal fun MiniThemePreviewThumbnail(
    backgroundHex: Long,
    iconHex: Long,
    lightHex: Long,
    lightEnabled: Boolean
) {
    val bgColor = Color(backgroundHex)
    val iconColor = Color(iconHex)
    val lightColor = Color(lightHex)
    val isDarkBg = bgColor.luminance() < 0.35f
    val miniCardColor = if (isDarkBg) Color(0xFF1F2733) else Color(0xFFF6EFE6)

    Box(
        modifier = Modifier
            .size(width = 78.dp, height = 78.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(0.9.dp, Color.Black.copy(alpha = 0.15f), RoundedCornerShape(18.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(color = bgColor)

            if (lightEnabled) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            lightColor.copy(alpha = 0.85f),
                            lightColor.copy(alpha = 0.25f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.16f, h * 0.14f),
                        radius = w * 0.82f
                    ),
                    radius = w * 0.82f,
                    center = Offset(w * 0.16f, h * 0.14f)
                )
            }

            // Miniature floating card
            drawRoundRect(
                color = miniCardColor,
                topLeft = Offset(w * 0.16f, h * 0.22f),
                size = Size(w * 0.68f, h * 0.56f),
                cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
            )

            // Miniature top menu lines & (+) FAB colored with iconHex
            drawRoundRect(
                color = iconColor,
                topLeft = Offset(w * 0.24f, h * 0.30f),
                size = Size(w * 0.18f, 2.dp.toPx()),
                cornerRadius = CornerRadius(2f, 2f)
            )
            drawRoundRect(
                color = iconColor,
                topLeft = Offset(w * 0.24f, h * 0.36f),
                size = Size(w * 0.12f, 2.dp.toPx()),
                cornerRadius = CornerRadius(2f, 2f)
            )
            drawCircle(
                color = iconColor,
                radius = 5.5.dp.toPx(),
                center = Offset(w * 0.30f, h * 0.66f)
            )
        }
    }
}
