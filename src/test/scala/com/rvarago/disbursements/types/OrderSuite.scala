package com.rvarago.disbursements.types

import cats.syntax.all._
import com.rvarago.disbursements.Gens
import com.rvarago.disbursements.types.DefaultCurrency
import com.rvarago.disbursements.types.disbursedAmount
import munit.FunSuite
import munit.ScalaCheckSuite
import org.scalacheck.Prop._
import squants.market.Money

class OrderSuite extends FunSuite with ScalaCheckSuite:

  test("disbursed amounts match fixed values") {
    val scenarios = List(
      (ordersWithAmount(DefaultCurrency(0)), DefaultCurrency(0)),
      (ordersWithAmount(DefaultCurrency(1)), DefaultCurrency(0.01)),
      (ordersWithAmount(DefaultCurrency(49)), DefaultCurrency(0.49)),
      (ordersWithAmount(DefaultCurrency(50)), DefaultCurrency(0.475)),
      (ordersWithAmount(DefaultCurrency(51)), DefaultCurrency(0.4845)),
      (ordersWithAmount(DefaultCurrency(299)), DefaultCurrency(2.8405)),
      (ordersWithAmount(DefaultCurrency(300)), DefaultCurrency(2.55)),
      (ordersWithAmount(DefaultCurrency(301)), DefaultCurrency(2.5585))
    ).map(_.bimap(_.sample.get, _.some))

    scenarios.foreach { case (order, expectedDisbursedAmount) =>
      val obtainedDisbursedAmount = order.disbursedAmount
      assertEquals(
        obtainedDisbursedAmount,
        expectedDisbursedAmount,
        s"mismatch for order $order"
      )
    }
  }

  property("disbursed amounts are not available for incomplete orders") {
    forAll(incompleteOrders) { _.disbursedAmount == none }
  }

  property("disbursed amounts never go negative") {
    forAll(Gens.orders) { _.disbursedAmount >= DefaultCurrency(0).some }
  }

  property("disbursed amounts never go beyond the order amount") {
    forAll(Gens.orders) { order =>
      order.disbursedAmount <= order.amount.some
    }
  }

  def ordersWithAmount(amount: Money) = Gens.orders.map(_.copy(amount = amount))

  lazy val incompleteOrders = Gens.orders.map(_.copy(completedAt = None))
