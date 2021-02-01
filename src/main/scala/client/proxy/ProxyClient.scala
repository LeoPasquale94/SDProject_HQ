package client.proxy

import akka.actor.{ActorSystem, Props}
import messages.{RequireReadMessage, RequireWriteMessage}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.duration._




trait ProxyClient {
  def write[T](op: T => T, oid: String): T
  def read[T](oid: String): T
}

object ProxyClient{

  private val instance: ProxyClientActor = new ProxyClientActor()

  def apply(): ProxyClient = instance

  implicit val timeout: Timeout = Timeout(5 seconds)

  private class ProxyClientActor extends ProxyClient(){
    //TODO inizializzare attore
    private val system = ActorSystem()
    private val clientActorRef = system.actorOf(Props(ClientActor(???, ???)))

    //TODO inviare messaggio all'attore
    override def write[T](op: T => T, oid: String): T = require(RequireWriteMessage(oid, op))

    //TODO inviare il messaggio all'attore
    override def read[T](oid: String): T = require(RequireReadMessage(oid))

    private def require[T, H](message : H): T = {
      val future = clientActorRef ? message
      Await.result(future, timeout.duration).asInstanceOf[T]
    }
  }
}