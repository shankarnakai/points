package com.shankarnakai.points.services.point

import cats.data.{EitherT, OptionT}
import cats.{Applicative, Monad}
import com.shankarnakai.points.domain.points.exceptions.{PointException, UserIdForPointFoundException}
import com.shankarnakai.points.domain.points.model.Point
import com.shankarnakai.points.domain.points.repository.PointRepository
import com.shankarnakai.points.domain.user.model.User
import com.shankarnakai.points.domain.user.repository.UserRepository

class GetBalanceFromUser[F[_]: Applicative](
    val userRepository: UserRepository[F],
    val pointRepository: PointRepository[F],
) {
  def exec(userId: Long)(implicit M: Monad[F]): EitherT[F, PointException, Map[String, Int]] = {
    val points: EitherT[F, PointException, List[Point]] = for {
      _ <- userExist(userId).toRight(UserIdForPointFoundException(userId, None))
      points <- EitherT.liftF(
        pointRepository.getByUser(userId),
      )
    } yield points

    points.map { points =>
      points.groupBy(_.payer).map((item) => {
        val total = item._2.map(_.point).sum
        (item._1, total)
      })
    }
  }

  private def userExist(id: Long): OptionT[F, User] = userRepository.getById(id)
}

object GetBalanceFromUser {
  def apply[F[_]: Applicative](
      userRepository: UserRepository[F],
      pointRepository: PointRepository[F],
  ): GetBalanceFromUser[F] =
    new GetBalanceFromUser(userRepository, pointRepository)
}
