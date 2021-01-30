package messages

case class SignedMessage[T](signerID: Int, msg: T, sign: Array[Byte])
