package com.shankarnakai.points.services.user

import cats.Applicative
import com.shankarnakai.points.domain.user.repository.UserRepository

trait UserUseCases[F[_]] {
  val createUser: CreateUser[F]
  val getUserById: GetUserById[F]
  val listUsers: ListUsers[F]
  val updateUser: UpdateUser[F]
}

case class UserUseCasesLive[F[_]: Applicative](userRepository: UserRepository[F])
    extends UserUseCases[F] {
  val createUser: CreateUser[F] = CreateUser(userRepository)
  val getUserById: GetUserById[F] = GetUserById(userRepository)
  val listUsers: ListUsers[F] = ListUsers(userRepository)
  val updateUser: UpdateUser[F] = UpdateUser(userRepository)
}
