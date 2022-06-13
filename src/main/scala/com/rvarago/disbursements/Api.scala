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

    enum ApiError:
      case NotFound(
          starting: LocalDateTime,
          ending: LocalDateTime,
          merchantId: Option[MerchantId]
      )

    object ApiError:
      def from: Disbursements.Error => ApiError = _ match
        case Disbursements.Error.NotFound(from, to) =>
          ApiError.NotFound(from, to, none)
        case Disbursements.Error.NotFoundWithMerchant(from, to, merchantId) =>
          ApiError.NotFound(from, to, merchantId.some)

    extension (e: ApiError)
      def fail = e match
        case ApiError.NotFound(_, _, _) => NotFound(e.asJson.deepDropNullValues)

    enum ApiSuccess:
      case Total(amount: String)

    object ApiSuccess:
      def from(
          amount: Money
      ): ApiSuccess =
        ApiSuccess.Total(amount.toFormattedString)

    extension (s: ApiSuccess) def succeed = Ok(s.asJson.deepDropNullValues)

    def toHttp(r: Either[Disbursements.Error, Money]) =
      r.fold(ApiError.from(_).fail, ApiSuccess.from(_).succeed)

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
