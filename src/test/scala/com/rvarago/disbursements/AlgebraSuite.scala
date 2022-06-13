package com.rvarago.disbursements

import cats.Id
import cats.data.NonEmptyVector
import cats.syntax.all._
import com.rvarago.disbursements.Period
import com.rvarago.disbursements.types.DefaultCurrency
import com.rvarago.disbursements.types.MerchantId
import com.rvarago.disbursements.types.Order
import munit.FunSuite
import munit.ScalaCheckSuite
import org.scalacheck.Gen
import org.scalacheck.Prop._

import java.time.LocalDate
import java.time.LocalDateTime
import scala.collection.immutable.HashMap
import scala.util.Left
import scala.util.Right
import java.time.LocalTime

class AlgebraSuite extends FunSuite with ScalaCheckSuite:

  test("total disbursed by period match fixed values") {
    val disbursementAlg = newDisbursementsAlg(
      Vector(
        (
          Period(
            LocalDate.parse("2022-04-25").atTime(LocalTime.MIN),
            LocalDate.parse("2022-05-01").atTime(LocalTime.MAX)
          ),
          Vector((50, Vector()))
        ),
        (
          Period(
            LocalDate.parse("2022-05-02").atTime(LocalTime.MIN),
            LocalDate.parse("2022-05-08").atTime(LocalTime.MAX)
          ),
          Vector(
            (
              50,
              Vector(orderWithAmount(100.10), orderWithAmount(200.20))
            ),
            (
              50,
              Vector(orderWithAmount(300.30), orderWithAmount(400.40))
            )
          )
        )
      )
    )

    val scenarios = List(
      (
        LocalDate.parse("2022-04-27"),
        none
      ),
      (
        LocalDate.parse("2022-05-03"),
        DefaultCurrency(8.80).some
      )
    )

    scenarios.foreach { case (inWeek, expectedDisbursedAmount) =>
      val obtainedDisbursedAmount = disbursementAlg.total(inWeek).toOption
      assertEquals(
        obtainedDisbursedAmount,
        expectedDisbursedAmount,
        s"mismatch for inWeek $inWeek"
      )
    }
  }

  test("total disbursed by period and merchant match fixed values") {
    val disbursementAlg = newDisbursementsAlg(
      Vector(
        (
          Period(
            LocalDate.parse("2022-04-25").atTime(LocalTime.MIN),
            LocalDate.parse("2022-05-01").atTime(LocalTime.MAX)
          ),
          Vector((50, Vector()))
        ),
        (
          Period(
            LocalDate.parse("2022-05-02").atTime(LocalTime.MIN),
            LocalDate.parse("2022-05-08").atTime(LocalTime.MAX)
          ),
          Vector(
            (
              50,
              Vector(orderWithAmount(100.10), orderWithAmount(200.20))
            ),
            (
              51,
              Vector(orderWithAmount(300.30), orderWithAmount(400.40))
            )
          )
        )
      )
    )

    val scenarios = List(
      (
        LocalDate.parse("2022-04-27"),
        50,
        none
      ),
      (
        LocalDate.parse("2022-05-03"),
        50,
        DefaultCurrency(2.90).some
      ),
      (
        LocalDate.parse("2022-05-03"),
        51,
        DefaultCurrency(6.00).some
      )
    )

    scenarios.foreach { case (inWeek, merchantId, expectedDisbursedAmount) =>
      val obtainedDisbursedAmount =
        disbursementAlg.totalBy(inWeek)(merchantId).toOption
      assertEquals(
        obtainedDisbursedAmount,
        expectedDisbursedAmount,
        s"mismatch for inWeek $inWeek and merchantId $merchantId"
      )
    }
  }

  property("there are no disbursement by period without orders") {
    val disbursements = newDisbursementsAlg(Vector.empty)
    forAll(Gens.localDateTimes) { inWeek =>
      disbursements.total(inWeek.toLocalDate) match
        case Left(Disbursements.Error.NotFound(_, _)) => true
        case _                                        => false
    }
  }

  property("there are no disbursement by period/merchant without orders") {
    val disbursements = newDisbursementsAlg(Vector.empty)
    forAll(Gens.localDateTimes, Gens.merchantsIds) { (inWeek, merchantId) =>
      disbursements.totalBy(inWeek.toLocalDate)(merchantId) match
        case Left(Disbursements.Error.NotFoundWithMerchant(_, _, _)) => true
        case _                                                       => false
    }
  }

  type Store = Vector[(Period, Vector[(MerchantId, Vector[Order])])]

  def newDisbursementsAlg(inner: Store) =
    Disbursements(newPreSetRepository(inner))

  def newPreSetRepository(inner: Store) = new Repository[Id]:
    def fetchOrders(period: Period): Id[Option[NonEmptyVector[Order]]] =
      inner.find(_._1 == period).map(_._2.flatMap(_._2)).flatMap(_.toNev)

    def fetchOrdersBy(period: Period)(
        merchantId: MerchantId
    ): Id[Option[NonEmptyVector[Order]]] =
      inner
        .find(_._1 == period)
        .map(_._2.filter(_._1 == merchantId).flatMap(_._2))
        .flatMap(_.toNev)

  def orderWithAmount(amount: BigDecimal) =
    Gens.orders.map(_.copy(amount = DefaultCurrency(amount))).sample.get
