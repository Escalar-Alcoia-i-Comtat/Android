package com.arnyminerz.escalaralcoiaicomtat.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import timber.log.Timber
import java.io.IOException
import java.math.BigInteger
import java.security.*
import java.security.cert.CertificateException
import java.security.spec.AlgorithmParameterSpec
import java.util.*
import javax.security.auth.x500.X500Principal


const val KEYSTORE_PROVIDER_ANDROID_KEYSTORE = "AndroidKeyStore"

const val TYPE_RSA = "RSA"
const val TYPE_DSA = "DSA"
const val TYPE_BKS = "BKS"

const val SIGNATURE_SHA256withRSA = "SHA256withRSA"
const val SIGNATURE_SHA512withRSA = "SHA512withRSA"

const val KEY_ALIAS = "escalaralcoiaicomtat"

class SecurityStorage {
    /**
     * Creates a public and private key and stores it using the Android Key Store, so that only
     * this application will be able to access the keys.
     */
    @Throws(
        NoSuchProviderException::class,
        NoSuchAlgorithmException::class,
        InvalidAlgorithmParameterException::class
    )
    fun createKeys(context: Context?) {
        // BEGIN_INCLUDE(create_valid_dates)
        // Create a start and end time, for the validity range of the key pair that's about to be
        // generated.
        val start: Calendar = GregorianCalendar()
        val end: Calendar = GregorianCalendar()
        end.add(Calendar.YEAR, 1)
        //END_INCLUDE(create_valid_dates)

        // BEGIN_INCLUDE(create_keypair)
        // Initialize a KeyPair generator using the the intended algorithm (in this example, RSA
        // and the KeyStore.  This example uses the AndroidKeyStore.
        val kpGenerator: KeyPairGenerator = KeyPairGenerator
            .getInstance(
                TYPE_RSA,
                KEYSTORE_PROVIDER_ANDROID_KEYSTORE
            )
        // END_INCLUDE(create_keypair)

        // BEGIN_INCLUDE(create_spec)
        // The KeyPairGeneratorSpec object is how parameters for your key pair are passed
        // to the KeyPairGenerator.
        val spec: AlgorithmParameterSpec
        spec =
                // On Android M or above, use the KeyGenparameterSpec.Builder and specify permitted
                // properties  and restrictions of the key.
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_SIGN)
                .setCertificateSubject(X500Principal("CN=$KEY_ALIAS"))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                .setCertificateSerialNumber(BigInteger.valueOf(1337))
                .setCertificateNotBefore(start.time)
                .setCertificateNotAfter(end.time)
                .build()
        kpGenerator.initialize(spec)
        val kp = kpGenerator.generateKeyPair()
        // END_INCLUDE(create_spec)
        Timber.d("Public Key is: %s", kp.public.toString())
    }

    /**
     * Signs the data using the key pair stored in the Android Key Store.  This signature can be
     * used with the data later to verify it was signed by this application.
     * @return A string encoding of the data signature generated
     */
    @Throws(
        KeyStoreException::class,
        UnrecoverableEntryException::class,
        NoSuchAlgorithmException::class,
        InvalidKeyException::class,
        SignatureException::class,
        IOException::class,
        CertificateException::class
    )
    fun signData(inputStr: String): String? {
        val data = inputStr.toByteArray()

        // BEGIN_INCLUDE(sign_load_keystore)
        val ks = KeyStore.getInstance(KEYSTORE_PROVIDER_ANDROID_KEYSTORE)

        // Weird artifact of Java API.  If you don't have an InputStream to load, you still need
        // to call "load", or it'll crash.
        ks.load(null)

        // Load the key pair from the Android Key Store
        val entry = ks.getEntry(KEY_ALIAS, null)

        /* If the entry is null, keys were never stored under this alias.
         * Debug steps in this situation would be:
         * -Check the list of aliases by iterating over Keystore.aliases(), be sure the alias
         *   exists.
         * -If that's empty, verify they were both stored and pulled from the same keystore
         *   "AndroidKeyStore"
         */if (entry == null) {
            Timber.w("No key found under alias: $KEY_ALIAS")
            Timber.w("Exiting signData()...")
            return null
        }

        /* If entry is not a KeyStore.PrivateKeyEntry, it might have gotten stored in a previous
         * iteration of your application that was using some other mechanism, or been overwritten
         * by something else using the same keystore with the same alias.
         * You can determine the type using entry.getClass() and debug from there.
         */if (entry !is KeyStore.PrivateKeyEntry) {
            Timber.w("Not an instance of a PrivateKeyEntry")
            Timber.w("Exiting signData()...")
            return null
        }
        // END_INCLUDE(sign_data)

        // BEGIN_INCLUDE(sign_create_signature)
        // This class doesn't actually represent the signature,
        // just the engine for creating/verifying signatures, using
        // the specified algorithm.
        val s: Signature = Signature.getInstance(SIGNATURE_SHA256withRSA)

        // Initialize Signature using specified private key
        s.initSign(entry.privateKey)

        // Sign the data, store the result as a Base64 encoded String.
        s.update(data)
        val signature: ByteArray = s.sign()
        // END_INCLUDE(sign_data)
        return Base64.encodeToString(signature, Base64.DEFAULT)
    }

    /**
     * Given some data and a signature, uses the key pair stored in the Android Key Store to verify
     * that the data was signed by this application, using that key pair.
     * @param input The data to be verified.
     * @param signatureStr The signature provided for the data.
     * @return A boolean value telling you whether the signature is valid or not.
     */
    @Throws(
        KeyStoreException::class,
        CertificateException::class,
        NoSuchAlgorithmException::class,
        IOException::class,
        UnrecoverableEntryException::class,
        InvalidKeyException::class,
        SignatureException::class
    )
    fun verifyData(input: String, signatureStr: String?): Boolean {
        val data = input.toByteArray()
        // BEGIN_INCLUDE(decode_signature)

        // Make sure the signature string exists.  If not, bail out, nothing to do.
        if (signatureStr == null) {
            Timber.w("Invalid signature.")
            Timber.w("Exiting verifyData()...")
            return false
        }
        val signature: ByteArray = try {
            // The signature is going to be examined as a byte array,
            // not as a base64 encoded string.
            Base64.decode(signatureStr, Base64.DEFAULT)
        } catch (e: IllegalArgumentException) {
            // signatureStr wasn't null, but might not have been encoded properly.
            // It's not a valid Base64 string.
            return false
        }
        // END_INCLUDE(decode_signature)
        val ks = KeyStore.getInstance("AndroidKeyStore")

        // Weird artifact of Java API.  If you don't have an InputStream to load, you still need
        // to call "load", or it'll crash.
        ks.load(null)

        // Load the key pair from the Android Key Store
        val entry = ks.getEntry(KEY_ALIAS, null)
        if (entry == null) {
            Timber.w("No key found under alias: $KEY_ALIAS")
            Timber.w("Exiting verifyData()...")
            return false
        }
        if (entry !is KeyStore.PrivateKeyEntry) {
            Timber.w("Not an instance of a PrivateKeyEntry")
            return false
        }

        // This class doesn't actually represent the signature,
        // just the engine for creating/verifying signatures, using
        // the specified algorithm.
        val s: Signature = Signature.getInstance(SIGNATURE_SHA256withRSA)

        // BEGIN_INCLUDE(verify_data)
        // Verify the data.
        s.initVerify(entry.certificate)
        s.update(data)
        return s.verify(signature)
        // END_INCLUDE(verify_data)
    }
}