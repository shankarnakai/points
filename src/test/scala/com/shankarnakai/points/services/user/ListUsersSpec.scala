package com.shankarnakai.points.services.user

import cats.effect.IO
import com.shankarnakai.points.domain.user.model.User
import com.shankarnakai.points.infrastructure.repository.inmemory.UserRepositoryInMemory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.collection.concurrent.TrieMap

class ListUsersSpec extends AnyFlatSpec with Matchers {
  def testResource: (ListUsers[IO], TrieMap[Long, User]) = {
    val cache = new TrieMap[Long, User]()
    val repo = UserRepositoryInMemory[IO](
      cache,
      () => 1, //Id generator is just important to create new users
    )

    (ListUsers[IO](repo), cache)
  }

  "ListUser" should "return a user's list" in {
    val (useCase, cache) = testResource
    for(id <- 1L to 5L) {
      cache.update(id, User(Some(id), s"user_$id"))
    }

    val pageSize = Some(5)
    val offset = Some(0)

    val program: IO[List[User]] = useCase.exec(pageSize, offset)
    val result = program.unsafeRunSync()
    result.size shouldBe  5
  }

  it should "return a list in the range specified in the page size and offset" in {
    val (useCase, cache) = testResource
    val letters = ('A' to 'Y').toList
    for(id <- 1 to 10) {
      val user = User(Some(id.toLong), s"user_${letters(id - 1)}")
      cache.update(id.toLong, user)
    }

    val pageSize = Some(5)
    val offset = Some(5)

    val program: IO[List[User]] = useCase.exec(pageSize, offset)
    val result = program.unsafeRunSync()
    result.size shouldBe  5
    result.head shouldBe  User(Some(6), "user_F")
  }
}
