package org.mule.amf

import amf.core.benchmark
import amf.client.AMF
import amf.client.model.document.BaseUnit
import amf.client.validate.{ValidationReport, ValidationResult}
import amf.core.benchmark.ExecutionLog
import org.mule.core.Specs.APIType

object AmfValidationHelper {

  def handleValidation(kind: APIType, baseUnit: BaseUnit): Either[Throwable, ValidationReport] = {
    try {
      val report = validate(kind, baseUnit)
      Right(report)
    } catch {
      case s: StackOverflowError => Left(s)
      case e: Exception => Left(e)
    }
  }

  private def validate(kind: APIType, baseUnit: BaseUnit): ValidationReport = {
    //    ExecutionLog.start()
    val profile = AmfObjectsHandler.getProfileName(kind)
    val result = AMF.validate(baseUnit, profile, profile.messageStyle).get()
//    result.conforms
//    println(result.results)
    //    ExecutionLog.finish()
    result
  }

  def handleValidationResults(amfResults: List[ValidationResult]): String = {
    val resultsGroups = amfResults.groupBy(_.level)
    s"""|Violations:
        |${resultsGroups.filter(_._1 == "Violation").flatMap(_._2).map(e => "Message: " + e.message + "; Position: " + e.position).mkString("/")}
        |Warnings:
        |${resultsGroups.filter(_._1 == "Warning").flatMap(_._2).map(e => "Message: " + e.message + "; Position: " + e.position).mkString("/")}"""
  }

}
