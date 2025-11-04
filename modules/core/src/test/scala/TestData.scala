import icu4scala.{Bundle, BundleError}

object TestData {
  def staticICUExpressionEN(index: Int): String =
    s"""i18n is fun! ($index)"""

  def staticICUExpressionDE(index: Int): String =
    s"""i18n macht Spaß! ($index)"""

  def simpleICUExpressionEN(index: Int): String =
    s"""Hello, {name}! ($index)"""

  def simpleICUExpressionDE(index: Int): String =
    s"""Hallöchen, {name}! ($index)"""

  def simpleICUExpressionFR(index: Int): String =
    s"""Bonjour, {name}! ($index)"""

  def complexICUExpressionEN(index: Int): String =
    s"""{gender_of_host, select,
      |  female {
      |    {num_guests, plural,
      |      =0 {{host} does not give a party. ($index)}
      |      =1 {{host} invites {guest} to her party. ($index)}
      |      =2 {{host} invites {guest} and one other person to her party. ($index)}
      |      other {{host} invites {guest} and # other people to her party. ($index)}}}
      |  male {
      |    {num_guests, plural,
      |      =0 {{host} does not give a party. ($index)}
      |      =1 {{host} invites {guest} to his party. ($index)}
      |      =2 {{host} invites {guest} and one other person to his party. ($index)}
      |      other {{host} invites {guest} and # other people to his party. ($index)}}}
      |  other {
      |    {num_guests, plural,
      |      =0 {{host} does not give a party. ($index)}
      |      =1 {{host} invites {guest} to their party. ($index)}
      |      =2 {{host} invites {guest} and one other person to their party. ($index)}
      |      other {{host} invites {guest} and # other people to their party. ($index)}}}}
      |""".stripMargin

  def complexICUExpressionDE(index: Int): String =
    s"""{gender_of_host, select,
      |  female {
      |    {num_guests, plural,
      |      =0 {{host} lädt nicht zu einer Party ein. ($index)}
      |      =1 {{host} lädt {guest} zu ihrer Party ein. ($index)}
      |      =2 {{host} lädt {guest} und eine andere Person zu ihrer Party ein. ($index)}
      |      other {{host} lädt {guest} und # andere Personen zu ihrer Party ein. ($index)}}}
      |  male {
      |    {num_guests, plural,
      |      =0 {{host} lädt nicht zu einer Party ein. ($index)}
      |      =1 {{host} lädt {guest} zu seiner Party ein. ($index)}
      |      =2 {{host} lädt {guest} und eine andere Person zu seiner Party ein. ($index)}
      |      other {{host} lädt {guest} und # andere Personen zu seiner Party ein. ($index)}}}
      |  other {
      |    {num_guests, plural,
      |      =0 {{host} lädt nicht zu einer Party ein. ($index)}
      |      =1 {{host} lädt {guest} zu ihrer Party ein. ($index)}
      |      =2 {{host} lädt {guest} und eine andere Person zu ihrer Party ein. ($index)}
      |      other {{host} lädt {guest} und # andere Personen zu ihrer Party ein. ($index)}}}}
      |""".stripMargin

  def complexICUExpressionES(index: Int): String =
    s"""{gender_of_host, select,
      |  female {
      |    {num_guests, plural,
      |      =0 {{host} no da una fiesta. ($index)}
      |      =1 {{host} invita a {guest} a su fiesta. ($index)}
      |      =2 {{host} invita a {guest} y a otra persona a su fiesta. ($index)}
      |      other {{host} invita a {guest} y a # personas más a su fiesta. ($index)}}}
      |  male {
      |    {num_guests, plural,
      |      =0 {{host} no da una fiesta. ($index)}
      |      =1 {{host} invita a {guest} a su fiesta. ($index)}
      |      =2 {{host} invita a {guest} y a otra persona a su fiesta. ($index)}
      |      other {{host} invita a {guest} y a # personas más a su fiesta. ($index)}}}
      |  other {
      |    {num_guests, plural,
      |      =0 {{host} no da una fiesta. ($index)}
      |      =1 {{host} invita a {guest} a su fiesta. ($index)}
      |      =2 {{host} invita a {guest} y a otra persona a su fiesta. ($index)}
      |      other {{host} invita a {guest} y a # personas más a su fiesta. ($index)}}}}
      |""".stripMargin

  val brokenICUExpressionMissingClosingBrace: String =
    """{gender_of_host, select,
      |  other {
      |    {num_guests, plural,
      |      =0 {{host} does not give a party.}
      |      =1 {{host} invites {guest} to their party.}
      |      =2 {{host} invites {guest} and one other person to their party.}
      |      other {{host} invites {guest} and # other people to their party.}}}
      |""".stripMargin

  def getTranslator(
      language: String,
      fallbackLanguage: Option[String]
  ): Either[Seq[BundleError], Bundle#Translator] =
    Bundle
      .buildBundle(
        Seq(
          "en" -> Seq(
            Seq("main", "test", "static") -> TestData.staticICUExpressionEN(0),
            Seq("main", "test", "simple") -> TestData.simpleICUExpressionEN(0),
            Seq("main", "test", "complex") -> TestData.complexICUExpressionEN(0)
          ).toMap,
          "de" -> Seq(
            Seq("main", "test", "simple") -> TestData.simpleICUExpressionDE(0)
          ).toMap,
          "fr" -> Seq(
            Seq("main", "test", "simple") -> TestData.simpleICUExpressionFR(0)
          ).toMap
        ).toMap,
        fallbackLanguage
      )
      .map(_.getTranslator(language))
}
