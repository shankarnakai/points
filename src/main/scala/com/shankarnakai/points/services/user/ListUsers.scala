package com.shankarnakai.points.services.user

import com.shankarnakai.points.domain.user.model.User
import com.shankarnakai.points.domain.user.repository.UserRepository

class ListUsers[F[_]](val userRepository: UserRepository[F]) {
  val DEFAULT_PAGE_SIZE: Int = 10
  val DEFAULT_OFFSET: Int = 0

  def exec(pageSize: Option[Int], offset: Option[Int]): F[List[User]] =
    userRepository.getAll(pageSize.getOrElse(DEFAULT_PAGE_SIZE), offset.getOrElse(DEFAULT_OFFSET))
}

object ListUsers {
  def apply[F[_]](userRepository: UserRepository[F]) : ListUsers[F] =
    new ListUsers(userRepository)
}
