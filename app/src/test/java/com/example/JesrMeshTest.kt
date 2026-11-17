package com.example

import android.content.Context
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.crypto.JesrCrypto
import com.example.data.AppDatabase
import com.example.data.entity.MessageEntity
import com.example.data.entity.RelayQueueEntity
import com.example.data.model.DeliveryStatus
import com.example.data.model.MeshPacket
import com.example.data.model.MessageType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class JesrMeshTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var database: AppDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testJesrIdFormat() {
        val crypto = JesrCrypto(context)
        val id = crypto.myJesrId
        assertTrue("Jesr ID must start with JESR-", id.startsWith("JESR-"))
        val parts = id.split("-")
        assertEquals("Jesr ID should have 3 parts", 3, parts.size)
        assertEquals(4, parts[1].length)
        assertEquals(4, parts[2].length)
    }

    @Test
    fun testEndToEndEncryption() {
        // Simulate Device A and Device B
        val cryptoA = JesrCrypto(context)
        val keyB = cryptoA.parsePublicKey(cryptoA.myPublicKeyBase64)
        assertNotNull("Public key parsing must succeed", keyB)

        val plaintext = "مرحباً، هذه رسالة مشفرة عبر شبكة جسر!"
        val encrypted = cryptoA.encrypt(plaintext, keyB!!)

        assertTrue(encrypted.ciphertextBase64.isNotEmpty())
        assertTrue(encrypted.ivBase64.isNotEmpty())

        val decrypted = cryptoA.decrypt(encrypted.ciphertextBase64, encrypted.ivBase64, keyB)
        assertEquals(plaintext, decrypted)

        // Test with Base64 key string helper
        val decryptedWithBase64Key = cryptoA.decryptWithBase64Key(
            encrypted.ciphertextBase64,
            encrypted.ivBase64,
            cryptoA.myPublicKeyBase64
        )
        assertEquals(plaintext, decryptedWithBase64Key)
    }

    @Test
    fun testMeshPacketJsonSerialization() {
        val original = MeshPacket(
            id = UUID.randomUUID().toString(),
            senderId = "JESR-AAAA-1111",
            recipientId = "JESR-BBBB-2222",
            type = MessageType.CHAT,
            payload = "SGVsbG8gTWVzaA==",
            iv = "MTIzNDU2Nzg5MDEy",
            ttl = 5,
            hopCount = 2,
            timestamp = 1700000000000L
        )

        val json = original.toJson()
        val parsed = MeshPacket.fromJson(json)

        assertNotNull("Parsed packet must not be null", parsed)
        assertEquals(original.id, parsed?.id)
        assertEquals(original.senderId, parsed?.senderId)
        assertEquals(original.recipientId, parsed?.recipientId)
        assertEquals(original.type, parsed?.type)
        assertEquals(original.payload, parsed?.payload)
        assertEquals(5, parsed?.ttl)
        assertEquals(2, parsed?.hopCount)
    }

    @Test
    fun testRoomDatabaseMessagePersistence() = runBlocking {
        val dao = database.messageDao()
        val msg = MessageEntity(
            id = "msg-001",
            conversationId = "conv-123",
            senderId = "JESR-A",
            recipientId = "JESR-B",
            text = "اختبار قاعدة البيانات",
            timestamp = System.currentTimeMillis(),
            status = DeliveryStatus.LOCAL_PENDING
        )

        dao.insertMessage(msg)
        val loaded = dao.getMessageById("msg-001")
        assertNotNull(loaded)
        assertEquals(DeliveryStatus.LOCAL_PENDING, loaded?.status)

        // Update status to DELIVERED
        dao.updateMessageStatus("msg-001", DeliveryStatus.DELIVERED)
        val updated = dao.getMessageById("msg-001")
        assertEquals(DeliveryStatus.DELIVERED, updated?.status)
    }

    @Test
    fun testStoreAndForwardQueuePersistence() = runBlocking {
        val queueDao = database.relayQueueDao()
        val entity = RelayQueueEntity(
            messageId = "queue-msg-01",
            targetPeerId = "JESR-OFFLINE",
            packetJson = "{}",
            createdAt = System.currentTimeMillis(),
            nextAttemptTime = System.currentTimeMillis() - 1000L,
            retryCount = 0,
            maxRetries = 5
        )

        queueDao.enqueue(entity)
        val ready = queueDao.getReadyPackets(System.currentTimeMillis())
        assertEquals(1, ready.size)
        assertEquals("queue-msg-01", ready[0].messageId)

        // Increment retry with backoff
        queueDao.incrementRetry("queue-msg-01", System.currentTimeMillis() + 10000L)
        val count = queueDao.getQueueCount().first()
        assertEquals(1, count)

        // Remove on successful delivery
        queueDao.remove("queue-msg-01")
        val finalCount = queueDao.getQueueCount().first()
        assertEquals(0, finalCount)
    }

    @Test
    fun testGlobalPrivacyDefaultsAndBidirectionalOverrides() = runBlocking {
        val repo = com.example.data.repository.JesrRepository(context)
        assertEquals(false, repo.globalDistanceAllowed.value)
        assertEquals(false, repo.globalMutualFriendsAllowed.value)
        assertEquals(false, repo.isNearbyVisibilityEnabled.value)

        val friendDao = database.friendDao()
        val friendId = "JESR-TEST-1001"
        friendDao.upsertFriend(
            com.example.data.entity.FriendEntity(
                jesrId = friendId,
                displayName = "صديق اختبار",
                status = com.example.data.model.FriendStatus.FRIEND
            )
        )

        // Global OFF, friend distance override ON (mutual friends untouched)
        friendDao.updateDistanceOverride(friendId, true)
        var loaded = friendDao.getFriend(friendId)!!
        assertEquals(true, loaded.distancePermissionOverride)
        assertEquals(null, loaded.mutualFriendsPermissionOverride)

        // Friend mutual override ON while distance OFF
        friendDao.updateDistanceOverride(friendId, false)
        friendDao.updateMutualFriendsOverride(friendId, true)
        loaded = friendDao.getFriend(friendId)!!
        assertEquals(false, loaded.distancePermissionOverride)
        assertEquals(true, loaded.mutualFriendsPermissionOverride)
    }

    @Test
    fun testRequestPrivacyMuteAndBlockSeparation() {
        val repo = com.example.data.repository.JesrRepository(context)
        val sender = "JESR-REQ-9999"

        // Muting does not block; blocking is independent
        repo.setGlobalMuteRequests(true)
        repo.setGlobalBlockRequests(false)
        assertTrue(repo.isRequestMutedFor(sender))
        assertEquals(false, repo.isRequestBlockedFor(sender))

        // Per-user block exception while global block is false
        repo.setUserRequestBlockRule(sender, true)
        assertTrue(repo.isRequestBlockedFor(sender))
        assertTrue(repo.isRequestMutedFor(sender))

        // Disabling block restores ability to receive future requests
        repo.setUserRequestBlockRule(sender, false)
        assertEquals(false, repo.isRequestBlockedFor(sender))
    }

    @Test
    fun testBlockUnblockResumeAndDuplicateFreeNavigation() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.ui.viewmodel.JesrViewModel(app)

        // Default appearance mode is AUTO
        assertEquals(com.example.data.model.AppearanceMode.AUTO, vm.appearanceMode.value)

        // Duplicate-free backstack navigation
        vm.openConversation("JESR-PEER-0001", "سارة")
        assertEquals(com.example.ui.viewmodel.Screen.CONVERSATION, vm.currentScreen.value)
        vm.openFriendSettings("JESR-PEER-0001")
        assertEquals(com.example.ui.viewmodel.Screen.FRIEND_SETTINGS, vm.currentScreen.value)

        // Re-navigating to CONVERSATION collapses backstack cleanly without duplicates
        vm.navigateBack()
        assertEquals(com.example.ui.viewmodel.Screen.CONVERSATION, vm.currentScreen.value)
        vm.navigateBack()
        assertEquals(com.example.ui.viewmodel.Screen.HOME, vm.currentScreen.value)
        assertEquals(1, vm.backStack.value.size)

        // Block -> Unblock sets unblockedAwaitingResume without deleting messages
        val friendDao = database.friendDao()
        friendDao.upsertFriend(
            com.example.data.entity.FriendEntity(
                jesrId = "JESR-PEER-0001",
                displayName = "سارة",
                status = com.example.data.model.FriendStatus.FRIEND
            )
        )
        friendDao.updateBlockAndResumeState("JESR-PEER-0001", blocked = true, awaitingResume = false, localApproved = false, peerApproved = false)
        assertTrue(friendDao.getFriend("JESR-PEER-0001")!!.isBlocked)

        friendDao.updateBlockAndResumeState("JESR-PEER-0001", blocked = false, awaitingResume = true, localApproved = false, peerApproved = false)
        val awaiting = friendDao.getFriend("JESR-PEER-0001")!!
        assertEquals(false, awaiting.isBlocked)
        assertTrue(awaiting.unblockedAwaitingResume)
    }

    @Test
    fun testCustomModeThreeColorsAndLightToggle() = runBlocking {
        val repo = com.example.data.repository.JesrRepository(context)
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.ui.viewmodel.JesrViewModel(app)

        val bgHex = 0xFF2B3544L
        val iconHex = 0xFFD8E7FFL
        val lightHex = 0xFFFFDDAAL

        repo.setAppearanceMode(com.example.data.model.AppearanceMode.CUSTOM)
        assertEquals(com.example.data.model.AppearanceMode.CUSTOM, repo.appearanceMode.value)

        repo.setCustomColors(bgHex, iconHex, lightHex)
        assertEquals(bgHex, repo.customBackgroundHex.value)
        assertEquals(iconHex, repo.customIconHex.value)
        assertEquals(lightHex, repo.customLightHex.value)

        repo.setCustomLightEnabled(false)
        assertEquals(false, repo.customLightEnabled.value)

        vm.setCustomColors(bgHex, iconHex, lightHex)
        assertEquals(bgHex, vm.customBackgroundHex.first { it == bgHex })
        assertEquals(iconHex, vm.customIconHex.first { it == iconHex })
        assertEquals(lightHex, vm.customLightHex.first { it == lightHex })

        vm.setCustomLightEnabled(true)
        assertEquals(true, vm.customLightEnabled.first { it })

        vm.setCustomColors(null, null, null)
        assertEquals(null, vm.customBackgroundHex.first { it == null })
        assertEquals(null, vm.customIconHex.first { it == null })
        assertEquals(null, vm.customLightHex.first { it == null })
    }

    @Test
    fun testSeparateColorsUndoProtectedBaseAndStore() {
        val repo = com.example.data.repository.JesrRepository(context)
        repo.setAppearanceMode(com.example.data.model.AppearanceMode.CUSTOM)

        // Restore protected base color
        repo.restoreBaseCustomColors()
        assertEquals(com.example.data.repository.JesrRepository.BASE_CUSTOM_BG_HEX, repo.customBackgroundHex.value)
        assertEquals(com.example.data.repository.JesrRepository.BASE_CUSTOM_ICON_HEX, repo.customIconHex.value)
        assertEquals(com.example.data.repository.JesrRepository.BASE_CUSTOM_LIGHT_HEX, repo.customLightHex.value)

        // Independent single-color updates do not touch other colors
        repo.setCustomBackgroundColor(0xFF152544L)
        assertEquals(0xFF152544L, repo.customBackgroundHex.value)
        assertEquals(com.example.data.repository.JesrRepository.BASE_CUSTOM_ICON_HEX, repo.customIconHex.value)
        assertTrue(repo.canUndoCustomAppearance.value)

        repo.setCustomIconColor(0xFFE6EEFFL)
        assertEquals(0xFFE6EEFFL, repo.customIconHex.value)

        // Step-by-step undo restores previous colors cleanly
        assertTrue(repo.undoCustomAppearance())
        assertEquals(com.example.data.repository.JesrRepository.BASE_CUSTOM_ICON_HEX, repo.customIconHex.value)
        assertEquals(0xFF152544L, repo.customBackgroundHex.value)

        assertTrue(repo.undoCustomAppearance())
        assertEquals(com.example.data.repository.JesrRepository.BASE_CUSTOM_BG_HEX, repo.customBackgroundHex.value)

        // Protected Base Color cannot be deleted
        assertEquals(false, repo.deleteLocalPublishedTheme(com.example.data.repository.JesrRepository.BASE_THEME_ID))

        // User-created $1 theme can be published & deleted
        val customTheme = com.example.data.repository.StoreThemeItem(
            id = "user_custom_test_1",
            creatorId = repo.myJesrId,
            creatorName = "مصمم",
            title = "ثيم تجريبي",
            description = "وصف",
            backgroundHex = 0xFF152544L,
            iconHex = 0xFFE6EEFFL,
            lightHex = 0xFF8AB4FFL,
            lightEnabled = true,
            priceUsd = 1
        )
        repo.saveLocalPublishedTheme(customTheme)
        assertTrue(repo.localPublishedThemes.value.any { it.id == "user_custom_test_1" && it.priceUsd == 1 })
        assertTrue(repo.deleteLocalPublishedTheme("user_custom_test_1"))
    }

    @Test
    fun testOriginalThemeRestoreFreeCatalogAndOwnedList() = runBlocking {
        // 1. Simulate legacy modified custom theme prior to v3 reset migration
        val prefs = context.getSharedPreferences("jesr_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("key_appearance_mode", com.example.data.model.AppearanceMode.CUSTOM.name)
            .putLong("key_custom_bg_color_hex", 0xFF152544L)
            .putLong("key_custom_icon_color_hex", 0xFFE6EEFFL)
            .putLong("key_custom_light_color_hex", 0xFF8AB4FFL)
            .putBoolean("key_custom_light_enabled", false)
            .remove("key_reset_to_original_theme_v3")
            .commit()

        val repo = com.example.data.repository.JesrRepository(context)
        // Migration must automatically restore the true Original Theme
        assertEquals(com.example.data.model.AppearanceMode.AUTO, repo.appearanceMode.value)
        assertEquals(null, repo.customBackgroundHex.value)
        assertEquals(null, repo.customIconHex.value)
        assertEquals(null, repo.customLightHex.value)
        assertEquals(true, repo.customLightEnabled.value)

        // 2. Original Theme exists inside Free store catalog & cannot be deleted
        val baseEntry = com.example.data.repository.JesrRepository.CURATED_STORE_THEMES
            .firstOrNull { it.id == com.example.data.repository.JesrRepository.BASE_THEME_ID }
        assertNotNull(baseEntry)
        assertEquals(0, baseEntry?.priceUsd)
        assertEquals("الثيم الأصلي (الواجهة الأساسية)", baseEntry?.title)

        // 3. Switch to CUSTOM, apply colors, then restoreOriginalTheme & Undo back to CUSTOM
        repo.setAppearanceMode(com.example.data.model.AppearanceMode.CUSTOM)
        repo.setCustomColors(0xFF1E2634L, 0xFFD8E7FFL, 0xFFC5DCFFL)
        assertEquals(com.example.data.model.AppearanceMode.CUSTOM, repo.appearanceMode.value)

        repo.restoreOriginalTheme()
        assertEquals(com.example.data.model.AppearanceMode.AUTO, repo.appearanceMode.value)
        assertEquals(null, repo.customBackgroundHex.value)

        assertTrue(repo.undoCustomAppearance())
        assertEquals(com.example.data.model.AppearanceMode.CUSTOM, repo.appearanceMode.value)
        assertEquals(0xFF1E2634L, repo.customBackgroundHex.value)

        // 4. Applying BASE_THEME_ID from store restores Original Theme (AUTO)
        repo.applyStoreThemeAppearance(
            backgroundHex = baseEntry!!.backgroundHex,
            iconHex = baseEntry.iconHex,
            lightHex = baseEntry.lightHex,
            lightEnabled = baseEntry.lightEnabled,
            themeId = baseEntry.id
        )
        assertEquals(com.example.data.model.AppearanceMode.AUTO, repo.appearanceMode.value)
        assertEquals(null, repo.customBackgroundHex.value)

        // 5. ViewModel ownedUserThemes always pins Original Theme at top
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.ui.viewmodel.JesrViewModel(app)
        val owned = vm.ownedUserThemes.first { it.isNotEmpty() }
        assertEquals(com.example.data.repository.JesrRepository.BASE_THEME_ID, owned.first().id)
    }

    @Test
    fun testDrawerStoreNavigationAndThemeStoreRendersWithoutErrors() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.ui.viewmodel.JesrViewModel(app)

        composeRule.setContent {
            com.example.ui.theme.JesrTheme {
                JesrApp(viewModel = vm)
            }
        }
        composeRule.waitForIdle()

        // 1. Tap Top-Left Two Horizontal Lines menu icon
        composeRule.onNodeWithTag("home_two_lines_menu").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        // 2. Staggered Drawer items + Store icon appear and reach their right place
        composeRule.onNodeWithTag("drawer_home").assertIsDisplayed()
        composeRule.onNodeWithTag("drawer_theme_store").assertIsDisplayed()
        composeRule.onNodeWithTag("drawer_google_signin_btn").assertIsDisplayed()

        // 3. Tap Store icon ("متجر الثيمات") -> opens real ThemeStoreScreen with NO error or "قريباً"
        composeRule.onNodeWithTag("drawer_theme_store").performClick()
        composeRule.waitForIdle()

        assertEquals(com.example.ui.viewmodel.Screen.THEME_STORE, vm.currentScreen.value)
        composeRule.onNodeWithText("متجر الثيمات والواجهات").assertIsDisplayed()
        composeRule.onNodeWithTag("store_publish_open_btn").assertDoesNotExist()
        composeRule.onNodeWithTag("store_filter_my_designs").assertDoesNotExist()
        composeRule.onNodeWithTag("store_filter_free").assertIsDisplayed()
        // Protected base theme stays pinned in Downloaded Themes (Appearance), not sold in Store
        composeRule.onNodeWithTag("store_theme_card_${com.example.data.repository.JesrRepository.BASE_THEME_ID}")
            .assertDoesNotExist()
        composeRule.onNodeWithTag("store_theme_card_curated_olive_grove").assertIsDisplayed()

        // 4. Filter Free themes cleanly
        composeRule.onNodeWithTag("store_filter_free").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("store_theme_card_curated_olive_grove").assertIsDisplayed()

        // 5. Return and verify Appearance Store button also navigates cleanly to THEME_STORE
        composeRule.onNodeWithTag("theme_store_back_btn").performClick()
        composeRule.waitForIdle()
        vm.navigateTo(com.example.ui.viewmodel.Screen.APPEARANCE)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("الثيمات المنزلة").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("appearance_open_store_btn").performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals(com.example.ui.viewmodel.Screen.THEME_STORE, vm.currentScreen.value)
    }

    @Test
    fun testDrawerItemsSettingsLanguageAppearanceAndRequestsSwipe() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.ui.viewmodel.JesrViewModel(app)

        composeRule.setContent {
            com.example.ui.theme.JesrTheme {
                JesrApp(viewModel = vm)
            }
        }
        composeRule.waitForIdle()

        // 1. Open drawer: Profile and Appearance must NOT be in the drawer
        composeRule.onNodeWithTag("home_two_lines_menu").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("drawer_home").assertIsDisplayed()
        composeRule.onNodeWithTag("drawer_privacy").assertIsDisplayed()
        composeRule.onNodeWithTag("drawer_theme_store").assertIsDisplayed()
        composeRule.onNodeWithTag("drawer_settings").assertIsDisplayed()
        composeRule.onNodeWithTag("drawer_profile").assertDoesNotExist()
        composeRule.onNodeWithTag("drawer_appearance").assertDoesNotExist()

        // 2. Navigate to Settings -> Appearance is inside Settings, and relay stats / E2EE cards are removed
        composeRule.onNodeWithTag("drawer_settings").performClick()
        composeRule.waitForIdle()
        assertEquals(com.example.ui.viewmodel.Screen.SETTINGS, vm.currentScreen.value)
        composeRule.onNodeWithText("إحصائيات التمرير (Store-and-Forward)").assertDoesNotExist()
        composeRule.onNodeWithText("تشفير طرفي NIST P-256 + AES-GCM").assertDoesNotExist()

        composeRule.onNodeWithTag("settings_appearance_btn").performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals(com.example.ui.viewmodel.Screen.APPEARANCE, vm.currentScreen.value)

        composeRule.onNodeWithTag("appearance_back_btn").performClick()
        composeRule.waitForIdle()
        assertEquals(com.example.ui.viewmodel.Screen.SETTINGS, vm.currentScreen.value)

        // 3. Switch language to English in Settings
        composeRule.onNodeWithTag("settings_lang_en").performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals(com.example.data.model.AppLanguage.EN, vm.appLanguage.value)
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        composeRule.onNodeWithText("Appearance").assertIsDisplayed()
        composeRule.onNodeWithText("Store-and-Forward Relay Stats").assertDoesNotExist()
        composeRule.onNodeWithText("E2EE: NIST P-256 + AES-GCM").assertDoesNotExist()

        // Switch back to Arabic cleanly
        composeRule.onNodeWithTag("settings_lang_ar").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(com.example.data.model.AppLanguage.AR, vm.appLanguage.value)
        composeRule.onNodeWithText("الإعدادات").assertIsDisplayed()

        // 4. Nearby People screen has NO search range filter pills
        vm.navigateTo(com.example.ui.viewmodel.Screen.NEARBY_PEOPLE)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nearby_refresh_btn").assertIsDisplayed()
        composeRule.onNodeWithTag("nearby_radius_all").assertDoesNotExist()
        composeRule.onNodeWithTag("nearby_radius_close").assertDoesNotExist()
        composeRule.onNodeWithTag("nearby_radius_medium").assertDoesNotExist()
        composeRule.onNodeWithTag("nearby_radius_wide").assertDoesNotExist()

        // 5. Friend Requests screen shows renamed tab & supports fast swipe pager
        vm.navigateTo(com.example.ui.viewmodel.Screen.FRIEND_REQUESTS)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("الطلبات التي أرسلتها (0)").assertIsDisplayed()
        composeRule.onNodeWithTag("requests_horizontal_pager").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_sent_requests").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("requests_horizontal_pager").performTouchInput { swipeRight() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("requests_horizontal_pager").performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
    }

    @Test
    fun testCinematicCatalogAllFreeAndAstronautDualModes() {
        val catalog = com.example.ui.theme.CinematicThemesCatalog.allThemes
        assertEquals(36, catalog.size)

        val astronaut = catalog.firstOrNull { item ->
            item.id == com.example.ui.theme.CinematicThemesCatalog.ASTRONAUT_ID
        }
        assertNotNull(astronaut)
        assertEquals(com.example.ui.theme.CinematicCategory.INTERACTIVE_WORLD, astronaut?.category)

        val sunriseTheme = catalog.firstOrNull { item ->
            item.id == com.example.ui.theme.CinematicThemesCatalog.SUNRISE_TREE_MOUNTAIN_WATER_ID
        }
        assertNotNull(sunriseTheme)
        assertEquals(com.example.ui.theme.CinematicSceneType.SUNRISE_TREE_MOUNTAIN_WATER, sunriseTheme?.sceneType)
        assertEquals(
            com.example.ui.components.GyroParticleKind.SUN_MOTES,
            com.example.ui.components.particleKindForTheme(sunriseTheme, isDark = false, lightEnabled = true)
        )
        assertEquals(
            com.example.ui.components.GyroParticleKind.COSMIC_DUST,
            com.example.ui.components.particleKindForTheme(sunriseTheme, isDark = true, lightEnabled = true)
        )
        assertNotNull(android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.theme_sunrise_tree_mountain_water))
        assertNotNull(android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.ic_jesr_logo))

        val frutigerAero = catalog.firstOrNull { item ->
            item.id == "cinematic_world_frutiger_aero"
        }
        assertNotNull(frutigerAero)
        assertEquals(com.example.ui.theme.CinematicSceneType.FRUTIGER_AERO, frutigerAero?.sceneType)
        assertEquals(
            com.example.ui.components.GyroParticleKind.WATER_BUBBLES,
            com.example.ui.components.particleKindForTheme(frutigerAero, isDark = false, lightEnabled = true)
        )
        assertEquals(
            com.example.ui.components.GyroParticleKind.WATER_BUBBLES,
            com.example.ui.components.particleKindForTheme(frutigerAero, isDark = true, lightEnabled = true)
        )

        val frostWorld = catalog.firstOrNull { item ->
            item.id == "cinematic_world_frost_campfire"
        }
        assertNotNull(frostWorld)
        assertTrue(frostWorld?.usesFrostLight == true)

        val worldsCount = catalog.count { item -> item.category == com.example.ui.theme.CinematicCategory.INTERACTIVE_WORLD }
        val mergedCinematicCount = catalog.count { item -> item.category == com.example.ui.theme.CinematicCategory.CINEMATIC }
        assertEquals(16, worldsCount)
        assertEquals(20, mergedCinematicCount)

        // Dynamic camera poses react across screens (Home -> Settings -> Profile)
        val homePose = com.example.ui.components.sceneCameraPoseFor(com.example.ui.viewmodel.Screen.HOME)
        val settingsPose = com.example.ui.components.sceneCameraPoseFor(com.example.ui.viewmodel.Screen.SETTINGS)
        val profilePose = com.example.ui.components.sceneCameraPoseFor(com.example.ui.viewmodel.Screen.PROFILE)
        assertEquals(0f, homePose.altitudeDepth, 0.01f)
        assertEquals(1f, settingsPose.altitudeDepth, 0.01f)
        assertTrue(profilePose.angleDeg > 5f && profilePose.zoom > homePose.zoom)

        // Gyroscope depth particle system maps worlds cleanly and scales parallax across depth layers
        assertEquals(
            com.example.ui.components.GyroParticleKind.SNOW_FROST,
            com.example.ui.components.particleKindForTheme(frostWorld, isDark = true, lightEnabled = true)
        )
        val sunlitRoom = com.example.ui.theme.CinematicThemesCatalog.findById("cinematic_world_sunlit_bedroom")
        assertEquals(
            com.example.ui.components.GyroParticleKind.SUN_MOTES,
            com.example.ui.components.particleKindForTheme(sunlitRoom, isDark = false, lightEnabled = true)
        )
        val campTruck = com.example.ui.theme.CinematicThemesCatalog.findById("cinematic_world_overland_truck")
        assertNotNull(campTruck)
        assertEquals(
            com.example.ui.components.GyroParticleKind.CAMPFIRE_EMBERS,
            com.example.ui.components.particleKindForTheme(campTruck, isDark = true, lightEnabled = true)
        )

        val tilt = com.example.ui.components.stepGyroTiltFromSensors(
            current = com.example.ui.components.GyroTiltState(),
            gyroPitchRate = 0.25f,
            gyroRollRate = -0.2f,
            gravityX = -4.2f,
            gravityY = 6.8f,
            dtSeconds = 0.05f
        )
        val nearShift = com.example.ui.components.computeParallaxOffset(tilt, depth = 1.18f, maxShiftPx = 36f)
        val midShift = com.example.ui.components.computeParallaxOffset(tilt, depth = 0.72f, maxShiftPx = 36f)
        val farShift = com.example.ui.components.computeParallaxOffset(tilt, depth = 0.36f, maxShiftPx = 36f)
        assertTrue(nearShift.getDistance() > midShift.getDistance() && midShift.getDistance() > farShift.getDistance())

        // Every store theme in curated catalog must be Free (priceUsd == 0)
        assertTrue(com.example.data.repository.JesrRepository.CURATED_STORE_THEMES.all { it.priceUsd == 0 })

        // Applying Astronaut in Dark and Light modes updates appearanceMode and activeCinematicThemeId
        val repo = com.example.data.repository.JesrRepository(context)
        repo.applyCinematicTheme(com.example.ui.theme.CinematicThemesCatalog.ASTRONAUT_ID, isDark = true)
        assertEquals(com.example.ui.theme.CinematicThemesCatalog.ASTRONAUT_ID, repo.activeCinematicThemeId.value)
        assertEquals(com.example.data.model.AppearanceMode.DARK, repo.appearanceMode.value)

        repo.applyCinematicTheme(com.example.ui.theme.CinematicThemesCatalog.ASTRONAUT_ID, isDark = false)
        assertEquals(com.example.ui.theme.CinematicThemesCatalog.ASTRONAUT_ID, repo.activeCinematicThemeId.value)
        assertEquals(com.example.data.model.AppearanceMode.LIGHT, repo.appearanceMode.value)
    }

    @Test
    fun testAppLockPinPasswordPatternAndCoverPhoto() = runBlocking {
        val repo = com.example.data.repository.JesrRepository(context)

        // 1. PIN lock verification
        repo.configureAppLockSecret(com.example.data.model.AppLockMethod.PIN, "2580")
        assertEquals(com.example.data.model.AppLockMethod.PIN, repo.appLockMethod.value)
        repo.lockAppNow()
        assertTrue(repo.isAppLocked.value)
        assertEquals(false, repo.verifyAppLockSecret("0000"))
        assertTrue(repo.isAppLocked.value)
        assertTrue(repo.verifyAppLockSecret("2580"))
        assertEquals(false, repo.isAppLocked.value)

        // 2. Password lock verification (alphanumeric)
        repo.configureAppLockSecret(com.example.data.model.AppLockMethod.PASSWORD, "Jesr@2026")
        assertEquals(com.example.data.model.AppLockMethod.PASSWORD, repo.appLockMethod.value)
        repo.lockAppNow()
        assertEquals(false, repo.verifyAppLockSecret("wrong"))
        assertTrue(repo.verifyAppLockSecret("Jesr@2026"))
        assertEquals(false, repo.isAppLocked.value)

        // 3. Pattern lock verification
        val patternEncoded = com.example.security.AppLockManager.encodePattern(listOf(0, 1, 4, 7, 8))
        repo.configureAppLockSecret(com.example.data.model.AppLockMethod.PATTERN, patternEncoded)
        assertEquals(com.example.data.model.AppLockMethod.PATTERN, repo.appLockMethod.value)
        repo.lockAppNow()
        assertEquals(false, repo.verifyAppLockSecret("0-1-2"))
        assertTrue(repo.verifyAppLockSecret(patternEncoded))
        assertEquals(false, repo.isAppLocked.value)

        // 4. Disable lock cleanly
        repo.disableAppLock()
        assertEquals(com.example.data.model.AppLockMethod.NONE, repo.appLockMethod.value)

        // 5. Account cover photo persistence and removal
        val bmp = android.graphics.Bitmap.createBitmap(160, 90, android.graphics.Bitmap.Config.ARGB_8888)
        repo.updateUserCoverFromBitmap(bmp)
        val savedPath = repo.userCoverPath.first { it != null }
        assertNotNull(savedPath)
        assertTrue(java.io.File(savedPath!!).exists())

        repo.clearUserCover()
        val clearedPath = repo.userCoverPath.first { it == null }
        assertEquals(null, clearedPath)
    }

    @Test
    fun testStoreCinematicCategoriesSettingsAppLockAndProfileCoverUi() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.ui.viewmodel.JesrViewModel(app)

        composeRule.setContent {
            com.example.ui.theme.JesrTheme {
                JesrApp(viewModel = vm)
            }
        }
        composeRule.waitForIdle()

        // 1. Theme Store keeps ONLY All and Free filters (all other category/gender/publish tabs removed)
        vm.navigateTo(com.example.ui.viewmodel.Screen.THEME_STORE)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("store_publish_open_btn").assertDoesNotExist()
        composeRule.onNodeWithTag("store_filter_all").assertIsDisplayed()
        composeRule.onNodeWithTag("store_filter_free").assertIsDisplayed()
        composeRule.onNodeWithTag("store_filter_interactive_worlds").assertDoesNotExist()
        composeRule.onNodeWithTag("store_filter_cinematic").assertDoesNotExist()
        composeRule.onNodeWithTag("store_filter_my_designs").assertDoesNotExist()
        composeRule.onNodeWithTag("store_filter_girls").assertDoesNotExist()
        composeRule.onNodeWithTag("store_filter_boys").assertDoesNotExist()
        composeRule.onNodeWithTag("store_filter_astronaut").assertDoesNotExist()

        // 2. Get Frost & Campfire World -> disappears from Store, auto-equips, transitions to Downloaded Themes in Appearance
        val frostId = "cinematic_world_frost_campfire"
        composeRule.onNodeWithTag("store_filter_free").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("store_themes_list")
            .performScrollToNode(hasTestTag("store_get_theme_$frostId"))
        composeRule.onNodeWithTag("store_get_theme_$frostId")
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()

        assertEquals(frostId, vm.activeCinematicThemeId.value)
        assertEquals(com.example.ui.viewmodel.Screen.APPEARANCE, vm.currentScreen.value)
        composeRule.onNodeWithTag("my_theme_card_$frostId").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("appearance_remove_theme_${com.example.data.repository.JesrRepository.BASE_THEME_ID}")
            .assertDoesNotExist()

        // Frost & Falling Snow toggle controls customLightEnabled on Frost Campfire
        composeRule.onNodeWithTag("appearance_frost_light_toggle").performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals(false, vm.customLightEnabled.value)

        // Switch Frost Campfire between Light and Dark inside Downloaded Themes
        composeRule.onNodeWithTag("appearance_cinematic_dark_btn").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(com.example.data.model.AppearanceMode.DARK, vm.appearanceMode.value)

        // Verify Frost Campfire is gone from the Store after acquisition while Frutiger Aero & other themes stay browsable
        vm.navigateTo(com.example.ui.viewmodel.Screen.THEME_STORE)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("store_theme_card_$frostId").assertDoesNotExist()

        composeRule.onNodeWithTag("store_themes_list")
            .performScrollToNode(hasTestTag("store_theme_card_cinematic_world_frutiger_aero"))
        composeRule.onNodeWithTag("store_theme_card_cinematic_world_frutiger_aero").assertIsDisplayed()

        composeRule.onNodeWithTag("store_themes_list")
            .performScrollToNode(hasTestTag("store_theme_card_cinematic_girl_sakura"))
        composeRule.onNodeWithTag("store_theme_card_cinematic_girl_sakura").assertIsDisplayed()
        composeRule.onNodeWithTag("store_themes_list")
            .performScrollToNode(hasTestTag("store_theme_card_cinematic_boy_cyber"))
        composeRule.onNodeWithTag("store_theme_card_cinematic_boy_cyber").assertIsDisplayed()

        // 3. Settings shows App Lock card and opens setup dialog with Biometric, PIN, Password, Pattern options
        vm.navigateTo(com.example.ui.viewmodel.Screen.SETTINGS)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_app_lock_btn").performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("app_lock_setup_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("lock_option_biometric").assertIsDisplayed()
        composeRule.onNodeWithTag("lock_option_pin").assertIsDisplayed()
        composeRule.onNodeWithTag("lock_option_password").assertIsDisplayed()
        composeRule.onNodeWithTag("lock_option_pattern").assertIsDisplayed()
        composeRule.onNodeWithTag("app_lock_cancel_btn").performClick()
        composeRule.waitForIdle()

        // 4. Profile shows cover customization banner & confirmation dialog before selecting cover image
        vm.navigateTo(com.example.ui.viewmodel.Screen.PROFILE)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("profile_cover_banner").assertIsDisplayed()
        composeRule.onNodeWithTag("profile_change_cover_btn").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("profile_cover_confirm_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("profile_cover_confirm_proceed_btn").assertIsDisplayed()
        composeRule.onNodeWithTag("profile_cover_confirm_cancel_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("profile_cover_confirm_dialog").assertDoesNotExist()
    }

    @Test
    fun testHomeMorphingPlusFabToSquircleMenuAndClose() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.ui.viewmodel.JesrViewModel(app)

        composeRule.setContent {
            com.example.ui.theme.JesrTheme {
                JesrApp(viewModel = vm)
            }
        }
        composeRule.waitForIdle()

        // 1. Initial closed state: soft-rounded square (+) button visible, semi-square menu absent
        composeRule.onNodeWithTag("home_add_friend_fab").assertIsDisplayed()
        composeRule.onNodeWithTag("home_fab_squircle_menu").assertDoesNotExist()

        // 2. Tap (+) -> spins, morphs into circle (×), and pops open semi-square 2x2 menu
        composeRule.onNodeWithTag("home_add_friend_fab").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("home_fab_squircle_menu").assertIsDisplayed()
        composeRule.onNodeWithTag("hub_add_by_id").assertIsDisplayed()
        composeRule.onNodeWithTag("hub_nearby_people").assertIsDisplayed()
        composeRule.onNodeWithTag("hub_friend_requests").assertIsDisplayed()
        composeRule.onNodeWithTag("hub_sent_requests").assertIsDisplayed()
        composeRule.onNodeWithTag("home_add_friend_fab", useUnmergedTree = true)
            .assertIsDisplayed()

        // 3. Tap (×) -> collapses menu and spins back to original bottom-left (+)
        composeRule.onNodeWithTag("home_add_friend_fab").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("home_fab_squircle_menu").assertDoesNotExist()
        assertEquals(com.example.ui.viewmodel.Screen.HOME, vm.currentScreen.value)

        // 4. Open menu again and navigate via Add by ID and Nearby People tiles
        composeRule.onNodeWithTag("home_add_friend_fab").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("hub_add_by_id").performClick()
        composeRule.waitForIdle()
        assertEquals(com.example.ui.viewmodel.Screen.ADD_BY_ID, vm.currentScreen.value)

        vm.navigateBack()
        composeRule.waitForIdle()
        assertEquals(com.example.ui.viewmodel.Screen.HOME, vm.currentScreen.value)

        composeRule.onNodeWithTag("home_add_friend_fab").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("hub_nearby_people").performClick()
        composeRule.waitForIdle()
        assertEquals(com.example.ui.viewmodel.Screen.NEARBY_PEOPLE, vm.currentScreen.value)
    }
}
