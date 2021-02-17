package pbft

import akka.actor.Actor
import messages.StartContentionResolutionMessage

trait ServicePBFT{
  def conflictResolution(startContentionResolutionMessage: StartContentionResolutionMessage): Unit

  def viewChange()
}

case class PBFT(idPbft: Int) extends Actor{

  override def receive: Receive = ???

  def primaryState: Receive = ???

  def prePrepareState: Receive = ???

  def prepareState: Receive = ???

  def commitState: Receive = ???
}
