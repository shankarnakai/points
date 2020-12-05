package com.shankarnakai.points.domain.points.model

import java.time.LocalDateTime

case class Point(
    id: Option[Long],
    userId: Long,
    payer: String,
    point: Int,
    transactionDate: LocalDateTime,
    consumed: Boolean,
)

object Point {
  def apply(x: Int): Point =
    Point(None, 0, "", x, LocalDateTime.now(), consumed = false)

  def apply(userId: Long, payer: String, point: Int): Point =
    Point(None, userId, payer, point, LocalDateTime.now(), consumed = false)

  def apply(userId: Long, payer: String, point: Int, transactionDate: LocalDateTime): Point =
    Point(None, userId, payer, point, transactionDate, consumed = false)
}
