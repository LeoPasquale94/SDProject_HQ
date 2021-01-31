package client

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

object trymsg2act extends App {

  case object AskNameMessage

  case class A() extends Actor {

    override def receive: Receive = {
      case AskNameMessage => {
        sleep(5000)
        context.sender() ! (Math.random() * 10).toInt
      }
    }

    def sleep(time: Long) {
      Thread.sleep(time)
    }
  }

  val system = ActorSystem()
  val aRef = system.actorOf(Props(A()))

  //Modo - 1 - Bloccante
  implicit val timeout: Timeout = Timeout(1000 seconds)
  val future = aRef ? AskNameMessage
  println("Sono in attesa:")
  val value = Await.result(future, timeout.duration).asInstanceOf[Int]
  println("Ecco il risultato:" + value)

}
