package Server

import AuthenticationCertification.{Certificate, GrantTS}
import messages.{Write1Message, Write1OKMessage, Write1RefusedMessage, Write2AnsMessage}


case class Ops(write1RequestsOk: Option[Write1Message], responseWrite1Req: Option[Write1OKMessage], write1RequestsRefused: List[(Write1Message, Write1RefusedMessage)], write1ReqExeRecently: Option[Write1Message]) {

  def addWrite1RequestOk[R](msg: Write1Message, response: Write1OKMessage):Ops =
    Ops(Option(msg), Option(response), write1RequestsRefused, write1ReqExeRecently)

  def addWrite1RequestRefused[R](msg: Write1Message, response: Write1RefusedMessage):Ops =
    Ops(write1RequestsOk, responseWrite1Req, write1RequestsRefused :+ (msg, response), write1ReqExeRecently)

  def setWrite1ReqExeRecently(msg: Write1Message): Ops =
    Ops(write1RequestsOk, responseWrite1Req, write1RequestsRefused, Option.apply(msg))

  def write1RequestExist(msg: Write1Message):Boolean =
    (write1RequestsOk.nonEmpty && write1RequestsOk.get == msg) || write1RequestsRefused.exists(_._1 == msg)

  def getLastResponse(msg: Write1Message): Any =
    if(write1RequestsOk.get == msg) responseWrite1Req.get else write1RequestsRefused.filter(_._1 == msg).head._2

  def getGrantedRequest: Write1Message =
    write1RequestsOk.get

}

object Ops {
  def apply(request: Write1Message, response: Write1OKMessage): Ops =
    Ops(Option(request), Option(response), List(), Option.empty)

  def apply(request: Write1Message, response: Write1RefusedMessage): Ops =
    Ops(Option.empty, Option.empty, List((request,response)), Option.empty)

  def apply(write1ReqExeRecently: Write1Message): Ops =
    Ops(Option.empty, Option.empty, List(), Option(write1ReqExeRecently))
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

  def getClientInf(clientID: Int): ClientAuthorizedToWriteInf =
    mapOldOps(clientID)

  def getOldWrite2Ans(clientID: Int, replicaID: Int): Write2AnsMessage =
    mapOldOps(clientID).getWrite2Ans(replicaID)

  def updateClientInf(clientId: Int, newNop: Int, newResult: Float, newWriteC: Certificate[GrantTS]):OldOps =
    OldOps(mapOldOps + (clientId -> mapOldOps(clientId).update(newNop,newResult, newWriteC)))

  def isClientContains(clientId: Int): Boolean =
    mapOldOps.contains(clientId)
}

case class ObjectInformation(currentC: Certificate[GrantTS], grantTS: Option[GrantTS], ops: Option[Ops], oldOps: Option[OldOps], vs: Double){

  def write1RequestExist(msg: Write1Message): Boolean = ops.nonEmpty && ops.get.write1RequestExist(msg)

  def updateClientInf(clientID: Int, result: Float): ObjectInformation = {
    val newVal = currentC.items.head
    val updateOldops = if(oldOps.isEmpty) {
        OldOps(Map(clientID -> ClientAuthorizedToWriteInf(newVal.numberOperation, ops.get.getGrantedRequest, result, currentC)))
     }else {
        oldOps.get.updateClientInf(clientID, newVal.numberOperation, result, currentC)
     }
    ObjectInformation(currentC, grantTS, ops, Option(updateOldops), vs)

  }

  def addWrite1RequestRefuseResponse(msg: Write1Message, response: Write1RefusedMessage):ObjectInformation = {
    val newOps = if(ops.isEmpty) Ops(msg, response) else ops.get.addWrite1RequestRefused(msg, response)
    ObjectInformation(currentC, grantTS, Option(newOps), oldOps, vs)
  }

  def addWrite1RequestOkResponse(msg: Write1Message, response: Write1OKMessage): ObjectInformation = {
    val newOps = if (ops.isEmpty) Ops(msg, response) else ops.get.addWrite1RequestOk(msg, response)
    ObjectInformation(currentC, grantTS, Option(newOps), oldOps, vs)
  }
  def addClientTAuthorizedToWrite(clientID: Int, clientInf:ClientAuthorizedToWriteInf): ObjectInformation= {
    val newoldOps = if(oldOps.nonEmpty) {Option(oldOps.get.add(clientID,clientInf))} else { Option(OldOps(Map(clientID -> clientInf)))}
    ObjectInformation(currentC, grantTS, ops, newoldOps, vs)
  }
  //ToDo forse si può eliminare
  def addClientTAuthorizedToWrite(clientID: Int, op: Int, mostRecentyExeWriteReq: Write1Message, result: Float, currentInWrite2AnsMex: Certificate[GrantTS]): ObjectInformation =
    ObjectInformation(currentC, grantTS, ops, Option(oldOps.get.add(clientID,op, mostRecentyExeWriteReq, result, currentInWrite2AnsMex)), vs)

  def setGrantTS(msg: Write1Message, replicaID: Int): ObjectInformation =
    ObjectInformation(currentC, Option(createGrantTS(msg, replicaID)), ops, oldOps, vs)

  def setGrantTS(newGrantTS: Option[GrantTS]): ObjectInformation =
    ObjectInformation(currentC, newGrantTS, ops, oldOps, vs)

  def setCurrentC(newCurrentC: Certificate[GrantTS]):ObjectInformation =
    ObjectInformation(newCurrentC, grantTS, ops, oldOps, vs)

  def setWrite1ReqExeRecently(msg: Write1Message):ObjectInformation = {
    //ToDo Dovrebbe essere chiamato solo quando la richiesta è eseguita
   /// val newOps = if (ops.isEmpty) Ops(msg) else ops.get.setWrite1ReqExeRecently(msg)
    ObjectInformation(currentC, grantTS, Option(Ops(msg)), oldOps, vs)
  }

  def getLastResponse(msg: Write1Message): Any =
    ops.get.getLastResponse(msg)

  private def createGrantTS(msg: Write1Message, replicaID: Int):GrantTS =
    GrantTS(msg.clientID, msg.objectID, msg.numberOperation, msg.clientID.hashCode() + msg.objectID.hashCode() + msg.numberOperation.hashCode(), currentC.items.head.timeStamp + 1, vs, replicaID)
}

