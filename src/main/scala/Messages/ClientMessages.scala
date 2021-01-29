package Messages

import AuthenticationCertification.{Certificate, GrantTS}
import Utils.WriteOperationType.WriteOperationType

case class RequireWriteMessage(objectID: String, writeOperationType: WriteOperationType)

case class RequireReadMessage(objectID: String)

case class Write1Message(clientID: String, objectID: String, numberOperation: Int, writeOperationType: WriteOperationType)

case class Write2Message(writeC: Certificate[GrantTS])

case class ReadMessage(clientID: String, objectID: String, nonce: Double= Math.random())

case class ResolveMessage(conflictC: Certificate[Write1OKMessage], write1Message: Write1Message)

case class WriteBackWriteMessage(writeC: Certificate[GrantTS], write1Message: Write1Message)

case class WriteBackReadMessage(writeC: Certificate[GrantTS], clietID: String, objectID: String, writeOperationType: WriteOperationType, nonce: Double= Math.random())


