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

import sbt.Keys._
import sbt._
import sbt.internal.io.Source
import sbt.plugins.JvmPlugin

//noinspection ScalaUnusedSymbol
object Icu4ScalaPlugin extends AutoPlugin {

  override def trigger = allRequirements
  override def requires = JvmPlugin

  // noinspection ScalaWeakerAccess
  object autoImport {
    val icuBundlePackageName =
      settingKey[String]("Package name for the ICU bundle.")
    val generateIcuBundleTask =
      taskKey[Seq[File]]("The ICU bundle generation task.")
//    val icuBreakOnMissingKeys =
//      settingKey[Boolean]("Option to break generation task on missing keys in configuration.")

  }

  import autoImport.*
  private val icuSource =
    settingKey[String](
      "Sub-Directory (in src/main) containing ICU sources (standard: icu)."
    )

  override lazy val projectSettings: Seq[Def.Setting[?]] =
    inConfig(Compile)(
      watchSourceSettings ++
        Seq(
          icuSource := "icu",
          generateIcuBundleTask := generateFromSource(
            streams.value,
            sourceDirectory.value / icuSource.value,
            streams.value.cacheDirectory / icuSource.value,
            sourceManaged.value / "sbt-icu4scala",
            icuBundlePackageName.value
          ),
          packageSrc / mappings ++= managedSources.value pair (Path
            .relativeTo(sourceManaged.value) | Path.flat),
          sourceGenerators += generateIcuBundleTask.taskValue
        )
    ) ++ Seq(icuBundlePackageName := "org.example.icu")

  private def generateFromSource(
      streams: TaskStreams,
      srcDir: File,
      cacheDir: File,
      outDir: File,
      packageName: String
  ): Seq[File] = {
    val cachedFun = FileFunction.cached(
      cacheDir
    ) { (inputs: Set[File]) =>
      if (inputs.nonEmpty)
        streams.log.info(
          s"Generating new ICU bundle in package $packageName."
        )
      streams.log.debug(s"Found ${inputs.size} template files in $srcDir.")
      val bundleFile = outDir / "Bundle.scala"

      HoconConfigCodegen.gen(inputs, None, packageName) match {
        case Right(Some(bundleCode)) =>
          IO.write(bundleFile, bundleCode)
          streams.log.info(s"ICU bundle created in '${bundleFile.getPath}'.")
          Set(bundleFile)
        case Right(None) =>
          streams.log.info(s"No ICU data present. No bundle was created.")
          Set.empty
        case Left(bundleErrors) =>
          bundleErrors.foreach {
            case FileError(path, errorMessage) =>
              streams.log.error(s"ICU: Error reading file '$path': $errorMessage")
            case ParseError(key, language, errorMessage) =>
              streams.log.error(
                s"ICU: Error parsing key '${key.mkString(".")}' for language " +
                  s"'${language.getOrElse("-")}': $errorMessage"
              )
            case ParamError(
                  existingParamLanguage,
                  existingParam,
                  collidingParamLanguage,
                  collidingParam
                ) =>
              streams.log.error(
                s"ICU: Error with colliding param '${collidingParam.paramName}' " +
                  s"in language '$collidingParamLanguage' and existing param " +
                  s"'${existingParam.paramName}' in language '$existingParamLanguage'"
              )
            case FallbackLanguageMissing(language) =>
              s"ICU: Error missing fallback language '$language'"
          }
          Set.empty
      }
    }

    // put the input file into a `Set` (as required by `cachedFun`),
    // pass it to the `cachedFun`,
    // convert the result to `Seq` (as required by `Def.task`)
    cachedFun(srcDir.allPaths.get.toSet).toSeq
  }

  private def watchSourceSettings =
    Def.settings(
      Seq(
        watchSources in Defaults.ConfigGlobal += new Source(
          sourceDirectory.value / icuSource.value,
          AllPassFilter,
          NothingFilter
        )
      )
    )
}
