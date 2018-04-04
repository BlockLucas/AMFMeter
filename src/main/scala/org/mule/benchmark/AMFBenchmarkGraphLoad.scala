package org.mule.benchmark

import java.io.File

import amf.client.AMF
import org.mule.amf.graph.AmfGraphHelper
import org.scalameter.Bench.LocalTime
import org.scalameter.api._
import org.scalameter.picklers.Implicits._

object AMFBenchmarkGraphLoad
  extends LocalTime{

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
    "src/main/resources/graph/platform/subset/955f0a36-2d23-4f8f-82bc-a401d9b16748.yaml",
    "src/main/resources/graph/tck/b29e6979-fb39-4274-8b16-171b8a3a72cd.yaml"
  )

  /* initialization */
  AMF.init().get()
  val dialectDir = new File("src/main/resources/dialects")
  val dialectFile = new File(dialectDir, "tckutor.raml")
  AMF.registerDialect("file://" + dialectFile.getAbsolutePath).get()

  /* tests */

  performance of "AMF" in {
    measure method "loadGraph" in {
      using(inputFile) config (
        exec.benchRuns -> 3,
        exec.maxWarmupRuns -> 3,
      ) in {
        f => testLoadGraph(f)
      }
    }
  }

  def testLoadGraph(file: String): Unit = {
    testLoadGraph(new File(file))
  }

  def testLoadGraph(file: File): Unit ={
    AmfGraphHelper.handleLoad(file) match {
      case Right(_) => // do nothing
      case Left(e) => AMFBenchmarkCommon.printError(s"AMF LOADING GRAPH ERROR: ${e.getMessage}", e, file)
    }
  }
}