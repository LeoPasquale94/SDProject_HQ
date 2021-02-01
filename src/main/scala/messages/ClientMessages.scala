package messages

import AuthenticationCertification.{Certificate, GrantTS}
import Utils.WriteOperationType.WriteOperationType

//TODO codice del messaggio in message
case class RequireWriteMessage[T](objectID: String, op: T => T, nOp: Int)

case class RequireReadMessage(objectID: String, nOp: Int)

case class Write1Message[T](clientID: Int, objectID: String, numberOperation: Int,  op: T => T)

case class Write2Message(writeC: Certificate[GrantTS])

case class ReadMessage(clientID: Int, objectID: String, nonce: Double = Math.random())

case class ResolveMessage[T](conflictC: Certificate[Write1OKMessage], write1Message: Write1Message[T])

case class WriteBackWriteMessage[T](writeC: Certificate[GrantTS], write1Message: Write1Message[T])

case class WriteBackReadMessage(writeC: Certificate[GrantTS], clietID: Int, objectID: String, op: WriteOperationType, nonce: Double = Math.random())


