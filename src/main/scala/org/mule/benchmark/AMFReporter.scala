package org.mule.benchmark

import org.mule.util.DependenciesUtils
import org.scalameter._
import org.scalameter.utils.Tree


/** Simply logs the measurement data to the standard output.
  */
case class AMFReporter[T]() extends Reporter[T] {

  def report(result: CurveData[T], persistor: Persistor) {
    // output context
    log(s"::Benchmark ${result.context.scope}:: modified")
    val machineKeys = result.context.properties
      .filterKeys(Context.machine.properties.keySet.contains).toSeq.sortBy(_._1)
    for ((key, value) <- machineKeys) {
      log(s"$key: $value")
    }

    // output measurements
    for (measurement <- result.measurements) {
      log(s"${measurement.params}: ${measurement.value}")
    }

    val amfVersion = DependenciesUtils.getDependencyVersion("amf-client")
    // add a new line
    log("")
  }

  def report(result: Tree[CurveData[T]], persistor: Persistor) = true

}
