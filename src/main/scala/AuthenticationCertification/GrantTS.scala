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
                   timeStamp: Int, viewStemp: Double,
                   replicaID: Int) {
   //TODO rivedere
   def ==(other: GrantTS): Boolean =
      areTSAndVSEqual(other) &&
        areAllEqualExceptTSAndVS(other)

   def areTSAndVSEqual(other: GrantTS): Boolean =
      other.timeStamp == timeStamp &&
      other.viewStemp == viewStemp

   //TODO cambiare nome
   def areAllEqualExceptTSAndVS(other: GrantTS): Boolean =
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

object GrantTS {
   def apply(clientID: Int, objectID: Int,
             numberOperation: Int,
             timeStamp: Int, viewStemp: Double,
             replicaID: Int):GrantTS =
      GrantTS(clientID, objectID, numberOperation, clientID.hashCode() + objectID.hashCode() + numberOperation.hashCode(), timeStamp, viewStemp, replicaID)
}