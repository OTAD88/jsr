package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.crypto.JesrCrypto
import com.example.data.AppDatabase
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.example.data.entity.ConversationEntity
import com.example.data.entity.FriendEntity
import com.example.data.entity.MessageEntity
import com.example.data.model.AppLanguage
import com.example.data.model.AppLockMethod
import com.example.data.model.AppearanceMode
import com.example.data.model.DeliveryStatus
import com.example.data.model.FriendStatus
import com.example.data.model.MeshPacket
import com.example.data.model.MessageType
import com.example.data.model.TransportType
import com.example.network.MeshForegroundService
import com.example.network.ble.BleMeshManager
import com.example.network.model.PeerNode
import com.example.network.routing.MeshRouter
import com.example.network.wifip2p.WifiDirectMeshManager
import com.example.security.AppLockManager
import com.example.ui.theme.CinematicThemesCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class CustomAppearanceSnapshot(
    val mode: AppearanceMode = AppearanceMode.CUSTOM,
    val backgroundHex: Long?,
    val iconHex: Long?,
    val lightHex: Long?,
    val lightEnabled: Boolean,
    val cinematicThemeId: String? = null
)

data class RequestPrivacyRule(
    val jesrId: String,
    val muteOverride: Boolean? = null,
    val blockOverride: Boolean? = null
)

sealed class AccountLookupResult {
    data object Idle : AccountLookupResult()
    data class Found(
        val jesrId: String,
        val displayName: String,
        val publicKeyBase64: String,
        val isNearby: Boolean,
        val status: FriendStatus?
    ) : AccountLookupResult()
    data class NotFound(val queriedId: String) : AccountLookupResult()
}

class JesrRepository(private val context: Context) {
    private val TAG = "JesrRepository"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val database = AppDatabase.getDatabase(context)
    val messageDao = database.messageDao()
    val conversationDao = database.conversationDao()
    val friendDao = database.friendDao()
    val relayQueueDao = database.relayQueueDao()
    val seenPacketDao = database.seenPacketDao()

    val crypto = JesrCrypto(context)
    val myJesrId: String get() = crypto.myJesrId
    val myPublicKeyBase64: String get() = crypto.myPublicKeyBase64
    private val _myJesrIdFlow = MutableStateFlow(crypto.myJesrId)
    val myJesrIdFlow = _myJesrIdFlow.asStateFlow()

    private val cloudSync: CloudAccountSyncRepository by lazy { CloudAccountSyncRepository(context) }

    private val prefs = context.getSharedPreferences("jesr_prefs", Context.MODE_PRIVATE)
    private val KEY_USER_NAME = "key_user_display_name"
    private val KEY_USER_AVATAR = "key_user_avatar_path"
    private val KEY_USER_COVER = "key_user_cover_path"
    private val KEY_GLOBAL_DISTANCE = "key_global_distance_allowed"
    private val KEY_GLOBAL_MUTUAL = "key_global_mutual_friends_allowed"
    private val KEY_NEARBY_VISIBLE = "key_nearby_visibility_enabled"
    private val KEY_GLOBAL_MUTE_REQ = "key_global_mute_requests"
    private val KEY_GLOBAL_BLOCK_REQ = "key_global_block_requests"
    private val KEY_REQ_PRIVACY_RULES = "key_request_privacy_rules_json"
    private val KEY_APPEARANCE_MODE = "key_appearance_mode"
    private val KEY_CUSTOM_BG_COLOR = "key_custom_bg_color_hex"
    private val KEY_CUSTOM_ICON_COLOR = "key_custom_icon_color_hex"
    private val KEY_CUSTOM_LIGHT_COLOR = "key_custom_light_color_hex"
    private val KEY_CUSTOM_LIGHT_ENABLED = "key_custom_light_enabled"
    private val KEY_CINEMATIC_THEME_ID = "key_cinematic_theme_id"
    private val KEY_LOCAL_STORE_THEMES = "key_local_store_themes_json"
    private val KEY_LOCAL_ACQUIRED_THEMES = "key_local_acquired_theme_ids_json"
    private val KEY_RESET_TO_ORIGINAL_V3 = "key_reset_to_original_theme_v3"
    private val KEY_APP_LANGUAGE = "key_app_language"
    private val KEY_APP_LOCK_METHOD = "key_app_lock_method"
    private val KEY_APP_LOCK_SALT = "key_app_lock_salt"
    private val KEY_APP_LOCK_HASH = "key_app_lock_hash"

    private val _isMeshActive = MutableStateFlow(false)
    val isMeshActive = _isMeshActive.asStateFlow()

    private val _appLanguage = MutableStateFlow(
        try {
            AppLanguage.valueOf(prefs.getString(KEY_APP_LANGUAGE, AppLanguage.AR.name) ?: AppLanguage.AR.name)
        } catch (_: Exception) {
            AppLanguage.AR
        }
    )
    val appLanguage = _appLanguage.asStateFlow()

    private val _appLockMethod = MutableStateFlow(
        try {
            AppLockMethod.valueOf(prefs.getString(KEY_APP_LOCK_METHOD, AppLockMethod.NONE.name) ?: AppLockMethod.NONE.name)
        } catch (_: Exception) {
            AppLockMethod.NONE
        }
    )
    val appLockMethod = _appLockMethod.asStateFlow()

    private val _isAppLocked = MutableStateFlow(_appLockMethod.value != AppLockMethod.NONE)
    val isAppLocked = _isAppLocked.asStateFlow()

    private val _userDisplayName = MutableStateFlow(
        prefs.getString(KEY_USER_NAME, "مستخدم جسر") ?: "مستخدم جسر"
    )
    val userDisplayName = _userDisplayName.asStateFlow()

    private val _userAvatarPath = MutableStateFlow(prefs.getString(KEY_USER_AVATAR, null))
    val userAvatarPath = _userAvatarPath.asStateFlow()

    private val _userCoverPath = MutableStateFlow(prefs.getString(KEY_USER_COVER, null))
    val userCoverPath = _userCoverPath.asStateFlow()

    // Section 7: All global permissions disabled by default
    private val _globalDistanceAllowed = MutableStateFlow(prefs.getBoolean(KEY_GLOBAL_DISTANCE, false))
    val globalDistanceAllowed = _globalDistanceAllowed.asStateFlow()

    private val _globalMutualFriendsAllowed = MutableStateFlow(prefs.getBoolean(KEY_GLOBAL_MUTUAL, false))
    val globalMutualFriendsAllowed = _globalMutualFriendsAllowed.asStateFlow()

    // Section 8: Nearby visibility explicitly opt-in (false by default)
    private val _isNearbyVisibilityEnabled = MutableStateFlow(prefs.getBoolean(KEY_NEARBY_VISIBLE, false))
    val isNearbyVisibilityEnabled = _isNearbyVisibilityEnabled.asStateFlow()

    // Section 10: Friend request mute & block controls
    private val _globalMuteRequests = MutableStateFlow(prefs.getBoolean(KEY_GLOBAL_MUTE_REQ, false))
    val globalMuteRequests = _globalMuteRequests.asStateFlow()

    private val _globalBlockRequests = MutableStateFlow(prefs.getBoolean(KEY_GLOBAL_BLOCK_REQ, false))
    val globalBlockRequests = _globalBlockRequests.asStateFlow()

    private val _requestPrivacyRules = MutableStateFlow(loadRequestRules())
    val requestPrivacyRules = _requestPrivacyRules.asStateFlow()

    // Section 13: Appearance Mode & Two-Step Color Picker (Background Color + Icon Color)
    private val _appearanceMode = MutableStateFlow(
        try {
            AppearanceMode.valueOf(prefs.getString(KEY_APPEARANCE_MODE, AppearanceMode.AUTO.name) ?: AppearanceMode.AUTO.name)
        } catch (e: Exception) {
            AppearanceMode.AUTO
        }
    )
    val appearanceMode = _appearanceMode.asStateFlow()

    private val _customBackgroundHex = MutableStateFlow<Long?>(
        if (prefs.contains(KEY_CUSTOM_BG_COLOR)) prefs.getLong(KEY_CUSTOM_BG_COLOR, 0L) else null
    )
    val customBackgroundHex = _customBackgroundHex.asStateFlow()

