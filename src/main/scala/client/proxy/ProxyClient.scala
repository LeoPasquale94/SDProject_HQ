package client.proxy

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import client.proxy.exception.WrongOpIndexException
import dummy.{DummyActor, DummyReplica}
import messages.{RequireReadMessage, RequireWriteMessage}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

trait ProxyClient {
  def write(op: Float => Float, oid: Int): Float
  def read(oid: Int): Float
}

object ProxyClient{

  private val instance: ProxyClientActor = new ProxyClientActor()

  def apply(): ProxyClient = instance

  /**
   * implementazione del proxy client con l'attore
   */
  private class ProxyClientActor extends ProxyClient(){
    //TODO inizializzare attore da finire
    private implicit val timeout: Timeout = Timeout(10 seconds)
    private val system = ActorSystem()
    private val replicaRef = system.actorOf(Props[DummyReplica])
    private val clientActorRef = system.actorOf(Props(DummyActor(replicaRef)))

    //private val clientActorRef = system.actorOf(Props(ClientActor(???, ???)))
    /*
    TODO
    1. I nomi dei client non ci fregano direttamente visto che possiamo memorizzare gli actor ref remoti
        (ma bisogna fa attenzione)
    2. Gli actor ref remoti dei server come li facciamo a conoscere a priori? bho
     */
    private var opCounter = 0

    override def write(op: Float => Float, oid: Int): Float= require(RequireWriteMessage(oid, op, opCounter)).asInstanceOf[Float]

    override def read(oid: Int): Float = require(RequireReadMessage(oid, opCounter)).asInstanceOf[Float]

    private def require[ H](message : H): Any = {
      var redo = false
      var op = Option.empty
      val future = clientActorRef ? message


      do{
        try {
            val future = clientActorRef ? message
            Await.result(future, timeout.duration)  match {
              case t: Float =>
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