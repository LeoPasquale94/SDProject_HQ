package dummy

import akka.actor.Actor
import messages.{RequireReadMessage, RequireWriteMessage}

object Memory {
  private var map: Map[Int, Any] = Map(123 -> 5, 321 -> "Hola ")

  def get[T](oId: Int): T = map(oId).asInstanceOf[T]

  def add[T](oId: Int, data: T): Unit = map = map + (oId -> data)

  def exist(oId: Int): Boolean = map.contains(oId)
}

case class Answer[T](oId: Int , data: T)

case class DummyReplica() extends Actor{

  override def receive: Receive = initState

  def initState[T]: Receive = {
    case msg: RequireReadMessage => read(msg.objectID)
    case msg: RequireWriteMessage=> write(msg.objectID, msg.op)
  }

  private def sleep(time: Long) { Thread.sleep(time) }

  private def read(oId: Int): Unit = {
    sleep(4000)
    sender(oId)
  }

  private def write[T](oId: Int, op: T => T): Unit = {
    sleep(4000)
    if(Memory.exist(oId))
      Memory.add(oId, op(Memory.get(oId)))
   // else
     // Memory.add(oId,op(0))
    sender(oId)
  }

  private def sender(oId: Int): Unit = context.sender()! Answer(oId, Memory.get(oId))


}
