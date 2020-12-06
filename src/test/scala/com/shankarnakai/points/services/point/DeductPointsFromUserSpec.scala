package com.shankarnakai.points.services.point

import cats.effect.IO
import com.shankarnakai.points.domain.points.exceptions.{
  NegativeCreditPointException,
  PointException,
}
import com.shankarnakai.points.domain.points.model.Point
import com.shankarnakai.points.domain.user.model.User
import com.shankarnakai.points.infrastructure.repository.inmemory._
import com.shankarnakai.points.services.point.helper._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

import java.time.LocalDateTime
import scala.collection.concurrent.TrieMap

class DeductPointsFromUserSpec extends AnyFlatSpec with Matchers {
  private val deductFn = (useCase: DeductPointsFromUser[IO]) =>
    (value: Int) => useCase.exec(1L, value)

  def testResource(initialPointId: Long = 0): (TrieMap[Long, Point], DeductPointsFromUser[IO]) = {
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

    var idPointGeneratorMutable: Long = initialPointId
    val pointCache = new TrieMap[Long, Point]()
    val pointRepo = PointRepositoryInMemory[IO](
      pointCache,
      () => {
        idPointGeneratorMutable += 1
        idPointGeneratorMutable
      },
    )

    (pointCache, DeductPointsFromUser[IO](userRepo, pointRepo, BalancePoints.byOldest()))
  }

  "DeductPointsFromUser" should "consume a single record related to the same company that it's deducting points" in {
    val (pointCache, useCase) = testResource(initialPointId = 2)
    val today = LocalDateTime.now()
    val yesterday = today.plusDays(-1)

    val deduct = deductFn(useCase)
    List(
      Point(Some(1), 1L, "UNILEVER", 300, yesterday, consumed = false, createBy = 1L),
      Point(Some(2), 1L, "DANNON", 300, today, consumed = false, createBy = 1L),
    ).foreach(p => pointCache.update(p.id.get, p))

    val result: Either[PointException, List[Point]] = deduct(500).value.unsafeRunSync()

    val expected = List(
      Point(Some(3), 1L, "UNILEVER", -300, yesterday, consumed = true, createBy = 1L),
      Point(Some(4), 1L, "DANNON", -200, yesterday, consumed = true, createBy = 1L),
    )

    result match {
      case Right(pointList) =>
        removeTransactionDate(pointList) shouldBe removeTransactionDate(expected)
      case Left(err) => fail(s"Unexpected error $err")
    }
  }

  it should "fail when attempt to negative the company total points from a user still with CREDIT" in {
    val (pointCache, useCase) = testResource(initialPointId = 2)
    val today = LocalDateTime.now()
    val yesterday = today.plusDays(-1)

    val deduct = deductFn(useCase)
    List(
      Point(Some(1), 1L, "UNILEVER", 300, yesterday, consumed = false),
      Point(Some(2), 1L, "DANNON", 300, today, consumed = false),
    ).foreach(p => pointCache.update(p.id.get, p))

    val result: Either[PointException, List[Point]] = deduct(700).value.unsafeRunSync()

    result match {
      case Left(NegativeCreditPointException(_, _, _)) => succeed
      case Right(_) => fail("Expected to fail with NegativeCreditPointException")
      case Left(err) =>
        fail(s"Unexpected expection $err, expected to fail with NegativeCreditPointException")
    }

  }
}
