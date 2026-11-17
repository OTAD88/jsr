package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

@IgnoreExtraProperties
data class StoreThemeItem(
    @DocumentId val id: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val title: String = "",
    val description: String = "",
    val backgroundHex: Long = 0xFFD5C3B0L,
    val iconHex: Long = 0xFF131312L,
    val lightHex: Long = 0xFFFFF5E4L,
    val lightEnabled: Boolean = true,
    val priceUsd: Int = 0,
    val isPublished: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AcquiredThemeRecord(
    @DocumentId val id: String = "",
    val themeId: String = "",
    val title: String = "",
    val priceUsd: Int = 0,
    val backgroundHex: Long = 0xFFD5C3B0L,
    val iconHex: Long = 0xFF131312L,
    val lightHex: Long = 0xFFFFF5E4L,
    val lightEnabled: Boolean = true,
    @ServerTimestamp val acquiredAt: Timestamp? = null
)

class ThemeStoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing the cloud Theme Store.")
    }

    fun handleFirestoreError(exception: Throwable, operationType: OperationType, path: String?): String {
        val causes = generateSequence(exception) { it.cause }
        val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
            ?: causes.firstOrNull { it !is CancellationException }
            ?: exception

        val currentUser = auth.currentUser
        val providerInfoList = currentUser?.providerData?.map { provider ->
            JSONObject().apply {
                put("providerId", provider.providerId)
                put("email", provider.email)
            }
        } ?: emptyList()

        val authInfoJson = JSONObject().apply {
            put("userId", currentUser?.uid)
            put("email", currentUser?.email)
            put("emailVerified", currentUser?.isEmailVerified)
            put("tenantId", currentUser?.tenantId)
            put("providerInfo", JSONArray(providerInfoList))
        }

        val errorInfoJson = JSONObject().apply {
            put("error", rootError.message ?: rootError.toString())
            put("operationType", operationType.value)
            put("path", path)
            put("authInfo", authInfoJson)
        }

        val jsonString = errorInfoJson.toString()
        Log.e("FirestoreError", "Firestore Error: $jsonString")
        return jsonString
    }

    fun observePublishedThemes(): Flow<List<StoreThemeItem>> = flow {
        val uid = requireUserId()
        val path = COLLECTION_STORE_THEMES
        emitAll(
            db.collection(path)
                .whereEqualTo("isPublished", true)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(StoreThemeItem::class.java) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeAcquiredThemes(): Flow<List<AcquiredThemeRecord>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/$COLLECTION_ACQUIRED_THEMES"
        emitAll(
            db.collection("users")
                .document(uid)
                .collection(COLLECTION_ACQUIRED_THEMES)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(AcquiredThemeRecord::class.java) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun publishTheme(
        title: String,
        description: String,
        backgroundHex: Long,
        iconHex: Long,
        lightHex: Long,
        lightEnabled: Boolean,
        priceUsd: Int,
        creatorName: String
    ): Result<String> {
        return try {
            val uid = requireUserId()
            val normalizedPrice = if (priceUsd == 1) 1 else 0
            val cleanTitle = title.trim().take(60).ifEmpty { "ثيم جسر مخصص" }
            val cleanCreator = creatorName.trim().take(60).ifEmpty { "مصمم جسر" }
            val cleanDesc = description.trim().take(180)
            val docRef = db.collection(COLLECTION_STORE_THEMES).document()
            val payload = mapOf<String, Any?>(
                "creatorId" to uid,
                "creatorName" to cleanCreator,
                "title" to cleanTitle,
                "description" to cleanDesc,
                "backgroundHex" to backgroundHex,
                "iconHex" to iconHex,
                "lightHex" to lightHex,
                "lightEnabled" to lightEnabled,
                "priceUsd" to normalizedPrice,
                "isPublished" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, COLLECTION_STORE_THEMES)
            Result.failure(e)
        }
    }

    suspend fun deletePublishedTheme(themeId: String): Result<Unit> {
        return try {
            requireUserId()
            val docRef = db.collection(COLLECTION_STORE_THEMES).document(themeId)
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "$COLLECTION_STORE_THEMES/$themeId")
            Result.failure(e)
        }
    }

    suspend fun acquireTheme(item: StoreThemeItem): Result<Unit> {
        return try {
            val uid = requireUserId()
            val docId = item.id.ifBlank { "theme_${item.title.hashCode()}" }
                .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
                .take(120)
            val docRef = db.collection("users")
                .document(uid)
                .collection(COLLECTION_ACQUIRED_THEMES)
                .document(docId)
            val payload = mapOf<String, Any?>(
                "themeId" to docId,
                "title" to item.title.trim().take(60).ifEmpty { "ثيم جسر" },
                "priceUsd" to if (item.priceUsd == 1) 1 else 0,
                "backgroundHex" to item.backgroundHex,
                "iconHex" to item.iconHex,
                "lightHex" to item.lightHex,
                "lightEnabled" to item.lightEnabled,
                "acquiredAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}/$COLLECTION_ACQUIRED_THEMES")
            Result.failure(e)
        }
    }

    companion object {
        const val COLLECTION_STORE_THEMES = "store_themes"
        const val COLLECTION_ACQUIRED_THEMES = "acquired_themes"
    }
}
