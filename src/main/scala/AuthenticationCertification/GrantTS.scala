package AuthenticationCertification

/**
 *
 * @param clientID
 * @param objectID
 * @param numberOperation
 * @param hashOperation
 * @param timeStamp
 * @param viewStemp
 * @param replicaID
 */
case class GrantTS(clientID: Int, objectID: Int,
                   numberOperation: Int, hashOperation: Int,
                   timeStamp: Double, viewStemp: Double,
                   replicaID: Int) {
   //TODO rivedere
   def ==(other: GrantTS): Boolean =
      areTSAndVSEqual(other) &&
        grantWithoutStamps(other)

   def areTSAndVSEqual(other: GrantTS): Boolean =
      other.timeStamp == timeStamp &&
      other.viewStemp == viewStemp

   //TODO cambiare nome
   def grantWithoutStamps(other: GrantTS): Boolean =
      other.clientID == clientID &&
      other.objectID == objectID &&
      other.numberOperation == numberOperation &&
      other.hashOperation == hashOperation

   def isSameReplica(other: GrantTS): Boolean =
      other.replicaID == replicaID

   def areTSOrVSNotEqual(other: GrantTS): Boolean =
      other.timeStamp != timeStamp ||
      other.viewStemp != viewStemp

   def >(other: GrantTS): Boolean =
      other.timeStamp < timeStamp ||
      other.viewStemp < viewStemp
}