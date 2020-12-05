package com.shankarnakai.points.services.point

import cats.implicits._
import cats.data.EitherT
import cats.{Applicative, Monad}
import com.shankarnakai.points.domain.points.exceptions.{
  IllegalPointArgumentException,
  NegativeCreditPointException,
  PointException,
}
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
      _ <- userExist(userId).leftMap((err: Exception) =>
        IllegalPointArgumentException(point, err.getMessage()),
      )
      points <- EitherT.liftF(
        pointRepository.getByConsumed(userId, consumed = false, Some(point.payer)),
      )
    } yield points.filter(_.payer == point.payer)

    availablePoints.flatMap { points =>
      val balanced = balancePoints.exec(points :+ point)
      val total = balanced.map(_.point).sum

      if (total < 0) EitherT.leftT(NegativeCreditPointException(userId, total))
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

  private def userExist(id: Long): EitherT[F, Exception, User] =
    userRepository.getById(id).toRight(new Exception(s"User #$id not found"))
}

object AddPointsToUser {
  def apply[F[_]: Applicative](
      userRepository: UserRepository[F],
      pointRepository: PointRepository[F],
      balancePoints: BalancePoints,
  ): AddPointsToUser[F] =
    new AddPointsToUser(userRepository, pointRepository, balancePoints)
}
