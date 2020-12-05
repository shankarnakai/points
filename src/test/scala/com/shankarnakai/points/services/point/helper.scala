package com.shankarnakai.points.services.point

import com.shankarnakai.points.domain.points.model.Point

object helper {
  def removeTransactionDate(list: List[Point]): List[(Option[Long], Int, Boolean, Long, String)] =
    list
      .sortBy(_.id.getOrElse(Long.MaxValue))
      .map(v => (v.id, v.point, v.consumed, v.userId, v.payer))
}
