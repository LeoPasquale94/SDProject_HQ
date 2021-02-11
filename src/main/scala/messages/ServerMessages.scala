package messages

import AuthenticationCertification.{GrantTS, Certificate}

case class Write1OKMessage(grantTS: GrantTS, currentC: Certificate[GrantTS]) {
  def ==(other: Write1OKMessage): Boolean = other.grantTS == grantTS && other.currentC == currentC

  def isThereConflict(other: Write1OKMessage): Boolean = other.grantTS.areTSAndVSEqual(grantTS) && !other.grantTS.grantWithoutStamps(grantTS)
}

case class Write1RefusedMessage(grantTS: GrantTS, clientID: Int, objectID: Int, numberOperation: Int, currentC: Certificate[GrantTS])

case class Write2AnsMessage(result: Float, currentC: Certificate[GrantTS], replicaID: Int)

case class ReadAnsMessage(result: Float, currentC: Certificate[GrantTS], replicaID: Int, nonce: Double= Math.random())

case class ObjectNotFoundMessage()
