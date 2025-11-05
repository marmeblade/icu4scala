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

import fastparse.Parsed
import icu4scala.AST._

sealed trait BundleError

case class FileError(path: String, errorMessage: String) extends BundleError

case class ParseError(
    key: Seq[String],
    language: Option[String],
    errorMessage: String
) extends BundleError

case class ParamError(
    existingParamLanguage: String,
    existingParam: Param,
    collidingParamLanguage: String,
    collidingParam: Param
) extends BundleError

case class FallbackLanguageMissing(
    language: String
) extends BundleError

case class BundleEntry(
    key: Seq[String],
    languageMap: Map[String, Seq[Fragment]],
    params: Seq[Param]
) {
  lazy val keyString: String = key.mkString(".")

  def trans(
      language: String,
      fallbackLanguage: Option[String],
      params: Map[String, Any]
  ): String = {
    languageMap.get(language) orElse fallbackLanguage.flatMap(
      languageMap.get
    ) match {
      case Some(fragments) =>
        transImpl(
          new StringBuilder,
          fragments,
          params
        )
      case None => BundleEntry.toUntranslatedKey(keyString)
    }
  }

  private def transImpl(
      sb: StringBuilder,
      fragments: Seq[Fragment],
      params: Map[String, Any]
  ): String = {
    def unknownParamToString(paramName: String): String =
      "{??" + paramName + "??}"

    def transPlural(plural: PluralFragment): Unit = {
      val longParam = params.get(plural.paramName) match {
        case Some(n: Int)   => Some(n.toLong)
        case Some(n: Short) => Some(n.toLong)
        case Some(n: Long)  => Some(n)
        case _              => None
      }

      longParam match {
        case Some(pluralLong) =>
          (
            plural.exact.get(pluralLong),
            plural.zero,
            plural.one,
            plural.two
          ) match {
            case (Some(fragments), _, _, _) =>
              transChoiceFragments(pluralLong, fragments)
            case (_, Some(fragments), _, _) if pluralLong == 0 =>
              transChoiceFragments(pluralLong, fragments)
            case (_, _, Some(fragments), _) if pluralLong == 1 =>
              transChoiceFragments(pluralLong, fragments)
            case (_, _, _, Some(fragments)) if pluralLong == 2 =>
              transChoiceFragments(pluralLong, fragments)
            case _ => transChoiceFragments(pluralLong, plural.other)
          }
        case None =>
          sb.append(unknownParamToString(plural.paramName))
      }
      ()
    }

    def transSelect(select: SelectFragment): Unit = {
      params.get(select.paramName) match {
        case Some(p) =>
          val stringParam = p.toString

          select.choices.get(stringParam) match {
            case Some(fragments) => transFragments(fragments)
            case None            => transFragments(select.other)
          }
        case None =>
          sb.append(unknownParamToString(select.paramName))
      }
      ()
    }

    def transFragment(fragment: Fragment): Unit = {
      fragment match {
        case StringFragment(str) => sb.append(str)
        case StringParam(paramName) =>
          val renderedValue = params.get(paramName) match {
            case Some(value) => value.toString
            case None        => unknownParamToString(paramName)
          }

          sb.append(renderedValue)
          ()
        case plural: PluralFragment => transPlural(plural)
        case select: SelectFragment => transSelect(select)
      }
      ()
    }

    def transChoiceFragments(
        parentPlural: Long,
        fragments: Seq[ChoiceFragment]
    ): Unit =
      fragments.foreach {
        case ChoicePlaceholder  => sb.append(parentPlural)
        case fragment: Fragment => transFragment(fragment)
      }

    def transFragments(fragments: Seq[Fragment]): Unit =
      fragments.foreach(transFragment)

    transFragments(fragments)
    sb.toString()
  }
}

object BundleEntry {
  def toUntranslatedKey(key: String): String = "??" + key + "??"
}

/** The primary structure to translate from. The Bundle is generated from a structured map of
  * language keyed property maps to ICU expressions, that will be parsed into an AST representing
  * said ICU expression (@see[[Bundle.buildBundle]]). The bundle serves as a data store to this AST
  * as well as an API to directly translate from using property keys (e.g. x.y.z) and a named and
  * typed parameter list to translate using classic stringly typed translation methods.
  *
  * The underlying AST can also be used to construct scala code to create typesafe translation
  * objects with typed parameters as methods. This is present in the sbt project.
  *
  * @param entries
  *   A map of a property key (e.g. "x.y.z") to a Bundle Entry, containing all translations for a
  *   specific keyed translation.
  */
case class Bundle(
    entries: Map[String, BundleEntry],
    fallbackLanguage: Option[String]
) {
  case class Translator(language: String) {
    @nowarn
    def apply(key: String): String = apply(key, Seq(): _*)

    def apply(key: String, params: (String, Any)*): String = {
      entries.get(key) match {
        case Some(bundleEntry) =>
          bundleEntry.trans(language, fallbackLanguage, params.toMap)
        case None => BundleEntry.toUntranslatedKey(key)
      }
    }
  }

  def getTranslator(language: String): Translator = Translator(language)

  private lazy val availableLanguages: Set[String] =
    entries.flatMap(_._2.languageMap.keys).toSet

  lazy val structuredBundle: StructuredBundle = {
    def inner(
        depth: Int,
        entries: Seq[BundleEntry]
    ): Seq[StructuredBundleEntry] = {
      val entryGroups = entries.groupBy { e =>
        // This is necessary for the group by mechanism to work if the depth is greater than the number of key elements.
        // This way these entries will remain in their own group by adding a unique part in the group by clause.
        (e.key.take(depth), if (depth > e.key.length) Some(e.key) else None)
      }

//      println((depth, entryGroups.keys))

      entryGroups.toSeq.collect { case ((nextPath, None), matchingEntries) =>
        val exactEntry = matchingEntries.find(_.key == nextPath)

        val subEntries =
          if (exactEntry.isDefined && matchingEntries.length == 1) {
            Seq()
          } else {
            inner(depth + 1, matchingEntries)
          }

        StructuredBundleEntry(
          nextPath,
          exactEntry,
          subEntries
        )
      }
    }

    StructuredBundle(
      inner(1, entries.values.toSeq),
      availableLanguages,
      fallbackLanguage
    )
  }
}

