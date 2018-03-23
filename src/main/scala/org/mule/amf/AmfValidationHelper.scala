package org.mule.amf

import amf.AMF
import amf.client.model.document.BaseUnit
import amf.client.validate.{ValidationReport, ValidationResult}
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
    AMF.validate(baseUnit, kind.label, kind.label).get()
  }

  def handleValidationResults(amfResults: List[ValidationResult]): String = {
    val resultsGroups = amfResults.groupBy(_.level)
    s"""|Violations:
        |${resultsGroups.filter(_._1 == "Violation").flatMap(_._2).map(e => "Message: " + e.message + "; Position: " + e.position).mkString("/")}
        |Warnings:
        |${resultsGroups.filter(_._1 == "Warning").flatMap(_._2).map(e => "Message: " + e.message + "; Position: " + e.position).mkString("/")}"""
  }

}
