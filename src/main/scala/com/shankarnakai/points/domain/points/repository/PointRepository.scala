package com.shankarnakai.points.domain.points.repository

import cats.Monad
import cats.data.{EitherT, OptionT}
import com.shankarnakai.points.domain.points.exceptions.PointException
import com.shankarnakai.points.domain.points.model.Point

trait PointRepository[F[_]] {
  def getById(id: Long): OptionT[F, Point]
  def getByConsumed(userId: Long, consumed: Boolean, payer: Option[String] = None): F[List[Point]]
  def getByUser(userId: Long): F[List[Point]]
  def create(point: Point): F[Point]
  def update(point: Point)(implicit M: Monad[F]): EitherT[F, PointException, Unit]
}
