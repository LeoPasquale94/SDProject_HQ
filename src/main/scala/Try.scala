object Try extends App{
 val map = Map( "A" -> 4, "B" -> 10)

  val map1 = map + ("A" -> 100)

  println(map1)
  println(map1("C"))


}
