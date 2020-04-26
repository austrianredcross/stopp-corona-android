package at.roteskreuz.stopcorona.model.repositories

import android.os.Build
import android.security.KeyPairGeneratorSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.annotation.RequiresApi
import at.roteskreuz.stopcorona.constants.Constants.Security.ADDRESS_PREFIX_LENGTH
import at.roteskreuz.stopcorona.constants.Constants.Security.CRYPTO_PROVIDER_CALLER_POSITION
import at.roteskreuz.stopcorona.constants.Constants.Security.FINGERPRINT_ALGORITHM
import at.roteskreuz.stopcorona.constants.Constants.Security.FULL_ALGORITHM
import at.roteskreuz.stopcorona.constants.Constants.Security.KEYSTORE
import at.roteskreuz.stopcorona.constants.Constants.Security.KEYSTORE_ALIAS
import at.roteskreuz.stopcorona.constants.Constants.Security.KEY_ALGORITHM
import at.roteskreuz.stopcorona.constants.Constants.Security.KEY_SIZE
import at.roteskreuz.stopcorona.constants.Constants.Security.KEY_VALIDILITY_YEARS
import at.roteskreuz.stopcorona.constants.Constants.Security.X500_PRINCIPAL_NAME
import org.spongycastle.asn1.ASN1Primitive
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo
import org.spongycastle.jce.provider.BouncyCastleProvider
import org.spongycastle.openssl.PEMParser
import org.spongycastle.util.io.pem.PemObject
import org.spongycastle.util.io.pem.PemWriter
import timber.log.Timber
import java.io.StringReader
import java.io.StringWriter
import java.math.BigInteger
import java.security.*
import java.security.cert.Certificate
import java.security.spec.X509EncodedKeySpec
import java.util.Calendar
import javax.crypto.Cipher
import javax.security.auth.x500.X500Principal

/**
 * Repository for managing public and private keys as well as encryption and decription
 *
 * The key pair will be lazily created and stored in androids key chain when first needed.
 */
interface CryptoRepository {

    /**
     * Our public key in PKCS#1 encoding.
     *
     * Call from background. The operation might be expensive if the key pair has to be created.
     */
    val publicKeyPKCS1: ByteArray

    /**
     * String of first bits of the fingerprint of our public key.
     *
     * Call from background. The operation might be expensive if the key pair has to be created.
     */
    val publicKeyPrefix: String

    /**
     * Decrypts a [cypherText] using our private key.
     *
     * Call from background. This operation might be expensive.
     *
     * @param cypherText
     * @return plain text or null if not decrypted.
     */
    fun decrypt(cypherText: String): ByteArray?

    /**
     * Encrypts the [plainText] with the given PKCS#1 encoded public key.
     *
     * Call from background. This operation might be expensive.
     *
     * @param plainText
     * @param encodedPublicKey encoded form of the public key
     * @return cypherText
     */
    fun encrypt(plainText: ByteArray, encodedPublicKey: ByteArray): ByteArray?

    /**
     * String of first bits of the fingerprint of the given PKCS#1 [encodedPublicKey]
     *
     * @param encodedPublicKey
     * @return String of binary digits. I.e. "00100100"
     */
    fun getPublicKeyPrefix(encodedPublicKey: ByteArray): String
}

