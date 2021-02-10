package messages

import AuthenticationCertification.{Certificate, GrantTS}

//TODO codice del messaggio in message
trait RequireMessage{
  def nOp: Int
}

case class RequireWriteMessage(objectID: Int, op: Float => Float, nOp: Int) extends RequireMessage

case class RequireReadMessage(objectID: Int, nOp: Int) extends RequireMessage

case class Write1Message(clientID: Int, objectID: Int, numberOperation: Int, op:Float => Float){
  def ==(other: Write1Message): Boolean = other.clientID == clientID && other.objectID == objectID
}

case class Write2Message(writeC: Certificate[GrantTS])

case class ReadMessage(clientID: Int, objectID: Int, nonce: Double = Math.random())

case class ResolveMessage(conflictC: Certificate[Write1OKMessage], write1Message: Write1Message)

case class WriteBackWriteMessage(writeC: Certificate[GrantTS], write1Message: Write1Message)

case class WriteBackReadMessage(writeC: Certificate[GrantTS], clietID: Int,
                                objectID: Int, nonce: Double = Math.random())


