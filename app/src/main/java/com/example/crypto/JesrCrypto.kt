package com.example.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class EncryptedResult(
    val ciphertextBase64: String,
    val ivBase64: String
)

class JesrCrypto(private val context: Context) {

    private val prefs = context.getSharedPreferences("jesr_crypto_vault", Context.MODE_PRIVATE)
    private val secureRandom = SecureRandom()

    @Volatile
    private var keyPair: KeyPair

    @Volatile
    private var signingKeyPair: KeyPair

    val myJesrId: String
        get() = generateJesrIdFromKey(keyPair.public)

    val myPublicKeyBase64: String
        get() = Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP)

    val mySigningPublicKeyBase64: String
        get() = Base64.encodeToString(signingKeyPair.public.encoded, Base64.NO_WRAP)

    init {
        keyPair = getOrGenerateKeyPair(
            wrappedKeyPref = "wrapped_ecdh_priv",
            legacyPrivPref = "private_key_pkcs8",
            pubPref = "public_key_x509"
        )
        signingKeyPair = getOrGenerateKeyPair(
            wrappedKeyPref = "wrapped_ecdsa_priv",
            legacyPrivPref = "signing_private_key_pkcs8",
            pubPref = "signing_public_key_x509"
        )
    }

    private fun getOrCreateKeystoreAesKey(): SecretKey? {
        return try {
            val ks = KeyStore.getInstance("AndroidKeyStore")
            ks.load(null)
            val alias = "jesr_vault_master_key_v1"
            val existing = ks.getKey(alias, null) as? SecretKey
            if (existing != null) return existing

            val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val spec = KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGen.init(spec)
            keyGen.generateKey()
        } catch (_: Exception) {
            null
        }
    }

    private fun wrapWithVaultKey(rawBytes: ByteArray): String? {
        val masterKey = getOrCreateKeystoreAesKey() ?: return null
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, masterKey)
            val iv = cipher.iv
            val ct = cipher.doFinal(rawBytes)
            val obj = JSONObject()
            obj.put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
            obj.put("ct", Base64.encodeToString(ct, Base64.NO_WRAP))
            obj.toString()
        } catch (_: Exception) {
            null
        }
    }

    private fun unwrapWithVaultKey(wrappedJson: String): ByteArray? {
        val masterKey = getOrCreateKeystoreAesKey() ?: return null
        return try {
            val obj = JSONObject(wrappedJson)
            val iv = Base64.decode(obj.getString("iv"), Base64.NO_WRAP)
            val ct = Base64.decode(obj.getString("ct"), Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, masterKey, GCMParameterSpec(128, iv))
            cipher.doFinal(ct)
        } catch (_: Exception) {
            null
        }
    }

    fun encryptLocalString(plaintext: String): String {
        val wrapped = wrapWithVaultKey(plaintext.toByteArray(Charsets.UTF_8)) ?: return plaintext
        return "KSv1:$wrapped"
    }

    fun decryptLocalString(stored: String): String {
        if (!stored.startsWith("KSv1:")) return stored
        val bytes = unwrapWithVaultKey(stored.removePrefix("KSv1:")) ?: return stored
        return String(bytes, Charsets.UTF_8)
    }

    private fun getOrGenerateKeyPair(
        wrappedKeyPref: String,
        legacyPrivPref: String,
        pubPref: String
    ): KeyPair {
        val pubBase64 = prefs.getString(pubPref, null)
        val wrapped = prefs.getString(wrappedKeyPref, null)
        val legacyPrivBase64 = prefs.getString(legacyPrivPref, null)

        if (pubBase64 != null) {
            try {
                val keyFactory = KeyFactory.getInstance("EC")
                val privBytes: ByteArray? = when {
                    wrapped != null -> unwrapWithVaultKey(wrapped)
                    legacyPrivBase64 != null -> {
                        val decoded = Base64.decode(legacyPrivBase64, Base64.NO_WRAP)
                        // Opportunistically migrate legacy plaintext key into AndroidKeyStore wrapper
                        val rewrapped = wrapWithVaultKey(decoded)
                        if (rewrapped != null) {
                            prefs.edit()
                                .putString(wrappedKeyPref, rewrapped)
                                .remove(legacyPrivPref)
                                .apply()
                        }
                        decoded
                    }
                    else -> null
                }
                if (privBytes != null) {
                    val pubBytes = Base64.decode(pubBase64, Base64.NO_WRAP)
                    val privateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(privBytes))
                    val publicKey = keyFactory.generatePublic(X509EncodedKeySpec(pubBytes))
                    return KeyPair(publicKey, privateKey)
                }
            } catch (_: Exception) {
                // Regenerate below if corrupted
            }
        }

        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec("secp256r1"), secureRandom)
        val newKeyPair = kpg.generateKeyPair()
        saveKeyPairToVault(newKeyPair, wrappedKeyPref, legacyPrivPref, pubPref)
        return newKeyPair
    }

    private fun saveKeyPairToVault(
        pair: KeyPair,
        wrappedKeyPref: String,
        legacyPrivPref: String,
        pubPref: String
    ) {
        val pubEncoded = Base64.encodeToString(pair.public.encoded, Base64.NO_WRAP)
        val wrapped = wrapWithVaultKey(pair.private.encoded)
        val editor = prefs.edit().putString(pubPref, pubEncoded)
        if (wrapped != null) {
            editor.putString(wrappedKeyPref, wrapped).remove(legacyPrivPref)
        } else {
            // Fallback ONLY for headless JVM environments where AndroidKeyStore does not exist
            val privEncoded = Base64.encodeToString(pair.private.encoded, Base64.NO_WRAP)
            editor.putString(legacyPrivPref, privEncoded)
        }
        editor.apply()
    }

    private fun generateJesrIdFromKey(publicKey: PublicKey): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(publicKey.encoded)
        val chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
        val part1 = StringBuilder(4)
        val part2 = StringBuilder(4)

        for (i in 0..3) {
            val idx = (digest[i].toInt() and 0xFF) % chars.length
            part1.append(chars[idx])
        }
        for (i in 4..7) {
            val idx = (digest[i].toInt() and 0xFF) % chars.length
            part2.append(chars[idx])
        }
        return "JESR-$part1-$part2"
    }

    fun deriveJesrIdFromPublicKeyBase64(pubBase64: String): String? {
        val pub = parsePublicKey(pubBase64) ?: return null
        return generateJesrIdFromKey(pub)
    }

    fun parsePublicKey(base64Str: String): PublicKey? {
        return try {
            val bytes = Base64.decode(base64Str, Base64.NO_WRAP)
            val kf = KeyFactory.getInstance("EC")
            kf.generatePublic(X509EncodedKeySpec(bytes))
        } catch (_: Exception) {
            null
        }
    }

    private fun deriveSharedKey(peerPublicKey: PublicKey): SecretKeySpec {
        val agreement = KeyAgreement.getInstance("ECDH")
        agreement.init(keyPair.private)
        agreement.doPhase(peerPublicKey, true)
        val rawSecret = agreement.generateSecret()

        val digest = MessageDigest.getInstance("SHA-256")
        digest.update("JESR-MESH-E2EE-SALT".toByteArray(Charsets.UTF_8))
        val derivedKeyBytes = digest.digest(rawSecret)
        return SecretKeySpec(derivedKeyBytes, "AES")
    }

    fun encrypt(plainText: String, peerPublicKey: PublicKey, aad: String? = null): EncryptedResult {
        val key = deriveSharedKey(peerPublicKey)
        val iv = ByteArray(12)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        if (!aad.isNullOrEmpty()) {
            cipher.updateAAD(aad.toByteArray(Charsets.UTF_8))
        }

        val ciphertext = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return EncryptedResult(
            ciphertextBase64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP),
            ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        )
    }

    fun decrypt(
        ciphertextBase64: String,
        ivBase64: String,
        peerPublicKey: PublicKey,
        aad: String? = null
    ): String {
        val key = deriveSharedKey(peerPublicKey)
        val ciphertext = Base64.decode(ciphertextBase64, Base64.NO_WRAP)
        val iv = Base64.decode(ivBase64, Base64.NO_WRAP)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        if (!aad.isNullOrEmpty()) {
            cipher.updateAAD(aad.toByteArray(Charsets.UTF_8))
        }

        val decryptedBytes = cipher.doFinal(ciphertext)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    fun decryptWithBase64Key(
        ciphertextBase64: String,
        ivBase64: String,
        peerPublicKeyBase64: String,
        aad: String? = null
    ): String {
        val pubKey = parsePublicKey(peerPublicKeyBase64)
            ?: throw IllegalArgumentException("Invalid peer public key")
        return decrypt(ciphertextBase64, ivBase64, pubKey, aad)
    }

    fun signData(data: ByteArray): String {
        val sig = Signature.getInstance("SHA256withECDSA")
        sig.initSign(signingKeyPair.private, secureRandom)
        sig.update(data)
        return Base64.encodeToString(sig.sign(), Base64.NO_WRAP)
    }

    fun verifySignature(data: ByteArray, signatureBase64: String, signingPubBase64: String): Boolean {
        if (signatureBase64.isBlank() || signingPubBase64.isBlank()) return false
        val pub = parsePublicKey(signingPubBase64) ?: return false
        return try {
            val sig = Signature.getInstance("SHA256withECDSA")
            sig.initVerify(pub)
            sig.update(data)
            sig.verify(Base64.decode(signatureBase64, Base64.NO_WRAP))
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Exports ECDH + ECDSA private/public keypair protected with an account-bound AES-256-GCM key
     * derived from UID/secret so Google Cloud Sync restores the exact same JISR identity.
     */
    fun exportCloudIdentityBundle(accountSecret: String): String {
        val inner = JSONObject().apply {
            put("ecdhPriv", Base64.encodeToString(keyPair.private.encoded, Base64.NO_WRAP))
            put("ecdhPub", myPublicKeyBase64)
            put("ecdsaPriv", Base64.encodeToString(signingKeyPair.private.encoded, Base64.NO_WRAP))
            put("ecdsaPub", mySigningPublicKeyBase64)
            put("jesrId", myJesrId)
        }
        val salt = ByteArray(16).also { secureRandom.nextBytes(it) }
        val iv = ByteArray(12).also { secureRandom.nextBytes(it) }
        val key = deriveBackupKey(accountSecret, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val ct = cipher.doFinal(inner.toString().toByteArray(Charsets.UTF_8))

        return JSONObject().apply {
            put("v", 1)
            put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
            put("ct", Base64.encodeToString(ct, Base64.NO_WRAP))
        }.toString()
    }

    fun importCloudIdentityBundle(bundleJson: String, accountSecret: String): Boolean {
        return try {
            val outer = JSONObject(bundleJson)
            val salt = Base64.decode(outer.getString("salt"), Base64.NO_WRAP)
            val iv = Base64.decode(outer.getString("iv"), Base64.NO_WRAP)
            val ct = Base64.decode(outer.getString("ct"), Base64.NO_WRAP)
            val key = deriveBackupKey(accountSecret, salt)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            val plain = String(cipher.doFinal(ct), Charsets.UTF_8)
            val inner = JSONObject(plain)

            val kf = KeyFactory.getInstance("EC")
            val ecdhPriv = kf.generatePrivate(PKCS8EncodedKeySpec(Base64.decode(inner.getString("ecdhPriv"), Base64.NO_WRAP)))
            val ecdhPub = kf.generatePublic(X509EncodedKeySpec(Base64.decode(inner.getString("ecdhPub"), Base64.NO_WRAP)))
            val ecdsaPriv = kf.generatePrivate(PKCS8EncodedKeySpec(Base64.decode(inner.getString("ecdsaPriv"), Base64.NO_WRAP)))
            val ecdsaPub = kf.generatePublic(X509EncodedKeySpec(Base64.decode(inner.getString("ecdsaPub"), Base64.NO_WRAP)))

            val newEcdh = KeyPair(ecdhPub, ecdhPriv)
            val newEcdsa = KeyPair(ecdsaPub, ecdsaPriv)
            saveKeyPairToVault(newEcdh, "wrapped_ecdh_priv", "private_key_pkcs8", "public_key_x509")
            saveKeyPairToVault(newEcdsa, "wrapped_ecdsa_priv", "signing_private_key_pkcs8", "signing_public_key_x509")
            keyPair = newEcdh
            signingKeyPair = newEcdsa
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun deriveBackupKey(secret: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(secret.toCharArray(), salt, 65536, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }
}
