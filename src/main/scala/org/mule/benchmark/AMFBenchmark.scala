package org.mule.benchmark

import java.io.File

import amf.AMF
import amf.model.document.BaseUnit
import org.mule.amf.{AmfParsingHelper, AmfValidationHelper}
import org.mule.core.Specs
import org.scalameter.api._
import org.scalameter.picklers.Implicits._
import scala.collection.JavaConverters._

object AMFBenchmark
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

  val inputFile: Gen[String] = Gen.enumeration("inputFile")("src/main/resources/raml08/simple.raml", "src/main/resources/raml10/simple.raml")

  /* initialization */
  AMF.init().get()

  /* tests */

  performance of "AMF" in {
    measure method "parseFileAsync" in {
      using(inputFile) in {
        f => testParse(f)
      }
    }
  }

  performance of "AMF" in {
    measure method "validate" in {
      using(inputFile) in {
        f => testValidation(f)
      }
    }
  }

  def testParse(file: String): Unit = {
    testParse(new File(file))
  }

  def testValidation(file: String): Unit = {
    testValidation(new File(file))
  }

  def testParse(file: File): Unit ={
    val apiKind = Specs.getApiKind(file)
    AmfParsingHelper.handleParse(file, apiKind) match {
      case Right(_) => // do nothing
      case Left(e) => println(s"AMF PARSING ERROR: ${e.getMessage}", e)
    }
  }

  def testValidation(file: File): Unit ={
    var baseUnit: BaseUnit = null
    val apiKind = Specs.getApiKind(file)
    AmfParsingHelper.handleParse(file, apiKind) match {
      case Right(b) => baseUnit = b
      case Left(e) => printAndThrow(s"AMF PARSING ERROR: ${e.getMessage}", e)
    }

    AmfValidationHelper.handleValidation(apiKind, baseUnit) match {
      case Right(r) =>
        if (!r.conforms) {
          println(s"AMF VALIDATION ERROR: ${AmfValidationHelper.handleValidationResults(r.results.asScala.toList)}")
        } // else do nothing
      case Left(e) => printAndThrow(s"AMF VALIDATION ERROR: ${e.getMessage}", e)
    }
  }

  private def printAndThrow(string: String, e: Throwable): Unit = {
    println(string)
    throw e
  }
}