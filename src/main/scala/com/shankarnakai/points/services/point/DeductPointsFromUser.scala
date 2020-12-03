package com.shankarnakai.points.services.point

import com.shankarnakai.points.domain.points.model.Point
import com.shankarnakai.points.domain.points.repository.PointRepository
import com.shankarnakai.points.domain.user.repository.UserRepository

class DeductPointsFromUser[F[_]](
    val userRepository: UserRepository[F],
    val pointRepository: PointRepository[F],
) {
  def exec(userId: Long, value: Long): F[List[Point]] = ???
  /*
    Rules:
    - I can't deduct more than what I have of credits --> Return Exception with the Max value that I can deduct
    - We want the oldest points to be spent first
    - We want no payer's points to go negative.
   */
}

object DeductPointsFromUser {
  def apply[F[_]](
      userRepository: UserRepository[F],
      pointRepository: PointRepository[F],
  ): DeductPointsFromUser[F] =
    new DeductPointsFromUser(userRepository, pointRepository)
}
