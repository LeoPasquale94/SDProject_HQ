package Server

import AuthenticationCertification.{Certificate, GrantTS}
import akka.actor.ActorRef
import messages.{Write1Message, Write1OKMessage, Write1RefusedMessage, Write2AnsMessage}


case class Ops(write1RequestsOk: Write1Message, responseWrite1Req: Write1OKMessage, write1RequestsRefused: Map[Write1Message, Write1RefusedMessage], write1ReqExeRecently: Option[Write1Message]) {

  def addWrite1RequestOk[R](msg: Write1Message, response: Write1OKMessage):Ops =
    Ops(msg, response, write1RequestsRefused, write1ReqExeRecently)

  def addWrite1RequestRefused[R](msg: Write1Message, response: Write1RefusedMessage):Ops =
    Ops(write1RequestsOk, responseWrite1Req, write1RequestsRefused + (msg -> response), write1ReqExeRecently)

  def setWrite1ReqExeRecently(mex: Write1Message): Ops =
    Ops(write1RequestsOk, responseWrite1Req, write1RequestsRefused, Option.apply(mex))

  def write1RequestExist(msg: Write1Message):Boolean = write1RequestsOk == msg || write1RequestsRefused.contains(msg)

  def sendToClientLastResponse(msg: Write1Message, clientRef: ActorRef): Unit = {
    if(write1RequestsOk == msg){
      clientRef ! responseWrite1Req
    } else {
      clientRef ! write1RequestsRefused(msg)
    }
  }

}

object Ops {
  def apply(request: Write1Message, response: Write1OKMessage): Ops = Ops(request, response, Map(), Option.empty)
}

case class ClientAuthorizedToWriteInf(nop: Int, mostRecentyExeWriteReq: Write1Message, result: Float, currentC: Certificate[GrantTS]){

  def >(otherNOp: Int): Boolean = nop > otherNOp

  def ==(otherNOp: Int): Boolean = nop == otherNOp

  def getWrite2Ans(replicaID: Int): Write2AnsMessage =
    Write2AnsMessage(result, currentC, replicaID)

  def update(newNop: Int, newResult: Float, newWriteC: Certificate[GrantTS]): ClientAuthorizedToWriteInf =
    ClientAuthorizedToWriteInf(newNop,mostRecentyExeWriteReq, newResult, newWriteC)
}

case class OldOps(mapOldOps: Map[Int, ClientAuthorizedToWriteInf]){

  def add(clientID: Int, clientInf:ClientAuthorizedToWriteInf): OldOps =
    OldOps( mapOldOps + (clientID -> clientInf))

  def add (clientID: Int, op: Int, mostRecentyExeWriteReq: Write1Message, result: Float, currentC: Certificate[GrantTS]): OldOps =
      add(clientID, ClientAuthorizedToWriteInf(op, mostRecentyExeWriteReq, result, currentC))

  def getClientInfOption(clientID: Int): Option[ClientAuthorizedToWriteInf] =
    if(mapOldOps.contains(clientID)){ Option.apply(mapOldOps(clientID)) } else {Option.empty}

  def getClientInf(clientID: Int): ClientAuthorizedToWriteInf =
    mapOldOps(clientID)

  def getWrite2Ans(clientID: Int, replicaID: Int): Write2AnsMessage =
    mapOldOps(clientID).getWrite2Ans(replicaID)

  def updateClientInf(clientId: Int, newNop: Int, newResult: Float, newWriteC: Certificate[GrantTS]):OldOps =
    OldOps(mapOldOps + (clientId -> mapOldOps(clientId).update(newNop,newResult, newWriteC)))
}

