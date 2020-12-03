package com.shankarnakai.points.infrastructure.repository.inmemory

import cats.Applicative
import com.shankarnakai.points.domain.points.model.Point
import com.shankarnakai.points.domain.points.repository.PointRepository

import scala.collection.concurrent.TrieMap

class PointRepositoryInMemory[F[_]: Applicative](
    private val cache: TrieMap[Long, Point],
    private val idGenerator: () => Long,
) extends PointRepository[F] {
  override def getById(id: Long): F[Option[Point]] = ???

  override def getByOldest(userId: Long): F[List[Point]] = ???

  override def create(point: Point): F[Point] = ???
}
