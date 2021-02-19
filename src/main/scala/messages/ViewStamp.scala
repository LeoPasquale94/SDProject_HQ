package messages

case class ViewStamp(viewNumber: Int, sequenceNumber: Int) {
  def <(other: ViewStamp): Boolean =
    other.viewNumber > viewNumber && other.sequenceNumber > sequenceNumber
}
