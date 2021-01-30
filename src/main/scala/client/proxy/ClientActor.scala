package client.proxy

import AuthenticationCertification.{Certificate, Crypto, GrantTS}
import akka.actor.{Actor, ActorRef}
import messages._

case class ClientActor[T](clientID: Int, serverReferences: Map[Int, ActorRef]) extends Actor {

  private val N_REPLICAS = serverReferences.size
  private val QUORUM = 2 / 3 * (N_REPLICAS - 1)

  override def receive : Receive = {
    case event: RequireWriteMessage[T] => requireWrite(event[T])
    case event: RequireReadMessage => requireRead(event)
  }

  def write1State(w1: Write1Message[T],
                  oKMessages: List[List[Write1OKMessage]],
                  latestWriteC: Option[Certificate[GrantTS]],
                  refusedMessages: List[Write1RefusedMessage],
                  recevedMessages: List[Int]): Receive = {
    case event: SignedMessage[Write1OKMessage] =>
      if (checkMex(event, recevedMessages))
        computeWrite1OkMessage(event.msg, w1[T], oKMessages, latestWriteC, refusedMessages, recevedMessages :+ event.signerID)
    case event: SignedMessage[Write1RefusedMessage] =>
      if (checkMex(event, recevedMessages))
        computeWrite1RefusedMessage(event.msg, w1[T], oKMessages, latestWriteC, refusedMessages, recevedMessages :+ event.signerID)
    case event: SignedMessage[Write2AnsMessage] =>
      if (checkMex(event, recevedMessages))
        otherClientIsDoingThisWrite(event.msg)
  }

  def write1OkQuorumState(w1: Write1Message[T], writeC: Certificate[GrantTS], latestWriteC: Certificate[GrantTS], recevedMessages: List[Int]): Receive = {
    case event: SignedMessage[Write1OKMessage] =>
      if (checkMex(event, recevedMessages))
        computeWrite1OkMessageQuorumState(event.msg, w1[T], writeC, latestWriteC, recevedMessages :+ event.signerID)
  }

  def write2State(recevedMessages: List[Int]): Receive = {
    case event: SignedMessage[Write2AnsMessage] =>
      if (checkMex(event, recevedMessages))
        computeWrite2State(event.msg, recevedMessages :+ event.signerID)
  }

  def readState(latestWriteC: Option[Certificate[GrantTS]], recevedMessages: List[Int]): Receive = {
    case event: SignedMessage[ReadAnsMessage] =>
      if (checkMex(event, recevedMessages))
        computeReadAnsMessage(event.msg, latestWriteC, recevedMessages)
  }

  private def requireWrite(mex: RequireWriteMessage[T]): Unit = {
    val w1 = Write1Message(clientID, mex.objectID, getOperationNumber(mex.objectID), mex.op)
    sendSignMexToAll(w1)
    context.become(write1State(w1, List(), Option.empty, List(), List()))
  }

  private def requireRead(mex: RequireReadMessage): Unit = {
    sendSignMexToAll(ReadMessage(clientID, mex.objectID))
    context.become(readState(Option.empty, List()))
  }

  private def computeWrite1OkMessage(mex: Write1OKMessage,
                                     w1: Write1Message[T],
                                     oKMessages: List[List[Write1OKMessage]],
                                     latestWriteC: Option[Certificate[GrantTS]],
                                     refusedMessages: List[Write1RefusedMessage],
                                     recevedMessages: List[Int]): Unit = {

    @scala.annotation.tailrec
    def addNewMex(oldList: List[List[Write1OKMessage]], newList: List[List[Write1OKMessage]]): List[List[Write1OKMessage]] = oldList match {
      case Nil => newList :+ List(mex)
      case (h :: t1) :: t2 if h == mex => newList ++ (((h :: t1) :+ mex) :: t2)
      case h :: t => addNewMex(t, newList :+ h)
    }

    @scala.annotation.tailrec
    def areThereMoreThenQuorumEqualOKMex(l: List[List[Write1OKMessage]]): Option[List[Write1OKMessage]] = l match {
      case Nil => Option.empty
      case h :: _ if h.size > QUORUM => Option.apply(h)
      case _ :: t => areThereMoreThenQuorumEqualOKMex(t)
    }

    val newLatestWriteC = setLatestCertificate(mex.currentC, latestWriteC)
    val newOkMessages = addNewMex(oKMessages, List())
    val eventualQuorum = areThereMoreThenQuorumEqualOKMex(newOkMessages)

    if (eventualQuorum.nonEmpty) {
      val writeC = Certificate(eventualQuorum.get.map(_.grantTS))
      context.become(write1OkQuorumState(w1, writeC, mex.currentC, recevedMessages))
      if (newOkMessages.size > 1) {
        val replicasNotUpdate = newOkMessages
          .flatten
          .diff(eventualQuorum.get)
          .filter(_.grantTS.areTSOrVSNotEqual(mex.grantTS))
          .map(_.grantTS.replicaID)
        sendSignMexToAny(replicasNotUpdate, WriteBackWriteMessage(mex.currentC, w1)) //Controllare per sicurezza
      }
    } else if (newLatestWriteC.nonEmpty && (newLatestWriteC.get.items.head > mex.currentC.items.head)) {
      sendToOne(mex.grantTS.replicaID, WriteBackWriteMessage(newLatestWriteC.get, w1))

    } else if (newOkMessages.size > QUORUM) {
      context.become(write1State(w1, List(), Option.empty, List(), List()))
      val conflictC = Certificate(newOkMessages.flatten)
      sendSignMexToAll(ResolveMessage(conflictC, w1))
    } else {
      context.become(write1State(w1, newOkMessages, newLatestWriteC, refusedMessages, recevedMessages))
    }
  }

  private def computeWrite1RefusedMessage(mex: Write1RefusedMessage,
                                          w1: Write1Message[T],
                                          oKMessages: List[List[Write1OKMessage]],
                                          latestWriteC: Option[Certificate[GrantTS]],
                                          refusedMessages: List[Write1RefusedMessage],
                                          recevedMessages: List[Int]): Unit = {
    val newRefusedMessages = mex :: refusedMessages
    if (newRefusedMessages.size > QUORUM) {
      context.become(write1State(w1, List(), Option.empty, List(), List()))
      sendSignMexToAll(WriteBackWriteMessage(mex.currentC, w1))
    } else {
      context.become(write1State(w1, oKMessages, latestWriteC, newRefusedMessages, recevedMessages))
    }
  }

  private def otherClientIsDoingThisWrite(mex: Write2AnsMessage): Unit = {
    context.become(write2State(List()))
    sendSignMexToAll(Write2Message(mex.currentC))
  }

  private def computeWrite1OkMessageQuorumState(mex: Write1OKMessage,
                                                w1: Write1Message[T],
                                                writeC: Certificate[GrantTS],
                                                oldWriteC: Certificate[GrantTS],
                                                recevedMessages: List[Int]): Unit = {
    var newWriteC = writeC
    if (writeC.items.head == mex.grantTS) {
      newWriteC = writeC + mex.grantTS
      context.become(write1OkQuorumState(w1, writeC + mex.grantTS, oldWriteC, recevedMessages))
    } else {
      sendSignMexToOne(mex.grantTS.replicaID, WriteBackWriteMessage(oldWriteC, w1))
    }

    if (recevedMessages.size == N_REPLICAS) {
      context.become(write2State(List()))
      sendSignMexToAll(Write2Message(newWriteC))
    }
  }

  private def computeWrite2State(mex: Write2AnsMessage, recevedMessages: List[Int]): Unit = {
    if (recevedMessages.size > QUORUM) {
      context.become(receive)
      returnClient(mex.result)
    } else {
      context.become(write2State(recevedMessages))
    }
  }

  private def computeReadAnsMessage(mex: ReadAnsMessage,
                                    latestWriteC: Option[Certificate[GrantTS]],
                                    recevedMessages: List[Int]): Unit = {

    val newLatestWriteC = setLatestCertificate(mex.currentC, latestWriteC)
    if (newLatestWriteC.nonEmpty && (newLatestWriteC.get.items.head > mex.currentC.items.head)) {
      sendToOne(mex.replicaID, WriteBackReadMessage(newLatestWriteC.get, clientID, newLatestWriteC.get.items.head.objectID, ???))
    }
    if (recevedMessages.size > QUORUM) {
      context.become(receive)
      returnClient(mex.result)
    } else {
      context.become(readState(latestWriteC, recevedMessages))
    }

  }

  private def sendSignMexToOne[T](serverID: Int, message: T): Unit = {
    sendToOne(serverID, Crypto.toSign(clientID, message))
  }

  private def sendSignMexToAny[T](serverIDSet: List[Int], mex: T): Unit = {
    serverIDSet.foreach(sendSignMexToOne(_, mex))
  }

  private def sendSignMexToAll[T](message: T): Unit = {
    sendToAll(Crypto.toSign(clientID, message))
  }

  private def sendToOne[T](serverID: Int, message: T): Unit = {
    if (serverReferences.contains(serverID))
      serverReferences(serverID) ! message
  }

  private def sendToAll[T](message: T): Unit = serverReferences.foreach(t => t._2 ! message)

  private def getOperationNumber(objectID: String): Int = ???

  private def checkMex[T](message: SignedMessage[T], recevedMessages: List[Int]): Boolean = {
    Crypto.checkMex(message) && !recevedMessages.contains(message.signerID)
  }

  private def returnClient[T](result: T): Unit = ???

  private def setLatestCertificate(currentC: Certificate[GrantTS], latestWriteC: Option[Certificate[GrantTS]]): Option[Certificate[GrantTS]] = latestWriteC match {
    case o if o.isEmpty => Option.apply(currentC)
    case o if o.get.items.head > currentC.items.head => o
    case _ => Option.apply(currentC)
  }

}
