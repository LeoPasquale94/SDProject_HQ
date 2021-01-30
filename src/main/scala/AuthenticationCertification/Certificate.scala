package AuthenticationCertification

case class Certificate[T] (items: List[T]){
 def == (other: Certificate [T]): Boolean = items.size == other.items.size && items.indices.forall(x => other.items(x) == items(x))

 def + (el: T): Certificate[T] = Certificate(items :+ el)

 def compare (other: T, operator: (T, T) => Boolean): Boolean =  operator(other, items.head)
}



