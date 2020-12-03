package com.shankarnakai.points.services.user

import cats.{Applicative, Monad}
import cats.data.EitherT
import com.shankarnakai.points.domain.user.exceptions.LoginAlreadyExistsError
import com.shankarnakai.points.domain.user.model.User
import com.shankarnakai.points.domain.user.repository.UserRepository

class CreateUser[F[_]: Applicative](val userRepository: UserRepository[F]) {
   def exec(user: User)(implicit M: Monad[F]): EitherT[F, LoginAlreadyExistsError, User] =
    for {
      _ <- alreadyExist(user)
      saved <- EitherT.liftF(userRepository.create(user))
    } yield saved

  def alreadyExist(user: User): EitherT[F, LoginAlreadyExistsError, Unit] =
    userRepository
      .getByLogin(user.login)
      .map(LoginAlreadyExistsError)
      .toLeft(())
}

object CreateUser {
  def apply[F[_]: Applicative](userRepository: UserRepository[F]): CreateUser[F] =
    new CreateUser(userRepository)
}
