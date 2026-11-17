package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.auth.attemptAutoSignIn
import com.example.auth.authStateFlow
import com.example.auth.onGoogleSignInClicked
import com.example.auth.signOutGoogle
import com.example.auth.switchGoogleAccount
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.example.ui.components.EditorialSunRaysOverlay
import com.example.ui.components.EditorialSunlightBackdrop
import com.example.ui.components.IncomingFriendRequestPrompt
import com.example.ui.components.LocalHasRootBackdrop
import com.example.ui.components.SideNavigationDrawer
import com.example.ui.components.rememberIsReducedMotionEnabled
import com.example.ui.screens.AddByIdScreen
import com.example.ui.screens.AddFriendsHubScreen
import com.example.ui.screens.AppearanceScreen
import com.example.ui.screens.ChatDetailScreen
import com.example.ui.screens.FriendRequestsScreen
import com.example.ui.screens.FriendSettingsScreen
import com.example.ui.screens.GlobalPrivacyScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NearbyPeopleScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RequestPrivacyScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ThemeStoreScreen
import com.example.data.model.AppLanguage
import com.example.data.model.AppLockMethod
import com.example.ui.components.AppLockOverlay
import com.example.ui.theme.DarkBackdropBase
import com.example.ui.theme.JesrTheme
import com.example.ui.theme.LocalJesrPalette
import com.example.ui.theme.LocalJesrStrings
import com.example.ui.theme.StoneBackdropShadow
import com.example.ui.theme.stringsFor
import com.example.ui.viewmodel.JesrViewModel
import com.example.ui.viewmodel.Screen

class MainActivity : ComponentActivity() {

    private val viewModel: JesrViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val appearanceMode by viewModel.appearanceMode.collectAsStateWithLifecycle()
            val customBackgroundHex by viewModel.customBackgroundHex.collectAsStateWithLifecycle()
            val customIconHex by viewModel.customIconHex.collectAsStateWithLifecycle()
            val customLightHex by viewModel.customLightHex.collectAsStateWithLifecycle()
            val customLightEnabled by viewModel.customLightEnabled.collectAsStateWithLifecycle()
            val activeCinematicThemeId by viewModel.activeCinematicThemeId.collectAsStateWithLifecycle()

