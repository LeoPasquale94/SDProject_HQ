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

  test("Multi Write e Read request"){
   /** Richiesta scrittura cliente 2 sull'oggetto 3 - Cliente ottiene il permesso di scrittura al ts = 2 */
    whenReady(replicaRef ?  Write1Message(2, 3, 6, _ + 5)) {
      case result: Write1OKMessage => result shouldBe Write1OKMessage(GrantTS(2, 3, 6, 2, 0.5, 1), Certificate(List(grantTS9, grantTS10, grantTS11, grantTS12)))
    }
    /**Richiesta duplicata*/
    whenReady(replicaRef ?  Write1Message(2, 3, 6, _ + 5)) {
      case result: Write1OKMessage => result shouldBe Write1OKMessage(GrantTS(2, 3, 6, 2, 0.5, 1), Certificate(List(grantTS9, grantTS10, grantTS11, grantTS12)))
    }
    /**Richiesta scrittura cliente 1 sull'oggetto 3 - Cliente non ottiene il permesso */
    whenReady(replicaRef ?  Write1Message(1, 3, 3, _ + 10)){
      case result: Write1RefusedMessage => result shouldBe Write1RefusedMessage(GrantTS(2, 3, 6, 2, 0.5, 1), 2, 3, 6, Certificate(List(grantTS9, grantTS10, grantTS11, grantTS12)))
    }
    whenReady(replicaRef ?  Write1Message(1, 3, 3, _ + 10)){
      case result: Write1RefusedMessage => result shouldBe Write1RefusedMessage(GrantTS(2, 3, 6, 2, 0.5, 1), 2, 3, 6, Certificate(List(grantTS9, grantTS10, grantTS11, grantTS12)))
    }
    /**Richiesta Lettura cliente 1 sull'oggetto 3 */
    whenReady(replicaRef ? ReadMessage(1, 3)){
      result => result.asInstanceOf[ReadAnsMessage].result shouldBe 10.0
    }
    /**Scrittura sull'oggetto 3 del cliente 2 che ha ottenuto il permesso */
    whenReady(replicaRef ?  Write2Message(Certificate(List(grantTS13, grantTS14, grantTS15, grantTS16)))) {
      case result: Write2AnsMessage => result shouldBe Write2AnsMessage(15, Certificate(List(grantTS13, grantTS14, grantTS15, grantTS16)), 1)
    }
   whenReady(replicaRef ?  Write2Message(Certificate(List(grantTS13, grantTS14, grantTS15, grantTS16)))) {
      case result: Write2AnsMessage => result shouldBe Write2AnsMessage(15, Certificate(List(grantTS13, grantTS14, grantTS15, grantTS16)), 1)
    }
    /**Lettura oggetto 3 da parte del cliente 1*/
    whenReady(replicaRef ? ReadMessage(1, 3)){
      result => result.asInstanceOf[ReadAnsMessage].result shouldBe 15.0
    }

    /** Richiesta scrittura cliente 1 sull'oggetto 3 - Cliente ottiene il permesso di scrittura al ts = 3 */
    whenReady(replicaRef ?  Write1Message(1, 3, 3, _ + 10)){
      case result: Write1OKMessage => result shouldBe Write1OKMessage(GrantTS(1, 3, 3, 3, 0.5, 1), Certificate(List(grantTS13, grantTS14, grantTS15, grantTS16)))
    }
    /**Vecchia Richiesta scrittura cliente 2  sull'oggetto 3 */
    whenReady(replicaRef ?  Write1Message(2, 3, 1, _ + 10)){
      result => result shouldBe Some("Old_Request")
    }
    /** Richiesta scrittura cliente 2 sull'oggetto 3 - Cliente non ottiene il permesso di scrittura al ts = 3 */
    whenReady(replicaRef ?  Write1Message(2, 3, 7, _ + 10)){
      case result: Write1RefusedMessage => result shouldBe Write1RefusedMessage(GrantTS(1, 3, 3, 3, 0.5, 1), 1, 3, 3, Certificate(List(grantTS13, grantTS14, grantTS15, grantTS16)))
    }
    /** Scrittura sull'oggetto 3 del cliente 1 che ha ottenuto il permesso al ts 3 */
    val writeC133 = Certificate(List(GrantTS(1, 3, 3, 3, 0.5, 1), GrantTS(1, 3, 3, 3, 0.5, 2), GrantTS(1, 3, 3, 3, 0.5, 3), GrantTS(1, 3, 3, 3, 0.5, 4)))
    whenReady(replicaRef ?  Write2Message(writeC133)) {
      case result: Write2AnsMessage => result shouldBe Write2AnsMessage(25, writeC133, 1)
    }
    //richiesta precedente
    whenReady(replicaRef ?  Write2Message(writeC133)) {
      case result: Write2AnsMessage => result shouldBe Write2AnsMessage(25, writeC133, 1)
    }
    /**Lettura oggetto 3 da parte del cliente 1 */
    whenReady(replicaRef ? ReadMessage(1, 3)){
      result => result.asInstanceOf[ReadAnsMessage].result shouldBe 25.0
    }
  }

}
