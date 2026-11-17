package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalJesrPalette
import com.example.ui.theme.LocalJesrStrings
import com.example.ui.theme.PureWhitePill
import com.example.ui.theme.StoneBackdropDeep
import com.example.ui.theme.TerracottaAccent
import com.example.ui.viewmodel.Screen
import com.google.firebase.auth.FirebaseUser

/**
 * Side Navigation Menu:
 * - No user avatar header at top; Profile is accessed from Home top-right avatar.
 * - Nearby People and Friend Requests are accessed via the bottom (+) button.
 * - Appearance is placed inside Settings.
 * - Navigation entries: Home, Privacy, Theme Store, Settings.
 * - Bottom area: Circular Google badge with direct Switch Account and Sign Out actions.
 */
@Composable
fun SideNavigationDrawer(
    isOpen: Boolean,
    currentScreen: Screen,
    @Suppress("UNUSED_PARAMETER") pendingRequestsCount: Int,
    currentUser: FirebaseUser?,
    isAuthLoading: Boolean,
    authError: String?,
    onGoogleSignInClick: () -> Unit,
    onGoogleSwitchAccountClick: () -> Unit,
    onGoogleSignOutClick: () -> Unit,
    onDismiss: () -> Unit,
    onNavigate: (Screen) -> Unit
) {
    val palette = LocalJesrPalette.current
    val strings = LocalJesrStrings.current
    val appLayoutDirection = LocalLayoutDirection.current
    val isReducedMotion = rememberIsReducedMotionEnabled()

    if (isOpen) {
        BackHandler {
            onDismiss()
        }
    }

    // Physical left-side overlay so it aligns naturally with the top-left Menu icon
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = isOpen,
                enter = fadeIn(tween(220)),
                exit = fadeOut(tween(180))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(StoneBackdropDeep.copy(alpha = 0.52f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss
                        )
                )
            }

            AnimatedVisibility(
                visible = isOpen,
                enter = slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(260)),
                exit = slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(210))
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides appLayoutDirection) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(296.dp)
                            .shadow(24.dp, RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp))
                            .clip(RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp))
                            .background(palette.cardSurface)
                            .border(
                                1.dp,
                                palette.highlight,
                                RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
                            )
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 18.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        DrawerNavRow(
                            title = strings.navHome,
                            icon = Icons.Outlined.Home,
                            selected = currentScreen == Screen.HOME,
                            testTag = "drawer_home",
                            entranceVisible = isOpen,
                            entranceIndex = 0,
                            isReducedMotion = isReducedMotion,
                            onClick = { onNavigate(Screen.HOME) }
                        )
                        DrawerNavRow(
                            title = strings.navPrivacy,
                            icon = Icons.Outlined.Lock,
                            selected = currentScreen == Screen.GLOBAL_PRIVACY,
                            testTag = "drawer_privacy",
                            entranceVisible = isOpen,
                            entranceIndex = 1,
                            isReducedMotion = isReducedMotion,
                            onClick = { onNavigate(Screen.GLOBAL_PRIVACY) }
                        )
                        DrawerNavRow(
                            title = strings.navThemeStore,
                            icon = Icons.Outlined.ShoppingCart,
                            selected = currentScreen == Screen.THEME_STORE,
                            testTag = "drawer_theme_store",
                            entranceVisible = isOpen,
                            entranceIndex = 2,
                            isReducedMotion = isReducedMotion,
                            onClick = { onNavigate(Screen.THEME_STORE) }
                        )
                        DrawerNavRow(
                            title = strings.navSettings,
                            icon = Icons.Outlined.Settings,
                            selected = currentScreen == Screen.SETTINGS,
                            testTag = "drawer_settings",
                            entranceVisible = isOpen,
                            entranceIndex = 3,
                            isReducedMotion = isReducedMotion,
                            onClick = { onNavigate(Screen.SETTINGS) }
                        )

                        Spacer(modifier = Modifier.weight(1f, fill = true))
                        Spacer(modifier = Modifier.height(14.dp))

                        // Bottom Circular Google Logo Sign-In / Switch Account Controls (no divider)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp)
                                .staggeredIconEntrance(
                                    visible = isOpen,
                                    index = 4,
                                    stepDelayMs = 65,
                                    initialDelayMs = 80,
                                    startOffsetX = (-24).dp,
                                    startOffsetY = 14.dp,
                                    startScale = 0.72f,
                                    isReducedMotion = isReducedMotion
                                ),
                            horizontalAlignment = Alignment.Start
                        ) {
                            if (currentUser == null) {
                                val signInInteraction = remember { MutableInteractionSource() }
                                val isPressed by signInInteraction.collectIsPressedAsState()
                                val scale by animateFloatAsState(
                                    targetValue = if (isPressed) 0.91f else 1f,
                                    animationSpec = spring(dampingRatio = 0.56f, stiffness = 500f),
                                    label = "google_circle_scale"
                                )

                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .graphicsLayer {
                                            scaleX = scale
                                            scaleY = scale
                                        }
                                        .shadow(
                                            elevation = if (isPressed) 3.dp else 8.dp,
                                            shape = CircleShape,
                                            ambientColor = StoneBackdropDeep.copy(alpha = 0.25f),
                                            spotColor = StoneBackdropDeep.copy(alpha = 0.30f)
                                        )
                                        .clip(CircleShape)
                                        .background(PureWhitePill)
                                        .border(1.dp, palette.border, CircleShape)
                                        .semantics {
                                            contentDescription = strings.signInWithGoogle
                                        }
                                        .clickable(
                                            interactionSource = signInInteraction,
                                            indication = ripple(bounded = true, color = palette.accent),
                                            enabled = !isAuthLoading,
                                            role = Role.Button,
                                            onClick = onGoogleSignInClick
                                        )
                                        .testTag("drawer_google_signin_btn"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isAuthLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            color = palette.accent,
                                            strokeWidth = 2.4.dp
                                        )
                                    } else {
                                        GoogleLogoIcon(modifier = Modifier.size(42.dp))
                                    }
                                }

                                if (!authError.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = authError,
                                        fontSize = 11.sp,
                                        color = TerracottaAccent,
                                        lineHeight = 15.sp,
                                        textAlign = TextAlign.Start
                                    )
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .shadow(6.dp, CircleShape)
                                            .clip(CircleShape)
                                            .background(PureWhitePill)
                                            .border(1.5.dp, palette.accent, CircleShape)
                                            .semantics {
                                                contentDescription = strings.connectedWithGoogle
                                            }
                                            .clickable(
                                                enabled = !isAuthLoading,
                                                role = Role.Button,
                                                onClick = onGoogleSwitchAccountClick
                                            )
                                            .testTag("drawer_google_connected_badge"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isAuthLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(22.dp),
                                                color = palette.accent,
                                                strokeWidth = 2.4.dp
                                            )
                                        } else {
                                            GoogleLogoIcon(modifier = Modifier.size(42.dp))
                                        }
                                    }

                                    // Direct Switch Google Account button without needing sign-out first
                                    Row(
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(palette.elevatedSurface)
                                            .border(0.8.dp, palette.border, RoundedCornerShape(50))
                                            .clickable(
                                                enabled = !isAuthLoading,
                                                role = Role.Button,
                                                onClick = onGoogleSwitchAccountClick
                                            )
                                            .testTag("drawer_google_switch_btn")
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SwapHoriz,
                                            contentDescription = strings.switchGoogleAccount,
                                            tint = palette.iconColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = strings.switchGoogleAccount,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.inkPrimary
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(palette.elevatedSurface)
                                            .border(0.8.dp, palette.border, CircleShape)
                                            .clickable(
                                                role = Role.Button,
                                                onClick = onGoogleSignOutClick
                                            )
                                            .testTag("drawer_google_signout_btn"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.Logout,
                                            contentDescription = strings.signOut,
                                            tint = TerracottaAccent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                if (!authError.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = authError,
                                        fontSize = 11.sp,
                                        color = TerracottaAccent,
                                        lineHeight = 15.sp,
                                        textAlign = TextAlign.Start
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

/**
 * Clean 4-color Google "G" emblem inside a subtle circular badge.
 */
@Composable
fun GoogleLogoIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(PureWhitePill)
            .border(0.7.dp, Color(0x26000000), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize(0.62f)) {
            val diameter = size.minDimension
            val stroke = diameter * 0.22f
            val arcSize = Size(diameter - stroke, diameter - stroke)
            val topLeft = Offset(stroke / 2f, stroke / 2f)

            // Red top arc
            drawArc(
                color = Color(0xFFEA4335),
                startAngle = 215f,
                sweepAngle = 100f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Butt)
            )
            // Yellow left arc
            drawArc(
                color = Color(0xFFFBBC05),
                startAngle = 145f,
                sweepAngle = 72f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Butt)
            )
            // Green bottom arc
            drawArc(
                color = Color(0xFF34A853),
                startAngle = 42f,
                sweepAngle = 105f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Butt)
            )
            // Blue bottom-right arc
            drawArc(
                color = Color(0xFF4285F4),
                startAngle = -5f,
                sweepAngle = 50f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Butt)
            )
            // Blue horizontal crossbar of "G"
            drawLine(
                color = Color(0xFF4285F4),
                start = Offset(size.width * 0.50f, size.height / 2f),
                end = Offset(size.width - stroke * 0.15f, size.height / 2f),
                strokeWidth = stroke,
                cap = StrokeCap.Butt
            )
        }
    }
}