    private val _customIconHex = MutableStateFlow<Long?>(
        if (prefs.contains(KEY_CUSTOM_ICON_COLOR)) prefs.getLong(KEY_CUSTOM_ICON_COLOR, 0L) else null
    )
    val customIconHex = _customIconHex.asStateFlow()

    private val _customLightHex = MutableStateFlow<Long?>(
        if (prefs.contains(KEY_CUSTOM_LIGHT_COLOR)) prefs.getLong(KEY_CUSTOM_LIGHT_COLOR, 0L) else null
    )
    val customLightHex = _customLightHex.asStateFlow()

    private val _customLightEnabled = MutableStateFlow(prefs.getBoolean(KEY_CUSTOM_LIGHT_ENABLED, true))
    val customLightEnabled = _customLightEnabled.asStateFlow()

    private val _activeCinematicThemeId = MutableStateFlow(prefs.getString(KEY_CINEMATIC_THEME_ID, null))
    val activeCinematicThemeId = _activeCinematicThemeId.asStateFlow()

    private val _customUndoStack = MutableStateFlow<List<CustomAppearanceSnapshot>>(emptyList())
    val customUndoStack = _customUndoStack.asStateFlow()

    private val _canUndoCustomAppearance = MutableStateFlow(false)
    val canUndoCustomAppearance = _canUndoCustomAppearance.asStateFlow()

    private val _localPublishedThemes = MutableStateFlow(loadLocalStoreThemes())
    val localPublishedThemes = _localPublishedThemes.asStateFlow()

    private val _localAcquiredThemeIds = MutableStateFlow(loadLocalAcquiredIds())
    val localAcquiredThemeIds = _localAcquiredThemeIds.asStateFlow()

    private lateinit var bleManager: BleMeshManager
    private lateinit var wifiManager: WifiDirectMeshManager
    private lateinit var router: MeshRouter

    val neighbors: Flow<Map<String, PeerNode>> get() = router.neighbors
    val conversations: Flow<List<ConversationEntity>> = conversationDao.getAllConversations()
    val friends: Flow<List<FriendEntity>> = friendDao.getAllFriends()
    val allFriendEntries: Flow<List<FriendEntity>> = friendDao.getAllEntries()
    val pendingReceivedRequests: Flow<List<FriendEntity>> = friendDao.getPendingReceivedRequests()
    val pendingSentRequests: Flow<List<FriendEntity>> = friendDao.getPendingSentRequests()
    val queuedCount: Flow<Int> = relayQueueDao.getQueueCount()
    val deliveredCount: Flow<Int> = messageDao.getDeliveredMessagesCount()
    val relayedCount: Flow<Int> get() = router.relayedPacketsCount

    private val _incomingFriendRequestSender = MutableStateFlow<String?>(null)
    val incomingFriendRequestSender = _incomingFriendRequestSender.asStateFlow()

    init {
        if (!prefs.getBoolean(KEY_RESET_TO_ORIGINAL_V3, false)) {
            prefs.edit()
                .putString(KEY_APPEARANCE_MODE, AppearanceMode.AUTO.name)
                .remove(KEY_CUSTOM_BG_COLOR)
                .remove(KEY_CUSTOM_ICON_COLOR)
                .remove(KEY_CUSTOM_LIGHT_COLOR)
                .putBoolean(KEY_CUSTOM_LIGHT_ENABLED, true)
                .putBoolean(KEY_RESET_TO_ORIGINAL_V3, true)
                .apply()
            _appearanceMode.value = AppearanceMode.AUTO
            _customBackgroundHex.value = null
            _customIconHex.value = null
            _customLightHex.value = null
            _customLightEnabled.value = true
        }
        initEngine()
        seedBroadcastConversation()
    }

    private fun loadRequestRules(): Map<String, RequestPrivacyRule> {
        val raw = prefs.getString(KEY_REQ_PRIVACY_RULES, null) ?: return emptyMap()
        return try {
            val obj = JSONObject(raw)
            val map = mutableMapOf<String, RequestPrivacyRule>()
            obj.keys().forEach { key ->
                val item = obj.getJSONObject(key)
                val mute = if (item.has("mute") && !item.isNull("mute")) item.getBoolean("mute") else null
                val block = if (item.has("block") && !item.isNull("block")) item.getBoolean("block") else null
                map[key] = RequestPrivacyRule(jesrId = key, muteOverride = mute, blockOverride = block)
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun saveRequestRules(rules: Map<String, RequestPrivacyRule>) {
        val root = JSONObject()
        rules.forEach { (id, rule) ->
            if (rule.muteOverride != null || rule.blockOverride != null) {
                val obj = JSONObject()
                if (rule.muteOverride != null) obj.put("mute", rule.muteOverride)
                if (rule.blockOverride != null) obj.put("block", rule.blockOverride)
                root.put(id, obj)
            }
        }
        prefs.edit().putString(KEY_REQ_PRIVACY_RULES, root.toString()).apply()
    }

    fun isRequestBlockedFor(senderId: String): Boolean {
        val rule = _requestPrivacyRules.value[senderId.uppercase()]
        return rule?.blockOverride ?: _globalBlockRequests.value
    }

    fun isRequestMutedFor(senderId: String): Boolean {
        val rule = _requestPrivacyRules.value[senderId.uppercase()]
        return rule?.muteOverride ?: _globalMuteRequests.value
    }

    suspend fun getEffectiveDistancePermission(peerId: String): Boolean {
        val friend = friendDao.getFriend(peerId)
        return friend?.distancePermissionOverride ?: _globalDistanceAllowed.value
    }

    fun dismissIncomingFriendRequest() {
        _incomingFriendRequestSender.value = null
    }

    fun updateUserDisplayName(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        prefs.edit().putString(KEY_USER_NAME, clean).apply()
        _userDisplayName.value = clean
        pushProfileToCloudAsync()
    }

    fun updateUserAvatarFromUri(uri: Uri) {
        scope.launch {
            try {
                val dest = File(context.filesDir, "user_profile_avatar_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(dest).use { output ->
                        input.copyTo(output)
                    }
                }
                // Remove older avatar files
                context.filesDir.listFiles()?.forEach { f ->
                    if (f.name.startsWith("user_profile_avatar_") && f.absolutePath != dest.absolutePath) {
                        f.delete()
                    }
                }
                prefs.edit().putString(KEY_USER_AVATAR, dest.absolutePath).apply()
                _userAvatarPath.value = dest.absolutePath
                pushProfileToCloudAsync()
            } catch (e: Exception) {
                Log.e(TAG, "Failed saving avatar from Photo Picker", e)
            }
        }
    }

    fun updateUserAvatarFromBitmap(bitmap: Bitmap) {
        scope.launch {
            try {
                val dest = File(context.filesDir, "user_profile_avatar_${System.currentTimeMillis()}.jpg")
                FileOutputStream(dest).use { output ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
                }
                context.filesDir.listFiles()?.forEach { f ->
                    if (f.name.startsWith("user_profile_avatar_") && f.absolutePath != dest.absolutePath) {
                        f.delete()
                    }
                }
                prefs.edit().putString(KEY_USER_AVATAR, dest.absolutePath).apply()
                _userAvatarPath.value = dest.absolutePath
                pushProfileToCloudAsync()
            } catch (e: Exception) {
                Log.e(TAG, "Failed saving cropped avatar bitmap", e)
            }
        }
    }

    fun updateUserCoverFromBitmap(bitmap: Bitmap) {
        scope.launch {
            try {
                val dest = File(context.filesDir, "user_profile_cover_${System.currentTimeMillis()}.jpg")
                FileOutputStream(dest).use { output ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
                }
                context.filesDir.listFiles()?.forEach { f ->
                    if (f.name.startsWith("user_profile_cover_") && f.absolutePath != dest.absolutePath) {
                        f.delete()
                    }
                }
                prefs.edit().putString(KEY_USER_COVER, dest.absolutePath).apply()
                _userCoverPath.value = dest.absolutePath
                pushProfileToCloudAsync()
            } catch (e: Exception) {
                Log.e(TAG, "Failed saving cropped cover bitmap", e)
            }
        }
    }

    fun clearUserCover() {
        scope.launch {
            try {
                context.filesDir.listFiles()?.forEach { f ->
                    if (f.name.startsWith("user_profile_cover_")) f.delete()
                }
                prefs.edit().remove(KEY_USER_COVER).apply()
                _userCoverPath.value = null
                pushProfileToCloudAsync()
            } catch (e: Exception) {
                Log.e(TAG, "Failed clearing cover", e)
            }
        }
    }

    fun setAppLanguage(language: AppLanguage) {
        if (_appLanguage.value == language) return
        prefs.edit().putString(KEY_APP_LANGUAGE, language.name).apply()
        _appLanguage.value = language
        pushProfileToCloudAsync()
    }

    fun hasAppLockSecret(): Boolean = !prefs.getString(KEY_APP_LOCK_HASH, null).isNullOrBlank()

    fun disableAppLock() {
        prefs.edit()
            .putString(KEY_APP_LOCK_METHOD, AppLockMethod.NONE.name)
            .remove(KEY_APP_LOCK_SALT)
            .remove(KEY_APP_LOCK_HASH)
            .apply()
        _appLockMethod.value = AppLockMethod.NONE
        _isAppLocked.value = false
    }

    fun configureAppLockBiometric() {
        prefs.edit()
            .putString(KEY_APP_LOCK_METHOD, AppLockMethod.BIOMETRIC.name)
            .remove(KEY_APP_LOCK_SALT)
            .remove(KEY_APP_LOCK_HASH)
            .apply()
        _appLockMethod.value = AppLockMethod.BIOMETRIC
        _isAppLocked.value = false
    }

    fun configureAppLockSecret(method: AppLockMethod, secret: String) {
        val salt = AppLockManager.generateSalt()
        val hash = AppLockManager.hashSecret(secret, salt)
        prefs.edit()
            .putString(KEY_APP_LOCK_METHOD, method.name)
            .putString(KEY_APP_LOCK_SALT, salt)
            .putString(KEY_APP_LOCK_HASH, hash)
            .apply()
        _appLockMethod.value = method
        _isAppLocked.value = false
    }

    fun verifyAppLockSecret(candidate: String): Boolean {
        val salt = prefs.getString(KEY_APP_LOCK_SALT, null)
        val expected = prefs.getString(KEY_APP_LOCK_HASH, null)
        val ok = AppLockManager.verifySecret(candidate, salt, expected)
        if (ok) {
            _isAppLocked.value = false
        }
        return ok
    }

    fun unlockWithBiometric() {
        _isAppLocked.value = false
    }

    fun lockAppNow() {
        if (_appLockMethod.value != AppLockMethod.NONE) {
            _isAppLocked.value = true
        }
    }

    fun setGlobalDistanceAllowed(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GLOBAL_DISTANCE, enabled).apply()
        _globalDistanceAllowed.value = enabled
        broadcastConsentUpdatesToActivePeers()
        pushProfileToCloudAsync()
    }

    fun setGlobalMutualFriendsAllowed(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GLOBAL_MUTUAL, enabled).apply()
        _globalMutualFriendsAllowed.value = enabled
        pushProfileToCloudAsync()
    }

