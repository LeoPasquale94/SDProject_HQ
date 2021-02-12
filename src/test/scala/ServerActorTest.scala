import AuthenticationCertification.{Certificate, GrantTS}
import Server.ObjectInfInitializer.{certificate2, grantTS10, grantTS11, grantTS12, grantTS13, grantTS14, grantTS15, grantTS16, grantTS5, grantTS6, grantTS7, grantTS8, grantTS9, grantTSObj2}
import Server.ServerActor
import org.junit.runner.RunWith
import org.scalatest.{FunSuite, Matchers}
import org.scalatest.junit.JUnitRunner
import akka.actor.{ActorSystem, Props}
import messages.{ReadAnsMessage, ReadMessage, Write1Message, Write1OKMessage, Write1RefusedMessage, Write2AnsMessage, Write2Message}
import akka.pattern.ask
import akka.util.Timeout
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._



class ServerActorTest extends FunSuite with Matchers with ScalaFutures{
  private val system = ActorSystem()
  implicit val disp: ExecutionContextExecutor =  system.dispatcher
  private val replicaRef = system.actorOf(Props(ServerActor(1)))
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
    whenReady(replicaRef ? Write1Message(2, 2, 5, _ + 5)){
      case result: Write2AnsMessage => result shouldBe Write2AnsMessage(7,Certificate(List(grantTS5, grantTS6, grantTS7, grantTS8)), 1)
    }
    whenReady(replicaRef ? Write1Message(2, 2, 5, _ + 5)){
      case result: Write2AnsMessage => result shouldBe Write2AnsMessage(7,Certificate(List(grantTS5, grantTS6, grantTS7, grantTS8)), 1)
    }
  }

  test("Write1 request stored in ops"){
    whenReady(replicaRef ? Write1Message(2, 2, 6, _ + 5)){
      case result: Write1OKMessage => result shouldBe Write1OKMessage(grantTSObj2, certificate2)
    }
  }

  test("Client 2 receives Write1OkMessage - client 1 receives Write1RefusedMessage - redundant request  - Client 2 computes write operation"){
    whenReady(replicaRef ?  Write1Message(2, 3, 6, _ + 5)) {
      case result: Write1OKMessage => result shouldBe Write1OKMessage(GrantTS(2, 3, 6, 2.hashCode() + 3.hashCode() + 6.hashCode(), 2, 0.5, 1), Certificate(List(grantTS9, grantTS10, grantTS11, grantTS12)))
    }
    whenReady(replicaRef ?  Write1Message(2, 3, 6, _ + 5)) {
      case result: Write1OKMessage => result shouldBe Write1OKMessage(GrantTS(2, 3, 6, 2.hashCode() + 3.hashCode() + 6.hashCode(), 2, 0.5, 1), Certificate(List(grantTS9, grantTS10, grantTS11, grantTS12)))
    }
    whenReady(replicaRef ?  Write1Message(1, 3, 3, _ + 10)){
      case result: Write1RefusedMessage => result shouldBe Write1RefusedMessage(GrantTS(2, 3, 6, 2.hashCode() + 3.hashCode() + 6.hashCode(), 2, 0.5, 1), 2, 3, 6, Certificate(List(grantTS9, grantTS10, grantTS11, grantTS12)))
    }
    whenReady(replicaRef ?  Write1Message(1, 3, 3, _ + 10)){
      case result: Write1RefusedMessage => result shouldBe Write1RefusedMessage(GrantTS(2, 3, 6, 2.hashCode() + 3.hashCode() + 6.hashCode(), 2, 0.5, 1), 2, 3, 6, Certificate(List(grantTS9, grantTS10, grantTS11, grantTS12)))
    }
    whenReady(replicaRef ? ReadMessage(1, 3)){
      result => result.asInstanceOf[ReadAnsMessage].result shouldBe 10.0
    }
    whenReady(replicaRef ?  Write2Message(Certificate(List(grantTS13, grantTS14, grantTS15, grantTS16)))) {
      case result: Write2AnsMessage => result shouldBe Write2AnsMessage(15, Certificate(List(grantTS13, grantTS14, grantTS15, grantTS16)), 1)
    }

   whenReady(replicaRef ?  Write2Message(Certificate(List(grantTS13, grantTS14, grantTS15, grantTS16)))) {
      case result: Write2AnsMessage => result shouldBe Write2AnsMessage(15, Certificate(List(grantTS13, grantTS14, grantTS15, grantTS16)), 1)
    }
    whenReady(replicaRef ? ReadMessage(1, 3)){
      result => result.asInstanceOf[ReadAnsMessage].result shouldBe 15.0
    }
  }

}
