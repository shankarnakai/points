package com.shankarnakai.points.domain.points.model
import io.circe._
import io.circe.generic.semiauto._
import cats.syntax.all._
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

case class PointRequest(
    id: Option[Long],
    payer: String,
    point: Int,
    transactionDate: Option[LocalDateTime],
)

case class DeductValue(point: Int)

case class Point(
    id: Option[Long],
    userId: Long,
    payer: String,
    point: Int,
    transactionDate: LocalDateTime = LocalDateTime.now(),
    consumed: Boolean = false,
    createBy: Long = 0 //track the user that made this change,
                       // zero means that it was created by the system
) {

  def toPointRequest: PointRequest = PointRequest(id, payer, point, transactionDate.some)
}

object Point {
  val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
  implicit val dateEncoder = Encoder.encodeString.contramap[LocalDateTime](_.format(formatter))
  implicit val dateDecoder = Decoder.decodeString.emap[LocalDateTime](f = str => {
    Either.catchNonFatal(LocalDateTime.parse(str, formatter)).leftMap(_.getMessage)
  })

  implicit val PointRequestEncoder = deriveEncoder[PointRequest]
  implicit val PointRequestDecoder = deriveDecoder[PointRequest]

  def apply(x: Int): Point =
    Point(None, 0, "", x)

  def apply(userId: Long, payer: String, point: Int): Point =
    Point(None, userId, payer, point)

  def apply(userId: Long, payer: String, point: Int, transactionDate: LocalDateTime): Point =
    Point(None, userId, payer, point, transactionDate)

  def apply(userId: Long, pointRequest: PointRequest): Point =
    Point(
      None,
      userId,
      pointRequest.payer,
      pointRequest.point,
      pointRequest.transactionDate.getOrElse(LocalDateTime.now()),
    )
}
