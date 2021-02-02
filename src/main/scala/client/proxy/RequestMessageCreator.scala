package Client.proxy

import messages.{RequireReadMessage, RequireWriteMessage}

object RequestMessageCreator {

  private var opCounter = 0

  def createRequireWriteMessage[T](op: T => T, oid: Int): RequireWriteMessage[T] = RequireWriteMessage(oid, op, next)

  def createRequireReadMessage[T](oid: Int): RequireReadMessage = RequireReadMessage(oid, next)

  private def next: Int = {
    opCounter += 1
    opCounter
  }

}
