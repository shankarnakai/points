package com.shankarnakai.points.services.user

import cats.effect.IO
import com.shankarnakai.points.domain.user.exceptions.LoginAlreadyExistsError
import com.shankarnakai.points.domain.user.model.User
import com.shankarnakai.points.infrastructure.repository.inmemory.UserRepositoryInMemory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.collection.concurrent.TrieMap

class CreateUserSpec extends AnyFlatSpec with Matchers {
  def testResource: CreateUser[IO] = {
    var idGeneratorMutable: Long = 0
    val database = UserRepositoryInMemory[IO](
      new TrieMap[Long, User](),
      () => {
        idGeneratorMutable += 1
        idGeneratorMutable
      },
    )

    CreateUser[IO](database)
  }

  "Create User" should "return user with id given a valid data" in {
    val useCase = testResource
    val user = User(None, "test_login")

    val program = useCase
      .exec(user)
      .value

    program.unsafeRunSync() match {
      case Right(User(Some(1L), _)) => succeed
      case Left(err) => fail(s"Fail with ${err}")
      case _ => fail("Didn't create a user with id 1")
    }
  }

  it should "return LoginAlreadyExistsError if login it's already in the database" in {
    val useCase = testResource
    val user = User(None, "test_login")

    val program = for {
      _ <- useCase.exec(user)
      savedUser <- useCase.exec(user)
    } yield savedUser

    program.value.unsafeRunSync() match {
      case Left(LoginAlreadyExistsError(_)) => succeed
      case Left(err) => fail(s"Unexpected error message ${err}")
      case Right(_) => fail("Should fail with already existent user")
    }
  }
}
