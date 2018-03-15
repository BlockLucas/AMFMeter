package org.mule.benchmark

import org.scalameter.Measurer.{IterationBasedValue, Timer}
import org.scalameter.{Context, Quantity, log}

import scala.collection.{Seq, mutable}

/** A default measurer executes the test as many times as specified and returns the
  *  sequence of measured times.
  */
class AMFMeasurer extends Timer with IterationBasedValue {
  def name = "Measurer.Default"

  def measure[T](context: Context, measurements: Int, setup: T => Any,
                 tear: T => Any, regen: () => T, snippet: T => Any): Seq[Quantity[Double]] = {
    var iteration = 0
    val times = mutable.ListBuffer.empty[Quantity[Double]]
    var value = regen()

    while (iteration < measurements) {
      value = valueAt(context, iteration, regen, value)
      setup(value)

      val start = System.nanoTime
      snippet(value)
      val end = System.nanoTime
      val time = Quantity((end - start) / 1000000.0, "ms")

      tear(value)

      times += time
      iteration += 1
    }

    log.verbose(s"measurements: ${times.mkString(", ")}")

    times.result()
  }
}