/*
 * Copyright (C) 2015 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.iscpif.family

import scala.util.{ Random, Try }

trait Model
trait A
trait B
trait C

object Test extends App {

  val family = new ModelFamily with ScriptEngineCompilation {
    def imports: Seq[String] = Seq("fr.iscpif.family._")
    def source(traits: String, attributes: String): String =
      s"""
         |val model = new Model with $traits {
         |  $attributes
         |}
         |val o1 = 1
       """.stripMargin

    def attributes =
      Seq(TypedValue[Double]("a"), TypedValue[Double]("b"), TypedValue[Double]("c"))
    def inputs = Seq.empty
    def outputs = Seq(TypedValue[Double]("o1"))
    def combination: Combination[Class[_]] = AnyOf(classOf[A], classOf[B], classOf[C])
  }

  implicit val rng = new Random(42)

  println(family.code)
  println(family.run(2, 1.6, 2.8, 9.0))

}
