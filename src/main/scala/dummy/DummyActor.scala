package dummy

import akka.actor.Actor
import akka.dispatch.Futures
import client.proxy.exception.WrongOpIndexException
import messages.{RequireReadMessage, RequireWriteMessage}

case class DummyActor() extends Actor{
  override def receive: Receive = initState(0,0)

  private def initState(nOp: Int, lastData: Int ): Receive = {
    case msg: RequireReadMessage => exec(x => x, lastData, nOp, msg.nOp)
    case msg: RequireWriteMessage[Int] => exec(msg.op, lastData, nOp, msg.nOp)
    case _ => context.sender() ! new WrongOpIndexException //TODO aggiungere eccezione
  }

  private def sleep(time: Long) { Thread.sleep(time) }

  private def exec(op: Int => Int, n: Int, nOp: Int, opN: Int): Unit = {
    if(opN == nOp) {
      sleep(5000)
      context.become(initState(nOp + 1, op(n)))
      context.sender() ! op(n)
    }
    else
      if(opN == nOp - 1)
        context.sender() ! n
      else
        context.sender() ! new WrongOpIndexException
  }
}
