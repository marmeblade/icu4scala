/*
 * Copyright 2024 Perpetuum Works
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package icu4scala

object AST {
  sealed trait ChoiceFragment extends Any
  case object ChoicePlaceholder extends ChoiceFragment
  sealed trait Fragment extends ChoiceFragment

  case class StringFragment(value: String) extends Fragment

  sealed trait Param {
    val paramName: String
  }

  case class StringParam(paramName: String) extends Param with Fragment

  case class IntegerParam(paramName: String) extends Param

  case class SelectFragment(
      paramName: String,
      choices: Map[String, Seq[Fragment]],
      other: Seq[Fragment]
  ) extends Fragment

  case class PluralFragment(
      paramName: String,
      exact: Map[Long, Seq[ChoiceFragment]],
      zero: Option[Seq[ChoiceFragment]],
      one: Option[Seq[ChoiceFragment]],
      two: Option[Seq[ChoiceFragment]],
      other: Seq[ChoiceFragment]
  ) extends Fragment
}
