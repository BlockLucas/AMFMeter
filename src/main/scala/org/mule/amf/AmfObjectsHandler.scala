package org.mule.amf

import amf._
import amf.core.client.{Generator, Parser, Resolver}
import org.mule.core.Specs._

object AmfObjectsHandler {

  def getProfileName(parser: Parser): String = {
    parser match {
      case _: Raml10Parser => ProfileNames.RAML
      case _: Raml08Parser => ProfileNames.RAML
      case _: Oas20Parser => ProfileNames.OAS
      case _: AmfGraphParser => ProfileNames.AMF
    }
  }

  def createParser(apiType: APIType): Parser = {
    apiType match {
      case RAML10 => AMF.raml10Parser()
      case RAML08 => AMF.raml08Parser()
      case OAS20 => AMF.oas20Parser()
      case JSON_LD => AMF.amfGraphParser()
      case _ => throw new IllegalArgumentException()
    }
  }

  def createGenerator(apiType: APIType): Generator = {
    apiType match {
      case RAML10 => AMF.raml10Generator()
      case RAML08 => AMF.raml08Generator()
      case OAS20 => AMF.oas20Generator()
      case JSON_LD => AMF.amfGraphGenerator()
      case _ => throw new IllegalArgumentException()
    }
  }

  def createResolver(apiType: APIType): Resolver = {
    apiType match {
      case RAML10 => new Raml10Resolver()
      case RAML08 => new Raml08Resolver()
      case OAS20 => new Oas20Resolver()
      case JSON_LD => new AmfGraphResolver()
      case _ => throw new IllegalArgumentException()
    }
  }
}
