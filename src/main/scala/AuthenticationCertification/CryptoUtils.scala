package AuthenticationCertification

import messages.{EncryptedMessage, SignedMessage}

import java.security.SecureRandom
import javax.crypto.spec.{IvParameterSpec, SecretKeySpec}
import javax.crypto.{Cipher, KeyGenerator}


object Crypto {

  private val ENCRYPT_ALGO = "AES/CBC/PKCS5PADDING"
  private val initVector = "encryptionIntVec"
  private val CHAR_SET_NAME = "UTF-8"
  private val BITS_AES_SECRET_KEY = 128

  var key1:SecretKeySpec = getAESKey(BITS_AES_SECRET_KEY)

  def toSign[T](idSigner: Int, mex: T): SignedMessage [T] = SignedMessage(idSigner, mex, sign(mex, searchKey(idSigner)))

  def checkMex [T] (signedMessage: SignedMessage [T]):Boolean = sign(signedMessage.msg, searchKey(signedMessage.signerID)).deep == signedMessage.sign.deep

  def sign [T] (mex: T, key: SecretKeySpec ): Array[Byte] = {

    val iv = new IvParameterSpec(initVector.getBytes(CHAR_SET_NAME))

    val cipher = Cipher.getInstance(ENCRYPT_ALGO)
    cipher.init(Cipher.ENCRYPT_MODE, key, iv)
    val sign = cipher.doFinal(mex.hashCode().toString.getBytes)
    sign
  }

  def encrypt[T] (idSigner: Int, msg: T): EncryptedMessage  = EncryptedMessage(idSigner,  sign(msg, searchKey(idSigner)))

  private def searchKey (idOwner: Int): SecretKeySpec = key1

  private def getAESKey(keysize: Int): SecretKeySpec = {
    val keyGen = KeyGenerator.getInstance("AES")
    keyGen.init(keysize, SecureRandom.getInstanceStrong)
    val sicretKey =keyGen.generateKey()
    val keySpec = new SecretKeySpec(sicretKey.getEncoded, "AES")
    keySpec
  }

}