            JesrTheme(
                appearanceMode = appearanceMode,
                customBackgroundHex = customBackgroundHex,
                customIconHex = customIconHex,
                customLightHex = customLightHex,
                customLightEnabled = customLightEnabled,
                cinematicThemeId = activeCinematicThemeId
            ) {
                JesrApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun JesrApp(viewModel: JesrViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val palette = LocalJesrPalette.current
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val appLayoutDirection = if (appLanguage == AppLanguage.EN) LayoutDirection.Ltr else LayoutDirection.Rtl
    val strings = remember(appLanguage) { stringsFor(appLanguage) }
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val isMeshActive by viewModel.isMeshActive.collectAsStateWithLifecycle()
    val isDrawerOpen by viewModel.isDrawerOpen.collectAsStateWithLifecycle()
    val userDisplayName by viewModel.userDisplayName.collectAsStateWithLifecycle()
    @Suppress("UNUSED_VARIABLE")
    val liveJesrId by viewModel.myJesrIdFlow.collectAsStateWithLifecycle()
    val pendingRequests by viewModel.pendingRequests.collectAsStateWithLifecycle()
    val incomingRequestSender by viewModel.incomingFriendRequest.collectAsStateWithLifecycle()
    val appLockMethod by viewModel.appLockMethod.collectAsStateWithLifecycle()
    val isAppLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()
    val hasLockSecret = remember(appLockMethod) { viewModel.hasAppLockSecret() }
    val isLockActive = appLockMethod != AppLockMethod.NONE && isAppLocked
    val isReducedMotion = rememberIsReducedMotionEnabled()

    val firebaseAuth = remember { runCatching { Firebase.auth }.getOrNull() }
    val authFlow = remember(firebaseAuth) {
        firebaseAuth?.authStateFlow() ?: kotlinx.coroutines.flow.flowOf(null)
    }
    val currentUser by authFlow
        .collectAsStateWithLifecycle(initialValue = runCatching { firebaseAuth?.currentUser }.getOrNull())
    var isAuthLoading by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    val triggerGoogleSignIn = {
        authError = null
        isAuthLoading = true
        onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            scope = coroutineScope,
            isEnglish = strings.isEnglish,
            onAuthSuccess = { user ->
                isAuthLoading = false
                authError = null
                val googleName = user?.displayName
                if (!googleName.isNullOrBlank() && (userDisplayName == "مستخدم جسر" || userDisplayName == "Jesr User")) {
                    viewModel.updateUserDisplayName(googleName)
                }
            },
            onAuthError = { errorMsg ->
                isAuthLoading = false
                authError = errorMsg
            },
            onAuthCancelled = {
                isAuthLoading = false
            }
        )
    }

    val triggerGoogleSwitchAccount = {
        authError = null
        isAuthLoading = true
        switchGoogleAccount(
            context = context,
            credentialManager = credentialManager,
            scope = coroutineScope,
            isEnglish = strings.isEnglish,
            onAuthSuccess = { user ->
                isAuthLoading = false
                authError = null
                val googleName = user?.displayName
                if (!googleName.isNullOrBlank() && (userDisplayName == "مستخدم جسر" || userDisplayName == "Jesr User")) {
                    viewModel.updateUserDisplayName(googleName)
                }
            },
            onAuthError = { errorMsg ->
                isAuthLoading = false
                authError = errorMsg
            },
            onAuthCancelled = {
                isAuthLoading = false
            }
        )
    }

    LaunchedEffect(currentUser?.uid) {
        viewModel.syncCloudStoreIfSignedIn(currentUser?.uid)
    }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            scope = coroutineScope,
            onAuthSuccess = { user ->
                val googleName = user?.displayName
                if (!googleName.isNullOrBlank() && (userDisplayName == "مستخدم جسر" || userDisplayName == "Jesr User")) {
                    viewModel.updateUserDisplayName(googleName)
                }
            },
            onUnauthenticated = {}
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val anyGranted = permissions.values.any { it }
        if (anyGranted) {
            viewModel.startMesh()
        }
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        permissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    val rootContainerColor = palette.customBackgroundColor ?: if (palette.isDark) DarkBackdropBase else StoneBackdropShadow

    CompositionLocalProvider(
        LocalLayoutDirection provides appLayoutDirection,
        LocalJesrStrings provides strings,
        LocalHasRootBackdrop provides true
    ) {
        if (incomingRequestSender != null && !isLockActive) {
            IncomingFriendRequestPrompt(
                senderId = incomingRequestSender!!,
                onAccept = {
                    viewModel.acceptFriendRequest(incomingRequestSender!!)
                    viewModel.dismissIncomingRequest()
                },
                onDecline = {
                    viewModel.dismissIncomingRequest()
                }
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = rootContainerColor
            ) { _ ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(rootContainerColor)
            ) {
                // Single persistent edge-to-edge camera-responsive animated backdrop
                EditorialSunlightBackdrop(
                    modifier = Modifier.fillMaxSize(),
                    isMeshActive = isMeshActive,
                    currentScreen = currentScreen,
                    forceRender = true
                )

                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        if (isReducedMotion) {
                            EnterTransition.None togetherWith ExitTransition.None
                        } else if (initialState == Screen.THEME_STORE && targetState == Screen.APPEARANCE) {
                            (scaleIn(initialScale = 0.88f, animationSpec = tween(380, easing = LinearOutSlowInEasing)) +
                                slideInVertically(animationSpec = tween(380, easing = LinearOutSlowInEasing)) { it / 6 } +
                                fadeIn(animationSpec = tween(340, easing = LinearOutSlowInEasing))) togetherWith
                                (scaleOut(targetScale = 1.05f, animationSpec = tween(240, easing = FastOutLinearInEasing)) +
                                    fadeOut(animationSpec = tween(220, easing = FastOutLinearInEasing)))
                        } else {
                            val isReturningHome = targetState == Screen.HOME && initialState != Screen.HOME
                            val dirSign = if (isReturningHome) -1 else 1
                            (fadeIn(animationSpec = tween(240, easing = LinearOutSlowInEasing)) +
                                slideInHorizontally(
                                    initialOffsetX = { fullWidth -> (fullWidth * 0.28f * dirSign).toInt() },
                                    animationSpec = tween(280, easing = LinearOutSlowInEasing)
                                )) togetherWith
                                (fadeOut(animationSpec = tween(180, easing = FastOutLinearInEasing)) +
                                    slideOutHorizontally(
                                        targetOffsetX = { fullWidth -> (-fullWidth * 0.16f * dirSign).toInt() },
                                        animationSpec = tween(210, easing = FastOutLinearInEasing)
                                    ))
                        }
                    },
                    label = "screen_transition"
                ) { screen ->
                    when (screen) {
                        Screen.HOME -> HomeScreen(viewModel = viewModel)
                        Screen.PROFILE -> ProfileScreen(viewModel = viewModel)
                        Screen.ADD_FRIENDS_HUB -> AddFriendsHubScreen(viewModel = viewModel)
                        Screen.ADD_BY_ID -> AddByIdScreen(viewModel = viewModel)
                        Screen.NEARBY_PEOPLE -> NearbyPeopleScreen(viewModel = viewModel)
                        Screen.FRIEND_REQUESTS -> FriendRequestsScreen(viewModel = viewModel)
                        Screen.CONVERSATION -> ChatDetailScreen(viewModel = viewModel)
                        Screen.FRIEND_SETTINGS -> FriendSettingsScreen(viewModel = viewModel)
                        Screen.GLOBAL_PRIVACY -> GlobalPrivacyScreen(viewModel = viewModel)
                        Screen.REQUEST_PRIVACY -> RequestPrivacyScreen(viewModel = viewModel)
                        Screen.APPEARANCE -> AppearanceScreen(viewModel = viewModel)
                        Screen.SETTINGS -> SettingsScreen(viewModel = viewModel)
                        Screen.THEME_STORE -> ThemeStoreScreen(
                            viewModel = viewModel,
                            currentUser = currentUser,
                            isAuthLoading = isAuthLoading,
                            onGoogleSignInClick = triggerGoogleSignIn
                        )
                    }
                }

                // Side Navigation Drawer (Section 3 + Bottom Google Sign-In / Switch Account)
                SideNavigationDrawer(
                    isOpen = isDrawerOpen,
                    currentScreen = currentScreen,
                    pendingRequestsCount = pendingRequests.size,
                    currentUser = currentUser,
                    isAuthLoading = isAuthLoading,
                    authError = authError,
                    onGoogleSignInClick = triggerGoogleSignIn,
                    onGoogleSwitchAccountClick = triggerGoogleSwitchAccount,
                    onGoogleSignOutClick = {
                        signOutGoogle(
                            credentialManager = credentialManager,
                            scope = coroutineScope,
                            onSignOutComplete = {
                                authError = null
                            }
                        )
                    },
                    onDismiss = { viewModel.closeDrawer() },
                    onNavigate = { target -> viewModel.navigateTo(target) }
                )

                // Non-interactive 42-degree sunlight & shadow overlay (only for non-cinematic base/custom modes)
                if (palette.cinematicTheme == null) {
                    EditorialSunRaysOverlay(
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            }

            if (isLockActive) {
                AppLockOverlay(
                    method = appLockMethod,
                    hasFallbackSecret = hasLockSecret,
                    onVerifySecret = { candidate -> viewModel.verifyAppLockSecret(candidate) },
                    onBiometricSuccess = { viewModel.unlockWithBiometric() }
                )
            }
        }
    }
}
