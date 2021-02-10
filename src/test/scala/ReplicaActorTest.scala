import AuthenticationCertification.Certificate
import Server.ObjectInfInitializer.{certificate2, grantTS5, grantTS6, grantTS7, grantTS8, grantTSObj2}
import Server.ReplicaActor
import org.junit.runner.RunWith
import org.scalatest.{FunSuite, Matchers}
import org.scalatest.junit.JUnitRunner
import akka.actor.{ActorSystem, Props}
import messages.{ReadAnsMessage, ReadMessage, Write1Message, Write1OKMessage, Write2AnsMessage}
import akka.pattern.ask
import akka.util.Timeout
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.duration._



@RunWith(classOf[JUnitRunner])
class ReplicaActorTest extends FunSuite with Matchers with ScalaFutures{
  private val system = ActorSystem()
  implicit val disp =  system.dispatcher
  private val replicaRef = system.actorOf(Props(ReplicaActor(1)))
  implicit var timeout: Timeout = Timeout(10 seconds)

  test("Read Request of objectId = 2 "){
      whenReady(replicaRef ? ReadMessage(1, 2)){
        result => result.asInstanceOf[ReadAnsMessage].result shouldBe 7.0
      }
  }

  test("Old Write1 Request of objectID = 2 "){
    whenReady(replicaRef ? Write1Message(2, 2, 3, _ + 5)){
      result => result shouldBe Some("Old_Request")
    }
  }

  test("Redundant Write1 Request of objectID = 2"){
    whenReady(replicaRef ? Write1Message(2, 2, 5, _ + 5)){
      case result: Write2AnsMessage => result shouldBe Write2AnsMessage(7,Certificate(List(grantTS5, grantTS6, grantTS7, grantTS8)), 1)
    }
  }

  test("Request Write1 stored in ops"){
    whenReady(replicaRef ? Write1Message(2, 2, 6, _ + 5)){
      case result: Write1OKMessage => result shouldBe Write1OKMessage(grantTSObj2, certificate2)
    }
  }


}
