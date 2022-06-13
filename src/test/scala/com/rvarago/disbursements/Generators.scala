package com.rvarago.disbursements

import cats.syntax.option._
import com.rvarago.disbursements.types.DefaultCurrency
import com.rvarago.disbursements.types.MerchantId
import com.rvarago.disbursements.types.Order
import com.rvarago.disbursements.types.ShopperId
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import squants.market.Money

import java.time.LocalDateTime
import java.time.ZoneOffset.UTC

object Gens:

  lazy val orders = for {
    merchantId <- merchantsIds
    shopperId <- arbitrary[ShopperId]
    amount <- amounts
    createdAt <- localDateTimes
    completedAt <- localDateTimes.map(_.some)
  } yield Order(merchantId, shopperId, amount, createdAt, completedAt)

  lazy val merchantsIds = arbitrary[MerchantId]

  lazy val amounts = Gen.posNum[BigDecimal].map(DefaultCurrency(_))

  lazy val localDateTimes = for
    seconds <- Gen.chooseNum(
      minDateTime.toEpochSecond(UTC),
      maxDateTime.toEpochSecond(UTC)
    )
    nanos <- Gen.chooseNum(
      minDateTime.getNano,
      maxDateTime.getNano
    )
  yield LocalDateTime.ofEpochSecond(seconds, nanos, UTC)

  private val minDateTime = LocalDateTime.parse("1900-01-01T00:00:00")
  private val maxDateTime = LocalDateTime.parse("2999-12-31T23:59:59")
