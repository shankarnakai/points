package com.shankarnakai.points.services.user

import cats.{Applicative, Monad}
import cats.data.EitherT
import com.shankarnakai.points.domain.user.exceptions.{LoginAlreadyExistsError, UserException}
import com.shankarnakai.points.domain.user.model.User
import com.shankarnakai.points.domain.user.repository.UserRepository

class UpdateUser[F[_] : Applicative](val userRepository: UserRepository[F]) {
  def exec(user: User)(implicit M: Monad[F]): EitherT[F, UserException, Unit] =
    for {
      _ <- alreadyExist(user)
      _ <- userRepository.update(user)
    } yield ()

  def alreadyExist(user: User): EitherT[F, LoginAlreadyExistsError, Unit] =
    userRepository
      .getByLogin(user.login)
      .filterNot(u => u.id.equals(user.id))
      .map(LoginAlreadyExistsError)
      .toLeft(())
}

object UpdateUser {
  def apply[F[_]: Applicative](userRepository: UserRepository[F]) : UpdateUser[F] =
    new UpdateUser(userRepository)
}
