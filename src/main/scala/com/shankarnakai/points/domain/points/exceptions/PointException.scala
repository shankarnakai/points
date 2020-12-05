package com.shankarnakai.points.domain.points.exceptions

import com.shankarnakai.points.domain.points.model.Point

sealed trait PointException extends Exception
final case class IllegalPointArgumentException(point: Point, message: String) extends PointException
final case class PointNotFoundException(pointId: Long) extends PointException
final case class NegativeCreditPointException(userId: Long, value: Int) extends PointException

