package dummy

import akka.actor.{Actor, ActorRef}
import client.proxy.exception.WrongOpIndexException
import messages.{RequireReadMessage, RequireWriteMessage}


case class DummyActor(replicaRef: ActorRef) extends Actor{
  override def receive: Receive = initState(0,Option.empty, context.sender())

  private def initState[T](nOp: Int, lastData: Option[T], senderRef: ActorRef ): Receive = {
    case msg: RequireReadMessage => exec( nOp, lastData, msg.nOp, msg)
    case msg: RequireWriteMessage[T] => exec(nOp, lastData, msg.nOp, msg)
    case msg: Answer[T] => answer(nOp, msg, senderRef)
    case _ => context.sender() ! new WrongOpIndexException //TODO aggiungere eccezione
  }


  private def exec[M,T](nOp: Int, n: Option[T], opN: Int, msg: M): Unit = {
    if(opN == nOp) {
      replicaRef ! msg
      println("Richiesta inviata alla replica")
      context.become(initState(nOp + 1, Option.empty, context.sender()))
    }
    else
      if(opN == nOp - 1)
        if (n.nonEmpty) {
          println("Risulato disponibile")
          context.sender() ! n.get
        }else {
          println("Risultato non disponibile")
        }
      else
        context.sender() ! new WrongOpIndexException
  }

  private def answer[T](nOp: Int, msg: Answer[T], senderRef: ActorRef ): Unit = {
    senderRef ! msg.data
    context.become(initState(nOp, Option.apply(msg.data), senderRef))
  }
}
