package org.mule.javaparser

import java.io.File

import org.mule.core.Specs.{APIType, RAML08}
import org.raml.parser.rule.ValidationResult.Level

import scala.collection.JavaConverters._

object RamlParsingValidationHelper {

  def getValidationErrors(file : File, kind: APIType): Either[Exception, List[String]] = {
    try {
      if (kind == RAML08) {
        // 0.8 use v1
        val validatorService = JavaParserHandler.getValidatorServiceV1
        Right(validatorService.validate(file.getAbsolutePath).asScala.filter(_.getLevel == Level.ERROR).map(_.getMessage).toList)
      } else {
        // 1.0 use v2
        val parser = JavaParserHandler.getParserV2
        Right(parser.buildApi(file).getValidationResults.asScala.map(_.getMessage).toList)
      }
    } catch {
      case e: Exception =>
        println(s"ERROR Java Parser/Validator. File: $file - ${e.getMessage}", e)
        Left(e)
    }
  }
}
