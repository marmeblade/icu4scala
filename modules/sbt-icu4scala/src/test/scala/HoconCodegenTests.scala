import icu4scala.HoconConfigCodegen

import java.nio.file.{Files, Path}
import scala.io.Source

class HoconCodegenTests extends munit.FunSuite {

  test("test 4 language project with generated hocon config and code generation") {
    val hoconStringEn = generateHoconString("en", 30)
    val hoconStringDe = generateHoconString("de", 20)
    val hoconStringFr = generateHoconString("fr", 10)
    val hoconStringEs = generateHoconString("es", 10)

    var tempFileEn: Path = null
    var tempFileDe: Path = null
    var tempFileFr: Path = null
    var tempFileEs: Path = null
    try {
      tempFileEn = Files.createTempFile("en-", ".conf")
      Files.write(tempFileEn, hoconStringEn.getBytes("UTF-8"))

      tempFileDe = Files.createTempFile("de-", ".conf")
      Files.write(tempFileDe, hoconStringDe.getBytes("UTF-8"))

      tempFileFr = Files.createTempFile("fr-", ".conf")
      Files.write(tempFileFr, hoconStringFr.getBytes("UTF-8"))

      tempFileEs = Files.createTempFile("es-", ".conf")
      Files.write(tempFileEs, hoconStringEs.getBytes("UTF-8"))

      val files = Set(tempFileEn.toFile, tempFileDe.toFile, tempFileFr.toFile, tempFileEs.toFile)

      val result = HoconConfigCodegen.gen(files, None, "com.example")

      result match {
        case Right(Some(bundleCode)) =>
//          saveResource("ExpectedBundle.scala.txt", bundleCode)
          val expectedBundleCode = loadResource("ExpectedBundle.scala.txt")

          assertEquals(bundleCode, expectedBundleCode)
        case Right(None) =>
          fail("No output generated!")
        case Left(errors) =>
          fail(errors.map(d => d.toString).mkString(", "))
      }
    } finally {
//      val deleteEn = if (tempFileEn != null) Files.deleteIfExists(tempFileEn) else true
//      val deleteDe = if (tempFileDe != null) Files.deleteIfExists(tempFileDe) else true
//      val deleteFr = if (tempFileFr != null) Files.deleteIfExists(tempFileFr) else true
//      val deleteEs = if (tempFileEs != null) Files.deleteIfExists(tempFileEs) else true
//
//      assert(deleteEn)
//      assert(deleteDe)
//      assert(deleteFr)
//      assert(deleteEs)
    }
  }

  private def loadResource(path: String): String = {
    val classLoader = getClass.getClassLoader
    val inputStream = classLoader.getResourceAsStream(path)

    if (inputStream == null) {
      throw new IllegalArgumentException(s"Resource not found: $path")
    }

    val source = Source.fromInputStream(inputStream)
    try source.mkString
    finally source.close()
  }

//  private def saveResource(relativePath: String, content: String): Unit = {
//    val absolutePath = Paths.get(s"modules/sbt-icu4scala/src/test/resources/$relativePath")
//    Files.write(absolutePath, content.getBytes(StandardCharsets.UTF_8))
//    println(s"New reference file written to: ${absolutePath.toAbsolutePath}")
//  }

  private def generateHoconString(languageCode: String, numEntries: Int): String = {
    def optionToHoconString(data: Option[String]): String = data match {
      case Some(d) => "\"\"\"" + d + "\"\"\""
      case None    => "null"
    }

    val entries = (1 to numEntries).map { i =>
      val paddedIdx = padNumber(i, numEntries)

      val (staticOption, simpleOption, complexOption) = languageCode match {
        case "en" =>
          (
            Some(TestData.staticICUExpressionEN(i)),
            Some(TestData.simpleICUExpressionEN(i)),
            Some(TestData.complexICUExpressionEN(i))
          )
        case "de" =>
          (
            Some(TestData.staticICUExpressionDE(i)),
            Some(TestData.simpleICUExpressionDE(i)),
            Some(TestData.complexICUExpressionDE(i))
          )
        case "fr" =>
          (
            None,
            Some(TestData.simpleICUExpressionFR(i)),
            None
          )
        case "es" =>
          (
            None,
            None,
            Some(TestData.complexICUExpressionES(i))
          )
        case _ => (None, None, None)
      }

      val static = optionToHoconString(staticOption)
      val simple = optionToHoconString(simpleOption)
      val complex = optionToHoconString(complexOption)

      s"""  key$paddedIdx {
         |    static = $static
         |    simple = $simple
         |    complex = $complex
         |  }
         |""".stripMargin
    }

    s"""$languageCode {
       |${entries.mkString("\n")}
       |}
       |""".stripMargin
  }

  private def padNumber(number: Int, numEntries: Int): String =
    s"%0${numEntries.toString.length}d".format(number)
}
