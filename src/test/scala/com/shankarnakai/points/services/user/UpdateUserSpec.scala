package com.shankarnakai.points.services.user

import cats.effect.IO
import com.shankarnakai.points.domain.user.exceptions.{InvalidUserException, LoginAlreadyExistsError, UserNotFoundException}
import com.shankarnakai.points.domain.user.model.User
import com.shankarnakai.points.infrastructure.repository.inmemory.UserRepositoryInMemory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.collection.concurrent.TrieMap

class UpdateUserSpec extends AnyFlatSpec with Matchers {
  def testResource: (UpdateUser[IO], TrieMap[Long, User]) = {
    val cache = new TrieMap[Long, User]()
    val repo = UserRepositoryInMemory[IO](
      cache,
      () => 1, //Id generator is just important to create new users
    )

    (UpdateUser[IO](repo), cache)
  }

  "Update User" should "update the user given a valid data" in {
    val (useCase, cache) = testResource
    cache.update(1L, User(Some(1L), "old_login"))
    val user = User(Some(1L), "updated_login")

    val program = useCase.exec(user).value
    program.unsafeRunSync() match {
        case Right(_) =>
          cache.get(1L) match {
            case Some(usr) if usr.login == "updated_login" => IO(succeed)
            case _ => fail(s"Didn't find user with id 1L and 'updated_login': ${cache}")
          }
        case Left(err) => fail(s"Unexpected error: ${err}")
    }
  }

  it should "succeed if the repeated login is from the same user id" in {
    val (useCase, cache) = testResource
    cache.update(1L, User(Some(1L), "same_login"))
    val user = User(Some(1L), "same_login")

    val program = useCase.exec(user).value
    program.unsafeRunSync() match {
      case Right(_) =>
        cache.get(1L) match {
          case Some(User(Some(1L), "same_login")) => succeed
          case _ => fail(s"Didn't find user with id 1L and 'updated_login': ${cache}")
        }
      case Left(err) => fail(s"Unexpected error: ${err}")
    }
  }

  it should "return InvalidUserException with None id" in {
    val (useCase, _) = testResource
    val user = User(None, "test_login")

    val program = useCase.exec(user).value

    program.unsafeRunSync() match {
      case Left(InvalidUserException(_)) => succeed
      case Left(err) => fail(s"Unexpected error message ${err}")
      case Right(_) => fail("Should fail with already existent user")
    }
  }

  it should "return LoginAlreadyExistsError with a login that already exist" in {
    val (useCase, cache) = testResource
    cache.update(1L, User(Some(1L), "user_A"))
    cache.update(2L, User(Some(2L), "user_B"))
    val userLoginThatAlreadyExist = User(Some(1L), "user_B")

    val program = useCase.exec(userLoginThatAlreadyExist).value

    program.unsafeRunSync() match {
      case Left(LoginAlreadyExistsError(_)) => succeed
      case Left(err) => fail(s"Unexpected error message ${err}")
      case Right(_) => fail("Should fail with already existent user")
    }
  }

  it should "return UserNotFoundException with a user id that don't exist" in {
    val (useCase, cache) = testResource
    cache.update(1L, User(Some(1L), "user_1"))
    val userThatDoNotExist = User(Some(3L), "test_login")

    val program = useCase.exec(userThatDoNotExist).value

    program.unsafeRunSync() match {
      case Left(UserNotFoundException(_)) => succeed
      case Left(err) => fail(s"Unexpected error message ${err}")
      case Right(_) => fail("Should fail with already existent user")
    }
  }
}
