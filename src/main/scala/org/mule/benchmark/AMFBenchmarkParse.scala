package org.mule.benchmark

import java.io.File

import amf.client.AMF
import org.mule.amf.AmfParsingHelper
import org.mule.core.Specs
import org.scalameter.Bench.LocalTime
import org.scalameter.api._
import org.scalameter.picklers.Implicits._

object AMFBenchmarkParse
  extends LocalTime{

  /* configuration */

  override lazy val executor = LocalExecutor(
    new Executor.Warmer.Default,
    Aggregator.median[Double],
    measurer)

//  override lazy val aggregator = Aggregator.median[Double]
  override lazy val measurer = new Measurer.Default
  override lazy val reporter = new AMFLogReporter[Double]
//  override lazy val reporter =  new HtmlReporter()
  override lazy val persistor: Persistor.None.type = Persistor.None

  /* inputs */

  val inputFile: Gen[String] = Gen.enumeration("inputFile")(
    "src/main/resources/raml08/only_title.raml",
    "src/main/resources/raml08/longest_valid_platform.raml",
    "src/main/resources/raml10/longest_valid_platform.raml",
    "src/main/resources/raml10/longest_valid_tck.raml",
    "src/main/resources/raml08/longest_valid_tck.raml",
    "src/main/resources/raml08/longest_platform.raml",
    "src/main/resources/raml10/longest_platform.raml",
    "src/main/resources/raml10/only_title.raml")

  /* initialization */
  AMF.init().get()

  /* tests */

  performance of "AMF" in {
    measure method "parseFileAsync" in {
      using(inputFile) config (
        exec.benchRuns -> 3,
        exec.maxWarmupRuns -> 3,
      ) in {
        f => testParse(f)
      }
    }
  }

  def testParse(file: String): Unit = {
    testParse(new File(file))
  }

  def testParse(file: File): Unit ={
    val apiKind = Specs.getApiKind(file)
    AmfParsingHelper.handleParse(file, apiKind) match {
      case Right(_) => // do nothing
      case Left(e) => AMFBenchmarkCommon.printError(s"AMF PARSING ERROR: ${e.getMessage}", e, file)
    }
  }
}