package Server

import AuthenticationCertification.{Certificate, GrantTS}
import messages.Write1Message

case class Ops(write1Requests: List[Write1Message], write1ReqExeRecently: Write1Message ) {
  def addWrite1Request(mex: Write1Message):Ops = Ops(write1Requests :+ mex, write1ReqExeRecently)

  def setWrite1ReqExeRecently(mex: Write1Message): Ops = Ops(write1Requests, mex)
}
case class ClientAuthorizedToWriteInf(op: Int, mostRecentyExeWriteReq: Write1Message, result: Any, currentC: Certificate[GrantTS]){
  def >(otherOp: Int): Boolean = op > otherOp

  def ==(otherOp: Int): Boolean = op == otherOp
}

case class OldOps(mapOldOps: Map[String, ClientAuthorizedToWriteInf]){
  def add(clientID: String, clientInf:ClientAuthorizedToWriteInf): OldOps = OldOps( mapOldOps + (clientID -> clientInf))

  def add (clientID: String, op: Int, mostRecentyExeWriteReq: Write1Message, result: Any, currentC: Certificate[GrantTS]): OldOps =
    OldOps( mapOldOps + (clientID -> ClientAuthorizedToWriteInf(op, mostRecentyExeWriteReq, result, currentC)))

  def getClientInf(clientID: String): Option[ClientAuthorizedToWriteInf] =
    if(mapOldOps.contains(clientID)){ Option.apply(mapOldOps(clientID)) } else {Option.empty}
}


case class ObjectInformation(currentC: Certificate[GrantTS], grantTS: Option[GrantTS], ops: Ops, oldOps: OldOps, vs: Double){
  def isGrantTSNull():Boolean = grantTS.isEmpty

  def setGrantTs(newGrantTS: GrantTS): ObjectInformation = ObjectInformation(currentC, Option.apply(newGrantTS), ops: Ops, oldOps, vs)

  def addWrite1Request(mex: Write1Message):ObjectInformation = ObjectInformation(currentC, grantTS, ops.addWrite1Request(mex), oldOps, vs)

  def setWrite1ReqExeRecently(mex: Write1Message):ObjectInformation = ObjectInformation(currentC, grantTS, ops.setWrite1ReqExeRecently(mex), oldOps, vs)
}
