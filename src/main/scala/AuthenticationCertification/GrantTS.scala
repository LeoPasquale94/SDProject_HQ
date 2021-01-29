package AuthenticationCertification

case class GrantTS(clientID: String, objectID: String, numberOperation: Int, hashOperation: Int, timeStamp: Double, viewStemp: Double, replicaID: String) {
   def ==(other: GrantTS): Boolean =
      areTSAndVSEqual(other) &&
      areEqualToLessTSAndVS(other)

   def areTSAndVSEqual(other: GrantTS): Boolean =
      other.timeStamp == timeStamp &&
      other.viewStemp == viewStemp

   def areEqualToLessTSAndVS(other: GrantTS): Boolean =
      other.clientID == clientID &&
      other.objectID == objectID &&
      other.numberOperation == numberOperation &&
      other.hashOperation == hashOperation

   def areReplicaIDEqual(other: GrantTS): Boolean =
      other.replicaID == replicaID

   def areTSOrVSNotEqual(other: GrantTS): Boolean =
      other.timeStamp != timeStamp ||
      other.viewStemp != viewStemp

   def >(other: GrantTS): Boolean =
      other.timeStamp < timeStamp ||
      other.viewStemp < viewStemp
}