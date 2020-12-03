package com.shankarnakai.points.services.user

import cats.Monad
import cats.data.EitherT
import com.shankarnakai.points.domain.user.exceptions.UserNotFoundException
import com.shankarnakai.points.domain.user.model.User
import com.shankarnakai.points.domain.user.repository.UserRepository

class GetUserById[F[_]](val userRepository: UserRepository[F]) {
  def exec(id: Long)(implicit M: Monad[F]): EitherT[F, UserNotFoundException, User] =
    userRepository.getById(id).toRight(UserNotFoundException(s"Not found user with id ${id}"))
}

object GetUserById {
  def apply[F[_]](userRepository: UserRepository[F]): GetUserById[F] =
    new GetUserById(userRepository)
}
