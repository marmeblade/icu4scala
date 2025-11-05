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

object HoconConfigCodegen {
  def gen(
      files: Set[File],
      fallbackLanguage: Option[String],
      packageName: String,
      indentation: String = "  "
  ): Either[Seq[BundleError], Option[String]] =
    HoconConfigParser.parse(files, fallbackLanguage).map { bundle =>
      if (bundle.entries.isEmpty) {
        None
      } else {
        Some(Codegen.build(bundle.structuredBundle, packageName, indentation))
      }
    }
}
