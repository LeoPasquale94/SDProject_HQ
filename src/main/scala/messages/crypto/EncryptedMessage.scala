package messages.crypto

import messages.Message

class EncryptedMessage[T](private val privateKey: T) {

  def encrypt(msg: Message): Array[Byte] = ???

  def decrypt(msg: Array[Byte]): Message = ???
}
