package com.rvarago.disbursements

import cats.Applicative
import cats.data.NonEmptyVector
import cats.syntax.all._
import com.rvarago.disbursements.types.MerchantId
import com.rvarago.disbursements.types.Order

import java.time.LocalDateTime

/** A channel through which orders are queried. */
trait Repository[F[_]]:

  /** Fetches all orders completed within the time period defined by [from, to).
    */
  def fetchOrders(period: Period): F[Option[NonEmptyVector[Order]]]

  /** Fetches all orders completed within the time period defined by [from, to)
    * to a given merchant.
    */
  def fetchOrdersBy(period: Period)(
      merchantId: MerchantId
  ): F[Option[NonEmptyVector[Order]]]

/** A semi-open time interval as defined by [from, to).
  */
final case class Period(
    from: LocalDateTime,
    to: LocalDateTime
)

object Repository:

  /** A repository backed by an naive, in-memory storage.
    *
    * For the sake of simplicity, this implementation relies on a rather
    * query-wise inefficient representation as a flat sequence. A perhaps better
    * option would be an associate structure with two nested levels:
    *
    *   - keyed by the time period truncated to the first day of the week,
    *   - then, keyed by the merchant.
    */
  def inMemoryConst[F[_]: Applicative](orders: Vector[Order]): Repository[F] =
    new Repository[F]:

      override def fetchOrders(
          period: Period
      ): F[Option[NonEmptyVector[Order]]] =
        allWithin(period).toNev.pure[F]

      override def fetchOrdersBy(period: Period)(
          merchantId: MerchantId
      ): F[Option[NonEmptyVector[Order]]] =
        allWithin(period).filter(_.merchantId == merchantId).toNev.pure[F]

      def allWithin(period: Period): Vector[Order] = {
        def in(instant: LocalDateTime) =
          !instant.isBefore(period.from) && instant.isBefore(period.to)

        orders.filter(_.completedAt.exists(in(_)))
      }
