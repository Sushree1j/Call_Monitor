package com.callmonitor.encryption

import android.content.Context
import android.util.Base64
import android.util.Log
import com.callmonitor.R
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyFactory
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Hybrid encryption manager using RSA + AES-GCM
 * 
 * File format:
 * [4 bytes: encrypted AES key length]
 * [N bytes: RSA-encrypted AES key]
 * [12 bytes: GCM IV/nonce]
 * [remaining: AES-GCM encrypted audio data + 16 byte auth tag]
 */
class EncryptionManager(private val context: Context) {

    companion object {
        private const val TAG = "EncryptionManager"
        private const val AES_KEY_SIZE = 256
        private const val GCM_IV_SIZE = 12
        private const val GCM_TAG_SIZE = 128
        private const val RSA_ALGORITHM = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
        private const val AES_ALGORITHM = "AES/GCM/NoPadding"
    }

    private val rsaPublicKey: PublicKey by lazy { loadPublicKey() }

    /**
     * Encrypts a file using hybrid RSA + AES-GCM encryption
     * The app cannot decrypt this - only the server with the private key can
     */
    fun encryptFile(inputFile: File, outputFile: File) {
        Log.d(TAG, "Encrypting file: ${inputFile.name}")

        // Generate random AES key for this file
        val aesKey = generateAESKey()
        
        // Generate random IV for GCM
        val iv = ByteArray(GCM_IV_SIZE)
        SecureRandom().nextBytes(iv)

        // Encrypt the AES key with RSA public key
        val encryptedAesKey = encryptAESKeyWithRSA(aesKey)

        // Read the input file
        val inputData = FileInputStream(inputFile).use { it.readBytes() }

        // Encrypt the audio data with AES-GCM
        val cipher = Cipher.getInstance(AES_ALGORITHM)
        val gcmSpec = GCMParameterSpec(GCM_TAG_SIZE, iv)
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec)
        val encryptedData = cipher.doFinal(inputData)

        // Write the encrypted file
        FileOutputStream(outputFile).use { fos ->
            // Write encrypted AES key length (4 bytes, big-endian)
            val keyLength = encryptedAesKey.size
            fos.write((keyLength shr 24) and 0xFF)
            fos.write((keyLength shr 16) and 0xFF)
            fos.write((keyLength shr 8) and 0xFF)
            fos.write(keyLength and 0xFF)

            // Write encrypted AES key
            fos.write(encryptedAesKey)

            // Write IV
            fos.write(iv)

            // Write encrypted data
            fos.write(encryptedData)
        }

        Log.d(TAG, "File encrypted successfully: ${outputFile.name}")
    }

    private fun generateAESKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(AES_KEY_SIZE, SecureRandom())
        return keyGenerator.generateKey()
    }

    private fun encryptAESKeyWithRSA(aesKey: SecretKey): ByteArray {
        val cipher = Cipher.getInstance(RSA_ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, rsaPublicKey)
        return cipher.doFinal(aesKey.encoded)
    }

    private fun loadPublicKey(): PublicKey {
        // Load the PEM-encoded public key from resources
        val keyString = context.resources.openRawResource(R.raw.public_key)
            .bufferedReader()
            .readText()
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\\s".toRegex(), "")

        val keyBytes = Base64.decode(keyString, Base64.DEFAULT)
        val keySpec = X509EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        
        return keyFactory.generatePublic(keySpec)
    }
}
