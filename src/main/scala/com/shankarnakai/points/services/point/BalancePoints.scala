package com.shankarnakai.points.services.point

import com.shankarnakai.points.domain.points.model.Point

import java.time.LocalDateTime
import scala.annotation.tailrec

trait BalancePoints {
  def exec(points: List[Point]): List[Point]
}

class BalancePointsByOldest() extends BalancePoints {
  override def exec(points: List[Point]): List[Point] = {
    val creditDebit = (cleanPoint _).andThen(separateCreditDebit)(points)
    processPoints(creditDebit._1, creditDebit._2)
  }

  private def processPoints(credit: List[Point], debit: List[Point]): List[Point] = {
    @tailrec
    def helper(
        creditList: List[Point],
        debitList: List[Point],
        consumed: List[Point],
        leftPoints: Int,
    ): List[Point] =
      (creditList, debitList) match {
        case (Nil, Nil) => consumed
        case (credList, Nil) =>
          balanceRemainingPoints(
            credList,
            consumed,
            leftPoints,
          )
        case (Nil, debList) =>
          balanceRemainingPoints(
            debList,
            consumed,
            leftPoints,
          )
        case (credList, debList) =>
          val (cred, deb, cons, left) = consumePoints(
            credList,
            debList,
            consumed,
            leftPoints,
          )
          helper(cred, deb, cons, left)
      }

    helper(credit, debit, List.empty[Point], leftPoints = 0)
  }

  private def balanceRemainingPoints(
      list: List[Point],
      consumed: List[Point],
      leftPoints: Int,
  ): List[Point] = {
    val head :: tail = list
    val balancedPoints = if (leftPoints != 0) {
      val resultPoint = head.point + leftPoints

      val leftPointList = List(
        Point(
          None,
          head.userId,
          head.payer,
          -resultPoint,
          LocalDateTime.now(),
          consumed = true,
        ),
        Point(None, head.userId, head.payer, resultPoint, LocalDateTime.now(), consumed = false),
      )

      List(head.copy(consumed = true)) ++
        tail ++
        leftPointList
    } else list
    consumed ++ balancedPoints
  }

  private def consumePoints(
      creditList: List[Point],
      debitList: List[Point],
      consumed: List[Point],
      leftPoints: Int,
  ): (List[Point], List[Point], List[Point], Int) = {
    val creditHead :: creditTail = creditList
    val debitHead :: debitTail = debitList

    val resultPoint = creditHead.point + debitHead.point + leftPoints
    if (resultPoint == 0) {
      (
        creditTail,
        debitTail,
        consumed :+ debitHead.copy(consumed = true) :+ creditHead.copy(consumed = true),
        0,
      )
    } else if (resultPoint > 0) {
      (
        creditHead +: creditTail,
        debitTail,
        consumed :+ debitHead.copy(consumed = true),
        leftPoints + debitHead.point,
      )
    } else {
      (
        creditTail,
        debitHead +: debitTail,
        consumed :+ creditHead.copy(consumed = true),
        leftPoints + creditHead.point,
      )
    }
  }

  private def cleanPoint(list: List[Point]): List[Point] = list
    .sortBy(_.transactionDate)
    .filterNot(_.consumed)

  private def separateCreditDebit(list: List[Point]): (List[Point], List[Point]) =
    list.foldLeft((List.empty[Point], List.empty[Point])) { (agg, item) =>
      if (item.point > 0) (agg._1 :+ item, agg._2)
      else (agg._1, agg._2 :+ item)
    }
}

object BalancePoints {
  def byOldest(): BalancePoints = new BalancePointsByOldest()
}