@Composable
private fun DrawerNavRow(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    badgeCount: Int = 0,
    testTag: String,
    entranceVisible: Boolean = true,
    entranceIndex: Int = 0,
    isReducedMotion: Boolean = false,
    onClick: () -> Unit
) {
    val palette = LocalJesrPalette.current
    val bgColor = if (selected) palette.accent else Color.Transparent
    val contentColor = if (selected) PureWhitePill else palette.inkPrimary
    val iconTint = if (selected) PureWhitePill else palette.iconColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .staggeredIconEntrance(
                visible = entranceVisible,
                index = entranceIndex,
                stepDelayMs = 65,
                initialDelayMs = 80,
                startOffsetX = (-26).dp,
                startOffsetY = 8.dp,
                startScale = 0.88f,
                isReducedMotion = isReducedMotion
            )
            .padding(vertical = 3.dp)
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = contentColor),
                onClick = onClick
            )
            .testTag(testTag)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier
                    .size(21.dp)
                    .staggeredIconEntrance(
                        visible = entranceVisible,
                        index = entranceIndex,
                        stepDelayMs = 65,
                        initialDelayMs = 115,
                        startOffsetX = (-14).dp,
                        startOffsetY = 0.dp,
                        startScale = 0.65f,
                        isReducedMotion = isReducedMotion
                    )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                fontSize = 14.5.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                color = contentColor
            )
        }

        if (badgeCount > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) PureWhitePill else palette.accent)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$badgeCount",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) palette.accent else PureWhitePill
                )
            }
        }
    }
}
