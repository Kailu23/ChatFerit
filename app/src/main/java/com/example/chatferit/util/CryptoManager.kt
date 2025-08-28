package com.example.chatferit.util

import android.content.Context
import android.util.Base64
import android.util.Log
import com.google.crypto.tink.Aead
import com.google.crypto.tink.CleartextKeysetHandle
import com.google.crypto.tink.HybridDecrypt
import com.google.crypto.tink.HybridEncrypt
import com.google.crypto.tink.JsonKeysetReader
import com.google.crypto.tink.JsonKeysetWriter
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.PublicKeySign
import com.google.crypto.tink.PublicKeyVerify
import com.google.crypto.tink.hybrid.HybridConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.google.crypto.tink.signature.SignatureConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton // Marks this class as a Singleton provided by Hilt
class CryptoManager @Inject constructor(
    // Hilt will inject the ApplicationContext
    @ApplicationContext private val context: Context
) {

    // Companion object for constants is fine, or they can be private const val within the class.
    companion object {
        private const val PREF_FILE_NAME = "chat_ferit_keyset_prefs"
        private const val KEYSET_NAME_HYBRID = "chat_ferit_hybrid_keyset"
        private const val MASTER_KEY_URI_HYBRID = "android-keystore://chat_ferit_hybrid_master_key"

        private const val KEYSET_NAME_SIGNATURE = "chat_ferit_signature_keyset"
        private const val MASTER_KEY_URI_SIGNATURE = "android-keystore://chat_ferit_signature_master_key"

        private val HYBRID_CONTEXT_INFO = byteArrayOf()
    }

    init {
        try {
            HybridConfig.register()
            SignatureConfig.register()
        } catch (e: GeneralSecurityException) {
            Log.e("CryptoManager", "Failed to initialize Tink CryptoManager", e)
            // Consider a more graceful way to handle this if the app can function partially without crypto
            throw RuntimeException("Failed to initialize Tink CryptoManager", e)
        }
    }

    @Throws(GeneralSecurityException::class, IOException::class)
    private fun getOrGenerateHybridKeysetHandle(): KeysetHandle {
        return AndroidKeysetManager.Builder()
            .withSharedPref(this.context, KEYSET_NAME_HYBRID, PREF_FILE_NAME)
            .withKeyTemplate(KeyTemplates.get("ECIES_P256_HKDF_HMAC_SHA256_AES128_GCM"))
            .withMasterKeyUri(MASTER_KEY_URI_HYBRID)
            .build()
            .keysetHandle
    }

    @Throws(GeneralSecurityException::class, IOException::class)
    fun getLocalUserHybridPublicKey(): String {
        val publicKeysetHandle = getOrGenerateHybridKeysetHandle().publicKeysetHandle
        return serializeKeysetHandleToJson(publicKeysetHandle)
    }

    @Throws(GeneralSecurityException::class, IOException::class)
    fun encryptHybrid(plaintext: String, recipientPublicKey: String): String {
        val recipientPublicKeysetHandle = deserializeJsonToKeysetHandle(recipientPublicKey)
        val hybridEncrypt = recipientPublicKeysetHandle.getPrimitive(HybridEncrypt::class.java)
        val plaintextBytes = plaintext.toByteArray(StandardCharsets.UTF_8)
        val ciphertextBytes = hybridEncrypt.encrypt(plaintextBytes, HYBRID_CONTEXT_INFO)
        return Base64.encodeToString(ciphertextBytes, Base64.NO_WRAP)
    }

    @Throws(GeneralSecurityException::class, IOException::class)
    fun decryptHybrid(encryptedPayloadBase64: String): String {
        val privateKeysetHandle = getOrGenerateHybridKeysetHandle() // Gets local private key
        val hybridDecrypt = privateKeysetHandle.getPrimitive(HybridDecrypt::class.java)
        val ciphertextBytes = Base64.decode(encryptedPayloadBase64, Base64.NO_WRAP)
        val decryptedBytes = hybridDecrypt.decrypt(ciphertextBytes, HYBRID_CONTEXT_INFO)
        return String(decryptedBytes, StandardCharsets.UTF_8)
    }

    @Throws(GeneralSecurityException::class, IOException::class)
    private fun getOrGenerateSignatureKeysetHandle(): KeysetHandle {
        // Now uses the injected 'context' field
        return AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME_SIGNATURE, PREF_FILE_NAME)
            .withKeyTemplate(KeyTemplates.get("ECDSA_P256"))
            .withMasterKeyUri(MASTER_KEY_URI_SIGNATURE)
            .build()
            .keysetHandle
    }

    @Throws(GeneralSecurityException::class, IOException::class)
    fun getLocalUserSignaturePublicKey(): String {
        val publicKeysetHandle = getOrGenerateSignatureKeysetHandle().publicKeysetHandle
        return serializeKeysetHandleToJson(publicKeysetHandle)
    }

    @Throws(GeneralSecurityException::class, IOException::class)
    fun getSigner(): PublicKeySign {
        return getOrGenerateSignatureKeysetHandle().getPrimitive(PublicKeySign::class.java)
    }

    @Throws(GeneralSecurityException::class, IOException::class)
    fun getVerifier(senderVerificationKey: String): PublicKeyVerify {
        val senderVerificationKeysetHandle = deserializeJsonToKeysetHandle(senderVerificationKey)
        return senderVerificationKeysetHandle.getPrimitive(PublicKeyVerify::class.java)
    }

    @Throws(GeneralSecurityException::class, IOException::class)
    fun serializeKeysetHandleToJson(keysetHandle: KeysetHandle): String {
        val outputStream = ByteArrayOutputStream()
        CleartextKeysetHandle.write(keysetHandle, JsonKeysetWriter.withOutputStream(outputStream))
        return outputStream.toString(StandardCharsets.UTF_8.name())
    }

    @Throws(GeneralSecurityException::class, IOException::class)
    fun deserializeJsonToKeysetHandle(jsonKeyset: String): KeysetHandle {
        return CleartextKeysetHandle.read(JsonKeysetReader.withBytes(jsonKeyset.toByteArray(StandardCharsets.UTF_8)))
    }

    @Throws(GeneralSecurityException::class)
    fun generateAeadKeysetHandle(): KeysetHandle {
        return KeysetHandle.generateNew(KeyTemplates.get("AES128_GCM"))
    }

    @Throws(GeneralSecurityException::class)
    fun getAead(keysetHandle: KeysetHandle): Aead {
        return keysetHandle.getPrimitive(Aead::class.java)
    }
}
