package com.shankarnakai.points.services.point

import cats.Applicative
import com.shankarnakai.points.domain.points.repository.PointRepository
import com.shankarnakai.points.domain.user.repository.UserRepository

trait PointUseCases[F[_]] {
  val addPointsToUser: AddPointsToUser[F]
  val deductPointsFromUser: DeductPointsFromUser[F]
  val getBalanceFromUser: GetBalanceFromUser[F]
}

case class PointUseCasesLive[F[_]: Applicative](
    userRepository: UserRepository[F],
    pointRepository: PointRepository[F],
    balancePointsUseCase: BalancePoints,
) extends PointUseCases[F] {
  val addPointsToUser: AddPointsToUser[F] =
    AddPointsToUser(userRepository, pointRepository, balancePointsUseCase)

  val deductPointsFromUser: DeductPointsFromUser[F] =
    DeductPointsFromUser(userRepository, pointRepository, balancePointsUseCase)

  val getBalanceFromUser: GetBalanceFromUser[F] =
    GetBalanceFromUser(userRepository, pointRepository)
}
