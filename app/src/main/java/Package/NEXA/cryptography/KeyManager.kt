package Package.NEXA.cryptography

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import android.util.Base64
import android.util.Log
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.InvalidKeySpecException
import java.security.spec.MGF1ParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
//import javax.crypto.spec.MGF1ParameterSpec
import javax.crypto.spec.PSource

//import java.util.Base64

/**
 * Class to generate public and private keys
 * Encrypts and Decodes messages using the keys
 */
object KeyManager {
    val KeyAlias = "NexaIDKey"

    /**
     * Function to generate the public and Private Key Pairs
     * Creates key generator for RSA keys, which is then stored in the android keystore
     * Defines what the key should do
     */
    fun generateKeyPair() : KeyPair
    {
        val keyGen = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore")

        val parm = KeyGenParameterSpec.Builder(KeyAlias, KeyProperties.PURPOSE_ENCRYPT
                or KeyProperties.PURPOSE_DECRYPT)
            .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA1)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
            .build()

        keyGen.initialize(parm)
        return keyGen.generateKeyPair()
    }

    /**
     * Function to return the user's public key
     * Loads the android keystore and retrieves public gey from keyStore
     */
    fun getPublicKey64(): String?
    {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        val pubKey = keyStore.getCertificate(KeyAlias)?.publicKey
        return pubKey?.encoded?.let { Base64.encodeToString(it, Base64.NO_WRAP) //return public key encoded as base 64
        }

    }

    /**
     * Function for encrypting message with RSA
     * @param message the Message we wish to encrypt
     * @param pubKey The public key used to encrypt the message
     * Uses Cipher Library to encrypt using the RSA method
     * returns the encoded string
     */
    fun encrypt(message: String, pubKey: PublicKey): String?
    {
        return try {
            val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding") //RSA cipher
            val spec = OAEPParameterSpec("SHA-256", "MGF1",
                MGF1ParameterSpec("SHA-1"), PSource.PSpecified.DEFAULT)
            cipher.init(Cipher.ENCRYPT_MODE, pubKey, spec)

            val eBytes = cipher.doFinal(message.toByteArray(Charsets.UTF_8)) //encrypt with public key
            return Base64.encodeToString(eBytes, Base64.NO_WRAP) //encode
        }
        catch(t : Throwable)
        {
            Log.e("KeyManager", "Encrypt failed: ${t.message}")
            null
        }
    }

    /**
     * Function for decrypting the message with the private key and then returns the message as normal text
     * @param message Message we wish to decrypt
     * @param privKey The Private Key used to decrypt the message
     * Returns the string format of the message
     */
    fun decrypt(message: String, privKey: PrivateKey): String?
    {
        return try{
            val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
            val spec = OAEPParameterSpec("SHA-256", "MGF1",
                MGF1ParameterSpec("SHA-1"), PSource.PSpecified.DEFAULT)

            cipher.init(Cipher.DECRYPT_MODE, privKey, spec) //Config for decryption
            val decodeByte = Base64.decode(message, Base64.NO_WRAP) //decode
            val ans = cipher.doFinal(decodeByte) //decrypt
            return String(ans, Charsets.UTF_8) //return a string

        }
        catch(t : Throwable)
        {
            Log.e("KeyManager", "Decrypt failed: ${t.message}")
            null
        }

    }

    /**
     * Decodes the Base64 key string to a public key object
     * @param pubKeyString The Public Key string we want to decode to a Public Key object
     * Remove any PEM headers/footers and whitespace
     * Decodes Base64 to bytes
     * Creates KeySpec
     * Generate public key (assuming RSA, change if needed)
     */
    fun decodePubKey(pubKeyString: String): PublicKey? {
        try {
            val cleanKey = pubKeyString
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("\\s".toRegex(), "")
            val decodedBytes = Base64.decode(cleanKey, Base64.DEFAULT)
            val keySpec = X509EncodedKeySpec(decodedBytes)
            val keyFactory = KeyFactory.getInstance("RSA")
            return keyFactory.generatePublic(keySpec)
        }
        catch (e: IllegalArgumentException) {
            Log.e("KeyManager", "Base64 decoding failed: ${e.message}")
        }
        catch (e: InvalidKeySpecException) {
            Log.e("KeyManager", "Invalid public key: ${e.message}")
        }
        catch (e: Exception) {
            Log.e("KeyManager", "Unexpected error decoding public key: ${e.message}")
        }
        return null
    }

    /**
     * Gets the private key from the Keystore
     * Load android keystore
     * Retrieves Private Key from KeyStore
     */
    fun getPrivKey() : PrivateKey?
    {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load (null)
        val priv = keyStore.getKey(KeyAlias, null) as? PrivateKey
        return priv
    }
}