package com.example.chatferit.util // Or your preferred package

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.CleartextKeysetHandle
import com.google.crypto.tink.HybridDecrypt
import com.google.crypto.tink.HybridEncrypt
import com.google.crypto.tink.JsonKeysetReader.withBytes
import com.google.crypto.tink.JsonKeysetWriter
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.PublicKeySign
import com.google.crypto.tink.PublicKeyVerify
import com.google.crypto.tink.hybrid.HybridConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.google.crypto.tink.signature.SignatureConfig
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException

object CryptoManager {

    private const val PREF_FILE_NAME = "chat_ferit_keyset_prefs"
    private const val KEYSET_NAME_HYBRID = "chat_ferit_hybrid_keyset"
    private const val MASTER_KEY_URI_HYBRID = "android-keystore://chat_ferit_hybrid_master_key"

    private const val KEYSET_NAME_SIGNATURE = "chat_ferit_signature_keyset"
    private const val MASTER_KEY_URI_SIGNATURE = "android-keystore://chat_ferit_signature_master_key"


    init {
        try {
            HybridConfig.register()
            SignatureConfig.register()
        } catch (e: GeneralSecurityException) {
            // Handle initialization error, e.g., log it
            // This usually means Tink couldn't register its primitives
            throw RuntimeException("Failed to initialize Tink CryptoManager", e)
        }
    }

    @Throws(GeneralSecurityException::class, IOException::class)
    private fun getOrGenerateHybridKeysetHandle(context: Context): KeysetHandle {
        return AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME_HYBRID, PREF_FILE_NAME)
            .withKeyTemplate(KeyTemplates.get("ECIES_P256_HKDF_HMAC_SHA256_AES128_GCM"))
            .withMasterKeyUri(MASTER_KEY_URI_HYBRID)
            .build()
            .keysetHandle
    }

    @Throws(GeneralSecurityException::class, IOException::class)
    fun getHybridPublicKeyHandle(context: Context): KeysetHandle {
        return getOrGenerateHybridKeysetHandle(context).publicKeysetHandle
    }

    @Throws(GeneralSecurityException::class)
    fun getHybridEncrypt(recipientPublicKeyHandle: KeysetHandle): HybridEncrypt {
        return recipientPublicKeyHandle.getPrimitive(HybridEncrypt::class.java)
    }

    @Throws(GeneralSecurityException::class, IOException::class)
    fun getHybridDecrypt(context: Context): HybridDecrypt {
        return getOrGenerateHybridKeysetHandle(context).getPrimitive(HybridDecrypt::class.java)
    }

    @Throws(GeneralSecurityException::class, IOException::class)
    private fun getOrGenerateSignatureKeysetHandle(context: Context): KeysetHandle {
        return AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME_SIGNATURE, PREF_FILE_NAME)
            .withKeyTemplate(KeyTemplates.get("ECDSA_P256")) // Common signature template
            .withMasterKeyUri(MASTER_KEY_URI_SIGNATURE)
            .build()
            .keysetHandle
    }

    @Throws(GeneralSecurityException::class, IOException::class)
    fun getSignatureVerificationKeyHandle(context: Context): KeysetHandle {
        return getOrGenerateSignatureKeysetHandle(context).publicKeysetHandle
    }

    @Throws(GeneralSecurityException::class, IOException::class)
    fun getSigner(context: Context): PublicKeySign {
        return getOrGenerateSignatureKeysetHandle(context).getPrimitive(PublicKeySign::class.java)
    }

    @Throws(GeneralSecurityException::class)
    fun getVerifier(senderVerificationKeyHandle: KeysetHandle): PublicKeyVerify {
        return senderVerificationKeyHandle.getPrimitive(PublicKeyVerify::class.java)
    }

    @Throws(GeneralSecurityException::class)
    fun generateAeadKeysetHandle(): KeysetHandle {
        return KeysetHandle.generateNew(KeyTemplates.get("AES128_GCM"))
    }

    @Throws(GeneralSecurityException::class)
    fun getAead(keysetHandle: KeysetHandle): Aead {
        return keysetHandle.getPrimitive(Aead::class.java)
    }

    @Throws(GeneralSecurityException::class, IOException::class)
    fun serializeKeysetHandleToJson(keysetHandle: KeysetHandle): String {
        val outputStream =
            ByteArrayOutputStream()
        CleartextKeysetHandle.write(keysetHandle, JsonKeysetWriter.withOutputStream(outputStream))
        return outputStream.toString(StandardCharsets.UTF_8.name())
    }

    @Throws(GeneralSecurityException::class, IOException::class)
    fun deserializeJsonToKeysetHandle(jsonKeyset: String): KeysetHandle {
        return CleartextKeysetHandle.read(withBytes(jsonKeyset.toByteArray(StandardCharsets.UTF_8)))
    }
}
