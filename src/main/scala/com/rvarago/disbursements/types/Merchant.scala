package com.rvarago.disbursements.types

import com.rvarago.disbursements.types.Email.Email
import com.rvarago.disbursements.types.Names.Name

/** An e-commerce shop.
  */
final case class Merchant(
    name: Name,
    email: Email,
    cif: Cif
)

opaque type Cif = Long
