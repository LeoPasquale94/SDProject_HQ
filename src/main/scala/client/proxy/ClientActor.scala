package client.proxy

import AuthenticationCertification.{Certificate, Crypto}
import akka.actor.{Actor, ActorRef}
import client.proxy.exception.WrongOpIndexException
import messages._

case class ClientActor(clientID: Int, serverReferences: Map[Int, ActorRef]) extends Actor {

  private val N_REPLICAS = serverReferences.size
  private val QUORUM = 2 / 3 * (N_REPLICAS - 1)


  override def receive : Receive = initState(0, Option.empty)

  def initState(opHash: Int, lastData: Option[Any]):Receive = {
    case event: RequireMessage => require(event, opHash, lastData)
  }

  def write1State[T](stateVariables: Write1StateVariable[T]): Receive = {
    case event: SignedMessage[Write1OKMessage] =>
      if (checkMex(event, stateVariables.recevedMessages))
        computeWrite1OkMessage(event.msg, stateVariables)
    case event: SignedMessage[Write1RefusedMessage] =>
    if (checkMex(event, stateVariables.recevedMessages))
        computeWrite1RefusedMessage(event.msg, stateVariables[T])
    case event: SignedMessage[Write2AnsMessage] =>
      if (checkMex(event, stateVariables.recevedMessages))
        otherClientIsDoingThisWrite(event.msg, stateVariables[T])
  }

  def write1OkQuorumState[T](stateVariables: Write1OkQuorumStateVariable[T]): Receive = {
    case event: SignedMessage[Write1OKMessage] =>
      if (checkMex(event, stateVariables.recevedMessages))
       computeWrite1OkMessageQuorumState(event.msg, stateVariables[T] )
  }

  def write2State(stateVariables: Write2StateVariable): Receive = {
    case event: SignedMessage[Write2AnsMessage] =>
      if (checkMex(event, stateVariables.recevedMessages))
        computeWrite2State(event.msg, stateVariables.addRecevedMessages(event.msg.replicaID))
  }

  def readState(stateVariables:ReadStateVariable): Receive = {
    case event: SignedMessage[ReadAnsMessage] =>
      if (checkMex(event, stateVariables.recevedMessages))
        computeReadAnsMessage(event.msg, stateVariables)
  }

  private def require[T](msg: RequireMessage, opHash: Int, lastData: Option[Any]): Unit =  {
    if(msg.nOp == opHash) {
      msg match {
        case msg: RequireWriteMessage[T] => requireWrite(msg, opHash + 1)
        case msg: RequireReadMessage => requireRead(msg, opHash + 1)
      }
    }
    else
      if(msg.nOp == opHash - 1)
        if (lastData.nonEmpty)
          context.sender() !lastData.get
      else
        context.sender() ! new WrongOpIndexException
  }

  private def requireWrite[T](msg: RequireWriteMessage[T], opHash: Int): Unit = {
    val w1 = Write1Message(clientID, msg.objectID, opHash, msg.op)
    sendSignMexToAll(w1)
    context.become(write1State(Write1StateVariable(w1, List(),Option.empty, List(), List(), opHash, context.sender())))
  }

  private def requireRead(msg: RequireReadMessage, opHash: Int): Unit = {
    sendSignMexToAll(ReadMessage(clientID, msg.objectID))
    context.become(readState(ReadStateVariable(Option.empty, List(), opHash, context.sender())))
  }

  private def computeWrite1OkMessage[T](msg: Write1OKMessage, stateVariables: Write1StateVariable[T]): Unit = {
    val updateStateVariable = stateVariables.update(msg)
    val eventualQuorum = updateStateVariable.areThereMoreThenQuorumEqualOKMsg(QUORUM)

    if(eventualQuorum.nonEmpty){
      val writeC = Certificate(eventualQuorum.get.map(_.grantTS))
      context.become(write1OkQuorumState(updateStateVariable.createWrite1OkQuorumStateVariable(writeC)))
      val replicasNotUpdate = updateStateVariable.getReplicasIDNotUpdate(eventualQuorum.get)
      sendSignMexToAny(replicasNotUpdate, WriteBackWriteMessage(msg.currentC, updateStateVariable.w1))
    }else if(updateStateVariable.isNotReplicaUpdatedOnWriteOperation(msg.currentC))
      sendToOne(msg.grantTS.replicaID, WriteBackWriteMessage(msg.currentC, updateStateVariable.w1))
    else if (updateStateVariable.getSizeDifferentWrite1OkMsg > QUORUM){
      context.become(write1State(updateStateVariable.resetState))
    }else
      context.become(write1State(updateStateVariable))
  }

  private def computeWrite1RefusedMessage[T](msg: Write1RefusedMessage, stateVariables: Write1StateVariable[T]): Unit = {
    val updateStatVariable = stateVariables.update(msg)
    if (updateStatVariable.getSizeRefusedMsg > QUORUM) {
      context.become(write1State(updateStatVariable.resetState))
      sendSignMexToAll(WriteBackWriteMessage(msg.currentC,stateVariables.w1))
    } else {
      context.become(write1State(updateStatVariable))
    }
  }

  private def otherClientIsDoingThisWrite[T](msg: Write2AnsMessage, stateVariables: Write1StateVariable[T]): Unit = {
    context.become(write2State(stateVariables.createWrite2StateVariable(msg.currentC)))
    sendSignMexToAll(Write2Message(msg.currentC))
  }

  private def computeWrite1OkMessageQuorumState[T](msg: Write1OKMessage, stateVariables: Write1OkQuorumStateVariable[T]): Unit = {
    var updateStatVariable = stateVariables
    if (stateVariables.isGrantTSSameAsOther(msg.grantTS)) {
      updateStatVariable = stateVariables.addGrantTS(msg.grantTS)
      context.become(write1OkQuorumState(updateStatVariable))
    } else {
      sendSignMexToOne(msg.grantTS.replicaID, WriteBackWriteMessage(stateVariables.latestWriteC, stateVariables.w1))
    }
    if (stateVariables.getSizeRecevedMessages == N_REPLICAS) {
      context.become(write2State(updateStatVariable.createWrite2StateVariable))
      sendSignMexToAll(Write2Message(updateStatVariable.writeC))
    }
  }

  private def computeWrite2State(msg: Write2AnsMessage, stateVariables: Write2StateVariable): Unit = {
    if (stateVariables.getSizeRecevedMessages > QUORUM) {
      context.become(initState(stateVariables.opHash, Option.apply(msg.result)))
      stateVariables ! msg.result
    } else {
      context.become(write2State(stateVariables))
    }
  }

  private def computeReadAnsMessage(msg: ReadAnsMessage, stateVariables: ReadStateVariable): Unit = {

    val updateStateVariable = stateVariables.update(msg)
    if (updateStateVariable.isNotReplicaUpdatedOnWriteOperation(msg.currentC)) {
      val latestWriteC = updateStateVariable.latestWriteC.get
      sendToOne(msg.replicaID, WriteBackReadMessage(latestWriteC, clientID, latestWriteC.items.head.objectID))
    }
    if (stateVariables.getSizeRecevedMessages > QUORUM) {
      context.become(initState(stateVariables.opHash, Option.apply(msg.result)))
      stateVariables ! msg.result
    } else {
      context.become(readState(stateVariables))
    }

  }

  private def sendSignMexToOne[H](serverID: Int, message: H): Unit = {
    sendToOne(serverID, Crypto.toSign(clientID, message))
  }

  private def sendSignMexToAny[H](serverIDSet: List[Int], mex: H): Unit = {
    serverIDSet.foreach(sendSignMexToOne(_, mex))
  }

  private def sendSignMexToAll[H](message: H): Unit = {
    sendToAll(Crypto.toSign(clientID, message))
  }

  private def sendToOne[H](serverID: Int, message: H): Unit = {
    if (serverReferences.contains(serverID))
      serverReferences(serverID) ! message
  }

  private def sendToAll[H](message: H): Unit = serverReferences.foreach(t => t._2 ! message)

  private def checkMex[H](message: SignedMessage[H], recevedMessages: List[Int]): Boolean = {
    Crypto.checkMex(message) && !recevedMessages.contains(message.signerID)
  }


}
