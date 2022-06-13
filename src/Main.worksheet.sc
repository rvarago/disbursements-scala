List(1).map(_ + 1)

trait Semigroup[A]:
  def combine(l: A, r: A): A

object Semigroup:
  implicit def optionSemigroup[A](implicit
      ev: Semigroup[A]
  ): Semigroup[Option[A]] =
    new Semigroup[Option[A]] {}
