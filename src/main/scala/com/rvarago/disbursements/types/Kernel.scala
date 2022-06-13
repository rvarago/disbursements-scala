package com.rvarago.disbursements.types

object Names:
  opaque type Name = String

  object Name:
    def apply: String => Name = identity

  extension (self: Name) def get: String = self

object Email:
  opaque type Email = String

  object Email:
    def apply: String => Email = identity

  extension (self: Email) def get: String = self

type MerchantId = Long

type ShopperId = Long
