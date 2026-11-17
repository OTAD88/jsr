package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeStoreRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun publishTheme_dollarOneAndFree_succeedsAndStreams() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repo = ThemeStoreRepository(firestore, auth)

        val paidId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.publishTheme(
                title = "ثيم ذهبي",
                description = "واجهة مدفوعة",
                backgroundHex = 0xFFD5C3B0L,
                iconHex = 0xFF131312L,
                lightHex = 0xFFFFF5E4L,
                lightEnabled = true,
                priceUsd = 1,
                creatorName = "أليس"
            ).getOrThrow()
        }
        assertTrue(paidId.isNotBlank())

        val themes = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observePublishedThemes().first { list -> list.any { it.id == paidId } }
        }
        val match = themes.first { it.id == paidId }
        assertEquals(1, match.priceUsd)
        assertEquals("ثيم ذهبي", match.title)
    }

    @Test
    fun crossUserDelete_failsWithPermissionDenied() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = ThemeStoreRepository(firestore, auth)
        val themeId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.publishTheme(
                title = "ثيم أليس",
                description = "",
                backgroundHex = 0xFF252E3CL,
                iconHex = 0xFFD8E7FFL,
                lightHex = 0xFFD8E7FFL,
                lightEnabled = true,
                priceUsd = 0,
                creatorName = "أليس"
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = ThemeStoreRepository(firestore, auth)
        val delResult = withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.deletePublishedTheme(themeId) }
        assertTrue(delResult.isFailure)
        val fsEx = delResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    @Test
    fun unauthenticatedObserve_failsBeforeQuery() = runBlocking {
        auth.signOut()
        val repo = ThemeStoreRepository(firestore, auth)
        var thrown: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) { repo.observePublishedThemes().first() }
        } catch (t: Throwable) {
            thrown = t
        }
        assertNotNull(thrown)
    }

    @Test
    fun acquireTheme_storesUnderCurrentUserLibrary() = runBlocking {
        signInTestUser(BOB_EMAIL)
        val bobRepo = ThemeStoreRepository(firestore, auth)
        val item = StoreThemeItem(
            id = "curated_sunset",
            creatorId = "system",
            creatorName = "جسر",
            title = "غروب الصحراء",
            description = "مجاني",
            backgroundHex = 0xFFEEDBC9L,
            iconHex = 0xFF3D291CL,
            lightHex = 0xFFFFF1DDL,
            lightEnabled = true,
            priceUsd = 0
        )
        val res = withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.acquireTheme(item) }
        assertTrue(res.isSuccess)
        val acquired = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeAcquiredThemes().first { list -> list.any { it.themeId == "curated_sunset" } }
        }
        assertEquals(1, acquired.count { it.themeId == "curated_sunset" })
    }

    private companion object {
        const val ALICE_EMAIL = "alice_store@test.com"
        const val BOB_EMAIL = "bob_store@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
