package Client

import AuthenticationCertification.{Certificate, Crypto, GrantTS}
import Messages.{ReadAnsMessage, ReadMessage, RequireReadMessage, RequireWriteMessage, ResolveMessage, SignedMessage, Write1Message, Write1OKMessage, Write1RefusedMessage, Write2AnsMessage, Write2Message, WriteBackReadMessage, WriteBackWriteMessage}
import akka.actor.{Actor, ActorRef}

case class ClentActor(clientID: String, serverReferences: Map[String, ActorRef]) extends Actor {

  private val N_REPLICAS = serverReferences.size
  private val QUORUM = 2/3 * (N_REPLICAS - 1)

  override def receive: Receive = {
    case event: RequireWriteMessage => requireWrite(event)
    case event: RequireReadMessage => requireRead(event)
  }

  def write1State(w1: Write1Message,
                  oKMessages: List[List[Write1OKMessage]],
                  latestWriteC: Option[Certificate[GrantTS]],
                  refusedMessages: List[Write1RefusedMessage],
                  recevedMessages: List[String]): Receive = {
    case event: SignedMessage [Write1OKMessage] =>
      if(checkMex(event, recevedMessages))
        computeWrite1OkMessage(event.mex, w1, oKMessages, latestWriteC, refusedMessages, recevedMessages :+ event.signerID)
    case event: SignedMessage [Write1RefusedMessage] =>
      if(checkMex(event, recevedMessages))
        computeWrite1RefusedMessage(event.mex, w1, oKMessages, latestWriteC, refusedMessages, recevedMessages :+ event.signerID )
    case event: SignedMessage [Write2AnsMessage] =>
      if(checkMex(event, recevedMessages))
        otherClientIsDoingThisWrite(event.mex)
  }

  def write1OkQuorum(w1: Write1Message, writeC: Certificate[GrantTS],latestWriteC: Certificate[GrantTS], recevedMessages: List[String]): Receive = {
    case event: SignedMessage [Write1OKMessage] =>
      if(checkMex(event, recevedMessages))
        computeWrite1OkMessageQuorumState(event.mex, w1, writeC, latestWriteC, recevedMessages :+ event.signerID)

  }

  def write2State (recevedMessages: List[String]): Receive = {
    case event: SignedMessage [Write2AnsMessage] =>
      if(checkMex(event, recevedMessages))
        computeWrite2State(event.mex, recevedMessages :+ event.signerID)
  }

  def readState( latestWriteC: Option[Certificate[GrantTS]], recevedMessages: List[String]): Receive = {
    case event: SignedMessage[ReadAnsMessage] =>
      if(checkMex(event, recevedMessages))
        computeReadAnsMessage(event.mex,latestWriteC, recevedMessages)
  }

  def conflictState: Receive = ???

  private def requireWrite(mex: RequireWriteMessage): Unit = {
    val w1 = Write1Message(clientID, mex.objectID, getOperationNumber(mex.objectID), mex.writeOperationType)
    sendSignMexToAll(w1)
    context.become(write1State(w1, List(), Option.empty, List(), List()))
  }

  private def requireRead(mex: RequireReadMessage): Unit = {
    sendSignMexToAll(ReadMessage(clientID, mex.objectID))
    context.become(readState(Option.empty, List()))
  }

  private def computeWrite1OkMessage(mex: Write1OKMessage,
                                     w1: Write1Message,
                                     oKMessages: List[List[Write1OKMessage]],
                                     latestWriteC: Option[Certificate[GrantTS]],
                                     refusedMessages: List[Write1RefusedMessage],
                                     recevedMessages: List[String]): Unit = {

    @scala.annotation.tailrec
    def addNewMex(oldList: List[List[Write1OKMessage]], newList: List[List[Write1OKMessage]] ): List[List[Write1OKMessage]] = oldList match {
      case Nil => newList :+ List(mex)
      case (h::t1)::t2 if h == mex => newList ++ (((h::t1) :+ mex):: t2)
      case h::t => addNewMex(t, newList :+ h)
    }

    @scala.annotation.tailrec
    def areThereMoreThenQuorumEqualElement(l: List[List[Write1OKMessage]]): Option[List[Write1OKMessage]] = l match {
      case Nil =>  Option.empty
      case h::_ if h.size > QUORUM => Option.apply(h)
      case _::t => areThereMoreThenQuorumEqualElement(t)
    }

    val newLatestWriteC = setLatestCertificate(mex.currentC, latestWriteC)
    val newOkMessages = addNewMex(oKMessages,List())
    val eventualQuorum = areThereMoreThenQuorumEqualElement(newOkMessages)

    if (eventualQuorum.nonEmpty){
      val writeC = Certificate(eventualQuorum.get.map(_.grantTS))
      context.become(write1OkQuorum(w1, writeC, mex.currentC,recevedMessages))
      if(newOkMessages.size > 1){
        val replicasNotUpdate = newOkMessages
          .flatten
          .diff(eventualQuorum.get)
          .filter(_.grantTS.areTSOrVSNotEqual(mex.grantTS))
          .map(_.grantTS.replicaID)
        sendSignMexToAny(replicasNotUpdate, WriteBackWriteMessage(mex.currentC, w1)) //Controllare per sicurezza
      }
    }else if (newLatestWriteC.nonEmpty && (newLatestWriteC.get.items.head > mex.currentC.items.head)){
      sendToOne(mex.grantTS.replicaID, WriteBackWriteMessage(newLatestWriteC.get, w1))

    }else if(newOkMessages.size > QUORUM ){
      context.become(conflictState)
      val conflictC = Certificate(newOkMessages.flatten)
      sendSignMexToAll(ResolveMessage(conflictC,w1))
    }else {
      context.become(write1State(w1, newOkMessages, newLatestWriteC, refusedMessages, recevedMessages))
    }
  }

  private def computeWrite1RefusedMessage(mex: Write1RefusedMessage,
                                          w1: Write1Message,
                                          oKMessages: List[List[Write1OKMessage]],
                                          latestWriteC: Option[Certificate[GrantTS]],
                                          refusedMessages: List[Write1RefusedMessage],
                                          recevedMessages: List[String]): Unit = {
    val newRefusedMessages = mex :: refusedMessages
    if (newRefusedMessages.size > QUORUM){
      context.become(write1State(w1, List(), Option.empty ,List(), List()))
      sendSignMexToAll(WriteBackWriteMessage(mex.currentC, w1))
    }else {
      context.become(write1State(w1, oKMessages, latestWriteC, newRefusedMessages, recevedMessages))
    }
  }

  private def otherClientIsDoingThisWrite(mex: Write2AnsMessage): Unit = {
    context.become(write2State( List()))
    sendSignMexToAll(Write2Message(mex.currentC))
  }

  private def computeWrite1OkMessageQuorumState(mex: Write1OKMessage,
                                                w1: Write1Message,
                                                writeC: Certificate[GrantTS],
                                                oldWriteC: Certificate[GrantTS],
                                                recevedMessages: List[String]): Unit = {
    var newWriteC = writeC
    if(writeC.items.head == mex.grantTS){
      newWriteC = writeC + mex.grantTS
      context.become(write1OkQuorum(w1, writeC + mex.grantTS,oldWriteC, recevedMessages))}
    else{
      sendSignMexToOne(mex.grantTS.replicaID, WriteBackWriteMessage(oldWriteC, w1))}

    if(recevedMessages.size == N_REPLICAS){
      context.become(write2State(List()))
      sendSignMexToAll(Write2Message(newWriteC))}
  }

  private def computeWrite2State(mex: Write2AnsMessage, recevedMessages: List[String]): Unit = {
    if(recevedMessages.size > QUORUM){
      context.become(receive)
      returnClient(mex.result)}
    else{
      context.become(write2State(recevedMessages))}
  }

  private def computeReadAnsMessage(mex: ReadAnsMessage,
                                    latestWriteC: Option[Certificate[GrantTS]],
                                    recevedMessages: List[String]): Unit = {

    val newLatestWriteC = setLatestCertificate(mex.currentC, latestWriteC)
    if(newLatestWriteC.nonEmpty && (newLatestWriteC.get.items.head > mex.currentC.items.head)){
      sendToOne(mex.replicaID, WriteBackReadMessage(newLatestWriteC.get, clientID, newLatestWriteC.get.items.head.objectID,???))
    }
    if(recevedMessages.size > QUORUM){
      context.become(receive)
      returnClient(mex.result)
    }else {
      context.become(readState(latestWriteC, recevedMessages))
    }

  }

  private def sendSignMexToOne[T] (serverID: String, message: T): Unit = {
    sendToOne(serverID, Crypto.toSign(clientID, message))
  }

  private def sendSignMexToAny[T] (serverIDSet: List[String], mex: T): Unit = {
    serverIDSet.foreach(sendSignMexToOne(_,mex))
  }

  private def sendSignMexToAll[T] (message: T): Unit = {
    sendToAll(Crypto.toSign(clientID, message))
  }

  private def sendToOne[T](serverID: String, message: T): Unit = {
    if(serverReferences.contains(serverID))
      serverReferences(serverID) ! message
  }

  private def sendToAll[T](message: T): Unit = serverReferences.foreach(t => t._2 ! message)

  private def getOperationNumber(objectID: String): Int = ???

  private def checkMex[T](message:SignedMessage[T], recevedMessages: List[String] ): Boolean = {
    Crypto.checkMex(message) && !recevedMessages.contains(message.signerID)}

  private def returnClient(result: Any): Unit = ???

  private def setLatestCertificate(currentC: Certificate[GrantTS],latestWriteC:Option[Certificate[GrantTS]]): Option[Certificate[GrantTS]] = latestWriteC match {
    case Option.empty => Option.apply(currentC)
    case o if o.get.items.head > currentC.items.head => o
    case _ => Option.apply(currentC)
  }

}