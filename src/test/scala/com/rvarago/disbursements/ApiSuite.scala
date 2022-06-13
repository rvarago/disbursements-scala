package com.rvarago.disbursements

import cats.effect.IO
import cats.syntax.all._
import com.rvarago.disbursements.types.MerchantId
import com.rvarago.disbursements.types.DefaultCurrency
import munit.CatsEffectSuite
import org.http4s._
import org.http4s.implicits._
import squants.market.Money

import java.time.LocalDate
import cats.Applicative
import java.time.LocalDateTime

class ApiSuite extends CatsEffectSuite:

  test("requesting disbursements by matching week succeeds") {
    given Disbursements[IO] =
      newPreSetAlgebra[IO](DefaultCurrency(100.90).asRight)

    val req = Request[IO](Method.GET, uri"/api/v1/disbursements/2000-01-01")

    assertIO(retDisbursements(req).map(_.status), Status.Ok)
    assertIO(
      retDisbursements(req).flatMap(_.as[String]),
      """{"Total":{"amount":"€100.90"}}"""
    )
  }

  test("requesting disbursements by matching week/merchant succeeds") {
    given Disbursements[IO] =
      newPreSetAlgebra[IO](DefaultCurrency(50.10).asRight)

    val req = Request[IO](Method.GET, uri"/api/v1/disbursements/2000-01-01/123")

    assertIO(retDisbursements(req).map(_.status), Status.Ok)
    assertIO(
      retDisbursements(req).flatMap(_.as[String]),
      """{"Total":{"amount":"€50.10"}}"""
    )
  }

  test("requesting disbursements by non-matching week fails with not found") {
    given Disbursements[IO] = newPreSetAlgebra[IO](
      Disbursements.Error
        .NotFound(
          LocalDateTime.parse("1999-12-27T00:00:00"),
          LocalDateTime.parse("2000-01-02T23:59:59.999999999")
        )
        .asLeft
    )

    val req = Request[IO](Method.GET, uri"/api/v1/disbursements/2000-01-01")

    assertIO(retDisbursements(req).map(_.status), Status.NotFound)
    assertIO(
      retDisbursements(req).flatMap(_.as[String]),
      """{"NotFound":{"starting":"1999-12-27T00:00:00","ending":"2000-01-02T23:59:59.999999999"}}"""
    )
  }

  test(
    "requesting disbursements by non-matching week/merchant fails with not found"
  ) {
    given Disbursements[IO] = newPreSetAlgebra[IO](
      Disbursements.Error
        .NotFoundWithMerchant(
          LocalDateTime.parse("1999-12-27T00:00:00"),
          LocalDateTime.parse("2000-01-02T23:59:59.999999999"),
          123
        )
        .asLeft
    )

    val req = Request[IO](Method.GET, uri"/api/v1/disbursements/2000-01-01/123")

    assertIO(retDisbursements(req).map(_.status), Status.NotFound)
    assertIO(
      retDisbursements(req).flatMap(_.as[String]),
      """{"NotFound":{"starting":"1999-12-27T00:00:00","ending":"2000-01-02T23:59:59.999999999","merchantId":123}}"""
    )
  }

  def retDisbursements(req: Request[IO])(using
      Disbursements[IO]
  ): IO[Response[IO]] =
    Api.routes[IO].orNotFound(req)

  def newPreSetAlgebra[F[_]: Applicative](
      ret: Either[Disbursements.Error, Money]
  ): Disbursements[F] =
    new Disbursements[F] {
      override def total(
          inWeek: LocalDate
      ): F[Either[Disbursements.Error, Money]] = ret.pure[F]

      override def totalBy(inWeek: LocalDate)(
          merchantId: MerchantId
      ): F[Either[Disbursements.Error, Money]] = ret.pure[F]
    }
