package com.shankarnakai.points.services.point

import cats.effect.IO
import com.shankarnakai.points.domain.points.exceptions.NegativeCreditPointException
import com.shankarnakai.points.infrastructure.repository.inmemory._
import com.shankarnakai.points.services.point.helper._
import com.shankarnakai.points.domain.points.model.Point
import com.shankarnakai.points.domain.user.model.User
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

import java.time.LocalDateTime
import scala.collection.concurrent.TrieMap

class AddPointsToUserSpec extends AnyFlatSpec with Matchers {
  private val addFn = (useCase: AddPointsToUser[IO]) =>
    (payer: String, value: Int, transactionDate: LocalDateTime) =>
      useCase.exec(1L, Point(1L, payer, value, transactionDate))

  def testResource: (TrieMap[Long, Point], AddPointsToUser[IO]) = {
    var idUserGeneratorMutable: Long = 0
    val userCache = new TrieMap[Long, User]()
    userCache.update(1, User(Some(1), "user_1"))
    val userRepo = UserRepositoryInMemory[IO](
      userCache,
      () => {
        idUserGeneratorMutable += 1
        idUserGeneratorMutable
      },
    )

    var idPointGeneratorMutable: Long = 0
    val pointCache = new TrieMap[Long, Point]()
    val pointRepo = PointRepositoryInMemory[IO](
      pointCache,
      () => {
        idPointGeneratorMutable += 1
        idPointGeneratorMutable
      },
    )

    (pointCache, AddPointsToUser[IO](userRepo, pointRepo, BalancePoints.byOldest()))
  }

  "AddPointsToUser" should "consume a single record related to the same company that it's deducting points" in {
    val (pointCache, useCase) = testResource
    val today = LocalDateTime.now()
    val yesterday = today.plusDays(-1)

    val add = addFn(useCase)

    (for {
      _ <- add("MILLER COORS", 1000, yesterday)
      _ <- add("UNILEVER", 200, yesterday)
      _ <- add("DANNON", 200, today)
      _ <- add("DANNON", -200, today) // <=== deducting points
    } yield ()).value.unsafeRunSync()

    val expected = List(
      Point(Some(1), 1L, "MILLER COORS", 1000, yesterday, consumed = false),
      Point(Some(2), 1L, "UNILEVER", 200, yesterday, consumed = false),
      Point(Some(3), 1L, "DANNON", 200, today, consumed = true),
      Point(Some(4), 1L, "DANNON", -200, today, consumed = true),
    )

    removeTransactionDate(
      pointCache.values.toList,
    ) shouldBe removeTransactionDate(expected)
  }

  "AddPointsToUser" should "consume the OLDEST record related to the same company that it's deducting points" in {
    val (pointCache, useCase) = testResource
    val today = LocalDateTime.now()
    val yesterday = today.plusDays(-1)

    val add = addFn(useCase)

    (for {
      _ <- add("DANNON", 200, yesterday)
      _ <- add("MILLER COORS", 1000, yesterday)
      _ <- add("UNILEVER", 200, yesterday)
      _ <- add("DANNON", 200, today)
      _ <- add("DANNON", -200, today) // <=== deducting points
    } yield ()).value.unsafeRunSync()

    val expected = List(
      Point(Some(1), 1L, "DANNON", 200, yesterday, consumed = true),
      Point(Some(2), 1L, "MILLER COORS", 1000, yesterday, consumed = false),
      Point(Some(3), 1L, "UNILEVER", 200, yesterday, consumed = false),
      Point(Some(4), 1L, "DANNON", 200, today, consumed = false),
      Point(Some(5), 1L, "DANNON", -200, today, consumed = true),
    )

    removeTransactionDate(
      pointCache.values.toList,
    ) shouldBe removeTransactionDate(expected)
  }

  it should "add points to user receiving positive value" in {
    val (pointCache, useCase) = testResource
    val today = LocalDateTime.now()
    val yesterday = today.plusDays(-1)

    val add = addFn(useCase)
    (for {
      _ <- add("DANNON", 300, yesterday)
      _ <- add("UNILEVER", 200, yesterday)
      _ <- add("DANNON", -200, yesterday)
      _ <- add("MILLER COORS", 10000, today)
      action <- add("DANNON", 1000, today)
    } yield action).value.unsafeRunSync()

    val expected = List(
      Point(Some(1), 1L, "DANNON", 300, yesterday, consumed = true),
      Point(Some(2), 1L, "UNILEVER", 200, yesterday, consumed = false),
      Point(Some(3), 1L, "DANNON", -200, yesterday, consumed = true),
      Point(Some(4), 1L, "DANNON", -100, today, consumed = true),
      Point(Some(5), 1L, "DANNON", 100, today, consumed = false),
      Point(Some(6), 1L, "MILLER COORS", 10000, today, consumed = false),
      Point(Some(7), 1L, "DANNON", 1000, today, consumed = false),
    )

    removeTransactionDate(
      pointCache.values.toList,
    ) shouldBe removeTransactionDate(expected)
  }

  it should "fail when attempt add negative number for user without credit" in {
    val (_, useCase) = testResource
    val today = LocalDateTime.now()
    val yesterday = today.plusDays(-1)
    val add = addFn(useCase)

    val program = (for {
      _ <- add("DANNON", -300, yesterday)
    } yield ()).value

    program.unsafeRunSync() match {
      case Left(NegativeCreditPointException(_, _)) => succeed
      case Right(_) => fail(s"Expected to fail with NegativeCreditPointException")
      case Left(err) => fail(
        s"Expected to fail with NegativeCreditPointException but failed with $err"
      )
    }
  }

  it should "fail when attempt to negative the company total points from a user still with CREDIT" in {
    val (_, useCase) = testResource
    val today = LocalDateTime.now()
    val yesterday = today.plusDays(-1)
    val add = addFn(useCase)

    val program = (for {
      _ <- add("DANNON", 300, yesterday)
      _ <- add("UNILEVER", 300, yesterday) //<==== remain credit
      _ <- add("DANNON", -500, yesterday)
    } yield ()).value

    program.unsafeRunSync() match {
      case Left(NegativeCreditPointException(_, _)) => succeed
      case Right(_) => fail(s"Expected to fail with NegativeCreditPointException")
      case Left(err) => fail(
        s"Expected to fail with NegativeCreditPointException but failed with $err"
      )
    }
  }
}
