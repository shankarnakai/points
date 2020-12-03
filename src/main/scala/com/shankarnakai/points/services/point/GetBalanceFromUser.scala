package com.shankarnakai.points.services.point

import com.shankarnakai.points.domain.points.model.Point
import com.shankarnakai.points.domain.points.repository.PointRepository
import com.shankarnakai.points.domain.user.repository.UserRepository

class GetBalanceFromUser[F[_]](val userRepository: UserRepository[F], val pointRepository: PointRepository[F]) {
  def exec(userId: Long): List[Point] = ???
}

object GetBalanceFromUser {
  def apply[F[_]](userRepository: UserRepository[F], pointRepository: PointRepository[F]) : GetBalanceFromUser[F] =
    new GetBalanceFromUser(userRepository, pointRepository)
}
