package dummy

import akka.actor.Actor
import messages.{RequireReadMessage, RequireWriteMessage}

object Memory {
  private var map: Map[Int,Int] = Map()

  def get(oId: Int): Int = map(oId)

  def add(oId: Int, data: Int): Unit = map = map + (oId -> data)

  def exist(oId: Int): Boolean = map.contains(oId)
}

case class Answer(oId: Int , data: Int)

case class DummyReplica() extends Actor{

  override def receive: Receive = {
    case msg: RequireReadMessage => read(msg.objectID)
    case msg: RequireWriteMessage[Int] => write(msg.objectID, msg.op)
  }

  private def sleep(time: Long) { Thread.sleep(time) }

  private def read(oId: Int): Unit = {
    sleep(4000)
    sender(oId)
  }

  private def write(oId: Int, op: Int => Int): Unit = {
    sleep(4000)
    if(Memory.exist(oId))
      Memory.add(oId, op(Memory.get(oId)))
    else
      Memory.add(oId,op(0))
    sender(oId)
  }

  private def sender(oId: Int): Unit = context.sender()! Answer(oId, Memory.get(oId))
}
