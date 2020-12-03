package com.shankarnakai.points.domain.points.model

import java.util.Date

case class Point(
  id: Option[Long],
  userId: Long,
  payer: String,
  point: Int,
  transactionDate: Date,
  consumed: Boolean
)
