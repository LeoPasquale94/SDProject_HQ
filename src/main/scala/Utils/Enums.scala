package Utils

object TypeMes extends Enumeration {
  type TypeMes = Value
  val Write1, Write2, Read, Resolve, WriteBackWrite, WriteBackRead = Value
}

object WriteOperationType extends Enumeration {
  type WriteOperationType = Value
  val Write, Update, Delete, Add = Value
}

