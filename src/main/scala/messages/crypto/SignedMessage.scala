package messages.crypto

import messages.Message

class SignedMessage[T](private val privateKey: T, private val publicKey: T ) {

  def sign(msg: Message): Array[Byte] = ???

  def verify(msg: Message, publicKey: T): Boolean = ???
}
