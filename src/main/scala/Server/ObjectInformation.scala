package Server

import AuthenticationCertification.{Certificate, GrantTS}
import messages.Write1Message

case class Ops[T](write1Requests: List[Write1Message[T]], write1ReqExeRecently: Write1Message[T] ) {
  def addWrite1Request(mex: Write1Message[T]):Ops[T] = Ops(write1Requests :+ mex, write1ReqExeRecently)

  def setWrite1ReqExeRecently(mex: Write1Message[T]): Ops[T] = Ops(write1Requests, mex)
}
case class ClientAuthorizedToWriteInf[T](nop: Int, mostRecentyExeWriteReq: Write1Message[T], result: Any, currentC: Certificate[GrantTS]){
  def >(otherNOp: Int): Boolean = nop > otherNOp

  def ==(otherNOp: Int): Boolean = nop == otherNOp
}

case class OldOps[T](mapOldOps: Map[Int, ClientAuthorizedToWriteInf[T]]){
  def add(clientID: Int, clientInf:ClientAuthorizedToWriteInf[T]): OldOps[T] = OldOps( mapOldOps + (clientID -> clientInf))

  def add (clientID: Int, op: Int, mostRecentyExeWriteReq: Write1Message[T], result: Any, currentC: Certificate[GrantTS]): OldOps[T] =
      add(clientID, ClientAuthorizedToWriteInf(op, mostRecentyExeWriteReq, result, currentC))

  def getClientInf(clientID: Int): Option[ClientAuthorizedToWriteInf[T]] =
    if(mapOldOps.contains(clientID)){ Option.apply(mapOldOps(clientID)) } else {Option.empty}
}


case class ObjectInformation[T](currentC: Certificate[GrantTS], grantTS: Option[GrantTS], ops: Ops[T], oldOps: OldOps[T], vs: Double){
  def isGrantTSNull:Boolean = grantTS.isEmpty

  def setGrantTs(newGrantTS: GrantTS): ObjectInformation[T] = ObjectInformation(currentC, Option.apply(newGrantTS), ops: Ops[T], oldOps, vs)

  def addWrite1Request(mex: Write1Message[T]):ObjectInformation[T] = ObjectInformation(currentC, grantTS, ops.addWrite1Request(mex), oldOps, vs)

  def setWrite1ReqExeRecently(mex: Write1Message[T]):ObjectInformation[T] = ObjectInformation(currentC, grantTS, ops.setWrite1ReqExeRecently(mex), oldOps, vs)

  def addClientTAuthorizedToWrite(clientID: Int, clientInf:ClientAuthorizedToWriteInf[T]): ObjectInformation[T] =
    ObjectInformation(currentC, grantTS, ops, oldOps.add(clientID,clientInf), vs)

  def addClientTAuthorizedToWrite(clientID: Int, op: Int, mostRecentyExeWriteReq: Write1Message[T], result: Any, currentInWrite2AnsMex: Certificate[GrantTS]): ObjectInformation[T] =
    ObjectInformation(currentC, grantTS, ops, oldOps.add(clientID,op, mostRecentyExeWriteReq, result, currentInWrite2AnsMex), vs)

  def getInfOfClientAuthorizedToWrite(clientID: Int):Option[ClientAuthorizedToWriteInf[T]] = oldOps.getClientInf(clientID)
}

case class Object[T](objectInformation: ObjectInformation[T], result: Any)

case class Objects[T](objs: Map[Int, Object[T]]){

  def getObjectResult(objectID:Int): Any = objs(objectID).result

  def getObjectInformation(objectID:Int ): ObjectInformation[T] = objs(objectID).objectInformation
}
