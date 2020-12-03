package com.shankarnakai.points.infrastructure.endpoints.user

import cats.data.EitherT
import cats.Monad
import com.shankarnakai.points.domain.user.repository.UserRepository
import com.shankarnakai.points.services.user.{
  CreateUser,
  GetUserById,
  ListUsers,
  UpdateUser,
  UserUseCases,
}
import org.http4s.dsl.Http4sDsl
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import io.circe.generic.auto._
import org.http4s._
import org.http4s.implicits._
import org.http4s.circe._
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.server.Router
import org.scalatest.flatspec.AnyFlatSpec
import cats.effect._
import com.shankarnakai.points.domain.user.exceptions.{
  InvalidUserException,
  LoginAlreadyExistsError,
  UserNotFoundException,
}
import com.shankarnakai.points.domain.user.model.User
import com.shankarnakai.points.infrastructure.endpoints.users.UserEndpoint
import org.http4s.HttpApp

class CreateUserIO(userRepository: UserRepository[IO]) extends CreateUser[IO](userRepository)
class GetUserByIdIO(userRepository: UserRepository[IO]) extends GetUserById[IO](userRepository)
class ListUsersIO(userRepository: UserRepository[IO]) extends ListUsers[IO](userRepository)
class UpdateUserIO(userRepository: UserRepository[IO]) extends UpdateUser[IO](userRepository)

class UserEndpointSpec
    extends AnyFlatSpec
    with Http4sDsl[IO]
    with Http4sClientDsl[IO]
    with MockFactory {

  implicit val userEnc: EntityEncoder[IO, User] = jsonEncoderOf
  implicit val userDec: EntityDecoder[IO, User] = jsonOf

  implicit val userListEnc: EntityEncoder[IO, List[User]] = jsonEncoderOf
  implicit val userListDec: EntityDecoder[IO, List[User]] = jsonOf

  "POST /Users" should "return 200 if valid" in {
    val (routes, userMock) = getTestResources
    val userReceived = User(None, "my.login")
    val userSaved = User(Some(1), "my.login")

    (userMock.createUser
      .exec(_: User)(_: Monad[IO]))
      .expects(userReceived, *)
      .returning(EitherT.rightT(userSaved))

    val data = """{"login": "my.login"}"""

    (for {
      req <- POST(data, uri"/users")
      response <- routes.run(req)
    } yield response.status shouldEqual Ok).unsafeRunSync()
  }

  it should "return 409 when duplicate user login" in {
    val (routes, userMock) = getTestResources
    val userReceived = User(None, "my.login")

    (userMock.createUser
      .exec(_: User)(_: Monad[IO]))
      .expects(userReceived, *)
      .returning(EitherT.leftT(LoginAlreadyExistsError(User(id = None, login = "my.login"))))

    val data = """{"login": "my.login"}"""

    (for {
      req <- POST(data, uri"/users")
      response <- routes.run(req)
    } yield response.status shouldEqual Conflict).unsafeRunSync()
  }

  "PUT /Users" should "return 200 if valid" in {
    val (routes, userMock) = getTestResources
    val userReceived = User(Some(1), "my.login.update")

    (userMock.createUser
      .exec(_: User)(_: Monad[IO]))
      .expects(userReceived, *)
      .returning(EitherT.rightT(userReceived))

    val data = """{"id": 1, "login": "my.login.update"}"""

    (for {
      req <- POST(data, uri"/users")
      response <- routes.run(req)
    } yield response.status shouldEqual Ok).unsafeRunSync()
  }

  it should "return 409" in {
    val (routes, userMock) = getTestResources
    val userReceived = User(Some(1), "my.login.update")

    (userMock.updateUser
      .exec(_: User)(_: Monad[IO]))
      .expects(userReceived, *)
      .returning(EitherT.leftT(LoginAlreadyExistsError(userReceived)))

    val data = """{"id": 1, "login": "my.login.update"}"""

    (for {
      req <- PUT(data, uri"/users")
      response <- routes.run(req)
    } yield response.status shouldEqual Conflict).unsafeRunSync()
  }

  it should "return 404" in {
    val (routes, userMock) = getTestResources
    val userReceived = User(Some(1), "my.login.update")

    (userMock.updateUser
      .exec(_: User)(_: Monad[IO]))
      .expects(userReceived, *)
      .returning(EitherT.leftT(UserNotFoundException("User with id 1")))

    val data = """{"id": 1, "login": "my.login.update"}"""

    (for {
      req <- PUT(data, uri"/users")
      response <- routes.run(req)
    } yield response.status shouldEqual NotFound).unsafeRunSync()
  }

  it should "return 400" in {
    val (routes, userMock) = getTestResources
    val userReceived = User(Some(1), "my.login.update")

    (userMock.updateUser
      .exec(_: User)(_: Monad[IO]))
      .expects(userReceived, *)
      .returning(EitherT.leftT(InvalidUserException("Invalid user")))

    val data = """{"id": 1, "login": "my.login.update"}"""

    (for {
      req <- PUT(data, uri"/users")
      response <- routes.run(req)
    } yield response.status shouldEqual BadRequest).unsafeRunSync()
  }

  "GET /Users" should "return 200 with empty list" in {
    val (routes, userMock) = getTestResources
    (userMock.listUsers.exec _)
      .expects(None, None)
      .returning(IO(List.empty[User]))

    (for {
      req <- GET(uri"/users")
      response <- routes.run(req)
      listUsers <- response.as[List[User]]
    } yield {
      response.status shouldEqual Ok
      listUsers shouldEqual List.empty[User]
    }).unsafeRunSync()
  }

  def getTestResources: (HttpApp[IO], UserUseCases[IO]) = {
    val userUseCasesMock: UserUseCases[IO] = new UserUseCases[IO] {
      override val createUser: CreateUser[IO] = mock[CreateUserIO]
      override val getUserById: GetUserById[IO] = mock[GetUserByIdIO]
      override val listUsers: ListUsers[IO] = mock[ListUsersIO]
      override val updateUser: UpdateUser[IO] = mock[UpdateUserIO]
    }

    val endpoints = UserEndpoint.endpoints[IO](userUseCasesMock)
    val userRoutes = Router(("/users", endpoints)).orNotFound
    (userRoutes, userUseCasesMock)
  }
}
