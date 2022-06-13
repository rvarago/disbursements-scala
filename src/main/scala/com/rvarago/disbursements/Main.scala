package com.rvarago.disbursements

import cats.Monad
import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.Resource
import cats.syntax.all._
import ciris._
import com.comcast.ip4s._
import com.rvarago.disbursements.Api
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.Logger

import java.nio.file.Path
import java.time.LocalDateTime

object Main extends IOApp:

  def run(args: List[String]): IO[ExitCode] =
    program.compile.drain.as(ExitCode.Success)

  def program = for {
    config <- fs2.Stream.eval(Config.fromEnv.load[IO])

    given _: Disbursements[IO] <- fs2.Stream.eval(
      newDisbursementsAlg(config.ordersCsvPath)
    )

    api = Api.routes[IO].orNotFound

    app = Logger.httpApp(logHeaders = true, logBody = true)(api)

    stream <- fs2.Stream
      .resource(
        EmberServerBuilder
          .default[IO]
          .withHost(config.ip)
          .withPort(config.port)
          .withHttpApp(app)
          .build >>
          Resource.eval(IO.never)
      )
      .drain
  } yield stream

  def newDisbursementsAlg(ordersCsvPath: Path) =
    Load
      .ordersFromCsv(ordersCsvPath)
      .map(orders => Disbursements[IO](Repository.inMemoryConst[IO](orders)))

  final case class Config(ip: IpAddress, port: Port, ordersCsvPath: Path)

  object Config:
    def fromEnv = {

      val ip =
        env("API_IP")
          .as[IpAddress](using
            ConfigDecoder[String, String].mapOption("IpAddress")(
              IpAddress.fromString
            )
          )
          .default(ip"127.0.0.1")

      val port = env("API_PORT")
        .as[Port](using
          ConfigDecoder[String, String].mapOption("Port")(
            Port.fromString
          )
        )
        .default(port"8080")

      val ordersCsvPath =
        env("API_ORDERS_CSV_PATH")
          .as[Path](using ConfigDecoder[String, String].map(Path.of(_)))
          .default(Path.of("orders.csv"))

      (ip, port, ordersCsvPath).parMapN(Config.apply)
    }
