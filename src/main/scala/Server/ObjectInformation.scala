package Server

import AuthenticationCertification.{Certificate, GrantTS}
import messages.Write1Message

case class Ops[T](write1Requests: List[Write1Message[T]], write1ReqExeRecently: Write1Message[T] ) {
  def addWrite1Request(mex: Write1Message[T]):Ops[T] = Ops(write1Requests :+ mex, write1ReqExeRecently)

  def setWrite1ReqExeRecently(mex: Write1Message[T]): Ops[T] = Ops(write1Requests, mex)
}
case class ClientAuthorizedToWriteInf[T](op: Int, mostRecentyExeWriteReq: Write1Message[T], result: Any, currentC: Certificate[GrantTS]){
  def >(otherOp: Int): Boolean = op > otherOp

  def ==(otherOp: Int): Boolean = op == otherOp
}

case class OldOps[T](mapOldOps: Map[String, ClientAuthorizedToWriteInf[T]]){
  def add(clientID: String, clientInf:ClientAuthorizedToWriteInf[T]): OldOps[T] = OldOps( mapOldOps + (clientID -> clientInf))

  def add (clientID: String, op: Int, mostRecentyExeWriteReq: Write1Message[T], result: Any, currentC: Certificate[GrantTS]): OldOps[T] =
      add(clientID, ClientAuthorizedToWriteInf(op, mostRecentyExeWriteReq, result, currentC))

  def getClientInf(clientID: String): Option[ClientAuthorizedToWriteInf[T]] =
    if(mapOldOps.contains(clientID)){ Option.apply(mapOldOps(clientID)) } else {Option.empty}
}


case class ObjectInformation[T](currentC: Certificate[GrantTS], grantTS: Option[GrantTS], ops: Ops[T], oldOps: OldOps[T], vs: Double){
  def isGrantTSNull:Boolean = grantTS.isEmpty

  def setGrantTs(newGrantTS: GrantTS): ObjectInformation[T] = ObjectInformation(currentC, Option.apply(newGrantTS), ops: Ops[T], oldOps, vs)

  def addWrite1Request(mex: Write1Message[T]):ObjectInformation[T] = ObjectInformation(currentC, grantTS, ops.addWrite1Request(mex), oldOps, vs)

  def setWrite1ReqExeRecently(mex: Write1Message[T]):ObjectInformation[T] = ObjectInformation(currentC, grantTS, ops.setWrite1ReqExeRecently(mex), oldOps, vs)

  def addClientTAuthorizedToWrite(clientID: String, clientInf:ClientAuthorizedToWriteInf[T]): ObjectInformation[T] =
    ObjectInformation(currentC, grantTS, ops, oldOps.add(clientID,clientInf), vs)

  def addClientTAuthorizedToWrite(clientID: String, op: Int, mostRecentyExeWriteReq: Write1Message[T], result: Any, currentInWrite2AnsMex: Certificate[GrantTS]): ObjectInformation[T] =
    ObjectInformation(currentC, grantTS, ops, oldOps.add(clientID,op, mostRecentyExeWriteReq, result, currentInWrite2AnsMex), vs)

  def getInfOfClientAuthorizedToWrite(clientID: String):Option[ClientAuthorizedToWriteInf[T]] = oldOps.getClientInf(clientID)
}
