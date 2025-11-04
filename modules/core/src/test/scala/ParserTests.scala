import fastparse.Parsed
import icu4scala.AST._
import icu4scala.Parser

class ParserTests extends munit.FunSuite {
  test("parse broken icu expression (missing closing brace)") {
    val res = testParser(TestData.brokenICUExpressionMissingClosingBrace)
    assertEquals(res, Left("""Expected "}":6:74, found """""))
  }

  test("parse simple icu expression (hello)") {
    val res = testParser(TestData.simpleICUExpressionEN(0))
    assertEquals(
      res,
      Right(
        Seq(StringFragment("Hello, "), StringParam("name"), StringFragment("! (0)"))
      )
    )
  }

  test("translate complex icu expression (edgar)") {
    val translation = TestData
      .getTranslator("en", None)
      .map(
        _.apply(
          "main.test.complex",
          "gender_of_host" -> "male",
          "host" -> "Edgar",
          "num_guests" -> 27,
          "guest" -> "Emily"
        )
      )

    assertEquals(
      translation,
      Right(
        "Edgar invites Emily and 27 other people to his party. (0)"
      )
    )
  }

  test("translate unknown expression") {
    val translation = TestData
      .getTranslator("en", None)
      .map(
        _.apply(
          "main.test.unknown"
        )
      )

    assertEquals(
      translation,
      Right(
        "??main.test.unknown??"
      )
    )
  }

  test("get unknown language") {
    val translation = TestData
      .getTranslator("es", None)
      .map(
        _.apply(
          "main.test.unknown"
        )
      )

    assertEquals(
      translation,
      Right(
        "??main.test.unknown??"
      )
    )
  }

  test("get fallback language") {
    val translation = TestData
      .getTranslator("fr", Some("en"))
      .map(
        _.apply(
          "main.test.static"
        )
      )

    assertEquals(
      translation,
      Right(
        "i18n is fun! (0)"
      )
    )
  }

  private def testParser(input: String): Either[String, Seq[Fragment]] =
    parseResultToEither(Parser.parseICU(input))

  private def parseResultToEither[T](res: Parsed[T]): Either[String, T] =
    res.fold((_, _, e) => Left(e.trace(false).msg), (r, _) => Right(r))
}
