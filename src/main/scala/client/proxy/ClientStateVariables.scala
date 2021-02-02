package Client.proxy

import AuthenticationCertification.{Certificate, GrantTS}
import messages.{ReadAnsMessage, Write1Message, Write1OKMessage, Write1RefusedMessage}

case class Write1StateVariable[T](w1: Write1Message[T],
                               oKMessages: List[List[Write1OKMessage]],
                               latestWriteC: Option[Certificate[GrantTS]],
                               refusedMessages: List[Write1RefusedMessage],
                               recevedMessages: List[Int],
                               opHash: Int){
  @scala.annotation.tailrec
  private def addNewMex(msg:Write1OKMessage, oldList: List[List[Write1OKMessage]], newList: List[List[Write1OKMessage]]): List[List[Write1OKMessage]] = oldList match {
    case Nil => newList :+ List(msg)
    case (h :: t1) :: t2 if h == msg => newList ++ (((h :: t1) :+ msg) :: t2)
    case h :: t => addNewMex(msg, t, newList :+ h)
  }

  def areThereMoreThenQuorumEqualOKMsg(quorum: Int): Option[List[Write1OKMessage]] = {
    @scala.annotation.tailrec
    def _areThereMoreThenQuorumEqualOKMex(l: List[List[Write1OKMessage]]): Option[List[Write1OKMessage]] = l match {
      case Nil => Option.empty
      case h :: _ if h.size > quorum => Option.apply(h)
      case _ :: t => _areThereMoreThenQuorumEqualOKMex(t)
    }
    _areThereMoreThenQuorumEqualOKMex(oKMessages)
  }
  def addWrite1OkMessage(msg:Write1OKMessage): Write1StateVariable[T] =
    Write1StateVariable(w1, addNewMex(msg, oKMessages, List()), latestWriteC,refusedMessages, recevedMessages, opHash)

  def addRecevedMessages(replicaID: Int): Write1StateVariable[T] =
    Write1StateVariable(w1, oKMessages, latestWriteC,refusedMessages, recevedMessages :+ replicaID, opHash)

  def addRefusedMessages(msg: Write1RefusedMessage): Write1StateVariable[T] =
    Write1StateVariable(w1, oKMessages, latestWriteC, refusedMessages :+ msg, recevedMessages , opHash)

  def setLatestWriteC(writeC: Certificate[GrantTS]): Write1StateVariable[T] ={
    val newLatestWriteC = latestWriteC match {
      case o if o.isEmpty => Option.apply(writeC)
      case o if o.get.items.head > writeC.items.head => o
      case _ => Option.apply(writeC)
    }
    Write1StateVariable(w1, oKMessages, newLatestWriteC,refusedMessages, recevedMessages, opHash)
  }

  def update(msg: Write1OKMessage): Write1StateVariable[T] =
    this.addWrite1OkMessage(msg)
    .addRecevedMessages(msg.grantTS.replicaID)
    .setLatestWriteC(msg.currentC)

  def update(msg: Write1RefusedMessage): Write1StateVariable[T] =
    this.addRefusedMessages(msg)
    .addRecevedMessages(msg.grantTS.replicaID)

  def createWrite1OkQuorumStateVariable(writeC: Certificate[GrantTS]): Write1OkQuorumStateVariable[T] =
    Write1OkQuorumStateVariable(w1, writeC, latestWriteC.get, recevedMessages,opHash)

  def createWrite2StateVariable(writeC: Certificate[GrantTS]):Write2StateVariable =
    Write2StateVariable(writeC, List(), opHash)

  def notEmptylatestWriteC : Boolean = latestWriteC.nonEmpty

  def isNotReplicaUpdatedOnWriteOperation(otherC: Certificate[GrantTS]): Boolean =
    notEmptylatestWriteC && (latestWriteC.get.items.head > otherC.items.head)

  def getNumberDifferentWrite1OkMsg: Int = oKMessages.size

  def getNumberRefusedMsg:Int = refusedMessages.size

  def getReplicasIDNotUpdate(updateReplicasMsg:List[Write1OKMessage]): List[Int] = oKMessages
    .flatten
    .diff(updateReplicasMsg)
    .filter(_.grantTS.areTSOrVSNotEqual(updateReplicasMsg.head.grantTS))
    .map(_.grantTS.replicaID)

  def resetState: Write1StateVariable[T] = Write1StateVariable(w1, List(), Option.empty, List(), List(), opHash)
}

case class Write1OkQuorumStateVariable[T](w1: Write1Message[T],
                                          writeC: Certificate[GrantTS],
                                          latestWriteC: Certificate[GrantTS],
                                          recevedMessages: List[Int],
                                          opHash: Int){
  def addRecevedMessages(replicaID: Int): Write1OkQuorumStateVariable[T] =
    Write1OkQuorumStateVariable(w1, writeC, latestWriteC, recevedMessages:+ replicaID, opHash)
  def addGrantTS(grantTS: GrantTS): Write1OkQuorumStateVariable[T] =
    Write1OkQuorumStateVariable(w1, Certificate(writeC.items :+ grantTS), latestWriteC, recevedMessages, opHash)
  def isGrantTSSameAsOther(grantTS: GrantTS):Boolean = grantTS == writeC.items.head

  def getNumberRecevedMsg: Int = recevedMessages.size

  def createWrite2StateVariable: Write2StateVariable =
    Write2StateVariable(writeC, List(), opHash)
}


case class Write2StateVariable(writeC: Certificate[GrantTS], recevedMessages: List[Int], opHash: Int){
  def addRecevedMessages(replicaID: Int): Write2StateVariable =
    Write2StateVariable(writeC,recevedMessages :+ replicaID, opHash)
  def getNumerRecevedMsg: Int = recevedMessages.size
}

//ToDo Ci sono dei metodi uguali alle case class precendenti, trovare un modo per eliminare i metodi ridonsanti
case class ReadStateVariable(latestWriteC: Option[Certificate[GrantTS]],  recevedMessages: List[Int], opHash: Int){
  def setLatestWriteC(writeC: Certificate[GrantTS]): ReadStateVariable ={
    val newLatestWriteC = latestWriteC match {
      case o if o.isEmpty => Option.apply(writeC)
      case o if o.get.items.head > writeC.items.head => o
      case _ => Option.apply(writeC)
    }
    ReadStateVariable(newLatestWriteC, recevedMessages, opHash)
  }

  def addRecevedMessages(replicaID: Int): ReadStateVariable =
    ReadStateVariable(latestWriteC, recevedMessages :+ replicaID, opHash)

  def update(msg: ReadAnsMessage): ReadStateVariable =
    this.addRecevedMessages(msg.replicaID)
    .setLatestWriteC(msg.currentC)

  def notEmptylatestWriteC: Boolean = latestWriteC.nonEmpty

  def isNotReplicaUpdatedOnWriteOperation(otherC: Certificate[GrantTS]): Boolean =
    notEmptylatestWriteC && (latestWriteC.get.items.head > otherC.items.head)

  def getNumerRecevedMsg: Int = recevedMessages.size
}





