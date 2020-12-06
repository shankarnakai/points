package com.shankarnakai.points.services.point

import cats.implicits._
import cats.data.{EitherT, OptionT}
import cats.{Applicative, Monad}
import com.shankarnakai.points.domain.points.exceptions.{NegativeCreditPointException, PointException, UserIdForPointFoundException}
import com.shankarnakai.points.domain.points.model.Point
import com.shankarnakai.points.domain.points.repository.PointRepository
import com.shankarnakai.points.domain.user.model.User
import com.shankarnakai.points.domain.user.repository.UserRepository

class AddPointsToUser[F[_]: Applicative](
    val userRepository: UserRepository[F],
    val pointRepository: PointRepository[F],
    val balancePoints: BalancePoints,
) {
  def exec(userId: Long, point: Point)(implicit M: Monad[F]): EitherT[F, PointException, Unit] = {
    val availablePoints: EitherT[F, PointException, List[Point]] = for {
      _ <- userExist(userId).toRight(UserIdForPointFoundException(userId, Some(point)))
      points <- EitherT.liftF(
        pointRepository.getByConsumed(userId, consumed = false, Some(point.payer)),
      )
    } yield points

    availablePoints.flatMap { points =>
      val balanced = balancePoints.exec(points :+ point.copy(createBy = userId))
      val debit = balanced.filter(_.point < 0).map(_.point).sum
      val credit = balanced.filter(_.point > 0).map(_.point).sum

      if (0 > credit + debit ) EitherT.leftT(NegativeCreditPointException(userId, point, credit))
      else {
        balanced.filter(_.id.isEmpty).map(p => pointRepository.create(p))
        balanced
          .filter(_.id.nonEmpty)
          .map { (oldPoint: Point) =>
            pointRepository.update(oldPoint)
          }
          .sequence
          .map(a => a.headOption.getOrElse(()))
      }
    }
  }

  private def userExist(id: Long): OptionT[F, User] = userRepository.getById(id)
}

object AddPointsToUser {
  def apply[F[_]: Applicative](
      userRepository: UserRepository[F],
      pointRepository: PointRepository[F],
      balancePoints: BalancePoints,
  ): AddPointsToUser[F] =
    new AddPointsToUser(userRepository, pointRepository, balancePoints)
}
