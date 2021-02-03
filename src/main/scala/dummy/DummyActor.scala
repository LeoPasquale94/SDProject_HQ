package dummy

import akka.actor.Actor
import client.proxy.exception.WrongOpIndexException
import messages.{RequireReadMessage, RequireWriteMessage}


case class DummyActor() extends Actor{

  case class ProxyClientStatus(nOp: Int = 0, lastResult: Int = 0, complete: Boolean = false)

  override def receive: Receive = waitRequest(ProxyClientStatus())

  private def waitRequest(status: ProxyClientStatus, client: Option[Actor] = Option.empty): Receive = {
    case msg: RequireReadMessage => exec(x => x, status, msg.nOp)
    case msg: RequireWriteMessage[Int] => exec(msg.op, lastData, nOp, msg.nOp)
    case msg: Response[Int] => response(msg.getValue())
    case _ => context.sender() ! new WrongOpIndexException //TODO aggiungere eccezione
  }

  private def sleep(time: Long) { Thread.sleep(time) }

  private def response(nOp: Int, value: Int): Unit = {
    context
  }

  private def exec(op: Int => Int, n: Int, nOp: Int, opN: Int): Unit = {
    if(opN == nOp) {
      //sleep(5000)
      context.become(initState(nOp + 1, op(n), context.sender()))
      context.sender() ! op(n)

    }
    else
      if(opN == nOp - 1)
        context.sender() ! n
      else
        context.sender() ! new WrongOpIndexException
  }
}
