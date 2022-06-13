package com.rvarago.disbursements

import cats.Id
import cats.syntax.all._
import com.rvarago.disbursements.types.DefaultCurrency
import com.rvarago.disbursements.types.Order
import com.rvarago.disbursements.types.disbursedAmount
import munit.FunSuite
import munit.ScalaCheckSuite
import org.scalacheck.Gen
import org.scalacheck.Prop._

import java.time.LocalDateTime
import scala.util.Left
import scala.util.Right

class RepositorySuite extends FunSuite with ScalaCheckSuite:

  test("orders queried by period match fixed values") {
    val repository = newRepository(
      Vector(
        Order(
          50L,
          60L,
          DefaultCurrency(1),
          LocalDateTime.parse("2000-12-31T00:00:00"),
          LocalDateTime.parse("2022-04-26T00:00:00").some
        ),
        Order(
          50L,
          60L,
          DefaultCurrency(2),
          LocalDateTime.parse("2000-12-31T00:00:00"),
          LocalDateTime.parse("2022-04-27T00:00:00").some
        ),
        Order(
          50L,
          60L,
          DefaultCurrency(3),
          LocalDateTime.parse("2000-12-31T00:00:00"),
          LocalDateTime.parse("2022-04-28T00:00:00").some
        ),
        Order(
          50L,
          60L,
          DefaultCurrency(4),
          LocalDateTime.parse("2000-12-31T00:00:00"),
          LocalDateTime.parse("2022-04-28T00:00:00").some
        )
      )
    )

    val scenarios = List(
      (
        Period(
          LocalDateTime.parse("2022-04-25T00:00:00"),
          LocalDateTime.parse("2022-04-25T23:59:59")
        ),
        none
      ),
      (
        Period(
          LocalDateTime.parse("2022-04-26T00:00:00"),
          LocalDateTime.parse("2022-04-26T23:59:59")
        ),
        1.some
      ),
      (
        Period(
          LocalDateTime.parse("2022-04-26T00:00:00"),
          LocalDateTime.parse("2022-04-27T23:59:59")
        ),
        2.some
      ),
      (
        Period(
          LocalDateTime.parse("2022-04-26T00:00:00"),
          LocalDateTime.parse("2022-04-28T23:59:59")
        ),
        4.some
      )
    )

    scenarios.foreach { case (period, expectedNumberOfOrders) =>
      val obtainedNumberOfOrders = repository.fetchOrders(period).map(_.length)
      assertEquals(
        obtainedNumberOfOrders,
        expectedNumberOfOrders,
        s"mismatch for period $period"
      )
    }
  }

  test("orders queried by period and merchant match fixed values") {
    val repository = newRepository(
      Vector(
        Order(
          51L,
          61L,
          DefaultCurrency(1),
          LocalDateTime.parse("2000-12-31T00:00:00"),
          LocalDateTime.parse("2022-04-26T00:00:00").some
        ),
        Order(
          52L,
          62L,
          DefaultCurrency(2),
          LocalDateTime.parse("2000-12-31T00:00:00"),
          LocalDateTime.parse("2022-04-27T00:00:00").some
        ),
        Order(
          53L,
          63L,
          DefaultCurrency(3),
          LocalDateTime.parse("2000-12-31T00:00:00"),
          LocalDateTime.parse("2022-04-28T00:00:00").some
        ),
        Order(
          53L,
          63L,
          DefaultCurrency(4),
          LocalDateTime.parse("2000-12-31T00:00:00"),
          LocalDateTime.parse("2022-04-28T00:00:00").some
        )
      )
    )

    val scenarios = List(
      (
        Period(
          LocalDateTime.parse("2022-04-25T00:00:00"),
          LocalDateTime.parse("2022-04-25T23:59:59")
        ),
        51,
        none
      ),
      (
        Period(
          LocalDateTime.parse("2022-04-26T00:00:00"),
          LocalDateTime.parse("2022-04-26T23:59:59")
        ),
        52,
        none
      ),
      (
        Period(
          LocalDateTime.parse("2022-04-26T00:00:00"),
          LocalDateTime.parse("2022-04-27T23:59:59")
        ),
        52,
        1.some
      ),
      (
        Period(
          LocalDateTime.parse("2022-04-26T00:00:00"),
          LocalDateTime.parse("2022-04-28T23:59:59")
        ),
        53,
        2.some
      )
    )

    scenarios.foreach { case (period, merchantId, expectedNumberOfOrders) =>
      val obtainedNumberOfOrders =
        repository.fetchOrdersBy(period)(merchantId).map(_.length)
      assertEquals(
        obtainedNumberOfOrders,
        expectedNumberOfOrders,
        s"mismatch for period $period and merchantId $merchantId"
      )
    }
  }

  property("there are no matching by period with empty orders") {
    val repository = newRepository(Vector.empty)
    forAll(periods) { repository.fetchOrders(_).isEmpty }
  }

  property("there are no matching by period and merchant with empty orders") {
    val repository = newRepository(Vector.empty)
    forAll(periods, Gens.merchantsIds) { (period, merchantId) =>
      repository.fetchOrdersBy(period)(merchantId).isEmpty
    }
  }

  def newRepository(orders: Vector[Order]) =
    Repository.inMemoryConst[Id](orders)

  def periods = for
    from <- Gens.localDateTimes
    to <- Gens.localDateTimes.suchThat(!_.isAfter(from))
  yield Period(from, to)
