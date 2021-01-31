import akka.actor.{Actor, ActorSystem, Props}
import akka.dispatch.Futures
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.{Await, TimeoutException}
import scala.util.{Failure, Success}

case class Start()

object Try extends App{

 case class A() extends Actor {

  private var nOp = 0
  private var lastData = 0

  override def receive: Receive = {
   case _:Start =>{
    sleep(5000)
    context.sender() ! (Math.random() * 10). toInt
   }
   case msg(nOp, n) => {
    if(nOp == this.nOp) {
     sleep(5000)
     lastData = n
     context.sender() ! n
     this.nOp += 1
    }
    else
     if(nOp == this.nOp - 1)
      context.sender() ! n
     else
      context.sender() ! Futures.failed(new wrongOpIndex)
   }
  }

  def sleep(time: Long) { Thread.sleep(time) }
 }

 val system = ActorSystem()
 val aRef = system.actorOf(Props(A()))
 implicit var timeout: Timeout = Timeout(4 seconds)

 //Modo - 1 - Bloccante
 /*
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
 }*/

 //Modo - 2 - Non bloccante
 case class msg(nOp: Int, op: Int)
 implicit val disp =  system.dispatcher

 var redo = false

 do{
  redo = false
  println("Sono in attesa")
  val future2 =  aRef ? msg(0, 2)
  /*future2.onComplete{
   case Success(value) => println(value)
   case Failure(exception) => println("errore")
  }*/
  try{
   val value =  Await.result(future2, timeout.duration).asInstanceOf[Int]
   println(value)
  }catch {
   case _: TimeoutException => redo = true; timeout = Timeout(5 seconds);println("tempo scaduto")
  }
 }while(redo)

 println("fatto")

 case class wrongOpIndex() extends Exception{

 }

}
