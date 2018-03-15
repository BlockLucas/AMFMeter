package org.mule.benchmark

import org.mule.util.DependenciesUtils
import org.scalameter._
import org.scalameter.utils.Tree


/** Simply logs the measurement data to the standard output.
  */
case class AMFLogReporter[T]() extends Reporter[T] {

  def report(result: CurveData[T], persistor: Persistor) {
    // output context
    log(s"::::Benchmark ${result.context.scope}::::")
    log(s"\t::Machine Data::")
    val machineKeys = result.context.properties
      .filterKeys(Context.machine.properties.keySet.contains).toSeq.sortBy(_._1)
    for ((key, value) <- machineKeys) {
      log(s"\t\t$key: $value")
    }
    log("")
    log(s"\t::Depedencies Data::")
    val amfVersion = DependenciesUtils.getDependencyVersion("amf-client")
    log(s"\t\tamfVersion: $amfVersion")
    log("")

    log(s"\t::Measurements Data::")
    // output measurements
    for (measurement <- result.measurements) {
      log(s"\t\t${measurement.params}: ")
      log(s"\t\tExecutions: ${measurement.data.complete.size}")
      log(s"\t\tTime: ${measurement.value} ${measurement.units}")
      log(s"\t\tAll Times: ${measurement.complete.mkString(",")}")
    }

    // add a new line
    log("")
    log("")
    log("")
    log("")
  }

  def report(result: Tree[CurveData[T]], persistor: Persistor) = true

}
