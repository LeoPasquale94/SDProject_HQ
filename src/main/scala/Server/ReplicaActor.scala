package Server

import akka.actor.Actor
import messages.{ReadMessage, Write1Message, Write2Message}

case class ReplicaActor() extends Actor{

  override def receive: Receive = ???

  def activeState[T]( ): Receive = {
    case event: ReadMessage => ???
    case event: Write1Message[T] => ???
    case event: Write2Message => ???
  }

  def frozenState: Receive = ???

  private def computeReadMessage:Unit = ???

  private def computeWrite1Message: Unit = ???

  private def computeWrite2Message: Unit = ???
}