    fun setFriendDistanceOverride(jesrId: String, overrideVal: Boolean?) {
        scope.launch {
            friendDao.updateDistanceOverride(jesrId, overrideVal)
            val effective = overrideVal ?: _globalDistanceAllowed.value
            val packet = MeshPacket(
                id = UUID.randomUUID().toString(),
                senderId = myJesrId,
                recipientId = jesrId,
                type = MessageType.DISTANCE_CONSENT_UPDATE,
                payload = effective.toString(),
                distanceConsent = effective,
                ttl = 2
            ).signedWith(crypto)
            router.routePacket(packet)
            friendDao.getFriend(jesrId)?.let { cloudSyncIfSignedIn { cloudSync.syncFriend(it) } }
        }
    }

    fun setFriendMutualFriendsOverride(jesrId: String, overrideVal: Boolean?) {
        scope.launch {
            friendDao.updateMutualFriendsOverride(jesrId, overrideVal)
        }
    }

    fun setNearbyVisibilityEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NEARBY_VISIBLE, enabled).apply()
        _isNearbyVisibilityEnabled.value = enabled
        pushProfileToCloudAsync()
    }

    fun setGlobalMuteRequests(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GLOBAL_MUTE_REQ, enabled).apply()
        _globalMuteRequests.value = enabled
    }

    fun setGlobalBlockRequests(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GLOBAL_BLOCK_REQ, enabled).apply()
        _globalBlockRequests.value = enabled
    }

    fun setUserRequestMuteRule(jesrId: String, muteOverride: Boolean?) {
        val key = jesrId.trim().uppercase()
        if (key.isEmpty()) return
        val current = _requestPrivacyRules.value.toMutableMap()
        val prev = current[key] ?: RequestPrivacyRule(key)
        val updated = prev.copy(muteOverride = muteOverride)
        if (updated.muteOverride == null && updated.blockOverride == null) {
            current.remove(key)
        } else {
            current[key] = updated
        }
        _requestPrivacyRules.value = current
        saveRequestRules(current)
        scope.launch { friendDao.updateMuteRequestsOverride(key, muteOverride) }
    }

    fun setUserRequestBlockRule(jesrId: String, blockOverride: Boolean?) {
        val key = jesrId.trim().uppercase()
        if (key.isEmpty()) return
        val current = _requestPrivacyRules.value.toMutableMap()
        val prev = current[key] ?: RequestPrivacyRule(key)
        val updated = prev.copy(blockOverride = blockOverride)
        if (updated.muteOverride == null && updated.blockOverride == null) {
            current.remove(key)
        } else {
            current[key] = updated
        }
        _requestPrivacyRules.value = current
        saveRequestRules(current)
        scope.launch { friendDao.updateBlockRequestsOverride(key, blockOverride) }
    }

    fun deleteAllIncomingRequests() {
        scope.launch {
            friendDao.deleteAllIncomingRequests()
        }
    }

    fun setAppearanceMode(mode: AppearanceMode) {
        val editor = prefs.edit().putString(KEY_APPEARANCE_MODE, mode.name)
        if (mode == AppearanceMode.AUTO) {
            editor.remove(KEY_CINEMATIC_THEME_ID)
            _activeCinematicThemeId.value = null
        }
        editor.apply()
        _appearanceMode.value = mode
    }

    private fun currentSnapshot(): CustomAppearanceSnapshot {
        return CustomAppearanceSnapshot(
            mode = _appearanceMode.value,
            backgroundHex = _customBackgroundHex.value,
            iconHex = _customIconHex.value,
            lightHex = _customLightHex.value,
            lightEnabled = _customLightEnabled.value,
            cinematicThemeId = _activeCinematicThemeId.value
        )
    }

    private fun pushCustomSnapshot() {
        val snap = currentSnapshot()
        val existing = _customUndoStack.value
        if (existing.lastOrNull() == snap) return
        val updated = (existing + snap).takeLast(30)
        _customUndoStack.value = updated
        _canUndoCustomAppearance.value = updated.isNotEmpty()
    }

    private fun applySnapshotImmediate(snapshot: CustomAppearanceSnapshot) {
        val editor = prefs.edit()
        editor.putString(KEY_APPEARANCE_MODE, snapshot.mode.name)
        if (snapshot.backgroundHex == null) editor.remove(KEY_CUSTOM_BG_COLOR) else editor.putLong(KEY_CUSTOM_BG_COLOR, snapshot.backgroundHex)
        if (snapshot.iconHex == null) editor.remove(KEY_CUSTOM_ICON_COLOR) else editor.putLong(KEY_CUSTOM_ICON_COLOR, snapshot.iconHex)
        if (snapshot.lightHex == null) editor.remove(KEY_CUSTOM_LIGHT_COLOR) else editor.putLong(KEY_CUSTOM_LIGHT_COLOR, snapshot.lightHex)
        editor.putBoolean(KEY_CUSTOM_LIGHT_ENABLED, snapshot.lightEnabled)
        if (snapshot.cinematicThemeId == null) editor.remove(KEY_CINEMATIC_THEME_ID) else editor.putString(KEY_CINEMATIC_THEME_ID, snapshot.cinematicThemeId)
        editor.apply()

        _appearanceMode.value = snapshot.mode
        _customBackgroundHex.value = snapshot.backgroundHex
        _customIconHex.value = snapshot.iconHex
        _customLightHex.value = snapshot.lightHex
        _customLightEnabled.value = snapshot.lightEnabled
        _activeCinematicThemeId.value = snapshot.cinematicThemeId
    }

    fun setCustomBackgroundColor(backgroundHex: Long?) {
        if (_customBackgroundHex.value == backgroundHex && _activeCinematicThemeId.value == null) return
        pushCustomSnapshot()
        val editor = prefs.edit().remove(KEY_CINEMATIC_THEME_ID)
        if (backgroundHex == null) editor.remove(KEY_CUSTOM_BG_COLOR) else editor.putLong(KEY_CUSTOM_BG_COLOR, backgroundHex)
        editor.apply()
        _activeCinematicThemeId.value = null
        _customBackgroundHex.value = backgroundHex
    }

    fun setCustomIconColor(iconHex: Long?) {
        if (_customIconHex.value == iconHex && _activeCinematicThemeId.value == null) return
        pushCustomSnapshot()
        val editor = prefs.edit().remove(KEY_CINEMATIC_THEME_ID)
        if (iconHex == null) editor.remove(KEY_CUSTOM_ICON_COLOR) else editor.putLong(KEY_CUSTOM_ICON_COLOR, iconHex)
        editor.apply()
        _activeCinematicThemeId.value = null
        _customIconHex.value = iconHex
    }

    fun setCustomLightColor(lightHex: Long?) {
        if (_customLightHex.value == lightHex && _activeCinematicThemeId.value == null) return
        pushCustomSnapshot()
        val editor = prefs.edit().remove(KEY_CINEMATIC_THEME_ID)
        if (lightHex == null) editor.remove(KEY_CUSTOM_LIGHT_COLOR) else editor.putLong(KEY_CUSTOM_LIGHT_COLOR, lightHex)
        editor.apply()
        _activeCinematicThemeId.value = null
        _customLightHex.value = lightHex
    }

    fun setCustomColors(backgroundHex: Long?, iconHex: Long?, lightHex: Long?) {
        if (_customBackgroundHex.value == backgroundHex &&
            _customIconHex.value == iconHex &&
            _customLightHex.value == lightHex &&
            _activeCinematicThemeId.value == null
        ) return
        pushCustomSnapshot()
        val editor = prefs.edit().remove(KEY_CINEMATIC_THEME_ID)
        if (backgroundHex == null) editor.remove(KEY_CUSTOM_BG_COLOR) else editor.putLong(KEY_CUSTOM_BG_COLOR, backgroundHex)
        if (iconHex == null) editor.remove(KEY_CUSTOM_ICON_COLOR) else editor.putLong(KEY_CUSTOM_ICON_COLOR, iconHex)
        if (lightHex == null) editor.remove(KEY_CUSTOM_LIGHT_COLOR) else editor.putLong(KEY_CUSTOM_LIGHT_COLOR, lightHex)
        editor.apply()
        _activeCinematicThemeId.value = null
        _customBackgroundHex.value = backgroundHex
        _customIconHex.value = iconHex
        _customLightHex.value = lightHex
    }

    fun setCustomLightEnabled(enabled: Boolean) {
        if (_customLightEnabled.value == enabled) return
        pushCustomSnapshot()
        prefs.edit().putBoolean(KEY_CUSTOM_LIGHT_ENABLED, enabled).apply()
        _customLightEnabled.value = enabled
    }

    fun restoreBaseCustomColors() {
        val target = CustomAppearanceSnapshot(
            mode = AppearanceMode.CUSTOM,
            backgroundHex = BASE_CUSTOM_BG_HEX,
            iconHex = BASE_CUSTOM_ICON_HEX,
            lightHex = BASE_CUSTOM_LIGHT_HEX,
            lightEnabled = BASE_CUSTOM_LIGHT_ENABLED,
            cinematicThemeId = null
        )
        if (currentSnapshot() != target) {
            pushCustomSnapshot()
            applySnapshotImmediate(target)
        } else if (_appearanceMode.value != AppearanceMode.CUSTOM) {
            setAppearanceMode(AppearanceMode.CUSTOM)
        }
    }

    fun restoreOriginalTheme() {
        val target = CustomAppearanceSnapshot(
            mode = AppearanceMode.AUTO,
            backgroundHex = null,
            iconHex = null,
            lightHex = null,
            lightEnabled = true,
            cinematicThemeId = null
        )
        if (currentSnapshot() != target) {
            pushCustomSnapshot()
            applySnapshotImmediate(target)
        }
    }

    fun undoCustomAppearance(): Boolean {
        val stack = _customUndoStack.value
        if (stack.isEmpty()) return false
        val previous = stack.last()
        val remaining = stack.dropLast(1)
        _customUndoStack.value = remaining
        _canUndoCustomAppearance.value = remaining.isNotEmpty()
        applySnapshotImmediate(previous)
        return true
    }

    fun applyCinematicTheme(themeId: String, isDark: Boolean) {
        val spec = CinematicThemesCatalog.findById(themeId) ?: return
        val mode = if (isDark) AppearanceMode.DARK else AppearanceMode.LIGHT
        val keepLightEnabled = if (_activeCinematicThemeId.value == spec.id) _customLightEnabled.value else true
        val target = CustomAppearanceSnapshot(
            mode = mode,
            backgroundHex = null,
            iconHex = null,
            lightHex = null,
            lightEnabled = keepLightEnabled,
            cinematicThemeId = spec.id
        )
        markThemeAcquiredLocally(spec.id)
        if (currentSnapshot() != target) {
            pushCustomSnapshot()
            applySnapshotImmediate(target)
        }
    }

    fun applyStoreThemeAppearance(
        backgroundHex: Long,
        iconHex: Long,
        lightHex: Long,
        lightEnabled: Boolean,
        themeId: String = "",
        preferDarkForCinematic: Boolean = true
    ) {
        if (themeId == BASE_THEME_ID) {
            restoreOriginalTheme()
            return
        }
        if (CinematicThemesCatalog.findById(themeId) != null) {
            applyCinematicTheme(themeId, isDark = preferDarkForCinematic)
            return
        }
        val target = CustomAppearanceSnapshot(
            mode = AppearanceMode.CUSTOM,
            backgroundHex = backgroundHex,
            iconHex = iconHex,
            lightHex = lightHex,
            lightEnabled = lightEnabled,
            cinematicThemeId = null
        )
        if (currentSnapshot() != target) {
            pushCustomSnapshot()
            applySnapshotImmediate(target)
        }
    }

    fun saveLocalPublishedTheme(item: StoreThemeItem) {
        val current = _localPublishedThemes.value.filterNot { it.id == item.id }
        val updated = listOf(item) + current
        _localPublishedThemes.value = updated
        persistLocalStoreThemes(updated)
    }

    fun deleteLocalPublishedTheme(themeId: String): Boolean {
        if (themeId == BASE_THEME_ID || themeId.startsWith("curated_")) return false
        val current = _localPublishedThemes.value
        val updated = current.filterNot { it.id == themeId }
        if (updated.size == current.size) return false
        _localPublishedThemes.value = updated
        persistLocalStoreThemes(updated)
        return true
    }

    fun markThemeAcquiredLocally(themeId: String) {
        val current = _localAcquiredThemeIds.value
        if (current.contains(themeId)) return
        val updated = current + themeId
        _localAcquiredThemeIds.value = updated
        val arr = JSONArray()
        updated.forEach { arr.put(it) }
        prefs.edit().putString(KEY_LOCAL_ACQUIRED_THEMES, arr.toString()).apply()
    }

    fun unacquireThemeLocally(themeId: String): Boolean {
        if (themeId == BASE_THEME_ID) return false
        val current = _localAcquiredThemeIds.value
        if (!current.contains(themeId)) return false
        val updated = current - themeId
        _localAcquiredThemeIds.value = updated
        val arr = JSONArray()
        updated.forEach { arr.put(it) }
        prefs.edit().putString(KEY_LOCAL_ACQUIRED_THEMES, arr.toString()).apply()
        if (_activeCinematicThemeId.value == themeId) {
            restoreOriginalTheme()
        }
        return true
    }

    private fun loadLocalStoreThemes(): List<StoreThemeItem> {
        val raw = prefs.getString(KEY_LOCAL_STORE_THEMES, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<StoreThemeItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    StoreThemeItem(
                        id = obj.optString("id"),
                        creatorId = obj.optString("creatorId", myJesrId),
                        creatorName = obj.optString("creatorName", "مصمم جسر"),
                        title = obj.optString("title", "ثيم مخصص"),
                        description = obj.optString("description", ""),
                        backgroundHex = obj.optLong("backgroundHex", BASE_CUSTOM_BG_HEX),
                        iconHex = obj.optLong("iconHex", BASE_CUSTOM_ICON_HEX),
                        lightHex = obj.optLong("lightHex", BASE_CUSTOM_LIGHT_HEX),
                        lightEnabled = obj.optBoolean("lightEnabled", true),
                        priceUsd = if (obj.optInt("priceUsd", 0) == 1) 1 else 0,
                        isPublished = true
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun persistLocalStoreThemes(list: List<StoreThemeItem>) {
        val arr = JSONArray()
        list.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("creatorId", item.creatorId)
            obj.put("creatorName", item.creatorName)
            obj.put("title", item.title)
            obj.put("description", item.description)
            obj.put("backgroundHex", item.backgroundHex)
            obj.put("iconHex", item.iconHex)
            obj.put("lightHex", item.lightHex)
            obj.put("lightEnabled", item.lightEnabled)
            obj.put("priceUsd", item.priceUsd)
            arr.put(obj)
        }
        prefs.edit().putString(KEY_LOCAL_STORE_THEMES, arr.toString()).apply()
    }

    private fun loadLocalAcquiredIds(): Set<String> {
        val raw = prefs.getString(KEY_LOCAL_ACQUIRED_THEMES, null) ?: return setOf(BASE_THEME_ID)
        return try {
            val arr = JSONArray(raw)
            val set = mutableSetOf(BASE_THEME_ID)
            for (i in 0 until arr.length()) {
                val id = arr.optString(i)
                if (id.isNotBlank()) set.add(id)
            }
            set
        } catch (_: Exception) {
            setOf(BASE_THEME_ID)
        }
    }

    private fun isSignedInToFirebase(): Boolean =
        runCatching { Firebase.auth.currentUser != null }.getOrDefault(false)

    private inline fun cloudSyncIfSignedIn(crossinline block: suspend () -> Unit) {
        if (!isSignedInToFirebase()) return
        scope.launch {
            runCatching { block() }
        }
    }

    private fun readFileScaledBase64(path: String?, maxDim: Int = 480, quality: Int = 62): String {
        if (path.isNullOrBlank()) return ""
        return runCatching {
            val f = File(path)
            if (!f.exists()) return ""
            val orig = BitmapFactory.decodeFile(path) ?: return ""
            val scale = minOf(1f, maxDim.toFloat() / maxOf(orig.width, orig.height).coerceAtLeast(1))
            val bmp = if (scale < 0.99f) {
                Bitmap.createScaledBitmap(orig, (orig.width * scale).toInt().coerceAtLeast(1), (orig.height * scale).toInt().coerceAtLeast(1), true)
            } else orig
            val bos = java.io.ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, quality, bos)
            if (bmp !== orig) bmp.recycle()
            orig.recycle()
            Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP)
        }.getOrDefault("")
    }

    private fun encodeAttachmentCompressedBase64(uriStr: String?): String? {
        if (uriStr.isNullOrBlank()) return null
        return runCatching {
            val inputStream = if (uriStr.startsWith("/")) {
                File(uriStr).inputStream()
            } else {
                context.contentResolver.openInputStream(Uri.parse(uriStr))
            } ?: return null
            val rawBytes = inputStream.use { it.readBytes() }
            val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
            if (decoded != null) {
                val maxDim = 640
                val scale = minOf(1f, maxDim.toFloat() / maxOf(decoded.width, decoded.height).coerceAtLeast(1))
                val scaled = if (scale < 0.99f) {
                    Bitmap.createScaledBitmap(
                        decoded,
                        (decoded.width * scale).toInt().coerceAtLeast(1),
                        (decoded.height * scale).toInt().coerceAtLeast(1),
                        true
                    )
                } else decoded
                val bos = java.io.ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 60, bos)
                if (scaled !== decoded) scaled.recycle()
                decoded.recycle()
                Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP)
            } else if (rawBytes.size <= 160_000) {
                Base64.encodeToString(rawBytes, Base64.NO_WRAP)
            } else {
                null
            }
        }.getOrNull()
    }

    private fun saveReceivedAttachmentFile(messageId: String, fileName: String, base64Data: String): String? {
        return runCatching {
            val bytes = Base64.decode(base64Data, Base64.NO_WRAP)
            val dir = File(context.filesDir, "attachments").apply { mkdirs() }
            val safeName = fileName.replace(Regex("[^a-zA-Z0-9._\\-]"), "_").take(80).ifBlank { "attachment.jpg" }
            val outFile = File(dir, "${messageId}_$safeName")
            FileOutputStream(outFile).use { it.write(bytes) }
            outFile.absolutePath
        }.getOrNull()
    }

    private fun currentIdentitySnapshot(): CloudIdentitySnapshot = CloudIdentitySnapshot(
        displayName = _userDisplayName.value,
        avatarBase64 = readFileScaledBase64(_userAvatarPath.value, 320, 62),
        coverBase64 = readFileScaledBase64(_userCoverPath.value, 640, 58),
        language = _appLanguage.value.name,
        globalDistanceAllowed = _globalDistanceAllowed.value,
        globalMutualFriendsAllowed = _globalMutualFriendsAllowed.value,
        nearbyVisible = _isNearbyVisibilityEnabled.value,
        appearanceMode = _appearanceMode.value.name,
        cinematicThemeId = _activeCinematicThemeId.value ?: ""
    )

    private fun pushProfileToCloudAsync() {
        cloudSyncIfSignedIn {
            cloudSync.pushProfileAndSettingsUpdate(crypto, currentIdentitySnapshot())
        }
    }

    fun syncCloudAccountOnSignIn() {
        if (!isSignedInToFirebase()) return
        scope.launch {
            val res = cloudSync.syncOrRestoreIdentity(crypto, currentIdentitySnapshot())
            val syncResult = res.getOrNull()
            if (syncResult != null) {
                _myJesrIdFlow.value = crypto.myJesrId
                syncResult.remoteProfile?.let { remote ->
                    if (remote.displayName.isNotBlank() && (_userDisplayName.value == "مستخدم جسر" || _userDisplayName.value == "Jesr User")) {
                        prefs.edit().putString(KEY_USER_NAME, remote.displayName).apply()
                        _userDisplayName.value = remote.displayName
                    }
                    if (_userAvatarPath.value == null && remote.avatarBase64.isNotBlank()) {
                        runCatching {
                            val bytes = Base64.decode(remote.avatarBase64, Base64.NO_WRAP)
                            val dest = File(context.filesDir, "user_profile_avatar_cloud.jpg")
                            FileOutputStream(dest).use { it.write(bytes) }
                            prefs.edit().putString(KEY_USER_AVATAR, dest.absolutePath).apply()
                            _userAvatarPath.value = dest.absolutePath
                        }
                    }
                    if (_userCoverPath.value == null && remote.coverBase64.isNotBlank()) {
                        runCatching {
                            val bytes = Base64.decode(remote.coverBase64, Base64.NO_WRAP)
                            val dest = File(context.filesDir, "user_profile_cover_cloud.jpg")
                            FileOutputStream(dest).use { it.write(bytes) }
                            prefs.edit().putString(KEY_USER_COVER, dest.absolutePath).apply()
                            _userCoverPath.value = dest.absolutePath
                        }
                    }
                }
            }

            // Pull cloud backup of friends, conversations, and encrypted/stored messages
            val bundle = cloudSync.fetchCloudBackupBundle().getOrNull() ?: return@launch
            bundle.friends.forEach { remoteFriend ->
                if (friendDao.getFriend(remoteFriend.jesrId) == null) {
                    friendDao.upsertFriend(remoteFriend)
                }
            }
            bundle.conversations.forEach { remoteConv ->
                if (conversationDao.getConversationById(remoteConv.id) == null) {
                    conversationDao.upsertConversation(remoteConv)
                }
            }
            bundle.messages.forEach { remoteMsg ->
                if (messageDao.getMessageById(remoteMsg.id) == null) {
                    messageDao.insertMessage(remoteMsg)
                }
            }
        }
    }

    private fun localNodeMetadataJson(): String {
        return JSONObject().apply {
            put("id", myJesrId)
            put("name", _userDisplayName.value)
            put("pubKey", myPublicKeyBase64)
            put("sigKey", crypto.mySigningPublicKeyBase64)
            put("nearbyVisible", _isNearbyVisibilityEnabled.value)
        }.toString()
    }

    private fun broadcastConsentUpdatesToActivePeers() {
        scope.launch {
            router.neighbors.value.keys.forEach { peerId ->
                val effective = getEffectiveDistancePermission(peerId)
                val packet = MeshPacket(
                    id = UUID.randomUUID().toString(),
                    senderId = myJesrId,
                    recipientId = peerId,
                    type = MessageType.DISTANCE_CONSENT_UPDATE,
                    payload = effective.toString(),
                    distanceConsent = effective,
                    ttl = 2
                ).signedWith(crypto)
                router.routePacket(packet)
            }
        }
    }

    fun requestPeerDistanceConsent(peerId: String) {
        scope.launch {
            val effective = getEffectiveDistancePermission(peerId)
            val packet = MeshPacket(
                id = UUID.randomUUID().toString(),
                senderId = myJesrId,
                recipientId = peerId,
                type = MessageType.DISTANCE_CONSENT_REQUEST,
                payload = effective.toString(),
                distanceConsent = effective,
                ttl = 3
            ).signedWith(crypto)
            router.routePacket(packet)
        }
    }

    private fun initEngine() {
        router = MeshRouter(
            getMyJesrId = { myJesrId },
            crypto = crypto,
            messageDao = messageDao,
            conversationDao = conversationDao,
            friendDao = friendDao,
            relayQueueDao = relayQueueDao,
            seenPacketDao = seenPacketDao,
            isDistanceSharingForPeer = { peerId -> getEffectiveDistancePermission(peerId) },
            isFriendRequestBlocked = { senderId -> isRequestBlockedFor(senderId) },
            isFriendRequestMuted = { senderId -> isRequestMutedFor(senderId) },
            sendViaBle = { target, packet, onResult ->
                if (::bleManager.isInitialized) {
                    bleManager.sendPacket(target, packet, onResult)
                } else {
                    onResult(false)
                }
            },
            sendViaWifi = { targetPeerId, packet, onResult ->
                if (::wifiManager.isInitialized) {
                    wifiManager.sendPacket(targetPeerId, packet, onResult)
                } else {
                    onResult(false)
                }
            },
            decryptPayload = { packet ->
                kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                    val friend = friendDao.getFriend(packet.senderId)
                    val neighbor = router.neighbors.value[packet.senderId]
                    val senderPub = friend?.publicKeyBase64?.takeIf { it.isNotBlank() }
                        ?: neighbor?.publicKeyBase64?.takeIf { it.isNotBlank() }
                        ?: packet.senderPublicKey.takeIf {
                            it.isNotBlank() && crypto.deriveJesrIdFromPublicKeyBase64(it) == packet.senderId
                        }
                    if (senderPub.isNullOrBlank()) {
                        null
                    } else {
                        runCatching {
                            // Try AAD-bound decryption first; fall back to legacy no-AAD for existing packets
                            try {
                                crypto.decryptWithBase64Key(packet.payload, packet.iv, senderPub, aad = packet.aadString())
                            } catch (_: Exception) {
                                crypto.decryptWithBase64Key(packet.payload, packet.iv, senderPub, aad = null)
                            }
                        }.getOrNull()
                    }
                }
            },
            saveReceivedAttachment = { msgId, fileName, b64 ->
                saveReceivedAttachmentFile(msgId, fileName, b64)
            },
            onIncomingFriendRequest = { senderId ->
                _incomingFriendRequestSender.value = senderId
            },
            onDecryptedMessageReceived = { msg ->
                cloudSyncIfSignedIn { cloudSync.syncMessage(msg) }
            }
        )

        bleManager = BleMeshManager(
            context = context,
            getMyJesrId = { myJesrId },
            getMetadataJson = { localNodeMetadataJson() },
            onPacketReceived = { packet -> router.handleIncomingPacket(packet) },
            onPeerDiscovered = { peer -> router.onNeighborDiscovered(peer) }
        )

        wifiManager = WifiDirectMeshManager(
            context = context,
            getMyJesrId = { myJesrId },
            getMetadataJson = { localNodeMetadataJson() },
            onPacketReceived = { packet -> router.handleIncomingPacket(packet) },
            onPeerDiscovered = { peer -> router.onNeighborDiscovered(peer) }
        )
    }

    private fun seedBroadcastConversation() {
        scope.launch {
            if (conversationDao.getConversationById("BROADCAST") == null) {
                conversationDao.upsertConversation(
                    ConversationEntity(
                        id = "BROADCAST",
                        peerName = "قناة البث العام (SOS)",
                        lastMessageText = "قناة البث اللامركزية المفتوحة لجميع الأجهزة القريبة",
                        lastMessageTime = System.currentTimeMillis(),
                        unreadCount = 0,
                        isOnline = true,
                        hopsDistance = 0
                    )
                )
            }
        }
    }

    fun startMesh() {
        if (_isMeshActive.value) return
        _isMeshActive.value = true
        runCatching { MeshForegroundService.start(context) }
        bleManager.startMesh()
        wifiManager.start()
        Log.d(TAG, "Jesr Mesh Network started for ID: $myJesrId")
    }

    fun stopMesh() {
        if (!_isMeshActive.value) return
        _isMeshActive.value = false
        runCatching { MeshForegroundService.stop(context) }
        bleManager.stopMesh()
        wifiManager.stop()
        Log.d(TAG, "Jesr Mesh Network stopped")
    }

    fun refreshMeshDiscovery() {
        if (!_isMeshActive.value) {
            startMesh()
        } else {
            bleManager.stopMesh()
            bleManager.startMesh()
        }
    }

    fun getMessages(conversationId: String): Flow<List<MessageEntity>> =
        messageDao.getMessagesForConversation(conversationId)

    suspend fun lookupAccountById(rawId: String): AccountLookupResult {
        val cleanId = rawId.trim().uppercase()
        if (cleanId.isEmpty() || cleanId == myJesrId) {
            return AccountLookupResult.NotFound(cleanId)
        }
        val neighbor = router.neighbors.value[cleanId]
        val friend = friendDao.getFriend(cleanId)
        if (neighbor != null || friend != null) {
            return AccountLookupResult.Found(
                jesrId = cleanId,
                displayName = friend?.displayName ?: neighbor?.name ?: cleanId,
                publicKeyBase64 = friend?.publicKeyBase64 ?: neighbor?.publicKeyBase64 ?: "",
                isNearby = neighbor != null,
                status = friend?.status
            )
        }
        // Check authenticated Firestore directory if signed in
        if (isSignedInToFirebase()) {
            val cloudRecord = cloudSync.lookupAccountById(cleanId).getOrNull()
            if (cloudRecord != null) {
                return AccountLookupResult.Found(
                    jesrId = cleanId,
                    displayName = cloudRecord.displayName.ifBlank { cleanId },
                    publicKeyBase64 = cloudRecord.ecdhPublicKey,
                    isNearby = false,
                    status = null
                )
            }
        }
        return AccountLookupResult.NotFound(cleanId)
    }

    fun sendMessage(
        recipientId: String,
        text: String,
        isSos: Boolean = false,
        attachmentUri: String? = null,
        attachmentName: String? = null
    ) {
        scope.launch {
            val friend = if (recipientId != "BROADCAST") friendDao.getFriend(recipientId) else null
            if (friend != null && (friend.isBlocked || friend.unblockedAwaitingResume)) {
                return@launch
            }

            val messageId = UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()
            val msgType = if (isSos) MessageType.EMERGENCY_SOS else MessageType.CHAT
            val displayBody = when {
                text.isNotBlank() && attachmentName != null -> "$text\n📎 $attachmentName"
                text.isBlank() && attachmentName != null -> "📎 مرفق: $attachmentName"
                else -> text
            }

            // Encode encrypted V2 attachment payload if an attachment is included
            val attachedBase64 = if (attachmentUri != null && attachmentName != null) {
                encodeAttachmentCompressedBase64(attachmentUri)
            } else null

            val plaintextWirePayload = if (attachedBase64 != null && attachmentName != null) {
                JSONObject().apply {
                    put("v", 2)
                    put("text", displayBody)
                    put("attachmentName", attachmentName)
                    put("attachmentDataBase64", attachedBase64)
                }.toString()
            } else {
                displayBody
            }

            var ciphertext = plaintextWirePayload
            var iv = ""
            var failClosedMissingKey = false

            if (recipientId != "BROADCAST") {
                val peerPubBase64 = friend?.publicKeyBase64?.takeIf { it.isNotBlank() }
                    ?: router.neighbors.value[recipientId]?.publicKeyBase64?.takeIf { it.isNotBlank() }
                    ?: if (isSignedInToFirebase()) {
                        cloudSync.lookupAccountById(recipientId).getOrNull()?.ecdhPublicKey?.takeIf { it.isNotBlank() }
                    } else null

                val pubKey = peerPubBase64?.let { crypto.parsePublicKey(it) }
                if (pubKey != null) {
                    val aad = "$messageId|$myJesrId|$recipientId|${msgType.name}|$timestamp"
                    val enc = crypto.encrypt(plaintextWirePayload, pubKey, aad = aad)
                    ciphertext = enc.ciphertextBase64
                    iv = enc.ivBase64
                } else {
                    failClosedMissingKey = true
                }
            }

            val localAllowed = if (recipientId != "BROADCAST") getEffectiveDistancePermission(recipientId) else false
            val peerConsented = friend?.distanceConsentEnabled == true
            val mutualConsent = localAllowed && peerConsented
            val neighbor = router.neighbors.value[recipientId]
            val distanceMeters = if (mutualConsent && neighbor != null) {
                com.example.network.DistanceEstimator.estimateMeters(neighbor.rssi)
            } else null

            val entity = MessageEntity(
                id = messageId,
                conversationId = recipientId,
                senderId = myJesrId,
                recipientId = recipientId,
                text = displayBody,
                timestamp = timestamp,
                status = if (failClosedMissingKey) DeliveryStatus.FAILED else DeliveryStatus.LOCAL_PENDING,
                isIncoming = false,
                ttl = 7,
                hopCount = 0,
                isBroadcast = recipientId == "BROADCAST",
                isSos = isSos,
                estimatedDistanceMeters = distanceMeters,
                isDistanceConsented = mutualConsent,
                attachmentUri = attachmentUri,
                attachmentName = attachmentName
            )
            messageDao.insertMessage(entity)

            val existing = conversationDao.getConversationById(recipientId)
            val peerName = if (recipientId == "BROADCAST") "قناة البث العام (SOS)" else (friend?.displayName ?: recipientId)
            val convEntity = if (existing == null) {
                ConversationEntity(
                    id = recipientId,
                    peerName = peerName,
                    lastMessageText = displayBody,
                    lastMessageTime = timestamp,
                    unreadCount = 0,
                    isOnline = neighbor != null,
                    hopsDistance = 1
                ).also { conversationDao.upsertConversation(it) }
            } else {
                conversationDao.updateLastMessage(recipientId, displayBody, timestamp)
                existing.copy(lastMessageText = displayBody, lastMessageTime = timestamp)
            }

            cloudSyncIfSignedIn {
                cloudSync.syncConversation(convEntity)
                cloudSync.syncMessage(entity)
            }

            if (failClosedMissingKey) {
                Log.w(TAG, "Refusing unencrypted 1-to-1 send for $recipientId: missing peer E2EE public key")
                return@launch
            }

            val packet = MeshPacket(
                id = messageId,
                senderId = myJesrId,
                recipientId = recipientId,
                type = msgType,
                payload = ciphertext,
                iv = iv,
                ttl = 7,
                hopCount = 0,
                timestamp = timestamp,
                distanceConsent = localAllowed,
                senderRssi = neighbor?.rssi ?: -65
            ).signedWith(crypto)

            router.routePacket(packet)

            if (recipientId == "BROADCAST") {
                messageDao.updateMessageStatus(messageId, DeliveryStatus.DELIVERED)
            }
        }
    }

    fun retryFailedMessage(messageId: String) {
        scope.launch {
            val msg = messageDao.getMessageById(messageId) ?: return@launch
            val msgType = if (msg.isSos) MessageType.EMERGENCY_SOS else MessageType.CHAT
            val localAllowed = if (msg.recipientId != "BROADCAST") getEffectiveDistancePermission(msg.recipientId) else false

            val attachedBase64 = if (msg.attachmentUri != null && msg.attachmentName != null) {
                encodeAttachmentCompressedBase64(msg.attachmentUri)
            } else null

            val wirePayload = if (attachedBase64 != null && msg.attachmentName != null) {
                JSONObject().apply {
                    put("v", 2)
                    put("text", msg.text)
                    put("attachmentName", msg.attachmentName)
                    put("attachmentDataBase64", attachedBase64)
                }.toString()
            } else {
                msg.text
            }

            var ciphertext = wirePayload
            var iv = ""
            if (msg.recipientId != "BROADCAST") {
                val friend = friendDao.getFriend(msg.recipientId)
                val peerPubBase64 = friend?.publicKeyBase64?.takeIf { it.isNotBlank() }
                    ?: router.neighbors.value[msg.recipientId]?.publicKeyBase64?.takeIf { it.isNotBlank() }
                    ?: if (isSignedInToFirebase()) {
                        cloudSync.lookupAccountById(msg.recipientId).getOrNull()?.ecdhPublicKey?.takeIf { it.isNotBlank() }
                    } else null
                val pubKey = peerPubBase64?.let { crypto.parsePublicKey(it) }
                if (pubKey == null) {
                    messageDao.updateMessageStatus(messageId, DeliveryStatus.FAILED)
                    return@launch
                }
                val aad = "${msg.id}|$myJesrId|${msg.recipientId}|${msgType.name}|${msg.timestamp}"
                val enc = crypto.encrypt(wirePayload, pubKey, aad = aad)
                ciphertext = enc.ciphertextBase64
                iv = enc.ivBase64
            }

            messageDao.updateMessageStatus(messageId, DeliveryStatus.SENDING)
            val packet = MeshPacket(
                id = msg.id, // preserve exact ID to guarantee zero duplicates on connection restore
                senderId = myJesrId,
                recipientId = msg.recipientId,
                type = msgType,
                payload = ciphertext,
                iv = iv,
                ttl = 7,
                timestamp = msg.timestamp,
                distanceConsent = localAllowed
            ).signedWith(crypto)
            router.routePacket(packet)
        }
    }

    fun markConversationRead(convId: String) {
        scope.launch {
            conversationDao.markConversationRead(convId)
        }
    }

    fun toggleMuteFriend(jesrId: String) {
        scope.launch {
            val friend = friendDao.getFriend(jesrId) ?: return@launch
            friendDao.updateMuted(jesrId, !friend.isMuted)
            cloudSyncIfSignedIn {
                friendDao.getFriend(jesrId)?.let { cloudSync.syncFriend(it) }
            }
        }
    }

    fun blockFriend(jesrId: String) {
        scope.launch {
            friendDao.updateBlockAndResumeState(
                jesrId = jesrId,
                blocked = true,
                awaitingResume = false,
                localApproved = false,
                peerApproved = false
            )
            cloudSyncIfSignedIn {
                friendDao.getFriend(jesrId)?.let { cloudSync.syncFriend(it) }
            }
        }
    }

    fun unblockFriend(jesrId: String) {
        scope.launch {
            friendDao.updateBlockAndResumeState(
                jesrId = jesrId,
                blocked = false,
                awaitingResume = true,
                localApproved = false,
                peerApproved = false
            )
            cloudSyncIfSignedIn {
                friendDao.getFriend(jesrId)?.let { cloudSync.syncFriend(it) }
            }
        }
    }

    fun approveResumeCommunication(jesrId: String) {
        scope.launch {
            val friend = friendDao.getFriend(jesrId) ?: return@launch
            if (friend.isBlocked) return@launch
            val bothApproved = friend.peerResumeApproved
            friendDao.updateBlockAndResumeState(
                jesrId = jesrId,
                blocked = false,
                awaitingResume = !bothApproved,
                localApproved = true,
                peerApproved = friend.peerResumeApproved
            )

            val packet = MeshPacket(
                id = UUID.randomUUID().toString(),
                senderId = myJesrId,
                recipientId = jesrId,
                type = MessageType.RESUME_APPROVAL,
                payload = "true",
                ttl = 7
            ).signedWith(crypto)
            router.routePacket(packet)

            cloudSyncIfSignedIn {
                friendDao.getFriend(jesrId)?.let { cloudSync.syncFriend(it) }
            }
        }
    }

    fun removeFriendMutually(jesrId: String) {
        scope.launch {
            val packet = MeshPacket(
                id = UUID.randomUUID().toString(),
                senderId = myJesrId,
                recipientId = jesrId,
                type = MessageType.FRIEND_REMOVE,
                payload = "remove",
                ttl = 7
            ).signedWith(crypto)
            router.routePacket(packet)

            messageDao.deleteMessagesForConversation(jesrId)
            conversationDao.deleteConversation(jesrId)
            friendDao.deleteFriend(jesrId)
            cloudSyncIfSignedIn {
                cloudSync.deleteFriendRecord(jesrId)
                cloudSync.deleteConversationRecord(jesrId)
            }
        }
    }

    fun sendFriendRequest(targetJesrId: String, customName: String = "") {
        scope.launch {
            val cleanId = targetJesrId.trim().uppercase()
            val neighbor = router.neighbors.value[cleanId]
            val cloudRecord = if (neighbor?.publicKeyBase64.isNullOrBlank() && isSignedInToFirebase()) {
                cloudSync.lookupAccountById(cleanId).getOrNull()
            } else null

            val displayName = customName.ifEmpty { neighbor?.name ?: cloudRecord?.displayName ?: cleanId }
            val peerPubKey = neighbor?.publicKeyBase64 ?: cloudRecord?.ecdhPublicKey ?: ""
            val existing = friendDao.getFriend(cleanId)
            if (existing?.status == FriendStatus.FRIEND) return@launch

            val entity = FriendEntity(
                jesrId = cleanId,
                displayName = displayName,
                publicKeyBase64 = peerPubKey,
                status = FriendStatus.REQUEST_SENT,
                addedAt = System.currentTimeMillis(),
                mutualFriendsCount = existing?.mutualFriendsCount ?: 0
            )
            friendDao.upsertFriend(entity)
            cloudSyncIfSignedIn { cloudSync.syncFriend(entity) }

            val localAllowed = getEffectiveDistancePermission(cleanId)
            val packet = MeshPacket(
                id = UUID.randomUUID().toString(),
                senderId = myJesrId,
                recipientId = cleanId,
                type = MessageType.FRIEND_REQUEST,
                payload = myPublicKeyBase64,
                distanceConsent = localAllowed,
                ttl = 7
            ).signedWith(crypto)
            router.routePacket(packet)
        }
    }

    fun cancelSentFriendRequest(targetJesrId: String) {
        scope.launch {
            val entry = friendDao.getFriend(targetJesrId)
            if (entry?.status == FriendStatus.REQUEST_SENT) {
                friendDao.deleteFriend(targetJesrId)
                cloudSyncIfSignedIn { cloudSync.deleteFriendRecord(targetJesrId) }
            }
        }
    }

    fun acceptFriendRequest(targetJesrId: String) {
        scope.launch {
            val existing = friendDao.getFriend(targetJesrId)
            friendDao.updateStatus(targetJesrId, FriendStatus.FRIEND)

            val localAllowed = getEffectiveDistancePermission(targetJesrId)
            val packet = MeshPacket(
                id = UUID.randomUUID().toString(),
                senderId = myJesrId,
                recipientId = targetJesrId,
                type = MessageType.FRIEND_ACCEPT,
                payload = myPublicKeyBase64,
                distanceConsent = localAllowed,
                ttl = 7
            ).signedWith(crypto)
            router.routePacket(packet)

            val name = existing?.displayName ?: targetJesrId
            val conv = ConversationEntity(
                id = targetJesrId,
                peerName = name,
                lastMessageText = "تم قبول طلب الصداقة، التواصل جاهز",
                lastMessageTime = System.currentTimeMillis()
            )
            conversationDao.upsertConversation(conv)
            cloudSyncIfSignedIn {
                friendDao.getFriend(targetJesrId)?.let { cloudSync.syncFriend(it) }
                cloudSync.syncConversation(conv)
            }
        }
    }

    fun rejectFriendRequest(targetJesrId: String) {
        scope.launch {
            friendDao.deleteFriend(targetJesrId)
            cloudSyncIfSignedIn { cloudSync.deleteFriendRecord(targetJesrId) }
        }
    }

    fun deleteConversation(id: String) {
        scope.launch {
            messageDao.deleteMessagesForConversation(id)
            conversationDao.deleteConversation(id)
            cloudSyncIfSignedIn { cloudSync.deleteConversationRecord(id) }
        }
    }

    companion object {
        // Protected Base Color of Jesr — permanent and non-deletable
        const val BASE_THEME_ID = "base_jesr_core"
        const val BASE_CUSTOM_BG_HEX = 0xFFD5C3B0L
        const val BASE_CUSTOM_ICON_HEX = 0xFF131312L
        const val BASE_CUSTOM_LIGHT_HEX = 0xFFFFF5E4L
        const val BASE_CUSTOM_LIGHT_ENABLED = true

        val CURATED_STORE_THEMES: List<StoreThemeItem> = listOf(
            StoreThemeItem(
                id = BASE_THEME_ID,
                creatorId = "system",
                creatorName = "جسر الأساسي",
                title = "الثيم الأصلي (الواجهة الأساسية)",
                description = "الواجهة الأساسية الأصلية لتطبيق جسر — مجاني ومحفوظ لا يُحذف",
                backgroundHex = BASE_CUSTOM_BG_HEX,
                iconHex = BASE_CUSTOM_ICON_HEX,
                lightHex = BASE_CUSTOM_LIGHT_HEX,
                lightEnabled = BASE_CUSTOM_LIGHT_ENABLED,
                priceUsd = 0,
                isPublished = true
            ),
            StoreThemeItem(
                id = "curated_olive_grove",
                creatorId = "system",
                creatorName = "استوديو جسر",
                title = "غابات الزيتون الهادئة",
                description = "خلفية زيتونية ناعمة مع إضاءة دافئة وأيقونات داكنة واضحة",
                backgroundHex = 0xFFD6DECAL,
                iconHex = 0xFF1F2B1CL,
                lightHex = 0xFFF7FCEBL,
                lightEnabled = true,
                priceUsd = 0,
                isPublished = true
            ),
            StoreThemeItem(
                id = "curated_moon_studio",
                creatorId = "system",
                creatorName = "استوديو جسر",
                title = "نور القمر الهادئ",
                description = "مظهر ليلي هادئ بأيقونات قمرية فضية وإشعاع ناعم",
                backgroundHex = 0xFF1E2634L,
                iconHex = 0xFFD8E7FFL,
                lightHex = 0xFFC5DCFFL,
                lightEnabled = true,
                priceUsd = 0,
                isPublished = true
            ),
            StoreThemeItem(
                id = "curated_royal_terracotta",
                creatorId = "system",
                creatorName = "استوديو جسر",
                title = "التيراكوتا الملكية",
                description = "واجهة فخارية غنية مع إضاءة عنبرية دافئة",
                backgroundHex = 0xFF9E4B2BL,
                iconHex = 0xFFFFF5E8L,
                lightHex = 0xFFFFD8A8L,
                lightEnabled = true,
                priceUsd = 0,
                isPublished = true
            ),
            StoreThemeItem(
                id = "curated_cobalt_night",
                creatorId = "system",
                creatorName = "استوديو جسر",
                title = "ليالي الكوبالت الفاخرة",
                description = "أزرق ليلي عميق بأيقونات واضحة وإضاءة زرقاء سماوية",
                backgroundHex = 0xFF152544L,
                iconHex = 0xFFE6EEFFL,
                lightHex = 0xFF8AB4FFL,
                lightEnabled = true,
                priceUsd = 0,
                isPublished = true
            ),
            StoreThemeItem(
                id = "curated_emerald_dusk",
                creatorId = "system",
                creatorName = "استوديو جسر",
                title = "الزمرد الليلي",
                description = "درجات الزمرد الهادئة مع لمعان ضوئي زمردي ناعم",
                backgroundHex = 0xFF132A24L,
                iconHex = 0xFFD6F7E8L,
                lightHex = 0xFF9DF0CBL,
                lightEnabled = true,
                priceUsd = 0,
                isPublished = true
            )
        ) + CinematicThemesCatalog.toStoreThemeItems()
    }
}