object Bundle {

  /** Constructs a bundle from a whole structure of keyed ICU expressions of different languages.
    * The structure represent an entire bundle of translations usually parsed from multiple files.
    * As there are a plethora of ways to parse files and their contents, especially property and
    * config files that would fit the bill here, the building of that said "languageMap" structure
    * is the concern of the user of this library. The result is either a sequence of errors showing
    * parsing errors or logic errors with the ICU params, or on success a complete @see[[Bundle]].
    *
    * @param languageMap
    *   A map from language code to a map of a string path (e.g. x.y.z = Seq(x,y,z)) to a ICU
    *   expressions
    * @return
    */
  def buildBundle(
      languageMap: Map[String, Map[Seq[String], String]],
      fallbackLanguage: Option[String]
  ): Either[Seq[BundleError], Bundle] = {
    val parseResults = languageMap.flatMap { case (languageCode, stringEntryMap) =>
      stringEntryMap.map { case (key, icuString) =>
        val parseResult =
          Parser
            .parseICU(icuString)
            .fold(
              (label, index, extra) => Left(Parsed.Failure(label, index, extra)),
              (fragments, _) => Right(fragments)
            )

        (key, languageCode, parseResult)
      }
    }

    val entriesResult: Seq[Either[Seq[BundleError], (String, BundleEntry)]] =
      parseResults.groupBy(_._1).toSeq.map { case (key, parsedEntries) =>
        // Parses and checks all language translations of a single entry
        val languageMap =
          parsedEntries.collect { case (_, language, Right(fragments)) =>
            (language, fragments)
          }.toMap

        val parseErrors =
          parsedEntries.collect { case (key, language, Left(parseFailure)) =>
            ParseError(key, Some(language), parseFailure.toString())
          }.toSeq

        val paramsResult = checkAndCreateParamsList(languageMap)

        (parseErrors, paramsResult) match {
          case (Nil, Right(params)) =>
            val entry = BundleEntry(key, languageMap, params)

            Right(entry.keyString -> entry)
          case (parseErrors, Right(_)) => Left(parseErrors)
          case (parseErrors, Left(paramErrors)) =>
            Left(parseErrors ++ paramErrors)
        }
      }

    val entriesErrors = entriesResult.collect { case Left(x) =>
      x
    }

    val entriesSuccesses = entriesResult.collect { case Right(x) =>
      x
    }

    entriesErrors.flatten match {
      case Nil =>
        fallbackLanguage match {
          case Some(lang)
              if !entriesSuccesses.exists(
                _._2.languageMap.keys.exists(_ == lang)
              ) =>
            Left(Seq(FallbackLanguageMissing(lang)))
          case _ => Right(Bundle(entriesSuccesses.toMap, fallbackLanguage))
        }
      case errors => Left(errors)
    }
  }

  private def checkAndCreateParamsList(
      languageMap: Map[String, Seq[Fragment]]
  ): Either[Seq[ParamError], Seq[Param]] = {
    def getParamsRec(fragments: Seq[ChoiceFragment]): Seq[Param] =
      fragments.flatMap {
        case p: Param => Seq(p)
        case SelectFragment(paramName, choices, other) =>
          getParamsRec(choices.values.flatten.toSeq ++ other) :+ StringParam(
            paramName
          )
        case PluralFragment(
              paramName,
              exact,
              zero,
              one,
              two,
              other
            ) =>
          getParamsRec(
            exact.values.flatten.toSeq ++ zero.toSeq.flatten ++ one.toSeq.flatten ++ two.toSeq.flatten ++ other
          ) :+ IntegerParam(paramName)
        case _ => Seq()
      }

    def condenseParamsList(
        params: Seq[(String, Param)]
    ): Either[Seq[ParamError], Seq[Param]] = {
      val (errors, paramMap) =
        params.foldLeft(
          (Seq.empty[ParamError], Map.empty[String, (String, Param)])
        ) { case ((errors, paramMap), (paramLanguage, param)) =>
          paramMap.get(param.paramName) match {
            case Some((_, existingParam)) if existingParam == param =>
              (errors, paramMap)
            case Some((existingParamLanguage, existingParam)) =>
              (
                errors :+ ParamError(
                  existingParamLanguage,
                  existingParam,
                  paramLanguage,
                  param
                ),
                paramMap
              )
            case None =>
              (errors, paramMap + (param.paramName -> ((paramLanguage, param))))
          }
        }

      errors match {
        case Nil => Right(paramMap.values.toSeq.map(_._2))
        case _   => Left(errors)
      }
    }

    val languageParamPairs = languageMap.toSeq.flatMap { case (language, fragments) =>
      getParamsRec(fragments).map((language, _))
    }

    condenseParamsList(languageParamPairs)
  }
}

case class StructuredBundle(
    entries: Seq[StructuredBundleEntry],
    availableLanguages: Set[String],
    fallbackLanguage: Option[String]
)

case class StructuredBundleEntry(
    key: Seq[String],
    entry: Option[BundleEntry],
    subEntries: Seq[StructuredBundleEntry]
)
