package com.rvarago.disbursements.types

import com.rvarago.disbursements.types.Email.Email
import com.rvarago.disbursements.types.Names.Name

/** A customer of an e-commerce shop.
  */
final case class Shopper(
    name: Name,
    email: Email,
    nif: Nif
)

opaque type Nif = Long
