package com.shankarnakai.points.infrastructure.endpoints.users

import cats.effect.Sync
import cats.syntax.all._
import com.shankarnakai.points.domain.user.exceptions.{
  InvalidUserException,
  LoginAlreadyExistsError,
  UserNotFoundException,
}
import com.shankarnakai.points.domain.user.model.User
import com.shankarnakai.points.infrastructure.endpoints.Pagination.{
  OptionalOffsetMatcher,
  OptionalPageSizeMatcher,
}
import com.shankarnakai.points.services.user.{
  CreateUser,
  GetUserById,
  ListUsers,
  UpdateUser,
  UserUseCases,
}
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, HttpRoutes}

class UserEndpoint[F[_]: Sync] extends Http4sDsl[F] {
  // import Pagination._

  /* Jsonization of our User type */
  implicit val userDecoder: EntityDecoder[F, User] = jsonOf

  private def createEndpoint(
      createUser: CreateUser[F],
  ): HttpRoutes[F] =
    HttpRoutes.of[F] { case req @ POST -> Root =>
      val action = for {
        user <- req.as[User]
        result <- createUser.exec(user).value
      } yield result

      action.flatMap {
        case Right(saved) => Ok(saved.asJson)
        case Left(LoginAlreadyExistsError(user)) =>
          Conflict(s"The user with login ${user.login} already exists")
      }
    }

  private def updateEndpoint(updateUser: UpdateUser[F]) = HttpRoutes.of[F] {
    case req @ PUT -> Root =>
      val action = for {
        user <- req.as[User]
        result <- updateUser.exec(user).value
      } yield result

      action.flatMap {
        case Right(saved) => Ok(saved.asJson)
        case Left(UserNotFoundException(_)) => NotFound("User not found")
        case Left(InvalidUserException(_)) => BadRequest("User is missing id or login")
        case Left(LoginAlreadyExistsError(user)) =>
          Conflict(s"The user with login ${user.login} already exists")
        case Left(_) => InternalServerError("Sorry, An internal server error happened")
      }
  }

  private def userEndpoint(getUserById: GetUserById[F]) = HttpRoutes.of[F] {
    case GET -> Root / LongVar(id) =>
      getUserById.exec(id).value.flatMap {
        case Right(found) => Ok(found.asJson)
        case Left(UserNotFoundException(_)) =>
          NotFound("The user was not found")
      }
  }

  private def listEndpoint(listUsers: ListUsers[F]) = HttpRoutes.of[F] {
    case GET -> Root :? OptionalPageSizeMatcher(pageSize) :? OptionalOffsetMatcher(
          offset,
        ) =>
      for {
        retrieved <- listUsers.exec(pageSize, offset)
        resp <- Ok(retrieved.asJson)
      } yield resp
  }

  def endpoints(service: UserUseCases[F]): HttpRoutes[F] = {
    val unAuthEndpoints =
      createEndpoint(service.createUser) <+>
        updateEndpoint(service.updateUser) <+>
        listEndpoint(service.listUsers) <+>
        userEndpoint(service.getUserById)

    unAuthEndpoints
  }
}

object UserEndpoint {
  def endpoints[F[_]: Sync](service: UserUseCases[F]): HttpRoutes[F] =
    new UserEndpoint[F].endpoints(service)
}
