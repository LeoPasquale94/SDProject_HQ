import akka.actor.{Actor, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.{Await, TimeoutException}
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
 implicit var timeout: Timeout = Timeout(4 seconds)
 var notTimeOut = true
 while(notTimeOut){
  try{
   val future = aRef ? Start()
   println("Sono in attesa")
   val value =  Await.result(future, timeout.duration).asInstanceOf[Int]
   println("Ecco il risultato: " + value)
   notTimeOut = false
  }catch {
   case _: TimeoutException => timeout = Timeout(10 seconds); println("tempo scaduto")
  }
 }

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
