package client

import client.proxy.ProxyClient

object TryClient extends App {
  val proxy: ProxyClient = ProxyClient()

  println("Risultato Write: " + proxy.write(_ + 2, 123))

  println("Risultato Read: " + proxy.read(123))



}
