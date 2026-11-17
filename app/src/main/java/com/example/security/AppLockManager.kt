package com.example.security

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import java.security.MessageDigest
import java.security.SecureRandom

object AppLockManager {

    private val random = SecureRandom()

    fun generateSalt(): String {
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hashSecret(secret: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$salt:$secret".toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun verifySecret(candidate: String, salt: String?, expectedHash: String?): Boolean {
        if (salt.isNullOrBlank() || expectedHash.isNullOrBlank()) return false
        val actual = hashSecret(candidate, salt)
        return MessageDigest.isEqual(actual.toByteArray(Charsets.UTF_8), expectedHash.toByteArray(Charsets.UTF_8))
    }

    fun encodePattern(dots: List<Int>): String = dots.joinToString("-")

    fun isBiometricHardwareSupported(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val manager = context.getSystemService(BiometricManager::class.java) ?: return false
                val res = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.BIOMETRIC_STRONG)
                } else {
                    @Suppress("DEPRECATION")
                    manager.canAuthenticate()
                }
                res == BiometricManager.BIOMETRIC_SUCCESS
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
            } else {
                false
            }
        } catch (_: Throwable) {
            false
        }
    }

    fun hasFingerprintFeature(context: Context): Boolean {
        return try {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                (context.packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT) ||
                    isBiometricHardwareSupported(context))
        } catch (_: Throwable) {
            false
        }
    }

    fun showBiometricPrompt(
        context: Context,
        title: String,
        subtitle: String,
        negativeButtonText: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            onError("Biometric prompt requires Android 9+")
            return
        }
        try {
            val cancellationSignal = CancellationSignal()
            val builder = BiometricPrompt.Builder(context)
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButton(
                    negativeButtonText,
                    context.mainExecutor
                ) { _, _ ->
                    onError(negativeButtonText)
                }

            val prompt = builder.build()
            prompt.authenticate(
                cancellationSignal,
                context.mainExecutor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                        super.onAuthenticationSucceeded(result)
                        onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                        super.onAuthenticationError(errorCode, errString)
                        onError(errString?.toString().orEmpty())
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                    }
                }
            )
        } catch (t: Throwable) {
            onError(t.message ?: "Biometric unavailable")
        }
    }
}
