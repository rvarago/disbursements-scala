package com.rvarago.disbursements

import cats.effect.Sync
import cats.implicits._
import com.rvarago.disbursements.Disbursements
import com.rvarago.disbursements.types.MerchantId
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.HttpRoutes

import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import squants.market.Money

import java.time.LocalDate
import java.time.LocalDateTime
import scala.util.Try

/** Http endpoints to query disbursements over time by merchant.
  */
object Api:

  def routes[F[_]: Sync](using alg: Disbursements[F]): HttpRoutes[F] =
    Router("api/v1/disbursements" -> get)

  private def get[F[_]: Sync](using
      alg: Disbursements[F]
  ) = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    enum Error:
      case NotFound(
          starting: LocalDateTime,
          ending: LocalDateTime,
          merchantId: Option[MerchantId]
      )

    object Error:
      def from: Disbursements.Error => Error = _ match
        case Disbursements.Error.NotFound(from, to) =>
          Error.NotFound(from, to, none)
        case Disbursements.Error.NotFoundWithMerchant(from, to, merchantId) =>
          Error.NotFound(from, to, merchantId.some)

    extension (e: Error)
      def fail = e match
        case Error.NotFound(_, _, _) => NotFound(e.asJson.deepDropNullValues)

    enum Success:
      case Total(amount: String)

    object Success:
      def from(amount: Money): Success =
        Success.Total(amount.toFormattedString)

    extension (s: Success) def succeed = Ok(s.asJson.deepDropNullValues)

    def toHttp(r: Either[Disbursements.Error, Money]) =
      r.fold(Error.from(_).fail, Success.from(_).succeed)

    HttpRoutes.of[F] {
      case GET -> Root / LocalDateVar(inWeek) =>
        alg
          .total(inWeek)
          .flatMap(toHttp(_))

      case GET -> Root / LocalDateVar(inWeek) / LongVar(merchantId) =>
        alg
          .totalBy(inWeek)(merchantId)
          .flatMap(toHttp(_))
    }
  }

object LocalDateVar:
  def unapply(str: String): Option[LocalDate] =
    Option.when(!str.isEmpty)(Try(LocalDate.parse(str)).toOption).flatten
