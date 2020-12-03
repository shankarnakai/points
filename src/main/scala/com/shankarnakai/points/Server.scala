package com.shankarnakai.points

//import config._
import cats.effect._
import com.shankarnakai.points.domain.user.model.User
import com.shankarnakai.points.infrastructure.endpoints.users.UserEndpoint
import com.shankarnakai.points.infrastructure.repository.inmemory.UserRepositoryInMemory
import com.shankarnakai.points.services.user.{UserUseCases, UserUseCasesLive}
import org.http4s.HttpApp
import org.http4s.server.{Router, Server => H4Server}
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.implicits._

import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext.global
import scala.util.Random

object Server extends IOApp {
  private val SERVER_HOST = "0.0.0.0"
  private val SERVER_PORT = 8080

  val idGenerator = new Random()

  def httpApp[F[_]: Effect: ContextShift: Timer](userUseCases: UserUseCases[F]): HttpApp[F] =
    Router(
      "/users" -> UserEndpoint.endpoints(userUseCases),
    ).orNotFound

  def createServer[F[_]: ConcurrentEffect: ContextShift: Timer]: Resource[F, H4Server[F]] =
    for {
      _ <- Blocker[F]
      userRepo = UserRepositoryInMemory[F](
        new TrieMap[Long, User](),
        () => idGenerator.nextLong(Long.MaxValue),
      )
      userUseCases = UserUseCasesLive[F](userRepo)
      app = httpApp[F](userUseCases)
      server <- BlazeServerBuilder[F](global)
        .bindHttp(SERVER_PORT, SERVER_HOST)
        .withHttpApp(app)
        .resource
    } yield server

  override def run(args: List[String]): IO[ExitCode] =
    createServer[IO].use(_ => IO.never).as(ExitCode.Success)
}
