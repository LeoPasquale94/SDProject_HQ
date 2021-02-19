package messages

import akka.actor.ActorRef
import pbft.Log

trait BftMsg{
  def viewNumber: Int
  def sequenceNumber: Int
}
case class ClientRequestMsg(startContentionResolutionMessage: StartContentionResolutionMessage, clientRef: ActorRef)

case class PrePrepareMessage(viewNumber: Int, sequenceNumber: Int, clientRequest: List[StartContentionResolutionMessage]) extends BftMsg

case class PrepareMessage(viewNumber: Int, sequenceNumber: Int, replicaNumber: Int) extends BftMsg

case class CommitMessage(viewNumber: Int, sequenceNumber: Int, replicaNumber: Int) extends BftMsg

case class ReplayMessage(stratQ: List[StartContentionResolutionMessage], viewStamp: ViewStamp)

case class InitialStateMsg(primary: Boolean, log: Log)
