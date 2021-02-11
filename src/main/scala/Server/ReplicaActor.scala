package Server

import akka.actor.Actor
import messages.{ObjectNotExistMessage, ReadAnsMessage, ReadMessage, SignedMessage, Write1Message, Write1OKMessage, Write1RefusedMessage, Write2AnsMessage, Write2Message}

case class ReplicaActor(replicaID: Int) extends Actor{

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
      context.sender() ! ObjectNotExistMessage()
    }

  }

  private def computeWrite1Message(msg: Write1Message, objects: Objects): Unit = {
    if(checkRequest(msg.objectID, msg.clientID, msg.numberOperation, objects, msg)){

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
    val grantTSOfMsg = msg.writeC.items.head
    if(checkRequest(grantTSOfMsg.objectID, grantTSOfMsg.clientID, grantTSOfMsg.numberOperation, objects, msg)){
      val currentC = objects.getCurrentC(grantTSOfMsg.objectID)
      val vs = objects.getViewstemp(grantTSOfMsg.objectID)
      if(vs == msg.writeC.items.head.viewStemp || currentC.items.head.timeStamp == currentC.items.head.timeStamp - 1 ){

      }else{
        //ToDo gestisci conflitto
      }
    }

  }

  private def checkRequest[M](objectId: Int, clientId: Int, nopMsg: Int, objs: Objects, msg: M): Boolean = {
    if(objs.isContainsObjectID(objectId) &&
      objs.isOldOpsNotEmpty(objectId) &&
      objs.isClientContainedInOldOps(objectId, clientId)){

      val oldOps = objs.getObjectInformation(objectId).oldOps.get
      if(oldOps.getClientInf(objectId) > nopMsg) {
        context.sender()! Some("Old_Request")
        return false
      }
      if(oldOps.getClientInf(objectId) == nopMsg){
        msg match {
          case msg: Write1Message => sendSignMsg(oldOps.getWrite2Ans(msg.clientID, replicaID))
          case _: Write2Message => sendSignMsg(objs.createWrite2Response(objectId, replicaID))
        }
        return false
      }
    }
    true
  }

  private def sendSignMsg[M](msg: M): Unit =  {
    context.sender() ! msg
  }
}
