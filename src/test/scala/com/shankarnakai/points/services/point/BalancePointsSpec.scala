package com.shankarnakai.points.services.point

import com.shankarnakai.points.services.point.helper._
import com.shankarnakai.points.domain.points.model.Point
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

import java.time.LocalDateTime

class BalancePointsSpec extends AnyFlatSpec with Matchers {
  "BalancePoints" should "consume older points first" in {
    val today = LocalDateTime.now()

    val points = List(
      Point(Some(1), 1L, "PAYER", 500, today, consumed = false),
      Point(Some(2), 1L, "PAYER", 500, today.plusDays(1), consumed = false),
      Point(Some(3), 1L, "PAYER", -500, today.plusDays(3), consumed = false),
    )

    val result = BalancePoints.byOldest().exec(points)
    result.size shouldBe points.size
    result.filter(_.consumed).map(_.id.get) should contain allElementsOf List(1, 3)
  }

  it should "mark as everything consumed" in {
    val points = List(
      Point(1L, "PAYER_NAME", 500),
      Point(1L, "PAYER_NAME", -500),
    )

    val result = BalancePoints.byOldest().exec(points)
    result.size shouldBe points.size
    result.map(_.consumed) shouldBe List(true, true)
  }

  it should "consume multiple record of points" in {
    val points = List(
      Point(1L, "PAYER_NAME", 100),
      Point(1L, "PAYER_NAME", 250),
      Point(1L, "PAYER_NAME", 150),
      Point(1L, "PAYER_NAME", -500),
    )

    val result = BalancePoints.byOldest().exec(points)
    result.size shouldBe points.size
    result.map(_.consumed) shouldBe List(true, true, true, true)
  }

  it should "consume partially points, balancing the remaining points" in {
    val today = LocalDateTime.now()
    val yesterday = today.plusDays(-1)
    val points = List(
      Point(Some(1), 1L, "PAYER_NAME", 100, yesterday, consumed = false),
      Point(Some(2), 1L, "PAYER_NAME", 300, yesterday, consumed = false),
      Point(Some(3), 1L, "PAYER_NAME", 250, today, consumed = false),
      Point(Some(4), 1L, "PAYER_NAME", -500, today, consumed = false),
    )

    val expected = List(
      //====== Those will be consumed first
      Point(Some(1), 1L, "PAYER_NAME", 100, yesterday, consumed = true),
      Point(Some(2), 1L, "PAYER_NAME", 300, yesterday, consumed = true),
      //====== (100 + 300 ) - 500 = -100 (remaining)
      Point(Some(3), 1L, "PAYER_NAME", 250, today, consumed = true),
      Point(Some(4), 1L, "PAYER_NAME", -500, today, consumed = true),
      //====== 250 - 100 = 150

      //====== Create 1 record with the negative value to nullify the remaining
      //====== value from the previous calc and then we can mark everything as consumed
      Point(None, 1L, "PAYER_NAME", -150, today, consumed = true),
      //======  Create 1 record with the remain value
      Point(None, 1L, "PAYER_NAME", 150, today, consumed = false),
    )

    val result = BalancePoints.byOldest().exec(points)
    result.size shouldBe expected.size

    val result_values = removeTransactionDate(result)
    val expected_values = removeTransactionDate(expected)
    result_values shouldBe expected_values
  }
}
