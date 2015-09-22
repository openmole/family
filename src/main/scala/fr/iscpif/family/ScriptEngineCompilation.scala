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

import javax.script.ScriptEngineManager
import scala.tools.nsc.interpreter.IMain
import scala.util.Try


trait ScriptEngineCompilation <: ModelFamily {
  def engine = {
    val eng = new ScriptEngineManager(this.getClass.getClassLoader).getEngineByName("scala").asInstanceOf[IMain]
    val settings = eng.settings
    class Plop
    settings.embeddedDefaults[Plop]
    settings.usejavacp.value = true
    eng
  }

  def compile(code: String): Try[Any] = Try {
    engine.eval(code)
  }

}