case class Object(objectInformation: ObjectInformation, result: Float){

  def write(op: Float => Float): Object =
    Object(objectInformation, op(result))

  def updateClientInf(clientID: Int): Object =
    Object(objectInformation.updateClientInf(clientID, result), result)

  def appendRequest(msg: Write1Message, response: Write1OKMessage): Object =
    Object(objectInformation.addWrite1RequestOkResponse(msg, response), result )

  def appendRequest(msg: Write1Message, response: Write1RefusedMessage): Object =
    Object(objectInformation.addWrite1RequestRefuseResponse(msg, response), result)

  def isGrantTSEmpty:Boolean = objectInformation.grantTS.isEmpty

  def isOldOpsNotEmpty: Boolean = objectInformation.oldOps.nonEmpty

  def isClientContainedInOldOps(clientID: Int): Boolean =
    objectInformation.oldOps.nonEmpty && objectInformation.oldOps.get.isClientContains(clientID)

  def write1RequestExist(msg: Write1Message):Boolean =
    objectInformation.write1RequestExist(msg)

  def setGrantTSEmpty(): Object =
    Object(objectInformation.setGrantTS(Option.empty), result)

  def setGrantTS(msg: Write1Message, replicaID: Int): Object =
    Object(objectInformation.setGrantTS(msg, replicaID), result)

  def setCurrentC(newCurrentC: Certificate[GrantTS]): Object =
    Object(objectInformation.setCurrentC(newCurrentC), result)

  def getCurrentC: Certificate[GrantTS] = objectInformation.currentC

  def getGrantTS: GrantTS = objectInformation.grantTS.get

  def getOps: Ops = objectInformation.ops.get

  def getOldOps: OldOps = objectInformation.oldOps.get

  def getViewstemp: Double = objectInformation.vs

  def getLastResponse(msg: Write1Message): Any =
    objectInformation.getLastResponse(msg)

  def setWrite1ReqExeRecently(msg: Write1Message):Object = {
    Object(objectInformation.setWrite1ReqExeRecently(msg), result)
  }

}

case class Objects(objs: Map[Int, Object]){

  def write(objectID: Int, op: Float => Float): Objects = {
    val update = objs(objectID).write(op)
    Objects(objs + (objectID -> update))
  }

  def read(objectID: Int): Float =
    objs(objectID).result

  def appendRequest(msg: Write1Message, response: Write1OKMessage): Objects = {
    val update = objs(msg.objectID).appendRequest(msg, response)
    Objects(objs + (msg.objectID -> update))
  }

  def appendRequest(msg: Write1Message, response: Write1RefusedMessage): Objects = {
    val update = objs(msg.objectID).appendRequest(msg, response)
    Objects(objs + (msg.objectID -> update))
  }

  def updateClientInf(objectID: Int, clientID: Int): Objects = {
    val update = objs(objectID).updateClientInf(clientID)
    Objects(objs + (objectID -> update))
  }

  def isContainsObjectID(objectID :Int): Boolean = objs.contains(objectID)

  def isGrantTSEmpty(objectID: Int): Boolean = objs(objectID).isGrantTSEmpty

  def isOldOpsNotEmpty(objectId: Int): Boolean = objs(objectId).isOldOpsNotEmpty

  def isClientContainedInOldOps(objectID: Int, clientID: Int): Boolean =
    objs(objectID).isClientContainedInOldOps(clientID)

  def write1RequestExist(msg: Write1Message) : Boolean =
    objs(msg.objectID).write1RequestExist(msg)

  def setWrite1ReqExeRecently(msg: Write1Message):Objects = {
    val update = objs(msg.objectID).setWrite1ReqExeRecently(msg)
    Objects(objs + (msg.objectID -> update))
  }

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
    objs(objectID).getGrantTS

  def getCurrentC(objectID: Int): Certificate[GrantTS] =
    objs(objectID).getCurrentC

  def getObject(objectID: Int): Object =
    objs(objectID)

  def getObjectInf(objectID: Int): ObjectInformation =
    objs(objectID).objectInformation

  def getViewstemp(objectID: Int): Double =
    objs(objectID).getViewstemp

  def getObjectInformation(objectID: Int): ObjectInformation =
    objs(objectID).objectInformation

  def getLastResponse(msg: Write1Message): Any =
    objs(msg.objectID).getLastResponse(msg)

  def getGrantedRequest(objectID: Int): Write1Message =
    objs(objectID).getOps.getGrantedRequest

  def getResult(objectID: Int): Float =
    objs(objectID).result
}
