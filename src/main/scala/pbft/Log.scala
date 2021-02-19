package pbft

import akka.actor.ActorRef
import messages.{CommitMessage, PrePrepareMessage, PrepareMessage, StartContentionResolutionMessage, ViewStamp}

case class SetMessages(prePrepareMsg: PrePrepareMessage, prepareMsgList: List[PrepareMessage], commitMsgList: List[CommitMessage]){

  def add(msg: PrepareMessage): SetMessages =
    SetMessages(prePrepareMsg, if(prepareMsgList.contains(msg)) prepareMsgList else prepareMsgList :+ msg , commitMsgList)

  def add(msg: CommitMessage): SetMessages =
    SetMessages(prePrepareMsg, prepareMsgList, if(commitMsgList.contains(msg)) commitMsgList else commitMsgList :+ msg)
}
object SetMessages{
  def apply(prePrepareMsg: PrePrepareMessage): SetMessages = {
    SetMessages(prePrepareMsg, List(), List())
  }
}

case class Log(receivedMsgs: Map[Int, SetMessages],
               sequenceNumber: Int,
               viewNumber:Int,
               startMsgList: List[StartContentionResolutionMessage],
               clientRef: Option[ActorRef]){

  def add(msg: PrePrepareMessage): Log =
    Log(receivedMsgs + (msg.sequenceNumber -> SetMessages(msg)), sequenceNumber, viewNumber, List(), clientRef)

  def add(msg: PrepareMessage): Log = {
    Log(receivedMsgs + (msg.sequenceNumber-> receivedMsgs(msg.sequenceNumber).add(msg)), sequenceNumber, viewNumber, startMsgList, clientRef)
  }
  def add(msg: CommitMessage): Log =
    Log(receivedMsgs + (msg.sequenceNumber -> receivedMsgs(msg.sequenceNumber).add(msg)), sequenceNumber, viewNumber, startMsgList, clientRef)

  def add(msg: StartContentionResolutionMessage): Log =
    if(!startMsgList.contains(msg))  Log(receivedMsgs , sequenceNumber, viewNumber, startMsgList :+ msg, clientRef)  else  this

  def sizeStartList : Int =
    startMsgList.size

  def sizePrepareList: Int =
    receivedMsgs(sequenceNumber).prepareMsgList.size

  def sizeCommitList: Int =
    receivedMsgs(sequenceNumber).commitMsgList.size

  def checkSequenceNumber(seqNumBerReceived: Int): Boolean = seqNumBerReceived == sequenceNumber

  def next():Log = {
    Log(receivedMsgs, sequenceNumber + 1, viewNumber + 1, List(), clientRef)
  }
  def setClientRef(clientRef: ActorRef): Log =
    Log(receivedMsgs,sequenceNumber,viewNumber,startMsgList, Option(clientRef))

  def getStartQ: List[StartContentionResolutionMessage] = {
    receivedMsgs(sequenceNumber).prePrepareMsg.clientRequest
  }
  def getViewStamp: ViewStamp =
    ViewStamp(viewNumber, sequenceNumber)
}

object Log{
  def apply(): Log =
    Log(Map(), 0, 0, List(), Option.empty)
}
