package Client.proxy

import AuthenticationCertification.{Certificate, GrantTS}
import messages.{Write1Message, Write1OKMessage, Write1RefusedMessage}

case class Write1StateVariable[T](w1: Write1Message[T],
                               oKMessages: List[List[Write1OKMessage]],
                               latestWriteC: Option[Certificate[GrantTS]],
                               refusedMessages: List[Write1RefusedMessage],
                               recevedMessages: List[Int],
                               opHash: Int){
 @scala.annotation.tailrec
 private def addNewMex(mes:Write1OKMessage, oldList: List[List[Write1OKMessage]], newList: List[List[Write1OKMessage]]): List[List[Write1OKMessage]] = oldList match {
    case Nil => newList :+ List(mes)
    case (h :: t1) :: t2 if h == mes => newList ++ (((h :: t1) :+ mes) :: t2)
    case h :: t => addNewMex(mes, t, newList :+ h)
  }

  def areThereMoreThenQuorumEqualOKMex(quorum: Int): Option[List[Write1OKMessage]] = {
    @scala.annotation.tailrec
    def _areThereMoreThenQuorumEqualOKMex(l: List[List[Write1OKMessage]]): Option[List[Write1OKMessage]] = l match {
      case Nil => Option.empty
      case h :: _ if h.size > quorum => Option.apply(h)
      case _ :: t => _areThereMoreThenQuorumEqualOKMex(t)
    }
    _areThereMoreThenQuorumEqualOKMex(oKMessages)
  }
  def addWrite1OkMessage(mes:Write1OKMessage): Write1StateVariable[T] =
    Write1StateVariable(w1, addNewMex(mes, oKMessages, List()), latestWriteC,refusedMessages, recevedMessages, opHash)

  def addRecevedMessages(replicaID: Int): Write1StateVariable[T] =
    Write1StateVariable(w1, oKMessages, latestWriteC,refusedMessages, recevedMessages :+ replicaID, opHash)

  def setLatestWriteC(writeC: Certificate[GrantTS]): Write1StateVariable[T] =
    Write1StateVariable(w1, oKMessages, Option.apply(writeC),refusedMessages, recevedMessages, opHash)

}

case class Write1OkQuorumStateVariable[T](w1: Write1Message[T],
                                          writeC: Certificate[GrantTS],
                                          latestWriteC: Certificate[GrantTS],
                                          recevedMessages: List[Int])

case class Write2StateVariable(recevedMessages: List[Int], opHash: Int )

case class ReadStateVariable(latestWriteC: Option[Certificate[GrantTS]],
                             recevedMessages: List[Int])




