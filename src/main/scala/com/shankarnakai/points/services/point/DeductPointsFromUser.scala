package com.shankarnakai.points.services.point

import cats.data.{EitherT, OptionT}
import cats.syntax.traverse._
import cats.{Applicative, Monad}
import com.shankarnakai.points.domain.points.exceptions.NegativeCreditPointException
//import com.shankarnakai.points.domain.points.exceptions.{NegativeCreditPointException, PointException, UserIdForPointFoundException}
import com.shankarnakai.points.domain.points.exceptions.{
  PointException,
  UserIdForPointFoundException,
}
import com.shankarnakai.points.domain.points.model.Point
import com.shankarnakai.points.domain.points.repository.PointRepository
import com.shankarnakai.points.domain.user.model.User
import com.shankarnakai.points.domain.user.repository.UserRepository

class DeductPointsFromUser[F[_]: Applicative](
    val userRepository: UserRepository[F],
    val pointRepository: PointRepository[F],
    val balancePoints: BalancePoints,
) {
  def exec(userId: Long, value: Int)(implicit
      M: Monad[F],
  ): EitherT[F, PointException, List[Point]] = {
    val point = Point(-value)
    val availablePoints: EitherT[F, PointException, List[Point]] = for {
      _ <- userExist(userId).toRight(UserIdForPointFoundException(userId, Some(point)))
      points <- EitherT.liftF(
        pointRepository.getByConsumed(userId, consumed = false),
      )
    } yield points

    availablePoints.flatMap { points =>
      val balanced = balancePoints.exec(points :+ point)
      val debit = balanced.filter(_.point < 0).map(_.point).sum
      val credit = balanced.filter(_.point > 0).map(_.point).sum
      val (newPoints, oldPoints) = balanced.partition(_.id.isEmpty)

      if (0 > credit + debit) EitherT.leftT(NegativeCreditPointException(userId, point, credit))
      else {
        for {
          cPoints <- createPoints(newPoints)
          uPoints <- updatePoints(oldPoints)
        } yield (cPoints ++ uPoints).filter(p => p.consumed == true && p.createBy > 0 && p.point < 0)
      }
    }
  }

  private def createPoints(list: List[Point])(implicit
      M: Monad[F],
  ): EitherT[F, PointException, List[Point]] =
   list.map(p =>  {
    val a: F[Point] = pointRepository.create(p)
     EitherT.liftF[F, PointException, Point](a)
   }).sequence

  private def updatePoints(list: List[Point])(implicit
      M: Monad[F],
  ): EitherT[F, PointException, List[Point]] =
    list.map { (oldPoint: Point) =>
      pointRepository
        .update(oldPoint)
        .map(_ => oldPoint)
    }.sequence

  private def userExist(id: Long): OptionT[F, User] = userRepository.getById(id)
}

object DeductPointsFromUser {
  def apply[F[_]: Applicative](
      userRepository: UserRepository[F],
      pointRepository: PointRepository[F],
      balancePointsUseCase: BalancePoints,
  ): DeductPointsFromUser[F] =
    new DeductPointsFromUser(userRepository, pointRepository, balancePointsUseCase)
}
