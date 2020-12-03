package com.shankarnakai.points.services.user

import cats.effect.IO
import com.shankarnakai.points.domain.user.exceptions.UserNotFoundException
import com.shankarnakai.points.domain.user.model.User
import com.shankarnakai.points.infrastructure.repository.inmemory.UserRepositoryInMemory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.collection.concurrent.TrieMap

class GetUserByIdSpec extends AnyFlatSpec with Matchers {
  def testResource: (GetUserById[IO], TrieMap[Long, User]) = {
    val cache = new TrieMap[Long, User]()
    val repo = UserRepositoryInMemory[IO](
      cache,
      () => 1, //Id generator is just important to create new users
    )

    (GetUserById[IO](repo), cache)
  }

  "GetUserById User" should "return user with a existent id" in {
    val (useCase, cache) = testResource
    val userId = 1L
    val user = User(Some(userId), "my_login")
    cache.update(userId, user)

    val program = useCase.exec(userId).value
    program.unsafeRunSync() match {
        case Right(usr) => {
          usr.id.get shouldBe 1L
          usr.login shouldBe "my_login"
        }
        case Left(err) => fail(s"Unexpected error: ${err}")
    }
  }

  it should "return UserNotFoundException with a user id that don't exist" in {
    val (useCase, cache) = testResource
    cache.update(1L, User(Some(1L), "user_1"))

    val userIdThatDoNotExist = 3L

    val program = useCase.exec(userIdThatDoNotExist).value

    program.unsafeRunSync() match {
      case Left(UserNotFoundException(_)) => succeed
      case Left(err) => fail(s"Unexpected error message ${err}")
      case Right(_) => fail("Should fail with already existent user")
    }
  }
}
