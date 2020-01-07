package org.mule.amf


import java.io.File

import amf._
import amf.client.AMF
import amf.client.parse.{Oas20Parser, Oas30Parser, Parser, Raml08Parser, Raml10Parser}
import amf.client.render._
import amf.client.resolve._
import org.mule.core.Specs._


object AmfObjectsHandler {
  def getProfileName(kind: APIType): ProfileName = {
    kind match {
      case RAML10 => ProfileNames.RAML
      case RAML08 => ProfileNames.RAML08
      case OAS20 => ProfileNames.OAS // JSON
//      case OAS20_YAML => ProfileNames.OAS
      case OAS30 => ProfileNames.OAS30 // JSON
//      case OAS30_YAML => ProfileNames.OAS30
      case _ => ProfileNames.AMF
    }
  }

//  def createParser(apiType: APIType, extension: String): Parser = {
  def createParser(apiType: APIType): Parser = {
    apiType match {
      case RAML10 => AMF.raml10Parser()
      case RAML08 => AMF.raml08Parser()
      case OAS20 => new Oas20Parser() // JSON
//      case OAS20_YAML => new Oas20YamlParser()
      case OAS20 => new Oas30Parser() // JSON
//      case OAS20_YAML => new Oas30YamlParser()
      case JSON_LD => AMF.amfGraphParser()
      case _ => throw new IllegalArgumentException()
    }
  }

  def createParser(apiType: APIType, file: File): Parser = {
    apiType match {
      case RAML10 => new Raml10Parser()
      case RAML08 => {
        new Raml08Parser()
      }
      case OAS20 => new Oas20Parser() // JSON
//      case OAS20_YAML => new Oas20YamlParser()
      case OAS30 => new Oas30Parser() // JSON
//      case OAS30_YAML => new Oas30YamlParser()
      case JSON_LD => AMF.amfGraphParser()
      case _ => throw new IllegalArgumentException()
    }
  }

  def createGenerator(apiType: APIType): Renderer = {
    apiType match {
      case RAML10 => new Raml10Renderer()
      case RAML08 => new Raml08Renderer()
      case OAS20 => new Oas20Renderer() // JSON
//      case OAS20_YAML => new Renderer(amf.core.remote.Oas20.name, "application/yaml")
      case OAS30 => new Oas30Renderer() // JSON
//      case OAS20_YAML => new Renderer(amf.core.remote.Oas30.name, "application/yaml")
      case JSON_LD => new AmfGraphRenderer()
      case _ => throw new IllegalArgumentException()
    }
  }

  def createResolver(apiType: APIType): Resolver = {
    apiType match {
      case RAML10 => new Raml10Resolver()
      case RAML08 => new Raml08Resolver()
      case OAS20 => new Oas20Resolver() // JSON
//      case OAS20_YAML => new Oas20Resolver()
      case OAS30 => new Oas30Resolver() // JSON
//      case OAS30_YAML => new Oas30Resolver()
      case JSON_LD => new AmfGraphResolver()
      case _ => throw new IllegalArgumentException()
    }
  }
}
