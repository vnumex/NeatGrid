package com.example.neatgrid.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
object ApiKeyCipher {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "neatgrid_api_keys"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val ENC_PREFIX = "enc:v1:"
    private const val PLAIN_PREFIX = "plain:"
    private const val GCM_TAG_BITS = 128

    fun encrypt(plainText: String): String {
        if (plainText.isBlank()) return ""
        val bytes = plainText.toByteArray(Charsets.UTF_8)
        val key = getOrCreateSecretKey()
        if (key != null) {
            try {
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.ENCRYPT_MODE, key)
                val cipherText = cipher.doFinal(bytes)
                return "$ENC_PREFIX${Base64.encode(cipher.iv)}:${Base64.encode(cipherText)}"
            } catch (_: Exception) {
            }
        }
        return "$PLAIN_PREFIX${Base64.encode(bytes)}"
    }

    fun decrypt(storedValue: String): String {
        if (storedValue.isBlank()) return ""
        if (storedValue.startsWith(ENC_PREFIX)) {
            val payload = storedValue.removePrefix(ENC_PREFIX)
            val separator = payload.indexOf(':')
            if (separator <= 0 || separator == payload.lastIndex) return ""
            val key = getOrCreateSecretKey() ?: return ""
            return try {
                val iv = Base64.decode(payload.substring(0, separator))
                val cipherText = Base64.decode(payload.substring(separator + 1))
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
                String(cipher.doFinal(cipherText), Charsets.UTF_8)
            } catch (_: Exception) {
                ""
            }
        }
        if (storedValue.startsWith(PLAIN_PREFIX)) {
            return try {
                String(Base64.decode(storedValue.removePrefix(PLAIN_PREFIX)), Charsets.UTF_8)
            } catch (_: Exception) {
                ""
            }
        }
        return storedValue
    }

    @Synchronized
    private fun getOrCreateSecretKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            (keyStore.getKey(KEY_ALIAS, null) as? SecretKey) ?: generateSecretKey()
        } catch (_: Exception) {
            null
        }
    }

    private fun generateSecretKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }
}
