package messages

case class SignedMessage[T](signerID: String, msg: T, sign: Array[Byte])
