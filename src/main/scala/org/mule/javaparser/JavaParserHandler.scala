package org.mule.javaparser

import org.raml.parser.visitor.RamlValidationService
import org.raml.v2.api.RamlModelBuilder

object JavaParserHandler {

  lazy val JavaParserHandlerV2 = new RamlModelBuilder

  def getParserV2: RamlModelBuilder = {
    JavaParserHandlerV2
  }

  def getValidatorServiceV1: RamlValidationService = {
    RamlValidationService.createDefault
  }
}
