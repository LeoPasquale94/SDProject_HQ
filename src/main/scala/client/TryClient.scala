package client

import client.proxy.ProxyClient

object TryClient extends App {
  val proxy: ProxyClient = ProxyClient()

  println("Risultato Write: " + proxy.write[Int](_ + 2, 123))

  println("Risultato Write: " + proxy.write[String](_ + "Ciao", 321))

  println("Risultato Read: " + proxy.read[Int](123))

  println("Risultato Read: " + proxy.read[String](321))


}
