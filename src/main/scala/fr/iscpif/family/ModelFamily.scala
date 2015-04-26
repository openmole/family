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

object TypedValue {
  def apply[T](name: String)(implicit `type`: Manifest[T]): TypedValue = new TypedValue(name, `type`)
}

class TypedValue(val name: String, val `type`: Manifest[_]) {
  override def toString = s"$name: ${`type`}"
}

trait ModelFamily { family ⇒

  def modelFamilyNameSpace = "_model_family_"
  def combination: Combination[Class[_]]
  def imports: Seq[String]
  def source(traits: String, attributes: String): String

  def inputs: Seq[TypedValue]
  def attributes: Seq[TypedValue]
  def outputs: Seq[TypedValue]

  def modelId = "_modelId"
  def emptyTrait = "EmptyTrait"

  def traits: Seq[Class[_]] = combination.components
  def traitsCombinations = combination.combinations

  def traitsString =
    traitsCombinations.map {
      case Seq() => s"$modelFamilyNameSpace.$emptyTrait"
      case ts => ts.map(t ⇒ s"${t.getName}").mkString(" with ")
    }

  def attributesStrings =
    attributes.map {
      a ⇒ s"lazy val ${a} = $modelFamilyNameSpace.attributes.${a.name}"
    }

  def modelCode =
    traitsString.map {
      ts ⇒ source(ts, attributesStrings.map("  " + _).mkString("\n"))
    }

  def mapOutputCode =
    s"""
       |import scala.collection.JavaConversions.mapAsJavaMap
       |mapAsJavaMap(Map[String, Any](${outputs.map(o => s""""${o.name}" -> ($o)""").mkString(", ")}))
     """.stripMargin

  def matchCode =
    modelCode.zipWithIndex.map {
      case (code, i) ⇒
        s"""
           |  case $i =>
           |    $code
           |    $mapOutputCode""".stripMargin
    }.mkString("\n")

  def allInputsString = allInputs.mkString(", ")
  def allInputs = attributes ++ inputs

  def code =
    s"""
        |${imports.map("import " + _).mkString("\n")}
        |
        |($modelId: Int, $allInputsString, rng: util.Random) => {
        |  implicit lazy val _rng = rng
        |
        |  object $modelFamilyNameSpace {
        |    trait $emptyTrait
        |    case class Attributes($allInputsString)
        |    val attributes = Attributes(${allInputs.map(_.name).mkString(", ")})
        |  }
        |
        |  ${modelId} match {
        |    $matchCode
        |    case x => throw new RuntimeException("Model id " + x + " too large")
        |  }
        |}
          """.stripMargin

  def compile(code: String): Try[Any]

  @transient lazy val compiled: Try[(Int, Seq[Any], util.Random) => Map[String, Any]] = {
    compile(code) map {
      c =>
        val method = c.getClass.getMethod("apply", (classOf[Int] :: allInputs.map(_.`type`.runtimeClass).toList ::: List(classOf[util.Random])): _*)
        (id: Int, inputs: Seq[Any], rng: util.Random) =>
          JavaConversions.mapAsScalaMap(
            method.invoke(c, (id.asInstanceOf[AnyRef] :: inputs.map(_.asInstanceOf[AnyRef]).toList ::: List(rng)): _*).asInstanceOf[java.util.Map[String, Any]]
          ).toMap
    }
  }

  def run(model: Int, parameters: Any*)(implicit rng: util.Random): Try[Map[String, Any]] =
    compiled.map(_(model, parameters, rng))

}
