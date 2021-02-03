package client

import client.proxy.ProxyClient

object TryClient extends App {
  val proxy: ProxyClient = ProxyClient()

  println(proxy.read[Int](123))

  println(proxy.write[Int](_ + 2, 123))

  case class ReqReadMessage[T](nOp: Int)( name: String)
}
