package Server

import AuthenticationCertification.{Certificate, GrantTS}
import messages.{Write1Message, Write1OKMessage}

object ObjectInfInitializer {

  /* Informazioni relative all'oggetto 1 */

  //ToDo consensi di 4 repliche
  val grantTS1 = GrantTS(1, 1, 1, 1, 1, 0.5, 1)
  val grantTS2 = GrantTS(1, 1, 1, 1, 1, 0.5, 2)
  val grantTS3 = GrantTS(1, 1, 1, 1, 1, 0.5, 3)
  val grantTS4 = GrantTS(1, 1, 1, 1, 1, 0.5, 4)

  //ToDo certificato
  val certificate1 = Certificate(List(grantTS1, grantTS2, grantTS3, grantTS4))
  //ToDo Informazioni Cliente autorizzato a scrivere


  //ToDo informazioni oggetto 1
  val objInf1 = ObjectInformation(certificate1, Option.empty, Option.empty, Option.empty, 5)

  //ToDo Object 1
  val obj1 = Object(objInf1, 5)

  /* Informazioni relative all'oggetto 2 */

  //ToDo Richiesta di scrittura sull'oggetto 2
  val wrt1req = Write1Message(2, 2, 6, _ + 5)


  //ToDo consensi di 4 repliche:
  val grantTS5 = GrantTS(2, 2, 5, 1, 1, 0.5, 1)
  val grantTS6 = GrantTS(2, 2, 5, 1, 1, 0.5, 2)
  val grantTS7 = GrantTS(2, 2, 5, 1, 1, 0.5, 3)
  val grantTS8 = GrantTS(2, 2, 5, 1, 1, 0.5, 4)

  //ToDo certificato corrente
  val certificate2 = Certificate(List(grantTS5, grantTS6, grantTS7, grantTS8))

  //ToDo un cliente autorizzato a scivere era ClientId = 2 con nop = 5
  val clientAutWrt2 = ClientAuthorizedToWriteInf(5, Write1Message(2, 2, 5, _ + 5), 7, certificate2)
  val oldOps = OldOps(Map(2 -> clientAutWrt2))

  //ToDo consenso della replica a scrivere sull'oggetto 2 al cliente 2 al timeStemp 2
  val grantTSObj2 = GrantTS(2, 2, 6, 6, 2, 0.5, 1)

  //ToDo Risposta della replica alla richiesta precedente
  val resWrt1req = Write1OKMessage(grantTSObj2, certificate2)

  //ToDo Ops
  val ops = Ops(Option(wrt1req), Option(resWrt1req), List(), Option.empty)

  //ToDo informazioni oggetto 2
  val objInf2 = ObjectInformation(certificate2, Option.empty, Option(ops), Option(oldOps), 0.5)

  //ToDo Object 2
  val obj2 = Object(objInf2, 7)

  /* Informazioni relative all'oggetto 3 - GrantTS empty */

  //ToDo Richiesta di scrittura sull'oggetto 3
  val wrt2req = Write1Message(2, 3, 6, _ + 5)

  //ToDo consensi di 4 repliche:
  val grantTS9 = GrantTS(2, 3, 5, 1, 1, 0.5, 1)
  val grantTS10 = GrantTS(2, 3, 5, 1, 1, 0.5, 2)
  val grantTS11= GrantTS(2, 3, 5, 1, 1, 0.5, 3)
  val grantTS12 = GrantTS(2, 3, 5, 1, 1, 0.5, 4)

  //ToDo consensi di 4 repliche:
  val grantTS13 = GrantTS(2, 3, 5, 1, 2, 0.5, 1)
  val grantTS14 = GrantTS(2, 3, 5, 1, 2, 0.5, 2)
  val grantTS15= GrantTS(2, 3, 5, 1, 2, 0.5, 3)
  val grantTS16 = GrantTS(2, 3, 5, 1, 2, 0.5, 4)

  //ToDo certificato corrente
  val certificate3 = Certificate(List(grantTS9, grantTS10, grantTS11, grantTS12))

  //ToDo un cliente autorizzato a scivere era ClientId = 2 con nop = 5
  val clientAutWrt3 = ClientAuthorizedToWriteInf(5, Write1Message(2, 3, 5, _ + 5), 7, certificate3)
  val oldOps1 = OldOps(Map(2 -> clientAutWrt3))

  //ToDo consenso della replica a scrivere sull'oggetto 3 al cliente 2 al timeStemp 2
  val grantTSObj3 = GrantTS(2, 2, 6, 6, 2, 0.5, 1)

  //ToDo Risposta della replica alla richiesta precedente
  val resWrt1reqObj3 = Write1OKMessage(grantTSObj2, certificate2)

  //ToDo informazioni oggetto 3
  val objInf3 = ObjectInformation(certificate3, Option.empty, Option.empty, Option.empty, 0.5)

  //ToDo Object 3
  val obj3 = Object(objInf3, 10)

  //ToDo Objects

  val objects = Objects(Map(1 -> obj1, 2 -> obj2, 3 -> obj3))


  def initObj: Objects = objects


}
