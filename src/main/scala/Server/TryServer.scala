package Server


import akka.actor.{ActorSystem, Props}
import messages.{ReadAnsMessage, ReadMessage, Write1Message}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import scala.util.{Failure, Success}


object TryServer extends App {

  private val system = ActorSystem()
  implicit val disp =  system.dispatcher
  private val replicaRef = system.actorOf(Props(ServerActor(1)))
  implicit var timeout: Timeout = Timeout(10 seconds)

  //Todo Richiesta lettura objectId = 2 - dovrebbe tornare value = 7
 /* val future = replicaRef ? ReadMessage(1,2)
  future.onComplete {
    case Success(value) => println(s"Got the callback, value = ${value.asInstanceOf[ReadAnsMessage].result}")
    case Failure(e) => e.printStackTrace
  }*/


  val future1 = replicaRef ? Write1Message(2,2,5, _ + 5)
  future1.onComplete {
    case Success(value) => println(s"Got the callback, value = ${value}")
    case Failure(e) => e.printStackTrace
  }

}
