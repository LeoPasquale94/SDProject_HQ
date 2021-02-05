object Prova extends App {
  case class H[T](a:Int, b:T)

  val msg1 = H(5, "Ciao")
  val msg2 = H(9, 0.5)

}
