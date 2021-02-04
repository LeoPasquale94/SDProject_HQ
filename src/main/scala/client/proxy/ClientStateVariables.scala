package Client.proxy

import AuthenticationCertification.{Certificate, GrantTS}
import messages.{ReadAnsMessage, Write1Message, Write1OKMessage, Write1RefusedMessage}

trait ManagerLatestWriteC {
  def chooseLatestWriteCFrom(writeC: Certificate[GrantTS], latestWriteC: Option[Certificate[GrantTS]]): Option[Certificate[GrantTS]] =  latestWriteC match {
      case o if o.isEmpty => Option.apply(writeC)
      case o if o.get.items.head > writeC.items.head => o
      case _ => Option.apply(writeC)
    }
}

case class StateVariables(recevedMessages: List[Int], opHash: Int){

  def addRecevedMsg(replicaID: Int): List[Int] = recevedMessages :+ replicaID

  def getSizeRecevedMessages: Int = recevedMessages.size

}

case class Write1StateVariable[T](w1: Write1Message[T],
                                  oKMessages: List[List[Write1OKMessage]],
                                  latestWriteC: Option[Certificate[GrantTS]],
                                  refusedMessages: List[Write1RefusedMessage],
                                  override val recevedMessages: List[Int],
                                  override val opHash: Int) extends  StateVariables(recevedMessages, opHash) with ManagerLatestWriteC {

  def update(msg: Write1OKMessage): Write1StateVariable[T] =
    this.addWrite1OkMessage(msg)
      .addRecevedMessages(msg.grantTS.replicaID)
      .setLatestWriteC(msg.currentC)

  def update(msg: Write1RefusedMessage): Write1StateVariable[T] =
    this.addRefusedMessages(msg)
      .addRecevedMessages(msg.grantTS.replicaID)

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
    Write1StateVariable(w1, oKMessages, latestWriteC,refusedMessages, addRecevedMsg(replicaID), opHash)

  def addRefusedMessages(msg: Write1RefusedMessage): Write1StateVariable[T] =
    Write1StateVariable(w1, oKMessages, latestWriteC, refusedMessages :+ msg, recevedMessages , opHash)

  def setLatestWriteC(writeC: Certificate[GrantTS]): Write1StateVariable[T] ={
    Write1StateVariable(w1, oKMessages, chooseLatestWriteCFrom(writeC,latestWriteC),refusedMessages, recevedMessages, opHash)
  }

  def createWrite1OkQuorumStateVariable(writeC: Certificate[GrantTS]): Write1OkQuorumStateVariable[T] =
    Write1OkQuorumStateVariable(w1, writeC, latestWriteC.get, recevedMessages,opHash)

  def createWrite2StateVariable(writeC: Certificate[GrantTS]):Write2StateVariable =
    Write2StateVariable(writeC, List(), opHash)

  def notEmptylatestWriteC : Boolean = latestWriteC.nonEmpty

  def isNotReplicaUpdatedOnWriteOperation(otherC: Certificate[GrantTS]): Boolean =
    notEmptylatestWriteC && (latestWriteC.get.items.head > otherC.items.head)

  def getSizeDifferentWrite1OkMsg: Int = oKMessages.size

  def getSizeRefusedMsg:Int = refusedMessages.size

  def getReplicasIDNotUpdate(updateReplicasMsg:List[Write1OKMessage]): List[Int] = oKMessages
    .flatten
    .diff(updateReplicasMsg)
    .filter(_.grantTS.areTSOrVSNotEqual(updateReplicasMsg.head.grantTS))
    .map(_.grantTS.replicaID)

  def resetState: Write1StateVariable[T] = Write1StateVariable(w1, List(), Option.empty, List(), List(), opHash)

  @scala.annotation.tailrec
  private def addNewMex(msg:Write1OKMessage, oldList: List[List[Write1OKMessage]], newList: List[List[Write1OKMessage]]): List[List[Write1OKMessage]] = oldList match {
    case Nil => newList :+ List(msg)
    case (h :: t1) :: t2 if h == msg => newList ++ (((h :: t1) :+ msg) :: t2)
    case h :: t => addNewMex(msg, t, newList :+ h)
  }

}

case class Write1OkQuorumStateVariable[T](w1: Write1Message[T],
                                          writeC: Certificate[GrantTS],
                                          latestWriteC: Certificate[GrantTS],
                                          override val recevedMessages: List[Int],
                                          override val opHash: Int)   extends  StateVariables(recevedMessages,opHash){

  def addRecevedMessages(replicaID: Int): Write1OkQuorumStateVariable[T] =
    Write1OkQuorumStateVariable(w1, writeC, latestWriteC, addRecevedMsg(replicaID), opHash)

  def addGrantTS(grantTS: GrantTS): Write1OkQuorumStateVariable[T] =
    Write1OkQuorumStateVariable(w1, Certificate(writeC.items :+ grantTS), latestWriteC, recevedMessages, opHash)

  def isGrantTSSameAsOther(grantTS: GrantTS):Boolean = grantTS == writeC.items.head

  def createWrite2StateVariable: Write2StateVariable =
    Write2StateVariable(writeC, List(), opHash)
}


case class Write2StateVariable(writeC: Certificate[GrantTS], override val recevedMessages: List[Int], override val opHash: Int)
  extends  StateVariables(recevedMessages,opHash){

  def addRecevedMessages(replicaID: Int): Write2StateVariable =
    Write2StateVariable(writeC,addRecevedMsg(replicaID), opHash)
}

case class ReadStateVariable(latestWriteC: Option[Certificate[GrantTS]], override val recevedMessages: List[Int], override val opHash: Int)
  extends  StateVariables(recevedMessages,opHash) with ManagerLatestWriteC {

  def setLatestWriteC(writeC: Certificate[GrantTS]): ReadStateVariable =
    ReadStateVariable(chooseLatestWriteCFrom(writeC,latestWriteC), recevedMessages, opHash)

  def addRecevedMessages(replicaID: Int): ReadStateVariable =
    ReadStateVariable(latestWriteC, recevedMessages :+ replicaID, opHash)

  def update(msg: ReadAnsMessage): ReadStateVariable =
    this.addRecevedMessages(msg.replicaID)
    .setLatestWriteC(msg.currentC)

  def notEmptylatestWriteC: Boolean = latestWriteC.nonEmpty

  def isNotReplicaUpdatedOnWriteOperation(otherC: Certificate[GrantTS]): Boolean =
    notEmptylatestWriteC && (latestWriteC.get.items.head > otherC.items.head)

  }





