package org.mule.benchmark

import java.io.File

import amf.AMF
import amf.client.model.document.BaseUnit
import org.mule.amf.{AmfParsingHelper, AmfValidationHelper}
import org.mule.core.Specs
import org.scalameter.api._
import org.scalameter.picklers.Implicits._

import scala.collection.JavaConverters._

object AMFBenchmarkValidation
  extends Bench[Double] {

  /* configuration */

  override lazy val executor = LocalExecutor(
    new Executor.Warmer.Default,
    Aggregator.median[Double],
    measurer)

  override lazy val measurer = new Measurer.Default
  override lazy val reporter = new AMFLogReporter[Double]
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
    measure method "validate" in {
      using(inputFile) config (
        exec.benchRuns -> 3,
      ) in {
        f => testValidation(f)
      }
    }
  }

  def testValidation(file: String): Unit = {
    testValidation(new File(file))
  }

  def testValidation(file: File): Unit ={
    var baseUnit: BaseUnit = null
    val apiKind = Specs.getApiKind(file)
    AmfParsingHelper.handleParse(file, apiKind) match {
      case Right(b) => baseUnit = b
      case Left(e) => AMFBenchmarkCommon.printError(s"AMF PARSING ERROR: ${e.getMessage}", e, file)
    }

    AmfValidationHelper.handleValidation(apiKind, baseUnit) match {
      case Right(r) =>
        if (!r.conforms) {
          println(s"AMF VALIDATION ERROR: ${AmfValidationHelper.handleValidationResults(r.results.asScala.toList)}")
        } // else do nothing
      case Left(e) => AMFBenchmarkCommon.printError(s"AMF VALIDATION ERROR: ${e.getMessage}", e, file)
    }
  }

}