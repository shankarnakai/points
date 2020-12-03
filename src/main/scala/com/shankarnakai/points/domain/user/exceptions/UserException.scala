package com.shankarnakai.points.domain.user.exceptions

import com.shankarnakai.points.domain.user.model.User

sealed trait UserException extends Exception
final case class InvalidUserException(msg: String) extends UserException
final case class UserNotFoundException(msg: String) extends UserException
final case class LoginAlreadyExistsError(user: User) extends UserException

