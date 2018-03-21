package org.mule.benchmark

import java.io.File

object AMFBenchmarkCommon{

  def printError(string: String, e: Throwable, file: File): Unit = {
    println(string)
    println(s"file : ${file.getAbsolutePath}")
    //    throw e
  }
}