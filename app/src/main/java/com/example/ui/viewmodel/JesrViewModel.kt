package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.entity.ConversationEntity
import com.example.data.entity.FriendEntity
import com.example.data.entity.MessageEntity
import com.example.data.model.AppLanguage
import com.example.data.model.AppLockMethod
import com.example.data.model.AppearanceMode
import com.example.security.AppLockManager
import com.example.ui.theme.CinematicThemesCatalog
import com.example.data.repository.AccountLookupResult
import com.example.data.repository.AcquiredThemeRecord
import com.example.data.repository.JesrRepository
import com.example.data.repository.RequestPrivacyRule
import com.example.data.repository.StoreThemeItem
import com.example.data.repository.ThemeStoreRepository
import com.example.network.DistanceEstimator
import com.example.network.model.PeerNode
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class Screen {
    HOME,
    PROFILE,
    ADD_FRIENDS_HUB,
    ADD_BY_ID,
    NEARBY_PEOPLE,
    FRIEND_REQUESTS,
    CONVERSATION,
    FRIEND_SETTINGS,
    GLOBAL_PRIVACY,
    REQUEST_PRIVACY,
    APPEARANCE,
    SETTINGS,
    THEME_STORE
}

enum class FriendRequestsTab {
    INCOMING,
    SENT
}

class JesrViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = JesrRepository(application)
    private val themeStoreRepository: ThemeStoreRepository by lazy { ThemeStoreRepository(application) }

    val myJesrId: String get() = repository.myJesrId
    val myPublicKey: String get() = repository.myPublicKeyBase64
    val myJesrIdFlow: StateFlow<String> = repository.myJesrIdFlow

    // Duplicate-free navigation backstack preserving screen context across submenus
    private val _backStack = MutableStateFlow(listOf(Screen.HOME))
    val backStack: StateFlow<List<Screen>> = _backStack.asStateFlow()

    private val _currentScreen = MutableStateFlow(Screen.HOME)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _isDrawerOpen = MutableStateFlow(false)
    val isDrawerOpen: StateFlow<Boolean> = _isDrawerOpen.asStateFlow()

    private val _requestsInitialTab = MutableStateFlow(FriendRequestsTab.INCOMING)
    val requestsInitialTab: StateFlow<FriendRequestsTab> = _requestsInitialTab.asStateFlow()

    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId = _activeConversationId.asStateFlow()

    private val _activeConversationName = MutableStateFlow("")
    val activeConversationName = _activeConversationName.asStateFlow()

    private val _selectedFriendSettingsId = MutableStateFlow<String?>(null)
    val selectedFriendSettingsId = _selectedFriendSettingsId.asStateFlow()

    private val _accountLookupResult = MutableStateFlow<AccountLookupResult>(AccountLookupResult.Idle)
    val accountLookupResult = _accountLookupResult.asStateFlow()

    val isMeshActive: StateFlow<Boolean> = repository.isMeshActive
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val appLanguage: StateFlow<AppLanguage> = repository.appLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppLanguage.AR)

    val appLockMethod: StateFlow<AppLockMethod> = repository.appLockMethod
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppLockMethod.NONE)

    val isAppLocked: StateFlow<Boolean> = repository.isAppLocked
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val userDisplayName: StateFlow<String> = repository.userDisplayName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "مستخدم جسر")

    val userAvatarPath: StateFlow<String?> = repository.userAvatarPath
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userCoverPath: StateFlow<String?> = repository.userCoverPath
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val neighbors: StateFlow<Map<String, PeerNode>> = repository.neighbors
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val conversations: StateFlow<List<ConversationEntity>> = repository.conversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val friends: StateFlow<List<FriendEntity>> = repository.friends
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFriendEntries: StateFlow<List<FriendEntity>> = repository.allFriendEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingRequests: StateFlow<List<FriendEntity>> = repository.pendingReceivedRequests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sentRequests: StateFlow<List<FriendEntity>> = repository.pendingSentRequests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val queuedCount: StateFlow<Int> = repository.queuedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val relayedCount: StateFlow<Int> = repository.relayedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val deliveredCount: StateFlow<Int> = repository.deliveredCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val incomingFriendRequest: StateFlow<String?> = repository.incomingFriendRequestSender
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Global & Friend Request Privacy States (disabled by default)
    val globalDistanceAllowed: StateFlow<Boolean> = repository.globalDistanceAllowed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val globalMutualFriendsAllowed: StateFlow<Boolean> = repository.globalMutualFriendsAllowed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isNearbyVisibilityEnabled: StateFlow<Boolean> = repository.isNearbyVisibilityEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val globalMuteRequests: StateFlow<Boolean> = repository.globalMuteRequests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val globalBlockRequests: StateFlow<Boolean> = repository.globalBlockRequests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val requestPrivacyRules: StateFlow<Map<String, RequestPrivacyRule>> = repository.requestPrivacyRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Appearance & Custom Mode Controls (Background + Icons + Light Color + Light Toggle)
    val appearanceMode: StateFlow<AppearanceMode> = repository.appearanceMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppearanceMode.AUTO)

    val customBackgroundHex: StateFlow<Long?> = repository.customBackgroundHex
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val customIconHex: StateFlow<Long?> = repository.customIconHex
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val customLightHex: StateFlow<Long?> = repository.customLightHex
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val customLightEnabled: StateFlow<Boolean> = repository.customLightEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val activeCinematicThemeId: StateFlow<String?> = repository.activeCinematicThemeId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val canUndoCustomAppearance: StateFlow<Boolean> = repository.canUndoCustomAppearance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Theme Store state (Protected Base Theme + Curated + Local + Cloud Firestore when signed in)
    private val _cloudPublishedThemes = MutableStateFlow<List<StoreThemeItem>>(emptyList())
    private val _cloudAcquiredThemeIds = MutableStateFlow<Set<String>>(emptySet())
    private val _isStoreBusy = MutableStateFlow(false)
    val isStoreBusy: StateFlow<Boolean> = _isStoreBusy.asStateFlow()

    private val _storeStatusMessage = MutableStateFlow<String?>(null)
    val storeStatusMessage: StateFlow<String?> = _storeStatusMessage.asStateFlow()

    private val _justAcquiredThemeId = MutableStateFlow<String?>(null)
    val justAcquiredThemeId: StateFlow<String?> = _justAcquiredThemeId.asStateFlow()

    private var cloudStoreSyncJob: Job? = null
    private var syncedStoreUid: String? = null

    val storeThemes: StateFlow<List<StoreThemeItem>> = combine(
        repository.localPublishedThemes,
        _cloudPublishedThemes
    ) { localList, cloudList ->
        val merged = linkedMapOf<String, StoreThemeItem>()
        JesrRepository.CURATED_STORE_THEMES.forEach { merged[it.id] = it }
        cloudList.forEach { merged[it.id] = it }
        localList.forEach { if (!merged.containsKey(it.id)) merged[it.id] = it }
        merged.values.toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), JesrRepository.CURATED_STORE_THEMES)

    val acquiredThemeIds: StateFlow<Set<String>> = combine(
        repository.localAcquiredThemeIds,
        _cloudAcquiredThemeIds
    ) { localIds, cloudIds ->
        localIds + cloudIds + JesrRepository.BASE_THEME_ID
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), setOf(JesrRepository.BASE_THEME_ID))

    private fun currentFirebaseUid(): String? = runCatching { Firebase.auth.currentUser?.uid }.getOrNull()

    // List of user's available interfaces/themes shown directly in AppearanceScreen (always includes Original Theme)
    val ownedUserThemes: StateFlow<List<StoreThemeItem>> = combine(
        storeThemes,
        acquiredThemeIds
    ) { catalog, ownedIds ->
        val uid = currentFirebaseUid()
        val owned = catalog.filter { item ->
            item.id == JesrRepository.BASE_THEME_ID ||
                ownedIds.contains(item.id) ||
                (uid != null && item.creatorId == uid) ||
                item.creatorId == myJesrId
        }
        val baseItem = owned.firstOrNull { it.id == JesrRepository.BASE_THEME_ID }
            ?: JesrRepository.CURATED_STORE_THEMES.first { it.id == JesrRepository.BASE_THEME_ID }
        listOf(baseItem) + owned.filterNot { it.id == JesrRepository.BASE_THEME_ID }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        listOf(JesrRepository.CURATED_STORE_THEMES.first { it.id == JesrRepository.BASE_THEME_ID })
    )

    val activeMessages: StateFlow<List<MessageEntity>> = _activeConversationId.flatMapLatest { convId ->
        if (convId != null) {
            repository.getMessages(convId)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun openDrawer() {
        _isDrawerOpen.value = true
    }

    fun closeDrawer() {
        _isDrawerOpen.value = false
    }

    fun navigateTo(screen: Screen) {
        _isDrawerOpen.value = false
        val stack = _backStack.value
        if (stack.lastOrNull() == screen) return
        val newStack = when {
            screen == Screen.HOME -> listOf(Screen.HOME)
            stack.contains(screen) -> {
                val idx = stack.indexOf(screen)
                stack.subList(0, idx + 1)
            }
            else -> stack + screen
        }
        _backStack.value = newStack
        _currentScreen.value = screen
    }

    fun navigateToFriendRequests(tab: FriendRequestsTab) {
        _requestsInitialTab.value = tab
        navigateTo(Screen.FRIEND_REQUESTS)
    }

    fun openConversation(convId: String, name: String) {
        _activeConversationId.value = convId
        _activeConversationName.value = name
        repository.markConversationRead(convId)
        navigateTo(Screen.CONVERSATION)
    }

    fun openFriendSettings(jesrId: String) {
        _selectedFriendSettingsId.value = jesrId
        navigateTo(Screen.FRIEND_SETTINGS)
    }

    fun navigateBack() {
        val stack = _backStack.value
        if (stack.size > 1) {
            val trimmed = stack.dropLast(1)
            val prev = trimmed.last()
            if (stack.last() == Screen.CONVERSATION && prev == Screen.HOME) {
                _activeConversationId.value = null
            }
            _backStack.value = trimmed
            _currentScreen.value = prev
        } else {
            _activeConversationId.value = null
            _backStack.value = listOf(Screen.HOME)
            _currentScreen.value = Screen.HOME
        }
    }

    // Profile Actions
    fun updateUserDisplayName(name: String) = repository.updateUserDisplayName(name)
    fun updateUserAvatar(uri: Uri) = repository.updateUserAvatarFromUri(uri)
    fun updateUserAvatarBitmap(bitmap: Bitmap) = repository.updateUserAvatarFromBitmap(bitmap)
    fun updateUserCoverBitmap(bitmap: Bitmap) = repository.updateUserCoverFromBitmap(bitmap)
    fun clearUserCover() = repository.clearUserCover()

    fun setAppLanguage(language: AppLanguage) = repository.setAppLanguage(language)

    // App Lock Actions
    fun hasAppLockSecret(): Boolean = repository.hasAppLockSecret()
    fun disableAppLock() = repository.disableAppLock()
    fun setAppLockBiometric() = repository.configureAppLockBiometric()
    fun setAppLockPin(pin: String) = repository.configureAppLockSecret(AppLockMethod.PIN, pin)
    fun setAppLockPassword(password: String) = repository.configureAppLockSecret(AppLockMethod.PASSWORD, password)
    fun setAppLockPattern(dots: List<Int>) =
        repository.configureAppLockSecret(AppLockMethod.PATTERN, AppLockManager.encodePattern(dots))
    fun verifyAppLockSecret(candidate: String): Boolean = repository.verifyAppLockSecret(candidate)
    fun unlockWithBiometric() = repository.unlockWithBiometric()
    fun lockAppNow() = repository.lockAppNow()

    // Lookup & Discovery Actions
    fun clearAccountLookup() {
        _accountLookupResult.value = AccountLookupResult.Idle
    }

    fun lookupAccountById(rawId: String) {
        viewModelScope.launch {
            _accountLookupResult.value = repository.lookupAccountById(rawId)
        }
    }

    fun refreshNearbyPeople() = repository.refreshMeshDiscovery()
    fun refreshFriendRequests() = repository.refreshMeshDiscovery()

    fun dismissIncomingRequest() = repository.dismissIncomingFriendRequest()

    // Mesh Control
    fun toggleMesh() {
        if (isMeshActive.value) repository.stopMesh() else repository.startMesh()
    }
    fun startMesh() = repository.startMesh()
    fun stopMesh() = repository.stopMesh()

    // Messaging & Attachments & Retry
    fun sendMessage(
        text: String,
        isSos: Boolean = false,
        attachmentUri: String? = null,
        attachmentName: String? = null
    ) {
        val target = _activeConversationId.value ?: return
        if (text.isBlank() && attachmentName == null) return
        repository.sendMessage(target, text.trim(), isSos, attachmentUri, attachmentName)
    }

    fun retryMessage(messageId: String) = repository.retryFailedMessage(messageId)

    // Friend Requests Actions
    fun sendFriendRequest(targetJesrId: String, customName: String = "") {
        if (targetJesrId.isBlank()) return
        repository.sendFriendRequest(targetJesrId.trim().uppercase(), customName.trim())
    }

    fun cancelSentRequest(targetJesrId: String) = repository.cancelSentFriendRequest(targetJesrId)
    fun acceptFriendRequest(targetJesrId: String) = repository.acceptFriendRequest(targetJesrId)
    fun rejectFriendRequest(targetJesrId: String) = repository.rejectFriendRequest(targetJesrId)

    // Friend Management & Blocking / Mutual Resumption / Mutual Deletion
    fun toggleMuteFriend(jesrId: String) = repository.toggleMuteFriend(jesrId)
    fun blockFriend(jesrId: String) = repository.blockFriend(jesrId)
    fun unblockFriend(jesrId: String) = repository.unblockFriend(jesrId)
    fun approveResumeCommunication(jesrId: String) = repository.approveResumeCommunication(jesrId)
    fun removeFriendMutually(jesrId: String) {
        repository.removeFriendMutually(jesrId)
        if (_activeConversationId.value == jesrId) {
            _activeConversationId.value = null
            _backStack.value = listOf(Screen.HOME)
            _currentScreen.value = Screen.HOME
        } else if (_currentScreen.value == Screen.FRIEND_SETTINGS && _selectedFriendSettingsId.value == jesrId) {
            navigateBack()
        }
    }

    // Global & Individual Privacy Controls
    fun setGlobalDistanceAllowed(enabled: Boolean) = repository.setGlobalDistanceAllowed(enabled)
    fun setGlobalMutualFriendsAllowed(enabled: Boolean) = repository.setGlobalMutualFriendsAllowed(enabled)
    fun setFriendDistanceOverride(jesrId: String, overrideVal: Boolean?) =
        repository.setFriendDistanceOverride(jesrId, overrideVal)
    fun setFriendMutualFriendsOverride(jesrId: String, overrideVal: Boolean?) =
        repository.setFriendMutualFriendsOverride(jesrId, overrideVal)
    fun setNearbyVisibilityEnabled(enabled: Boolean) = repository.setNearbyVisibilityEnabled(enabled)

    fun effectiveDistanceFor(friend: FriendEntity): Boolean =
        friend.distancePermissionOverride ?: globalDistanceAllowed.value

    fun effectiveMutualFriendsFor(friend: FriendEntity): Boolean =
        friend.mutualFriendsPermissionOverride ?: globalMutualFriendsAllowed.value

    // Friend Request Privacy Controls
    fun setGlobalMuteRequests(enabled: Boolean) = repository.setGlobalMuteRequests(enabled)
    fun setGlobalBlockRequests(enabled: Boolean) = repository.setGlobalBlockRequests(enabled)
    fun setUserRequestMuteRule(jesrId: String, muteOverride: Boolean?) =
        repository.setUserRequestMuteRule(jesrId, muteOverride)
    fun setUserRequestBlockRule(jesrId: String, blockOverride: Boolean?) =
        repository.setUserRequestBlockRule(jesrId, blockOverride)
    fun deleteAllIncomingRequests() = repository.deleteAllIncomingRequests()

    fun isRequestMuted(senderId: String): Boolean = repository.isRequestMutedFor(senderId)
    fun isRequestBlocked(senderId: String): Boolean = repository.isRequestBlockedFor(senderId)

    // Appearance Controls (Separate colors, Base Color restore, Undo)
    fun setAppearanceMode(mode: AppearanceMode) = repository.setAppearanceMode(mode)
    fun setCustomBackgroundColor(backgroundHex: Long?) = repository.setCustomBackgroundColor(backgroundHex)
    fun setCustomIconColor(iconHex: Long?) = repository.setCustomIconColor(iconHex)
    fun setCustomLightColor(lightHex: Long?) = repository.setCustomLightColor(lightHex)
    fun setCustomColors(backgroundHex: Long?, iconHex: Long?, lightHex: Long?) =
        repository.setCustomColors(backgroundHex, iconHex, lightHex)
    fun setCustomLightEnabled(enabled: Boolean) = repository.setCustomLightEnabled(enabled)
    fun restoreBaseCustomColors() = repository.restoreBaseCustomColors()
    fun restoreOriginalTheme() = repository.restoreOriginalTheme()
    fun undoCustomAppearance(): Boolean = repository.undoCustomAppearance()

    // Theme Store Controls (Cloud-backed via Firestore when Google-authenticated + offline fallback)
    fun syncCloudStoreIfSignedIn(uid: String?) {
        if (uid == null) {
            cloudStoreSyncJob?.cancel()
            cloudStoreSyncJob = null
            syncedStoreUid = null
            _cloudPublishedThemes.value = emptyList()
            _cloudAcquiredThemeIds.value = emptySet()
            return
        }
        if (syncedStoreUid == uid && cloudStoreSyncJob?.isActive == true) return
        cloudStoreSyncJob?.cancel()
        syncedStoreUid = uid
        repository.syncCloudAccountOnSignIn()
        cloudStoreSyncJob = viewModelScope.launch {
            launch {
                themeStoreRepository.observePublishedThemes()
                    .catch { /* Handled via handleFirestoreError; keep local/curated store resilient */ }
                    .collect { items ->
                        _cloudPublishedThemes.value = items
                    }
            }
            launch {
                themeStoreRepository.observeAcquiredThemes()
                    .catch { /* Keep local acquired fallback intact */ }
                    .collect { records: List<AcquiredThemeRecord> ->
                        _cloudAcquiredThemeIds.value = records.map { it.themeId }.toSet()
                    }
            }
        }
    }

    fun clearStoreStatusMessage() {
        _storeStatusMessage.value = null
    }

    fun clearJustAcquiredHighlight() {
        _justAcquiredThemeId.value = null
    }

    fun acquireFromStoreAndMoveToAppearance(item: StoreThemeItem, isDarkForCinematic: Boolean = true) {
        acquireAndApplyStoreTheme(item = item, isDarkForCinematic = isDarkForCinematic)
        _justAcquiredThemeId.value = item.id
        navigateTo(Screen.APPEARANCE)
    }

    fun removeDownloadedTheme(item: StoreThemeItem) {
        if (item.id == JesrRepository.BASE_THEME_ID) return
        repository.unacquireThemeLocally(item.id)
    }

    fun applyCinematicTheme(themeId: String, isDark: Boolean) {
        repository.applyCinematicTheme(themeId, isDark)
        val spec = CinematicThemesCatalog.findById(themeId) ?: return
        val isEn = appLanguage.value == AppLanguage.EN
        val name = if (isEn) spec.titleEn else spec.titleAr
        val modeLabel = if (isDark) {
            if (isEn) "Dark" else "مظلم"
        } else {
            if (isEn) "Light" else "مضيء"
        }
        _storeStatusMessage.value = if (isEn) {
            "Applied \"$name\" ($modeLabel) for free"
        } else {
            "تم تطبيق «$name» ($modeLabel) مجاناً"
        }
    }

    fun acquireAndApplyStoreTheme(item: StoreThemeItem, isDarkForCinematic: Boolean = true) {
        if (CinematicThemesCatalog.findById(item.id) != null) {
            applyCinematicTheme(item.id, isDark = isDarkForCinematic)
            return
        }
        repository.markThemeAcquiredLocally(item.id)
        repository.applyStoreThemeAppearance(
            backgroundHex = item.backgroundHex,
            iconHex = item.iconHex,
            lightHex = item.lightHex,
            lightEnabled = item.lightEnabled,
            themeId = item.id,
            preferDarkForCinematic = isDarkForCinematic
        )
        if (currentFirebaseUid() != null) {
            viewModelScope.launch {
                themeStoreRepository.acquireTheme(item)
            }
        }
        val isEn = appLanguage.value == AppLanguage.EN
        val displayTitle = when (item.id) {
            JesrRepository.BASE_THEME_ID -> if (isEn) "Original Theme (Base Interface)" else item.title
            else -> item.title
        }
        _storeStatusMessage.value = when {
            item.id == JesrRepository.BASE_THEME_ID ->
                if (isEn) "Restored Original Theme (Base Interface)" else "تمت استعادة الثيم الأصلي (الواجهة الأساسية)"
            else ->
                if (isEn) "Applied \"$displayTitle\" for free" else "تم تطبيق «$displayTitle» مجاناً"
        }
    }

    fun publishCustomThemeToStore(
        title: String,
        description: String,
        priceUsd: Int,
        backgroundHex: Long,
        iconHex: Long,
        lightHex: Long,
        lightEnabled: Boolean
    ) {
        val isEn = appLanguage.value == AppLanguage.EN
        val normalizedPrice = if (priceUsd == 1) 1 else 0
        val cleanTitle = title.trim().ifEmpty { if (isEn) "My Custom Theme" else "واجهتي المخصصة" }.take(60)
        val cleanDesc = description.trim().take(180)
        val creatorName = userDisplayName.value.ifBlank { if (isEn) "Jesr Designer" else "مصمم جسر" }.take(60)
        val currentFirebaseUid = currentFirebaseUid()

        viewModelScope.launch {
            _isStoreBusy.value = true
            if (currentFirebaseUid != null) {
                val res = themeStoreRepository.publishTheme(
                    title = cleanTitle,
                    description = cleanDesc,
                    backgroundHex = backgroundHex,
                    iconHex = iconHex,
                    lightHex = lightHex,
                    lightEnabled = lightEnabled,
                    priceUsd = normalizedPrice,
                    creatorName = creatorName
                )
                _isStoreBusy.value = false
                if (res.isSuccess) {
                    val publishedId = res.getOrNull() ?: "cloud_${UUID.randomUUID()}"
                    repository.markThemeAcquiredLocally(publishedId)
                    _storeStatusMessage.value = if (normalizedPrice == 1) {
                        if (isEn) "Listed \"$cleanTitle\" in the Store for $1" else "تم عرض «$cleanTitle» للبيع في المتجر مقابل 1$"
                    } else {
                        if (isEn) "Published \"$cleanTitle\" for free in Jesr Store" else "تم نشر «$cleanTitle» مجاناً في متجر جسر"
                    }
                } else {
                    val localItem = StoreThemeItem(
                        id = "local_${UUID.randomUUID()}",
                        creatorId = currentFirebaseUid,
                        creatorName = creatorName,
                        title = cleanTitle,
                        description = cleanDesc,
                        backgroundHex = backgroundHex,
                        iconHex = iconHex,
                        lightHex = lightHex,
                        lightEnabled = lightEnabled,
                        priceUsd = normalizedPrice,
                        isPublished = true
                    )
                    repository.saveLocalPublishedTheme(localItem)
                    repository.markThemeAcquiredLocally(localItem.id)
                    _storeStatusMessage.value = if (isEn) {
                        "Saved \"$cleanTitle\" in local Store list"
                    } else {
                        "تم حفظ «$cleanTitle» في قائمة المتجر المحلية"
                    }
                }
            } else {
                val localItem = StoreThemeItem(
                    id = "local_${UUID.randomUUID()}",
                    creatorId = myJesrId,
                    creatorName = creatorName,
                    title = cleanTitle,
                    description = cleanDesc,
                    backgroundHex = backgroundHex,
                    iconHex = iconHex,
                    lightHex = lightHex,
                    lightEnabled = lightEnabled,
                    priceUsd = normalizedPrice,
                    isPublished = true
                )
                repository.saveLocalPublishedTheme(localItem)
                repository.markThemeAcquiredLocally(localItem.id)
                _isStoreBusy.value = false
                _storeStatusMessage.value = if (isEn) {
                    "Listed \"$cleanTitle\" (${if (normalizedPrice == 1) "$1" else "Free"}) • Sign in with Google for cloud sync"
                } else {
                    "تم عرض «$cleanTitle» (${if (normalizedPrice == 1) "1$" else "مجاني"}) • سجّل دخول Google للمزامنة السحابية"
                }
            }
        }
    }

    fun deleteMyStoreTheme(item: StoreThemeItem) {
        val isEn = appLanguage.value == AppLanguage.EN
        // Protected Base Color and system curated themes can NEVER be deleted
        if (item.id == JesrRepository.BASE_THEME_ID || item.id.startsWith("curated_") || item.creatorId == "system") {
            _storeStatusMessage.value = if (isEn) {
                "Base theme is protected and cannot be deleted"
            } else {
                "اللون الأساسي محمي ولا يمكن حذفه"
            }
            return
        }
        viewModelScope.launch {
            repository.deleteLocalPublishedTheme(item.id)
            val currentUid = currentFirebaseUid()
            if (currentUid != null && item.creatorId == currentUid) {
                themeStoreRepository.deletePublishedTheme(item.id)
            }
            _storeStatusMessage.value = if (isEn) {
                "Removed \"${item.title}\" from Store"
            } else {
                "تمت إزالة «${item.title}» من المتجر"
            }
        }
    }

    fun getEstimatedDistanceToNeighbor(peerId: String): Float? {
        val neighbor = neighbors.value[peerId] ?: return null
        return DistanceEstimator.estimateMeters(neighbor.rssi)
    }
}
