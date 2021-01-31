import akka.actor.{Actor, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success}

case class Start()

object Try extends App{

 case class A() extends Actor {

  override def receive: Receive = {
   case _:Start =>{
    sleep(5000)
    context.sender() ! (Math.random() * 10). toInt
   }
  }

  def sleep(time: Long) { Thread.sleep(time) }
 }

 val system = ActorSystem()
 val aRef = system.actorOf(Props(A()))

 //Modo - 1 - Bloccante
 implicit val timeout: Timeout = Timeout(1000 seconds)
 val future = aRef ? Start()
 println("Sono in attesa:")
 val value =  Await.result(future, timeout.duration).asInstanceOf[Int]
 println("Ecco il risultato:" + value)


 //Modo - 2 - Non bloccante
 val future2 =  aRef ? Start()
 implicit val disp =  system.dispatcher
 println("Sono in attesa")
 future2.onComplete{
  case Success(value) => println(value)
  case Failure(exception) => println("non completata")
 }
 println("dopo")
}
