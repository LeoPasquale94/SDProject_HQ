package Messages

case class SignedMessage[T](signerID: String, mex: T, sign: Array[Byte])
