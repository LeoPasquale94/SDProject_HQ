package messages

case class SignedMessage[T](signerID: Int, msg: T, sign: Array[Byte])

case class EncryptedMessage(senderID: Int, msg: Array[Byte])