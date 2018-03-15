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
    log(s"::Machine Data::")
    val machineKeys = result.context.properties
      .filterKeys(Context.machine.properties.keySet.contains).toSeq.sortBy(_._1)
    for ((key, value) <- machineKeys) {
      log(s"$key: $value")
    }
    log("")
    log(s"::Depedencies Data::")
    val amfVersion = DependenciesUtils.getDependencyVersion("amf-client")
    log(s"amfVersion: $amfVersion")
    log("")

    log(s"::Measurements Data::")
    // output measurements
    for (measurement <- result.measurements) {
      log(s"${measurement.params}: ")
      log(s"Executions: ${measurement.data.complete.size}")
      log(s"Time: ${measurement.value} ${measurement.units}")
    }

    // add a new line
    log("")
    log("")
  }

  def report(result: Tree[CurveData[T]], persistor: Persistor) = true

}
