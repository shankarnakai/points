package com.shankarnakai.points.infrastructure.repository.inmemory

import cats.data.{EitherT, OptionT}
import cats.implicits.{catsSyntaxApplicativeId, catsSyntaxOptionId}
import cats.{Applicative, Monad}
import com.shankarnakai.points.domain.user.exceptions.{
  InvalidUserException,
  UserException,
  UserNotFoundException,
}
import com.shankarnakai.points.domain.user.model.User
import com.shankarnakai.points.domain.user.repository.UserRepository

import scala.collection.concurrent.TrieMap

class UserRepositoryInMemory[F[_]: Applicative](
    private val cache: TrieMap[Long, User],
    private val idGenerator: () => Long,
) extends UserRepository[F] {

  override def getById(id: Long): OptionT[F, User] =
    OptionT.fromOption(cache.get(id))

  override def getByLogin(login: String): OptionT[F, User] =
    OptionT.fromOption(cache.values.find(u => u.login == login))

  override def getAll(pageSize: Int, offset: Int): F[List[User]] =
    cache.values.toList
      .sortBy(_.login)
      .slice(offset, offset + pageSize)
      .pure[F]

  override def create(user: User): F[User] = {
    val id = idGenerator()
    val toSave = user.copy(id = id.some)
    cache += (id -> toSave)
    toSave.pure[F]
  }

  override def update(user: User)(implicit M: Monad[F]): EitherT[F, UserException, Unit] =
    for {
      userId <- EitherT.fromOption(user.id, InvalidUserException(""))
      found <- getById(userId).toRight[UserException](UserNotFoundException(""))
    } yield cache.update(userId, found.copy(Some(userId), user.login))
}

object UserRepositoryInMemory {
  def apply[F[_]: Applicative](
      cache: TrieMap[Long, User],
      idGenerator: () => Long,
  ) =
    new UserRepositoryInMemory[F](
      cache,
      idGenerator,
    )
}
