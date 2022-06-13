# Disbursements Backend

A simple application built to calculate disbursements for orders bought by shoppers from merchants .

## Implementation

The application is written in [Scala](https://docs.scala-lang.org/) version 3 and relies on [cats-effect](https://typelevel.org/cats-effect/) as the async runtime where computations are reified as values of type `IO`.

The design takes a simple approach where we may acknowledge are four layers:

1. Api - HTTP routes for request/response to query by date/merchant, then
2. Algebra - Service where disbursements are accumulated given the search parameters, then
3. Repository - A thin abstraction over a storage layer, for the sake of simplicity it's currenctly backed by a in-memory constant store, then
4. Types - A set of types modelling business concepts, namely the order abstraction from which disbursements are computed.

Upon providing a date within the week of interest (and possibly the merchant id), we then find its corresponding week as per:

A week is defined by the truncating the date such that:
_ opening time is the previous (or same) Monday (beginning)
_ closing time is the next (or same) Sunday (ending).

Then, after obtaining the list of orders (and possibly filtering by merchant id), we then accumulate the disbursements for each orders, yielding the total as the JSON response.

## Api

The exposed API comprise two HTTP endpoints where one may query disbursements over time and, optionally, by merchant.

- Over time:

```text
- Request:

    api/v1/disbursements/$DATE

    -- Example:

        api/v1/disbursements/2000-01-01

- Response:

    -- On Success:

        {"Total":{"amount":"$DISBURSED"}}

        --- Example:

            {"Total":{"amount":"€100.90"}}

    -- On Error:

        {"NotFound":{"starting":"$START","ending":"$END"}}

        --- Example:

            {"NotFound":{"starting":"1999-12-27T00:00:00","ending":"2000-01-02T23:59:59.999999999"}}
```

- Over time by merchant:

```text
- Request:

    api/v1/disbursements/$DATE/$MERCHANTID

    -- Example:

        api/v1/disbursements/2000-01-01/123

- Response:

    -- On Success:

        {"Total":{"amount":"$DISBURSED"}}

        --- Example:

            {"Total":{"amount":"€50.10"}}

    -- On Error:

        {"NotFound":{"starting":"$START","ending":"$END","merchantId":$MERCHANTID}}}}

        --- Example:

            {"NotFound":{"starting":"1999-12-27T00:00:00","ending":"2000-01-02T23:59:59.999999999","merchantId":123}}
```

Where:

```text
* $DATE:       Date within the week one wants to query disbursements,
* $START:      Start of aggreation period containing $DATE,
* $END:        End of the aggreation period containing $DATE,
* $MERCHANTID: Identifier of the merchant to which disbursements are due,
* $DISBURSED:  Amount disbursed.

```

## Initial load

Upon start up, the app expects a CSV file to present from which it loads orders into the data store.

The file must adhere the following schema:

```text
$ID,$MERCHANT_ID,$SHOPPER_ID,$AMOUNT,$CREATED_AT,$COMPLETED_AT
```

Where:

```text
* There must *not* be any header,
* Dates must be encoded as dd/MM/yyyy HH:mm:ss,
* $COMPLETED_AT is optional,
* Extra columns are ignored.

```

The amount is mapped onto the app's default currency (EUR).

Example

```text
113,3,359,268.76,04/01/2018 22:33:00,
4,9,11,185.36,01/01/2018 03:48:00,03/01/2018 01:59:56
```

## Future improvements

- Tune usage of thread pools (e.g. shift disbursements computation onto a cached, unbounded pool where blocking is fine),
- Adjust logging to obtain relevantly useful information and nothing more,
- Replace the in-memory DB by a more fitting storage technology (e.g. PostgreSQL),
- Auto-generate API documentation (e.g. OpenAPI via [tapir](https://tapir.softwaremill.com/en/latest/docs/openapi.html))
- Expose endpoints to insert/change entities,
- Package the application into a distribution mechanism (e.g. Docker),
- Add infrastructure for load testing (e.g. via reproducible setup, e.g. via Nix),
- Protect endpoints with security layers (e.g. auth/authz),

## Run application

> The setup requires (sbt)[https://www.scala-sbt.org/] available in the system.

```shell
sbt run
```

By default, the application binds on `127.0.0.1:8080` and loads the set of orders from `orders.csv` (relative to the path of the binary).

> It's possible to override such defaults by setting the environment variables listed in the [template](.env.template).

After starting it up, we may query it (e.g. via curl) as:

```shell
λ curl -v localhost:8080/api/v1/disbursements/2000-01-01

λ curl -v localhost:8080/api/v1/disbursements/2000-01-01/123
```

## Run tests

```shell
sbt test
```
