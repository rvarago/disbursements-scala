package com.rvarago.disbursements

import cats.Monad
import cats.implicits._
import cats.effect.IO
import cats.effect.MonadCancel
import cats.effect.Resource
import cats.effect.kernel.Sync
import com.github.tototoshi.csv._
import com.rvarago.disbursements.Api
import com.rvarago.disbursements.types.DefaultCurrency
import com.rvarago.disbursements.types.MerchantId
import com.rvarago.disbursements.types.Order
import com.rvarago.disbursements.types.ShopperId
import org.http4s.implicits._

import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Reads a CSV file into a sequence of orders.
  *
  * A CSV file must adhere the following schema:
  *
  * $ID,$MERCHANT_ID,$SHOPPER_ID,$AMOUNT,$CREATED_AT,$COMPLETED_AT
  *
  * Where:
  *   - There must *not* be any header,
  *   - Dates must be encoded as 'dd/MM/yyyy HH:mm:ss',
  *   - $COMPLETED_AT is optional,
  *   - Extra columns are ignored.
  *
  * The amount is mapped onto the app's default currency.
  *
  * # Examples
  *
  * 113,3,359,268.76,04/01/2018 22:33:00, 4,9,11,185.36,01/01/2018
  * 03:48:00,03/01/2018 01:59:56
  */
object Load:
  private val dateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")

  // TODO: Define in terms of an abstract F[_] effect.
  def ordersFromCsv(path: Path): IO[Vector[Order]] = {

    def parseEntries: List[List[String]] => IO[Vector[Order]] = _.traverse {
      case _ :: merchantId :: shopperId :: amount :: createdAt :: maybeCompletedAtPlusRest =>
        IO.delay(
          Order(
            merchantId = merchantId.toLong,
            shopperId = shopperId.toLong,
            amount = DefaultCurrency(BigDecimal(amount)),
            createdAt = LocalDateTime.parse(createdAt, dateTimeFormatter),
            completedAt = maybeCompletedAtPlusRest.headOption
              .filter(_.nonEmpty)
              .map(
                LocalDateTime.parse(_, dateTimeFormatter)
              )
          )
        )
      case _ =>
        IO.raiseError(new RuntimeException("Not enough columns"))
    }.map(_.toVector)

    Resource
      .make(IO.blocking(CSVReader.open(path.toFile)))(r =>
        IO.blocking(r.close())
      )
      .use { r =>
        IO.blocking(r.all()).flatMap(parseEntries(_))
      }
      .recoverWith { e =>
        IO.raiseError(
          new RuntimeException(s"Could not parse CSV at $path, cause: $e")
        )
      }
  }
