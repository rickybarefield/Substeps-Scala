package com.technophobia.substeps.parsing

import _root_.com.technophobia.substeps.domain._
import scala.Some
import com.technophobia.substeps.domain.repositories.SubstepRepository

class FeatureFileParser(protected val substepRepository: SubstepRepository) extends AbstractParser[Feature] {

  protected override def entryPoint = featureFile

  Oh isn't it features which have backgrounds???

  private def featureFile: Parser[Feature] = opt(tagDef <~ rep1(eol)) ~ (featureDef <~ rep1(eol)) ~ opt(background) ~ (rep(scenario) <~ rep(eol)) ^^ {

    case (Some(tags) ~ optBackground ~ featureName ~ scenarios) => Feature(featureName, scenarios, tags.toSet)
    case (None ~ optBackground ~ featureName ~ scenarios) => Feature(featureName, scenarios, Set())
  }

  private def tagDef: Parser[List[Tag]] = opt(whiteSpace) ~> "Tags:" ~> opt(whiteSpace) ~> repsep(tag, whiteSpace)

  private def tag: Parser[Tag]   = """[1-9A-Za-z-_~]+""".r

  private def featureDef: Parser[String] = opt(whiteSpace) ~> "Feature:" ~> opt(whiteSpace) ~> """[^\r\n]+""".r

  private def scenario: Parser[Scenario] = basicScenario | scenarioOutline

  private def basicScenario: Parser[BasicScenario] = (opt(tagDef <~ rep1(eol)) ~ scenarioDef <~ rep1(eol)) ~ rep1sep(substepInvocation, rep1(eol)) <~ rep(eol) ^^ {

    case (Some(tags) ~ scenarioName ~ substepInvocations) => BasicScenario(substepRepository, scenarioName, substepInvocations, tags.toSet)
    case (None ~ scenarioName ~ substepInvocations) => BasicScenario(substepRepository, scenarioName, substepInvocations, Set[Tag]())
  }

  def substepInvocation: Parser[String] = """([^:\r\n])+""".r ^^ (_.trim)

  private def scenarioDef: Parser[String] = opt(whiteSpace) ~> "Scenario:" ~> opt(whiteSpace) ~> """[^\n\r]+""".r

  private def scenarioOutline: Parser[OutlinedScenario] = opt(tagDef <~ rep1(eol)) ~ (scenarioOutlineDef <~ rep1(eol)) ~ (rep1sep(substepInvocation, eol) <~ rep(eol)) ~ exampleSection <~ rep(eol) ^^ {

    case (Some(tags) ~ scenarioName ~ substeps ~ examples) => OutlinedScenario(substepRepository, scenarioName, substeps, examples, tags.toSet)
    case (None ~ scenarioName ~ substeps ~ examples) => OutlinedScenario(substepRepository, scenarioName, substeps, examples, Set())

  }

  private def scenarioOutlineDef: Parser[String] = "Scenario Outline:" ~> opt(whiteSpace) ~> """[^\n\r]+""".r

  private def exampleSection: Parser[List[Map[String, String]]] = ("Examples:" ~ opt(whiteSpace) ~ rep1(eol)) ~> (lineOfCells <~ rep1(eol)) ~ repsep(lineOfCells, eol) ^^ {

    case (headings ~ examples) => for(example <- examples; examplesWithHeading = headings.zip(example)) yield Map(examplesWithHeading:_*)
  }

  private def lineOfCells: Parser[List[String]] = rep(cellSeparator ~> """[^|\n\r ]*(?=([ ]*\|))""".r) <~ cellSeparator

  private def cellSeparator: Parser[Any] = opt(whiteSpace) ~ "|" ~ opt(whiteSpace)

}