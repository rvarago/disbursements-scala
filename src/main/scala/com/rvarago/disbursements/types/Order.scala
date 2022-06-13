package com.rvarago.disbursements.types

import squants.market.EUR
import squants.market.Money

import java.time.LocalDateTime

/** A good bought by a shopper from a merchant.
  *
  * Requires: `amount >= 0`.
  *
  * TODO: Use a wrapped type for amount where we enforce
  */
final case class Order(
    merchantId: MerchantId,
    shopperId: ShopperId,
    amount: Money,
    createdAt: LocalDateTime,
    completedAt: Option[LocalDateTime]
)

/** Single currency supported throughout the system.
  *
  * It's **assumed** that all money-relevant operations are performed over this
  * single currency.
  */
val DefaultCurrency = EUR

extension (order: Order)
  /** Returns the disbursed amount for an order if complete, none otherwise
    *
    * The disbursement is defined as a percentage of the order's amount varying
    * according to its tier.
    */
  def disbursedAmount: Option[Money] =
    Option.when(order.isCompleted)(order.amount * order.tier.fee)

  private def tier: Tier = order.amount match
    case amount if amount < DefaultCurrency(50)  => Tier.Small
    case amount if amount < DefaultCurrency(300) => Tier.Medium
    case _                                       => Tier.High

  private def isCompleted: Boolean = order.completedAt.isDefined

private enum Tier:
  case Small
  case Medium
  case High

extension (category: Tier)
  def fee: BigDecimal = category match
    case Tier.Small  => BigDecimal.decimal(0.01)
    case Tier.Medium => BigDecimal.decimal(0.0095)
    case Tier.High   => BigDecimal.decimal(0.0085)
