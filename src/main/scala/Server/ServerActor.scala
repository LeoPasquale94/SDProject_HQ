package Server

import akka.actor.Actor
import messages.{ObjectNotFoundMessage, ReadAnsMessage, ReadMessage, SignedMessage, Write1Message, Write1OKMessage, Write1RefusedMessage, Write2AnsMessage, Write2Message}

case class ServerActor(replicaID: Int) extends Actor{

  override def receive: Receive = activeState(ObjectInfInitializer.initObj)

  def activeState(objects: Objects): Receive = {
    case event: ReadMessage => computeReadMessage(event, objects)
    case event: Write1Message=> computeWrite1Message(event, objects)
    case event: Write2Message => computeWrite2Message(event, objects)
  }

  def frozenState: Receive = ???

  private def computeReadMessage(msg: ReadMessage, objects: Objects): Unit = {
    if(objects.isContainsObjectID(msg.objectID)){
      val result = objects.read(msg.objectID)
      val currentC = objects.getCurrentC(msg.objectID)
      sendSignMsg(ReadAnsMessage(result,currentC, replicaID))
      context.become(activeState(objects))
    }else{
      context.sender() ! ObjectNotFoundMessage()
    }
  }

  private def computeWrite1Message(msg: Write1Message, objects: Objects): Unit = {
    if(checkRequest(msg.objectID, msg.clientID, msg.numberOperation, objects)){

      if(objects.write1RequestExist(msg)){
        context.sender() ! objects.getLastResponse(msg)
      }else if(objects.isGrantTSEmpty(msg.objectID)){
        val updateObjects = objects.setGrantTS(msg, replicaID)
        val grantTS = updateObjects.getGrantTS(msg.objectID)
        val response = Write1OKMessage(grantTS, objects.getCurrentC(msg.objectID))
        sendSignMsg(response)
        context.become(activeState(updateObjects.appendRequest(msg, response)))
      }else{
        val currentC = objects.getCurrentC(msg.objectID)
        val grantTS = objects.getGrantTS(msg.objectID)
        val response = Write1RefusedMessage(grantTS, grantTS.clientID, grantTS.objectID, grantTS.numberOperation, currentC)
        sendSignMsg(response)
        context.become(activeState(objects.appendRequest(msg, response)))
      }
    }
  }

  private def computeWrite2Message(msg: Write2Message, objects: Objects): Unit = {
    val clientWriteCertificate = msg.writeC.items.head
    if(checkRequest(clientWriteCertificate.objectID, clientWriteCertificate.clientID, clientWriteCertificate.numberOperation, objects)){

      val currentC = objects.getCurrentC(clientWriteCertificate.objectID).items.head
      val vs = objects.getViewstemp(clientWriteCertificate.objectID)

      if(vs == clientWriteCertificate.viewStemp && currentC.timeStamp == clientWriteCertificate.timeStamp - 1 ){
        val write1Req = objects.getGrantedRequest(clientWriteCertificate.objectID)
        val objectID = write1Req.objectID
        //ToDo controllare meglio
        val updateObjects = objects
          .write(objectID, write1Req.op)
          .setGrantTSEmpty(objectID)
          .setCurrentC(objectID, msg.writeC)
          .updateClientInf(objectID, write1Req.clientID)
          .setWrite1ReqExeRecently(write1Req)
        val response = Write2AnsMessage(updateObjects.getResult(write1Req.objectID), msg.writeC, replicaID)
        sendSignMsg(response)
        context.become(activeState(updateObjects))
      }else{
        //ToDo gestisci conflitto
      }
    }

  }

  private def checkRequest[M](objectID: Int, clientID: Int, nopMsg: Int, objs: Objects): Boolean = {
    if(objs.isContainsObjectID(objectID) &&
      objs.isOldOpsNotEmpty(objectID) &&
      objs.isClientContainedInOldOps(objectID, clientID)){

      val oldOps = objs.getObject(objectID).getOldOps
      if(oldOps.getClientInf(clientID) > nopMsg) {
        context.sender()! Some("Old_Request")
        return false
      }
      if(oldOps.getClientInf(clientID) == nopMsg){
        sendSignMsg(oldOps.getOldWrite2Ans(clientID, replicaID))
        return false
      }
    }
    true
  }

  private def sendSignMsg[M](msg: M): Unit =  {
    context.sender() ! msg
  }
}
