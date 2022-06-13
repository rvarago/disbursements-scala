package com.rvarago.disbursements

import cats.Monad
import cats.syntax.all._
import com.rvarago.disbursements.types.MerchantId
import com.rvarago.disbursements.types.Order
import com.rvarago.disbursements.Period
import com.rvarago.disbursements.types.disbursedAmount
import squants.market.Money
import com.rvarago.disbursements.types.DefaultCurrency

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import com.rvarago.disbursements.types.Merchant
import cats.data.NonEmptyVector
import squants.market.MoneyConversions.MoneyNumeric
import java.math.MathContext
import java.math.RoundingMode

/** Algebra to compute amount disbursed.
  */
trait Disbursements[F[_]]:

  /** Returns the total amount of disbursements in the given week.
    *
    * A week is defined by the truncating the date such that:
    *   - opening time is the previous (or same) Monday (beginning) and
    *   - closing time is the next (or same) Sunday (ending).
    *
    * Requires: All stored amounts to be in the same currency.
    */
  def total(inWeek: LocalDate): F[Either[Disbursements.Error, Money]]

  /** Returns the total amount of disbursements in the given week made to the
    * given merchant.
    *
    * A week is defined by the truncating the date such that:
    *   - opening time is the previous (or same) Monday (beginning) and
    *   - closing time is the next (or same) Sunday (ending).
    *
    * Requires: All stored amounts to be in the same currency.
    */
  def totalBy(inWeek: LocalDate)(
      merchantId: MerchantId
  ): F[Either[Disbursements.Error, Money]]

object Disbursements:
  enum Error:
    case NotFound(
        from: LocalDateTime,
        to: LocalDateTime
    )
    case NotFoundWithMerchant(
        from: LocalDateTime,
        to: LocalDateTime,
        merchantId: MerchantId
    )

  def apply[F[_]: Monad](repository: Repository[F]): Disbursements[F] =
    new Disbursements[F]:

      override def total(
          inWeek: LocalDate
      ): F[Either[Error, Money]] = {
        val period = weekPeriodContaining(inWeek)

        def notFound =
          Error.NotFound(period.from, period.to)

        repository
          .fetchOrders(period)
          .map {
            _.fold(notFound.asLeft)(accumulateDisbursements(_).asRight)
          }
      }

      override def totalBy(
          inWeek: LocalDate
      )(merchantId: MerchantId): F[Either[Error, Money]] = {
        val period = weekPeriodContaining(inWeek)

        def notFound =
          Error.NotFoundWithMerchant(period.from, period.to, merchantId)

        repository
          .fetchOrdersBy(period)(merchantId)
          .map {
            _.fold(notFound.asLeft)(accumulateDisbursements(_).asRight)
          }
      }

      def accumulateDisbursements(orders: NonEmptyVector[Order]): Money = {
        val amount =
          orders
            .map(_.disbursedAmount.map(_.amount))
            .toVector
            .flatten
            .sumAll
            .round(MathContext(2, RoundingMode.HALF_UP))

        DefaultCurrency(amount)
      }

      def weekPeriodContaining(when: LocalDate): Period = {
        val from = when
          .`with`(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
          .atTime(LocalTime.MIN)
        val to = when
          .`with`(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
          .atTime(LocalTime.MAX)

        Period(from, to)
      }
