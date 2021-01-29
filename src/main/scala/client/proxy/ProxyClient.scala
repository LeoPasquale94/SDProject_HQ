package client.proxy

trait ProxyClient {
  def write[T](op: T => T, oid: String): T
  def read[T](oid: String): T
}

object ProxyClient{

  private val instance: ProxyClientActor = new ProxyClientActor()

  def apply(): ProxyClient = instance

  private class ProxyClientActor extends ProxyClient(){
    //TODO inizializzare attore

    //TODO inviare messaggio all'attore
    override def write[T](op: T => T, oid: String): T = {
      ???
    }
    //TODO inviare il messaggio all'attore
    override def read[T](oid: String): T = ???
  }
}