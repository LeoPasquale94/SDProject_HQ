package pbft

import akka.actor.{Actor, ActorRef}
import messages.{BftMsg, ClientRequestMsg, CommitMessage, InitialStateMsg, PrePrepareMessage, PrepareMessage, ReplayMessage, StartContentionResolutionMessage}

trait ServicePBFT{
  def conflictResolution(startContentionResolutionMessage: StartContentionResolutionMessage): Unit

  def viewChange()
}
//ToDo in serverReferences ci sono solo i riferimenti alle altre repliche
case class PBFTActor(idPbft: Int, serverReferences: Map[Int, ActorRef]) extends Actor with ServicePBFT {

  private val N_REPLICAS = serverReferences.size + 1
  private val QUORUM = 2 / 3 * (N_REPLICAS - 1) + 1
  private val QUORUM_PREPARE_STATE = QUORUM - 1

  override def preStart(): Unit =
    self ! InitialStateMsg(idPbft % N_REPLICAS == idPbft , Log())

  override def receive: Receive = {
    case InitialStateMsg(true, log) => context.become(primaryState(log))
    case InitialStateMsg(false, log) => context.become(backupState(log))
  }

  def primaryState(log: Log): Receive = {
    case event: StartContentionResolutionMessage => computeStartContentionResolutionMsg(event, log)
    case event: ClientRequestMsg => {
      context.become(primaryState(log.setClientRef(event.clientRef)))
      self ! event.startContentionResolutionMessage}
  }

  def backupState(log: Log): Receive = {
    case event: ClientRequestMsg => {
      sendPrimary(event, log)
      context.become(prePrepareState(log.setClientRef(event.clientRef)))
    }
  }

  def prePrepareState(log: Log): Receive = {
    case event: PrePrepareMessage => computePrePrepareMsg(event, log)
  }

  def prepareState(log: Log): Receive = {
    case event: PrepareMessage => computePrepareMsg(event, log)
  }

  def commitState(log: Log): Receive = {
    case event: CommitMessage => computeCommitMsg(event, log)
  }

  private def computeStartContentionResolutionMsg(msg: StartContentionResolutionMessage, log: Log): Unit = {
    val updateLog = log.add(msg)
    if (updateLog.sizeStartList > QUORUM) {
      val prePrepareMessage = PrePrepareMessage(log.sequenceNumber, log.viewNumber, log.startMsgList)
      sendAll(prePrepareMessage)
      context.become(prepareState(updateLog.add(prePrepareMessage)))}
    else
      context.become(primaryState(updateLog))

  }

  private def computePrePrepareMsg(msg: PrePrepareMessage, log: Log): Unit = {
    if(checkMsg(msg, log)){
      context.become(prepareState(log.add(msg)))
      sendAll(PrepareMessage(log.viewNumber, log.sequenceNumber, idPbft))
    }
  }

  private def computePrepareMsg(msg: PrepareMessage, log: Log): Unit = {
    if(checkMsg(msg, log)){
      val updateLog = log.add(msg)
      if(updateLog.sizePrepareList == QUORUM_PREPARE_STATE){
        val commitMsg = CommitMessage(log.viewNumber, log.sequenceNumber, idPbft)
        sendAll(commitMsg)
        context.become(commitState(updateLog.add(commitMsg)))}
      else
        context.become(prepareState(updateLog))
    }
  }

  private def computeCommitMsg(msg: CommitMessage, log: Log): Unit = {
    if(checkMsg(msg, log)) {
      val updaLog = log.add(msg)
      if (updaLog.sizeCommitList == QUORUM) {
        val reply = ReplayMessage(updaLog.getStartQ, updaLog.getViewStamp)
      }
      else
        context.become(commitState(updaLog))
    }
  }

  private def checkMsg(msg: BftMsg, log: Log): Boolean =
    msg.sequenceNumber == log.sequenceNumber && msg.viewNumber == log.viewNumber

  private def sendAll[T](msg: T): Unit =
    serverReferences.foreach( el => el._2 ! msg)

  private def sendPrimary[T](msg: T, log: Log): Unit =
    serverReferences(log.viewNumber % N_REPLICAS) ! msg

  override def conflictResolution(startContentionResolutionMessage: StartContentionResolutionMessage): Unit =
    self ! ClientRequestMsg(startContentionResolutionMessage, sender())

  override def viewChange(): Unit = ???
}