case class ObjectInformation(currentC: Certificate[GrantTS], grantTS: Option[GrantTS], ops: Option[Ops], oldOps: Option[OldOps], vs: Double){

  def isGrantTSNull:Boolean = grantTS.isEmpty

  def setGrantTS(msg: Write1Message, replicaID: Int): ObjectInformation =
    ObjectInformation(currentC, Option.apply(createGrantTS(msg, replicaID)), ops, oldOps, vs)

  def setGrantTS(newGrantTS: Option[GrantTS]): ObjectInformation =
    ObjectInformation(currentC, newGrantTS, ops, oldOps, vs)

  def setCurrentC(newCurrentC: Certificate[GrantTS]):ObjectInformation =
    ObjectInformation(newCurrentC, grantTS, ops, oldOps, vs)

  def updateClientInf(clientID: Int, result: Float): ObjectInformation =
    ObjectInformation(currentC, grantTS, ops, Option.apply(oldOps.get.updateClientInf(clientID, ??? ,result, currentC)), vs)

  def addWrite1RequestRefuseResponse(mex: Write1Message, response: Write1RefusedMessage):ObjectInformation =
    ObjectInformation(currentC, grantTS,  Option.apply(ops.get.addWrite1RequestRefused(mex,  response)), oldOps, vs)

  def addWrite1RequestOkResponse(msg: Write1Message, response: Write1OKMessage): ObjectInformation = {
    val newOps = if(ops.nonEmpty) Option.apply(ops.get.addWrite1RequestOk(msg, response)) else Option.apply(Ops(msg, response))
    ObjectInformation(currentC,  grantTS, newOps, oldOps, vs)
  }

  def setWrite1ReqExeRecently(mex: Write1Message):ObjectInformation = {
    ObjectInformation(currentC, grantTS, Option.apply(ops.get.setWrite1ReqExeRecently(mex)), oldOps, vs)
  }

  def addClientTAuthorizedToWrite(clientID: Int, clientInf:ClientAuthorizedToWriteInf): ObjectInformation=
    ObjectInformation(currentC, grantTS, ops, Option.apply(oldOps.get.add(clientID,clientInf)), vs)

  def addClientTAuthorizedToWrite(clientID: Int, op: Int, mostRecentyExeWriteReq: Write1Message, result: Float, currentInWrite2AnsMex: Certificate[GrantTS]): ObjectInformation =
    ObjectInformation(currentC, grantTS, ops, Option.apply(oldOps.get.add(clientID,op, mostRecentyExeWriteReq, result, currentInWrite2AnsMex)), vs)

  def getInfOfClientAuthorizedToWrite(clientID: Int):Option[ClientAuthorizedToWriteInf] = oldOps.get.getClientInfOption(clientID)

  private def createGrantTS(msg: Write1Message, replicaID: Int):GrantTS =
    GrantTS(msg.clientID, msg.objectID, msg.numberOperation, msg.clientID.hashCode() + msg.objectID.hashCode() + msg.numberOperation.hashCode(), currentC.items.head.timeStamp + 1, vs, replicaID)
}

case class Object(objectInformation: ObjectInformation, result: Float){

  def appendRequest(msg: Write1Message, response: Write1OKMessage): Object =
    Object(objectInformation.addWrite1RequestOkResponse(msg, response), result )

  def appendRequest(msg: Write1Message, response: Write1RefusedMessage): Object =
    Object(objectInformation.addWrite1RequestRefuseResponse(msg, response), result)

  def isGrantTSNull:Boolean = objectInformation.isGrantTSNull

  def setGrantTS(msg: Write1Message, replicaID: Int): Object =
    Object(objectInformation.setGrantTS(msg, replicaID), result)

  def setCurrentC(newCurrentC: Certificate[GrantTS]): Object =
    Object(objectInformation.setCurrentC(newCurrentC), result)

  def setGrantTSEmpty(): Object =
    Object(objectInformation.setGrantTS(Option.empty), result)

  def getCurrentC: Certificate[GrantTS] = objectInformation.currentC

  def createWrite2Response(replicaID: Int): Write2AnsMessage = Write2AnsMessage(result, getCurrentC, replicaID)

  def write(op: Float => Float): Object =
    Object(objectInformation, op(result.asInstanceOf))

}

case class Objects(objs: Map[Int, Object]){

  def getObjectInformation(objectID: Int): ObjectInformation = objs(objectID).objectInformation

  def appendRequest(msg: Write1Message, response: Write1OKMessage): Objects = {
    val update = objs(msg.objectID).appendRequest(msg, response)
    Objects(objs + (msg.objectID -> update))
  }

  def appendRequest(msg: Write1Message, response: Write1RefusedMessage): Objects = {
    val update = objs(msg.objectID).appendRequest(msg, response)
    Objects(objs + (msg.objectID -> update))
  }

  def isGrantTSNull(objectID: Int): Boolean = objs(objectID).isGrantTSNull

  def contains(objectID :Int): Boolean = objs.contains(objectID)

  def setGrantTS(msg: Write1Message, replicaID: Int): Objects = {
    val update = objs(msg.objectID).setGrantTS(msg, replicaID)
    Objects(objs + (msg.objectID -> update))
  }
  def setGrantTSEmpty(objectID: Int): Objects = {
    val update = objs(objectID).setGrantTSEmpty()
    Objects(objs + (objectID -> update))
  }

  def setCurrentC(objectID: Int, newCurrentC: Certificate[GrantTS]): Objects = {
    val update = objs(objectID).setCurrentC(newCurrentC)
    Objects(objs + (objectID -> update))
  }

  def getGrantTS(objectID: Int): GrantTS =
    objs(objectID).objectInformation.grantTS.get

  def getCurrentC(objectID: Int): Certificate[GrantTS] =
    objs(objectID).getCurrentC

  def getObjectInf(objectID: Int): ObjectInformation =
    objs(objectID).objectInformation

  def createWrite2Response(objectID: Int, clientID: Int): Write2AnsMessage =
    objs(objectID).createWrite2Response(clientID)

  def getViewstamp(objectID: Int): Double =
    objs(objectID).objectInformation.vs

  def write(objectID: Int, op: Float => Float): Objects = {
    val update = objs(objectID).write(op)
    Objects(objs + (objectID -> update))
  }

  def read(objectID: Int): Float =
    objs(objectID).result

}
