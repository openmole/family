package fr.iscpif.family

object Combination {
  def empty[T] = new Combination[T] {
    def combinations = Seq.empty
    def components = Seq.empty
  }
}

sealed trait Combination[T] {
  def combinations: Seq[Seq[T]]
  def components: Seq[T]
}

case class AnyOf[T](components: T*) extends Combination[T] {
  def combinations = (0 to components.size).flatMap(components.combinations)
}

case class OneOf[T](components: T*) extends Combination[T] {
  def combinations = components.map(t ⇒ Seq(t))
}

case class AllToAll[T](cs: Combination[T]*) extends Combination[T] {
  def components = cs.flatMap(_.components).distinct

  def combinations =
    cs.map(_.combinations).reduceOption((ts1, ts2) ⇒ combine(ts1, ts2)).getOrElse(Seq.empty)

  def combine[A](it1: Seq[Seq[A]], it2: Seq[Seq[A]]): Seq[Seq[A]] =
    for (v1 ← it1; v2 ← it2) yield v1 ++ v2
}
