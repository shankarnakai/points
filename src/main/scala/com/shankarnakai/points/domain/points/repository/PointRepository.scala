package com.shankarnakai.points.domain.points.repository

import com.shankarnakai.points.domain.points.model.Point

trait PointRepository[F[_]] {
 def getById(id: Long): F[Option[Point]]
 def getByOldest(userId: Long): F[List[Point]]
 def create(point: Point): F[Point]
}

