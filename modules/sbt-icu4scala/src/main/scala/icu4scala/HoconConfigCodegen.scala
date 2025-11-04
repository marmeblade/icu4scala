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
