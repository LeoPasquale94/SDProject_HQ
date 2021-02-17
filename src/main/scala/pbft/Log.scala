package pbft

import messages.{CommitMessage, PrePrepareMessage, PrepareMessage}

case class SetMessages(prePrepareMsg: PrePrepareMessage, prepareMsgList: List[PrepareMessage], commitMsgList: List[CommitMessage]){

  def add(msg: PrepareMessage): SetMessages =
    SetMessages(prePrepareMsg, prepareMsgList :+ msg, commitMsgList)

  def add(msg: CommitMessage): SetMessages =
    SetMessages(prePrepareMsg, prepareMsgList, commitMsgList :+ msg)
}

object SetMessages{
  def apply(prePrepareMsg: PrePrepareMessage): SetMessages = {
    SetMessages(prePrepareMsg, List(), List())
  }

}


case class Log(receivedMsgs: Map[Int, SetMessages], opNumb: Int){

  def add[T](msg: T): Log = {
    val setMessages = msg match {
      case msg: PrePrepareMessage => SetMessages(msg)
      case msg: PrepareMessage => receivedMsgs(opNumb).add(msg)
      case msg: CommitMessage => receivedMsgs(opNumb).add(msg)
    }
    Log(receivedMsgs + (opNumb -> setMessages), opNumb)
  }

}

object Log{
  def apply(): Log =
    Log(Map(), 0)
}
