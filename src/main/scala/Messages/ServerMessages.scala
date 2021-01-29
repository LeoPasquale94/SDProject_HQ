package Messages

import AuthenticationCertification.{GrantTS, Certificate}

case class Write1OKMessage(grantTS: GrantTS, currentC: Certificate[GrantTS]) {
  def ==(other: Write1OKMessage): Boolean = other.grantTS == grantTS && other.currentC == currentC

  def isThereConflict(other: Write1OKMessage): Boolean = other.grantTS.areTSAndVSEqual(grantTS) && !other.grantTS.areEqualToLessTSAndVS(grantTS)
}
case class Write1RefusedMessage(grantTS: GrantTS, clientID: String, objectID: String, numberOperation: Int, currentC: Certificate[GrantTS])

case class Write2AnsMessage(result: Any, currentC: Certificate[GrantTS], replicaID: String)

case class ReadAnsMessage(result: Any, nonce: Double= Math.random(), currentC: Certificate[GrantTS], replicaID: String)
