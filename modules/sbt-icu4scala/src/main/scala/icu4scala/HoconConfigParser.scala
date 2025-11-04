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

import java.io.File

import scala.jdk.CollectionConverters.asScalaSetConverter
import scala.jdk.CollectionConverters.collectionAsScalaIterableConverter
import scala.util.Try

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigUtil
import com.typesafe.config.ConfigValueType

object HoconConfigParser {
  def parse(
      files: Set[File],
      fallbackLanguage: Option[String]
  ): Either[Seq[BundleError], Bundle] =
    createSingleConfig(files).flatMap { config =>
      val entryResults = config.entrySet().asScala.toList.map { entry =>
        ConfigUtil.splitPath(entry.getKey).asScala.toList match {
          case Nil =>
            Left(ParseError(Nil, None, "No entries in config exists."))
          case languageCode :: Nil =>
            Left(
              ParseError(
                Nil,
                Some(languageCode),
                s"No entries for language '$languageCode' exists."
              )
            )
          case languageCode :: key =>
            entry.getValue.valueType() match {
              case ConfigValueType.STRING =>
                Right((languageCode, key, entry.getValue.unwrapped().asInstanceOf[String]))
              case cvt =>
                Left(
                  ParseError(
                    key,
                    Some(languageCode),
                    s"Invalid type for leaf object in config '${cvt.toString}'."
                  )
                )
            }
        }
      }

      val entryErrors = entryResults.collect { case Left(pe) => pe }
      val entries = entryResults.collect { case Right(e) => e }

      entryErrors match {
        case Nil =>
          val languageMap: Map[String, Map[Seq[String], String]] =
            entries.groupBy(_._1).map { case (languageCode, es) =>
              val entryMap: Map[Seq[String], String] = es.map(e => (e._2.toSeq, e._3)).toMap
              (languageCode, entryMap)
            }
          Bundle.buildBundle(languageMap, fallbackLanguage)
        case _ =>
          Left(entryErrors)
      }
    }

  private def createSingleConfig(
      files: Set[File]
  ): Either[Seq[FileError], Config] = {
    val fileConfigResults = files.toList.map(file =>
      Try(ConfigFactory.parseFile(file).resolve()).toEither.left.map(ex =>
        FileError(file.getPath, ex.getMessage)
      )
    )

    val fileErrors = fileConfigResults.collect { case Left(fileError) => fileError }
    val configs = fileConfigResults.collect { case Right(config) => config }

    fileErrors match {
      case Nil =>
        Right(configs.foldLeft(ConfigFactory.empty()) { case (acc, config) =>
          acc.withFallback(config)
        })
      case _ => Left(fileErrors)
    }
  }

}
