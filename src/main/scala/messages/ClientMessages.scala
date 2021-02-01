package messages

import AuthenticationCertification.{Certificate, GrantTS}

//TODO codice del messaggio in message
case class RequireWriteMessage[T](objectID: Int, op: T => T, nOp: Int)

case class RequireReadMessage(objectID: Int, nOp: Int)

case class Write1Message[T](clientID: Int, objectID: Int, numberOperation: Int,  op: T => T)

case class Write2Message(writeC: Certificate[GrantTS])

case class ReadMessage(clientID: Int, objectID: Int, nonce: Double = Math.random())

case class ResolveMessage[T](conflictC: Certificate[Write1OKMessage], write1Message: Write1Message[T])

case class WriteBackWriteMessage[T](writeC: Certificate[GrantTS], write1Message: Write1Message[T])

case class WriteBackReadMessage[T](writeC: Certificate[GrantTS], clietID: Int,
                                objectID: Int, nonce: Double = Math.random())


