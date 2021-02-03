package client

import client.proxy.ProxyClient

object TryClient extends App {
  val proxy: ProxyClient = ProxyClient()

  println("Risultato Write: " + proxy.write[Int](_ + 2, 123))

  println("RisultatoRead: " + proxy.read[Int](123))
}
