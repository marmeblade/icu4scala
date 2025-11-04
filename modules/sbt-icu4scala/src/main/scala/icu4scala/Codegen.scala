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

import icu4scala.AST.*

import scala.annotation.nowarn

@nowarn
object Codegen {
  def build(
      bundle: StructuredBundle,
      packageName: String,
      indentation: String
  ): String = {
    val sb = new StringBuilder()
    val singleStringQuotation = """""""
    val tripleStringQuotation = """""""""

    val lsbs = bundle.availableLanguages.map {
      _ -> new StringBuilder()
    }.toMap

    def quoteName(name: String): String = {
      def isValidScalaName(name: String): Boolean = {
        // Regular expression for valid Scala identifiers
        val pattern = "^[a-zA-Z_][a-zA-Z0-9_]*$".r

        // List of Scala keywords
        val keywords = Set(
          "abstract",
          "case",
          "catch",
          "class",
          "def",
          "do",
          "else",
          "extends",
          "false",
          "final",
          "finally",
          "for",
          "forSome",
          "if",
          "implicit",
          "import",
          "lazy",
          "match",
          "new",
          "null",
          "object",
          "override",
          "package",
          "private",
          "protected",
          "return",
          "sealed",
          "super",
          "this",
          "throw",
          "trait",
          "try",
          "true",
          "type",
          "val",
          "var",
          "while",
          "with",
          "yield"
        )

        // Check if the name matches the pattern and is not a keyword
        name match {
          case pattern() if !keywords.contains(name) => true
          case _                                     => false
        }
      }

      if (isValidScalaName(name)) {
        name
      } else {
        s"""`$name`"""
      }
    }

    def buildBundleEntry(
        entry: BundleEntry,
        quotedName: Option[String],
        signature: Option[String]
    ): Unit = {

      def buildBody(
          lsb: StringBuilder,
          language: String,
          bodyIndentationOffset: Int,
          isRecursiveCall: Boolean,
          choiceParam: Option[String],
          fragments: Seq[ChoiceFragment]
      ): Unit = {
        val bodyIndentation = indentation.repeat(bodyIndentationOffset)

        fragments match {
          case StringFragment(value) +: Nil =>
            lsb
              .append(bodyIndentation)
              .append(tripleStringQuotation)
              .append(value)
              .append(tripleStringQuotation)
          case _ =>
            if (!isRecursiveCall) {
              lsb
                .append(bodyIndentation)
                .append("val sb = new StringBuilder()\n")
            }

            fragments.foreach {
              case StringFragment(value) =>
                lsb
                  .append(bodyIndentation)
                  .append("sb.append(")
                  .append(tripleStringQuotation)
                  .append(value)
                  .append(tripleStringQuotation)
                  .append(")\n")
              case ChoicePlaceholder =>
                lsb
                  .append(bodyIndentation)
                  .append("sb.append(")

                choiceParam match {
                  case Some(choiceParamName) =>
                    lsb.append(quoteName(choiceParamName))
                  case None =>
                    lsb
                      .append(tripleStringQuotation)
                      .append(BundleEntry.toUntranslatedKey("#"))
                      .append(tripleStringQuotation)
                }
                lsb.append(")\n")
              case StringParam(paramName) =>
                val quotedParam = quoteName(paramName)
                lsb
                  .append(bodyIndentation)
                  .append("sb.append(")
                  .append(quotedParam)
                  .append(")\n")
              case SelectFragment(paramName, choices, other) =>
                val quotedParam = quoteName(paramName)
                lsb
                  .append(bodyIndentation)
                  .append(quotedParam)
                  .append(" match {\n")

                choices.foreach { case (value, subFragments) =>
                  lsb
                    .append(bodyIndentation)
                    .append(indentation)
                    .append("case ")
                    .append(tripleStringQuotation)
                    .append(value)
                    .append(tripleStringQuotation)
                    .append(" =>\n")

                  buildBody(
                    lsb,
                    language,
                    bodyIndentationOffset + 2,
                    isRecursiveCall = true,
                    Some(paramName),
                    subFragments
                  )
                }

                lsb
                  .append(bodyIndentation)
                  .append(indentation)
                  .append("case _ =>\n")

                buildBody(
                  lsb,
                  language,
                  bodyIndentationOffset + 2,
                  isRecursiveCall = true,
                  Some(paramName),
                  other
                )

                lsb
                  .append(bodyIndentation)
                  .append("}\n")
              case PluralFragment(paramName, exact, zero, one, two, other) =>
                val quotedParam = quoteName(paramName)
                lsb
                  .append(bodyIndentation)
                  .append(quotedParam)
                  .append(" match {\n")

                exact.foreach { case (value, subFragments) =>
                  lsb
                    .append(bodyIndentation)
                    .append(indentation)
                    .append("case ")
                    .append(value)
                    .append(" =>\n")

                  buildBody(
                    lsb,
                    language,
                    bodyIndentationOffset + 2,
                    isRecursiveCall = true,
                    Some(paramName),
                    subFragments
                  )
                }

                zero.foreach { subFragments =>
                  lsb
                    .append(bodyIndentation)
                    .append(indentation)
                    .append("case 0 =>\n")

                  buildBody(
                    lsb,
                    language,
                    bodyIndentationOffset + 2,
                    isRecursiveCall = true,
                    Some(paramName),
                    subFragments
                  )
                }

                one.foreach { subFragments =>
                  lsb
                    .append(bodyIndentation)
                    .append(indentation)
                    .append("case 1 =>\n")

                  buildBody(
                    lsb,
                    language,
                    bodyIndentationOffset + 2,
                    isRecursiveCall = true,
                    Some(paramName),
                    subFragments
                  )
                }

                two.foreach { subFragments =>
                  lsb
                    .append(bodyIndentation)
                    .append(indentation)
                    .append("case 2 =>\n")

                  buildBody(
                    lsb,
                    language,
                    bodyIndentationOffset + 2,
                    isRecursiveCall = true,
                    Some(paramName),
                    subFragments
                  )
                }

                lsb
                  .append(bodyIndentation)
                  .append(indentation)
                  .append("case _ =>\n")

                buildBody(
                  lsb,
                  language,
                  bodyIndentationOffset + 2,
                  isRecursiveCall = true,
                  Some(paramName),
                  other
                )

                lsb
                  .append(bodyIndentation)
                  .append("}\n")
              case x =>
                lsb
                  .append(bodyIndentation)
                  .append(x.getClass.toString)
                  .append("?\n")
            }

            if (!isRecursiveCall) {
              lsb.append(bodyIndentation).append("sb.toString()")
            }
        }
      }

      val baseIndentationOffset =
        entry.key.length + quotedName.map(_ => 1).getOrElse(2)
      val entryIndentation = indentation.repeat(baseIndentationOffset)

      lsbs.map { case (language, lsb) =>
        val (defType, sig, name) = (signature, quotedName) match {
          case (Some(sig), Some(nm)) => ("def ", sig, nm)
          case (Some(sig), None)     => ("def ", sig, "apply")
          case (None, Some(nm))      => ("val ", "", nm)
          case (None, None)          => ("def ", "()", "apply")
        }

        lsb
          .append(entryIndentation)
          .append(defType)
          .append(name)
          .append(sig)
          .append(": String = {\n")

        entry.languageMap.get(language) match {
          case Some(fragments) =>
            buildBody(
              lsb,
              language,
              baseIndentationOffset + 1,
              isRecursiveCall = false,
              None,
              fragments
            )
          case None =>
            lsb
              .append(entryIndentation)
              .append(indentation)

            bundle.fallbackLanguage match {
              case Some(fallbackLanguage) if fallbackLanguage != language =>
                lsb
                  .append(fallbackLanguage)
                  .append(".")
                  .append(entry.keyString)
                  .append("(")
                  .append(
                    entry.params.map(p => quoteName(p.paramName)).mkString(", ")
                  )
                  .append(")")
              case _ =>
                lsb
                  .append(tripleStringQuotation)
                  .append(BundleEntry.toUntranslatedKey(entry.keyString))
                  .append(tripleStringQuotation)
            }
        }

        lsb
          .append("\n")
          .append(entryIndentation)
          .append("}\n")
      }
    }

    def buildStructuredEntry(entry: StructuredBundleEntry): Unit = {
      val memberName = entry.key.last
      val quotedName = quoteName(memberName)
      val memberIndentation = indentation.repeat(entry.key.length + 1)

      val bundleEntryWithSignature = entry.entry.map { e =>
        val paramString = e.params match {
          case Nil => None
          case params =>
            Some(
              "(" + params
                .map {
                  case StringParam(paramName) =>
                    quoteName(paramName) + ": String"
                  case IntegerParam(paramName) =>
                    quoteName(paramName) + ": Long"
                }
                .mkString(", ") + ")"
            )
        }
        (e, paramString)
      }

      entry.subEntries match {
        case Nil =>
          // In that case it is a def in abstract

          bundleEntryWithSignature.foreach { case (bundleEntry, optionSig) =>
            sb.append(memberIndentation)
              .append("def ")
              .append(quotedName)
            optionSig.foreach(sb.append)
            sb.append(": String\n")

            buildBundleEntry(bundleEntry, Some(quotedName), optionSig)
          }
        case subEntries =>
          val className = quoteName(memberName.capitalize)
          // Start block for abstract string builder
          sb.append("\n")
            .append(memberIndentation)
            .append("abstract class ")
            .append(className)
            .append(" {\n")

          // Start block for language string builders
          lsbs.map { case (_, lsb) =>
            lsb
              .append(memberIndentation)
              .append("object ")
              .append(quotedName)
              .append(" extends ")
              .append(className)
              .append(" {\n")
          }

          // Here we have to build each language structure and also, if necessary, the apply method for the abstract and
          // every language

          bundleEntryWithSignature.foreach { case (bundleEntry, optionSig) =>
            sb.append(memberIndentation)
              .append(indentation)
              .append("def apply")
              .append(optionSig.getOrElse("()"))
              .append(": String\n")

            buildBundleEntry(bundleEntry, None, optionSig)
          }

          // Recursion point
          subEntries.foreach(buildStructuredEntry)

          // End block for abstract string builder
          sb.append(memberIndentation).append("}\n")
          sb.append(memberIndentation)
            .append("def ")
            .append(memberName)
            .append(": ")
            .append(className)
            .append("\n")

          // End block for language string builders

          lsbs.map { case (_, lsb) =>
            lsb
              .append(memberIndentation)
              .append("}\n")
          }
      }
    }

    sb.append("package ").append(packageName).append("\n\n")
    sb.append("object Bundle {\n")

    sb.append(indentation).append("abstract class I18N {\n")
    bundle.entries.foreach(buildStructuredEntry)
    sb.append(indentation).append("}\n\n")

    lsbs.foreach { case (lang, lsb) =>
      sb.append(indentation)
        .append("object ")
        .append(lang)
        .append(" extends I18N {\n")
      sb.append(lsb)
      sb.append(indentation).append("}\n\n")
    }

    sb.append(indentation).append("val languages: Map[String, I18N] = Map(\n")

    bundle.availableLanguages.zipWithIndex.foreach { case (lang, idx) =>
      sb.append(indentation)
        .append(indentation)
        .append(singleStringQuotation)
        .append(lang)
        .append(singleStringQuotation)
        .append(" -> ")
        .append(lang)

      if (idx + 1 != bundle.availableLanguages.size) {
        sb.append(",")
      }
      sb.append("\n")
    }

    sb.append(indentation).append(")\n}")
    sb.toString()
  }
}