class CryptoRepositoryImpl(
    @Suppress("DEPRECATION")
    private val keyPairGeneratorSpecBuilder: KeyPairGeneratorSpec.Builder
) : CryptoRepository {

    companion object {
        init {
            Security.insertProviderAt(BouncyCastleProvider(), CRYPTO_PROVIDER_CALLER_POSITION)
        }
    }

    private val keyStore by lazy {
        KeyStore.getInstance(KEYSTORE).apply { load(null) }
    }

    private val keyPair: Certificate by lazy {
        if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {
            Timber.d("### Creating keys")
            createKeyPair()
        }
        keyStore.getCertificate(KEYSTORE_ALIAS)
    }

    private val publicKey: PublicKey by lazy {
        keyPair.publicKey
    }

    override val publicKeyPKCS1: ByteArray by lazy {
        val spkInfo: SubjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(publicKeyX509)
        val primitive: ASN1Primitive = spkInfo.parsePublicKey()
        primitive.encoded
    }

    override val publicKeyPrefix by lazy { getPublicKeyPrefix(publicKeyPKCS1) }

    override fun decrypt(cypherText: String): ByteArray? {
        return try {
            val cipher: Cipher = Cipher.getInstance(FULL_ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, privateKey)
            val byteArray = Base64.decode(cypherText, Base64.DEFAULT)
            cipher.doFinal(byteArray)
        } catch (e: Exception) {
            // ignored exception, it is ok if messages don't belong to user
            null
        }
    }

    override fun encrypt(plainText: ByteArray, encodedPublicKey: ByteArray): ByteArray? {
        return decodePKCS1PublicKey(encodedPublicKey)?.let { publicKey ->
            val cipher: Cipher = Cipher.getInstance(FULL_ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, publicKey)
            cipher.doFinal(plainText)
        }
    }

    override fun getPublicKeyPrefix(encodedPublicKey: ByteArray): String {
        val md = MessageDigest.getInstance(FINGERPRINT_ALGORITHM)
        val fingerprint = md.digest(encodedPublicKey)

        val prefixString = fingerprint
            // Do not create intermediate lists
            .asSequence()
            // Convert byte to int for arithmethic compatibility
            .map {
                it.toInt()
            }
            // map byte to 8 bits
            .flatMap { currentByte ->
                (7 downTo 0).asSequence()
                    .map { currentBitPos ->
                        (currentByte shr (currentBitPos)) and 0x01
                    }
            }
            // get relevant bits
            .take(ADDRESS_PREFIX_LENGTH)
            // map bits to digit string ("1"/"0")
            .map { prefixBit ->
                prefixBit.toString()
            }
            // join digits to string
            .joinToString("")

        return prefixString
    }

    private val publicKeyX509: ByteArray by lazy {
        publicKey.encoded
    }

    private fun decodePKCS1PublicKey(encodedPublicKey: ByteArray): PublicKey? {
        return try {
            derToPublicKey(pemToDer(pkcs1ToPem(encodedPublicKey)))
        } catch (e: Exception) {
            // Non exceptional exception. Not a public key
            null
        }
    }

    private fun pkcs1ToPem(publicKeyPKCS1: ByteArray): String {
        val pemObject = PemObject("RSA PUBLIC KEY", publicKeyPKCS1)
        val stringWriter = StringWriter()
        val pemWriter = PemWriter(stringWriter)
        pemWriter.writeObject(pemObject)
        pemWriter.close()
        return stringWriter.toString()
    }

    private fun pemToDer(publicKeyPem: String): ByteArray {
        val pemParser = PEMParser(StringReader(publicKeyPem))
        val pemObject: Any = pemParser.readObject()
        return (pemObject as SubjectPublicKeyInfo).encoded
    }

    private fun derToPublicKey(asn1key: ByteArray): PublicKey {
        val spec = X509EncodedKeySpec(asn1key)
        val keyFactory = KeyFactory.getInstance(KEY_ALGORITHM)
        return keyFactory.generatePublic(spec)
    }

    private val privateKey by lazy {
        val privateKeyEntry = keyStore.getEntry(KEYSTORE_ALIAS, null) as KeyStore.PrivateKeyEntry
        privateKeyEntry.privateKey
    }

    private fun createKeyPair() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            createKeyPairApiMOrHigher()
        } else {
            createKeyPairApiSmallerM()
        }
    }

    @Suppress("DEPRECATION")
    private fun createKeyPairApiSmallerM(): KeyPair {
        val startDate = Calendar.getInstance()
        val endDate = Calendar.getInstance().apply { add(Calendar.YEAR, KEY_VALIDILITY_YEARS) }

        val spec = keyPairGeneratorSpecBuilder
            .setAlias(KEYSTORE_ALIAS)
            .setSubject(X500Principal(X500_PRINCIPAL_NAME))
            .setSerialNumber(BigInteger.ONE)
            .setStartDate(startDate.time)
            .setEndDate(endDate.time)
            .setKeySize(KEY_SIZE)
            .build()

        val generator = KeyPairGenerator.getInstance(KEY_ALGORITHM, KEYSTORE).apply { initialize(spec) }
        return generator.generateKeyPair()
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun createKeyPairApiMOrHigher(): KeyPair {
        val startDate = Calendar.getInstance()
        val endDate = Calendar.getInstance().apply { add(Calendar.YEAR, KEY_VALIDILITY_YEARS) }

        val spec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT or KeyProperties.PURPOSE_SIGN)
            .setCertificateSubject(X500Principal(X500_PRINCIPAL_NAME))
            .setCertificateSerialNumber(BigInteger.ONE)
            .setKeyValidityStart(startDate.time)
            .setKeyValidityEnd(endDate.time)
            .setKeySize(KEY_SIZE)
            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PSS)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
            .setRandomizedEncryptionRequired(true)
            .build()

        val generator = KeyPairGenerator.getInstance(KEY_ALGORITHM, KEYSTORE).apply { initialize(spec) }
        return generator.generateKeyPair()
    }
}
