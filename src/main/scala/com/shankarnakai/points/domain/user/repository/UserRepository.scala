package com.shankarnakai.points.domain.user.repository

import cats.Monad
import cats.data.{EitherT, OptionT}
import com.shankarnakai.points.domain.user.exceptions.UserException
import com.shankarnakai.points.domain.user.model.User

trait UserRepository[F[_]] {
  def getById(id: Long): OptionT[F, User]
  def getByLogin(name: String): OptionT[F, User]
  def getAll(pageSize: Int, offset: Int): F[List[User]]
  def create(user: User): F[User]
  def update(user: User)(implicit M: Monad[F]): EitherT[F, UserException, Unit]
}
