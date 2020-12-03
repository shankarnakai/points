package com.shankarnakai.points

import cats.effect.{ExitCode, IO, IOApp}

object Main extends IOApp {
  def run(args: List[String]) =
    PointsServer.stream[IO].compile.drain.as(ExitCode.Success)
}
