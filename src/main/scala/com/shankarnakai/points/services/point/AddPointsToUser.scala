package com.shankarnakai.points.services.point

import com.shankarnakai.points.domain.points.model.Point
import com.shankarnakai.points.domain.points.repository.PointRepository
import com.shankarnakai.points.domain.user.repository.UserRepository

class AddPointsToUser[F[_]](val userRepository: UserRepository[F], val pointRepository: PointRepository[F]) {
  def exec(userId: Long, points: List[Point]) = ???
}

object AddPointsToUser {
  def apply[F[_]](userRepository: UserRepository[F], pointRepository: PointRepository[F]) : AddPointsToUser[F] =
    new AddPointsToUser(userRepository, pointRepository)
}
