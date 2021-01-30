package client.proxy

import akka.actor.{ActorSystem, Props}
import messages.RequireWriteMessage
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

  private class ProxyClientActor extends ProxyClient(){
    //TODO inizializzare attore
    private val system = ActorSystem()
    private val clientActor = ClientActor(???, ???)
    private val clientActorRef = system.actorOf(Props(clientActor))


    //TODO inviare messaggio all'attore
    override def write[T](op: T => T, oid: String): T = {
      implicit val timeout: Timeout = Timeout(5 seconds)
      val future = clientActorRef ? RequireWriteMessage(oid, op)
      Await.result(future, timeout.duration).asInstanceOf[T]
    }
    //TODO inviare il messaggio all'attore
    override def read[T](oid: String): T = ???
  }
}