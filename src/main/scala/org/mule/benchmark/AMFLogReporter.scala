package org.mule.benchmark

import amf.core.benchmark.ExecutionLog
import org.mule.util.DependenciesUtils
import org.scalameter._
import org.scalameter.utils.Tree


/** Simply logs the measurement data to the standard output.
  */
case class AMFLogReporter[T]() extends Reporter[T] {

  def report(result: CurveData[T], persistor: Persistor) {
    // output context
    println(s"::::Benchmark ${result.context.scope}::::")
    println(s"::Machine Data::")
    val machineKeys = result.context.properties
      .filterKeys(Context.machine.properties.keySet.contains).toSeq.sortBy(_._1)
    for ((key, value) <- machineKeys) {
      println(s"$key: $value")
    }
    println("")
    println(s"::Depedencies Data::")
    val amfVersion = DependenciesUtils.getDependencyVersion("amf-client")
    println(s"amfVersion: $amfVersion")
    println("")

    println(s"::Measurements Data::")
    // output measurements
    for (measurement <- result.measurements) {
      println(s"${measurement.params}: ")
      println(s"Executions: ${measurement.data.complete.size}")
      println(s"Time: ${measurement.value.asInstanceOf[Double].toLong} ${measurement.units}")
      println(s"All Times: {${measurement.complete.map(_.asInstanceOf[Double].toLong).mkString(", ")}}")
      println("")
    }

    // add a new line
    println("")
    println("")

//    println(s"::::Benchmark Tony: ${result.context.scope}::::")
//    ExecutionLog.buildReport
  }

  def report(result: Tree[CurveData[T]], persistor: Persistor) = true

}
