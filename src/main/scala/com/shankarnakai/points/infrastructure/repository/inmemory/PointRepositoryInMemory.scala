package com.shankarnakai.points.infrastructure.repository.inmemory

import cats.{Applicative, Monad}
import cats.data.{EitherT, OptionT}
import cats.implicits.{catsSyntaxApplicativeId, catsSyntaxOptionId}
import com.shankarnakai.points.domain.points.exceptions.{
  IllegalPointArgumentException,
  PointException,
  PointNotFoundException,
}
import com.shankarnakai.points.domain.points.model.Point
import com.shankarnakai.points.domain.points.repository.PointRepository

import java.time.LocalDateTime
import scala.collection.concurrent.TrieMap

class PointRepositoryInMemory[F[_]: Applicative](
    private val cache: TrieMap[Long, Point],
    private val idGenerator: () => Long,
) extends PointRepository[F] {

  override def getById(id: Long): OptionT[F, Point] =
    OptionT.fromOption(cache.get(id))

  override def getByConsumed(
      userId: Long,
      consumed: Boolean,
      payer: Option[String] = None,
  ): F[List[Point]] = {
    cache.values.toList
      .filter(point => payer match {
        case Some(name) => point.payer == name
        case None => true
      })
      .filter(point => point.userId == userId && point.consumed == consumed)
      .sortBy(_.transactionDate)
      .pure[F]
  }

  override def getByUser(userId: Long): F[List[Point]] =
  cache.values.toList
    .filter(point => point.userId == userId)
    .sortBy(_.transactionDate)
    .pure[F]

  override def create(point: Point): F[Point] = {
    val id = idGenerator()
    val toSave = point.copy(id = id.some, transactionDate = LocalDateTime.now())
    cache += (id -> toSave)
    toSave.pure[F]
  }

  override def update(point: Point)(implicit M: Monad[F]): EitherT[F, PointException, Unit] =
    for {
      pointId <- EitherT.fromOption(
        point.id,
        IllegalPointArgumentException(point, "It is not possible to update point with a empty ID"),
      )
      saved <- getById(pointId)
        .toRight[PointException](PointNotFoundException(pointId))
        .map(_ => cache.update(pointId, point))
    } yield saved
}

object PointRepositoryInMemory {
  def apply[F[_]: Applicative](
      cache: TrieMap[Long, Point],
      idGenerator: () => Long,
  ) =
    new PointRepositoryInMemory[F](
      cache,
      idGenerator,
    )
}
