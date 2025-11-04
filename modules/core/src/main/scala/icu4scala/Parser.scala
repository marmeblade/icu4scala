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

import scala.annotation.nowarn

import fastparse.NoWhitespace._
import fastparse._
import icu4scala.AST._

@nowarn
object Parser {

  def parseICU(input: String): Parsed[Seq[Fragment]] = {
    def space[$: P]: P[Unit] = P(CharsWhileIn(" \r\n\t", 0))
    def strChars[$: P]: P[Unit] = P(CharsWhile(stringChars))
    def number[$: P]: P[Long] = P(CharsWhileIn("0-9").!).map(_.toLong)

//    def hexDigit[$: P] = P(CharIn("0-9a-fA-F"))
//    def unicodeEscape[$: P] = P("u" ~ hexDigit ~ hexDigit ~ hexDigit ~ hexDigit)
//    def escape[$: P] = P("\\" ~ (CharIn("{}/\\\\bfnrt") | unicodeEscape))

    def string[$: P]: P[StringFragment] =
      P(strChars.!).map(StringFragment)

    def choicePlaceholder[$: P]: P[AST.ChoicePlaceholder.type] =
      P("#").map(_ => ChoicePlaceholder)

    def stringParam[$: P]: P[StringParam] =
      P("{" ~ paramName.! ~ "}").map(StringParam)

    def paramName[$: P]: P[Unit] =
      P(!"other" ~ CharsWhileIn("a-zA-Z_\\-"))

    def pluralExact[$: P]: P[(Long, Seq[ChoiceFragment])] = P(
      "=" ~ number ~/ space ~ "{" ~/ choiceFragmentExpr ~ "}"
    )
    def pluralZero[$: P]: P[Seq[ChoiceFragment]] = P(
      "zero" ~/ space ~ "{" ~/ choiceFragmentExpr ~ "}"
    )
    def pluralOne[$: P]: P[Seq[ChoiceFragment]] = P(
      "one" ~/ space ~ "{" ~/ choiceFragmentExpr ~ "}"
    )
    def pluralTwo[$: P]: P[Seq[ChoiceFragment]] = P(
      "two" ~/ space ~ "{" ~/ choiceFragmentExpr ~ "}"
    )
    def pluralOther[$: P]: P[Seq[ChoiceFragment]] = P(
      "other" ~/ space ~ "{" ~/ choiceFragmentExpr ~ "}"
    )

    def plural[$: P]: P[PluralFragment] =
      P(
        "{" ~ space ~ paramName.! ~ "," ~ space ~ "plural" ~/ space ~ "," ~ space ~ (pluralExact ~ space).rep ~
          pluralZero.? ~ space ~ pluralOne.? ~ space ~ pluralTwo.? ~ space ~ pluralOther ~ space ~ "}"
      ).map { case (paramName, exacts, zero, one, two, other) =>
        PluralFragment(
          paramName,
          exacts.toMap,
          zero,
          one,
          two,
          other
        )
      }

    def selectPart[$: P]: P[(String, Seq[Fragment])] = P(
      paramName.! ~/ space ~ "{" ~/ fragmentExpr ~ "}"
    )
    def selectOther[$: P]: P[Seq[Fragment]] = P(
      "other" ~/ space ~ "{" ~/ fragmentExpr ~ "}"
    )

    def select[$: P]: P[SelectFragment] =
      P(
        "{" ~ space ~ paramName.! ~ "," ~ space ~ "select" ~/ space ~ "," ~ space ~ (selectPart ~ space).rep ~
          space ~ selectOther ~ space ~ "}"
      ).map { case (paramName, parts, other) =>
        SelectFragment(
          paramName,
          parts.toMap,
          other
        )
      }

    def choiceFragmentExpr[$: P]: P[Seq[ChoiceFragment]] =
      P(
        (string | plural | select | stringParam | choicePlaceholder).rep
      )

    def fragmentExpr[$: P]: P[Seq[Fragment]] =
      P(
        (string | plural | select | stringParam).rep
      )

    def icuExpr[$: P]: P[Seq[Fragment]] =
      P(Start ~ fragmentExpr ~ space ~ End)

    // Remove whitespaces between consecutive open braces and close braces, as these are merely for code formating
    // reasons and should not be present in the export.

    val trimmedInput: String =
      input.trim.replaceAll("\\{\\s*\\{", "{{").replaceAll("}\\s*}", "}}")

    parse(trimmedInput, icuExpr(_))
  }

  private def stringChars(c: Char): Boolean =
    c != '#' && c != '{' && c != '}' && c != '\\'
}
