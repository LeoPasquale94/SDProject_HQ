package AuthenticationCertification

case class Certificate[T] (items: List[T]){
 def == (other: Certificate [T]): Boolean = items.forall(compare(_, _ == _))

 def + (el: T): Certificate[T] = Certificate(items :+ el)

 def compare (other: T, operator: (T, T) => Boolean): Boolean =  operator(other, items.head)
}



