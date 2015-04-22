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

import scala.util.Try
import collection.JavaConversions

trait ModelFamily { family ⇒

  def attributeNameSpace = "_model_family_attributes_"
  def combination: Combination[Class[_]]
  def imports: Seq[String]
  def source(traits: String, attributes: String): String
  def attributes: Seq[String]
  def outputs: Seq[String]
  def modelId = "modelId"

  def traits: Seq[Class[_]] = combination.components
  def traitsCombinations = combination.combinations

  def traitsString = traitsCombinations.map { ts ⇒ ts.map(t ⇒ s"with ${t.getName}").mkString(" ") }

  def attributesStrings =
    attributes.map {
      name ⇒ s"def ${name}: Double = $attributeNameSpace.attributes.${name}"
    }

  def modelCode =
    traitsString.map {
      ts ⇒ source(ts, attributesStrings.map("  " + _).mkString("\n"))
    }

  def mapOutputCode =
    s"""
       |import scala.collection.JavaConversions.mapAsJavaMap
       |mapAsJavaMap(Map[String, Any](${outputs.map(o => s""""$o" -> $o""").mkString(", ")}))
     """.stripMargin

  def matchCode =
    modelCode.zipWithIndex.map {
      case (code, i) ⇒
        s"""
           |  case $i =>
           |    $code
           |    $mapOutputCode""".stripMargin
    }.mkString("\n")

  def attributesPrototypes = attributes.map(_ + ": Double").mkString(", ")

  def code =
    s"""
        |${imports.map("import " + _).mkString("\n")}
        |
        |($modelId: Int, $attributesPrototypes, rng: util.Random) => {
        |  implicit lazy val _rng = rng
        |
        |  object $attributeNameSpace {
        |    case class Attributes($attributesPrototypes)
        |    val attributes = Attributes(${attributes.mkString(", ")})
        |  }
        |
        |  ${modelId} match {
        |    $matchCode
        |    case x => throw new RuntimeException("Model id " + x + " too large")
        |  }
        |}
          """.stripMargin

  def compile(code: String): Try[Any]

  @transient lazy val compiled: Try[(Int, Seq[Double], util.Random) => Map[String, Any]] = {
    compile(code) map {
      c =>
        val method = c.getClass.getMethod("apply", (classOf[Int] :: attributes.map(c ⇒ classOf[Double]).toList ::: List(classOf[util.Random])): _*)
        (id: Int, attributes: Seq[Double], rng: util.Random) =>
          JavaConversions.mapAsScalaMap(
            method.invoke(c, (id.asInstanceOf[AnyRef] :: attributes.map(_.asInstanceOf[AnyRef]).toList ::: List(rng)): _*).asInstanceOf[java.util.Map[String, Any]]
          ).toMap
    }
  }

  def run(model: Int, attribute: Double*)(implicit rng: util.Random): Try[Map[String, Any]] = compiled.map(_(model, attribute, rng))

}
