package Server

import AuthenticationCertification.{Certificate, GrantTS}
import akka.actor.{Actor, ActorRef}
import messages.{ObjectNotFoundMessage, ReadAnsMessage, ReadMessage, SignedMessage, Write1Message, Write1OKMessage, Write1RefusedMessage, Write2AnsMessage, Write2Message, WriteBackReadMessage, WriteBackWriteMessage}

case class ServerActor(replicaID: Int) extends Actor{

  override def receive: Receive = activeState(ObjectInfInitializer.initObj)

  def activeState(objects: Objects, sender: Option[ActorRef] = Option.empty): Receive = {
    case event: ReadMessage => computeReadMessage(event, objects, sender)
    case event: Write1Message=> computeWrite1Message(event, objects, sender)
    case event: Write2Message => computeWrite2Message(event.writeC, objects, sender)
    case event: WriteBackWriteMessage => computeWriteBackWriteMessage(event, objects)
    case event: WriteBackReadMessage => computeWriteBackReadMessage(event, objects)
  }

  def frozenState: Receive = ???

  private def computeReadMessage(msg: ReadMessage, objects: Objects, sender: Option[ActorRef]): Unit = {
    if(objects.isContainsObjectID(msg.objectID)){
      val result = objects.read(msg.objectID)
      val currentC = objects.getCurrentC(msg.objectID)
      sendSignMsg(ReadAnsMessage(result,currentC, replicaID), sendMsg = true, sender)
      context.become(activeState(objects, Option.empty))
    }else{
      sendSignMsg(ObjectNotFoundMessage())
    }
  }

  private def computeWrite1Message(msg: Write1Message, objects: Objects, sender: Option[ActorRef] ): Unit = {
    if(checkRequest(msg.objectID, msg.clientID, msg.numberOperation, objects)){

      if(objects.write1RequestExist(msg)){
        sendSignMsg(objects.getLastResponse(msg),sendMsg = true ,sender)
      }else if(objects.isGrantTSEmpty(msg.objectID)){
        val updateObjects = objects.setGrantTS(msg, replicaID)
        val grantTS = updateObjects.getGrantTS(msg.objectID)
        val response = Write1OKMessage(grantTS, objects.getCurrentC(msg.objectID))
        sendSignMsg(response, sendMsg = true, sender)
        context.become(activeState(updateObjects.appendRequest(msg, response), Option.empty))
      }else{
        val currentC = objects.getCurrentC(msg.objectID)
        val grantTS = objects.getGrantTS(msg.objectID)
        val response = Write1RefusedMessage(grantTS, grantTS.clientID, grantTS.objectID, grantTS.numberOperation, currentC)
        sendSignMsg(response, sendMsg = true, sender)
        context.become(activeState(objects.appendRequest(msg, response), Option.empty))
      }
    }
  }

  private def computeWrite2Message(writeC: Certificate[GrantTS], objects: Objects, sender: Option[ActorRef], sendMsg: Boolean = true): Unit = {
    val clientWriteCertificate = writeC.items.head
    if(checkRequest(clientWriteCertificate.objectID, clientWriteCertificate.clientID, clientWriteCertificate.numberOperation, objects)){

      val currentC = objects.getCurrentC(clientWriteCertificate.objectID).items.head
      val vs = objects.getViewstemp(clientWriteCertificate.objectID)
      //ToDo bisogna controllare write.cid == grantTS.cid
      if(vs == clientWriteCertificate.viewStemp && currentC.timeStamp == clientWriteCertificate.timeStamp - 1 ){
        val write1Req = objects.getGrantedRequest(clientWriteCertificate.objectID)
        val objectID = write1Req.objectID
        //ToDo controllare meglio
        val updateObjects = objects
          .write(objectID, write1Req.op)
          .setGrantTSEmpty(objectID)
          .setCurrentC(objectID, writeC)
          .updateClientInf(objectID, write1Req.clientID)
          .setWrite1ReqExeRecently(write1Req)
        val response = Write2AnsMessage(updateObjects.getResult(write1Req.objectID), writeC, replicaID)
        sendSignMsg(response, sendMsg, sender)
        context.become(activeState(updateObjects, sender))
      }else{
        //ToDo ottenere le informazioni mancati dalle altre repliche invocando BFT
      }
    }

  }

  private def computeWriteBackWriteMessage(msg: WriteBackWriteMessage, objects: Objects): Unit = {
    computeWrite2Message(msg.writeC, objects,  Option(context.sender()), sendMsg = false)
    self ! msg.write1Message
  }

  private def computeWriteBackReadMessage(msg: WriteBackReadMessage, objects: Objects): Unit = {
    computeWrite2Message(msg.writeC, objects,  Option(context.sender()), sendMsg = false)
    self ! ReadMessage(msg.clietID, msg.objectID)
  }

  private def checkRequest[M](objectID: Int, clientID: Int, nopMsg: Int, objs: Objects, sendMsg: Boolean = true): Boolean = {
    if(objs.isContainsObjectID(objectID) &&
      objs.isOldOpsNotEmpty(objectID) &&
      objs.isClientContainedInOldOps(objectID, clientID)){

      val oldOps = objs.getObject(objectID).getOldOps
      if(oldOps.getClientInf(clientID) > nopMsg) {
        context.sender()! Some("Old_Request")
        return false
      }
      if(oldOps.getClientInf(clientID) == nopMsg){
        sendSignMsg(oldOps.getOldWrite2Ans(clientID, replicaID), sendMsg)
        return false
      }
    }
    true
  }

  private def sendSignMsg[M](msg: M, sendMsg: Boolean = true, sender: Option[ActorRef] = Option.empty ): Unit =  {
    if(sendMsg) {
      if(sender.isEmpty)
        context.sender() ! msg
      else
        sender.get ! msg
    }
  }
}
