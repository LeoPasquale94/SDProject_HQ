package client.proxy

import Client.proxy.RequestMessageCreator
import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import client.proxy.exception.WrongOpIndexException
import dummy.DummyActor

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

trait ProxyClient {
  def write[T](op: T => T, oid: Int): T
  def read[T](oid: Int): T
}

object ProxyClient{

  private val instance: ProxyClientActor = new ProxyClientActor()

  def apply(): ProxyClient = instance

  /**
   * implementazione del proxy client con l'attore
   */
  private class ProxyClientActor extends ProxyClient(){
    //TODO inizializzare attore da finire
    private implicit val timeout: Timeout = Timeout(5 seconds)
    private val system = ActorSystem()
    private val clientActorRef = system.actorOf(Props[DummyActor])
    //private val clientActorRef = system.actorOf(Props(ClientActor(???, ???)))
    /*
    TODO
    1. I nomi dei client non ci fregano direttamente visto che possiamo memorizzare gli actor ref remoti
        (ma bisogna fa attenzione)
    2. Gli actor ref remoti dei server come li facciamo a conoscere a priori? bho
     */
    private var opCounter = 0

    //ToDo la classe statica RequestMessageCreator permette di ordinare le richieste utente poichè numera le richieste utente.

    override def write[T](op: T => T, oid: Int): T = require(RequestMessageCreator.createRequireWriteMessage(op,oid)).asInstanceOf[T]

    override def read[T](oid: Int): T = require(RequestMessageCreator.createRequireReadMessage(oid)).asInstanceOf[T]

    private def require[T, H](message : H): Any = {
      var redo = false
      var op = Option.empty
      val future = clientActorRef ? message


      do{
        try {
            val future = clientActorRef ? message
            Await.result(future, timeout.duration)  match {
              case t: T =>
                opCounter += 1
                return t
              case _ => throw WrongOpIndexException()
            }
        } catch {
            case _/*: TimeoutException*/ =>
              redo = true
              println("tempo scaduto")
            /*case _: WrongOpIndexException =>
              println("da sincronizzare")
            //TODO protocollo di sincronizzazione*/
        }
      }while(redo)
    }
  }
}