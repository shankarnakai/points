package com.shankarnakai.points.infrastructure.endpoints.points

import cats.effect.Sync
import cats.syntax.all._
import com.shankarnakai.points.domain.points.model.{DeductValue, Point, PointRequest}
import com.shankarnakai.points.domain.points.exceptions.{NegativeCreditPointException, UserIdForPointFoundException}
import com.shankarnakai.points.domain.points.exceptions.IllegalPointArgumentException
import com.shankarnakai.points.services.point.{AddPointsToUser, DeductPointsFromUser, GetBalanceFromUser, PointUseCases}
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, HttpRoutes}
import Point._


class PointEndpoint[F[_]: Sync] extends Http4sDsl[F] {
  /* Jsonization of our User type */

  implicit val deductValueDecoder: EntityDecoder[F, DeductValue] = jsonOf

  implicit val pointRequestDecoder: EntityDecoder[F, PointRequest] = jsonOf
  implicit val pointRequestListDecoder: EntityDecoder[F, List[PointRequest]] = jsonOf

  private def addPoint(
      addPoint: AddPointsToUser[F],
  ): HttpRoutes[F] =
    HttpRoutes.of[F] { case req @ POST -> Root / LongVar(userId) / "points" =>
      val action = for {
        point <- req.as[PointRequest].map(body => Point(userId, body))
        result <- addPoint.exec(userId, point).value
      } yield result

      action.flatMap {
        case Right(saved) => Ok(saved.asJson)
        case Left(UserIdForPointFoundException(userId, _)) =>
          NotFound(s"User with id #$userId not found")
        case Left(NegativeCreditPointException(_, point, credit)) =>
          NotAcceptable(s"Payer ${point.payer} don't have enough credit. Credit: $credit points")
        case Left(IllegalPointArgumentException(_, _)) =>
          BadRequest(
            s"User id in the url is not compatible with the user id present in the request data",
          )
        case Left(_) => InternalServerError("Server Error")
      }
    }

  private def getBalance(getBalance: GetBalanceFromUser[F]) : HttpRoutes[F] =
  HttpRoutes.of[F] { case GET -> Root / LongVar(userId) / "points" =>
    val action = for {
      result <- getBalance.exec(userId).value
    } yield result

    action.flatMap {
      case Right(saved) => Ok(saved.asJson)
      case Left(UserIdForPointFoundException(userId, _)) =>
        NotFound(s"User with id #$userId not found")
      case Left(_) => InternalServerError("Server Error")
    }
  }

  private def deductPoint(deduct: DeductPointsFromUser[F]) : HttpRoutes[F] =
    HttpRoutes.of[F] { case req @ DELETE -> Root / LongVar(userId) / "points" =>
      val action = for {
        pointToDeduct <- req.as[DeductValue]
        result <- deduct.exec(userId, pointToDeduct.point).value
      } yield result

      action.flatMap {
        case Right(saved) => Ok(saved.asJson)
        case Left(UserIdForPointFoundException(userId, _)) =>
          NotFound(s"User with id #$userId not found")
        case Left(NegativeCreditPointException(_, point, credit)) =>
          NotAcceptable(s"Payer ${point.payer} don't have enough credit. Credit: $credit points")
        case Left(IllegalPointArgumentException(_, _)) =>
          BadRequest(
            s"User id in the url is not compatible with the user id present in the request data",
          )
        case Left(_) => InternalServerError("Server Error")
      }
    }

  def endpoints(service: PointUseCases[F]): HttpRoutes[F] = {
    val unAuthEndpoints =
      addPoint(service.addPointsToUser) <+>
      getBalance(service.getBalanceFromUser) <+>
      deductPoint(service.deductPointsFromUser)

    unAuthEndpoints
  }
}

object PointEndpoint {
  def endpoints[F[_]: Sync](service: PointUseCases[F]): HttpRoutes[F] =
    new PointEndpoint[F].endpoints(service)
}
